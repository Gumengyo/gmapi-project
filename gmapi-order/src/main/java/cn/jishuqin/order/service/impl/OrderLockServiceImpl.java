package cn.jishuqin.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.jishuqin.common.model.entity.OrderLock;
import cn.jishuqin.order.mapper.OrderLockMapper;
import cn.jishuqin.order.service.OrderLockService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
* @author 374943980_1941563569
* @description 针对表【order_lock(锁定订单)】的数据库操作Service实现
* @createDate 2023-09-06 21:56:06
*/
@Service
public class OrderLockServiceImpl extends ServiceImpl<OrderLockMapper, OrderLock>
    implements OrderLockService {

    /**
     * 修改状态
     * @param orderSn
     */
    @Override
    public void orderPaySuccess(String orderSn) {
        this.update(new UpdateWrapper<OrderLock>().eq("orderSn",orderSn).set("lockStatus",2).set("updateTime",new Date()));
    }
}




