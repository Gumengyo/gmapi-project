package cn.jishuqin.backend.service;

import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.common.model.vo.LockChargingVo;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.jishuqin.common.model.entity.InterfaceInfo;

/**
* @author gumeng
* @description 针对表【interface_info(接口信息)】的数据库操作Service
* @createDate 2023-04-23 19:19:45
*/
public interface InterfaceInfoService extends IService<InterfaceInfo> {

    /**
     * 验证接口
     * @param interfaceInfo
     * @param add
     */
    void validInterfaceInfo(InterfaceInfo interfaceInfo, boolean add);


    /**
     * 更新库存
     * @param lockChargingVo
     * @return
     */
    boolean updateAvailablePieces(LockChargingVo lockChargingVo);

    /**
     * 远程解锁库存
     * @param lockChargingVo
     * @return
     */
    boolean unlockAvailablePieces(LockChargingVo lockChargingVo);
}
