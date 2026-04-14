package com.wenxi.neko_ai_agent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceDownloadToolTest {

    @Test
    void downloadResource() {
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        String url = "https://tse3-mm.cn.bing.net/th/id/OIP-C._qG9wtspfrHRAVgVragloAAAAA?w=203&h=271&c=7&r=0&o=7&pid=1.7&rm=3";
        String fileName = "JasonZhang.png";
        String result = resourceDownloadTool.downloadResource(url, fileName);
        Assertions.assertNotNull(result);
    }
}