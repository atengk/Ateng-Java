# 观察者模式

观察者模式定义对象间的一对多依赖关系，当一个对象状态改变时通知所有依赖者。Spring 中的事件机制（`ApplicationEvent` + `@EventListener`）是其体现。可用于异步消息、通知、任务联动等。
