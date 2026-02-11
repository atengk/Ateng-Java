package io.github.atengk.ai.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.function.Function;

/**
 * 根据城市名称获取天气信息工具
 *
 * @author 孔余
 * @since 2026-02-11
 */
public class WeatherTool
        implements Function<WeatherTool.Request, WeatherTool.Response> {

    @Override
    public WeatherTool.Response apply(WeatherTool.Request request) {

        String city = request.city;

        // 这里是模拟天气数据，实际项目可以改成调用第三方天气 API
        String weather = "晴";
        String temperature = "25°";

        return new Response(
                String.format("%s的天气是%s，温度%s",
                        city,
                        weather,
                        temperature));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("根据城市名称获取天气信息")
    public record Request(
            @JsonProperty(required = true, value = "city")
            @JsonPropertyDescription("城市名称，例如 重庆、北京")
            String city) {
    }

    @JsonClassDescription("天气查询结果")
    public record Response(String description) {
    }

}
