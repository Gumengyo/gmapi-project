package cn.jishuqin.thirdParty.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.jishuqin.common.model.entity.AlipayInfo;
import cn.jishuqin.thirdParty.mapper.AlipayInfoMapper;
import cn.jishuqin.thirdParty.service.AlipayInfoService;
import org.springframework.stereotype.Service;

/**
* @author 顾梦
* @description 针对表【alipay_info(订单支付信息)】的数据库操作Service实现
* @createDate 2023-09-04 20:22:59
*/
@Service
public class AlipayInfoServiceImpl extends ServiceImpl<AlipayInfoMapper, AlipayInfo>
    implements AlipayInfoService {

}




