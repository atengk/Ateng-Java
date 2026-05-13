# MySQL 8 常用业务建模模型

## [建模基础规范](/database/mysql/modeling/basic-standards)

- [建模基础规范](/database/mysql/modeling/basic-standards#建模基础规范)
- [主键设计模型](/database/mysql/modeling/basic-standards#主键设计模型)
- [业务单号模型](/database/mysql/modeling/basic-standards#业务单号模型)
- [基础字段模型](/database/mysql/modeling/basic-standards#基础字段模型)

## [基础关系模型](/database/mysql/modeling/basic-relation-model)

- [单表模型](/database/mysql/modeling/basic-relation-model#单表模型)
- [主从表模型](/database/mysql/modeling/basic-relation-model#主从表模型)
- [一对一模型](/database/mysql/modeling/basic-relation-model#一对一模型)
- [一对多模型](/database/mysql/modeling/basic-relation-model#一对多模型)
- [多对多模型](/database/mysql/modeling/basic-relation-model#多对多模型)

## [通用字典与配置模型](/database/mysql/modeling/common-dictionary-and-configuration-model)

- [字典表模型](/database/mysql/modeling/common-dictionary-and-configuration-model#字典表模型)
- [配置项模型](/database/mysql/modeling/common-dictionary-and-configuration-model#配置项模型)
- [状态机模型](/database/mysql/modeling/common-dictionary-and-configuration-model#状态机模型)
- [草稿-发布模型](/database/mysql/modeling/common-dictionary-and-configuration-model#草稿-发布模型)

## [层级、分类与资源模型](/database/mysql/modeling/hierarchy-category-resource-model)

- [树形层级模型](/database/mysql/modeling/hierarchy-category-resource-model#树形层级模型)
- [分类模型](/database/mysql/modeling/hierarchy-category-resource-model#分类模型)
- [标签模型](/database/mysql/modeling/hierarchy-category-resource-model#标签模型)
- [附件资源模型](/database/mysql/modeling/hierarchy-category-resource-model#附件资源模型)

## [用户、权限与组织模型](/database/mysql/modeling/user-permission-organization-model)

- [用户-角色-权限模型](/database/mysql/modeling/user-permission-organization-model#用户-角色-权限模型)
- [数据权限模型](/database/mysql/modeling/user-permission-organization-model#数据权限模型)
- [组织架构模型](/database/mysql/modeling/user-permission-organization-model#组织架构模型)
- [岗位模型](/database/mysql/modeling/user-permission-organization-model#岗位模型)

## [商品、交易与支付模型](/database/mysql/modeling/product-transaction-payment-model)

- [商品-SPU-SKU模型](/database/mysql/modeling/product-transaction-payment-model#商品-spu-sku模型)
- [订单-订单明细模型](/database/mysql/modeling/product-transaction-payment-model#订单-订单明细模型)
- [支付单模型](/database/mysql/modeling/product-transaction-payment-model#支付单模型)
- [退款单模型](/database/mysql/modeling/product-transaction-payment-model#退款单模型)

## [账户、库存与流水模型](/database/mysql/modeling/account-inventory-ledger-model)

- [账户流水模型](/database/mysql/modeling/account-inventory-ledger-model#账户流水模型)
- [库存模型](/database/mysql/modeling/account-inventory-ledger-model#库存模型)
- [库存流水模型](/database/mysql/modeling/account-inventory-ledger-model#库存流水模型)

## [并发控制与防重模型](/database/mysql/modeling/concurrency-control-and-duplicate-prevention-model)

- [乐观锁模型](/database/mysql/modeling/concurrency-control-and-duplicate-prevention-model#乐观锁模型)
- [悲观锁模型](/database/mysql/modeling/concurrency-control-and-duplicate-prevention-model#悲观锁模型)
- [唯一约束防重模型](/database/mysql/modeling/concurrency-control-and-duplicate-prevention-model#唯一约束防重模型)
- [幂等模型](/database/mysql/modeling/concurrency-control-and-duplicate-prevention-model#幂等模型)

## [扩展字段与读模型](/database/mysql/modeling/extension-fields-and-read-models)

- [冗余字段模型](/database/mysql/modeling/extension-fields-and-read-models#冗余字段模型)
- [快照模型](/database/mysql/modeling/extension-fields-and-read-models#快照模型)
- [宽表模型](/database/mysql/modeling/extension-fields-and-read-models#宽表模型)
- [JSON扩展字段模型](/database/mysql/modeling/extension-fields-and-read-models#json扩展字段模型)
- [EAV动态属性模型](/database/mysql/modeling/extension-fields-and-read-models#eav动态属性模型)

## [多租户与数据隔离模型](/database/mysql/modeling/multi-tenant-and-data-isolation-model)

- [多租户模型](/database/mysql/modeling/multi-tenant-and-data-isolation-model#多租户模型)

## [历史版本与数据变更模型](/database/mysql/modeling/historical-version-and-data-change-model)

- [历史版本模型](/database/mysql/modeling/historical-version-and-data-change-model#历史版本模型)
- [数据变更记录模型](/database/mysql/modeling/historical-version-and-data-change-model#数据变更记录模型)

## [日志与审计模型](/database/mysql/modeling/log-and-audit-model)

- [操作日志模型](/database/mysql/modeling/log-and-audit-model#操作日志模型)
- [审计日志模型](/database/mysql/modeling/log-and-audit-model#审计日志模型)
- [登录日志模型](/database/mysql/modeling/log-and-audit-model#登录日志模型)
- [接口调用日志模型](/database/mysql/modeling/log-and-audit-model#接口调用日志模型)

## [消息与事件模型](/database/mysql/modeling/message-and-event-model)

- [消息事件模型](/database/mysql/modeling/message-and-event-model#消息事件模型)
- [Outbox事件表模型](/database/mysql/modeling/message-and-event-model#outbox事件表模型)

## [统计、搜索与查询优化模型](/database/mysql/modeling/statistics-search-query-optimization-model)

- [统计汇总模型](/database/mysql/modeling/statistics-search-query-optimization-model#统计汇总模型)
- [搜索辅助表模型](/database/mysql/modeling/statistics-search-query-optimization-model#搜索辅助表模型)
- [读写分离模型](/database/mysql/modeling/statistics-search-query-optimization-model#读写分离模型)

## [数据生命周期模型](/database/mysql/modeling/data-lifecycle-model)

- [软删除模型](/database/mysql/modeling/data-lifecycle-model#软删除模型)
- [归档数据模型](/database/mysql/modeling/data-lifecycle-model#归档数据模型)
- [冷热数据模型](/database/mysql/modeling/data-lifecycle-model#冷热数据模型)

## [大数据量架构模型](/database/mysql/modeling/large-data-volume-architecture-model)

- [分区表模型](/database/mysql/modeling/large-data-volume-architecture-model#分区表模型)
- [分库分表模型](/database/mysql/modeling/large-data-volume-architecture-model#分库分表模型)