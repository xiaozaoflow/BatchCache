package com.example.cache;

import com.example.cache.aspect.BatchCacheAspect;
import com.example.cache.service.BatchCacheClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 描述
 *
 * @author zhuwei
 * @Date 2020/10/27 PM4:00
 */
@EnableCaching
@Configuration
@ConditionalOnClass(RedisMultiCache.class)
public class RedisCacheConfig extends CachingConfigurerSupport {

    /**
     * 默认缓存存活时间(time to live), 2 分钟
     */
    private static final long DEFAULT_TTL_SECONDS = 2 * 60;

    public static final String BATCH_CACHE = "batch_cache";

    /*@Primary
    @Bean("dataCache")
    public RedisMultiCache dataCache(RedisTemplate redisTemplate, RedisCacheManager cacheManager) {
        // 这里为了方便演示，创建缓存时，清空已存在的缓存数据，
        return new RedisMultiCache(cacheManager.getCache(DATA_CACHE), redisTemplate, true);
    }*/

    @Bean("batchCache")
    public RedisMultiCache dataListCache(RedisTemplate redisTemplate, RedisCacheManager cacheManager) {
        return new RedisMultiCache(cacheManager.getCache(BATCH_CACHE)
            , redisTemplate
            , cacheManager.getCacheConfigurations().get(BATCH_CACHE)
            , true);
    }

    /******************************************     ⬇redis cache 配置⬇   ******************************************/

    /**
     * 配置 redisTemplate
     * 缓存数据的序列化方式 StringRedisSerializer.UTF_8
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        redisTemplate.setKeySerializer(StringRedisSerializer.UTF_8);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * redis 缓存管理器(可以添加自定义缓存配置)
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory);

        return RedisCacheManager.builder(redisCacheWriter)
            .withInitialCacheConfigurations(initCacheConfig())
            // 设置默认的缓存配置，默认过期时间 2分钟
            .cacheDefaults(redisCacheConfig(DEFAULT_TTL_SECONDS))
//            .transactionAware() // 是否支持spring事务
            .build();
    }

    /**
     * 如果需要指定某个缓存的个性化的配置（如过期时间），可以在map里面添加
     *
     * @return 初始化缓存配置
     */
    private Map<String, RedisCacheConfiguration> initCacheConfig() {
        Map<String, RedisCacheConfiguration> configurationMap = new HashMap<>(4);
        // 指定 BATCH_CACHE 的过期时间为 3分钟
        configurationMap.put(BATCH_CACHE, redisCacheConfig(3 * 60));
        return configurationMap;
    }

    /**
     * 设置 redis 缓存的配置
     * 主要设置缓存时间, 和序列化方式
     *
     * @param seconds 指定过期时间 单位：秒
     */
    private RedisCacheConfiguration redisCacheConfig(long seconds) {

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        return RedisCacheConfiguration.defaultCacheConfig()
            // TODO 注意 这里的 key 和 value 的 serialize 务必和 redisTemplate 保持一致，否则批量操作和单个操作使用不一样的序列化工具会导致读取缓存时报错
            .serializeKeysWith(RedisSerializationContext
                .SerializationPair
                .fromSerializer(StringRedisSerializer.UTF_8))
            .serializeValuesWith(RedisSerializationContext
                .SerializationPair
                .fromSerializer(serializer))
            .computePrefixWith(RedisCacheConfig::computeCachePrefix)
            .entryTtl(Duration.ofSeconds(seconds));
    }

    /**
     * 根据缓存名设置对应缓存的key的前缀，推荐使用 程序名+缓存名
     *
     * @param cacheName 缓存名（类似数据库表名）
     */
    public static String computeCachePrefix(String cacheName) {
        return "xiaozao-" + cacheName + "::";
    }

    @Bean
    @ConditionalOnMissingBean
    public BatchCacheAspect redisLockAspect(BatchCacheClient batchCacheClient) {
        return new BatchCacheAspect(batchCacheClient);
    }
}
