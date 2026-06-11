package com.wenxi.neko_ai_agent.tools;

import cn.hutool.core.io.FileUtil;
import com.wenxi.neko_ai_agent.constant.FileConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PDFGenerationToolTest {

    @Test
    void generatePDF() {
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        String fileName = "spring-ai-study-plan.pdf";
        String content = "我正在学习 Spring AI，需要你帮我制定一个学习计划。";
        String result = pdfGenerationTool.generatePDF(fileName, content);
        String expectedPath = FileConstant.FILE_SAVE_DIR + "/pdf/" + fileName;
        Assertions.assertTrue(result.startsWith("PDF generated successfully. "));
        Assertions.assertTrue(result.contains("/api/files/pdf/" + fileName));
        Assertions.assertTrue(FileUtil.exist(expectedPath));
    }
}
