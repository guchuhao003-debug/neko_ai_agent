package com.wenxi.neko_ai_agent.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 网页抓取工具（使用 Jsoup 库）
 */
public class WebScrapingTool {

    @Tool(description = "Scrape the content of a web page")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        try {
            // 抓取成功
            Document document = Jsoup.connect(url).get();
            return document.html();
        } catch (Exception e) {
            // 抓取失败
            return "Error scraping web page: " + e.getMessage();
        }
    }

}
