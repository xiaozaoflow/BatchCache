package com.example.cache.controller;

import com.example.cache.entity.User;
import com.example.cache.service.ICacheTestService;
import com.example.cache.utils.CollUtil;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 描述
 *
 * @author zhuwei
 * @Date 2020/10/27 PM8:05
 */
@RestController
@AllArgsConstructor
@RequestMapping(value = "/cache")
public class CacheTestController {

    private final ICacheTestService cacheTestService;

    @PostMapping(value = "/get")
    public User get() {
        return cacheTestService.getById(1L);
    }

    @PostMapping(value = "/list")
    public Map<Long, User> list(@RequestBody List<Long> userIdList) {
        return cacheTestService.listByIds(userIdList);
    }
}
