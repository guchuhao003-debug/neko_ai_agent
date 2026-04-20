package com.wenxi.neko_ai_agent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是温习";
        String answer = loveApp.doChat(message,chatId);
        // 第二轮
        message = "我是一名程序员，单身，最近在相亲，但是总是找不到心仪的对象，你能帮我分析一下吗？";
        answer = loveApp.doChat(message,chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我单身吗？我刚告诉过你";
        answer = loveApp.doChat(message,chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 测试 AI 恋爱报告功能
     */
    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是温习，我想让另一半（鞠婧祎）更爱我，但我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }

    /**
     * 测试 RAG 知识库问答功能
     */
    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办？";
        String doChatWithRag = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(doChatWithRag);
    }

    /**
     * 测试 RAG 恋爱对象匹配功能
     */
    @Test
    void doChatWithRagCloud(){
        String chatId = UUID.randomUUID().toString();
        String message = "我想要一个女朋友，帮我推荐一个吧";
        String answer = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);

    }

    // 测试工具调用
    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");

        // 测试网页抓取：恋爱案例分析
        testMessage("最近和对象吵架了，看看虎扑社区（https://bbs.hupu.com/love-20）的其他情侣是怎么解决矛盾的？");

        // 测试资源下载：图片下载
        testMessage("直接下载一张适合做手机壁纸的歌手张杰图片为文件");

        // 测试终端操作：执行代码
        testMessage("执行 Python3 脚本来生成数据分析报告");

        // 测试文件操作：保存用户档案
        testMessage("保存我的恋爱档案为文件");

        // 测试 PDF 生成
        testMessage("生成一份‘七夕约会计划’PDF，包含餐厅预订、活动流程和礼物清单");

    }

    @Test
    void testTool() {
        testMessage("发送邮件给 Tom 的 QQ 邮箱（2836143370）, 主题是七夕，内容是七夕的礼物");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        String message = "我的另一半住在深圳市龙华区福城街道，帮我寻找一下附近5公里内的商场有哪些？";
        String answer = loveApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }
}