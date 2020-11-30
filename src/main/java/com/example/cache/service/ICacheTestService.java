package com.example.cache.service;

import com.example.cache.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 描述
 *
 * @author zhuwei
 * @Date 2020/10/27 PM8:02
 */
public interface ICacheTestService {

    User getById(Long userId);

    Map<Long, User> listByIds(List<Long> uesrIdList);
}
