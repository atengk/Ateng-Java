# 分布式链路追踪

**普通微服务**

```
java -javaagent:/usr/local/software/skywalking-agent/skywalking-agent.jar \
     -Dskywalking.agent.service_name=spring-cloud-nacos \
     -Dskywalking.collector.backend_service=192.168.1.10:8128 \
     -jar spring-cloud-nacos-v1.0.jar
```

**网关**

将 optional-plugins 目录下的 `apm-spring-cloud-gateway-4.x-plugin-9.4.0.jar` 和 `apm-spring-webflux-6.x-plugin-9.4.0.jar` 拷贝到 plugins 目录下

```
java -javaagent:/usr/local/software/skywalking-agent/skywalking-agent.jar \
     -Dskywalking.agent.service_name=spring-cloud-gateway \
     -Dskywalking.collector.backend_service=192.168.1.10:8128 \
     -jar spring-cloud-gateway-v1.0.jar
```

