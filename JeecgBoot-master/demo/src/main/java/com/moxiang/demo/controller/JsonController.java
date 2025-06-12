package com.moxiang.demo.controller;

import com.moxiang.demo.entity.User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/json")
public class JsonController {
    @RequestMapping("/user")
    public User getUser(){
        return new User(1,"测试","123456");
    }

    @RequestMapping("list")
    public List<User> getUserList(){
        List<User> userList = new ArrayList<>();
        User user1 = new User(1,"测试","123456");
        User user2 = new User(2,"实验","123456");
        userList.add(user1);
        userList.add(user2);
        return userList;
    }

    /*Map数组是可以自动扩容的*/
    @RequestMapping("/map")
    public Map<String, Object> getMap(){
        Map<String, Object> map = new HashMap<>();
        User user = new User(1,"测试","123456");
        map.put("作者信息", user);
        map.put("博客地址","xxxxxxx");
        map.put("csdn地址","55wwssss");
        map.put("cxs地址","55wss897ss");
        return map;
    }


}
