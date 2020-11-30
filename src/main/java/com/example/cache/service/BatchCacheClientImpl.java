package com.example.cache.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.cache.RedisMultiCache;
import lombok.AllArgsConstructor;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 描述
 *
 * @author zhuwei
 * @Date 2020/11/26 PM8:05
 */
@Service
@AllArgsConstructor
public class BatchCacheClientImpl implements BatchCacheClient {

    private final RedisMultiCache redisMultiCache;

    @Override
    public Map<Object, Object> loadCache(String keyPrefix, List<Object> keyList, MethodSignature methodSignature,
                                         Class voClass, String keyMapper, Long seconds) {
        // 1 从redis中获取缓存
        List<String> cacheKeyList = keyList.stream().map(key -> this.generatetKey(keyPrefix, key)).collect(Collectors.toList());
        List<Object> cacheHitList = redisMultiCache.list(cacheKeyList);

        // 2 过滤出缓存不存在的keys
        int size = keyList.size();
        List<Object> missKeyList = new ArrayList<>(size);
        List<Object> cacheValueList = new ArrayList<>(size);

        if (CollectionUtils.isEmpty(cacheHitList)) {
            missKeyList.addAll(keyList);
        } else {
            int cacheHitSize = cacheHitList.size();
            for (int i = 0; i < cacheHitSize; i++) {
                Object o = cacheHitList.get(i);

                // 如果查询的缓存为空，则添加key到missKeyList中
                if (ObjectUtils.isEmpty(o)) {
                    missKeyList.add(keyList.get(i));
                } else {
                    // 处理redis中存储对象 JsonObject 或 JsonArray
                    if (o instanceof JSONArray) {
                        cacheValueList.addAll(((JSONArray) o).toJavaList(voClass));
                    } else if (o instanceof JSONObject) {
                        cacheValueList.add(JSONObject.toJavaObject((JSONObject) o, voClass));
                    } else {
                        cacheValueList.add(o);
                    }
                }
            }
        }

        // 3 重新查询不存在缓存的keys，重新放回redis
        if (!CollectionUtils.isEmpty(missKeyList)) {
            Map missValueMap = this.loadValueForMissKeys(voClass, methodSignature, missKeyList);
            redisMultiCache.putBatch(this.convertKeyForResult(keyPrefix, missValueMap), seconds);
            cacheValueList.addAll(missValueMap.values());
        }

        // 4 List转Map
        return this.convertListToMap(keyMapper, cacheValueList);
    }

    /**
     * 转换缓存成Map形式
     *
     * @param keyMapper
     * @param cacheValueList
     * @return
     */
    private Map<Object, Object> convertListToMap(String keyMapper, List<Object> cacheValueList) {
        if (cacheValueList == null || cacheValueList.size() == 0) {
            return new HashMap<>();
        }

        Class targetClass = cacheValueList.get(0).getClass();
        return cacheValueList
            .stream()
            .collect(Collectors.toMap(new Function<Object, Object>() {
                @Override
                public Object apply(Object v) {
                    try {
                        Field[] fields = targetClass.getDeclaredFields();
                        for (Field field : fields) {
                            if (field.getName().equals(keyMapper)) {

                                PropertyDescriptor pd = new PropertyDescriptor(field.getName(), targetClass);
                                Method getMethod = pd.getReadMethod();
                                return getMethod.invoke(v);
                            }
                        }
                    } catch (Exception e) {

                    }
                    return null;
                }
            }, Function.identity()));

    }

    /**
     * 转换map的key，成rediskey
     *
     * @param keyPrefix
     * @param resultMap
     * @return
     */
    private Map<String, Object> convertKeyForResult(String keyPrefix, Map<Object, Object> resultMap) {
        Map<String, Object> newResultMap = new HashMap<>();
        resultMap.forEach((key, value) -> {
            newResultMap.put(this.generatetKey(keyPrefix, key), value);
        });
        return newResultMap;
    }

    /**
     * 生成key
     *
     * @param keyPrefix key前缀
     * @param param
     * @return
     */
    private String generatetKey(String keyPrefix, Object param) {
        Objects.requireNonNull(param, "generatetKey Parameters required not null");
        return keyPrefix + ":" + param.toString();
    }

    /**
     * 调用原方法加载没有缓存的keyList
     *
     * @param tartgetClass
     * @param missKeyList
     * @return
     */
    private Map<Object, Object> loadValueForMissKeys(Class tartgetClass, MethodSignature methodSignature, List<Object> missKeyList) {
        try {
            Method abstructMethod = methodSignature.getMethod();
            Method targetMethod = tartgetClass.getMethod(abstructMethod.getName(), abstructMethod.getParameterTypes());
            Object object = targetMethod.invoke(tartgetClass.newInstance(), missKeyList);
            if (ObjectUtils.isEmpty(object)) {
                // TODO: 2020-10-28
            }
            return (Map<Object, Object>) object;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return null;
    }
}
