package com.moxiang.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/start")
public class StartController {
    @RequestMapping("springboot")
    public String startSpringBoot(){
        return "你好";
    }
}
