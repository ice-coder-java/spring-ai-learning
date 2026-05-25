package com.demo.structured;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;

@RestController
public class MultiModelsController {

    private final ConcurrentHashMap<String, AiJob.BookingInfo> historyMap = new ConcurrentHashMap<>();

    @Autowired
    ChatClient planningChatClient;

    @Autowired
    ChatClient botChatClient;


    @GetMapping(value = "/stream", produces = "text/stream;charset=UTF8")
    Flux<String> stream(@RequestParam String message, @RequestParam String conversationId) {
        // 创建一个用于接收多条消息的 Sink
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        // 推送消息
        sink.tryEmitNext("正在计划任务...<br/>");

        new Thread(() -> {
            // 获取历史存储，注入 prompt 上下文
            AiJob.BookingInfo stored = historyMap.get(conversationId);
            String userMessage = message;
            if (stored != null) {
                if (stored.name() != null && !stored.name().isBlank()) {
                    userMessage += "\n已知姓名：" + stored.name();
                }
                if (stored.bookingNumber() != null && !stored.bookingNumber().isBlank()) {
                    userMessage += "\n已知预定号：" + stored.bookingNumber();
                }
            }

            AiJob.Job job = planningChatClient
                    .prompt()
                    .user(userMessage)
                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .call()
                    .entity(AiJob.Job.class);
            System.out.println(conversationId + job);

            // 用历史存储补全 AI 未提取到的字段
            AiJob.BookingInfo aiInfo = job.keyInfos();
            String name = (aiInfo == null || aiInfo.name() == null || aiInfo.name().isBlank())
                    ? (stored != null ? stored.name() : null) : aiInfo.name();
            String bookingNumber = (aiInfo == null || aiInfo.bookingNumber() == null || aiInfo.bookingNumber().isBlank())
                    ? (stored != null ? stored.bookingNumber() : null) : aiInfo.bookingNumber();
            AiJob.BookingInfo merged = new AiJob.BookingInfo(name, bookingNumber);
            job = new AiJob.Job(job.jobType(), merged);
            if (name != null || bookingNumber != null) {
                historyMap.put(conversationId, merged);
            }

            switch (job.jobType()) {
                case CANCEL -> {
                    boolean missingName = job.keyInfos() == null
                            || job.keyInfos().name() == null || job.keyInfos().name().isBlank();
                    boolean missingBooking = job.keyInfos() == null
                            || job.keyInfos().bookingNumber() == null || job.keyInfos().bookingNumber().isBlank();
                    if (missingName || missingBooking) {
                        String missing = "";
                        if (missingName && missingBooking) {
                            missing = "姓名和订单号";
                        } else if (missingName) {
                            missing = "姓名";
                        } else {
                            missing = "订单号";
                        }
                        Flux<String> content = botChatClient.prompt()
                                .user("用户正在进行退票操作，缺少信息：" + missing + "。请友好地提醒用户补充。")
                                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                                .stream().content();
                        content.doOnNext(sink::tryEmitNext)
                                .doOnComplete(() -> sink.tryEmitComplete())
                                .subscribe();
                    } else {
                        sink.tryEmitNext("退票成功!");
                        historyMap.remove(conversationId);
                    }
                }
                case QUERY -> {
                    boolean missingBooking = job.keyInfos() == null
                            || job.keyInfos().bookingNumber() == null || job.keyInfos().bookingNumber().isBlank();
                    if (missingBooking) {
                        Flux<String> content = botChatClient.prompt()
                                .user("用户查询票务但尚未提供预定号，请友好地提示用户提供预定号。")
                                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                                .stream().content();
                        content.doOnNext(sink::tryEmitNext)
                                .doOnComplete(() -> sink.tryEmitComplete())
                                .subscribe();
                    } else {
                        sink.tryEmitNext("查询预定信息：" + job.keyInfos().bookingNumber());
                    }
                }
                case OTHER -> {
                    Flux<String> content = botChatClient.prompt()
                            .user(message)
                            .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                            .stream().content();
                    content.doOnNext(sink::tryEmitNext)
                            .doOnComplete(() -> sink.tryEmitComplete())
                            .subscribe();
                }
                default -> {
                    Flux<String> content = botChatClient.prompt()
                            .user("无法理解用户的意图，请友好地告知用户当前只能处理退票和查询票务。")
                            .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
                            .stream().content();
                    content.doOnNext(sink::tryEmitNext)
                            .doOnComplete(() -> sink.tryEmitComplete())
                            .subscribe();
                }
            }
        }).start();

        return sink.asFlux();
    }
}