import {defineConfig} from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
    base: '/Ateng-Java',
    title: "后端技术网站",
    description: "",
    themeConfig: {
        siteTitle: '阿腾集团',
        logo: '/logo.svg',

        // https://vitepress.dev/reference/default-theme-config
        nav: [
            {text: '首页', link: '/'},

            {
                text: 'Java',
                items: [
                    {text: 'Java 基础', link: '/basic/'},
                    {text: '工具相关', link: '/tools/'},
                    {text: '设计模式', link: '/designpattern/'}
                ]
            },

            {
                text: 'SpringBoot',
                items: [
                    {text: 'SpringBoot2', link: '/springboot2/'},
                    {text: 'SpringBoot3', link: '/springboot3/'},
                    {text: 'SpringBoot4', link: '/springboot4/'}
                ]
            },

            {
                text: '中间件',
                items: [
                    {text: '数据库', link: '/database/'},
                    {text: '缓存', link: '/cache/'},
                    {text: '消息队列', link: '/mq/'}
                ]
            },

            {
                text: '微服务',
                items: [
                    {text: '分布式', link: '/distributed/'},
                    {text: '任务调度', link: '/task/'},
                    {text: '实时通信', link: '/realtime/'}
                ]
            },

            {
                text: '生态',
                items: [
                    {text: 'HTTP', link: '/http/'},
                    {text: '存储', link: '/storage/'},
                    {text: '支付', link: '/pay/'},
                    {text: '权限认证', link: '/auth/'}
                ]
            },

            {
                text: 'AI',
                items: [
                    {text: 'Ollama', link: '/ai/ollama/'},
                    {text: 'Spring AI 相关', link: '/ai/'},
                ]
            },

            {
                text: '大数据',
                items: [
                    {text: '大数据', link: '/bigdata/'}
                ]
            },

            {
                text: '软件安装',
                items: [
                    {text: '环境安装', link: '/doc/software-setup/'}
                ]
            },

            {
                text: '关于',
                items: [
                    {text: '运维技术网站', link: 'https://atengk.github.io/linux/'},
                    {text: '后端技术网站', link: 'https://atengk.github.io/java/'},
                    {text: '前端技术网站', link: 'https://atengk.github.io/vue/'},
                    {text: '工程化框架技术网站', link: 'https://atengk.github.io/project/'},
                    {text: 'VitePress', link: 'https://vitejs.cn/vitepress/'}
                ]
            }
        ],

        sidebar: {
            '/doc/': {
                text: '软件安装',
                collapsed: false,
                items: [
                    {
                        text: '环境安装',
                        items: [
                            {text: 'IntelliJ IDEA', link: '/doc/software-setup/install-idea/'},
                            {text: 'IntelliJ IDEA 配置', link: '/doc/software-setup/config-idea/'},
                            {text: 'JRebel', link: '/doc/software-setup/install-plugin-jrebel/'},
                        ]
                    }
                ]
            },

            '/basic/': {
                text: 'Java 基础',
                collapsed: false,
                items: [
                    {
                        text: 'Java 基础',
                        items: [
                            {text: 'Optional', link: '/basic/optional/README'},
                            {text: 'Functional Interface', link: '/basic/functional-interface/README'},
                            {text: 'Java 新语法', link: '/basic/java-new-syntax/README'},
                            {text: 'ThreadLocal', link: '/basic/thread-local/README'},
                            {text: 'Scoped Value', link: '/basic/scoped-value/README'}
                        ]
                    }
                ]
            },

            '/springboot2/': {
                text: 'SpringBoot2相关',
                collapsed: false,
                items: [
                    {
                        text: '集成与扩展',
                        items: [
                            { text: 'Email邮箱', link: '/springboot2/boot2-email/README' },
                            { text: 'Jasypt配置加密', link: '/springboot2/boot2-jasypt/README' },
                            { text: 'SMS4J短信', link: '/springboot2/sms4j/README' },
                            { text: 'SMS4J邮箱', link: '/springboot2/sms4j-email/README' },
                            { text: 'SMS4J OA', link: '/springboot2/sms4j-oa/README' }
                        ]
                    },
                    {
                        text: '工程化',
                        items: [
                            { text: '发布Maven仓库', link: '/springboot2/boot2-deploy/README' }
                        ]
                    }
                ]
            },

            '/springboot3/': {
              text: 'SpringBoot3相关',
              collapsed: false,
              items: [
                {
                  text: '基础能力',
                  items: [
                    { text: '配置文件', link: '/springboot3/config/README' },
                    { text: 'Banner', link: '/springboot3/banner/README' },
                    { text: '应用启动', link: '/springboot3/startup/README' },
                    { text: '虚拟线程', link: '/springboot3/virtual/README' }
                  ]
                },
                {
                  text: 'Web 开发',
                  items: [
                    { text: 'HTTP接口', link: '/springboot3/http-interface/README' },
                    { text: '参数效验', link: '/springboot3/validator/README' },
                    { text: '异常处理', link: '/springboot3/exception/README' },
                    { text: 'AOP切面', link: '/springboot3/aop/README' },
                    { text: '拦截器', link: '/springboot3/boot3-interceptor/README' },
                    { text: '过滤器', link: '/springboot3/boot3-filter/README' },
                    { text: 'XSS', link: '/springboot3/xss/README' },
                    { text: '接口加密解密', link: '/springboot3/crypto/README' },
                  ]
                },
                {
                  text: '可观测性',
                  items: [
                    { text: '日志管理', link: '/springboot3/log/README' },
                    { text: 'Actuator', link: '/springboot3/actuator/README' },
                    {
                      text: 'Admin 监控',
                      items: [
                        { text: '服务端', link: '/springboot3/admin-server/README' },
                        { text: '客户端', link: '/springboot3/admin-client/README' }
                      ]
                    }
                  ]
                },
                {
                  text: '数据处理',
                  items: [
                    {
                      text: '序列化和反序列化',
                      items: [
                        { text: 'Jackson', link: '/springboot3/serialize-jackson/README' },
                        { text: 'Jackson3', link: '/springboot3/serialize-jackson3/README' },
                        { text: 'Fastjson', link: '/springboot3/serialize-fastjson/README' },
                        { text: 'Fastjson2', link: '/springboot3/serialize-fastjson2/README' }
                      ]
                    }
                  ]
                },
                {
                  text: '集成与扩展',
                  items: [
                    { text: 'Email邮箱', link: '/springboot3/email/README' },
                    { text: 'Jasypt配置加密', link: '/springboot3/jasypt/README' },
                    { text: 'Docker插件', link: '/springboot3/docker/README' }
                  ]
                },
                {
                  text: '工程化',
                  items: [
                    { text: '发布Maven仓库', link: '/springboot3/boot3-deploy/README' },
                    { text: 'Spring Boot DevTools', link: '/springboot3/doc/devtools' },
                    { text: '源码包和依赖包分离', link: '/springboot3/doc/separate' }
                  ]
                }
              ]
            },

            '/springboot4/': {
                text: 'SpringBoot4相关',
                collapsed: false,
                items: [
                    {
                        text: '基础能力',
                        items: [
                            {text: 'Web 示例', link: '/springboot4/boot4-web/README'},
                            {text: '配置文件', link: '/springboot4/boot4-config/README'}
                        ]
                    }
                ]
            },

            '/ai/ollama/': {
                text: 'Ollama',
                collapsed: false,
                items: [
                    {
                        text: 'Ollama 快速开始',
                        link: '/ai/ollama/quick-start/README',
                    },
                ]
            },

            '/ai/': {
                text: 'AI',
                collapsed: false,
                items: [
                    {
                        text: 'Spring AI',
                        items: [
                            { text: 'Spring AI 1', link: '/ai/spring-ai1/README' },
                            { text: 'Spring AI 2', link: '/ai/spring-ai2/README' }
                        ]
                    },
                    {
                        text: 'MCP Server',
                        items: [
                            { text: 'Spring AI 1 MCP Server', link: '/ai/spring-ai1-mcp-server/README' },
                            { text: 'Spring AI 2 MCP Server', link: '/ai/spring-ai2-mcp-server/README' },
                        ]
                    },
                    {
                        text: 'Spring AI Alibaba',
                        items: [
                            { text: 'Spring AI Alibaba 1', link: '/ai/spring-ai-alibaba1/README' },
                            { text: 'Spring AI Alibaba 2', link: '/ai/spring-ai-alibaba2/README' }
                        ]
                    }
                ]
            },

            '/tools/': {
                text: '工具相关',
                collapsed: false,
                items: [
                    {
                        text: 'JSON 处理',
                        items: [
                            { text: 'Jackson', link: '/tools/jackson/README' },
                            { text: 'Jackson3', link: '/tools/jackson3/README' },
                            { text: 'FastJson', link: '/tools/fastjson1/README' },
                            { text: 'FastJson2', link: '/tools/fastjson2/README' }
                        ]
                    },
                    {
                        text: 'Excel 处理',
                        items: [
                            { text: 'Apache Fesod', link: '/tools/apache-fesod/README' },
                            { text: 'FastExcel', link: '/tools/fast-excel/README' },
                            { text: 'FastExcel-JDK8', link: '/tools/fast-excel-jdk8/README' },
                            { text: 'EasyPoi', link: '/tools/easy-poi/README' },
                            { text: 'EasyPoi SpringBoot3', link: '/tools/easy-poi-boot3/README' }
                        ]
                    },
                    {
                        text: '工具库',
                        items: [
                            { text: 'Hutool', link: '/tools/hutool/README' },
                            { text: 'Lombok', link: '/tools/lombok/README' },
                            { text: 'QLExpress 规则引擎', link: '/tools/qlexpress4/README' },
                            { text: 'Drools 规则引擎', link: '/tools/drools/README' },
                            { text: 'Warm Flow 工作流', link: '/tools/warm-flow/README' },
                            { text: 'ip2region 离线IP管理', link: '/tools/ip2region/README' },
                            { text: 'SQL 格式化工具', link: '/tools/sql-formatter/README' },
                        ]
                    },
                    {
                        text: '数据处理',
                        items: [
                            { text: 'Stream', link: '/tools/stream/README' },
                            { text: 'MapStructPlus', link: '/tools/mapstruct-plus/README' }
                        ]
                    },
                    {
                        text: '并发与异步',
                        items: [
                            { text: '异步编程', link: '/tools/async/README' },
                            { text: '线程池', link: '/tools/thread-pool/README' }
                        ]
                    },
                    {
                        text: '文件与文档',
                        items: [
                            { text: 'Apache Tika', link: '/tools/apache-tika/README' },
                            { text: 'OnlyOffice', link: '/tools/onlyoffice/README' }
                        ]
                    },
                    {
                        text: '网络与远程',
                        items: [
                            { text: 'SSHJ', link: '/tools/sshj/README' }
                        ]
                    },
                    {
                        text: '自定义扩展',
                        items: [
                            { text: '自定义工具类', link: '/tools/custom-utils/README' }
                        ]
                    }
                ]
            },

            '/database/': {
                text: '数据库相关',
                collapsed: false,
                items: [
                    {
                        text: '关系型数据库（RDBMS）',
                        items: [
                            {
                                text: 'MyBatis 体系',
                                items: [
                                    { text: 'MyBatis-Flex', link: '/database/mybatis-flex/README' },
                                    { text: 'MyBatis Plus', link: '/database/mybatis-plus/README' },
                                    { text: 'MyBatis Plus JDK8', link: '/database/mybatis-plus-jdk8/README' }
                                ]
                            },
                            {
                                text: 'JPA 体系',
                                items: [
                                    { text: 'JPA', link: '/database/spring-jpa/README' }
                                ]
                            },
                            {
                                text: 'SQL',
                                items: [
                                    { text: 'Mysql SQL', link: '/database/mybatis-plus-jdk8/SQL' }
                                ]
                            },
                            {
                                text: '增强工具',
                                items: [
                                    { text: 'JdbcTemplate', link: '/database/jdbc-template/README' },
                                    { text: 'AutoTable', link: '/database/autotable/README' },
                                    { text: 'Beetl', link: '/database/beetl/README' }
                                ]
                            }
                        ]
                    },
                    {
                        text: 'NoSQL 数据库',
                        items: [
                            {
                                text: 'MongoDB',
                                items: [
                                    { text: 'MongoTemplate', link: '/database/mongo-template/README' },
                                    { text: 'MongoPlus', link: '/database/mongo-plus/README' }
                                ]
                            },
                            {
                                text: 'Elasticsearch',
                                items: [
                                    { text: 'Easy-Es', link: '/database/easy-es/README' }
                                ]
                            }
                        ]
                    },
                    {
                        text: '空间数据库',
                        items: [
                            { text: 'PostGIS', link: '/database/mybatis-flex-postgis/README' },
                            { text: 'PostGIS SQL', link: '/database/mybatis-flex-postgis/SQL' }
                        ]
                    },
                    {
                        text: '向量数据库',
                        items: [
                            { text: 'Milvus', link: '/database/milvus/README' },
                            { text: 'MilvusPlus', link: '/database/milvus-plus/README' },
                            { text: 'pgvector sql', link: '/database/pgvector/SQL' },
                        ]
                    }
                ]
            },

            '/cache/': {
                text: '缓存相关',
                collapsed: false,
                items: [
                    {
                        text: 'Redis',
                        items: [
                            { text: 'RedisTemplate', link: '/cache/redis-template/README' },
                            { text: 'RedisTemplate-JDK8', link: '/cache/redis-template-jdk8/README' },
                            { text: 'Redisson', link: '/cache/redisson/README' },
                            { text: 'Redisson-JDK8', link: '/cache/redisson-jdk8/README' },
                            { text: 'Redisson 项目实战案例', link: '/cache/redisson-example/README' },
                        ]
                    },
                    {
                        text: '缓存框架',
                        items: [
                            { text: 'SpringCache', link: '/cache/spring-cache/README' },
                            { text: 'JetCache', link: '/cache/jetcache/README' }
                        ]
                    },
                    {
                        text: '本地缓存',
                        items: [
                            { text: 'Caffeine', link: '/cache/caffeine/README' }
                        ]
                    }
                ]
            },

            '/auth/': {
                text: '权限认证',
                collapsed: false,
                items: [
                    {
                        text: '权限认证',
                        items: [
                            {text: 'Sa-Token', link: '/auth/sa-token/README'},
                            {text: 'Spring Security', link: '/auth/spring-security/README'}
                        ]
                    },
                    {
                        text: 'License 授权',
                        items: [
                            {text: 'License 授权', link: '/auth/license/README'},
                        ]
                    }
                ]
            },

            '/pay/': {
                text: '支付相关',
                collapsed: false,
                items: [
                    {
                        text: '支付',
                        items: [
                            {text: '支付宝支付', link: '/pay/alipay/README'},
                            {text: 'IJPay聚合支付', link: '/pay/IJPay/README'}
                        ]
                    }
                ]
            },

            '/storage/': {
                text: '存储相关',
                collapsed: false,
                items: [
                    {
                        text: '存储',
                        items: [
                            {text: 'X File Storage', link: '/storage/x-file-storage/README'},
                            {text: 'AWS S3', link: '/storage/aws-s3/README'}
                        ]
                    }
                ]
            },

            '/mq/': {
                text: '消息队列相关',
                collapsed: false,
                items: [
                    {
                        text: 'Kafka',
                        items: [
                            { text: '生产者', link: '/mq/kafka-provider/README' },
                            { text: '消费者', link: '/mq/kafka-consumer/README' }
                        ]
                    },
                    {
                        text: 'RabbitMQ',
                        items: [
                            { text: '生产者', link: '/mq/rabbitmq-provider/README' },
                            { text: '消费者', link: '/mq/rabbitmq-consumer/README' }
                        ]
                    },
                    {
                        text: 'RocketMQ',
                        items: [
                            { text: '生产者', link: '/mq/rocketmq-provider/README' },
                            { text: '消费者', link: '/mq/rocketmq-consumer/README' }
                        ]
                    }
                ]
            },

            '/http/': {
                text: 'HTTP请求相关',
                collapsed: false,
                items: [
                    {
                        text: '底层客户端',
                        items: [
                            { text: 'Apache HttpClient4', link: '/http/httpclient4/README' },
                            { text: 'Apache HttpClient5', link: '/http/httpclient5/README' }
                        ]
                    },
                    {
                        text: 'Spring 体系',
                        items: [
                            { text: 'RestTemplate', link: '/http/rest-template/README' },
                            { text: 'RestTemplate JDK8', link: '/http/rest-template-jdk8/README' },
                            { text: 'WebClient', link: '/http/web-client/README' },
                            { text: 'RestClient', link: '/http/rest-client/README' }
                        ]
                    },
                    {
                        text: '封装框架',
                        items: [
                            { text: 'Forest', link: '/http/forest/README' }
                        ]
                    }
                ]
            },

            '/task/': {
                text: '任务相关',
                collapsed: false,
                items: [
                    {
                        text: '本地任务',
                        items: [
                            { text: 'Scheduled', link: '/task/scheduled/README' }
                        ]
                    },
                    {
                        text: '分布式调度',
                        items: [
                            { text: 'PowerJob', link: '/task/power-job/README' },
                            { text: 'Snail Job', link: '/task/snail-job/README' }
                        ]
                    },
                    {
                        text: '任务模型',
                        items: [
                            { text: '数据库驱动的任务执行模型', link: '/task/database-job/README' }
                        ]
                    }
                ]
            },

            '/realtime/': {
                text: '实时性的服务',
                collapsed: false,
                items: [
                    {
                        text: '实时通信协议',
                        items: [
                            { text: 'WebSocket', link: '/realtime/websocket/README' },
                            { text: 'STOMP', link: '/realtime/stomp/README' },
                            { text: 'SSE', link: '/realtime/sse/README' },
                            { text: 'MQTT', link: '/realtime/mqtt/README' }
                        ]
                    },
                    {
                        text: '部署模式',
                        items: [
                            { text: 'WebSocket Single', link: '/realtime/websocket-single/README' },
                            { text: 'WebSocket Cluster', link: '/realtime/websocket-cluster/README' },
                            { text: 'STOMP Cluster', link: '/realtime/stomp-cluster/README' }
                        ]
                    },
                    {
                        text: '底层网络',
                        items: [
                            { text: 'Netty', link: '/realtime/netty/README' }
                        ]
                    }
                ]
            },

            '/distributed/': {
                text: '分布式相关',
                collapsed: false,
                items: [
                    {
                        text: '基础能力',
                        items: [
                            { text: 'Lock4j', link: '/distributed/lock4j/README' }
                        ]
                    },
                    {
                        text: '服务治理（Spring Cloud）',
                        items: [
                            { text: 'Nacos', link: '/distributed/spring-cloud-nacos/README' },
                            { text: 'Spring Cloud Gateway', link: '/distributed/spring-cloud-gateway/README' },
                            { text: 'Spring Cloud OpenFeign', link: '/distributed/spring-cloud-openfeign/README' },
                            { text: 'Spring Cloud Sentinel', link: '/distributed/spring-cloud-sentinel/README' },
                            { text: 'Spring Cloud Seata', link: '/distributed/spring-cloud-seata/README' },
                            { text: 'Spring Cloud Stream', link: '/distributed/spring-cloud-stream/README' }
                        ]
                    },
                    {
                        text: 'RPC 通信',
                        items: [
                            { text: 'Apache Dubbo', link: '/distributed/spring-cloud-dubbo-provider/README' },
                            { text: 'Spring gRPC', link: '/distributed/spring-grpc/README' }
                        ]
                    },
                    {
                        text: '可观测性',
                        items: [
                            { text: '分布式链路追踪 Zipkin', link: '/distributed/doc/brave-zipkin' },
                            { text: 'OpenTelemetry', link: '/distributed/doc/observability' },
                            { text: 'SkyWalking', link: '/distributed/doc/skywalking' }
                        ]
                    },
                    {
                        text: '架构设计',
                        items: [
                            { text: '微服务模块', link: '/distributed/doc/spring-cloud-module' },
                            { text: '微服务模块（详细版）', link: '/distributed/doc/spring-cloud-module-details' }
                        ]
                    }
                ]
            },

            '/bigdata/': {
                text: '大数据相关',
                collapsed: false,
                items: [
                    {
                        text: 'Flink',
                        items: [
                            {text: '使用文档', link: '/bigdata/flink-examples/README'},
                            {text: '单机运行', link: '/bigdata/flink-standalone/README'},
                            {text: '集群运行', link: '/bigdata/flink-cluster/README'}
                        ]
                    },
                    {
                        text: 'Spark',
                        items: [
                            {text: '使用文档', link: '/bigdata/spark-examples/README'},
                            {text: '单机运行', link: '/bigdata/spark-standalone/README'},
                            {text: '集群运行', link: '/bigdata/spark-cluster/README'}
                        ]
                    },
                    {
                        text: 'Zookeeper',
                        items: [
                            {text: '使用文档', link: '/bigdata/zookeeper/README'}
                        ]
                    }
                ]
            },

            '/designpattern/': {
                text: '设计模式',
                collapsed: false,
                items: [
                    {text: '单例模式', link: '/designpattern/singleton/README'},
                    {text: '🔥工厂模式🔥', link: '/designpattern/factory/README'},
                    {text: '🔥策略模式🔥', link: '/designpattern/strategy/README'},
                    {text: '模板方法模式', link: '/designpattern/template/README'},
                    {text: '装饰器模式', link: '/designpattern/decorator/README'},
                    {text: '代理模式', link: '/designpattern/proxy/README'},
                    {text: '适配器模式', link: '/designpattern/adapter/README'},
                    {text: '外观模式', link: '/designpattern/facade/README'},
                    {text: '责任链模式', link: '/designpattern/chain/README'},
                    {text: '构建者模式', link: '/designpattern/builder/README'},
                    {text: '原型模式', link: '/designpattern/prototype/README'},
                    {text: '状态模式', link: '/designpattern/state/README'},
                    {text: '命令模式', link: '/designpattern/command/README'},
                    {text: '组合模式', link: '/designpattern/composite/README'},
                    {text: '迭代器模式', link: '/designpattern/iterator/README'},
                    {text: '抽象工厂模式', link: '/designpattern/abstractfactory/README'},
                    {text: '工厂方法模式', link: '/designpattern/factorymethod/README'},
                    {text: '桥接模式', link: '/designpattern/bridge/README'},
                    {text: '中介者模式', link: '/designpattern/mediator/README'},
                    {text: '享元模式', link: '/designpattern/flyweight/README'},
                    {text: '备忘录模式', link: '/designpattern/memento/README'},
                    {text: '解释器模式', link: '/designpattern/interpreter/README'},
                    {text: '访问者模式', link: '/designpattern/visitor/README'}
                ]
            }
        },

        socialLinks: [
            {
                icon: 'github',
                link: 'https://github.com/atengk',
                ariaLabel: 'GitHub'
            },
            {
                icon: {
                    svg: '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><linearGradient id="fav_grad" x1="0%" y1="0%" x2="100%" y2="100%"><stop offset="0%" style="stop-color:#38bdf8;stop-opacity:1" /><stop offset="100%" style="stop-color:#2563eb;stop-opacity:1" /></linearGradient><filter id="glow_icon" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="3" result="coloredBlur"/><feMerge><feMergeNode in="coloredBlur"/><feMergeNode in="SourceGraphic"/></feMerge></filter></defs><path d="M40 70 L60 30 L80 70" fill="none" stroke="url(#fav_grad)" stroke-width="10" stroke-linecap="round" filter="url(#glow_icon)" /><path d="M40 45 L80 45" fill="none" stroke="url(#fav_grad)" stroke-width="10" stroke-linecap="round" filter="url(#glow_icon)" /></svg>'
                },
                link: 'https://atengk.github.io',
                ariaLabel: '阿腾技术网站'
            },
            {
                icon: {
                    svg: '<svg version="1.0" xmlns="http://www.w3.org/2000/svg" width="726.000000pt" height="726.000000pt" viewBox="0 0 726.000000 726.000000"><g transform="translate(0.000000,726.000000) scale(0.100000,-0.100000)" stroke="none"><path fill="#18a058" d="M3255 7239 c-112 -39 -191 -120 -230 -238 -14 -43 -17 -88 -17 -237 0 -144 -3 -186 -14 -195 -8 -6 -14 -9 -14 -6 0 3 -23 -1 -52 -9 -28 -9 -60 -17 -71 -19 -10 -3 -28 -7 -40 -11 -12 -5 -26 -8 -32 -9 -5 -1 -17 -4 -25 -7 -8 -3 -53 -19 -100 -34 -47 -15 -89 -31 -94 -36 -6 -4 -16 -8 -24 -8 -15 0 -68 -22 -114 -46 -16 -9 -28 -12 -28 -7 0 4 -4 4 -8 -2 -6 -9 -103 -58 -109 -56 -2 0 -46 -24 -99 -54 -53 -30 -100 -55 -105 -55 -5 0 -9 -3 -9 -7 0 -9 -57 -43 -71 -43 -5 0 -69 60 -142 133 -132 130 -163 152 -264 185 -76 24 -223 -4 -292 -57 -56 -42 -441 -432 -466 -471 -60 -96 -75 -246 -32 -325 6 -11 12 -25 13 -32 4 -23 60 -88 177 -205 64 -64 117 -120 117 -123 0 -4 -13 -25 -29 -48 -15 -23 -31 -52 -35 -64 -4 -13 -11 -23 -15 -23 -5 0 -14 -15 -22 -32 -7 -18 -16 -35 -19 -38 -5 -4 -82 -157 -85 -170 -1 -3 -9 -23 -19 -45 -41 -92 -103 -255 -111 -290 -1 -5 -5 -17 -8 -25 -9 -20 -47 -164 -59 -222 -6 -27 -13 -48 -17 -49 -3 0 -91 -2 -196 -3 -105 -1 -208 -8 -231 -14 -120 -35 -230 -160 -251 -287 -5 -27 -8 -185 -7 -350 1 -282 3 -303 23 -357 41 -106 113 -177 222 -220 48 -18 77 -21 249 -22 l195 -1 13 -50 c7 -27 15 -58 17 -68 2 -10 7 -27 10 -37 3 -10 7 -27 9 -37 15 -75 116 -345 128 -341 5 2 186 180 402 396 l393 393 -8 44 c-24 126 -32 424 -15 550 5 33 9 71 10 85 10 99 86 364 138 480 70 156 158 310 222 392 12 14 32 41 46 59 85 113 253 277 380 372 116 87 304 191 440 244 120 47 141 55 155 58 6 2 35 10 65 19 30 8 73 18 95 22 22 3 44 8 48 11 4 2 22 7 40 9 18 3 43 7 57 9 112 18 365 24 485 11 299 -32 547 -112 810 -260 39 -22 72 -43 75 -46 3 -3 26 -19 52 -35 27 -17 48 -33 48 -37 0 -5 5 -8 11 -8 11 0 84 -59 168 -135 41 -37 58 -54 141 -145 50 -54 142 -178 194 -258 90 -142 216 -429 241 -552 2 -8 8 -33 13 -55 6 -22 13 -53 16 -70 3 -16 7 -41 10 -55 7 -36 17 -109 21 -155 6 -61 6 -315 0 -360 -25 -201 -29 -221 -80 -405 -31 -114 -117 -310 -183 -420 -12 -19 -29 -48 -38 -65 -9 -16 -20 -32 -23 -35 -3 -3 -19 -24 -34 -48 -32 -50 -172 -217 -215 -259 -95 -90 -183 -165 -242 -206 -154 -107 -272 -173 -410 -230 -79 -33 -223 -82 -266 -92 -228 -50 -308 -60 -499 -59 -170 0 -297 10 -355 29 -14 4 -111 -87 -414 -390 -218 -218 -396 -400 -396 -405 0 -4 24 -16 53 -26 28 -9 57 -20 62 -24 6 -4 57 -22 115 -39 58 -18 116 -37 130 -41 14 -5 50 -14 80 -20 106 -23 97 -1 98 -223 0 -122 5 -203 12 -216 6 -12 8 -21 5 -21 -3 0 6 -22 20 -49 27 -53 103 -141 123 -141 7 0 12 -4 12 -9 0 -5 17 -15 38 -22 20 -6 43 -17 51 -23 10 -8 117 -11 350 -11 307 0 341 2 394 20 31 10 57 23 57 27 0 4 6 8 14 8 20 0 112 96 131 137 36 75 42 119 42 307 1 103 2 189 5 191 2 2 15 6 28 9 140 29 398 113 533 174 37 17 67 28 67 24 0 -4 4 -2 8 3 4 6 43 28 87 49 44 22 82 42 85 45 3 4 17 12 32 18 15 7 46 25 70 40 98 64 83 69 228 -76 138 -139 184 -172 270 -193 92 -23 172 -12 265 36 40 21 440 412 487 476 77 107 85 261 19 386 -12 23 -83 103 -157 178 l-136 137 19 26 c22 32 48 75 76 128 12 22 24 42 28 45 6 6 102 202 103 211 1 3 12 30 25 60 13 30 27 63 32 74 17 42 71 203 85 255 19 69 45 170 48 190 2 13 32 15 187 16 201 2 230 5 296 35 46 21 70 38 110 79 24 24 75 104 80 125 1 5 7 31 13 56 12 49 9 646 -3 688 -33 117 -150 231 -262 257 -16 3 -115 7 -219 8 -211 3 -199 -2 -216 83 -5 26 -19 80 -30 118 -12 39 -22 77 -25 85 -2 8 -4 16 -5 18 -2 1 -3 5 -5 10 -1 4 -5 14 -8 22 -4 8 -14 38 -23 65 -9 28 -21 61 -27 75 -5 14 -11 30 -12 36 -7 29 -149 311 -202 400 l-60 102 136 138 c91 92 144 155 159 187 68 144 44 300 -63 415 -114 123 -402 403 -435 424 -95 58 -234 73 -325 34 -22 -9 -42 -17 -45 -18 -13 -1 -86 -67 -191 -172 -64 -64 -124 -116 -132 -116 -9 0 -25 10 -37 22 -11 12 -20 18 -20 12 0 -5 -4 -4 -8 2 -9 13 -185 111 -277 154 -73 34 -263 111 -280 114 -5 1 -14 4 -20 7 -5 4 -37 14 -70 24 -33 10 -87 26 -120 36 -33 10 -85 23 -115 29 l-55 12 0 207 -1 206 -29 60 c-29 60 -125 165 -151 165 -8 0 -14 4 -14 8 0 5 -26 16 -57 25 -48 13 -115 16 -373 16 -293 1 -319 -1 -375 -20z"/><path fill="#1e1e1e" d="M3577 5134 c-1 -1 -44 -4 -94 -8 -51 -4 -98 -8 -105 -11 -7 -2 -24 -6 -38 -9 -106 -19 -283 -79 -375 -126 -65 -34 -177 -100 -185 -109 -3 -4 -23 -18 -45 -33 -22 -15 -56 -42 -76 -60 -20 -18 -47 -43 -60 -55 -74 -66 -182 -197 -237 -288 -56 -90 -130 -249 -152 -325 -13 -41 -27 -80 -31 -86 -5 -6 -7 -13 -4 -16 3 -2 0 -20 -5 -39 -6 -19 -13 -52 -16 -74 -3 -22 -8 -51 -10 -65 -3 -14 -6 -88 -8 -165 -3 -123 7 -245 28 -345 3 -14 8 -35 10 -48 3 -12 15 -54 28 -92 l22 -71 -1037 -1037 c-570 -570 -1053 -1059 -1072 -1086 -19 -27 -35 -55 -35 -63 0 -7 -3 -13 -8 -13 -8 0 -31 -48 -37 -79 -2 -12 -6 -25 -9 -29 -21 -34 -29 -211 -14 -291 9 -51 55 -180 67 -191 3 -3 12 -17 19 -31 17 -33 116 -137 162 -171 110 -80 245 -120 388 -114 110 5 176 22 277 74 65 33 151 116 1128 1092 l1058 1058 49 -19 c27 -10 59 -20 72 -23 13 -3 32 -7 43 -10 148 -34 189 -39 350 -39 150 -1 226 7 345 34 82 19 233 70 275 92 11 6 22 12 25 12 23 4 240 133 260 155 3 3 25 21 50 41 134 108 265 260 348 404 49 86 121 250 137 315 1 3 4 12 7 20 10 24 37 146 44 195 11 70 14 116 15 215 0 101 -8 237 -16 250 -2 3 -6 26 -10 50 -22 151 -160 233 -287 171 -28 -14 -133 -111 -303 -282 -143 -144 -282 -279 -309 -301 -131 -104 -308 -135 -469 -81 -39 14 -74 28 -77 31 -3 4 -17 13 -32 21 -39 20 -109 89 -144 142 -106 157 -115 340 -26 513 31 59 78 111 328 362 160 161 298 306 307 322 8 16 14 57 15 90 0 63 -24 113 -73 154 -34 29 -172 62 -280 68 -69 4 -175 6 -178 4z"/></g></svg>'
                },
                link: 'https://atengk.github.io/tools/',
                ariaLabel: 'IT TOOLS'
            },
            {
                icon: {
                    svg: '<svg xmlns="http://www.w3.org/2000/svg" width="256" height="193" viewBox="0 0 256 193"><path fill="#4285f4" d="M58.182 192.05V93.14L27.507 65.077L0 49.504v125.091c0 9.658 7.825 17.455 17.455 17.455z"/><path fill="#34a853" d="M197.818 192.05h40.727c9.659 0 17.455-7.826 17.455-17.455V49.505l-31.156 17.837l-27.026 25.798z"/><path fill="#ea4335" d="m58.182 93.14l-4.174-38.647l4.174-36.989L128 69.868l69.818-52.364l4.669 34.992l-4.669 40.644L128 145.504z"/><path fill="#fbbc04" d="M197.818 17.504V93.14L256 49.504V26.231c0-21.585-24.64-33.89-41.89-20.945z"/><path fill="#c5221f" d="m0 49.504l26.759 20.07L58.182 93.14V17.504L41.89 5.286C24.61-7.66 0 4.646 0 26.23z"/></svg>'
                },
                link: 'mailto:kongyu2385569970@gmail.com',
                ariaLabel: '邮箱联系我'
            },
            {
                icon: {
                    svg: '<svg t="1774506302010" class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="13242" width="200" height="200"><path d="M511.09761 957.257c-80.159 0-153.737-25.019-201.11-62.386-24.057 6.702-54.831 17.489-74.252 30.864-16.617 11.439-14.546 23.106-11.55 27.816 13.15 20.689 225.583 13.211 286.912 6.767v-3.061z" fill="#FAAD08" p-id="13243"></path><path d="M496.65061 957.257c80.157 0 153.737-25.019 201.11-62.386 24.057 6.702 54.83 17.489 74.253 30.864 16.616 11.439 14.543 23.106 11.55 27.816-13.15 20.689-225.584 13.211-286.914 6.767v-3.061z" fill="#FAAD08" p-id="13244"></path><path d="M497.12861 474.524c131.934-0.876 237.669-25.783 273.497-35.34 8.541-2.28 13.11-6.364 13.11-6.364 0.03-1.172 0.542-20.952 0.542-31.155C784.27761 229.833 701.12561 57.173 496.64061 57.162 292.15661 57.173 209.00061 229.832 209.00061 401.665c0 10.203 0.516 29.983 0.547 31.155 0 0 3.717 3.821 10.529 5.67 33.078 8.98 140.803 35.139 276.08 36.034h0.972z" fill="#000000" p-id="13245"></path><path d="M860.28261 619.782c-8.12-26.086-19.204-56.506-30.427-85.72 0 0-6.456-0.795-9.718 0.148-100.71 29.205-222.773 47.818-315.792 46.695h-0.962C410.88561 582.017 289.65061 563.617 189.27961 534.698 185.44461 533.595 177.87261 534.063 177.87261 534.063 166.64961 563.276 155.56661 593.696 147.44761 619.782 108.72961 744.168 121.27261 795.644 130.82461 796.798c20.496 2.474 79.78-93.637 79.78-93.637 0 97.66 88.324 247.617 290.576 248.996a718.01 718.01 0 0 1 5.367 0C708.80161 950.778 797.12261 800.822 797.12261 703.162c0 0 59.284 96.111 79.783 93.637 9.55-1.154 22.093-52.63-16.623-177.017" fill="#000000" p-id="13246"></path><path d="M434.38261 316.917c-27.9 1.24-51.745-30.106-53.24-69.956-1.518-39.877 19.858-73.207 47.764-74.454 27.875-1.224 51.703 30.109 53.218 69.974 1.527 39.877-19.853 73.2-47.742 74.436m206.67-69.956c-1.494 39.85-25.34 71.194-53.24 69.956-27.888-1.238-49.269-34.559-47.742-74.435 1.513-39.868 25.341-71.201 53.216-69.974 27.909 1.247 49.285 34.576 47.767 74.453" fill="#FFFFFF" p-id="13247"></path><path d="M683.94261 368.627c-7.323-17.609-81.062-37.227-172.353-37.227h-0.98c-91.29 0-165.031 19.618-172.352 37.227a6.244 6.244 0 0 0-0.535 2.505c0 1.269 0.393 2.414 1.006 3.386 6.168 9.765 88.054 58.018 171.882 58.018h0.98c83.827 0 165.71-48.25 171.881-58.016a6.352 6.352 0 0 0 1.002-3.395c0-0.897-0.2-1.736-0.531-2.498" fill="#FAAD08" p-id="13248"></path><path d="M467.63161 256.377c1.26 15.886-7.377 30-19.266 31.542-11.907 1.544-22.569-10.083-23.836-25.978-1.243-15.895 7.381-30.008 19.25-31.538 11.927-1.549 22.607 10.088 23.852 25.974m73.097 7.935c2.533-4.118 19.827-25.77 55.62-17.886 9.401 2.07 13.75 5.116 14.668 6.316 1.355 1.77 1.726 4.29 0.352 7.684-2.722 6.725-8.338 6.542-11.454 5.226-2.01-0.85-26.94-15.889-49.905 6.553-1.579 1.545-4.405 2.074-7.085 0.242-2.678-1.834-3.786-5.553-2.196-8.135" fill="#000000" p-id="13249"></path><path d="M504.33261 584.495h-0.967c-63.568 0.752-140.646-7.504-215.286-21.92-6.391 36.262-10.25 81.838-6.936 136.196 8.37 137.384 91.62 223.736 220.118 224.996H506.48461c128.498-1.26 211.748-87.612 220.12-224.996 3.314-54.362-0.547-99.938-6.94-136.203-74.654 14.423-151.745 22.684-215.332 21.927" fill="#FFFFFF" p-id="13250"></path><path d="M323.27461 577.016v137.468s64.957 12.705 130.031 3.91V591.59c-41.225-2.262-85.688-7.304-130.031-14.574" fill="#EB1C26" p-id="13251"></path><path d="M788.09761 432.536s-121.98 40.387-283.743 41.539h-0.962c-161.497-1.147-283.328-41.401-283.744-41.539l-40.854 106.952c102.186 32.31 228.837 53.135 324.598 51.926l0.96-0.002c95.768 1.216 222.4-19.61 324.6-51.924l-40.855-106.952z" fill="#EB1C26" p-id="13252"></path></svg>'
                },
                link: 'http://wpa.qq.com/msgrd?v=3&uin=2385569970&Menu=yes',
                ariaLabel: 'QQ 联系我'
            }
        ],
        notFound: {
            code: '404',
            title: '页面未找到',
            quote: '您访问的页面不存在',
            linkLabel: '返回首页',
            linkText: '点击这里返回主页'
        },
        footer: {
            message: 'MIT License · Built with VitePress',
            copyright: `© ${new Date().getFullYear()} Ateng`
        },
        docFooter: {
            prev: '上一页',
            next: '下一页'
        },
        editLink: {
            pattern: 'https://github.com/atengk/Ateng-Java/edit/main/:path',
            text: '在 GitHub 上编辑此页'
        },
        outline: {
            level: 'deep',
            label: '目录'
        },
        search: {
            provider: 'local',
            options: {
                detailedView: true,
                disableQueryPersistence: false,
                translations: {
                    button: {
                        buttonText: '搜索',
                        buttonAriaLabel: '搜索文档'
                    },
                    modal: {
                        noResultsText: '未找到结果',
                        resetButtonTitle: '清除查询',
                        footer: {
                            selectText: '选择',
                            navigateText: '切换',
                            closeText: '关闭'
                        }
                    }
                }
            }
        },
        lastUpdated: {
            text: '🕒 最后更新',
            formatOptions: {
                // @ts-ignore
                dateStyle: 'medium',
                timeStyle: 'short'
            }
        },
        externalLinkIcon: true,
        langMenuLabel: '多语言',
        returnToTopLabel: '回到顶部',
        sidebarMenuLabel: '菜单',
        darkModeSwitchLabel: '主题',
        lightModeSwitchTitle: '切换到浅色模式',
        darkModeSwitchTitle: '切换到深色模式',
    },
    head: [
        ['link', {rel: 'icon', type: 'image/svg+xml', href: '/favicon.svg'}]
    ],
    markdown: {
        lineNumbers: true,
        image: {
            lazyLoading: true // 基于浏览器原生懒加载
        }
    },
    // 死链处理策略
    ignoreDeadLinks: true,
})
