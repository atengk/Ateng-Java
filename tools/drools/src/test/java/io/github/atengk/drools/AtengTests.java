package io.github.atengk.drools;

import io.github.atengk.drools.model.Order;
import io.github.atengk.drools.util.DroolsUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest
public class AtengTests {

    @Test
    public void test() {
        Order order = new Order(new BigDecimal("200"));

        DroolsUtil.execute("rules/demo.drl", order);

        System.out.println("最终折扣：" + order.getDiscount());
    }

}
