package com.wenxi.neko_ai_agent.demo.invoke;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;

import java.util.Arrays;


/**
 * SDK 接入大模型
 */
public class SdkAiInvoke {

    private static final String API_KEY = "******";

    public static GenerationResult callWithMessage() throws ApiException, NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("你是一个专业的Java开发工程师，请根据用户提供的需求，给出对应的技术流程分析")
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content("请你帮我写一个登录页面技术调用流程文档")
                .build();
        GenerationParam param = GenerationParam.builder()
                // 可以直接使用上述声明的 API KEY 属性
                .apiKey(API_KEY)
                // 选择使用的模型
                .model("qwen-plus")
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
        return gen.call(param);
    }

    public static void main(String[] args) {
        try{
            GenerationResult result = callWithMessage();
            // 通过 JsonUtils 中的 toJson() 方法将 AI 返回的结果转化为 JSON 格式
            String JsonResult = JsonUtils.toJson(result);
            System.out.println(JsonResult);
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            // 使用日志框架记录异常信息
            System.err.println("An error occurred while calling the generation service: " + e.getMessage());
        }
        System.exit(0);
    }

}
