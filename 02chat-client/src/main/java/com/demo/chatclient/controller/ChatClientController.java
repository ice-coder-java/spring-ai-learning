package com.demo.chatclient.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.demo.chatclient.advisor.ReReadingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("chatClient")
public class ChatClientController {

    @Autowired
    private DashScopeChatModel dashScopeChatModel;

    @RequestMapping("chat")
    public String chat(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel).build();
        return chatClient.prompt().user(question).call().content();
    }

    @RequestMapping(value = "stream", produces = "text/stream;charset=UTF-8")
    public Flux<String> stream(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        ChatClient chatClient = ChatClient.builder(dashScopeChatModel).build();
        Flux<String> result = chatClient.prompt().user(question).stream().chatResponse()
                .mapNotNull(chatResponse -> chatResponse.getResult().getOutput().getText());
        return result;
    }

    @Value("classpath:/prompt.txt")
    Resource prompt;

    @RequestMapping("systemPrompt")
    public String systemPrompt(@RequestParam(value = "question", defaultValue = "你好，你是谁") String question,@RequestParam (value = "name", defaultValue = "小王") String name) {
        ChatClient chatClient = ChatClient
                .builder(dashScopeChatModel)
                .defaultSystem(prompt)
                .build();
        return chatClient.prompt()
                .system(p->p.param("name", name))
                .user(question)
                .call().content();
    }

    @RequestMapping("advisor")
    public String advisor(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        ChatClient chatClient = ChatClient
                .builder(dashScopeChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(),new SafeGuardAdvisor(List.of("你")))
                .build();
        return chatClient
                .prompt()
                .user(question)
//                .advisors()
                .call()
                .content();
    }
    @RequestMapping("myAdvisor")
    public String myAdvisor(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        ChatClient chatClient = ChatClient
                .builder(dashScopeChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(),new ReReadingAdvisor())
                .build();
        return chatClient
                .prompt()
                .user(question)
//                .advisors()
                .call()
                .content();
    }
}
