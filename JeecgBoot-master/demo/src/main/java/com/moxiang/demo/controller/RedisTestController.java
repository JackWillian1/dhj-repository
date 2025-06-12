package com.moxiang.demo.controller;

import com.moxiang.demo.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedisTestController {
    @Autowired
    private RedisService redisService;


    @GetMapping("/test-redis")
    public String testRedis(){
        redisService.setKeyValue("dhjS","acsSe");
        String value = redisService.getValue("dhjS");
        return "获取的值：" + value;
    }
}
