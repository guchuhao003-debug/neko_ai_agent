package com.wenxi.neko_ai_agent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PDFGenerationToolTest {

    @Test
    void generatePDF() {
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        String fileName = "JasonZhang.pdf";
        String content = "六度归巢|万象春生|斗转星移|张杰开往1982巡回演唱会鸟巢站16连开圆满成功！";
        String result = pdfGenerationTool.generatePDF(fileName, content);
        Assertions.assertNotNull(result);
    }
}