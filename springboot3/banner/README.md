# SpringBoot3 Banner相关的模块



## 基础配置

### Maven配置环境

编辑pom.xml配置环境信息

```xml
    <!-- 项目环境配置 -->
    <profiles>
        <!-- 开发环境配置 -->
        <profile>
            <id>dev</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <profiles.active>dev</profiles.active>
                <profiles.desc>开发环境</profiles.desc>
                <logging.level>info</logging.level>
            </properties>
        </profile>

        <!-- 测试环境配置 -->
        <profile>
            <id>test</id>
            <properties>
                <profiles.active>test</profiles.active>
                <profiles.desc>测试环境</profiles.desc>
                <logging.level>info</logging.level>
            </properties>
        </profile>

        <!-- 生产环境配置 -->
        <profile>
            <id>prod</id>
            <properties>
                <profiles.active>prod</profiles.active>
                <profiles.desc>生产环境</profiles.desc>
                <logging.level>warn</logging.level>
            </properties>
        </profile>
    </profiles>
```



### 编辑配置文件

**编辑 `application.yml` 文件**

```yaml
server:
  port: 12008
  servlet:
    context-path: /
spring:
  main:
    web-application-type: servlet
  application:
    name: ${project.artifactId}
  profiles:
    active: @profiles.active@
    desc: @profiles.desc@
```

**创建 `banner.txt`** 

在resources目录下创建该文件

```

         █████╗ ████████╗███████╗███╗   ██╗ ██████╗
        ██╔══██╗╚══██╔══╝██╔════╝████╗  ██║██╔════╝
        ███████║   ██║   █████╗  ██╔██╗ ██║██║  ███╗
        ██╔══██║   ██║   ██╔══╝  ██║╚██╗██║██║   ██║
        ██║  ██║   ██║   ███████╗██║ ╚████║╚██████╔╝
        ╚═╝  ╚═╝   ╚═╝   ╚══════╝╚═╝  ╚═══╝ ╚═════╝

${AnsiColor.CYAN}        Application Name:${AnsiColor.BLACK} ${AnsiColor.BRIGHT_GREEN}${spring.application.name:Ateng} ${AnsiColor.BLACK}
${AnsiColor.CYAN}        SpringBoot Version:${AnsiColor.BLACK} ${AnsiColor.BRIGHT_GREEN}${spring-boot.version} ${AnsiColor.BLACK}
${AnsiColor.CYAN}        Profiles Active:${AnsiColor.BLACK} ${AnsiColor.BRIGHT_GREEN}${spring.profiles.active:-} ${AnsiColor.BLACK}
${AnsiColor.CYAN}        Profiles Describe:${AnsiColor.BLACK} ${AnsiColor.BRIGHT_GREEN}${spring.profiles.desc:-} ${AnsiColor.BLACK}

```



