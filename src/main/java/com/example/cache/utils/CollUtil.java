package com.example.cache.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 集合工具
 *
 * @author zhuwei
 * @Date 2020/10/27 PM4:46
 */
public class CollUtil {

    /**
     * 新建一个ArrayList
     *
     * @param <T>    集合元素类型
     * @param values 数组
     * @return ArrayList对象
     */
    @SafeVarargs
    public static <T> ArrayList<T> newArrayList(T... values) {
        return (ArrayList<T>) list(false, values);
    }

    /**
     * 新建一个List
     *
     * @param <T>      集合元素类型
     * @param isLinked 是否新建LinkedList
     * @param values   数组
     * @return List对象
     * @since 4.1.2
     */
    @SafeVarargs
    public static <T> List<T> list(boolean isLinked, T... values) {
        if (values == null || values.length == 0) {
            return list(isLinked);
        }
        final List<T> arrayList = isLinked ? new LinkedList<>() : new ArrayList<>(values.length);
        Collections.addAll(arrayList, values);
        return arrayList;
    }
}
