package cn.jishuqin.backend;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@MapperScan("cn.jishuqin.backend.mapper")
@EnableDubbo
@EnableDiscoveryClient
@EnableFeignClients
public class BackendApplication {

    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(BackendApplication.class, args);
    }

    /**
     * 让RabbitMQ转为JSON发送数据
     *
     * @return
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
