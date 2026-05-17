package com.wenxi.neko_ai_agent.controller;

import com.wenxi.neko_ai_agent.common.ResultUtils;
import com.wenxi.neko_ai_agent.exception.BaseResponse;
import com.wenxi.neko_ai_agent.exception.ErrorCode;
import com.wenxi.neko_ai_agent.exception.ThrowUtils;
import com.wenxi.neko_ai_agent.model.dto.chatmemory.ChatHistoryDetailDTO;
import com.wenxi.neko_ai_agent.model.dto.chatmemory.ChatHistoryListDTO;
import com.wenxi.neko_ai_agent.model.dto.chatmemory.SaveChatMessageRequest;
import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.service.ChatHistoryService;
import com.wenxi.neko_ai_agent.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat/history")
@Slf4j
public class ChatHistoryController {

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private UserService userService;

    /**
     * 获取用户的历史对话列表
     *
     * @param appType 应用类型: love / pet / manus
     * @param request
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<ChatHistoryListDTO>> getChatHistoryList(
            @RequestParam String appType,
            HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(appType == null || appType.isEmpty(), ErrorCode.PARAMS_ERROR, "应用类型不能为空");
        ThrowUtils.throwIf(!isValidAppType(appType), ErrorCode.PARAMS_ERROR, "不支持的应用类型");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 获取历史对话列表
        List<ChatHistoryListDTO> chatHistoryList = chatHistoryService.getChatHistoryList(loginUser.getId(), appType);
        return ResultUtils.success(chatHistoryList);
    }

    /**
     * 获取单个对话的完整详细消息
     *
     * @param chatId  对话ID
     * @param appType 应用类型: love / pet / manus
     * @param request
     * @return
     */
    @GetMapping("/detail")
    public BaseResponse<ChatHistoryDetailDTO> getChatHistoryDetail(
            @RequestParam String chatId,
            @RequestParam String appType,
            HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(chatId == null || chatId.isEmpty(), ErrorCode.PARAMS_ERROR, "对话 ID 不能为空");
        ThrowUtils.throwIf(!isValidAppType(appType), ErrorCode.PARAMS_ERROR, "不支持的应用类型");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 获取对话详情
        ChatHistoryDetailDTO chatHistoryDetail = chatHistoryService.getChatHistoryDetail(loginUser.getId(), chatId, appType);
        ThrowUtils.throwIf(chatHistoryDetail == null, ErrorCode.NOT_FOUND_ERROR, "对话记录不存在");
        return ResultUtils.success(chatHistoryDetail);
    }

    /**
     * 保存对话消息
     *
     * @param saveRequest 保存请求（包含 chatId, appType, messages）
     * @param request
     * @return
     */
    @PostMapping("/save")
    public BaseResponse<Boolean> saveChatMessages(
            @RequestBody SaveChatMessageRequest saveRequest,
            HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(saveRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ThrowUtils.throwIf(saveRequest.getChatId() == null || saveRequest.getChatId().isEmpty(),
                ErrorCode.PARAMS_ERROR, "对话 ID 不能为空");
        ThrowUtils.throwIf(!isValidAppType(saveRequest.getAppType()),
                ErrorCode.PARAMS_ERROR, "不支持的应用类型");
        ThrowUtils.throwIf(saveRequest.getMessages() == null || saveRequest.getMessages().isEmpty(),
                ErrorCode.PARAMS_ERROR, "消息列表不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 保存消息
        chatHistoryService.saveChatMessages(
                loginUser.getId(),
                saveRequest.getChatId(),
                saveRequest.getAppType(),
                saveRequest.getMessages()
        );
        return ResultUtils.success(true);
    }

    /**
     * 删除对话记录
     *
     * @param chatId  对话ID
     * @param appType 应用类型: love / pet / manus
     * @param request
     * @return
     */
    @DeleteMapping("/delete")
    public BaseResponse<Boolean> deleteChatHistory(
            @RequestParam String chatId,
            @RequestParam String appType,
            HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(chatId == null || chatId.isEmpty(), ErrorCode.PARAMS_ERROR, "对话 ID 不能为空");
        ThrowUtils.throwIf(!isValidAppType(appType), ErrorCode.PARAMS_ERROR, "不支持的应用类型");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        boolean result = chatHistoryService.deleteChatHistory(loginUser.getId(), chatId, appType);
        ThrowUtils.throwIf(!result, ErrorCode.NOT_FOUND_ERROR, "删除失败，对话记录不存在");
        return ResultUtils.success(true);
    }

    /**
     * 校验应用类型是否合法
     */
    private boolean isValidAppType(String appType) {
        return "love".equals(appType) || "pet".equals(appType) || "manus".equals(appType);
    }
}
