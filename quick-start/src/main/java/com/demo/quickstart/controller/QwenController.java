package com.demo.quickstart.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeApiSpec;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("qwen")
public class QwenController {
    @Autowired
    private DashScopeChatModel dashScopeChatModel;

    @RequestMapping("chat")
    public String chat(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        return dashScopeChatModel.call(question);
    }

    @RequestMapping(value = "stream", produces = "text/stream;charset=UTF-8")
    public Flux<String> stream(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        Prompt prompt = new Prompt(question);
        Flux<ChatResponse> stream = dashScopeChatModel.stream(prompt);
        return Flux.create(sink ->
                stream.subscribe(chatResponse ->
                        sink.next(String.valueOf(chatResponse.getResult().getOutput().getText()))));
    }

    @Autowired
    private DashScopeImageModel dashScopeImageModel;

    @RequestMapping("image")
    public String image(@RequestParam(value = "prompt", defaultValue = "一个笑脸") String prompt) {
        DashScopeImageOptions imageOptions = DashScopeImageOptions.builder()
                .withModel("wanx2.1-t2i-turbo").build();
        ImagePrompt imagePrompt = new ImagePrompt(prompt, imageOptions);
        ImageResponse response = dashScopeImageModel.call(imagePrompt);
        return response.getResults().get(0).getOutput().getUrl();
    }
}
