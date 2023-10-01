package cn.jishuqin.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.common.model.entity.ApiOrder;
import cn.jishuqin.order.model.dto.ApiOrderCancelDto;
import cn.jishuqin.order.model.dto.ApiOrderStatusInfoDto;
import cn.jishuqin.order.model.dto.OrderDto;
import cn.jishuqin.order.model.vo.ApiOrderStatusVo;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
* @author gumeng
* @description 针对表【api_order(订单信息)】的数据库操作Service
* @createDate 2023-09-04 23:32:22
*/
public interface ApiOrderService extends IService<ApiOrder> {
    /**
     * 生成防重令牌：保证创建订单的接口幂等性
     * @param id
     * @param response
     * @return
     */
    BaseResponse generateToken(Long id, HttpServletResponse response);

    /**
     * 创建订单
     * @param orderDto
     * @param request
     * @return
     */
    BaseResponse<String> generateOrder(OrderDto orderDto, HttpServletRequest request) throws IOException;


    /**
     * 获取订单
     * @param statusInfoDto
     * @param request
     * @return
     */
    BaseResponse<Page<ApiOrderStatusVo>> getCurrentOrderInfo(ApiOrderStatusInfoDto statusInfoDto, HttpServletRequest request);

    /**
     * 取消订单
     * @param apiOrderCancelDto
     * @param request
     * @return
     */
    BaseResponse<String> cancelOrderSn(ApiOrderCancelDto apiOrderCancelDto, HttpServletRequest request);

    /**
     * 修改订单状态
     * @param orderSn
     */
    void orderPaySuccess(String orderSn);

    /**
     * 获取echarts图中最近7天的交易数
     * @param dateList
     * @return
     */
    BaseResponse getOrderEchartsData(List<String> dateList);
}
