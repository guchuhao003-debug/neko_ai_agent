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
        GitHubDocumentLoader gitHubDocumentLoader = GitHubDocumentLoader.builder()
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
}