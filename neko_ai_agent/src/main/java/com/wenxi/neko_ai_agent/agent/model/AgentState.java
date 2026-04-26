package com.wenxi.neko_ai_agent.agent.model;

/**
 * 代理执行状态的枚举类
 */
public enum AgentState {

    /**
     * 空闲状态
     */
    IDLE,
    /**
     * 执行中状态
     */
    RUNNING,
    /**
     * 已完成状态
     */
    FINISHED,
    /**
     * 错误状态
     */
    ERROR

}
