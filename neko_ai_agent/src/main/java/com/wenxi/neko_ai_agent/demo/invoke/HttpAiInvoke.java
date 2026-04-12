package com.wenxi.neko_ai_agent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

/**
 * HTTP 调用
 */
public class HttpAiInvoke {
    private static final Log log = LogFactory.get();

    // API端点配置
    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

    // TODO 替换为实际的API密钥
    private static final String API_KEY = "******";

    public static void main(String[] args) {
        try {
            String responseContent = sendAiRequest("你是谁？");
            log.info("AI回复：\n{}", responseContent);
        } catch (Exception e) {
            log.error("请求处理失败", e);
        }
    }

    /**
     * 发送AI请求并获取结果
     *
     * @param userQuestion 用户问题
     * @return AI回复内容
     */
    public static String sendAiRequest(String userQuestion) {

        // 构建请求体
        JSONObject requestBody = buildRequestBody(userQuestion, "qwen-plus");
        // 发送请求
        HttpResponse response = HttpRequest.post(API_URL)
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .timeout(5000)
                .execute();

        // 4. 处理响应
        if (response.getStatus() != 200) {
            throw new RuntimeException("API请求失败，状态码：" + response.getStatus());
        }

        return parseResponse(response.body());
    }

    /**
     * 构建请求JSON
     */
    private static JSONObject buildRequestBody(String userQuestion, String model) {
        JSONArray messages = new JSONArray();
        messages.add(JSONUtil.createObj()
                .set("role", "system")
                .set("content", "You are a helpful assistant."));
        messages.add(JSONUtil.createObj()
                .set("role", "user")
                .set("content", userQuestion));

        return JSONUtil.createObj()
                .set("model", model == null ? "qwen-plus" : model)
                .set("input", JSONUtil.createObj().set("messages", messages))
                .set("parameters", JSONUtil.createObj()
                        .set("result_format", "message"));
    }

    /**
     * 解析API响应
     */
    private static String parseResponse(String responseBody) {
        JSONObject responseJson = JSONUtil.parseObj(responseBody);

        // 根据API文档调整解析路径
        return responseJson.getByPath("output.choices[0].message.content", String.class);
    }

}
