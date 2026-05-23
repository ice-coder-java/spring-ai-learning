package com.demo.platformdemo.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;

@RestController
@RequestMapping("platform")
public class PlatformController {

    HashMap<String, ChatModel>  platforms = new HashMap<>();

    public PlatformController(
            DashScopeChatModel dashScopeChatModel,
            DeepSeekChatModel deepSeekChatModel
    ) {
        platforms.put("dashScope", dashScopeChatModel);
        platforms.put("deepSeek", deepSeekChatModel);
    }

    @RequestMapping(value = "chat", produces = "text/stream;charset=UTF-8")
    public Flux<String> chat(String  question, ChatRequest  request) {
        ChatModel platform = platforms.get(request.platform());
        ChatClient chatClient = ChatClient.builder(platform).defaultOptions(
                ChatOptions.builder()
                        .model(request.model())
                        .temperature(request.temperature())
                        .build()
        ).build();
        return chatClient.prompt().user(question).stream().content();
    }
}

record ChatRequest(String platform,String model,Double  temperature) {}
