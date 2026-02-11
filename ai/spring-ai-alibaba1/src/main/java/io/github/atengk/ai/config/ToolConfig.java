package io.github.atengk.ai.config;

import io.github.atengk.ai.tool.TimeByZoneIdTool;
import io.github.atengk.ai.tool.WeatherTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfig {

    @Bean
    public ToolCallback weatherTool() {
        return FunctionToolCallback.builder("getWeather", new WeatherTool())
                .description("根据城市名称获取天气信息工具")
                .inputType(WeatherTool.Request.class)
                .build();
    }

    @Bean
    public ToolCallback timeByZoneIdTool() {
        return FunctionToolCallback.builder("getTimeByZoneId", new TimeByZoneIdTool())
                .description("根据时区获取当前时间工具")
                .inputType(TimeByZoneIdTool.Request.class)
                .build();
    }

}
