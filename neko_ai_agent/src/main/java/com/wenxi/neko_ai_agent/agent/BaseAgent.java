package com.wenxi.neko_ai_agent.agent;

import cn.hutool.core.util.StrUtil;
import com.wenxi.neko_ai_agent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 运行代理
     * @param userPrompt
     * @return
     */
    public String run(String userPrompt) {
        // 1.基础校验
        if(this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state" + this.state);
        }
        if(StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("User prompt cannot be empty");
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
                String result = "Step " + stepNumber + ": " + stepResult;
                resultList.add(result);
            }
            // 检查是否超出步骤限制
            if(currentStep >= maxSteps) {
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
     * 定义单个步骤 （具体由子类去实现）
     * @return
     */
    public abstract String step();

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 子类可以重写此方法清理资源
    }

}
