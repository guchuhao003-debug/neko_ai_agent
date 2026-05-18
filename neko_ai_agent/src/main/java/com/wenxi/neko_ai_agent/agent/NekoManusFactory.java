package com.wenxi.neko_ai_agent.agent;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * NekoManus 工厂 — 每请求创建一个新实例，避免并发阻塞，同时封装工具合并逻辑。
 */
@Component
public class NekoManusFactory {

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    @Resource
    private Executor agentTaskExecutor;

    /**
     * 根据指定模型创建 NekoManus 实例（合并本地工具 + MCP 工具）。
     */
    public NekoManus create(ChatModel model) {
        List<ToolCallback> combinedTools = new ArrayList<>(Arrays.asList(allTools));
        ToolCallback[] mcpTools = (ToolCallback[]) toolCallbackProvider.getToolCallbacks();
        combinedTools.addAll(Arrays.asList(mcpTools));
        return new NekoManus(combinedTools.toArray(new ToolCallback[0]), model, agentTaskExecutor);
    }
}
