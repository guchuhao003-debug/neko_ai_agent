package com.wenxi.neko_ai_agent.rag;


import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.stereotype.Component;

/**
 * 创建上下文查询增强器的工厂
 */
@Component
public class LoveAppContextualQueryAugmenterFactory {

    /**
     * 创建上下文查询增强器实例
     * @return
     */
    public static ContextualQueryAugmenter createInstance() {
        // 自定义当 AI 查询不到相关内容时所返回的内容
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate(
                """
                        你应该输出以下的内容：
                        抱歉，我只能回答恋爱相关的问题，别的没办法帮到您哦，
                        有问题可以联系作者: 935070021@qq.com
                        """
        );
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                .build();
    }
}
