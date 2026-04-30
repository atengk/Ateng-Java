package io.github.atengk.spring;

import io.github.atengk.utils.SpringUtil;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;

import static org.junit.jupiter.api.Assertions.*;

class AopProxyTest {

    @Test
    void shouldInspectAopProxy() {
        SpringUtilTestSupport.TestService target = new SpringUtilTestSupport.TestService("aop");
        ProxyFactory proxyFactory = new ProxyFactory(target);
        Object proxy = proxyFactory.getProxy();

        assertTrue(SpringUtil.isAopProxy(proxy));
        assertTrue(SpringUtil.isCglibProxy(proxy));
        assertFalse(SpringUtil.isJdkDynamicProxy(proxy));
        assertEquals(SpringUtilTestSupport.TestService.class, SpringUtil.getTargetClass(proxy));
        assertSame(target, SpringUtil.getTargetObject(proxy));
        assertEquals(SpringUtilTestSupport.TestService.class, SpringUtil.getUltimateTargetClass(proxy));
    }

    @Test
    void shouldHandleNonProxyAndNull() {
        SpringUtilTestSupport.TestService target = new SpringUtilTestSupport.TestService("plain");
        assertFalse(SpringUtil.isAopProxy(target));
        assertFalse(SpringUtil.isAopProxy(null));
        assertThrows(NullPointerException.class, () -> SpringUtil.getTargetClass(null));
    }
}
