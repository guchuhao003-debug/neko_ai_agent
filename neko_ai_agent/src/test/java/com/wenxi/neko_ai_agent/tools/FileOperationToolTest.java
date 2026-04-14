package com.wenxi.neko_ai_agent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 此处无需用到 SpringBoot 的配置信息，所以直接进行测试即可
 */
class FileOperationToolTest {

    @Test
    void readFile() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        String fileName = "neko_ai_agent.md";
        String result = fileOperationTool.readFile(fileName);
        Assertions.assertNotNull(result);
    }

    @Test
    void writeFile() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        String fileName = "neko_ai_agent.md";
        String content = "Neko AI Agent Tool Test";
        String result = fileOperationTool.writeFile(fileName,content);
        Assertions.assertNotNull(result);
    }
}