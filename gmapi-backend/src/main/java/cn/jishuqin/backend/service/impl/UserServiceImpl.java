package cn.jishuqin.backend.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.CircleCaptcha;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import cn.jishuqin.backend.common.RabbitUtils;
import cn.jishuqin.backend.common.SmsLimiter;
import cn.jishuqin.backend.feign.ApiOrderFeignClient;
import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.common.ResultUtils;
import cn.jishuqin.common.model.dto.SmsDto;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.jishuqin.common.ErrorCode;
import cn.jishuqin.common.constant.UserConstant;
import cn.jishuqin.common.exception.BusinessException;
import cn.jishuqin.common.model.entity.User;
import cn.jishuqin.common.model.vo.EchartsVo;
import cn.jishuqin.backend.common.MobileSignature;
import cn.jishuqin.backend.mapper.InterfaceInfoMapper;
import cn.jishuqin.backend.mapper.UserMapper;
import cn.jishuqin.backend.model.dto.user.UserBindPhoneRequest;
import cn.jishuqin.backend.model.dto.user.UserLoginRequest;
import cn.jishuqin.backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static cn.jishuqin.common.constant.UserConstant.*;


/**
 * 用户服务实现类
 *
 * @author gumeng
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private InterfaceInfoMapper interfaceInfoMapper;

    @Resource
    private ApiOrderFeignClient apiOrderFeignClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private MobileSignature mobileSignature;

    @Resource
    private SmsLimiter smsLimiter;

    @Autowired
    private RabbitUtils rabbitUtils;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "gumeng";

    @Override
    public User codeLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userPhone = userLoginRequest.getUserPhone();
        //开头数字必须为1，第二位必须为3至9之间的数字，后九尾必须为0至9组织成的十一位电话号码
        if (!ReUtil.isMatch(UserConstant.MOBILE_REGEX, userPhone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式错误！");
        }

        // 3.从redis获取验证码并校验
        String code = userLoginRequest.getCode();
        boolean verified = smsLimiter.verifyCode(userPhone, code);
        if (!verified) {
            // 不一致，报错
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误！");
        }

        // 4.一致，根据手机号查询用户 select * from tb_user where phone = ?
        User user = query().eq("userPhone", userPhone).one();
        if (user == null) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在！");
        }

        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return user;
    }

    @Override
    public ArrayList<Object> getEchartsData() {
        //1、获取最近7天的日期
        List<String> dateList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < 7; i++) {
            Date date = DateUtils.addDays(new Date(), -i);
            String formatDate = sdf.format(date);
            dateList.add(formatDate);
        }
        ArrayList<Object> objects = new ArrayList<>();
        //2、查询最近7天的交易成功信息
        BaseResponse<List<EchartsVo>> orderEchartsData = apiOrderFeignClient.getOrderEchartsData(dateList);
        ArrayList<Long> orderList = extracted(dateList, orderEchartsData.getData(), true);
        //3、根据最近七天的日期去数据库中查询用户信息
        ArrayList<Long> userList = extracted(dateList, userMapper.getUserList(dateList), false);
        //4、查询最近7天的接口信息
        ArrayList<Long> interfaceList = extracted(dateList, interfaceInfoMapper.getInterfaceList(dateList), false);
        Collections.reverse(dateList);
        objects.add(dateList);
        objects.add(userList);
        objects.add(interfaceList);
        objects.add(orderList);
        return objects;
    }

    /**
     * 生成图形验证码
     *
     * @param request
     * @param response
     */
    @Override
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) {
        // 随机生成 4 位验证码
        RandomGenerator randomGenerator = new RandomGenerator("1234567890", 4);
        // 定义图片的显示大小
        CircleCaptcha circleCaptcha = CaptchaUtil.createCircleCaptcha(100, 30);
        response.setContentType("image/jpeg");
        response.setHeader("Pragma", "No-cache");
        // 在前端发送请求时携带captchaId，用于标识不同的用户。
        String signature = request.getHeader("signature");
        if (null == signature) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        try {
            // 调用父类的 setGenerator() 方法，设置验证码的类型
            circleCaptcha.setGenerator(randomGenerator);
            // 输出到页面
            circleCaptcha.write(response.getOutputStream());
            // 打印日志
            log.info("captchaId：{} ----生成的验证码:{}", signature, circleCaptcha.getCode());
            // 关闭流
            response.getOutputStream().close();
            //将对应的验证码存入redis中去，2分钟后过期
            stringRedisTemplate.opsForValue().set(CAPTCHA_PREFIX + signature, circleCaptcha.getCode(), 4, TimeUnit.MINUTES);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 绑定用户手机号
     *
     * @param userBindPhoneRequest
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String bindPhone(UserBindPhoneRequest userBindPhoneRequest, HttpServletRequest request) {

        Long id = userBindPhoneRequest.getId();
        String userAccount = userBindPhoneRequest.getUserAccount();
        String userPhone = userBindPhoneRequest.getUserPhone();
        String code = userBindPhoneRequest.getCode();
        String captcha = userBindPhoneRequest.getCaptcha();
        String signature = request.getHeader("signature");

        if (StringUtils.isAnyBlank(userAccount, String.valueOf(id), userPhone, code, captcha, signature)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 验证手机号是否正确
        if (!ReUtil.isMatch(UserConstant.MOBILE_REGEX, userPhone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式错误！");
        }

        // 从redis获取验证码并校验
        String picCaptcha = stringRedisTemplate.opsForValue().get(CAPTCHA_PREFIX + signature);
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + userPhone);

        // 验证图形验证码是否正确
        if (null == picCaptcha || !picCaptcha.equals(captcha)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图形验证码错误或已经过期，请重新刷新验证码");
        }

        // 验证手机验证码是否正确
        if (null == cacheCode || !cacheCode.equals(code)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "手机验证码错误或已经过期，请重新刷新验证码");
        }

        synchronized (userAccount.intern()) {
            //手机号不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userPhone", userPhone);
            long count = userMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号已经被注册");
            }
            boolean update = this.update(new UpdateWrapper<User>().eq("id", id)
                    .eq("userAccount", userAccount)
                    .set("userPhone", userPhone)
                    .set("updateTime", new Date()));
            if (!update) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
        }
        String phone = DesensitizedUtil.mobilePhone(userPhone);
        // 更新全局对象中的用户信息
        // 移除登录态
        User loginUser = getLoginUser(request);
        loginUser.setUserPhone(phone);
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        request.getSession().setAttribute(USER_LOGIN_STATE, loginUser);
        return phone;
    }

    @Override
    public String getPassUserType(String username, HttpServletResponse response) {

        if (null == username) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 查询用户手机号
        String userPhone = userMapper.getUserPhone(username);

        if (null == userPhone) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未注册");
        }
        //手机号签名时长为10分钟
        long expiryTime = System.currentTimeMillis() + 1000L * (long) 600;
        String signature = null;

        try {
            // 将手机号进行加密后，存入redis
            signature = mobileSignature.makeMobileSignature(username);
            String encryptHex = mobileSignature.makeEncryptHex(username, userPhone);
            stringRedisTemplate.opsForValue().set(signature, encryptHex, expiryTime, TimeUnit.SECONDS);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        Cookie cookie = new Cookie(MOBILE_SIGNATURE, signature);
        cookie.setMaxAge(600);
        cookie.setPath("/"); //登陆页面下才可以访问
        response.addCookie(cookie);
        return DesensitizedUtil.mobilePhone(userPhone);
    }

    @Override
    public String authPassUserCode(String code, HttpServletRequest request) {

        if (null == code) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Cookie[] cookies = request.getCookies();
        String value = null;
        for (Cookie cookie : cookies) {
            String name = cookie.getName();
            if (MOBILE_SIGNATURE.equals(name)) {
                value = cookie.getValue();
            }
        }
        if (null == value) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //从redis中拿到加密后的手机号
        String s = stringRedisTemplate.opsForValue().get(value);
        String[] strings = mobileSignature.decodeHex(s);
        String userPhone = strings[1];
        boolean verified = smsLimiter.verifyCode(userPhone, code);
        // 验证手机验证码是否正确
        if (!verified) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "手机验证码错误或已经过期，请重新刷新验证码");
        }
        return "验证成功";
    }

    /**
     * 忘记密码请求第二步，发送验证码
     *
     * @param request
     * @return
     */
    @Override
    public String sendPassUserCode(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String value = null;
        for (Cookie cookie : cookies) {
            String name = cookie.getName();
            if (MOBILE_SIGNATURE.equals(name)) {
                value = cookie.getValue();
            }
        }
        if (null == value) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 从redis中拿到加密后的手机号
        String s = stringRedisTemplate.opsForValue().get(value);
        String[] strings = mobileSignature.decodeHex(s);
        //验证签名
        String username = strings[0];
        String signature = null;
        try {
            signature = mobileSignature.makeMobileSignature(username);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        if (!signature.equals(value)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "请求数据非法");
        }
        // 发送对应验证码
        String phone = strings[1];
        BaseResponse response = this.captcha(phone);
        if (response.getCode() != 0){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"发送验证码失败");
        }
        return "发送验证码成功";
    }

    @Override
    public String updateUserPass(String password, HttpServletRequest request) {

        if (null == password){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Cookie[] cookies = request.getCookies();
        String value = null;
        for (Cookie cookie : cookies) {
            String name = cookie.getName();
            if ("api-mobile-signature".equals(name)){
                value = cookie.getValue();
            }
        }
        // value为空会停止执行下面的语句
        assert value != null;
        //从redis中拿到加密后的信息
        String s = stringRedisTemplate.opsForValue().get(value);
        String[] strings = mobileSignature.decodeHex(s);
        String username = strings[0];
        if (null == username){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //更新密码
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + password).getBytes());
        boolean update = this.update(new UpdateWrapper<User>().eq("userAccount", username).set("userPassword", encryptPassword));
        if (!update){
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        stringRedisTemplate.delete(value);
        return "修改成功";
    }

    @Override
    public BaseResponse<String> captcha(String userPhone) {

        if (StringUtils.isBlank(userPhone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 开头数字必须为1，第二位必须为3至9之间的数字，后九尾必须为0至9组织成的十一位电话号码
        if (!ReUtil.isMatch(UserConstant.MOBILE_REGEX, userPhone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式错误！");
        }

        // 发送对应验证码
        String code = RandomUtil.randomNumbers(6);
        // 使用redis来存储手机号和验证码 ，同时使用令牌桶算法来实现流量控制
        boolean sendSmsAuth = smsLimiter.sendSmsAuth(userPhone, code);
        if (!sendSmsAuth) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "发送频率过高，请稍后再试");
        }
        SmsDto smsTo = new SmsDto(userPhone,code);
        try {
            //实际发送短信的功能交给第三方服务去实现
            rabbitUtils.sendSms(smsTo);
        }catch (Exception e){
            //发送失败，删除令牌桶
            redisTemplate.delete("sms:"+userPhone+"_last_refill_time");
            redisTemplate.delete("sms:"+userPhone+"_tokens");
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"发送验证码失败，请稍后再试");
        }
        log.info("发送验证码成功---->手机号为{}，验证码为{}",userPhone,code);
        return ResultUtils.success("发送验证码成功");
    }

    /**
     * 封装echarts返回数据
     *
     * @param dateList
     * @param list
     * @return
     */
    private static ArrayList<Long> extracted(List<String> dateList, List<EchartsVo> list, boolean isChange) {
        ArrayList<Long> echartsVos = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            boolean bool = false;
            //创建内循环 根据查询出已有的数量 循环次数
            for (int m = 0; m < list.size(); m++) {
                if (!isChange) {
                    EchartsVo echartsVo = list.get(m);
                    if (dateList.get(i).equals(echartsVo.getDate())) {
                        echartsVos.add(echartsVo.getCount());
                        bool = true;
                        break;
                    }
                } else {
                    //处理数据转化问题
                    String s = JSONUtil.toJsonStr(list.get(m));
                    EchartsVo echartsVo = JSONUtil.toBean(s, EchartsVo.class);
                    if (dateList.get(i).equals(echartsVo.getDate())) {
                        echartsVos.add(echartsVo.getCount());
                        bool = true;
                        break;
                    }
                }
            }
            if (!bool) {
                echartsVos.add(0L);
            }
        }
        Collections.reverse(echartsVos);
        return echartsVos;
    }

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String userPhone, String code) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 校验手机号
        if (!ReUtil.isMatch(MOBILE_REGEX, userPhone)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式错误");
        }

        // 从redis获取验证码并校验
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + userPhone);
        if (cacheCode == null || !cacheCode.equals(code)) {
            // 不一致，报错
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误！");

        }

        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            QueryWrapper<User> queryWrapper1 = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            queryWrapper1.eq("userPhone", userPhone);
            long count = userMapper.selectCount(queryWrapper);
            long count1 = userMapper.selectCount(queryWrapper1);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            if (count1 > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号已被注册！");

            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 分配 accessKey，secretKey
            String accessKey = DigestUtil.md5Hex(SALT + userAccount + RandomUtil.randomNumbers(5));
            String secretKey = DigestUtil.md5Hex(SALT + userAccount + RandomUtil.randomNumbers(8));
            // 4. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            user.setUserPhone(userPhone);
            user.setAccessKey(accessKey);
            user.setSecretKey(secretKey);
            user.setUserAvatar(USER_DEFAULT_AVATAR);
            user.setUserName(userAccount + "_" + RandomUtil.randomString(4));
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        String userPhone = user.getUserPhone();
        String phone = DesensitizedUtil.mobilePhone(userPhone);
        user.setUserPhone(phone);
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return user;
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && ADMIN_ROLE.equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

}




