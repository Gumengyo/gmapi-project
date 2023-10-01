package cn.jishuqin.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cn.jishuqin.common.model.entity.OrderLock;

/**
* @author 374943980_1941563569
* @description 针对表【order_lock(锁定订单)】的数据库操作Service
* @createDate 2023-09-06 21:56:06
*/
public interface OrderLockService extends IService<OrderLock> {

    /**
     * 修改库存占用状态
     * @param orderSn
     */
    void orderPaySuccess(String orderSn);
}
