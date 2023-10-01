package cn.jishuqin.backend.service.impl;

import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.common.model.vo.LockChargingVo;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.jishuqin.common.ErrorCode;
import cn.jishuqin.common.exception.BusinessException;
import cn.jishuqin.common.model.entity.InterfaceInfo;
import cn.jishuqin.backend.mapper.InterfaceInfoMapper;
import cn.jishuqin.backend.service.InterfaceInfoService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @author gumeng
 * @description 针对表【interface_info(接口信息)】的数据库操作Service实现
 * @createDate 2023-04-23 19:19:45
 */
@Service
public class InterfaceInfoServiceImpl extends ServiceImpl<InterfaceInfoMapper, InterfaceInfo>
        implements InterfaceInfoService {

    @Override
    public void validInterfaceInfo(InterfaceInfo interfaceInfo, boolean add) {
        if (interfaceInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String name = interfaceInfo.getName();
        // 创建时，所有参数必须非空
        if (add) {
            if (StringUtils.isAnyBlank(name)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
        if (StringUtils.isNotBlank(name) && name.length() > 50) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "名称过长");
        }
    }

    @Override
    public boolean updateAvailablePieces(LockChargingVo lockChargingVo) {
        Long interfaceid = lockChargingVo.getInterfaceid();
        Long orderNum = lockChargingVo.getOrderNum();
        return this.update().setSql("availablePieces = availablePieces - " + orderNum).eq("id", interfaceid).update();
    }

    @Override
    public boolean unlockAvailablePieces(LockChargingVo lockChargingVo) {
        Long orderNum = lockChargingVo.getOrderNum();
        Long interfaceid = lockChargingVo.getInterfaceid();
        if (orderNum == null || interfaceid == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return this.update().setSql("availablePieces = availablePieces + " + orderNum)
                .eq("id", interfaceid).update();
    }

}





