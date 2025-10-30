package io.github.atengk.service.impl;

import io.github.atengk.service.AtengService;

import java.util.Map;

public class AtengServiceImpl implements AtengService {
    @Override
    public String hello() {
        return "Hello from Ateng！";
    }

    @Override
    public Map<String, String> getEnv() {
        return System.getenv();
    }
}
