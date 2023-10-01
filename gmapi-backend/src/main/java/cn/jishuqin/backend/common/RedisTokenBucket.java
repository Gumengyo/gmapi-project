package cn.jishuqin.backend.common;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 删除请求
 *
 * @author gumeng
 */
@Component
public class RedisTokenBucket {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private final long EXPIRE_TIME = 400; // 400秒后过期

    /**
     * 令牌桶算法，一分钟以内，每个手机号只能发送一次
     *
     * @param phoneNum
     * @return
     */
    public boolean tryAcquire(String phoneNum) {
        // 每个手机号码一分钟内只能发送一条短信
        int permitsPerMinute = 1;
        // 令牌桶容量
        int maxPermits = 1;
        // 获取当前时间戳
        long now = System.currentTimeMillis();

        // 使用分布式锁来确保并发安全性
        String lockKey = phoneNum + "_lock";
        boolean locked = false;
        try {
            // 尝试获取分布式锁，设置过期时间为 1 秒
            locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 1, TimeUnit.SECONDS);
            if (locked) {
                // 获取锁成功，执行令牌桶算法逻辑
                // 计算令牌桶内令牌数
                int tokens = Integer.parseInt(redisTemplate.opsForValue().get(phoneNum + "_tokens") == null ? "0" : redisTemplate.opsForValue().get(phoneNum + "_tokens"));
                // 计算令牌桶上次填充的时间戳
                long lastRefillTime = Long.parseLong(redisTemplate.opsForValue().get(phoneNum + "_last_refill_time") == null ? "0" : redisTemplate.opsForValue().get(phoneNum + "_last_refill_time"));
                // 计算当前时间与上次填充时间的时间差
                long timeSinceLast = now - lastRefillTime;
                // 计算需要填充的令牌数
                int refill = (int) (timeSinceLast / 1000 * permitsPerMinute / 60);
                // 更新令牌桶内令牌数
                tokens = Math.min(refill + tokens, maxPermits);
                // 更新上次填充时间戳
                redisTemplate.opsForValue().set(phoneNum + "_last_refill_time", String.valueOf(now), EXPIRE_TIME, TimeUnit.SECONDS);
                // 如果令牌数大于等于1，则获取令牌
                if (tokens >= 1) {
                    tokens--;
                    redisTemplate.opsForValue().set(phoneNum + "_tokens", String.valueOf(tokens), EXPIRE_TIME, TimeUnit.SECONDS);
                    // 如果获取到令牌，则返回true
                    return true;
                }
            }
        } finally {
            // 释放分布式锁
            if (locked) {
                redisTemplate.delete(lockKey);
            }
        }
        // 如果没有获取到令牌或获取锁失败，则返回false
        return false;
    }
}

