package com.wenxi.neko_ai_agent.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wenxi.neko_ai_agent.annotation.AuthCheck;
import com.wenxi.neko_ai_agent.common.DeleteRequest;
import com.wenxi.neko_ai_agent.common.ResultUtils;
import com.wenxi.neko_ai_agent.constant.UserConstant;
import com.wenxi.neko_ai_agent.exception.BaseResponse;
import com.wenxi.neko_ai_agent.exception.BusinessException;
import com.wenxi.neko_ai_agent.exception.ErrorCode;
import com.wenxi.neko_ai_agent.exception.ThrowUtils;
import com.wenxi.neko_ai_agent.manager.CosManager;
import com.wenxi.neko_ai_agent.model.dto.agent.AgentCreateRequest;
import com.wenxi.neko_ai_agent.model.dto.agent.AgentUpdateRequest;
import com.wenxi.neko_ai_agent.model.entity.Agent;
import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.model.vo.AgentVO;
import com.wenxi.neko_ai_agent.service.AgentService;
import com.wenxi.neko_ai_agent.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 自定义智能体接口。
 */
@RestController
@RequestMapping("/agent")
@Slf4j
public class AgentController {

    @Resource
    private AgentService agentService;

    @Resource
    private UserService userService;

    @Resource
    private CosManager cosManager;

    /**
     * 创建智能体。
     *
     * @param agentCreateRequest 创建请求
     * @param request HTTP 请求
     * @return 智能体 ID
     */
    @PostMapping("/create")
    public BaseResponse<Long> createAgent(@RequestBody AgentCreateRequest agentCreateRequest,
                                          HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Agent agent = agentService.createAgent(String.valueOf(loginUser.getId()), agentCreateRequest);
        return ResultUtils.success(agent.getId());
    }

    /**
     * 更新智能体。
     *
     * @param agentUpdateRequest 更新请求
     * @param request HTTP 请求
     * @return 是否成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateAgent(@RequestBody AgentUpdateRequest agentUpdateRequest,
                                             HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        agentService.updateAgent(String.valueOf(loginUser.getId()), agentUpdateRequest);
        return ResultUtils.success(true);
    }

    /**
     * 删除智能体。
     *
     * @param deleteRequest 删除请求
     * @param request HTTP 请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteAgent(@RequestBody DeleteRequest deleteRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        agentService.deleteAgent(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 获取我的智能体列表。
     *
     * @param current 当前页
     * @param pageSize 每页大小
     * @param request HTTP 请求
     * @return 智能体分页
     */
    @GetMapping("/list/my")
    public BaseResponse<Page<AgentVO>> listMyAgents(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<Agent> agentPage = agentService.listUserAgents(String.valueOf(loginUser.getId()),
                current, pageSize);
        return ResultUtils.success(toVoPage(agentPage));
    }

    /**
     * 获取公开智能体列表。
     *
     * @param current 当前页
     * @param pageSize 每页大小
     * @return 智能体分页
     */
    @GetMapping("/list/public")
    public BaseResponse<Page<AgentVO>> listPublicAgents(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<Agent> agentPage = agentService.listPublicAgents(current, pageSize);
        return ResultUtils.success(toVoPage(agentPage));
    }

    /**
     * 获取所有智能体列表（管理员）。
     *
     * @param current 当前页
     * @param pageSize 每页大小
     * @return 智能体分页
     */
    @GetMapping("/list/all")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AgentVO>> listAllAgents(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<Agent> agentPage = agentService.listAllAgents(current, pageSize);
        return ResultUtils.success(toVoPage(agentPage));
    }

    /**
     * 获取智能体详情。
     *
     * @param id 智能体 ID
     * @param request HTTP 请求
     * @return 智能体详情
     */
    @GetMapping("/get")
    public BaseResponse<AgentVO> getAgent(@RequestParam Long id, HttpServletRequest request) {
        String userId = null;
        try {
            User loginUser = userService.getLoginUser(request);
            userId = String.valueOf(loginUser.getId());
        } catch (Exception ignored) {
            // 公开智能体允许未登录查看，私有智能体会在 Service 内继续鉴权。
        }
        Agent agent = agentService.getAgent(userId, id);
        return ResultUtils.success(toAgentVO(agent));
    }

    /**
     * 自定义智能体 SSE 聊天。
     *
     * @param agentId 智能体 ID
     * @param chatId 会话 ID
     * @param message 用户消息
     * @param modelId 临时模型 ID
     * @param request HTTP 请求
     * @return SSE 文本流
     */
    @GetMapping(value = "/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatWithAgent(@RequestParam Long agentId,
                                      @RequestParam String chatId,
                                      @RequestParam String message,
                                      @RequestParam(required = false) String modelId,
                                      HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return agentService.streamChat(String.valueOf(loginUser.getId()), agentId, chatId,
                modelId, message).onErrorResume(e -> {
                    log.error("Agent SSE streaming error, agentId={}, userId={}: {}",
                            agentId, loginUser.getId(), e.getMessage(), e);
                    return Flux.just("[智能体调用异常] " + e.getMessage());
                });
    }

    /**
     * 上传智能体头像到 COS。
     *
     * @param multipartFile 图片文件
     * @param request HTTP 请求
     * @return 头像 URL
     */
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAgentAvatar(
            @RequestPart("file") MultipartFile multipartFile,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);

        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        if (multipartFile.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过 2MB");
        }
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        List<String> allowedSuffixes = Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".gif");
        if (!allowedSuffixes.contains(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "不支持的文件格式，仅支持 jpg/jpeg/png/webp/gif");
        }

        String key = String.format("agent-avatar/%s/%s%s", loginUser.getId(),
                UUID.randomUUID().toString().replace("-", ""), suffix);

        File tempFile = null;
        try {
            tempFile = File.createTempFile("agent_avatar_", suffix);
            multipartFile.transferTo(tempFile);
            String avatarUrl = cosManager.uploadFile(key, tempFile);
            return ResultUtils.success(avatarUrl);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("智能体头像上传失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "头像上传失败");
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 转换分页视图。
     *
     * @param agentPage 智能体分页
     * @return 视图分页
     */
    private Page<AgentVO> toVoPage(Page<Agent> agentPage) {
        Page<AgentVO> voPage = new Page<>(agentPage.getCurrent(), agentPage.getSize(),
                agentPage.getTotal());
        voPage.setRecords(agentPage.getRecords().stream().map(this::toAgentVO).toList());
        return voPage;
    }

    /**
     * 转换智能体视图。
     *
     * @param agent 智能体
     * @return 视图对象
     */
    private AgentVO toAgentVO(Agent agent) {
        AgentVO agentVO = new AgentVO();
        BeanUtil.copyProperties(agent, agentVO);
        return agentVO;
    }
}
