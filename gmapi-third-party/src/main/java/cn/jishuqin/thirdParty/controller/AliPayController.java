package cn.jishuqin.thirdParty.controller;

import cn.jishuqin.thirdParty.dto.AliPay;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.common.ResultUtils;
import cn.jishuqin.common.constant.RedisConstant;
import cn.jishuqin.common.model.entity.AlipayInfo;
import cn.jishuqin.thirdParty.config.AliPayConfig;
import cn.jishuqin.thirdParty.service.AlipayInfoService;
import cn.jishuqin.thirdParty.utils.RabbitOrderUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static cn.jishuqin.thirdParty.config.AliPayConfig.*;
import static cn.jishuqin.thirdParty.config.AliPayConfig.GATEWAY_URL;


/**
 * @author 顾梦
 * @description 支付接口
 * @since 2023/9/3
 */
@Api(tags = "支付接口")
@RestController
@RequestMapping("/alipay")
@Slf4j
public class AliPayController {

    @Resource
    private AliPayConfig aliPayConfig;

    @Resource
    private AlipayInfoService alipayInfoService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RabbitOrderUtils rabbitOrderUtils;

    /**
     * 支付订单
     * @param aliPay
     * @return
     * @throws Exception
     */
    @ApiOperation("支付订单")
    @GetMapping("/pay")
    public BaseResponse<String> pay(AliPay aliPay) {
        AlipayClient alipayClient = new DefaultAlipayClient(GATEWAY_URL, aliPayConfig.getAppId(),
                aliPayConfig.getAppPrivateKey(), FORMAT, CHARSET, aliPayConfig.getAlipayPublicKey(), SIGN_TYPE);
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(aliPayConfig.getNotifyUrl());
        request.setBizContent("{\"out_trade_no\":\"" + aliPay.getTraceNo() + "\","
                + "\"total_amount\":\"" + aliPay.getTotalAmount() + "\","
                + "\"subject\":\"" + aliPay.getSubject() + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\","
                + "\"timeout_express\":\"30m\"}");  // 设置订单过期时间为30分钟
        String form = "";
        try {
            form = alipayClient.pageExecute(request).getBody(); // 调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return ResultUtils.success(form);
    }

    /**
     * 查询支付状态
     * @param outTradeNo
     * @return
     */
    @ApiOperation("查询支付状态")
    @GetMapping("/order/status")
    public String getOrderStatus(@RequestParam("out_trade_no") String outTradeNo) {
        AlipayClient alipayClient = new DefaultAlipayClient(GATEWAY_URL, aliPayConfig.getAppId(),
                aliPayConfig.getAppPrivateKey(), FORMAT, CHARSET, aliPayConfig.getAlipayPublicKey(), SIGN_TYPE);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{\"out_trade_no\":\"" + outTradeNo + "\"}");
        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                // 订单查询成功，处理返回的订单状态等信息
                return "订单状态：" + response.getTradeStatus();
            } else {
                // 订单查询失败，处理失败原因
                return "订单查询失败：" + response.getSubMsg();
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
            return "订单查询发生异常";
        }
    }

    /**
     * 完成订单通知
     * @param request
     * @return
     * @throws Exception
     */
    @ApiOperation("完成订单通知")
    @PostMapping("/notify")  // 注意这里必须是POST接口
    public String payNotify(HttpServletRequest request) throws Exception {

        if (request.getParameter("trade_status").equals("TRADE_SUCCESS")) {
            System.out.println("=========支付宝异步回调========");

            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String name : requestParams.keySet()) {
                params.put(name, request.getParameter(name));
            }

            // 支付宝验签
            boolean verify_result = AlipaySignature.rsaCheckV1(params, aliPayConfig.getAlipayPublicKey(), CHARSET, "RSA2");
            if (verify_result) {
                // 验签通过
                String subject = params.get("subject");
                String tradeStatus = params.get("trade_status");
                String tradeNo = params.get("trade_no");
                String outTradeNo = params.get("out_trade_no");
                String totalAmount = params.get("total_amount");
                String buyerId = params.get("buyer_id");
                String gmtPayment = params.get("gmt_payment");
                String buyerPayAmount = params.get("buyer_pay_amount");

                log.info("订单：{} 支付成功，付款金额：{}，付款时间：{}",outTradeNo,buyerPayAmount,gmtPayment);

                // 更新订单未已支付
                AlipayInfo alipayInfo = new AlipayInfo();
                alipayInfo.setOrderSn(outTradeNo);
                alipayInfo.setSubject(subject);
                alipayInfo.setTotalAmount(Double.parseDouble(totalAmount));
                alipayInfo.setBuyerPayAmount(Double.parseDouble(buyerPayAmount));
                alipayInfo.setBuyerId(buyerId);
                alipayInfo.setTradeNo(tradeNo);
                alipayInfo.setTradeStatus(tradeStatus);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                alipayInfo.setGmtPayment(dateFormat.parse(gmtPayment));
                alipayInfoService.save(alipayInfo);

                // 同时将交易结果存入redis中去，保证支付请求幂等性
                redisTemplate.opsForValue().set(RedisConstant.ALIPAY_TRADE_INFO+alipayInfo.getOrderSn(),alipayInfo,30, TimeUnit.MINUTES);
                // 修改数据库，完成整个订单功能
                rabbitOrderUtils.sendOrderPaySuccess(params.get("out_trade_no"));
            }
        }
        return "success";
    }

}
