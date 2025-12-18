package io.github.atengk.beetl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {
    private final JdbcTemplate mysqlJdbcTemplate;
    private final JdbcTemplate postgresqlJdbcTemplate;

    @GetMapping("/mysqlPrimary")
    public String mysqlPrimary() {
        return mysqlJdbcTemplate.queryForObject("SELECT NOW()", String.class);
    }

    @GetMapping("/postgresqlSecondary")
    public String postgresqlSecondary() {
        return postgresqlJdbcTemplate.queryForObject("SELECT NOW()", String.class);
    }

}
