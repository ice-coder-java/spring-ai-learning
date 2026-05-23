package com.demo.quickstart.controller;

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioSpeechApi;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechOptions;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeApiSpec;
import org.springframework.http.MediaType;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.tts.TextToSpeechOptions;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("qwen")
public class QwenController {
    @Autowired
    private DashScopeChatModel dashScopeChatModel;

    @RequestMapping("chat")
    public String chat(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        ChatOptions options = ChatOptions.builder()
//                .model("qwen3.6-plus")
                .build();
        Prompt prompt = new Prompt(question,options);
        return dashScopeChatModel.call(prompt).getResult().getOutput().getText();
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

    @Autowired
    private DashScopeAudioSpeechModel dashScopeAudioSpeechModel;

    /*
    TODO，无法播放
     */
    @RequestMapping(value = "speech", produces = "audio/mpeg")
    public ResponseEntity<byte[]> speech(@RequestParam(value = "prompt", defaultValue = "你好，你是谁") String prompt) throws IOException {
        TextToSpeechOptions textToSpeechOptions = TextToSpeechOptions.builder()
                .model("sambert-zhinan-v1")
                .build();
        TextToSpeechPrompt textToSpeechPrompt = new TextToSpeechPrompt(prompt,textToSpeechOptions);
        byte[] output = dashScopeAudioSpeechModel.call(textToSpeechPrompt).getResult().getOutput();
        System.out.println("output.length=" + output.length);
        System.out.println("output=" + output);
        Path filePath = Paths.get("speech_" + System.currentTimeMillis() + ".mp3");
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(output);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header("Content-Disposition", "attachment; filename=\"speech.mp3\"")
                .body(output);
    }

    @Autowired
    private DashScopeAudioTranscriptionModel dashScopeAudioTranscriptionModel;
    private static final String AUDIO_RESOURCES_URL = "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav";

    @RequestMapping("transcription")
    public String transcription(@RequestParam(value = "file", defaultValue = "") String file) throws MalformedURLException {
        return dashScopeAudioTranscriptionModel.call(new AudioTranscriptionPrompt(new UrlResource(AUDIO_RESOURCES_URL))).getResult().getOutput();
    }

    @RequestMapping("multimodal")
    public String multimodal(@RequestParam(value = "question", defaultValue = "图中是什么内容") String question) {
        Media media = new Media(MimeTypeUtils.IMAGE_PNG, new ClassPathResource("image.png"));
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .model("qwen-vl-max")
                .multiModel( true)
                .build();
        Prompt prompt = Prompt.builder()
                .chatOptions( options)
                .messages(UserMessage.builder()
                        .text(question)
                        .media(media)
                        .build()
                )
                .build();
        return dashScopeChatModel.call(prompt).getResult().getOutput().getText();
    }
}
