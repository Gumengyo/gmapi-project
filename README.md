<h1 align=center>
   Gm API 开放平台
</h1>
<p align="center">
<img src="doc/images/banner.png" alt="image-20230623213937364" style="zoom:200%;" align="center" />
</p>
<p align="center">
<a target="_blank" href="https://github.com/Gumengyo/gmapi-backend">
    	<img src="https://img.shields.io/hexpm/l/plug.svg" ></img>
		<img src="https://img.shields.io/badge/JDK-1.8-green.svg" ></img>
        <img src="https://img.shields.io/badge/nodejs-18.17.1-green" ></img>
        <img src="https://img.shields.io/badge/springboot-2.7.0-green" ></img>
        <img src="https://img.shields.io/badge/react-18.2.0-green" ></img>
		<img src="https://img.shields.io/badge/Umi-4.0.79-green" ></img>
        <img src="https://img.shields.io/badge/swagger-3.0.0-brightgreen" ></img>
        <img src="https://img.shields.io/badge/Dubbo-2.7.4.1-green" ></img>
        <img src="https://img.shields.io/badge/RabbitMQ-3.8.8-green" ></img>
		<img src="https://img.shields.io/badge/MySQL-5.7-green" ></img>
		<img src="https://img.shields.io/badge/Nacos-2.2.0-green" ></img>
		<img src="https://img.shields.io/badge/Redis-7.0.2-green" ></img>
</a></p>


## 1. 项目介绍

​		Gm API 开放平台，一个基于 React、Spring Boot、Redis、MySQL、RabbitMQ、Feign、Dubbo 和 Gateway 的 API开放平台，实现了管理员接入发布接口和可视化接口调用情况，用户可以购买接口调用权限、浏览接口及在线调试，并通过下载客户端 SDK 调用接口。

👉[接口文档](./doc/接口文档.md)📄

## 2. 模块介绍

1）**gmapi-backend：** 是项目的主要模块，主要包括用户相关、接口相关等功能

2）**gmapi-gateway：** 网关模块，具体实现了路由的转发、网关限流、API签名校验、接口统计以及请求和响应日志的处理

3）**gmapi-interface：** 提供的接口服务，后续这些可以接入来自各种的第三方的接口服务再自己进行扩展。

5）**gmapi-third-party：**为第三方服务，主要涉及到发送短信、支付宝沙箱支付功能

6）**gmapi-common：** 抽取一些重复使用的方法、统一抽取的公共实体，异常处理以及全局配置等

7）**gmapi-client-sdk：** 根据接口提供定制化的 SDK 调用，给用户提供简单，快捷，高效的接口调用体验

## 3. 在线体验

演示地址: http://api.jishuqin.cn

> 账号：user
>
> 密码：123456789

> 支付宝沙箱
>
> 账号：woalaf9618@sandbox.com
>
> 密码：111111
>
> 支付密码：111111



