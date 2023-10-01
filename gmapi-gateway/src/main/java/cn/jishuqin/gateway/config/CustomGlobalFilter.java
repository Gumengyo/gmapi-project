package cn.jishuqin.gateway.config;

import cn.hutool.core.text.AntPathMatcher;
import cn.hutool.http.HttpRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.common.model.entity.InterfaceInfo;
import cn.jishuqin.common.model.entity.User;
import cn.jishuqin.common.service.InnerInterfaceInfoService;
import cn.jishuqin.common.service.InnerUserInterfaceInfoService;
import cn.jishuqin.common.service.InnerUserService;
import com.gumeng.gmapiclientsdk.utils.SignUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.Reference;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 顾梦
 * @create 2023/5/7
 * 全局过滤
 */
@Slf4j
@Component
public class CustomGlobalFilter implements GlobalFilter, Ordered {

    @Reference
    private InnerUserService innerUserService;

    @Reference
    private InnerInterfaceInfoService innerInterfaceInfoService;

    @Reference
    private InnerUserInterfaceInfoService innerUserInterfaceInfoService;

    private static final List<String>  PATH_WHITE_LIST = Arrays.asList("/api/user/**","/api/alipay/notify");

    //需要登录才能进行访问
    private static final List<String> PATH_LOGIN_LIST = Arrays.asList("/api/userInterfaceInfo/**" ,"/api/interfaceInfo/**","/api/key/**","/api/analysis/**","/api/order/**","/api/alipay/**");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //  1.请求日志
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String method = request.getMethod().toString();
        ServerHttpResponse response = exchange.getResponse();
        logPrint(request);
        //查询用户是否登录时、用户登录等请求，直接放行
        List<Boolean> collect = PATH_WHITE_LIST.stream().map(item -> {
            AntPathMatcher antPathMatcher = new AntPathMatcher();
            return antPathMatcher.match(item, path);
        }).collect(Collectors.toList());
        if(collect.contains(true)){
            return chain.filter(exchange);
        }
        // 2、网关统一鉴权，其他接口需要判断用户是否登录
        List<Boolean> collectLogin = PATH_LOGIN_LIST.stream().map(item -> {
            AntPathMatcher antPathMatcher = new AntPathMatcher();
            return antPathMatcher.match(item, path);
        }).collect(Collectors.toList());
        HttpHeaders headers = request.getHeaders();
        String cookie = headers.getFirst("Cookie");
        if (collectLogin.contains(true)){
            String loginUserVo = HttpRequest.get("http://192.168.0.234:7529/api/user/checkUserLogin")
                    .header("Cookie", cookie)
                    .timeout(20000)
                    .execute().body();
            Gson gson = new Gson();
            BaseResponse baseResponse = gson.fromJson(loginUserVo, BaseResponse.class);
            Object data = baseResponse.getData();
            if (null == data){
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return response.setComplete();
            }else {
                return chain.filter(exchange);
            }
        }

        //  3.用户鉴权（判断ak、sk是否合法）
        String accessKey = headers.getFirst("accessKey");
        String nonce = headers.getFirst("nonce");
        String timestamp = headers.getFirst("timestamp");
        String sign = headers.getFirst("sign");
        String body = null;
        if ("POST".equals(method) || "post".equals(method)) {
            try {
                body = URLDecoder.decode(headers.getFirst("body"), "utf-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        // 去数据库中查询是否已分配给用户
        User invokeUser = null;
        try{
            invokeUser = innerUserService.getInvokeUser(accessKey);
        }catch (Exception e){
            log.error("getInvokeUser error",e);
        }

        if (invokeUser == null){
            return handleNoAuth(response);
        }

        if (Long.parseLong(nonce) > 10000){
            return handleNoAuth(response);
        }
        // 时间和当前时间不能超过 5 分钟
        Long currentTime = System.currentTimeMillis() / 1000;
        final Long FIVE_MINUTES = 60 * 5L;
        if ((currentTime - Long.parseLong(timestamp)) >= FIVE_MINUTES){
            return handleNoAuth(response);
        }

        // 实际情况中是从数据库中查出 secretKey
        String secretKey = invokeUser.getSecretKey();
        String serverSign = SignUtils.getSign(body, secretKey);
        if (sign == null || !sign.equals(serverSign)){
            return handleNoAuth(response);
        }
        //  4.请求的模拟接口是否存在,以及请求方法是否匹配
        InterfaceInfo interfaceInfo = null;
        try{
            interfaceInfo = innerInterfaceInfoService.getInterfaceInfo(path.replace("/api/interface",""), method);
        }catch (Exception e){
            log.error("getInterfaceInfo error",e);
        }
        if (interfaceInfo == null){
            return handleNoAuth(response);
        }
        Long interfaceInfoId = interfaceInfo.getId();
        Long userId = invokeUser.getId();
        // 是否还有调用次数
        boolean checkCount = innerUserInterfaceInfoService.checkCount(interfaceInfoId, userId);
        if(!checkCount){
            //调用次数不足，自定义返回结果
            response.setStatusCode(HttpStatus.FORBIDDEN);
            DataBufferFactory bufferFactory = response.bufferFactory();
            ObjectMapper objectMapper = new ObjectMapper();
            DataBuffer wrap = null;
            try {
                wrap = bufferFactory.wrap(objectMapper.writeValueAsBytes(new BaseResponse<>(403, "","接口调用次数不足，禁止调用，请购买次数！")));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            DataBuffer finalWrap = wrap;
            return response.writeWith(Mono.fromSupplier(() -> finalWrap));
        }

        return handleResponse(exchange,chain,interfaceInfoId, userId);

    }

    /**
     * 打印基本日志信息
     *
     * @param request
     */
    private void logPrint(ServerHttpRequest request) {
        log.info("=====  {} 请求开始 =====", request.getId());
        String path = request.getPath().value();
        String method = request.getMethod().toString();
        log.info("请求路径：" + path);
        log.info("请求方法：" + method);
        log.info("请求参数：" + request.getQueryParams());
        String sourceAddress = request.getLocalAddress().getHostString();
        log.info("请求来源地址：" + sourceAddress);
    }

    /**
     * 处理响应
     * @param exchange
     * @param chain
     * @return
     */
    public Mono<Void> handleResponse(ServerWebExchange exchange,GatewayFilterChain chain,long interfaceInfoId,long userId){
        try {
            ServerHttpResponse originalResponse = exchange.getResponse();
            // 缓存数据的工厂
            DataBufferFactory bufferFactory = originalResponse.bufferFactory();
            // 拿到响应码
            HttpStatus statusCode = originalResponse.getStatusCode();

            if(statusCode == HttpStatus.OK){
                ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {

                    @Override
                    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                        log.info("body instanceof Flux: {}", (body instanceof Flux));
                        if (body instanceof Flux) {
                            Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                            // 往返回值里写数据
                            return super.writeWith(fluxBody.map(dataBuffer -> {
                                //  7. 调用成功，接口调用次数+1 invokeCount
                                try {
                                    innerUserInterfaceInfoService.invokeCount(interfaceInfoId,userId);
                                } catch (Exception e) {
                                    log.error("invokeCount error",e);
                                }
                                byte[] content = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(content);
                                DataBufferUtils.release(dataBuffer);//释放掉内存
                                // 构建日志
                                StringBuilder sb2 = new StringBuilder(200);
                                List<Object> rspArgs = new ArrayList<>();
                                rspArgs.add(originalResponse.getStatusCode());
                                //rspArgs.add(requestUrl);
                                String data = new String(content, StandardCharsets.UTF_8);//data
                                sb2.append(data);
                                log.info("响应结果：" + data);
                                return bufferFactory.wrap(content);
                            }));
                        } else {
                            // 8. 调用失败，返回一个规范的错误码
                            log.error("<--- {} 响应code异常", getStatusCode());
                        }
                        return super.writeWith(body);
                    }
                };
                // 设置 response 对象为装饰过的
                return chain.filter(exchange.mutate().response(decoratedResponse).build());
            }
            return chain.filter(exchange);//降级处理返回数据
        }catch (Exception e){
            log.error("网关处理响应异常.\n" + e);
            return chain.filter(exchange);
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    public Mono<Void> handleNoAuth(ServerHttpResponse response){
        response.setStatusCode(HttpStatus.FORBIDDEN);
        return response.setComplete();
    }

}