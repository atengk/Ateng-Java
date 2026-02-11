package io.github.atengk.ai.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/**
 * 根据时区获取当前时间工具
 *
 * @author 孔余
 * @since 2026-02-11
 */
public class TimeByZoneIdTool
        implements Function<TimeByZoneIdTool.Request, TimeByZoneIdTool.Response> {

    @Override
    public TimeByZoneIdTool.Response apply(TimeByZoneIdTool.Request request) {
        String timeZoneId = request.timeZoneId;
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of(timeZoneId));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String currentTime = zonedDateTime.format(formatter);
        return new Response(String.format("当前时区为 %s，当前时间为 %s",
                timeZoneId,
                currentTime));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("根据时区ID获取当前时间")
    public record Request(
            @JsonProperty(required = true, value = "timeZoneId")
            @JsonPropertyDescription("时区ID，例如 Asia/Shanghai")
            String timeZoneId) {
    }

    @JsonClassDescription("根据时区ID获取时间的响应结果")
    public record Response(String description) {
    }

}

