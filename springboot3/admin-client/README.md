# Spring Boot 3 Admin 监控 客户端

Spring Boot 3 Admin 是一个基于 Spring Boot 的应用监控和管理工具，可视化展示微服务的运行状态，包括健康检查、日志、线程、指标等信息。通过服务端和客户端依赖快速集成，便于开发者实时掌握应用状况，提升运维效率。



## 基础配置

**添加依赖**

```xml
        <!-- Spring Boot Admin 监控工具 -->
        <dependency>
            <groupId>de.codecentric</groupId>
            <artifactId>spring-boot-admin-starter-client</artifactId>
            <version>${spring-boot.version}</version>
        </dependency>
```

**编辑配置文件**

```yaml
server:
  port: 12005
  servlet:
    context-path: /
spring:
  main:
    web-application-type: servlet
  application:
    name: ${project.artifactId}
---
# 开启虚拟线程
spring:
  threads:
    virtual:
      enabled: true
---
# 监控配置
spring.boot.admin.client:
  # 启动客户端
  enabled: true
  # 设置 Spring Boot Admin Server 地址
  url: http://localhost:12004/admin/
  instance:
    service-host-type: IP
  username: admin
  password: Admin@123
---
# Actuator 监控端点的配置项
management:
  endpoints: # 配置Actuator端点的管理
    web:
      exposure: # 暴露端点
        include: '*'  # 包含所有端点，可以根据需要更改为具体的端点列表
  endpoint: # 配置各个端点的行为
    shutdown: # 关闭端点配置
      access: unrestricted # 启用shutdown端点，允许通过POST请求关闭应用程序
    health: # 健康检查端点配置
      show-details: ALWAYS # 显示完整的健康信息，包括详细的检查项
  info:
    env:
      enabled: true
```



## 访问服务

**访问Admin Server查看客户端**

```
URL: http://localhost:12004/admin
Username: admin
Password: Admin@123
```

**访问actuator端点**

查看健康状态

```
GET: http://localhost:12005/actuator/health
```

关闭应用

```
POST: http://localhost:12005/actuator/shutdown
```

