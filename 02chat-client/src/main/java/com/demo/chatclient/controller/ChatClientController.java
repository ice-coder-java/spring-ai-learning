package com.demo.chatclient.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.demo.chatclient.advisor.ReReadingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
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
    public String systemPrompt(@RequestParam(value = "question", defaultValue = "你好，你是谁") String question, @RequestParam(value = "name", defaultValue = "小王") String name) {
        ChatClient chatClient = ChatClient
                .builder(dashScopeChatModel)
                .defaultSystem(prompt)
                .build();
        return chatClient.prompt()
                .system(p -> p.param("name", name))
                .user(question)
                .call().content();
    }

    @RequestMapping("advisor")
    public String advisor(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        ChatClient chatClient = ChatClient
                .builder(dashScopeChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(), new SafeGuardAdvisor(List.of("你")))
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
                .defaultAdvisors(new SimpleLoggerAdvisor(), new ReReadingAdvisor())
                .build();
        return chatClient
                .prompt()
                .user(question)
//                .advisors()
                .call()
                .content();
    }

    @Autowired
    ChatMemory chatMemory;

    @RequestMapping("memoryAdvisor")
    public String memoryAdvisor(@RequestParam(value = "question", defaultValue = "你是谁") String question) {
        ChatClient chatClient = ChatClient
                .builder(dashScopeChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(),
//                        PromptChatMemoryAdvisor.builder(chatMemory).build())
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        return chatClient
                .prompt()
                .user(question)
//                .advisors()
                .call()
                .content();
    }

//    @Autowired
//    ChatMemoryRepository chatMemoryRepository;

//    @Bean
//    public ChatMemory chatMemory() {
//        return MessageWindowChatMemory
//                .builder()
//                .maxMessages(10)
//                .chatMemoryRepository(chatMemoryRepository)
//                .build();
//    }

    @RequestMapping("UniqueMemoryAdvisor")
    public String UniqueMemoryAdvisor(@RequestParam(value = "question", defaultValue = "你是谁") String question, @RequestParam(value = "conversationId", defaultValue = "1") String conversationId) {
        ChatClient chatClient = ChatClient
                .builder(dashScopeChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        PromptChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        return chatClient
                .prompt()
                .user(question)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
//                .advisors()
                .call()
                .content();
    }

    @Autowired
    ChatMemory redisChatMemory;

    @RequestMapping("redisUniqueMemoryAdvisor")
    public String redisUniqueMemoryAdvisor(@RequestParam(value = "question", defaultValue = "你是谁") String question, @RequestParam(value = "conversationId", defaultValue = "1") String conversationId) {
        ChatClient chatClient = ChatClient
                .builder(dashScopeChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor(),
                        PromptChatMemoryAdvisor.builder(redisChatMemory).build())
                .build();
        return chatClient
                .prompt()
                .user(question)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationId))
//                .advisors()
                .call()
                .content();
    }

    @RequestMapping("BoolOut")
    public String BoolOut(@RequestParam(value = "question", defaultValue = "你们家的快递迟迟不到,我要退货") String question) {
        ChatClient chatClient = ChatClient
                .builder(dashScopeChatModel)
                .defaultSystem("""
                        请判断用户信息是否表达了投诉意图?
                        能用 true 或 false 回答，不要输出多余内容
                        """)
                .build();
        Boolean entity = chatClient.
                prompt()
                .user(question)
                .call()
                .entity(Boolean.class);
        if (entity) {
            return "请将用户信息反馈给客服";
        } else {
            return "请将用户信息反馈给销售";
        }
    }

    public record Address(
            String name,        // 收件人姓名
            String phone,       // 联系电话
            String province,    // 省
            String city,        // 市
            String district,    // 区/县
            String detail       // 详细地址
    ) {}

    @RequestMapping("address")
    public String address(@RequestParam(value = "question", defaultValue = "收货人：张三，电话13588888888，地址：浙江省杭州市西湖区文一西路100号8幢202室") String question) {
        ChatClient chatClient = ChatClient
                .builder(dashScopeChatModel)
                .defaultSystem("""
                        请从下面这条文本中提取收货信息
                        """)
                .build();
        Address entity = chatClient.
                prompt()
                .user(question)
                .call()
                .entity(Address.class);
        return entity.toString();
    }

}
