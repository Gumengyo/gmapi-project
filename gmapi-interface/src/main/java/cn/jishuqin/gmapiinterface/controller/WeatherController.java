package cn.jishuqin.gmapiinterface.controller;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 顾梦
 * @description 获取天气
 * @since 2023/9/10
 */
@RestController
@RequestMapping("/weather")
public class WeatherController {

    @GetMapping("/weatherInfo")
    public String getWeatherInfo(String city) {

        Map<String,Object> map = new HashMap<>();
        map.put("key","984c9a7ef685f8fe99b127fb701be5d1");
        map.put("city",city);
        HttpResponse httpResponse = HttpRequest.get("https://restapi.amap.com/v3/weather/weatherInfo").form(map).execute();

        return httpResponse.body();
    }
}
