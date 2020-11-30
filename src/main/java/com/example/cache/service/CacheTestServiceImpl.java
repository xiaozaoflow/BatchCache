package com.example.cache.service;

import com.example.cache.annotation.BatchCache;
import com.example.cache.entity.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 描述
 *
 * @author zhuwei
 * @Date 2020/10/27 PM8:04
 */
@Service
public class CacheTestServiceImpl implements ICacheTestService {

    @Cacheable(value = "cache:user", key = "#userId")
    @Override
    public User getById(Long userId) {
        System.out.println("查询数据库ing.....");
        return new User(1L, "zhuwei");
    }

    //    @Cacheable(value = "cache:user", key = "#userIdList")
    @BatchCache(value = "cache:user:batch", seconds = 1000)
    @Override
    public Map<Long, User> listByIds(List<Long> userIdList) {
        Map<Long, User> userMap = new HashMap<>();

        if (userIdList.contains(1L)) {
            System.out.println("模拟查询....id=" + 1L);
            userMap.put(1L, new User(1L, "zhuwei1"));
        }
        if (userIdList.contains(2L)) {
            System.out.println("模拟查询....id=" + 2L);
            userMap.put(2L, new User(2L, "zhuwei2"));
        }
        if (userIdList.contains(3L)) {
            System.out.println("模拟查询....id=" + 3L);
            userMap.put(3L, new User(3L, "zhuwei3"));
        }
        return userMap;
    }
}
