package cn.jishuqin.backend.service.impl.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import cn.jishuqin.common.ErrorCode;
import cn.jishuqin.common.exception.BusinessException;
import cn.jishuqin.common.model.entity.User;
import cn.jishuqin.common.service.InnerUserService;
import cn.jishuqin.backend.mapper.UserMapper;
import cn.jishuqin.backend.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Service;

import javax.annotation.Resource;

@Service
public class InnerUserServiceImpl implements InnerUserService{

    @Resource
    private UserMapper userMapper;

    @Override
    public User getInvokeUser(String accessKey) {
        if (StringUtils.isAnyBlank(accessKey)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("accessKey", accessKey);
        return userMapper.selectOne(queryWrapper);
    }

}
