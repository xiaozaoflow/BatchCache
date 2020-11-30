package com.example.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * redis缓存 操作类
 *
 * @author zhuwei
 * @Date 2020/10/27 PM3:54
 */
@Slf4j
public class RedisMultiCache implements Cache {

    /**
     * spring redis cache,
     */
    private Cache cache;

    /**
     * 个性化定制缓存配置
     */
    private RedisCacheConfiguration cacheConfig;

    private RedisTemplate redisTemplate;

    /**
     * 默认不清除原有缓存
     */
    public RedisMultiCache(Cache cache, RedisCacheConfiguration cacheConfig, RedisTemplate redisTemplate) {
        this(cache, redisTemplate, cacheConfig, false);
    }

    /**
     * @param cache         spring cache
     * @param redisTemplate 用于进行缓存的批量操作
     * @param clearExist    是否初始化缓存（清除redis中已经存在的缓存数据）
     */
    public RedisMultiCache(Cache cache, RedisTemplate redisTemplate, RedisCacheConfiguration cacheConfig, boolean clearExist) {
        this.cache = cache;
        this.redisTemplate = redisTemplate;
        this.cacheConfig = cacheConfig;
        // 创建缓存前, 是否初始化缓存，清除原有的
        if (clearExist) {
            cache.clear();
        }
    }

    @Override
    public String getName() {
        return this.cache.getName();
    }

    @Override
    public Object getNativeCache() {
        return cache.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        try {
            return cache.get(key);
        } catch (Exception e) {
            log.error("RedisMultiCache 异常", e);
        }
        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        try {
            return cache.get(key, type);
        } catch (Exception e) {
            log.error("RedisMultiCache 异常", e);
        }
        return null;
    }

    /**
     * 如果缓存中存储的为null，则直接返回null
     * 如果 valueLoader 执行时抛出异常，则使用 RuntimeException 继续抛出，调用方自己处理
     *
     * @return {@code null} if cached null
     */
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        T value;
        try {
            value = cache.get(key, valueLoader);
        } catch (Exception e) {
            log.error("RedisMultiCache 异常", e);
            try {
                return valueLoader.call();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return value;
    }

    @Override
    public void put(Object key, Object value) {
        try {
            cache.put(key, value);
        } catch (Exception e) {
            log.error("RedisMultiCache 异常", e);
        }
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        try {
            return cache.putIfAbsent(key, value);
        } catch (Exception e) {
            log.error("RedisMultiCache 异常", e);
        }
        return null;
    }

    @Override
    public void evict(Object key) {
        try {
            cache.evict(key);
        } catch (Exception e) {
            log.error("RedisMultiCache 异常", e);
        }
    }

    @Override
    public void clear() {
        try {
            cache.clear();
        } catch (Exception e) {
            log.error("RedisMultiCache 异常", e);
        }
    }

    /**
     * 批量读取缓存，默认 key-value 为一对一的关系
     *
     * @param keyList key列表
     * @param <K>     key的类型
     * @param <V>     value的类型
     * @return
     */
    public <K, V> List<V> list(List<K> keyList) {
        Objects.requireNonNull(redisTemplate, "redisTemplate required not null");

        List<V> cacheHitList = Collections.emptyList();
        try {
            cacheHitList = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {

                // 循环处理key
                RedisSerializer keySerializer = redisTemplate.getKeySerializer();
                for (K k : keyList) {

                    // 序列化key
                    byte[] key = keySerializer.serialize(k);
                    if (key != null) {

                        // 获取value
                        connection.get(key);
                    } else {
                        log.warn("RedisMultiCache 批量操作序列化失败， key={}", k);
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("RedisMultiCache 异常", e);
        }
        return cacheHitList;
    }

    /**
     * 批量存入缓存
     *
     * @param map 需要存入的数据
     * @param <K> 数据的 key 的类型
     * @param <V> 数据的 value 的类型
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <K, V> void putBatch(Map<K, V> map, Long seconds) {
        if (CollectionUtils.isEmpty(map)) {
            return;
        }
        Objects.requireNonNull(redisTemplate, "redisTemplate required not null");
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {

                RedisSerializer keySerializer = redisTemplate.getKeySerializer();
                RedisSerializer valueSerializer = redisTemplate.getValueSerializer();

                for (Map.Entry<K, V> entry : map.entrySet()) {
                    byte[] key = keySerializer.serialize(entry.getKey());
                    byte[] value = valueSerializer.serialize(entry.getValue());

                    if (key != null && value != null) {
                        connection.setEx(key, seconds, value);
                    } else {
                        log.warn("RedisMultiCache 批量操作序列化失败， entry={}", entry);
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("RedisMultiCache 异常", e);
        }
    }

    /**
     * 获取不为空的缓存，如果缓存中存在为null，则使用 valueLoader 重新加载，并将结果存入缓存中
     *
     * @param key         缓存数据的key
     * @param valueLoader 加载缓存的方法
     * @param <T>         缓存数据类型
     * @return {@code null} if valueLoader returned null
     */
    public <T> T getNonNull(Object key, Callable<T> valueLoader) {
        T value;
        try {
            ValueWrapper wrapper = cache.get(key);
            if (wrapper == null || (value = (T) wrapper.get()) == null) {
                value = valueLoader.call();
                cache.put(key, value);
            }
        } catch (Exception e) {
            log.error("RedisMultiCache 异常", e);
            try {
                return valueLoader.call();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return value;
    }
}
