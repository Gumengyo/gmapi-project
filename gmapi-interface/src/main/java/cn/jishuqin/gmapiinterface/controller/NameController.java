package cn.jishuqin.gmapiinterface.controller;


import cn.jishuqin.gmapiinterface.entity.User;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 名称 API
 *
 * @author 顾梦
 * @create 2023/4/26
 */
@RestController
@RequestMapping("/name")
public class NameController {
    @GetMapping("/get")
    public String getNameByGet(String name) {
        return "GET 你的名字是：" + name;
    }

    @PostMapping("/post")
    public String getNameByPost(@RequestParam String name) {
        return "POST 你的名字是：" + name;
    }
    @PostMapping("/user")
    public String getUsernameByPost(@RequestBody User user, HttpServletRequest request) {
        String result = "POST 用户名字是：" + user.getUsername();
        return result;
    }


}








