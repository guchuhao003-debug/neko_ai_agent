package com.wenxi.neko_ai_agent.tools;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集中的工具注册类 （依赖注入模式、工厂模式、注册模式、适配器模式）
 * 注册模式： 该类作为一个中央注册点，集中管理和注册所有可用的工具，使其能被系统其他部分统一访问
 */
@Configuration
public class ToolRegistration {

    /**
     * 依赖注入模式
     */
    @Value("${search-api.api-key}")
    private String searchApiKey;

    /**
     * 工厂模式： allTools 作为一个工厂方法，负责创建和配置多个工具实例，统一包装返回。
     *
     * @return
     */
    @Bean
    public ToolCallback[] allTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        QQEmailSenderTool qqEmailSenderTool = new QQEmailSenderTool();
        TerminateTool terminateTool = new TerminateTool();
        // ToolCallbacks.from() : 把一系列工具类对象转换为工具
        // 适配器模式的应用： ToolCallbasks.from() 可以看作是一种适配器，它将各种不同的工具类转换为统一的 ToolCallback 数组
        // 使得系统能够以一致的方式处理它们。
        return ToolCallbacks.from(
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool,
                terminateTool,
                qqEmailSenderTool
        );
    }



}
