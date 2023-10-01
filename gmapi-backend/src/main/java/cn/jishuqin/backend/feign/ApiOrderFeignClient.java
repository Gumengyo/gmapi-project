package cn.jishuqin.backend.feign;

import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.common.model.vo.EchartsVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author gumeng
 */
@FeignClient(value = "gmapi-order",url = "http://47.113.220.20:7530/api/")
public interface ApiOrderFeignClient {

    /**
     * 获取echarts图中最近7天的交易数
     * @return
     */
    @PostMapping("/order/getOrderEchartsData")
    BaseResponse<List<EchartsVo>> getOrderEchartsData(@RequestBody List<String> dateList);
}
