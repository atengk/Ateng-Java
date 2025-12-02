package local.ateng.java.mybatisjdk8.controller;

import local.ateng.java.mybatisjdk8.utils.JdbcTemplateProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/jdbc")
@RequiredArgsConstructor
public class JDBCTemplateController {
    private final JdbcTemplateProvider jdbcTemplateProvider;

    @GetMapping("/getJdbcTemplate")
    public String getJdbcTemplate(String dataSourceName){
        JdbcTemplate jdbcTemplate = jdbcTemplateProvider.getJdbcTemplate(dataSourceName);
        return jdbcTemplate.queryForObject("SELECT VERSION()", String.class);
    }

}
