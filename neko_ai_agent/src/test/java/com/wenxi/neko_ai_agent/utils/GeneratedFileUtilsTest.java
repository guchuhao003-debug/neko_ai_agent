package com.wenxi.neko_ai_agent.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 生成文件访问链接工具测试。
 */
class GeneratedFileUtilsTest {

    /**
     * 测试生成 PDF 文件访问链接。
     */
    @Test
    void buildMarkdownLinkShouldReturnOpenableFileUrl() {
        String link = GeneratedFileUtils.buildMarkdownLink(
                GeneratedFileUtils.PDF_CATEGORY,
                "Spring AI 学习计划.pdf",
                "打开 PDF"
        );

        Assertions.assertTrue(link.contains("http://localhost:8123/api/files/pdf/"));
        Assertions.assertTrue(link.contains("Spring%20AI%20%E5%AD%A6%E4%B9%A0"));
        Assertions.assertTrue(link.startsWith("[打开 PDF]("));
    }

    /**
     * 测试不允许访问未知文件分类。
     */
    @Test
    void isAllowedCategoryShouldRejectUnknownCategory() {
        Assertions.assertTrue(GeneratedFileUtils.isAllowedCategory("pdf"));
        Assertions.assertFalse(GeneratedFileUtils.isAllowedCategory("secret"));
    }

    /**
     * 测试 Markdown 文件直接打开时会声明 UTF-8，避免浏览器乱码。
     */
    /**
     * 测试可以通过文件名找到已有的 PDF 生成文件。
     *
     * @throws Exception 文件创建异常
     */
    @Test
    void resolveExistingFilePathShouldFindGeneratedPdfByFileName() throws Exception {
        String fileName = "existing-file-test.pdf";
        Path filePath = GeneratedFileUtils.buildCategoryDir(GeneratedFileUtils.PDF_CATEGORY)
                .resolve(fileName);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, "pdf content");

        try {
            Path resolvedPath = GeneratedFileUtils.resolveExistingFilePath(fileName);

            Assertions.assertEquals(filePath.toAbsolutePath().normalize(), resolvedPath);
        } finally {
            Files.deleteIfExists(filePath);
        }
    }

    /**
     * 测试可以通过访问链接找到已有的 PDF 生成文件。
     *
     * @throws Exception 文件创建异常
     */
    @Test
    void resolveExistingFilePathShouldFindGeneratedPdfByAccessUrl() throws Exception {
        String fileName = "Spring AI 学习计划-test.pdf";
        Path filePath = GeneratedFileUtils.buildCategoryDir(GeneratedFileUtils.PDF_CATEGORY)
                .resolve(fileName);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, "pdf content");

        try {
            String accessUrl = GeneratedFileUtils.buildAccessUrl(
                    GeneratedFileUtils.PDF_CATEGORY, fileName);
            Path resolvedPath = GeneratedFileUtils.resolveExistingFilePath(accessUrl);

            Assertions.assertEquals(filePath.toAbsolutePath().normalize(), resolvedPath);
        } finally {
            Files.deleteIfExists(filePath);
        }
    }

    /**
     * 测试可以从 Markdown 链接中解析已有生成文件。
     *
     * @throws Exception 文件创建异常
     */
    @Test
    void resolveExistingFilePathShouldFindGeneratedPdfByMarkdownLink() throws Exception {
        String fileName = "markdown-link-file-test.pdf";
        Path filePath = GeneratedFileUtils.buildCategoryDir(GeneratedFileUtils.PDF_CATEGORY)
                .resolve(fileName);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, "pdf content");

        try {
            String markdownLink = GeneratedFileUtils.buildMarkdownLink(
                    GeneratedFileUtils.PDF_CATEGORY, fileName, "打开 PDF");
            Path resolvedPath = GeneratedFileUtils.resolveExistingFilePath(markdownLink);

            Assertions.assertEquals(filePath.toAbsolutePath().normalize(), resolvedPath);
        } finally {
            Files.deleteIfExists(filePath);
        }
    }

    @Test
    void resolveMediaTypeShouldUseUtf8ForMarkdown() {
        MediaType mediaType = GeneratedFileUtils.resolveMediaType(Path.of("学习计划.md"));

        Assertions.assertEquals("text", mediaType.getType());
        Assertions.assertEquals("markdown", mediaType.getSubtype());
        Assertions.assertEquals(StandardCharsets.UTF_8, mediaType.getCharset());
    }

    /**
     * 测试普通文本文件直接打开时会声明 UTF-8。
     */
    @Test
    void resolveMediaTypeShouldUseUtf8ForText() {
        MediaType mediaType = GeneratedFileUtils.resolveMediaType(Path.of("result.txt"));

        Assertions.assertEquals(MediaType.TEXT_PLAIN.getType(), mediaType.getType());
        Assertions.assertEquals(MediaType.TEXT_PLAIN.getSubtype(), mediaType.getSubtype());
        Assertions.assertEquals(StandardCharsets.UTF_8, mediaType.getCharset());
    }
}
