package local.ateng.java.config.service;


import local.ateng.java.config.event.MyCustomEvent;

/**
 * 程序启动需要运行的代码
 */
public interface AppStartupService {
    void myBean();
    void event1();
    void event2(MyCustomEvent myCustomEvent);
}
