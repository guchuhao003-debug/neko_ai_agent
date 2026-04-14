package com.wenxi.neko_ai_agent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebScrapingToolTest {

    @Test
    void scrapeWebPage() {
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        String pageUrl = "https://k.sina.com.cn/article_7857201851_v1d45362bb06801c4nw.html";
        String result = webScrapingTool.scrapeWebPage(pageUrl);
        Assertions.assertNotNull(result);
    }
}