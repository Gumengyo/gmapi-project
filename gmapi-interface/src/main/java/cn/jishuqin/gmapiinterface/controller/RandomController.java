package cn.jishuqin.gmapiinterface.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.jishuqin.gmapiinterface.entity.Content;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 顾梦
 * @create 2023/5/21
 */
@RestController
@RequestMapping("/random")
public class RandomController {

    @GetMapping("/quotations")
    public String getQuotations(HttpServletRequest request) {
        HttpResponse httpResponse = HttpRequest.get("https://v1.hitokoto.cn/")
                .execute();
        String body = httpResponse.body();

        JSONObject jsonObject = JSONUtil.parseObj(body);
        String hitokoto = jsonObject.getStr("hitokoto");
        String from = jsonObject.getStr("from");

        String quotation = hitokoto + "————" + " 《" + from + "》";
        return quotation;
    }

    @GetMapping("/background")
    public String getBackground(HttpServletRequest request) {
        HttpResponse httpResponse = HttpRequest.get("https://api.kbai.cc/dmh/")
                .execute();
        String body = httpResponse.body();
        return body;
    }

    @PostMapping("/Content")
    public String getContent(@RequestBody Content content, HttpServletRequest request) {
        HttpResponse httpResponse = null;
        String type = content.getType();

        httpResponse = HttpRequest.get("https://api.eatrice.top/" + type).execute();
        String body = httpResponse.body();
        JSONObject jsonObject = JSONUtil.parseObj(body);
        String content1 = jsonObject.getStr("Content");
        String author = jsonObject.getStr("Author");

        return content1+"————"+" 《"+author+"》";
    }
}
