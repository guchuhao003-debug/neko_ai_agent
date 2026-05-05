package com.wenxi.neko_ai_agent.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 联网搜索工具类（ 使用 SearchApi ）
 * searchapi.io
 */
public class WebSearchTool {

    // SearchAPI 的搜索接口地址
    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";

    /**
     * Search ApiKey
     */
    @Value("${search-api.api-key}")
    private final String apiKey;

    /**
     * 使用构造器注入 SearchApiKey
     * @param apiKey
     */
    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 使用 Baidu Search Engine 搜索信息（联网搜索工具）
     * @param query
     */
    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");
        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            // 提取 organic_results 部分
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
            if (organicResults == null || organicResults.isEmpty()) {
                return "未找到相关搜索结果，请尝试更换关键词。";
            }
            // 指定返回前 5 条
            int limit = Math.min(5, organicResults.size());
            List<Object> objects = organicResults.subList(0, limit);
            // 拼接搜索结果为字符串
            String result = objects.stream().map(obj -> {
                JSONObject tmpJSONObject = (JSONObject) obj;
                return tmpJSONObject.toString();
            }).collect(Collectors.joining(","));
            return result;
        } catch (Exception e) {
            return "Error searching Baidu: " + e.getMessage();
        }
    }
}
