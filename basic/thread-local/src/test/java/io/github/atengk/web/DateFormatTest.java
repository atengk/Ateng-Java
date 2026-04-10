package io.github.atengk.web;

import io.github.atengk.basic.util.DateFormatUtil;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 日期工具测试类
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class DateFormatTest {

    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 20; i++) {
            executor.execute(() -> {
                String dateStr = DateFormatUtil.format(new Date());
                System.out.println(Thread.currentThread().getName() + " -> " + dateStr);

                // 再解析回去
                Date date = DateFormatUtil.parse(dateStr);
                System.out.println(Thread.currentThread().getName() + " parse -> " + date);
            });
        }

        executor.shutdown();
    }
}