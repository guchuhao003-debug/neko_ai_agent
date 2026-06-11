package com.wenxi.neko_ai_agent.controller;

import com.wenxi.neko_ai_agent.annotation.AuthCheck;
import com.wenxi.neko_ai_agent.exception.BusinessException;
import com.wenxi.neko_ai_agent.exception.ErrorCode;
import com.wenxi.neko_ai_agent.utils.GeneratedFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 生成文件访问控制器。
 */
@Slf4j
@RestController
@RequestMapping("/files")
public class GeneratedFileController {

    /**
     * 打开生成文件，供前端聊天结果中的文件链接访问。
     *
     * @param category 文件分类
     * @param fileName 文件名
     * @return 文件资源响应
     */
    @GetMapping("/{category}/{fileName:.+}")
    @AuthCheck
    public ResponseEntity<Resource> openGeneratedFile(@PathVariable String category,
                                                      @PathVariable String fileName) {
        if (!GeneratedFileUtils.isAllowedCategory(category)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的文件分类");
        }
        Path categoryDir = GeneratedFileUtils.buildCategoryDir(category);
        Path filePath = categoryDir.resolve(fileName).normalize();
        if (!filePath.startsWith(categoryDir) || !Files.isRegularFile(filePath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "文件不存在");
        }
        try {
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(GeneratedFileUtils.resolveMediaType(filePath))
                    .header(HttpHeaders.CONTENT_DISPOSITION, buildInlineDisposition(fileName))
                    .body(resource);
        } catch (MalformedURLException e) {
            log.warn("生成文件访问失败: {}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件访问失败");
        }
    }

    /**
     * 构建内联打开的响应头。
     *
     * @param fileName 文件名
     * @return Content-Disposition 响应头
     */
    private String buildInlineDisposition(String fileName) {
        return ContentDisposition.inline()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();
    }
}
