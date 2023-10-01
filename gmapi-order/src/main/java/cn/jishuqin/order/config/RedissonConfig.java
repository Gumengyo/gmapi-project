package cn.jishuqin.order.config;

import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 顾梦
 * @description Redisson 配置
 * @since 2023/7/28
 */
@Configuration
@Data
public class RedissonConfig {

    @Value("${spring.redis.host}")
    String redisHost;

    @Value("${spring.redis.port}")
    String redisPort;

    @Value("${spring.redis.password}")
    String redisPassword;

    @Bean
    public RedissonClient redissonClient(){
        // 1. 创建配置
        Config config = new Config();
        String redisAddress = String.format("redis://%s:%s",redisHost,redisPort);
        config.useSingleServer()
                .setAddress(redisAddress)
                .setDatabase(1)
                .setPassword(redisPassword);


        // 2. 创建 redisson 实例
        return Redisson.create(config);
    }
}
