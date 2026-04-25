package com.wenxi.neko_ai_agent.rag;

import com.wenxi.neko_ai_agent.constant.GlobalConstant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.ai.document.Document;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootTest
class GitHubDocumentLoaderTest {

    @Test
    public void test() throws IOException {

        /**
         * 获取 Token
         */
        String token = GlobalConstant.GITHUB_TOKEN;

        // 推荐使用 GitHubBuilder
        GitHub github = new GitHubBuilder().withOAuthToken(token).build();
        GitHubDocumentReader gitHubDocumentLoader = GitHubDocumentReader.builder()
                .gitHub(github)
                .owner(GlobalConstant.GITHUB_OWNER)
                .repo("hello-GithubDocumentLoader")
                .branch("main")
                .build();

        // 读取文件
        Document document = gitHubDocumentLoader.loadDocument("/test.md");
        // 读取目录
        List<Document> documents = gitHubDocumentLoader.loadDocuments("/");
        // 获取仓库信息
        Map<String, Object> repositoryInfo = gitHubDocumentLoader.getRepositoryInfo();
        Assertions.assertNotNull(repositoryInfo);

    }

    // 指定特定文件
    @Test
    void testWithPointer() throws IOException {

        /**
         * 获取 Token
         */
        String token = GlobalConstant.GITHUB_TOKEN;

        // 创建 gitHub 客户端
        GitHub gitHub = new GitHubBuilder().withOAuthToken(token).build();
        // 使用 Builder 模式创建 DocumentReader
        GitHubDocumentReader reader = GitHubDocumentReader.builder()
                .gitHub(gitHub)
                .owner(GlobalConstant.GITHUB_OWNER)
                .repo("hello-GithubDocumentLoader")
                .branch("main")
                .withPointer("test.md")   // 指定特定文件
                .build();

        // 获取文档列表
        List<Document> documents = reader.get();
        Assertions.assertNotNull(documents);
    }

    @Test
    void testWithReader() throws IOException {

        /**
         * 获取 Token
         */
        String token = GlobalConstant.GITHUB_TOKEN;

        // 创建 gitHub 客户端
        GitHub gitHub = new GitHubBuilder().withOAuthToken(token).build();
        // 使用 Builder 模式创建 DocumentReader
        GitHubDocumentReader reader = GitHubDocumentReader.builder()
                .gitHub(gitHub)
                .owner(GlobalConstant.GITHUB_OWNER)
                .repo("hello-GithubDocumentLoader")
                .branch("main")
                .withStartPath("/")
                .withFileExtensions("md")   // 指定文件扩展名
                .withAdditionalMetadata(
                        Map.of("author", "wenxi", "date", "2026-4-25")
                )   // 添加额外的元数据

                .build();

        // 获取文档列表
        List<Document> documents = reader.get();
        Assertions.assertNotNull(documents);
    }



}