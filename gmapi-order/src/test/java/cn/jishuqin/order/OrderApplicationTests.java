package cn.jishuqin.order;

import cn.jishuqin.common.BaseResponse;
import cn.jishuqin.common.model.dto.LeftNumUpdateDto;
import cn.jishuqin.common.service.InnerUserInterfaceInfoService;
import cn.jishuqin.order.feign.UserFeignServices;
import org.apache.dubbo.config.annotation.Reference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class OrderApplicationTests {

    @Autowired
    private UserFeignServices userFeignServices;

    @Test
    void testUpdateUserLeftNum() {
        LeftNumUpdateDto leftNumUpdateDto = new LeftNumUpdateDto();
        leftNumUpdateDto.setUserId(6L);
        leftNumUpdateDto.setInterfaceInfoId(6L);
        leftNumUpdateDto.setLockNum(10L);

        BaseResponse<Boolean> booleanBaseResponse = userFeignServices.updateUserLeftNum(leftNumUpdateDto);
        System.out.println(booleanBaseResponse);
    }
}
