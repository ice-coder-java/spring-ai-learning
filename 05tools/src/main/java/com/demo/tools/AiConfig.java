package com.demo.tools;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatProperties;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    public ChatClient ChatClient(DashScopeChatModel chatModel,
                                 DashScopeChatProperties options,
                                 ChatMemory chatMemory,
                                 ToolsService toolsService) {

        DashScopeChatOptions dashScopeChatOptions = DashScopeChatOptions.fromOptions(options.getOptions());
        dashScopeChatOptions.setTemperature(1.2);
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是XS航空智能客服代理， 请以友好的语气服务用户。
                        """)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .defaultTools(toolsService)
                .defaultOptions(dashScopeChatOptions)
                .build();
    }

}