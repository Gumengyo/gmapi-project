package cn.jishuqin.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.jishuqin.common.model.entity.ApiOrder;
import cn.jishuqin.common.model.vo.EchartsVo;

import java.util.List;

/**
* @author gumeng
* @description 针对表【api_order(订单信息)】的数据库操作Mapper
* @createDate 2023-09-04 23:32:22
* @Entity generator.domain.ApiOrder
*/
public interface ApiOrderMapper extends BaseMapper<ApiOrder> {

    /**
     * 获取订单统计数据
     * @param dateList
     * @return
     */
    List<EchartsVo> getOrderEchartsData(List<String> dateList);
}




