package cn.jishuqin.order.controller;

import cn.jishuqin.common.model.vo.EchartsVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.common.ErrorCode;
import cn.jishuqin.common.ResultUtils;
import cn.jishuqin.common.exception.BusinessException;
import cn.jishuqin.common.model.entity.ApiOrder;
import cn.jishuqin.order.model.dto.ApiOrderCancelDto;
import cn.jishuqin.order.model.dto.ApiOrderStatusInfoDto;
import cn.jishuqin.order.model.dto.OrderDto;
import cn.jishuqin.order.model.vo.ApiOrderStatusVo;
import cn.jishuqin.order.service.ApiOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@Api(tags = "订单接口")
@RequestMapping("/order")
public class OrderController {
    @Resource
    private ApiOrderService apiOrderService;

    /**
     * 生成防重令牌：保证创建订单的接口幂等性
     * @param id
     * @param response
     * @return
     */
    @ApiOperation("生成防重令牌")
    @GetMapping("/generateToken")
    public BaseResponse generateToken(Long id, HttpServletResponse response){
        return apiOrderService.generateToken(id,response);
    }

    @ApiOperation("生成订单")
    @PostMapping("/generateOrder")
    public BaseResponse<String> generateOrder(OrderDto orderDto, HttpServletRequest request) throws IOException {
        if (orderDto == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return apiOrderService.generateOrder(orderDto, request);
    }

    /**
     * 获取echarts图中最近7天的交易数
     * @return
     */
    @PostMapping("/getOrderEchartsData")
    public BaseResponse<List<EchartsVo>> getOrderEchartsData(@RequestBody List<String> dateList){
        return apiOrderService.getOrderEchartsData(dateList);
    }

    /**
     * 获取当前登录用户的status订单信息
     * @param statusInfoDto
     * @param request
     * @return
     */
    @ApiOperation("获取当前登录用户的status订单信息")
    @PostMapping("/getCurrentOrderInfo")
    public BaseResponse<Page<ApiOrderStatusVo>> getCurrentOrderInfo(ApiOrderStatusInfoDto statusInfoDto, HttpServletRequest request){
        if (statusInfoDto == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return apiOrderService.getCurrentOrderInfo(statusInfoDto,request);
    }

    /**
     * 取消订单
     * @param apiOrderCancelDto
     * @param request
     * @return
     */
    @ApiOperation("取消订单")
    @PostMapping("/cancelOrderSn")
    public BaseResponse<String> cancelOrderSn(ApiOrderCancelDto apiOrderCancelDto, HttpServletRequest request) {
        return apiOrderService.cancelOrderSn(apiOrderCancelDto,request);
    }

    /**
     * 获取全站成功交易数
     * @return
     */
    @ApiOperation("获取全站成功交易数")
    @GetMapping("/getSuccessOrder")
    public BaseResponse<Long> getSuccessOrder(){
        return ResultUtils.success(apiOrderService.count(new QueryWrapper<ApiOrder>().eq("status",1)));
    }

    /**
     * 获取最高成交额
     * @return
     */
    @ApiOperation("获取最高成交额")
    @GetMapping("/getTopOrder")
    public BaseResponse<Double> getTopOrder(){
        ApiOrder topOrder = apiOrderService.query().orderByDesc("totalAmount").eq("status",1).list().get(0);
        return ResultUtils.success(topOrder.getTotalAmount());
    }
}
