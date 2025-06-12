package com.moxiang.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

@Service
public class RedisService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    public void setKeyValue(String key,String value){
        //设置Redis键值对
        stringRedisTemplate.opsForValue().set(key,value);
    }

    public String getValue(String key){
        //获取Redis键对应的值
        return stringRedisTemplate.opsForValue().get(key);
    }
}
