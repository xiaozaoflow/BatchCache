package com.example.cache.aspect;

import com.example.cache.annotation.BatchCache;
import com.example.cache.service.BatchCacheClient;
import lombok.AllArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 切面
 *
 * @author zhuwei
 * @Date 2020/10/27 PM5:11
 */
@Aspect
@Order(2)
@AllArgsConstructor
public class BatchCacheAspect {

    private final BatchCacheClient batchCacheClient;

    @Around("@annotation(batchCache)")
    public Object around(ProceedingJoinPoint joinPoint, BatchCache batchCache) {
        Object[] args = joinPoint.getArgs();
        Signature signature = joinPoint.getSignature();

        String key = batchCache.key();
        String keyMapper = batchCache.keyMapper();
        String keyPrefix = batchCache.value();
        Long seconds = batchCache.seconds();

        // 1 校验（注解只能应用于方法）
        if (!(signature instanceof MethodSignature)) {
            throw new IllegalStateException("@BatchCache can only apply to method");
        }

        // 1 校验（key的参数类型只能是Collection）
        MethodSignature methodSignature = (MethodSignature) signature;
        Object parameter = this.getParamsByKeyName(methodSignature.getParameterNames(), key, args);
        if (!Collection.class.isAssignableFrom(parameter.getClass())) {
            throw new IllegalStateException("the Parameter type of the key  must be Collection");
        }

        // 1 校验（方法返回值只能是Map）
        if (!Map.class.isAssignableFrom(methodSignature.getReturnType())) {
            throw new IllegalStateException("the returnType of the method must be Map");
        }


        // 2 从redis获取
        Class clazz = joinPoint.getTarget().getClass();
        Map map = batchCacheClient.loadCache(keyPrefix, (List<Object>) parameter, methodSignature, clazz, keyMapper, seconds);
        return map;
    }

    /**
     * 根据注解BatchCache中的key获取目标方法参数值
     *
     * @param parameterNames 目标方法的所有参数名称
     * @param key            key后缀适配字段
     * @param parameters     目标方法的所有参数值
     * @return
     */
    private Object getParamsByKeyName(String[] parameterNames, String key, Object[] parameters) {

        // 1 校验参数
        if (parameterNames == null || parameterNames.length == 0) {
            throw new IllegalStateException("the parameterNames is null");
        }
        if (parameters == null || parameters.length == 0) {
            throw new IllegalStateException("the parameters is null");
        }
        // 如果未指定key值，则默认取方法的第一个参数
        if (StringUtils.isEmpty(key) || parameterNames.length == 1) {
            return parameters[0];
        }

        // 2 查询key后缀的参数值
        for (int i = 0; i < parameterNames.length; i++) {
            if (key.equals(parameterNames[i])) {
                return parameters[i];
            }
        }
        throw new IllegalStateException("the Parameter of the keyMapper  not exist");
    }
}
