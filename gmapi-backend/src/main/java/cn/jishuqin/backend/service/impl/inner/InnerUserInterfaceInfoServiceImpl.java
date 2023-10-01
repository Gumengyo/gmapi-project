package cn.jishuqin.backend.service.impl.inner;

import cn.jishuqin.backend.mapper.UserInterfaceInfoMapper;
import cn.jishuqin.common.ErrorCode;
import cn.jishuqin.common.exception.BusinessException;
import cn.jishuqin.common.model.entity.UserInterfaceInfo;
import cn.jishuqin.common.service.InnerUserInterfaceInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.dubbo.config.annotation.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class InnerUserInterfaceInfoServiceImpl implements InnerUserInterfaceInfoService {

    @Resource
    private UserInterfaceInfoMapper userInterfaceInfoMapper;

    @Override
    @Transactional
    public boolean invokeCount(long interfaceInfoId, long userId) {
        // 判断
        if (interfaceInfoId <= 0 || userId <=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        return userInterfaceInfoMapper.invokeCount(interfaceInfoId,userId) > 0;
    }

    @Override
    public boolean checkCount(long interfaceInfoId, long userId) {

        QueryWrapper<UserInterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("interfaceInfoId",interfaceInfoId).eq("userId",userId);

        UserInterfaceInfo interfaceInfo = userInterfaceInfoMapper.selectOne(queryWrapper);
        Long leftNum = interfaceInfo.getLeftNum();

        // 判断是否有剩余次数
        return leftNum > 0;
    }

}
