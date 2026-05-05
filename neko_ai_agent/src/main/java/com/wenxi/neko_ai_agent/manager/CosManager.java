package com.wenxi.neko_ai_agent.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.wenxi.neko_ai_agent.config.CosClientConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 腾讯云 COS 对象存储管理器
 */
@Component
public class CosManager {

    @Resource
    private COSClient cosClient;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 获取 COS 访问域名
     *
     * @return host URL
     */
    public String getHost() {
        return cosClientConfig.getHost();
    }

    /**
     * 上传文件到 COS
     *
     * @param key  文件在 COS 中的路径（如 avatar/123/uuid.jpg）
     * @param file 本地文件
     * @return 文件的公开访问 URL
     */
    public String uploadFile(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        PutObjectResult putObjectResult = cosClient.putObject(putObjectRequest);
        // 返回公开访问 URL
        return cosClientConfig.getHost() + "/" + key;
    }

    /**
     * 删除 COS 中的文件
     *
     * @param key 文件在 COS 中的路径
     */
    public void deleteFile(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}
