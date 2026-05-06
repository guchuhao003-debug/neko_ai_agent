package com.wenxi.neko_ai_agent.rag;

import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * GitHub 文档加载器： 用于加载 GitHub 仓库中的文档内容
 * 仿照 Spring AI 官方的 DocumentReader 的 Builder 构造器实现，并无实现 DocumentReader 接口，可以自定义增强
 */
@Slf4j
public class GitHubDocumentReader implements DocumentReader {

    /**
     * GitHub 实例
     */
    private final GitHub gitHub;

    /**
     * GitHub 仓库所有者
     */
    private final String owner;

    /**
     * GitHub 仓库名称
     */
    private final String repo;

    /**
     * GitHub 仓库分支
     */
    private final String branch;
    /**
     * 起始路径
     */
    private String startPath;
    /**
     * 文件扩展名
     */
    private List<String> fileExtensions;
    /**
     * 是否递归加载
     */
    private boolean recursive = true;
    /**
     * 最大文件大小
     */
    private long maxFileSize = Long.MAX_VALUE;
    /**
     * 字符集
     */
    private Charset charset = StandardCharsets.UTF_8;
    /**
     * 自定义元数据
     */
    private Map<String, Object> customMetadata;
    /**
     * 指针（定位）
     */
    private String pointer;

    /**
     * 构造函数
     * @param builder Builder 实例
     */
    public GitHubDocumentReader(Builder builder) {
        Assert.notNull(builder.gitHub,"GitHub 实例不能为空");
        Assert.notNull(builder.owner,"GitHub 仓库所有者不能为空");
        Assert.notNull(builder.repo,"GitHub 仓库名称不能为空");
        this.gitHub = builder.gitHub;
        this.owner = builder.owner;
        this.repo = builder.repo;
        this.branch = builder.branch != null ? builder.branch : "main";
        this.startPath = builder.startPath;
        this.fileExtensions = builder.fileExtensions;
        this.recursive = builder.recursive;
        this.maxFileSize = builder.maxFileSize;
        this.charset = builder.charset;
        this.customMetadata = builder.customMetadata;
        this.pointer = builder.pointer;
    }

    /**
     * 加载单个文件内容
     * @param path  文件路径
     * @return  Document 对象
     */
    public Document loadDocument(String path) {
        try {
            GHContent content = getRepository().getFileContent(path, branch);
            Assert.isTrue(content.isFile(),"路径必须指向文件");
            return createDocument(content);
        } catch (IOException e) {
            log.error("从 GitHub 加载文档失败：{}", path, e);
            throw new RuntimeException("从 GitHub 加载文档失败：" + path, e);
        }
    }

    /**
     * 加载目录下的所有文件
     * @param path   目录路径
     * @return  Document列表
     */
    public List<Document> loadDocuments(String path) {
        List<Document> documents = new ArrayList<>();
        try {
            List<GHContent> contents = getRepository().getDirectoryContent(path, branch);
            for(GHContent content : contents) {
                if (content.isFile()) {
                    // 增加过滤逻辑
                    // 1. 检查文件扩展名
                    if (fileExtensions != null && !fileExtensions.isEmpty()) {
                        boolean matchesExtension = fileExtensions.stream().anyMatch( ext -> {
                            String fileName = content.getName();
                            int lastDotIndex = fileName.lastIndexOf('.');
                            // 如果找不到点，或者点在开头/结尾，视为无扩展名
                            if (lastDotIndex <= 0 || lastDotIndex == fileName.length() - 1) {
                                return false; // 或者根据需求返回 ext.isEmpty()
                            }
                            String fileExt = fileName.substring(lastDotIndex + 1);
                            return ext.equalsIgnoreCase(fileExt);
                        });
                        if (!matchesExtension) {
                            continue;  // 跳过不符合扩展名的文件
                        }
                        // 2.检查文件大小
                        if (content.getSize() > maxFileSize) {
                            log.debug("跳过文件，超出最大大小限制：{} ({} bytes)", content.getPath(), content.getSize());
                            continue;
                        }
                        // 过滤条件结束
                        documents.add(createDocument(content));
                    } else if (content.isDirectory() && recursive) {
                        // 3. 检查是否递归
                        documents.addAll(loadDocuments(content.getPath()));
                    }
                }
            }
        } catch (IOException e) {
            log.error("从 GitHub 加载文档失败：{}", path, e);
            throw new RuntimeException("从 GitHub 加载文档失败：" + path, e);
        }
        return documents;
    }

    /**
     * 获取仓库信息
     * @return 仓库信息 Map
     */
    public Map<String, Object> getRepositoryInfo() {
        try {
            GHRepository repository = getRepository();
            return Map.of(
                    "name", Objects.toString(repository.getName(),""),
                    "description", Objects.toString(repository.getDescription(),""),
                    "stars", repository.getStargazersCount(),
                    "forks", repository.getForksCount(),
                    "language", Objects.toString(repository.getLanguage(),""),
                    "defaultBranch", Objects.toString(repository.getDefaultBranch(), ""),
                    "htmlUrl", repository.getHtmlUrl() != null ? repository.getHtmlUrl().toString() : "",
                    "cloneUrl", Objects.toString(repository.getHttpTransportUrl(), "")
            );
        } catch (IOException e) {
            log.error("获取仓库信息失败 ：{}/{}", owner,repo, e);
            throw new RuntimeException("获取仓库信息失败: " + owner + "/" + repo, e);
        }

    }

    /**
     * 获取仓库对象
     * @return
     * @throws IOException
     */
    private GHRepository getRepository() throws IOException {
        return gitHub.getRepository(owner + "/" + repo);
    }

    /**
     * 创建 Document 对象 - 包含完整元数据
     * @param content   GitHub 内容对象
     * @return  Document 对象
     * @throws IOException
     */
    private Document createDocument(GHContent content) throws IOException {
        String text = content.getContent();
        // 构建元数据 Map (使用 HashMap 以支持动态添加)
        Map<String, Object> metadata = new HashMap<>();
        // GitHub 原始元数据
        metadata.put("github_git_url", Objects.toString(content.getGitUrl(),""));
        metadata.put("github_download_url", Objects.toString(content.getDownloadUrl(),""));
        metadata.put("github_html_url", Objects.toString(content.getHtmlUrl(),""));
        metadata.put("github_url", Objects.toString(content.getUrl(),""));
        metadata.put("github_file_name", Objects.toString(content.getName(),""));
        metadata.put("github_file_path", Objects.toString(content.getPath(),""));
        metadata.put("github_file_sha", Objects.toString(content.getSha(),""));
        metadata.put("github_file_size", Long.toString(content.getSize()));
        metadata.put("github_file_encoding", Objects.toString(content.getUrl(),""));
        // 仓库信息元数据
        metadata.put("github_owner", this.owner);
        metadata.put("github_repo", this.repo);
        metadata.put("github_branch", this.branch);
        // 解析后的元数据
        String fileName = content.getName();
        String extension = "";
        int lastDotIndex = fileName.lastIndexOf('.');
        if(lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            extension = fileName.substring(lastDotIndex + 1);
        }
        metadata.put("file_extension", extension);
        metadata.put("is_directory", content.isDirectory());
        metadata.put("encoding", Objects.toString(content.getUrl(),""));
        // 添加入口路径信息
        metadata.put("source_path", Objects.toString(this.startPath,""));
        // 合并自定义元数据（确保 customMetadata 本身不为 null，且内部 value 也不为 null）
        if(this.customMetadata != null) {
            // 过滤掉 customMetadata 中 value 为 null 的项，防止触发 Spring AI 的校验异常
            for(Map.Entry<String, Object> entry : this.customMetadata.entrySet()){
                if(entry.getValue() != null)
                    metadata.put(entry.getKey(), entry.getValue());
            }
        }
        // 返回文档
        return new Document(text, metadata);
    }

    /**
     * 创建 Builder 实例
     * @return
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 实现 DocumentReader 接口 - 获取文档列表
     * 这是 ETL 管道的核心方法
     */
    @Override
    public List<Document> get() {
        // 如果指定了 JSON Pointer, 加载特定文件
        if(this.pointer != null && !this.pointer.isEmpty()) {
            // 移除 JSON Pointer 开头的 '/'
            String path = this.pointer.startsWith("/") ? this.pointer.substring(1) : this.pointer;
            return Collections.singletonList(loadDocument(path));
        }
        // 否则加载起始路径下的所有文档
        return loadDocuments(this.startPath);
    }

    /**
     *  读取特定文档
     * @param pointer
     * @return
     */
    public Document read(String pointer) {
        return loadDocument(pointer);
    }

    /**
     * 构造器类
     */
    public static class Builder {
        // gitHub 实例
        private GitHub gitHub;
        // gitHub 仓库所有者
        private String owner;
        // gitHub 仓库名称
        private String repo;
        // gitHub 仓库分支
        private String branch;
        // 起始路径
        private String startPath;
        // 文件扩展名
        private List<String> fileExtensions;
        // 是否递归加载
        private boolean recursive = true;
        // 文件最大大小
        private long maxFileSize = Long.MAX_VALUE;
        // 字符集格式
        private Charset charset = StandardCharsets.UTF_8;
        // 自定义元数据
        private Map<String, Object> customMetadata;
        // 文档指针
        private String pointer;

        /**
         * 设置 GitHub 客户端实例
         * @param gitHub GitHub 客户端实例
         * @return  Builder 实例
         */
        public Builder gitHub(GitHub gitHub) {
            this.gitHub = gitHub;
            return this;
        }

        /**
         * 设置仓库所有者
         * @param owner 仓库所有者
         * @return  Builder 实例
         */
        public Builder owner(String owner) {
            this.owner = owner;
            return this;
        }

        /**
         * 设置仓库名称
         * @param repo 仓库名称
         * @return  Builder 实例
         */
        public Builder repo(String repo) {
            this.repo = repo;
            return this;
        }

        /**
         * 设置仓库分支
         * @param branch 仓库分支
         * @return  Builder 实例
         */
        public Builder branch(String branch) {
            this.branch = branch;
            return this;
        }

        /**
         * 设置文件扩展名过滤（可变参数）
         * 方便直接传入多个文件扩展名
         * @param fileExtensions
         * @return
         */
        public Builder withFileExtensions(String... fileExtensions) {
            this.fileExtensions = List.of(fileExtensions);
            return this;
        }

        /**
         * 添加自定义元数据
         * @return
         */
        public Builder withAdditionalMetadata(Map<String, Object> customMetadata) {
            this.customMetadata = customMetadata;
            return this;
//            if(this.customMetadata == null) {
//                this.customMetadata = new HashMap<>();
//            }
//            this.customMetadata.put(key,value);

        }

        /**
         * 设置 JSON Pointer （加载特定文件）
         * 类似 JsonReader 的指针定位功能
         * @param pointer
         * @return
         */
        public Builder withPointer(String pointer) {
            this.pointer = pointer;
            return this;
        }

        /**
         * 设置起始路径
         * @param startPath
         * @return
         */
        public Builder withStartPath(String startPath) {
            this.startPath = startPath;
            return this;
        }

        /**
         * 是否递归加载
         * @param recursive
         * @return
         */
        public Builder withRecursive(Boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        /**
         * 设置文件最大大小
         * @param maxFileSize
         * @return
         */
        public Builder withMaxFileSize(Long maxFileSize) {
            this.maxFileSize = maxFileSize;
            return this;
        }

        /**
         * 设置字符集编码格式
         * @param charset
         * @return
         */
        public Builder withCharset(Charset charset) {
            this.charset = charset;
            return this;
        }

        /**
         * 构建 GitHubDocumentLoader 实例
         * @return GitHubDocumentLoader 实例
         */
        public GitHubDocumentReader build() {
            Assert.notNull(gitHub,"GitHub 实例不能为空");
            Assert.notNull(owner,"GitHub 仓库所有者不能为空");
            Assert.notNull(repo,"GitHub 仓库名称不能为空");
            return new GitHubDocumentReader(this);
        }
    }

    /**
     * 获取文件扩展名（Java 原生实现）
     * @param filename 文件名
     * @return 扩展名（不带点），如果没有扩展名则返回空字符串
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
