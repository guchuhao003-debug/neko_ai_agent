package com.wenxi.neko_ai_agent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.wenxi.neko_ai_agent.constant.FileConstant;
import com.wenxi.neko_ai_agent.utils.GeneratedFileUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;

/**
 * 资源下载工具
 */
public class ResourceDownloadTool {

    @Tool(description = "Download a resource from a given URL")
    public String downloadResource(
            @ToolParam(description = "URL of the resource to download") String url,
            @ToolParam(description = "Name of the file to save the download resource") String fileName
    ) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/download";
        String filePath = fileDir + "/" + fileName;
        try {
            // 创建目录
            FileUtil.mkdir(fileDir);
            // 使用 Hutool 的 downloadFile 方法来下载资源
            HttpUtil.downloadFile(url, new File(filePath));
            String fileLink = GeneratedFileUtils.buildMarkdownLink(
                    GeneratedFileUtils.DOWNLOAD_CATEGORY,
                    FileUtil.getName(filePath),
                    "打开下载文件：" + FileUtil.getName(filePath)
            );
            return "Resource downloaded successfully. " + fileLink;
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }

    }
}
