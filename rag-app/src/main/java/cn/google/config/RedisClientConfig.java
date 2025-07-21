package cn.google.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis客户端配置类
 * 
 * 使用Redisson作为Redis客户端，提供分布式锁、集合操作等功能。
 * 主要用于存储知识库标签列表和缓存数据。
 *
 */
@Configuration
@EnableConfigurationProperties(RedisClientConfigProperties.class)
public class RedisClientConfig {

    /**
     * 配置Redisson客户端
     * 
     * 创建Redisson客户端实例，配置连接池、重试策略等参数。
     * 使用JSON编解码器进行数据序列化。
     * 
     * @param applicationContext Spring应用上下文
     * @param properties Redis配置属性
     * @return RedissonClient实例
     */
    @Bean("redissonClient")
    public RedissonClient redissonClient(ConfigurableApplicationContext applicationContext, RedisClientConfigProperties properties) {
        Config config = new Config();
        // 设置JSON编解码器，用于数据序列化
        // 参考：https://github.com/redisson/redisson/wiki/4.-%E6%95%B0%E6%8D%AE%E5%BA%8F%E5%88%97%E5%8C%96
        config.setCodec(JsonJacksonCodec.INSTANCE);

        // 配置单机Redis服务器连接
        config.useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
//                .setPassword(properties.getPassword())  // 如果需要密码认证，取消注释
                .setConnectionPoolSize(properties.getPoolSize())
                .setConnectionMinimumIdleSize(properties.getMinIdleSize())
                .setIdleConnectionTimeout(properties.getIdleTimeout())
                .setConnectTimeout(properties.getConnectTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval())
                .setPingConnectionInterval(properties.getPingInterval())
                .setKeepAlive(properties.isKeepAlive())
        ;

        return Redisson.create(config);
    }

}
