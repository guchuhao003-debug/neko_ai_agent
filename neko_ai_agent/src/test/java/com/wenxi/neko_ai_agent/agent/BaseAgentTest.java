package com.wenxi.neko_ai_agent.agent;

import com.wenxi.neko_ai_agent.agent.model.AgentState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 基础智能体状态机单元测试。
 */
class BaseAgentTest {

    /**
     * 终止工具执行后不应继续进入下一步。
     */
    @Test
    void runShouldStopImmediatelyAfterTerminalStep() {
        TerminalAgent agent = new TerminalAgent();
        agent.setMaxSteps(5);

        String result = agent.run("测试终止");

        assertEquals(2, agent.stepCount);
        assertTrue(result.contains("Step 1"));
        assertFalse(result.contains("Step 2"));
        assertFalse(result.contains("Step 3"));
    }

    /**
     * 测试用智能体。
     */
    private static class TerminalAgent extends BaseAgent {

        private int stepCount = 0;

        @Override
        public String step() {
            stepCount++;
            if (stepCount == 2) {
                setState(AgentState.FINISHED);
                return "任务结束";
            }
            return "工具返回的中间结果";
        }
    }
}
