# 可观测

## OpenTelemetry

- [Github文档](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
- [使用文档](https://kongyu666.github.io/ops/#/work/service/opentelemetry/)
- [Jaeger使用文档](https://kongyu666.github.io/ops/#/work/service/jaeger/)

**添加依赖**

添加actuator和micrometer-tracing依赖，使日志中有TraceID和SpanID

```xml
<!-- Spring Boot Actuator（提供追踪端点） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer Tracing 核心库，提供 度量（Metrics）和追踪（Tracing）-->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing</artifactId>
</dependency>
```

**下载Agent**

```
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.14.0/opentelemetry-javaagent.jar
```

**启动服务**

将应用打包后，使用Otel的Agent运行应用并使用OTLP协议的gRPC端口对接到Otel Collector中

```bash
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=spring-cloud-gateway \
     -Dotel.traces.exporter=otlp \
     -Dotel.metrics.exporter=otlp \
     -Dotel.logs.exporter=otlp \
     -Dotel.exporter.otlp.protocol=grpc \
     -Dotel.exporter.otlp.endpoint=http://192.168.1.12:4317 \
     -jar spring-cloud-gateway-v1.0.jar
```


