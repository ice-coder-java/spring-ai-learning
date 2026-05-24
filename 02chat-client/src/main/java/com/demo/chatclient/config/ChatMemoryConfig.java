package com.demo.chatclient.config;

import com.alibaba.cloud.ai.memory.redis.RedissonRedisChatMemoryRepository;
import com.alibaba.cloud.ai.memory.redis.builder.RedisChatMemoryBuilder;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {
    //    @Bean
    ChatMemory chatMemory(JdbcChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory
                .builder()
                .maxMessages(2)
                .chatMemoryRepository(chatMemoryRepository).build();
    }

    @Value("${spring.ai.memory.redis.host}")
    private String redisHost;
    @Value("${spring.ai.memory.redis.port}")
    private int redisPort;
    @Value("${spring.ai.memory.redis.password}")
    private String redisPassword;
    @Value("${spring.ai.memory.redis.timeout}")
    private int redisTimeout;

    @Bean
    public RedissonRedisChatMemoryRepository redisChatMemoryRepository() {
        return RedissonRedisChatMemoryRepository.builder()
                .host(redisHost)
                .port(redisPort)
                // 若没有设置密码则注释该项
//           .password(redisPassword)
                .timeout(redisTimeout)
                .build();
    }

    @Bean
    ChatMemory redisChatMemory(RedissonRedisChatMemoryRepository redisChatMemoryRepository) {
        return MessageWindowChatMemory
                .builder()
                .maxMessages(2)
                .chatMemoryRepository(redisChatMemoryRepository).build();
    }
}
