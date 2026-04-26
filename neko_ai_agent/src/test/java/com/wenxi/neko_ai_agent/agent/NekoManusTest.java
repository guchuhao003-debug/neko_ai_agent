package com.wenxi.neko_ai_agent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NekoManusTest {

    @Resource
    private NekoManus nekoManus;

    @Test
    void run() {
        String userPrompt = """
                我的另一半居住在广东深圳龙华区福城街道，请帮我帮到 5 公里内合适的约会地点，
                并结合一些网络图片，制定一份详细的约会计划
                并以 PDF 格式输出，且使用简体中文。
                """;
        String answer = nekoManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }

}