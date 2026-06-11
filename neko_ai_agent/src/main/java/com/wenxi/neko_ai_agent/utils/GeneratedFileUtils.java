package com.wenxi.neko_ai_agent.utils;

import cn.hutool.core.util.StrUtil;
import com.wenxi.neko_ai_agent.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Set;

/**
 * 生成文件访问链接工具类。
 */
@Slf4j
public final class GeneratedFileUtils {

    public static final String PDF_CATEGORY = "pdf";

    public static final String FILE_CATEGORY = "file";

    public static final String DOWNLOAD_CATEGORY = "download";

    private static final Set<String> ALLOWED_CATEGORIES =
            Set.of(PDF_CATEGORY, FILE_CATEGORY, DOWNLOAD_CATEGORY);

    private static final String DEFAULT_PUBLIC_BASE_URL = "http://localhost:8123/api";

    private static final String FILE_URL_SEGMENT = "/files/";

    private static final MediaType MARKDOWN_UTF8 =
            new MediaType("text", "markdown", StandardCharsets.UTF_8);

    private static final MediaType TEXT_PLAIN_UTF8 =
            new MediaType("text", "plain", StandardCharsets.UTF_8);

    private static final MediaType TEXT_CSV_UTF8 =
            new MediaType("text", "csv", StandardCharsets.UTF_8);

    private GeneratedFileUtils() {
    }

    /**
     * 判断文件分类是否允许访问。
     *
     * @param category 文件分类
     * @return 是否允许
     */
    public static boolean isAllowedCategory(String category) {
        return ALLOWED_CATEGORIES.contains(category);
    }

    /**
     * 构建生成文件的保存目录。
     *
     * @param category 文件分类
     * @return 保存目录路径
     */
    public static Path buildCategoryDir(String category) {
        return Path.of(FileConstant.FILE_SAVE_DIR, category).toAbsolutePath().normalize();
    }

    /**
     * 构建生成文件的可访问 URL。
     *
     * @param category 文件分类
     * @param fileName 文件名
     * @return 可访问 URL
     */
    public static String buildAccessUrl(String category, String fileName) {
        String encodedFileName = UriUtils.encodePathSegment(fileName, StandardCharsets.UTF_8);
        return resolvePublicBaseUrl() + "/files/" + category + "/" + encodedFileName;
    }

    /**
     * 构建生成文件的 Markdown 链接。
     *
     * @param category 文件分类
     * @param fileName 文件名
     * @param label 链接文案
     * @return Markdown 链接
     */
    public static String buildMarkdownLink(String category, String fileName, String label) {
        String linkLabel = StrUtil.blankToDefault(label, "打开文件");
        return "[" + linkLabel + "](" + buildAccessUrl(category, fileName) + ")";
    }

    /**
     * 解析生成文件的媒体类型，文本类文件必须显式声明 UTF-8。
     *
     * @param filePath 文件路径
     * @return 媒体类型
     */
    public static MediaType resolveMediaType(Path filePath) {
        String lowerFileName = filePath.getFileName().toString().toLowerCase();
        if (lowerFileName.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        if (lowerFileName.endsWith(".md") || lowerFileName.endsWith(".markdown")) {
            return MARKDOWN_UTF8;
        }
        if (lowerFileName.endsWith(".txt") || lowerFileName.endsWith(".log")) {
            return TEXT_PLAIN_UTF8;
        }
        if (lowerFileName.endsWith(".csv")) {
            return TEXT_CSV_UTF8;
        }
        return resolveDetectedMediaType(filePath);
    }

    /**
     * 根据文件名、文件访问链接或保存路径解析已生成文件。
     *
     * @param fileReference 文件名、访问链接或保存路径
     * @return 已存在的文件路径，不存在时返回 null
     */
    public static Path resolveExistingFilePath(String fileReference) {
        if (StrUtil.isBlank(fileReference)) {
            return null;
        }
        String normalizedReference = decodeReference(extractMarkdownUrl(fileReference));
        Path urlPath = resolveFromAccessUrl(normalizedReference);
        if (urlPath != null) {
            return urlPath;
        }
        Path savedPath = resolveFromSavedPath(normalizedReference);
        if (savedPath != null) {
            return savedPath;
        }
        return resolveByFileName(normalizedReference);
    }

    /**
     * 使用系统探测结果解析媒体类型，并为 text/* 补齐 UTF-8。
     *
     * @param filePath 文件路径
     * @return 媒体类型
     */
    private static MediaType resolveDetectedMediaType(Path filePath) {
        try {
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                return MediaType.APPLICATION_OCTET_STREAM;
            }
            MediaType mediaType = MediaType.parseMediaType(contentType);
            if ("text".equalsIgnoreCase(mediaType.getType())
                    && mediaType.getCharset() == null) {
                return new MediaType(mediaType, StandardCharsets.UTF_8);
            }
            return mediaType;
        } catch (IOException e) {
            log.debug("无法识别生成文件媒体类型: {}", filePath, e);
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 从文件访问链接中解析文件路径。
     *
     * @param fileReference 文件访问链接
     * @return 文件路径
     */
    private static Path resolveFromAccessUrl(String fileReference) {
        int filesIndex = fileReference.indexOf(FILE_URL_SEGMENT);
        if (filesIndex < 0) {
            return null;
        }
        String fileSegment = fileReference.substring(filesIndex + FILE_URL_SEGMENT.length());
        int queryIndex = fileSegment.indexOf('?');
        if (queryIndex >= 0) {
            fileSegment = fileSegment.substring(0, queryIndex);
        }
        int hashIndex = fileSegment.indexOf('#');
        if (hashIndex >= 0) {
            fileSegment = fileSegment.substring(0, hashIndex);
        }
        String[] parts = fileSegment.split("/", 2);
        if (parts.length != 2 || !isAllowedCategory(parts[0])) {
            return null;
        }
        return resolveInCategory(parts[0], parts[1]);
    }

    /**
     * 从保存路径中解析生成文件。
     *
     * @param fileReference 文件保存路径
     * @return 文件路径
     */
    private static Path resolveFromSavedPath(String fileReference) {
        try {
            Path rootDir = Path.of(FileConstant.FILE_SAVE_DIR).toAbsolutePath().normalize();
            Path filePath = Path.of(fileReference).toAbsolutePath().normalize();
            if (filePath.startsWith(rootDir) && Files.isRegularFile(filePath)) {
                return filePath;
            }
        } catch (InvalidPathException ignored) {
            return null;
        }
        return null;
    }

    /**
     * 按文件名在所有生成文件目录中查找。
     *
     * @param fileReference 文件名或包含文件名的引用
     * @return 文件路径
     */
    private static Path resolveByFileName(String fileReference) {
        for (String category : ALLOWED_CATEGORIES) {
            Path filePath = resolveInCategory(category, fileReference);
            if (filePath != null) {
                return filePath;
            }
        }
        return null;
    }

    /**
     * 在指定生成文件目录中查找文件。
     *
     * @param category 文件分类
     * @param fileReference 文件名或路径
     * @return 文件路径
     */
    private static Path resolveInCategory(String category, String fileReference) {
        String fileName = extractFileName(fileReference);
        if (StrUtil.isBlank(fileName)) {
            return null;
        }
        Path categoryDir = buildCategoryDir(category);
        Path filePath = categoryDir.resolve(fileName).normalize();
        if (filePath.startsWith(categoryDir) && Files.isRegularFile(filePath)) {
            return filePath;
        }
        return null;
    }

    /**
     * 从引用中提取安全文件名，避免路径穿越。
     *
     * @param fileReference 文件引用
     * @return 文件名
     */
    private static String extractFileName(String fileReference) {
        String normalized = StrUtil.blankToDefault(fileReference, "")
                .replace("\\", "/")
                .trim();
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        int hashIndex = normalized.indexOf('#');
        if (hashIndex >= 0) {
            normalized = normalized.substring(0, hashIndex);
        }
        int lastSlashIndex = normalized.lastIndexOf('/');
        return lastSlashIndex >= 0 ? normalized.substring(lastSlashIndex + 1) : normalized;
    }

    /**
     * 解码文件引用中的 URL 编码字符。
     *
     * @param fileReference 原始文件引用
     * @return 解码后的文件引用
     */
    private static String decodeReference(String fileReference) {
        return URLDecoder.decode(fileReference.trim(), StandardCharsets.UTF_8);
    }

    /**
     * 从 Markdown 链接中提取真实 URL，普通文件引用会原样返回。
     *
     * @param fileReference 原始文件引用
     * @return URL 或原始文件引用
     */
    private static String extractMarkdownUrl(String fileReference) {
        String trimmedReference = fileReference.trim();
        int linkStart = trimmedReference.indexOf("](");
        if (linkStart < 0) {
            return trimmedReference;
        }
        int urlStart = linkStart + 2;
        int urlEnd = trimmedReference.indexOf(')', urlStart);
        if (urlEnd < 0) {
            return trimmedReference.substring(urlStart);
        }
        return trimmedReference.substring(urlStart, urlEnd);
    }

    /**
     * 获取外部可访问的 API 基础地址。
     *
     * @return API 基础地址
     */
    private static String resolvePublicBaseUrl() {
        String propertyValue = System.getProperty("neko.file.public-base-url");
        String envValue = System.getenv("NEKO_FILE_PUBLIC_BASE_URL");
        String baseUrl = StrUtil.blankToDefault(propertyValue, envValue);
        return StrUtil.removeSuffix(StrUtil.blankToDefault(baseUrl, DEFAULT_PUBLIC_BASE_URL), "/");
    }
}
