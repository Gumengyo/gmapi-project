package cn.jishuqin.order.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.common.ErrorCode;
import cn.jishuqin.common.ResultUtils;
import cn.jishuqin.common.constant.CookieConstant;
import cn.jishuqin.common.constant.OrderConstant;
import cn.jishuqin.common.exception.BusinessException;
import cn.jishuqin.common.model.entity.*;
import cn.jishuqin.common.model.vo.EchartsVo;
import cn.jishuqin.common.model.vo.LockChargingVo;
import cn.jishuqin.common.service.InnerAlipayInfoService;
import cn.jishuqin.order.feign.UserFeignServices;
import cn.jishuqin.order.mapper.ApiOrderMapper;
import cn.jishuqin.order.model.dto.ApiOrderCancelDto;
import cn.jishuqin.order.model.dto.ApiOrderStatusInfoDto;
import cn.jishuqin.order.model.dto.OrderDto;
import cn.jishuqin.order.model.vo.ApiOrderStatusVo;
import cn.jishuqin.order.service.ApiOrderService;
import cn.jishuqin.order.service.OrderLockService;
import cn.jishuqin.order.utils.RabbitOrderUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;



/**
 * @author gumeng
 * @description 针对表【api_order(订单信息)】的数据库操作Service实现
 * @createDate 2023-09-04 23:32:22
 */
@Service
@Slf4j
public class ApiOrderServiceImpl extends ServiceImpl<ApiOrderMapper, ApiOrder>
        implements ApiOrderService {
    @Resource
    private RedisTemplate redisTemplate;

    @Autowired
    private UserFeignServices userFeignServices;

    @Reference
    private InnerAlipayInfoService innerAlipayInfoService;

    @Resource
    private OrderLockService orderLockService;

    @Resource
    private RabbitOrderUtils rabbitOrderUtils;

    @Resource
    private ApiOrderMapper apiOrderMapper;

    @Override
    public BaseResponse generateToken(Long id, HttpServletResponse response) {

        if (null == id) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
        //防重令牌
        String token = IdUtil.simpleUUID();
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + id, token, 30, TimeUnit.MINUTES);
        Cookie cookie = new Cookie(CookieConstant.orderToken, token);
        cookie.setPath("/");
        cookie.setMaxAge(CookieConstant.orderTokenExpireTime);
        response.addCookie(cookie);
        return ResultUtils.success(null);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public BaseResponse<String> generateOrder(OrderDto orderDto, HttpServletRequest request) throws IOException {

        // 1、验证用户是否登录
        String header = request.getHeader("Cookie");
        String loginUserVo = HttpRequest.get("http://139.9.212.105:7529/api/user/checkUserLogin")
                .header("Cookie", header)
                .timeout(20000)
                .execute().body();
        Gson gson = new Gson();
        Type baseResponseType = new TypeToken<BaseResponse<User>>() {}.getType();
        BaseResponse<User> baseResponse = gson.fromJson(loginUserVo, baseResponseType);
        User user = baseResponse.getData();

        if (null == user) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }

        // 2、健壮性校验
        Long userId = orderDto.getUserId();
        Double totalAmount = orderDto.getTotalAmount();
        Long orderNum = orderDto.getOrderNum();
        Double charging = orderDto.getCharging();
        Long interfaceId = orderDto.getInterfaceId();

        if (null == userId || null == totalAmount || null == orderNum || null == charging || null == interfaceId) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        if (!userId.equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }

        //保留两位小数
        Double temp = orderNum * charging;
        BigDecimal two = new BigDecimal(temp);
        Double three = two.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
        if (!three.equals(totalAmount)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        //3、验证令牌是否合法【令牌的对比和删除必须保证原子性】
        Cookie[] cookies = request.getCookies();
        String token = null;
        for (Cookie cookie : cookies) {
            if (CookieConstant.orderToken.equals(cookie.getName())) {
                token = cookie.getValue();
            }
        }
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = (Long) redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + user.getId()),
                token);
        if (result == 0L) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "提交太快了，请重新提交");
        }

        //4、远程查询是否还有库存、远程异步调用查询接口信息
        BaseResponse<InterfaceInfo> response = userFeignServices.getInterfaceInfoById(interfaceId);
        InterfaceInfo interfaceInfo = response.getData();
        Integer availablePieces = interfaceInfo.getAvailablePieces();
        if (availablePieces < orderNum){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"库存不足，请刷新页面,当前剩余库存为："+availablePieces);
        }

        //5、使用雪花算法生成订单id，并保存订单
        String orderSn = IdWorker.getIdStr();
        ApiOrder apiOrder = new ApiOrder();
        apiOrder.setTotalAmount(totalAmount);
        apiOrder.setOrderSn(orderSn);
        apiOrder.setOrderNum(orderNum);
        apiOrder.setStatus(OrderConstant.toBePaid);
        apiOrder.setInterfaceId(interfaceId);
        apiOrder.setUserId(userId);
        apiOrder.setCharging(charging);
        try {
            save(apiOrder);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "订单保存失败");
        }
        // 6、锁定剩余库存
        OrderLock orderLock = new OrderLock();
        orderLock.setUserId(userId);
        orderLock.setLockNum(orderNum);
        orderLock.setLockStatus(1);
        orderLock.setChargingId(interfaceId);
        orderLock.setOrderSn(orderSn);
        try {
            orderLockService.save(orderLock);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "库存锁定失败");
        }
        //7、更新剩余可调用接口数量
        LockChargingVo lockChargingVo = new LockChargingVo();
        lockChargingVo.setOrderNum(orderNum);
        lockChargingVo.setInterfaceid(interfaceId);
        BaseResponse<Boolean> updateAvailablePieces = userFeignServices.updateAvailablePieces(lockChargingVo);
        Boolean updated = updateAvailablePieces.getData();
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "库存更新失败");
        }

        //8、全部锁定完成后，向mq延时队列发送订单消息，且30分钟过期
        rabbitOrderUtils.sendOrderSnInfo(apiOrder);

        return ResultUtils.success("创建订单成功");
    }

    @Override
    public BaseResponse<Page<ApiOrderStatusVo>> getCurrentOrderInfo(ApiOrderStatusInfoDto statusInfoDto, HttpServletRequest request) {

        String header = request.getHeader("Cookie");
        String loginUserVo = HttpRequest.get("http://139.9.212.105:7529/api/user/checkUserLogin")
                .header("Cookie", header)
                .timeout(20000)
                .execute().body();
        Gson gson = new Gson();
        Type baseResponseType = new TypeToken<BaseResponse<User>>() {}.getType();
        BaseResponse<User> baseResponse = gson.fromJson(loginUserVo, baseResponseType);
        User loginUser = baseResponse.getData();

        if (null == loginUser) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
        Long userId = statusInfoDto.getUserId();

        if (!loginUser.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = statusInfoDto.getCurrent();
        // 限制爬虫
        long size = statusInfoDto.getPageSize();
        if (size > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<ApiOrder> orderPage = query().eq("userId", userId)
                .orderByDesc("createTime")
                .page(new Page<>(current, size));
        List<ApiOrderStatusVo> apiOrderStatusVos = orderPage.getRecords().stream().map(order -> {
            ApiOrderStatusVo apiOrderStatusVo = new ApiOrderStatusVo();
            BeanUtils.copyProperties(order, apiOrderStatusVo);
            BaseResponse<InterfaceInfo> response = userFeignServices.getInterfaceInfoById(order.getInterfaceId());
            InterfaceInfo interfaceInfo = response.getData();
            apiOrderStatusVo.setName(interfaceInfo.getName());
            // 如果已支付，获取支付信息
            if (OrderConstant.finish == order.getStatus()) {
                AlipayInfo alipayInfo = innerAlipayInfoService.getByOrderSn(order.getOrderSn());
                BeanUtils.copyProperties(alipayInfo, apiOrderStatusVo);
            }
            return apiOrderStatusVo;
        }).collect(Collectors.toList());
        Page<ApiOrderStatusVo> orderStatusVoPage = new Page<>(current, size, orderPage.getTotal());
        orderStatusVoPage.setRecords(apiOrderStatusVos);
        return ResultUtils.success(orderStatusVoPage);
    }

    @Override
    public BaseResponse<String> cancelOrderSn(ApiOrderCancelDto apiOrderCancelDto, HttpServletRequest request) {
        Long orderNum = apiOrderCancelDto.getOrderNum();
        String orderSn = apiOrderCancelDto.getOrderSn();
        Long interfaceId = apiOrderCancelDto.getInterfaceId();
        //订单已经被取消的情况
        ApiOrder orderSn1 = this.getOne(new QueryWrapper<ApiOrder>().eq("orderSn", orderSn));
        if (orderSn1.getStatus() == 2) {
            return ResultUtils.success("取消订单成功");
        }
        // 更新库存状态信息表
        orderLockService.update(new UpdateWrapper<OrderLock>().eq("orderSn", orderSn).set("lockStatus",0).set("updateTime", new Date()));
        // 更新订单表状态
        this.update(new UpdateWrapper<ApiOrder>().eq("orderSn", orderSn).set("status", 2).set("updateTime", new Date()));
        // 解锁库存
        BaseResponse<Boolean> response = userFeignServices.unlockAvailablePieces(new LockChargingVo(interfaceId, orderNum));
        boolean updated = response.getData();
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "库存更新失败");
        }
        return ResultUtils.success("取消订单成功");
    }

    @Override
    public void orderPaySuccess(String orderSn) {
        this.update(new UpdateWrapper<ApiOrder>().eq("orderSn",orderSn).set("status",1));
    }

    @Override
    public BaseResponse getOrderEchartsData(List<String> dateList) {
        List<EchartsVo> list=apiOrderMapper.getOrderEchartsData(dateList);
        return ResultUtils.success(list);
    }

}




