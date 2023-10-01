package cn.jishuqin.thirdParty.utils;

import cn.jishuqin.common.ErrorCode;
import cn.jishuqin.common.exception.BusinessException;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.tea.TeaException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

import static cn.jishuqin.common.constant.UserConstant.LOGIN_CODE_KEY;
import static cn.jishuqin.common.constant.UserConstant.LOGIN_CODE_TTL;


@Slf4j
@Component
@Data
@ConfigurationProperties(prefix = "alisms")
public class AliyunSmsUtils {

    private String accessKeyId;
    private String accessKeySecret;
    private String signName;
    private String templateCode;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 使用AK&SK初始化账号Client
     * @param accessKeyId
     * @param accessKeySecret
     * @return Client
     * @throws Exception
     */
    public static com.aliyun.dysmsapi20170525.Client createClient(String accessKeyId, String accessKeySecret) throws Exception {
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                // 您的 AccessKey ID
                .setAccessKeyId(accessKeyId)
                // 您的 AccessKey Secret
                .setAccessKeySecret(accessKeySecret);
        // 访问的域名
        config.endpoint = "dysmsapi.aliyuncs.com";
        return new com.aliyun.dysmsapi20170525.Client(config);
    }

    /**
     * 发送短信
     * @param phone 手机号
     * @param code 参数
     */
    public SendSmsResponse sendMessage(String phone, String code) throws Exception {

        com.aliyun.dysmsapi20170525.Client client = AliyunSmsUtils.createClient(accessKeyId, accessKeySecret);
        com.aliyun.dysmsapi20170525.models.SendSmsRequest sendSmsRequest = new com.aliyun.dysmsapi20170525.models.SendSmsRequest()
                .setPhoneNumbers(phone)
                .setSignName(signName)
                .setTemplateCode(templateCode)
                .setTemplateParam("{\"code\":\""+code+"\"}");
        com.aliyun.teautil.models.RuntimeOptions runtime = new com.aliyun.teautil.models.RuntimeOptions();
        SendSmsResponse sendSmsResponse = null;
        try {
            // 复制代码运行请自行打印 API 的返回值
            sendSmsResponse = client.sendSmsWithOptions(sendSmsRequest, runtime);
        } catch (TeaException error) {
            // 如有需要，请打印 error
            com.aliyun.teautil.Common.assertAsString(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            // 如有需要，请打印 error
            com.aliyun.teautil.Common.assertAsString(error.message);
        }
        return sendSmsResponse;
    }

}
