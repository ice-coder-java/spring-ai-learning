package com.demo.quickstart.controller;


import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekAssistantMessage;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("deepSeek")
public class DeepSeekController {
    @Autowired
    private DeepSeekChatModel deepSeekChatModel;

    @RequestMapping("chat")
    public String chat(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        return deepSeekChatModel.call(question);
    }

    @RequestMapping(value = "stream", produces = "text/stream;charset=UTF-8")
    public Flux<String> stream(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        return deepSeekChatModel.stream(question);
    }

    @RequestMapping(value = "deepSeekReasonerStreamExample", produces = "text/stream;charset=UTF-8")
    public Flux<String> deepSeekReasonerStreamExample(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        Prompt prompt = new Prompt(question, DeepSeekChatOptions.builder()
                .model("deepseek-reasoner")
                .build());
        return deepSeekChatModel.stream(prompt).map(response -> {
            DeepSeekAssistantMessage message = (DeepSeekAssistantMessage) response.getResult().getOutput();
            if (message.getReasoningContent() != null) {
                return "reasoning:" + message.getReasoningContent();
            }
            return "content:" + message.getText();
        });
    }
}