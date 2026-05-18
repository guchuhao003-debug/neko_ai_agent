package com.wenxi.neko_ai_agent.agent;

import cn.hutool.core.util.StrUtil;
import com.wenxi.neko_ai_agent.agent.model.AgentState;
import com.wenxi.neko_ai_agent.exception.BusinessException;
import com.wenxi.neko_ai_agent.exception.ErrorCode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 *
 * 提供状态转换、内存管理和基于步数的执行循环的基础功能
 * 子类必须实现 step 方法
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // 核心属性
    private String name;

    // 提示词
    private String systemPrompt;
    private String nextStepPrompt;

    // 代理状态 （默认空闲状态）
    private AgentState state = AgentState.IDLE;

    // 执行步骤限制
    private int currentStep = 0; // 当前步数
    private int maxSteps = 10; // 最大步数

    // LLM 大模型
    private ChatClient chatClient;

    // Memory 记忆（需要自主维护会话上下文，默认空数组）
    private List<Message> messageList = new ArrayList<>();

    // 自定义线程池（用于 runStream 异步执行，避免占用 ForkJoinPool.commonPool）
    private Executor executor;

    /**
     * 运行代理
     * @param userPrompt
     * @return
     */
    public String run(String userPrompt) {
        // 1.基础校验
        if(this.state != AgentState.IDLE) {
            log.error("Agent is not in IDLE state" + this.state);
            throw new BusinessException(ErrorCode.STATE_ERROR);
        }
        if(StrUtil.isBlank(userPrompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户提示词不能为空");
        }
        // 2.执行，更改状态
        this.state = AgentState.RUNNING;
        // 记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> resultList = new ArrayList<>();
        try {
            // 执行循环
            for(int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                // currentStep 初始化为 0
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {} / {} ", stepNumber, maxSteps);
                // 单步执行
                String stepResult = step();
                if (!isTerminalStepResult(stepResult)) {
                    String result = "Step " + stepNumber + ": " + stepResult;
                    resultList.add(result);
                }
                if (state == AgentState.FINISHED) {
                    break;
                }
            }
            // 检查是否超出步骤限制
            if(currentStep >= maxSteps && state != AgentState.FINISHED) {
                state = AgentState.FINISHED;
                resultList.add("Terminated Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", resultList);
        } catch(Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            // 3.清理资源
            this.cleanup();
        }
    }

    /**
     * 运行代理（流式输出）
     * @param userPrompt
     * @return
     */
    public SseEmitter runStream(String userPrompt) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(300000L); // 5分钟超时
        // 标记连接是否已断开
        final boolean[] aborted = {false};
        sseEmitter.onError(e -> aborted[0] = true);
        sseEmitter.onTimeout(() -> {
            aborted[0] = true;
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timeout");
        });
        sseEmitter.onCompletion(() -> {
            if(this.state == AgentState.RUNNING){
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        // 使用自定义线程池异步处理，避免占用 ForkJoinPool.commonPool
        Executor taskExecutor = this.executor != null ? this.executor : ForkJoinPool.commonPool();
        CompletableFuture.runAsync(() -> {
            try {
                // 1.基础校验
                if(this.state != AgentState.IDLE) {
                    sseEmitter.send("错误：无法从状态运行代理： " + this.state);
                    sseEmitter.complete();
                    return;
                }
                if(StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("错误：无法使用空提示词运行代理");
                    sseEmitter.complete();
                    return;
                }
            } catch (Exception e) {
                sseEmitter.completeWithError(e);
                return;
            }
            // 2.执行，更改状态
            this.state = AgentState.RUNNING;
            // 记录消息上下文
            messageList.add(new UserMessage(userPrompt));
            // 保存结果列表
            List<String> resultList = new ArrayList<>();
            try {
                // 执行循环
                for(int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    if (aborted[0]) {
                        log.warn("SSE connection aborted by client, stopping agent");
                        break;
                    }
                    // currentStep 初始化为 0
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {} / {} ", stepNumber, maxSteps);
                    // 单步执行
                    String stepResult = step();
                    boolean terminalStep = isTerminalStepResult(stepResult);
                    String result = "Step " + stepNumber + ": " + stepResult;
                    if (!terminalStep) {
                        resultList.add(result);
                    }
                    // 输出当前每一步的结果到 SSE
                    if (!aborted[0] && !terminalStep) {
                        try {
                            sendSseEvent(sseEmitter, "step", result);
                        } catch (IOException e) {
                            log.warn("Client disconnected during send, stopping agent");
                            aborted[0] = true;
                            break;
                        }
                    }
                    if (state == AgentState.FINISHED) {
                        break;
                    }
                }
                if (aborted[0]) {
                    state = AgentState.FINISHED;
                    return;
                }
                // 检查是否超出步骤限制
                if(currentStep >= maxSteps && state != AgentState.FINISHED) {
                    state = AgentState.FINISHED;
                    resultList.add("Terminated Reached max steps (" + maxSteps + ")");
                    sendSseEvent(sseEmitter, "step", "执行结束，达到最大步骤：(" + maxSteps + ")");
                }
                String finalAnswer = buildFinalAnswer(userPrompt, resultList);
                if (!aborted[0] && StrUtil.isNotBlank(finalAnswer)) {
                    sendSseEvent(sseEmitter, "final", finalAnswer);
                }
                // 正常完成
                sendSseEvent(sseEmitter, "done", "[DONE]");
                sseEmitter.complete();
            } catch(Exception e) {
                state = AgentState.ERROR;
                log.error("Error executing agent", e);
                if (!aborted[0]) {
                    try {
                        sendSseEvent(sseEmitter, "step", "执行错误： " + e.getMessage());
                        sseEmitter.complete();
                    } catch (IOException ex) {
                        sseEmitter.completeWithError(ex);
                    }
                }
            } finally {
                // 3.清理资源
                this.cleanup();
            }
        }, taskExecutor);

        return sseEmitter;
    }


    /**
     * 定义单个步骤 （具体由子类去实现）
     * @return
     */
    public abstract String step();

    /**
     * 构建最终答复。
     *
     * @param userPrompt 用户原始任务
     * @param stepResults 步骤执行结果
     * @return 最终答复
     */
    protected String buildFinalAnswer(String userPrompt, List<String> stepResults) {
        if (stepResults == null || stepResults.isEmpty()) {
            return "";
        }
        return stepResults.get(stepResults.size() - 1);
    }

    /**
     * 判断当前步骤是否仅表示终止任务。
     *
     * @param stepResult 步骤结果
     * @return 是否终止步骤
     */
    protected boolean isTerminalStepResult(String stepResult) {
        if (StrUtil.isBlank(stepResult)) {
            return false;
        }
        return stepResult.contains("doTerminate") || stepResult.contains("任务结束");
    }

    /**
     * 发送具名 SSE 事件。
     *
     * @param sseEmitter SSE 连接
     * @param eventName 事件名称
     * @param data 事件内容
     * @throws IOException 发送异常
     */
    protected void sendSseEvent(SseEmitter sseEmitter, String eventName, String data)
            throws IOException {
        sseEmitter.send(SseEmitter.event().name(eventName).data(data));
    }

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 子类可以重写此方法清理资源
    }

}
