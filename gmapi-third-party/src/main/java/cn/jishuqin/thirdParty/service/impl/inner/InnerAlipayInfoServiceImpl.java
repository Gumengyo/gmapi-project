package cn.jishuqin.thirdParty.service.impl.inner;

import cn.jishuqin.common.model.entity.AlipayInfo;
import cn.jishuqin.common.service.InnerAlipayInfoService;
import cn.jishuqin.thirdParty.service.AlipayInfoService;
import org.apache.dubbo.config.annotation.Service;

import javax.annotation.Resource;

@Service
public class InnerAlipayInfoServiceImpl implements InnerAlipayInfoService {

    @Resource
    private AlipayInfoService alipayInfoService;

    @Override
    public AlipayInfo getByOrderSn(String orderSn) {
        return alipayInfoService.getById(orderSn);
    }
}
