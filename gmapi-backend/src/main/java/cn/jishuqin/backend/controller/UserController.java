package cn.jishuqin.backend.controller;

import cn.jishuqin.backend.annotation.AuthCheck;
import cn.jishuqin.backend.common.DeleteRequest;
import cn.jishuqin.backend.model.dto.user.*;
import cn.jishuqin.backend.model.vo.UserVO;
import cn.jishuqin.backend.service.UserService;
import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.common.ErrorCode;
import cn.jishuqin.common.ResultUtils;
import cn.jishuqin.common.exception.BusinessException;
import cn.jishuqin.common.model.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static cn.jishuqin.common.constant.UserConstant.ADMIN_ROLE;
import static cn.jishuqin.common.constant.UserConstant.DEFAULT_ROLE;


/**
 * 用户接口
 *
 * @author gumeng
 */
@RestController
@RequestMapping("/user")
@Api(tags = "用户操作接口")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;


    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @ApiOperation("用户注册")
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String userPhone = userRegisterRequest.getUserPhone();
        String code = userRegisterRequest.getCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, userPhone, code)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, userPhone, code);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("用户登录")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    @ApiOperation("验证用户的登录状态")
    @GetMapping("/checkUserLogin")
    public BaseResponse checkUserLogin(HttpServletRequest request){
        return getLoginUser(request);
    }

    /**
     * 生成图形验证码
     * @param request
     * @param response
     */
    @ApiOperation("生成图形验证码")
    @GetMapping("/getCaptcha")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) {
        userService.getCaptcha(request, response);
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @ApiOperation("用户退出")
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @ApiOperation("获取当前登录用户")
    @GetMapping("/get/login")
    public BaseResponse<UserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return ResultUtils.success(userVO);
    }

    /**
     * 绑定用户手机号
     * @param userBindPhoneRequest
     * @param request
     * @return
     */
    @ApiOperation("绑定用户手机号")
    @PostMapping("/bindPhone")
    public BaseResponse<String> bindPhone(UserBindPhoneRequest userBindPhoneRequest,HttpServletRequest request){
        String phone = userService.bindPhone(userBindPhoneRequest,request);
        return ResultUtils.success(phone);
    }

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @return
     */
    @ApiOperation("添加用户")
    @AuthCheck(mustRole = ADMIN_ROLE)
    @PostMapping("/add")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        boolean result = userService.save(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(user.getId());
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @ApiOperation("删除用户")
    @PostMapping("/delete")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 手机号登陆
     * @param userLoginRequest
     * @param request
     * @return
     */
    @ApiOperation("手机号登陆")
    @PostMapping("/login/phone")
    public BaseResponse<User> codeLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {

        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User user = userService.codeLogin(userLoginRequest, request);
        return ResultUtils.success(user);
    }

    /**
     * 发送验证码
     * @param userPhone
     * @return
     */
    @ApiOperation("发送验证码")
    @GetMapping("/send/code")
    public BaseResponse<String> sendCode(String userPhone) {
        return userService.captcha(userPhone);
    }

    /**
     * 忘记密码
     * @param username
     * @param response
     * @return
     */
    @ApiOperation("用户忘记密码，返回用户注册时的手机号")
    @PostMapping("/getpassusertype")
    public BaseResponse<String> getPassUserType(String username,HttpServletResponse response){
        String newMobile =  userService.getPassUserType(username,response);
        return ResultUtils.success(newMobile);
    }

    /**
     * 忘记密码请求第二步，发送验证码
     * @param request
     * @return
     */
    @ApiOperation("忘记密码请求第二步，发送验证码")
    @PostMapping("/sendPassUserCode")
    public BaseResponse<String> sendPassUserCode(HttpServletRequest request) {
        String result = userService.sendPassUserCode(request);
        return ResultUtils.success(result);
    }

    /**
     * 忘记密码部分-验证手机号和验证码输入是否正确
     * @param code
     * @param request
     * @return
     */
    @ApiOperation("忘记密码部分-验证手机号和验证码输入是否正确")
    @PostMapping("/authPassUserCode")
    public BaseResponse<String> authPassUserCode(String code,HttpServletRequest request){
        String result = userService.authPassUserCode(code,request);
        return ResultUtils.success(result);
    }

    /**
     * 修改用户密码
     * @param password
     * @param request
     * @return
     */
    @ApiOperation("修改用户密码")
    @PostMapping("/updateUserPass")
    public BaseResponse<String> updateUserPass(String password,HttpServletRequest request){
        String result = userService.updateUserPass(password,request);
        return ResultUtils.success(result);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @return
     */
    @ApiOperation("更新用户")
    @PostMapping("/update")
    @AuthCheck(anyRole = {ADMIN_ROLE,DEFAULT_ROLE})
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取用户
     *
     * @param id
     * @return
     */
    @ApiOperation("根据 id  获取用户")
    @GetMapping("/get")
    public BaseResponse getUserById(@RequestParam("id") int id) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return ResultUtils.success(userVO);
    }

    /**
     * 获取用户列表
     *
     * @param userQueryRequest
     * @return
     */
    @ApiOperation("获取用户列表")
    @GetMapping("/list")
    public BaseResponse<List<UserVO>> listUser(UserQueryRequest userQueryRequest) {
        User userQuery = new User();
        if (userQueryRequest != null) {
            BeanUtils.copyProperties(userQueryRequest, userQuery);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(userQuery);
        List<User> userList = userService.list(queryWrapper);
        List<UserVO> userVOList = userList.stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).collect(Collectors.toList());
        return ResultUtils.success(userVOList);
    }

    /**
     * 分页获取用户列表
     *
     * @param userQueryRequest
     * @return
     */
    @ApiOperation("分页获取用户列表")
    @GetMapping("/list/page")
    public BaseResponse<Page<UserVO>> listUserByPage(UserQueryRequest userQueryRequest ) {
        long current = 1;
        long size = 10;
        User userQuery = new User();
        if (userQueryRequest != null) {
            BeanUtils.copyProperties(userQueryRequest, userQuery);
            current = userQueryRequest.getCurrent();
            size = userQueryRequest.getPageSize();
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(userQuery);
        Page<User> userPage = userService.page(new Page<>(current, size), queryWrapper);
        Page<UserVO> userVOPage = new PageDTO<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserVO> userVOList = userPage.getRecords().stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).collect(Collectors.toList());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * sdk下载
     *
     * @param response
     * @throws FileNotFoundException
     */
    @ApiOperation("sdk 下载")
    @GetMapping("/downLoad")
    public BaseResponse<Boolean> downLoad(HttpServletResponse response) {
        // 下载本地文件
        String fileName = "gmapi-client-sdk-1.0.0.jar"; // 文件的默认保存名
        // 读到流中
        InputStream inStream = this.getClass().getClassLoader().getResourceAsStream(fileName);
        // 设置输出的格式
        response.reset();
        response.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        // 允许所有来源访同
        response.addHeader("Access-Control-Allow-Method", "POST,GET");//允许访问的方式

        // 循环取出流中的数据
        byte[] b = new byte[100];
        int len = 0;
        try {
            while (true) {

                try {
                    if (!((len = inStream.read(b)) > 0)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                response.getOutputStream().write(b, 0, len);
            }
            inStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return ResultUtils.success(true);
    }

    /**
     * 获取站点用户数
     * @return
     */
    @ApiOperation("获取站点用户数")
    @GetMapping("/getActiveUser")
    public BaseResponse<Long> getActiveUser() {
        return ResultUtils.success(userService.count());
    }

    /**
     * 获取echarts需要展示的数据
     * @return
     */
    @ApiOperation("获取echarts需要展示的数据")
    @GetMapping("/getEchartsData")
    public BaseResponse<ArrayList<Object>> getEchartsData() {
        ArrayList<Object> echartsData = userService.getEchartsData();
        return ResultUtils.success(echartsData);
    }

}
