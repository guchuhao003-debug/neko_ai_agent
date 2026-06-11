package com.wenxi.neko_ai_agent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.wenxi.neko_ai_agent.constant.FileConstant;
import com.wenxi.neko_ai_agent.utils.GeneratedFileUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * PDF 生成工具类
 */
public class PDFGenerationTool {

    private static final String PDF_FILE_DIR = FileConstant.FILE_SAVE_DIR + "/pdf";

    private static final String PDF_EXTENSION = ".pdf";

    private static final String CJK_ENCODING = "UniGB-UCS2-H";

    private static final String PRIMARY_CJK_FONT = "STSongStd-Light";

    private static final String FALLBACK_CJK_FONT = "STSong-Light";

    /**
     * 生成 PDF 文件，支持中文内容写入。
     *
     * @param fileName PDF 文件名
     * @param content PDF 正文内容
     * @return 工具执行结果
     */
    @Tool(description = "Generate a PDF file with given content")
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        try {
            String filePath = buildSafePdfPath(fileName);
            // 创建目录，确保 PDF 工具在首次调用时也能正常写入文件。
            FileUtil.mkdir(PDF_FILE_DIR);
            // 创建 PdfWriter 和 PdfDocument 对象。
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                // 使用 iText font-asian 提供的 CJK 字体，避免中文 PDF 生成时报字体不识别。
                PdfFont font = createChineseFont();
                document.setFont(font);
                // 创建段落并写入正文。
                Paragraph paragraph = new Paragraph(StrUtil.blankToDefault(content, ""));
                document.add(paragraph);
            }
            String fileLink = GeneratedFileUtils.buildMarkdownLink(
                    GeneratedFileUtils.PDF_CATEGORY,
                    FileUtil.getName(filePath),
                    "打开 PDF：" + FileUtil.getName(filePath)
            );
            return "PDF generated successfully. " + fileLink;
        } catch (Exception e) {
            return "Error generating PDF: " + e.getMessage();
        }
    }

    /**
     * 构建安全的 PDF 保存路径，防止工具参数携带路径穿越字符。
     *
     * @param fileName 原始文件名
     * @return PDF 保存路径
     */
    private String buildSafePdfPath(String fileName) {
        String safeFileName = FileUtil.getName(StrUtil.blankToDefault(fileName, "neko-manus.pdf"))
                .replaceAll("[\\\\/:*?\"<>|]+", "_");
        if (!StrUtil.endWithIgnoreCase(safeFileName, PDF_EXTENSION)) {
            safeFileName += PDF_EXTENSION;
        }
        return PDF_FILE_DIR + "/" + safeFileName;
    }

    /**
     * 创建中文字体，优先使用 iText 9 font-asian 中的 STSongStd 字体。
     *
     * @return 可渲染中文的 PDF 字体
     */
    private PdfFont createChineseFont() throws java.io.IOException {
        try {
            return PdfFontFactory.createFont(PRIMARY_CJK_FONT, CJK_ENCODING);
        } catch (Exception ignored) {
            // 部分 iText 环境只识别旧字体名，保留回退保证 PDF 工具可用。
            return PdfFontFactory.createFont(FALLBACK_CJK_FONT, CJK_ENCODING);
        }
    }
}
