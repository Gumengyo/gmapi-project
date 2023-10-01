package cn.jishuqin.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cn.jishuqin.common.model.entity.User;
import cn.jishuqin.common.model.vo.EchartsVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Entity cn.jishuqin.project.model.model.entity.User
 */
public interface UserMapper extends BaseMapper<User> {
    List<EchartsVo> getUserList(@Param("dateList") List<String> dateList);
    String getUserPhone(@Param("username") String username);
}




