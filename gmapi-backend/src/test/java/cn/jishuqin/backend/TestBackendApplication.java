package cn.jishuqin.backend;

import cn.jishuqin.backend.service.InterfaceInfoService;
import cn.jishuqin.backend.service.UserInterfaceInfoService;
import cn.jishuqin.common.model.dto.LeftNumUpdateDto;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class TestBackendApplication {

    @Resource
    private UserInterfaceInfoService userInterfaceInfoService;

    @Resource
    private InterfaceInfoService interfaceInfoService;

    @Test
    void testUpdateUserLeftNum() {
        LeftNumUpdateDto leftNumUpdateDto = new LeftNumUpdateDto();
        leftNumUpdateDto.setUserId(6L);
        leftNumUpdateDto.setInterfaceInfoId(6L);
        leftNumUpdateDto.setLockNum(10L);

        boolean updated = userInterfaceInfoService.updateUserLeftNum(leftNumUpdateDto);
        Assert.assertTrue(updated);
    }

}
