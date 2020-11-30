# 支持批量缓存的注解@BatchCache

### 需求背景
分布式集群环境下，为提高系统接口访问效率，可使用redis中间件缓存技术，spring-data-redis提供的注解@Cacheable、@CacheEvict、@CachePut，只支持单个对象的缓存。但在实际开发中，接口的访问查询往往是批量的，此项目基于此开发了@BatchCache批量插入缓存注解。

### 注解使用限制
使用注解的接口：
* 接口参数需要传入，确定redisKey唯一性的参数（比如id）
* 返回类型必须是Map类型（key为1中的参数值，value为对象值）

### 参考
* [Spring Cache 缺陷，我好像有解决方案了](https://cloud.tencent.com/developer/article/1613143)
* [扩展spring cache 支持缓存多租户及其自动过期](https://segmentfault.com/a/1190000018650525)
* [easy-cache](https://github.com/shenjianeng/easy-cache/blob/master/src/main/java/com/github/shenjianeng/easycache/core/Cache.java)
* [spring-cache-ext](https://github.com/leozlliang/spring-cache-ext)

欢迎大家可以随意提意见～