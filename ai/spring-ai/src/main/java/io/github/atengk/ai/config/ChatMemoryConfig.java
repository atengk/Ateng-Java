package io.github.atengk.ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPooled;

import java.time.Duration;

@Configuration
public class ChatMemoryConfig {

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled("175.178.193.128", 20003, null, "Admin@123");
    }

    @Bean
    public ChatMemory chatMemory(JedisPooled jedisPooled) {
        ChatMemoryRepository chatMemoryRepository = RedisChatMemoryRepository.builder()
                .jedisClient(jedisPooled)
                .indexName("my-chat-index")
                .keyPrefix("my-chat:")
                .timeToLive(Duration.ofHours(24))
                .build();

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)
                .build();

        return chatMemory;
    }

}
