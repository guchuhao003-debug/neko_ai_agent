package com.wenxi.neko_ai_agent.tools;

import com.wenxi.neko_ai_agent.utils.GeneratedFileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

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
    void readFileShouldReturnLinkForExistingPdf() throws Exception {
        FileOperationTool fileOperationTool = new FileOperationTool();
        String fileName = "file-operation-existing.pdf";
        Path filePath = GeneratedFileUtils.buildCategoryDir(GeneratedFileUtils.PDF_CATEGORY)
                .resolve(fileName);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, "pdf content");

        try {
            String result = fileOperationTool.readFile(fileName);

            Assertions.assertTrue(result.contains("/api/files/pdf/" + fileName));
        } finally {
            Files.deleteIfExists(filePath);
        }
    }

    @Test
    void writeFile() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        String fileName = "neko_ai_agent.md";
        String content = "Neko AI Agent Tool Test";
        String result = fileOperationTool.writeFile(fileName,content);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("/api/files/file/" + fileName));
    }
}
