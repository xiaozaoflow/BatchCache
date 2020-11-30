package com.example.cache.service;

import org.aspectj.lang.reflect.MethodSignature;

import java.util.List;
import java.util.Map;

/**
 * 批量缓存客户端接口
 *
 * @author zhuwei
 * @Date 2020/11/26 PM7:33
 */
public interface BatchCacheClient {

    /**
     * 从redis中加载缓存
     * <p>
     * 1 缓存中存在的数据，取缓存
     * 2 不存在，访问方法获取
     * </p>
     *
     * @param keyPrefix       key前缀
     * @param keyList         key列表
     * @param methodSignature 方法上下文
     * @param voClass         返回数据的类型
     * @param keyMapper
     * @return
     */
    Map<Object, Object> loadCache(String keyPrefix, List<Object> keyList, MethodSignature methodSignature
        , Class voClass, String keyMapper, Long seconds);
}
