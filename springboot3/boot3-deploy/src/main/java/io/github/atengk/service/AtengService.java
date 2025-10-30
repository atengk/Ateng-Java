package io.github.atengk.service;

import java.util.Map;

/**
 * 阿腾服务接口类
 *
 * @author 孔余
 * @since 2025-10-30
 */
public interface AtengService {

    /**
     * Hello
     * @return 欢迎语
     */
    String hello();

    /**
     * 获取系统环境变量
     *
     * @return 以Map返回所有环境变量
     */
    Map<String, String> getEnv();

}
