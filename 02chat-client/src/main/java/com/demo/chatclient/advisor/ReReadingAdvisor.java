package com.demo.chatclient.advisor;


import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

public class ReReadingAdvisor implements BaseAdvisor {

    private static final String RE_READING = """
            {prompt}
            Read the question again:{prompt}
            """;

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String prompt = chatClientRequest.prompt().getContents();
        String newPrompt = PromptTemplate
                .builder()
                .template(RE_READING)
                .build()
                .render(Map.of("prompt", prompt));
        return ChatClientRequest.builder()
                .prompt(Prompt.builder().content(newPrompt).build())
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
