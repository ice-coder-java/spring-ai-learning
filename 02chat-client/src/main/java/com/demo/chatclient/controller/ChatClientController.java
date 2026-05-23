package com.demo.chatclient.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("chatClient")
public class ChatClientController {

    @Autowired
    private DashScopeChatModel dashScopeChatModel;

    @RequestMapping("chat")
    public String chat(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel).build();
       return chatClient.prompt().user( question).call().content();
    }

    @RequestMapping(value = "stream", produces = "text/stream;charset=UTF-8")
    public Flux<String> stream(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel).build();
        Flux<String> result = chatClient.prompt().user(question).stream().chatResponse()
                .mapNotNull(chatResponse -> chatResponse.getResult().getOutput().getText());
        return result;
    }
}
