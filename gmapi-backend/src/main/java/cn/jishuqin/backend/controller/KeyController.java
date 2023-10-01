package cn.jishuqin.backend.controller;


import cn.hutool.core.util.RandomUtil;
import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.common.ResultUtils;
import cn.jishuqin.common.model.entity.User;
import cn.jishuqin.backend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static com.sun.javafx.font.FontResource.SALT;


/**
 * 用户密钥"
 *
 * @author 顾梦
 */
@Api(tags = "用户密钥")
@RestController
@RequestMapping("/key")
public class KeyController {


    @Resource
    private UserService userService;


    /**
     * 获取用户个人信息
     *
     * @param request
     * @return
     */
    @ApiOperation("获取用户个人信息")
    @GetMapping("/get")
    public BaseResponse<User> getUser(HttpServletRequest request) {

        User user = userService.getLoginUser(request);
        return ResultUtils.success(user);

    }

    /**
     * 修改 ak sk
     * @param request
     * @return
     */
    @ApiOperation("修改 ak sk")
    @PostMapping("/modify")
    public BaseResponse<Boolean> updateSetting(HttpServletRequest request) {


        User user = userService.getLoginUser(request);
        Long userId = user.getId();
        User realUser = userService.getById(userId);


        String accessKey = DigestUtils.md5DigestAsHex((SALT + System.currentTimeMillis() / 1000 + RandomUtil.randomNumbers(5)).getBytes());
        String secretKey = DigestUtils.md5DigestAsHex((SALT + System.currentTimeMillis() / 1000 + RandomUtil.randomNumbers(8)).getBytes());
        realUser.setAccessKey(accessKey);
        realUser.setSecretKey(secretKey);

        boolean result = userService.updateById(realUser);

        return ResultUtils.success(result);

    }


}
