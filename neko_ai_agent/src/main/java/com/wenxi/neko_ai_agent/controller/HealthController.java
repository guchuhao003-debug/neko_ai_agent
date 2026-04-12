package com.wenxi.neko_ai_agent.controller;

import com.wenxi.neko_ai_agent.common.ResultUtils;
import com.wenxi.neko_ai_agent.exception.BaseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HealthController {

    @GetMapping("/health")
    public BaseResponse<String> health(){
        return ResultUtils.success("ok");
    }
}
