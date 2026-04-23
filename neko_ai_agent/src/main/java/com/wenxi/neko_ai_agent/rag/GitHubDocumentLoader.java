package com.wenxi.neko_ai_agent.rag;

import cn.hutool.core.lang.Assert;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * GitHub 文档加载器： 用于加载 GitHub 仓库中的文档内容
 * 仿照 Spring AI 官方的 DocumentReader 的 Builder 构造器实现，并无实现 DocumentReader 接口，可以自定义增强
 */
@Slf4j
public class GitHubDocumentLoader {

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
     * 构造函数
     * @param gitHub
     * @param owner
     * @param repo
     * @param branch
     */
    public GitHubDocumentLoader(GitHub gitHub, String owner, String repo, String branch) {
        Assert.notNull(gitHub, "GitHub 实例不能为空");
        Assert.notNull(owner, "GitHub 仓库所有者不能为空");
        Assert.notNull(repo, "GitHub 仓库名称不能为空");
        this.gitHub = gitHub;
        this.owner = owner;
        this.repo = repo;
        this.branch = branch != null ? branch : "main";
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
                if(content.isFile()) {
                    documents.add(createDocument(content));
                } else if(content.isDirectory()) {
                    documents.addAll(loadDocuments(content.getPath()));
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
     * 创建 Document 对象
     * @param content   GitHub 内容对象
     * @return  Document 对象
     * @throws IOException
     */
    private Document createDocument(GHContent content) throws IOException {
        String text = content.getContent();
        Map<String, Object> metadata = Map.of(
                "github_git_url", content.getGitUrl(),
                "github_download_url", content.getDownloadUrl(),
                "github_html_url", content.getHtmlUrl(),
                "github_url", content.getUrl(),
                "github_file_name", content.getName(),
                "github_file_path", content.getPath(),
                "github_file_sha", content.getSha(),
                "github_file_size", Long.toString(content.getSize()),
                "github_file_encoding", content.getEncoding()
        );
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
     * 构造器类
     */
    public static class Builder {
        private GitHub gitHub;
        private String owner;
        private String repo;
        private String branch;

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
         * 构建 GitHubDocumentLoader 实例
         * @return GitHubDocumentLoader 实例
         */
        public GitHubDocumentLoader build() {
            return new GitHubDocumentLoader(gitHub,owner,repo,branch);
        }
    }

}
