# Scoped Value

## 技术概述

本节用于说明 Scoped Value 在 Java 项目开发中的定位、与 ThreadLocal 的核心区别，以及适合在业务系统中落地的典型场景。Scoped Value 主要用于在限定调用范围内传递只读上下文数据，例如请求上下文、用户信息、TraceId、租户信息等。

### Scoped Value 的定位

Scoped Value 是 Java 提供的一种作用域内上下文传递机制，用于在不显式修改方法参数的情况下，将数据从上层调用方安全地传递给下层调用链中的业务代码。它可以理解为一种受作用域约束的“隐式方法参数”。

在传统 Java 项目中，如果请求上下文、登录用户、TraceId、租户编号等数据需要被多层 Service、组件或工具类读取，通常有两种做法：一种是逐层增加方法参数，另一种是使用 ThreadLocal 保存当前线程上下文。

逐层传参的优点是数据流向清晰，但会污染方法签名，导致业务参数和技术上下文混杂。ThreadLocal 的优点是使用方便，但它的生命周期绑定在线程上，如果在线程池、异步任务或复杂调用链中清理不当，容易出现上下文残留、数据串扰或排查困难等问题。

Scoped Value 的定位是解决“调用方需要向被调用方传递上下文，但又不希望污染每一层方法参数”的问题。它强调作用域限定、单向传递、只读使用和自动恢复。数据只在指定的动态作用域内有效，作用域结束后自动失效；下游代码通常只读取上下文，不负责修改上下文；如果需要临时变更上下文值，也应通过嵌套作用域完成，而不是在当前作用域中直接修改。

在 Java 项目开发中，Scoped Value 更适合作为基础设施层和业务调用链之间的上下文承载方式，而不是替代普通方法参数。普通业务参数仍然应该通过方法参数显式传递；只有那些跨层级、跨组件、但不适合作为业务参数暴露的上下文信息，才适合使用 Scoped Value。

例如，请求入口可以绑定当前请求的 TraceId、用户 ID、租户 ID 等信息。后续日志组件、审计组件、权限组件、数据权限组件可以在调用链中读取这些信息，而不需要每一层方法都额外声明这些参数。

### 与 ThreadLocal 的区别

Scoped Value 和 ThreadLocal 都可以在不显式传递方法参数的情况下，让调用链下游读取上下文数据，但二者的设计目标和使用边界不同。

ThreadLocal 是线程维度的可变存储。它将数据绑定到当前线程上，只要线程存在，ThreadLocal 中的数据就可能继续存在。开发者通常需要在业务结束后手动调用 `remove()` 清理数据，否则在线程池复用线程时可能出现上下文污染。

Scoped Value 是作用域维度的上下文绑定。它将数据绑定到一段明确的执行作用域中，作用域结束后绑定关系自动失效。它不依赖开发者手动清理，因此更容易保证上下文生命周期和业务调用范围一致。

| 对比项       | ThreadLocal                               | Scoped Value                                        |
| ------------ | ----------------------------------------- | --------------------------------------------------- |
| 数据维度     | 线程本地变量                              | 作用域绑定值                                        |
| 生命周期     | 跟随线程，需要手动清理                    | 跟随动态作用域，作用域结束自动失效                  |
| 数据修改     | 支持 `set()`、`remove()` 修改当前线程数据 | 当前作用域内以读取为主，可通过嵌套作用域重新绑定    |
| 数据流向     | 可读可写，容易形成隐式双向通信            | 更偏向调用方到被调用方的单向传递                    |
| 线程池场景   | 清理不当可能导致数据残留                  | 作用域结束自动解绑，边界更清晰                      |
| 虚拟线程场景 | 大量 ThreadLocal 可能增加内存和管理成本   | 更适合与虚拟线程结合                                |
| 结构化并发   | 需要额外处理上下文传递                    | 可以配合 StructuredTaskScope 进行上下文继承         |
| 典型用途     | 兼容旧框架上下文、线程私有状态            | 请求上下文、用户信息、TraceId、租户信息等只读上下文 |

在项目选型时，可以按照以下原则判断：

1. 如果上下文数据需要在调用链中只读传递，并且生命周期应严格限定在一次业务调用内，优先考虑 Scoped Value。
2. 如果数据需要在线程生命周期内长期保存，或者需要在多个位置被主动修改，ThreadLocal 仍然可能更合适。
3. 如果项目使用虚拟线程或 StructuredTaskScope，并且需要在父任务和结构化子任务之间传递上下文，Scoped Value 更符合 Java 新并发模型的设计方向。
4. 如果已有框架强依赖 ThreadLocal，例如部分日志 MDC、认证上下文、事务上下文等，应结合框架能力逐步封装，不建议直接替换底层实现。

需要注意的是，Scoped Value 并不是 ThreadLocal 的完全替代品。它更适合表达“在某个明确作用域内只读共享上下文”的场景，而 ThreadLocal 更偏向“线程私有可变状态”的场景。

### 适用场景

Scoped Value 适合传递那些由入口统一创建、在调用链中多处读取、下游不应随意修改、作用域结束后必须失效的上下文数据。在 Java 后端项目中，它通常用于请求处理、权限校验、链路追踪、多租户隔离、审计日志等基础能力。

第一类典型场景是请求上下文传递。Web 请求进入系统后，可以在 Filter、Interceptor 或 Controller 层解析请求上下文，例如请求 ID、客户端 IP、请求来源、请求路径等信息，然后将其绑定到 Scoped Value 中。下游 Service、工具类或日志组件可以按需读取这些信息，不需要在每个方法中重复传递。

第二类典型场景是用户信息传递。用户认证完成后，可以将当前用户 ID、用户名、角色摘要、数据权限标识等信息封装为不可变上下文对象，并绑定到当前请求作用域中。后续权限判断、业务审计、操作日志、数据过滤等逻辑可以直接读取当前用户上下文。

第三类典型场景是 TraceId 传递。TraceId 通常由请求入口生成，或者从上游请求头中解析。业务日志、异常日志、远程调用、消息发送等逻辑都需要读取 TraceId。使用 Scoped Value 可以减少显式参数传递，同时避免 ThreadLocal 清理遗漏造成的 TraceId 串扰。

第四类典型场景是租户信息传递。在 SaaS 或多租户系统中，请求入口可以解析租户 ID，并在当前调用作用域内绑定租户上下文。下游数据权限、SQL 条件拼接、缓存 Key 构造、对象存储路径生成等逻辑都可以读取租户信息。租户上下文应设计为不可变对象，避免业务代码在调用过程中修改租户标识。

第五类典型场景是结构化并发中的上下文继承。当业务逻辑使用 StructuredTaskScope 拆分多个子任务并发执行时，Scoped Value 可以将父作用域中的上下文传递给结构化子任务，避免手动复制上下文对象，也避免普通线程池中上下文传播边界不清的问题。

不适合使用 Scoped Value 的场景也需要明确。普通业务参数不应为了减少方法参数数量而全部放入 Scoped Value，否则会降低接口表达能力。需要被下游更新并返回给上游的数据，也不适合放入 Scoped Value。缓存可复用对象、连接对象、可变状态对象、跨请求共享数据等，也不应使用 Scoped Value 承载。

推荐的设计方式是将同一类上下文字段聚合为一个不可变上下文对象，而不是为每一个字段都单独定义一个 Scoped Value。例如，可以定义 `RequestContext` 承载 TraceId、用户 ID、租户 ID、请求来源等字段。这样可以减少绑定数量，也便于后续统一扩展和维护。

## 环境要求

本节用于说明 Scoped Value 在 Java 项目中的版本要求、依赖关系和启用方式。由于 Scoped Value 在不同 JDK 版本中的 API 状态不同，项目落地前需要先明确目标 JDK 版本，避免在开发、编译、运行和部署环境中出现不一致。

### JDK 版本要求

Scoped Value 在 JDK 20 中首次以 incubator API 形式引入，位于 `jdk.incubator.concurrent` 包；在 JDK 21 到 JDK 24 中进入 preview 阶段，API 位于 `java.lang.ScopedValue`；在 JDK 25 中正式交付，成为 Java 平台的正式 API。OpenJDK JEP 506 明确标注 Scoped Values 的发布版本为 JDK 25，并说明其用于在同一线程的被调用方法以及子线程之间共享不可变数据。([OpenJDK](https://openjdk.org/jeps/506?utm_source=chatgpt.com)) Oracle JDK 25 API 文档也将 `ScopedValue` 定义为 `java.lang.ScopedValue<T>`，用于在不使用方法参数的情况下安全、高效地向方法共享数据。([Oracle Docs](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/ScopedValue.html?utm_source=chatgpt.com))

推荐在正式项目中使用 JDK 25 或更高版本。这样可以直接使用稳定版 `java.lang.ScopedValue`，不需要开启 preview 特性，也不需要引入 incubator 模块。

| JDK 版本 | API 状态  | 包路径                                 | 是否推荐生产使用 | 说明                                             |
| -------- | --------- | -------------------------------------- | ---------------- | ------------------------------------------------ |
| JDK 20   | Incubator | `jdk.incubator.concurrent.ScopedValue` | 不推荐           | API 处于孵化阶段，使用方式和后续版本存在差异     |
| JDK 21   | Preview   | `java.lang.ScopedValue`                | 谨慎使用         | 需要开启 preview 特性，适合验证和技术预研        |
| JDK 22   | Preview   | `java.lang.ScopedValue`                | 谨慎使用         | 仍为 preview API，发布后可能调整                 |
| JDK 23   | Preview   | `java.lang.ScopedValue`                | 谨慎使用         | 适合跟进新并发模型，但不建议作为长期稳定接口依赖 |
| JDK 24   | Preview   | `java.lang.ScopedValue`                | 谨慎使用         | 第四次 preview，API 已较稳定，但仍需开启 preview |
| JDK 25+  | 正式 API  | `java.lang.ScopedValue`                | 推荐             | 可作为正式项目中的上下文传递方案                 |

如果项目已经升级到 JDK 25，可以直接使用 `ScopedValue.newInstance()`、`ScopedValue.where(...).run(...)`、`ScopedValue.where(...).call(...)` 等 API。JDK 21 到 JDK 24 虽然也提供 `ScopedValue`，但 Oracle JDK 21 API 文档明确标注它属于 preview API，程序必须启用 preview features 才能使用。([Oracle Docs](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ScopedValue.html?utm_source=chatgpt.com))

生产项目建议统一以下版本策略：

1. 新项目优先使用 JDK 25 或更高版本。
2. 如果当前项目仍在 JDK 17 或 JDK 21 LTS，不建议为了 Scoped Value 单独引入 preview API 到核心生产链路。
3. 如果只是技术预研，可以在 JDK 21 到 JDK 24 中通过 preview 参数进行验证。
4. 如果历史代码使用过 JDK 20 incubator 版本，需要迁移到 `java.lang.ScopedValue`，不要继续依赖 `jdk.incubator.concurrent` 包。

### 项目依赖说明

Scoped Value 是 JDK 自带能力，不属于第三方框架能力。正常情况下，项目不需要引入额外 Maven 或 Gradle 依赖，只需要确保编译 JDK 和运行 JDK 满足版本要求。

对于 Spring Boot 项目，也不需要额外引入 Spring Boot Starter。Scoped Value 可以直接在普通 Java 类、Service、Filter、Interceptor、AOP 切面、异步任务或结构化并发任务中使用。

如果项目使用 JDK 25，可以在 Maven 中配置编译版本为 25。

```xml
<properties>
    <!-- 指定项目编译使用的 Java 版本 -->
    <maven.compiler.release>25</maven.compiler.release>
</properties>
```

如果项目使用 Gradle，可以配置 Java Toolchain。

```groovy
java {
    // 指定项目使用 JDK 25 编译
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
```

如果是 Spring Boot 项目，可以在 `pom.xml` 中统一声明 Java 版本。

```xml
<properties>
    <!-- Spring Boot 项目的 Java 编译版本 -->
    <java.version>25</java.version>
</properties>
```

需要注意，Scoped Value 不依赖 Hutool、Lombok、Spring Context、Servlet API 或 Reactor。它属于 JDK 层面的上下文传递能力。项目中可以结合 Hutool、Lombok 或 Spring Boot 使用，但这些依赖不是使用 Scoped Value 的前置条件。

如果项目中需要传递请求上下文，可以将 Scoped Value 封装在基础设施模块中，例如：

| 模块          | 职责                                                   |
| ------------- | ------------------------------------------------------ |
| `common-core` | 定义上下文对象和 Scoped Value 持有类                   |
| `common-web`  | 在 Filter 或 Interceptor 中绑定请求上下文              |
| `biz-service` | 在业务代码中读取当前上下文                             |
| `common-log`  | 在日志、审计或异常处理中读取 TraceId、用户 ID、租户 ID |

建议不要在业务模块中到处直接定义 `ScopedValue` 静态变量，而是统一封装为上下文组件。例如定义 `RequestContextHolder`，由该类负责绑定、读取和判断当前上下文是否存在。这样可以避免不同业务模块重复定义上下文变量，也便于后续替换实现或增加校验逻辑。

### 启用方式

在 JDK 25 或更高版本中，Scoped Value 是正式 API，不需要额外启用参数。只要项目的编译 JDK 和运行 JDK 都是 JDK 25 或更高版本，即可直接使用。

JDK 25 推荐启用方式如下：

```bash
# 查看当前 Java 版本
java -version

# 使用 Maven 编译项目
mvn clean package

# 运行 Spring Boot 应用
java -jar target/app.jar
```

如果输出的 Java 版本为 25 或更高版本，则可以直接使用 `java.lang.ScopedValue`。

在 JDK 21 到 JDK 24 中，Scoped Value 属于 preview API，需要在编译和运行时同时开启 preview。仅在编译阶段开启是不够的，运行阶段也必须开启，否则应用启动或执行相关代码时会失败。

Maven 编译配置示例：

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <!-- 使用对应 JDK 的 release 版本，例如 21、22、23、24 -->
                <release>24</release>
                <!-- 启用 preview API -->
                <compilerArgs>
                    <arg>--enable-preview</arg>
                </compilerArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

运行时也需要增加 `--enable-preview`。

```bash
java --enable-preview -jar target/app.jar
```

如果是单文件或简单示例，可以使用以下方式编译和运行。

```bash
javac --release 24 --enable-preview ScopedValueDemo.java
java --enable-preview ScopedValueDemo
```

如果使用 JDK 20 的 incubator API，则需要额外添加 incubator 模块。但该方式只适合阅读历史代码或迁移旧示例，不建议在新项目中使用。

```bash
javac --add-modules jdk.incubator.concurrent ScopedValueDemo.java
java --add-modules jdk.incubator.concurrent ScopedValueDemo
```

项目落地时应优先采用 JDK 25 的正式 API 方式。除非项目有明确的历史兼容要求，否则不建议在生产项目中使用 JDK 20 incubator 版本或 JDK 21 到 JDK 24 preview 版本作为长期实现基础。

## 核心概念

本节用于说明 Scoped Value 的关键使用模型，包括作用域绑定、不可变上下文、嵌套作用域和线程继承关系。理解这些概念后，才能正确判断哪些数据适合放入 Scoped Value，哪些数据仍然应该通过方法参数或其他机制传递。

### 作用域绑定

作用域绑定是 Scoped Value 最核心的概念。它表示将某个值绑定到一个明确的动态执行范围内，在这个范围内，当前方法以及它直接或间接调用的下游方法都可以读取该值。作用域结束后，绑定关系自动失效。

Scoped Value 的典型绑定方式是使用 `ScopedValue.where(...)` 创建绑定关系，然后通过 `run(...)` 或 `call(...)` 执行业务逻辑。`run(...)` 适合无返回值的业务逻辑，`call(...)` 适合有返回值的业务逻辑。

作用域绑定具有以下特点：

| 特点           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 动态作用域     | 绑定范围由代码执行过程决定，而不是由类、方法或代码块的静态位置单独决定 |
| 自动解绑       | `run` 或 `call` 执行结束后，绑定自动失效                     |
| 异常安全       | 即使作用域内部抛出异常，绑定也会随作用域退出而结束           |
| 下游可读       | 当前作用域内直接或间接调用的方法可以读取绑定值               |
| 作用域外不可读 | 离开作用域后读取该值会失败或返回未绑定状态                   |

从开发角度看，作用域绑定可以理解为“只在这次业务调用中有效的上下文”。例如一次 HTTP 请求进入系统后，在请求处理作用域内绑定 `RequestContext`；请求处理结束后，该上下文自动失效，不会继续影响后续请求。

作用域绑定的设计目的不是创建全局变量，而是创建边界清晰的上下文传递范围。开发时应避免在作用域之外读取上下文，也不应把 Scoped Value 当作跨请求、跨任务的长期存储容器。

### 不可变上下文

Scoped Value 更适合承载不可变上下文。不可变上下文是指对象一旦创建，其内部字段不再被修改。对于请求上下文、用户上下文、租户上下文、TraceId 等数据，通常应在入口处一次性创建，然后在调用链中只读使用。

不可变上下文可以降低并发场景下的数据竞争风险，也能让上下文的数据流向更清晰。调用方负责创建和绑定上下文，下游代码负责读取上下文，而不是修改上下文。

推荐使用以下对象类型承载 Scoped Value 数据：

| 类型                      | 说明                                                  |
| ------------------------- | ----------------------------------------------------- |
| `record`                  | 推荐，用于定义简单不可变上下文对象                    |
| 只有 `final` 字段的普通类 | 可用于需要兼容旧版本 Java 或复杂构造逻辑的场景        |
| 不可变集合                | 例如 `List.copyOf(...)`、`Map.copyOf(...)` 创建的集合 |
| 简单值对象                | 例如 `String`、`Long`、`UUID` 等不可变类型            |

不推荐放入以下类型：

| 类型           | 问题                                           |
| -------------- | ---------------------------------------------- |
| 可变 Map       | 下游代码可能修改上下文字段，导致数据来源不清晰 |
| 可变 DTO       | 容易被业务逻辑复用和修改，破坏上下文稳定性     |
| 数据库连接对象 | 生命周期和资源释放不适合交给 Scoped Value 管理 |
| 大对象缓存     | Scoped Value 不是缓存容器                      |
| 跨请求共享对象 | Scoped Value 的生命周期应限定在当前调用作用域  |

在项目设计中，建议将请求相关字段聚合为一个不可变上下文对象，例如 `RequestContext`。该对象可以包含 TraceId、用户 ID、租户 ID、请求来源、客户端 IP 等字段。这样可以减少多个 Scoped Value 分散定义的问题，也便于统一校验和扩展。

上下文对象应尽量保持轻量，不要放入完整用户实体、完整权限树、大型配置对象或可变业务对象。Scoped Value 承载的是“上下文标识”和“上下文摘要”，不是完整业务状态。

### 嵌套作用域

Scoped Value 支持嵌套作用域。嵌套作用域表示在已有绑定的基础上，临时为同一个 Scoped Value 绑定一个新的值。新的绑定只在内部作用域生效，内部作用域结束后，会自动恢复外层作用域的绑定值。

嵌套作用域适合处理临时身份切换、内部任务隔离、局部 TraceId 重写、子流程上下文覆盖等场景。例如在一个请求处理过程中，外层绑定的是当前用户上下文；某个内部任务需要以系统身份执行，就可以在内部创建一个新的作用域绑定系统用户上下文。内部任务结束后，外层仍然恢复为原来的用户上下文。

嵌套作用域需要注意以下规则：

1. 内层绑定不会修改外层绑定。
2. 内层作用域结束后，外层绑定会自动恢复。
3. 下游方法读取到的是距离当前执行位置最近的绑定值。
4. 不应通过嵌套作用域频繁覆盖上下文，否则会降低代码可读性。
5. 嵌套作用域应有明确业务含义，例如“系统代执行”“租户切换”“子任务隔离”。

嵌套作用域不是变量赋值，也不是在当前作用域中修改原有值。它本质上是创建一个新的临时绑定范围。这个设计可以避免下游代码随意修改上游上下文，从而保证上下文传递方向更稳定。

在实际项目中，嵌套作用域应谨慎使用。正常请求链路中通常只需要在入口绑定一次上下文。只有在明确需要局部覆盖上下文时，才应创建嵌套作用域。对于复杂嵌套场景，建议在封装方法名称中体现业务意图，例如 `runAsSystem(...)`、`runWithTenant(...)`、`runWithTraceId(...)`，不要在业务代码中直接散落 `ScopedValue.where(...)` 调用。

### 线程继承关系

Scoped Value 不仅可以在同一线程的调用链中传递上下文，也可以在结构化并发模型中传递给子线程。OpenJDK 对 Scoped Value 的目标描述包括在同一线程的被调用方法以及子线程之间共享不可变数据，并特别强调它与虚拟线程、结构化并发结合时具有更低的空间和时间成本。([OpenJDK](https://openjdk.org/jeps/506?utm_source=chatgpt.com))

需要注意，Scoped Value 的线程继承并不是对所有线程创建方式都无条件生效。它主要面向结构化并发场景，尤其是通过 `StructuredTaskScope` 创建的子任务。JEP 487 对 preview 阶段的说明中也提到，父线程中的 Scoped Value 绑定可以被 `StructuredTaskScope` 创建的子线程自动继承，而不像 ThreadLocal 那样复制父线程的绑定数据。([OpenJDK](https://openjdk.org/jeps/487?utm_source=chatgpt.com))

在同一线程中，Scoped Value 的读取范围由当前动态作用域决定。只要方法是在 `run(...)` 或 `call(...)` 执行过程中被直接或间接调用，就可以读取当前绑定值。

在线程继承场景中，可以按以下方式理解：

| 场景                         | 是否适合使用 Scoped Value 传递上下文 | 说明                                                         |
| ---------------------------- | ------------------------------------ | ------------------------------------------------------------ |
| 普通同步方法调用             | 适合                                 | 同一调用链内直接读取当前作用域绑定                           |
| 虚拟线程中的结构化子任务     | 适合                                 | 配合 `StructuredTaskScope` 使用，父作用域上下文可被子任务读取 |
| 平台线程中的结构化子任务     | 适合                                 | 关键在于是否使用结构化并发模型                               |
| 手动 `new Thread(...)`       | 不建议依赖                           | 生命周期不受结构化作用域约束，容易破坏边界                   |
| 普通线程池 `ExecutorService` | 不建议直接依赖                       | 线程复用和任务生命周期可能与作用域不一致                     |
| 异步回调链路                 | 谨慎使用                             | 需要明确任务是否仍在绑定作用域内执行                         |

Scoped Value 的线程继承设计强调“有边界的并发”。父任务创建子任务后，应在父作用域结束前等待子任务完成。这样可以保证子线程不会在父作用域结束后继续访问已经失效的上下文。

这也是 Scoped Value 更适合与 StructuredTaskScope 搭配使用的原因。结构化并发要求子任务的生命周期受父任务控制，父任务可以等待、取消和收敛子任务结果。Scoped Value 的上下文继承正好建立在这种清晰的生命周期边界之上。

在 Java 项目开发中，应避免将 Scoped Value 作为普通线程池任务的隐式上下文传递方案。如果业务需要在线程池、MQ 消费、定时任务、异步回调中传递上下文，应显式构造新的上下文作用域，而不是假设原请求线程中的 Scoped Value 会自动传递到其他线程。

推荐原则如下：

1. 同步调用链中，可以直接使用 Scoped Value 读取当前上下文。
2. 结构化并发中，可以通过 StructuredTaskScope 让子任务继承父作用域上下文。
3. 普通线程池任务中，应显式重新绑定上下文。
4. MQ、定时任务、异步事件等脱离原请求生命周期的场景，应重新创建独立上下文。
5. 不要把 Scoped Value 当作跨线程全局变量使用。



## 项目设计

本节用于说明在 Java 项目中如何设计 Scoped Value 的上下文对象、定义方式和业务调用链。Scoped Value 本身只是 JDK 提供的上下文绑定能力，项目中不应直接在各业务类中零散使用，而应通过统一封装形成稳定的上下文访问入口。

### 上下文对象设计

上下文对象用于承载当前调用链中需要被多个组件读取的公共信息，例如 TraceId、用户 ID、租户 ID、客户端 IP、请求来源等。由于 Scoped Value 更适合传递不可变数据，上下文对象应优先设计为不可变对象。

推荐使用 `record` 定义上下文对象。`record` 天然具备不可变字段、构造方法、访问方法、`equals`、`hashCode` 和 `toString`，适合承载轻量上下文数据。

上下文对象设计时应遵循以下原则：

| 设计项     | 建议                                           |
| ---------- | ---------------------------------------------- |
| 对象类型   | 优先使用 `record`，字段保持不可变              |
| 字段数量   | 只放调用链中真正需要共享的上下文字段           |
| 字段内容   | 放标识、摘要、链路信息，不放大型业务对象       |
| 默认值处理 | 在构造阶段完成空值兜底                         |
| 可变对象   | 避免放入可变 `Map`、可变 DTO、集合原始引用     |
| 生命周期   | 只表示当前请求、当前任务或当前业务调用的上下文 |

推荐的上下文字段如下：

| 字段       | 说明                                     |
| ---------- | ---------------------------------------- |
| `traceId`  | 链路追踪 ID，用于日志关联和问题排查      |
| `userId`   | 当前登录用户 ID，未登录场景可为空        |
| `tenantId` | 当前租户 ID，多租户系统中用于数据隔离    |
| `source`   | 请求来源，例如 `web`、`app`、`job`、`mq` |
| `clientIp` | 客户端 IP，用于审计和风控                |

上下文对象不建议包含完整用户实体、完整租户实体、权限树、数据库连接、缓存对象或可变业务对象。Scoped Value 传递的是“当前调用的上下文摘要”，不是业务状态容器。

上下文对象应放在公共模块中，例如 `common-core` 或 `common-context`，供 Web 层、业务层、日志组件、审计组件共同使用。

推荐目录结构如下：

```text
src/main/java/io/github/atengk/scoped/context/
├── RequestContext.java
├── RequestContextHolder.java
└── RequestContextFilter.java
```

该对象用于承载当前请求的上下文摘要，适合放在 `common-core` 或独立上下文模块中。

```java
package io.github.atengk.scoped.context;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

/**
 * 请求上下文
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record RequestContext(
        String traceId,
        String userId,
        String tenantId,
        String source,
        String clientIp
) {

    public RequestContext {
        traceId = StrUtil.blankToDefault(traceId, IdUtil.fastSimpleUUID());
        tenantId = StrUtil.blankToDefault(tenantId, "default");
        source = StrUtil.blankToDefault(source, "unknown");
        clientIp = StrUtil.blankToDefault(clientIp, "unknown");
    }

}
```

### Scoped Value 定义方式

Scoped Value 推荐定义为 `private static final` 字段，并集中封装在 Holder 类中。JEP 506 和 JDK 25 API 文档都强调，`ScopedValue` 通常作为静态字段声明，并通过访问控制限制可见范围；它可以在限定执行期间绑定值，并在执行完成后自动恢复为未绑定状态。([OpenJDK](https://openjdk.org/jeps/506?utm_source=chatgpt.com))

不建议在业务 Service、Controller 或工具类中直接暴露 `ScopedValue` 变量。更好的做法是通过 `RequestContextHolder` 提供统一的绑定、读取和判断方法。

Scoped Value 定义方式建议如下：

| 设计项     | 建议                                           |
| ---------- | ---------------------------------------------- |
| 字段修饰符 | `private static final`                         |
| 创建方式   | `ScopedValue.newInstance()`                    |
| 暴露方式   | 不直接暴露变量，只暴露封装方法                 |
| 绑定方法   | 统一封装 `runWith`、`callWith`                 |
| 读取方法   | 统一封装 `getRequired`、`getOrNull`、`isBound` |
| 异常策略   | 未绑定时明确抛出业务可理解的异常               |

该 Holder 类统一管理请求上下文的绑定和读取，业务代码只依赖该类，不直接操作 `ScopedValue` 变量。

```java
package io.github.atengk.scoped.context;

import java.lang.ScopedValue;

/**
 * 请求上下文持有器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public final class RequestContextHolder {

    private static final ScopedValue<RequestContext> REQUEST_CONTEXT = ScopedValue.newInstance();

    private RequestContextHolder() {
    }

    public static void runWith(RequestContext context, Runnable runnable) {
        ScopedValue.where(REQUEST_CONTEXT, context).run(runnable);
    }

    public static <T, E extends Exception> T callWith(RequestContext context, ScopedValue.CallableOp<T, E> callable) throws E {
        return ScopedValue.where(REQUEST_CONTEXT, context).call(callable);
    }

    public static RequestContext getRequired() {
        if (!REQUEST_CONTEXT.isBound()) {
            throw new IllegalStateException("当前请求上下文未绑定");
        }
        return REQUEST_CONTEXT.get();
    }

    public static RequestContext getOrNull() {
        return REQUEST_CONTEXT.orElse(null);
    }

    public static boolean isBound() {
        return REQUEST_CONTEXT.isBound();
    }

}
```

这种封装方式可以避免业务代码到处调用 `ScopedValue.where(...)` 和 `ScopedValue.get()`，也便于后续统一增加监控、异常处理、默认值策略或兼容逻辑。

### 业务调用链设计

业务调用链设计的核心是：在入口处绑定上下文，在下游业务中只读使用，在作用域结束后自动释放。Scoped Value 不应在调用链中途被随意修改，也不应作为跨请求的状态存储。

典型 Web 请求调用链如下：

```text
HTTP 请求
  ↓
Filter / Interceptor 解析请求头
  ↓
构造 RequestContext
  ↓
RequestContextHolder.runWith(...) 绑定上下文
  ↓
Controller
  ↓
Service
  ↓
Repository / Client / Audit / Log
  ↓
作用域结束，RequestContext 自动失效
```

在这个调用链中，Filter 或 Interceptor 是上下文入口。它负责从请求头、认证信息、网关透传信息中提取上下文数据，并创建 `RequestContext`。Controller 和 Service 不需要显式传递 TraceId、租户 ID 等上下文参数，下游组件可以通过 `RequestContextHolder.getRequired()` 读取当前上下文。

推荐的设计分层如下：

| 层级          | 职责                                         |
| ------------- | -------------------------------------------- |
| Web 入口层    | 解析请求头、认证信息、租户信息，绑定上下文   |
| Controller 层 | 处理接口入参，不直接关心上下文绑定细节       |
| Service 层    | 处理业务逻辑，可读取当前用户、租户、TraceId  |
| Repository 层 | 可根据租户上下文处理数据隔离                 |
| 日志 / 审计层 | 读取 TraceId、用户 ID、租户 ID，记录操作行为 |
| 异步 / 并发层 | 根据线程模型决定是否显式重新绑定上下文       |

设计时应避免以下做法：

1. 在多个业务类中重复定义 `ScopedValue`。
2. 在 Service 中随意创建新的上下文作用域。
3. 将普通业务参数全部放入 `RequestContext`。
4. 在上下文对象中放入可变业务对象。
5. 假设普通线程池任务可以自动读取原请求上下文。

## 开发实现

本节给出一个基于 Spring Boot 的基础实现示例，用于演示如何定义 Scoped Value 变量、绑定上下文数据、在业务代码中读取上下文，以及在多层调用中传递上下文。示例使用 JDK 25 的正式 `java.lang.ScopedValue` API；如果使用 `StructuredTaskScope`，仍需注意该 API 在 JDK 25 中属于 preview。([Oracle Docs](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/ScopedValue.html?utm_source=chatgpt.com))

### 定义 Scoped Value 变量

Scoped Value 变量应集中定义在上下文持有器中，并设置为 `private static final`。这样可以将上下文访问能力限制在指定封装类内，避免外部代码直接绑定或读取内部上下文。

推荐定义方式如下：

```java
private static final ScopedValue<RequestContext> REQUEST_CONTEXT = ScopedValue.newInstance();
```

完整实现建议使用前文的 `RequestContextHolder`。业务代码不直接接触 `REQUEST_CONTEXT`，只调用封装方法：

| 方法            | 说明                                |
| --------------- | ----------------------------------- |
| `runWith(...)`  | 绑定上下文并执行无返回值逻辑        |
| `callWith(...)` | 绑定上下文并执行有返回值逻辑        |
| `getRequired()` | 获取当前上下文，未绑定时抛出异常    |
| `getOrNull()`   | 获取当前上下文，未绑定时返回 `null` |
| `isBound()`     | 判断当前线程是否已绑定上下文        |

这种方式可以让 Scoped Value 成为项目基础设施的一部分，而不是散落在业务代码中的低层 API 调用。

### 绑定上下文数据

绑定上下文数据通常发生在请求入口，例如 Spring Boot 的 `Filter`、`HandlerInterceptor` 或网关适配层。这里以 `OncePerRequestFilter` 为例，在请求进入 Controller 之前创建并绑定 `RequestContext`。

该过滤器从请求头中读取 TraceId、用户 ID、租户 ID 等信息，并在当前请求处理作用域内绑定上下文。

```java
package io.github.atengk.scoped.context;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 请求上下文过滤器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {

    private static final String HEADER_TRACE_ID = "X-Trace-Id";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_TENANT_ID = "X-Tenant-Id";
    private static final String HEADER_SOURCE = "X-Source";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RequestContext context = buildRequestContext(request);
        response.setHeader(HEADER_TRACE_ID, context.traceId());

        try {
            RequestContextHolder.callWith(context, () -> {
                log.debug("绑定请求上下文，traceId={}，tenantId={}，userId={}",
                        context.traceId(), context.tenantId(), context.userId());
                filterChain.doFilter(request, response);
                return null;
            });
        } catch (IOException | ServletException e) {
            throw e;
        } catch (Exception e) {
            log.error("请求上下文处理异常，traceId={}", context.traceId(), e);
            throw new ServletException("请求上下文处理异常", e);
        }
    }

    private RequestContext buildRequestContext(HttpServletRequest request) {
        String traceId = StrUtil.blankToDefault(request.getHeader(HEADER_TRACE_ID), IdUtil.fastSimpleUUID());
        String userId = request.getHeader(HEADER_USER_ID);
        String tenantId = request.getHeader(HEADER_TENANT_ID);
        String source = request.getHeader(HEADER_SOURCE);
        String clientIp = getClientIp(request);

        return new RequestContext(traceId, userId, tenantId, source, clientIp);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwardedFor)) {
            List<String> ipList = StrUtil.split(forwardedFor, ',');
            if (CollUtil.isNotEmpty(ipList)) {
                return StrUtil.trim(ipList.getFirst());
            }
        }
        return request.getRemoteAddr();
    }

}
```

绑定逻辑应尽量靠近请求入口。这样可以保证后续 Controller、Service、日志组件、审计组件都在同一个上下文作用域内执行。

如果项目中使用网关统一生成 TraceId，应用侧应优先读取请求头中的 TraceId；如果请求头中不存在，再本地生成新的 TraceId。

### 在业务代码中读取上下文

业务代码读取上下文时，不需要知道 Scoped Value 的绑定细节，只需要调用 `RequestContextHolder.getRequired()`。如果业务逻辑必须依赖上下文，建议使用 `getRequired()`；如果是兼容定时任务、单元测试或非 Web 入口场景，可以使用 `getOrNull()` 做降级处理。

该业务服务演示在 Service 中读取当前请求上下文，并记录审计日志。

```java
package io.github.atengk.scoped.service;

import io.github.atengk.scoped.context.RequestContext;
import io.github.atengk.scoped.context.RequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 操作审计服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class AuditService {

    public void recordOperation(String operation) {
        RequestContext context = RequestContextHolder.getRequired();

        log.info("记录业务操作，traceId={}，tenantId={}，userId={}，operation={}",
                context.traceId(), context.tenantId(), context.userId(), operation);

        // 这里可以继续写入数据库、消息队列或审计日志系统
    }

}
```

如果业务代码不是强依赖请求上下文，可以使用兼容写法。

```java
package io.github.atengk.scoped.service;

import io.github.atengk.scoped.context.RequestContext;
import io.github.atengk.scoped.context.RequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通用日志服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class CommonLogService {

    public void record(String message) {
        RequestContext context = RequestContextHolder.getOrNull();

        if (context == null) {
            log.info("记录日志，message={}", message);
            return;
        }

        log.info("记录日志，traceId={}，tenantId={}，userId={}，message={}",
                context.traceId(), context.tenantId(), context.userId(), message);
    }

}
```

推荐在核心业务中使用强依赖方式，在通用工具、兼容任务或测试代码中使用可降级方式。这样既能保证关键链路上下文完整，也能避免非请求场景下出现不必要的异常。

### 多层调用传递上下文

Scoped Value 的优势在于多层调用时不需要显式传递上下文参数。只要下游代码运行在绑定作用域内，就可以读取当前上下文。JDK 25 API 文档将其描述为一种“隐式方法参数”，即中间方法不需要声明该参数，但拥有 `ScopedValue` 访问权限的代码可以读取绑定值。([Oracle Docs](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/ScopedValue.html?utm_source=chatgpt.com))

典型调用链如下：

```text
OrderController.createOrder(...)
  ↓
OrderService.createOrder(...)
  ↓
InventoryService.lockStock(...)
  ↓
AuditService.recordOperation(...)
  ↓
RequestContextHolder.getRequired()
```

Controller 不需要把 `RequestContext` 作为参数传给 Service，Service 也不需要继续传给 AuditService。上下文由请求入口绑定，后续链路按需读取。

该 Controller 提供订单创建接口，业务入参只保留业务数据，不混入 TraceId、租户 ID 等技术上下文。

```java
package io.github.atengk.scoped.controller;

import io.github.atengk.scoped.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/{orderId}/submit")
    public String submitOrder(@PathVariable String orderId) {
        orderService.submitOrder(orderId);
        return "success";
    }

}
```

该业务服务处理订单提交，并在下游审计服务中读取当前上下文。

```java
package io.github.atengk.scoped.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final InventoryService inventoryService;
    private final AuditService auditService;

    public void submitOrder(String orderId) {
        if (StrUtil.isBlank(orderId)) {
            throw new IllegalArgumentException("订单ID不能为空");
        }

        log.info("开始提交订单，orderId={}", orderId);
        inventoryService.lockStock(orderId);
        auditService.recordOperation(StrUtil.format("提交订单：{}", orderId));
        log.info("订单提交完成，orderId={}", orderId);
    }

}
```

该库存服务不需要感知上下文传递细节，只处理自己的业务逻辑。

```java
package io.github.atengk.scoped.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 库存服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class InventoryService {

    public void lockStock(String orderId) {
        log.info("锁定订单库存，orderId={}", orderId);

        // 这里可以调用库存系统、数据库或远程服务
    }

}
```

这种设计能保持业务接口清晰。业务方法只声明业务参数，上下文数据由基础设施层提供，日志、审计、权限、租户等横切逻辑按需读取。

## 与并发模型结合

本节说明 Scoped Value 在平台线程、虚拟线程和 StructuredTaskScope 中的使用方式。需要重点区分：Scoped Value 在同一调用链中可以直接读取，但跨线程使用时必须关注线程创建方式和生命周期边界。JDK 25 文档明确说明，Scoped Value 的跨线程共享限制在结构化场景中；使用 `StructuredTaskScope` 时，创建作用域时会捕获当前线程的 Scoped Value 绑定，并由该作用域内 fork 的子任务继承。([Oracle Docs](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/StructuredTaskScope.html?utm_source=chatgpt.com))

### 平台线程中的使用

平台线程是传统 Java 线程模型。对于普通同步调用链，只要代码在同一个绑定作用域内执行，就可以直接读取 Scoped Value。

例如，Web 请求由平台线程处理时，Filter 中绑定的 `RequestContext` 可以被 Controller、Service 和 Repository 读取。这个场景不需要额外处理。

需要注意的是，普通线程池中的任务不会因为提交动作发生在绑定作用域内，就天然具备清晰的上下文继承边界。线程池中的线程会复用，任务执行时间也可能晚于当前请求作用域。因此，在 `ExecutorService` 中使用 Scoped Value 时，推荐显式捕获当前上下文，并在任务内部重新绑定。

该示例演示在平台线程池任务中显式重新绑定请求上下文。

```java
package io.github.atengk.scoped.concurrent;

import io.github.atengk.scoped.context.RequestContext;
import io.github.atengk.scoped.context.RequestContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 平台线程任务服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformThreadTaskService {

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public void submitAsyncTask(String taskName) {
        RequestContext context = RequestContextHolder.getRequired();

        executorService.submit(() -> RequestContextHolder.runWith(context, () -> {
            RequestContext current = RequestContextHolder.getRequired();
            log.info("平台线程执行任务，traceId={}，tenantId={}，taskName={}",
                    current.traceId(), current.tenantId(), taskName);

            // 执行业务任务
        }));
    }

}
```

这种方式的关键是不要依赖线程池自动继承上下文，而是把当前上下文作为不可变对象捕获下来，再在任务内部创建新的 Scoped Value 作用域。

平台线程使用建议如下：

1. 同步调用链中可以直接读取当前上下文。
2. 普通线程池任务中应显式捕获并重新绑定上下文。
3. 不要把 Scoped Value 当作线程池中的长期状态。
4. 异步任务执行完成后，作用域会自动结束，不需要手动清理。
5. 如果任务脱离当前请求生命周期，应重新构造独立上下文，而不是复用请求上下文。

### 虚拟线程中的使用

虚拟线程适合处理大量阻塞式任务，例如 HTTP 调用、数据库访问、文件 IO 等。Scoped Value 与虚拟线程结合时，可以减少 ThreadLocal 在大量线程场景下的管理成本。JEP 506 也明确提到，Scoped Value 在与虚拟线程和结构化并发结合使用时具有更低的空间和时间成本。([OpenJDK](https://openjdk.org/jeps/506?utm_source=chatgpt.com))

如果一个请求本身运行在虚拟线程中，并且后续业务逻辑仍然是同步调用链，那么使用方式与平台线程基本一致。入口绑定上下文，下游读取上下文即可。

如果使用 `Executors.newVirtualThreadPerTaskExecutor()` 创建新的虚拟线程任务，也建议显式捕获并重新绑定上下文。原因与普通线程池类似：任务已经进入新的执行线程，最好在任务入口处重新建立明确作用域。

该示例演示在虚拟线程任务中显式重新绑定请求上下文。

```java
package io.github.atengk.scoped.concurrent;

import io.github.atengk.scoped.context.RequestContext;
import io.github.atengk.scoped.context.RequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 虚拟线程任务服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class VirtualThreadTaskService {

    public void submitVirtualThreadTask(String taskName) {
        RequestContext context = RequestContextHolder.getRequired();

        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            executorService.submit(() -> RequestContextHolder.runWith(context, () -> {
                RequestContext current = RequestContextHolder.getRequired();
                log.info("虚拟线程执行任务，traceId={}，tenantId={}，taskName={}",
                        current.traceId(), current.tenantId(), taskName);

                // 执行阻塞式业务逻辑，例如远程调用、数据库查询、文件处理
            }));
        }
    }

}
```

虚拟线程使用 Scoped Value 的推荐原则如下：

| 场景                     | 建议                                            |
| ------------------------ | ----------------------------------------------- |
| 请求线程本身是虚拟线程   | 在入口绑定，下游同步调用直接读取                |
| 使用虚拟线程执行独立任务 | 捕获当前上下文，并在任务内部重新绑定            |
| 大量短生命周期任务       | 优先使用不可变上下文，避免 ThreadLocal 可变状态 |
| 脱离请求生命周期的任务   | 不复用请求上下文，重新创建任务上下文            |
| 需要父子任务协作         | 优先考虑 StructuredTaskScope                    |

虚拟线程并不意味着上下文可以随意跨线程自动传播。项目中仍然应保持清晰的上下文边界，避免把 Scoped Value 当成全局上下文。

### StructuredTaskScope 中的上下文传递

`StructuredTaskScope` 是结构化并发模型中的核心类，用于在一个明确的父作用域内创建、等待和管理多个子任务。与普通线程池不同，结构化并发强调子任务的生命周期受父任务约束，父任务不会在子任务结束前脱离管理范围。

Scoped Value 与 `StructuredTaskScope` 的结合是推荐使用方式之一。JDK 25 API 文档说明，打开 `StructuredTaskScope` 时会捕获当前线程的 Scoped Value 绑定，这些绑定会被该任务作用域中通过 `fork` 创建的子线程继承；同时，父任务在 `close` 之前不会继续越过子任务生命周期，从而避免上下文在子任务仍运行时提前失效。([Oracle Docs](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/StructuredTaskScope.html?utm_source=chatgpt.com))

需要注意，在 JDK 25 中，`ScopedValue` 已经是正式 API，但 `StructuredTaskScope` 仍然是 preview API。使用该类时，编译和运行都需要开启 `--enable-preview`。([Oracle Docs](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/concurrent/StructuredTaskScope.html?utm_source=chatgpt.com))

该示例演示在结构化并发中读取父作用域绑定的请求上下文。

```java
package io.github.atengk.scoped.concurrent;

import io.github.atengk.scoped.context.RequestContext;
import io.github.atengk.scoped.context.RequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.StructuredTaskScope;

/**
 * 结构化并发查询服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class StructuredQueryService {

    public OrderSummary queryOrderSummary(String orderId) throws InterruptedException {
        RequestContext context = RequestContextHolder.getRequired();
        log.info("开始聚合查询订单摘要，traceId={}，orderId={}", context.traceId(), orderId);

        try (var scope = StructuredTaskScope.open()) {
            StructuredTaskScope.Subtask<String> orderTask = scope.fork(() -> queryOrder(orderId));
            StructuredTaskScope.Subtask<String> stockTask = scope.fork(() -> queryStock(orderId));
            StructuredTaskScope.Subtask<String> deliveryTask = scope.fork(() -> queryDelivery(orderId));

            scope.join();

            return new OrderSummary(orderTask.get(), stockTask.get(), deliveryTask.get());
        }
    }

    private String queryOrder(String orderId) {
        RequestContext context = RequestContextHolder.getRequired();
        log.info("查询订单信息，traceId={}，tenantId={}，orderId={}",
                context.traceId(), context.tenantId(), orderId);
        return "order:" + orderId;
    }

    private String queryStock(String orderId) {
        RequestContext context = RequestContextHolder.getRequired();
        log.info("查询库存信息，traceId={}，tenantId={}，orderId={}",
                context.traceId(), context.tenantId(), orderId);
        return "stock:" + orderId;
    }

    private String queryDelivery(String orderId) {
        RequestContext context = RequestContextHolder.getRequired();
        log.info("查询配送信息，traceId={}，tenantId={}，orderId={}",
                context.traceId(), context.tenantId(), orderId);
        return "delivery:" + orderId;
    }

}
```

该对象用于承载结构化并发聚合后的查询结果。

```java
package io.github.atengk.scoped.concurrent;

/**
 * 订单摘要
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record OrderSummary(
        String orderInfo,
        String stockInfo,
        String deliveryInfo
) {
}
```

结构化并发中的调用关系如下：

```text
RequestContextHolder.runWith(context, ...)
  ↓
StructuredTaskScope.open()
  ↓
scope.fork(queryOrder)
scope.fork(queryStock)
scope.fork(queryDelivery)
  ↓
子任务继承父作用域中的 Scoped Value 绑定
  ↓
scope.join()
  ↓
父任务聚合结果
  ↓
StructuredTaskScope 关闭，子任务生命周期结束
  ↓
Scoped Value 外层作用域结束后自动失效
```

StructuredTaskScope 场景下的使用建议如下：

1. 在打开 `StructuredTaskScope` 之前完成 Scoped Value 绑定。
2. 子任务中可以直接读取父作用域绑定的上下文。
3. 上下文对象必须保持不可变，或保证跨线程访问时具备同步保护。
4. 不要在子任务未完成前提前结束父作用域。
5. 不要在普通线程池中套用结构化并发的上下文继承假设。
6. 如果使用 JDK 25 的 `StructuredTaskScope`，需要为编译和运行开启 preview。

对于需要并发聚合多个远程接口、多个数据库查询或多个 IO 任务的场景，`StructuredTaskScope + Scoped Value` 是更清晰的组合。它既能保持上下文传递的隐式性，又能保证并发任务的生命周期边界明确。



## 典型业务场景

本节用于说明 Scoped Value 在 Java 后端项目中的常见落地方式。Scoped Value 适合承载一次请求、一次任务或一次业务调用中的上下文摘要，尤其适合那些需要被多层业务代码读取，但又不适合作为普通业务参数逐层传递的数据。

### 请求上下文传递

请求上下文传递是 Scoped Value 最典型的使用场景。Web 请求进入系统后，通常会携带 TraceId、用户 ID、租户 ID、客户端 IP、请求来源等信息。这些信息并不是某一个业务方法的核心入参，但日志、审计、权限、数据隔离、异常处理等组件都可能需要读取。

传统做法通常是在 Filter 或 Interceptor 中将这些数据放入 ThreadLocal，然后在请求结束时手动清理。Scoped Value 的优势在于它通过作用域绑定管理上下文生命周期，请求处理完成后上下文自动失效，减少了手动清理遗漏的风险。

请求上下文传递的典型流程如下：

```text
HTTP 请求
  ↓
Filter / Interceptor 解析请求头
  ↓
构造 RequestContext
  ↓
RequestContextHolder.runWith(...) 绑定上下文
  ↓
Controller 处理接口
  ↓
Service 执行业务逻辑
  ↓
日志、审计、权限、数据隔离组件读取上下文
  ↓
请求处理结束，上下文自动失效
```

在请求上下文中建议放入以下字段：

| 字段       | 来源                      | 用途                          |
| ---------- | ------------------------- | ----------------------------- |
| `traceId`  | 网关透传或应用生成        | 日志追踪、异常定位、链路关联  |
| `userId`   | 登录态、Token、请求头     | 权限判断、操作审计、数据归属  |
| `tenantId` | 域名、Token、请求头、网关 | 多租户数据隔离、缓存隔离      |
| `source`   | 请求头、客户端标识        | 区分 Web、App、Job、MQ 等来源 |
| `clientIp` | 请求头或连接信息          | 审计、安全风控、访问分析      |

请求上下文不应设计得过重。它只应包含调用链中需要共享的轻量信息，不应包含完整用户对象、权限树、数据库连接、缓存对象或大型业务 DTO。

该服务演示在业务代码中读取当前请求上下文，并用于业务日志和审计记录。

```java
package io.github.atengk.scoped.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.scoped.context.RequestContext;
import io.github.atengk.scoped.context.RequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 请求上下文业务服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class RequestContextBizService {

    public void handleRequestBiz(String bizId) {
        if (StrUtil.isBlank(bizId)) {
            throw new IllegalArgumentException("业务ID不能为空");
        }

        RequestContext context = RequestContextHolder.getRequired();

        log.info("处理请求业务，traceId={}，tenantId={}，userId={}，bizId={}",
                context.traceId(), context.tenantId(), context.userId(), bizId);

        // 这里继续执行业务逻辑，例如查询数据、调用远程服务、写入审计日志
    }

}
```

该场景的重点不是减少所有方法参数，而是避免将 TraceId、租户 ID、客户端 IP 这类横切上下文污染到每一层业务方法签名中。

### 用户信息传递

用户信息传递适合在认证完成后，将当前用户的关键身份信息绑定到当前请求作用域中。业务代码可以读取当前用户上下文，用于权限判断、数据过滤、操作审计和业务规则判断。

用户上下文应保持轻量，建议只放用户标识和少量摘要信息，例如用户 ID、用户名、角色编码、部门 ID、数据权限标识等。不要将完整用户实体、完整权限树或可变会话对象直接放入 Scoped Value。

推荐的用户上下文字段如下：

| 字段       | 说明                     |
| ---------- | ------------------------ |
| `userId`   | 当前登录用户 ID          |
| `username` | 当前登录用户名           |
| `roleCode` | 当前用户主角色或角色摘要 |
| `deptId`   | 当前用户所属部门 ID      |
| `admin`    | 是否为管理员             |

如果用户信息较多，可以单独定义 `UserContext`，也可以将核心字段合并到 `RequestContext` 中。对于中小型项目，将用户 ID、租户 ID、TraceId 统一放入 `RequestContext` 更简单；对于权限模型复杂的项目，可以拆分为 `RequestContext` 和 `UserContext`。

该用户上下文对象用于承载当前登录用户的轻量身份信息。

```java
package io.github.atengk.scoped.context;

import cn.hutool.core.util.StrUtil;

/**
 * 用户上下文
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record UserContext(
        String userId,
        String username,
        String roleCode,
        String deptId,
        boolean admin
) {

    public UserContext {
        username = StrUtil.blankToDefault(username, "unknown");
        roleCode = StrUtil.blankToDefault(roleCode, "USER");
        deptId = StrUtil.blankToDefault(deptId, "default");
    }

}
```

如果项目需要单独管理用户上下文，可以定义独立的 Holder。

```java
package io.github.atengk.scoped.context;

import java.lang.ScopedValue;

/**
 * 用户上下文持有器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public final class UserContextHolder {

    private static final ScopedValue<UserContext> USER_CONTEXT = ScopedValue.newInstance();

    private UserContextHolder() {
    }

    public static void runWith(UserContext context, Runnable runnable) {
        ScopedValue.where(USER_CONTEXT, context).run(runnable);
    }

    public static <T, E extends Exception> T callWith(UserContext context, ScopedValue.CallableOp<T, E> callable) throws E {
        return ScopedValue.where(USER_CONTEXT, context).call(callable);
    }

    public static UserContext getRequired() {
        if (!USER_CONTEXT.isBound()) {
            throw new IllegalStateException("当前用户上下文未绑定");
        }
        return USER_CONTEXT.get();
    }

    public static UserContext getOrNull() {
        return USER_CONTEXT.orElse(null);
    }

    public static boolean isBound() {
        return USER_CONTEXT.isBound();
    }

}
```

该服务演示如何在业务方法中读取当前用户信息，并进行基础权限判断。

```java
package io.github.atengk.scoped.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.scoped.context.UserContext;
import io.github.atengk.scoped.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户权限业务服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class UserPermissionService {

    public void checkDeletePermission(String resourceId) {
        if (StrUtil.isBlank(resourceId)) {
            throw new IllegalArgumentException("资源ID不能为空");
        }

        UserContext userContext = UserContextHolder.getRequired();

        if (!userContext.admin()) {
            log.warn("用户无删除权限，userId={}，resourceId={}", userContext.userId(), resourceId);
            throw new SecurityException("当前用户无删除权限");
        }

        log.info("用户权限校验通过，userId={}，resourceId={}", userContext.userId(), resourceId);
    }

}
```

用户信息传递要避免两个问题：第一，不要把用户上下文当成登录状态存储；第二，不要在下游业务中修改用户上下文。如果业务中需要切换身份，例如系统代执行、管理员代操作，应创建嵌套作用域，而不是修改已有上下文对象。

### TraceId 传递

TraceId 传递用于将同一次请求、同一次任务或同一条业务链路中的日志关联起来。它通常由网关生成并通过请求头透传，也可以由应用入口在缺失时自动生成。

在 Scoped Value 场景中，TraceId 可以作为 `RequestContext` 的一个字段传递。下游日志、异常处理、远程调用、消息发送、审计记录都可以从当前上下文中读取 TraceId。

TraceId 的典型使用位置如下：

| 使用位置 | 说明                                  |
| -------- | ------------------------------------- |
| 请求入口 | 解析或生成 TraceId                    |
| 响应头   | 将 TraceId 返回给调用方，便于问题排查 |
| 业务日志 | 输出 TraceId，关联一次请求中的日志    |
| 异常处理 | 输出 TraceId，便于定位错误链路        |
| 远程调用 | 将 TraceId 透传给下游服务             |
| 消息发送 | 将 TraceId 写入消息 Header            |

该客户端服务演示在远程调用前读取当前 TraceId，并将其写入请求头。

```java
package io.github.atengk.scoped.service;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.scoped.context.RequestContext;
import io.github.atengk.scoped.context.RequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 远程调用服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class RemoteClientService {

    public void callRemoteService(String url) {
        RequestContext context = RequestContextHolder.getRequired();

        Map<String, String> headers = MapUtil.<String, String>builder()
                .put("X-Trace-Id", context.traceId())
                .put("X-Tenant-Id", context.tenantId())
                .put("X-User-Id", context.userId())
                .build();

        log.info("调用远程服务，traceId={}，url={}，headers={}", context.traceId(), url, headers);

        // 这里可以使用 RestClient、OpenFeign、OkHttp 等组件发起调用
    }

}
```

TraceId 传递时需要注意以下规则：

1. 优先使用上游透传的 TraceId，不要在每一层服务重复生成。
2. 如果上游没有 TraceId，应在入口统一生成。
3. TraceId 应写入响应头，方便调用方和运维排查问题。
4. 远程调用、MQ 消息、异步任务应显式透传 TraceId。
5. 不要在业务链路中途随意替换 TraceId，除非是明确的新链路或子任务。

对于日志系统，如果项目已经使用 MDC，可以在请求入口同时将 TraceId 写入 MDC，并在作用域结束后清理 MDC。Scoped Value 用于业务代码读取上下文，MDC 用于日志框架自动输出字段，二者可以配合使用，不是互斥关系。

### 租户信息传递

租户信息传递主要用于 SaaS、多租户后台、多组织系统或平台型系统。请求入口解析租户 ID 后，可以将租户信息绑定到当前作用域中。后续数据权限、缓存 Key、文件路径、消息 Topic、远程调用 Header 都可以读取租户上下文。

租户信息适合放入 Scoped Value 的原因是：它通常由请求入口统一确定，在一次调用链中保持不变，并且需要被多个横切组件读取。

租户上下文的典型使用位置如下：

| 使用位置 | 说明                  |
| -------- | --------------------- |
| 数据查询 | 自动追加租户过滤条件  |
| 数据写入 | 自动填充租户 ID       |
| 缓存 Key | 按租户隔离缓存数据    |
| 对象存储 | 按租户隔离文件目录    |
| 远程调用 | 向下游服务透传租户 ID |
| 审计日志 | 记录操作所属租户      |

该服务演示如何使用租户 ID 构造缓存 Key，避免不同租户的数据混用。

```java
package io.github.atengk.scoped.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.scoped.context.RequestContext;
import io.github.atengk.scoped.context.RequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 租户缓存服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class TenantCacheService {

    public String buildTenantCacheKey(String bizType, String bizId) {
        if (StrUtil.hasBlank(bizType, bizId)) {
            throw new IllegalArgumentException("缓存业务类型和业务ID不能为空");
        }

        RequestContext context = RequestContextHolder.getRequired();
        String cacheKey = StrUtil.format("tenant:{}:{}:{}", context.tenantId(), bizType, bizId);

        log.debug("构造租户缓存Key，tenantId={}，cacheKey={}", context.tenantId(), cacheKey);
        return cacheKey;
    }

}
```

该服务演示在数据查询时读取当前租户 ID。实际项目中可以在 MyBatis-Plus 拦截器、SQL 拼接组件或数据权限组件中使用类似逻辑。

```java
package io.github.atengk.scoped.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.scoped.context.RequestContext;
import io.github.atengk.scoped.context.RequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 租户数据服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class TenantDataService {

    public void queryTenantData(String tableName) {
        if (StrUtil.isBlank(tableName)) {
            throw new IllegalArgumentException("表名不能为空");
        }

        RequestContext context = RequestContextHolder.getRequired();

        log.info("查询租户数据，tenantId={}，tableName={}", context.tenantId(), tableName);

        // 实际项目中建议通过 MyBatis-Plus 拦截器或数据权限组件统一追加 tenant_id 条件
    }

}
```

租户信息传递需要特别注意安全边界。租户 ID 不应完全信任前端传入，通常应由网关、认证中心、Token、域名或服务端租户解析规则确定。业务代码可以读取租户上下文，但不应随意覆盖租户 ID。确实需要跨租户操作时，应创建明确的系统管理作用域，并做好审计记录。

## 测试与验证

本节用于说明如何验证 Scoped Value 的绑定、读取、隔离和并发行为。Scoped Value 的测试重点不是单纯验证 API 能否调用，而是验证上下文是否只在预期作用域内有效，是否不会串扰，是否能在多层调用和并发任务中按预期传递。

### 单元测试

单元测试主要验证基础绑定和读取能力。测试目标包括：绑定后可以读取上下文，作用域结束后上下文失效，未绑定时读取会抛出明确异常。

建议先对 `RequestContextHolder` 做基础单元测试，保证上下文持有器的行为稳定。

该测试类验证请求上下文的基础绑定、读取和作用域结束行为。

```java
package io.github.atengk.scoped.context;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 请求上下文持有器单元测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class RequestContextHolderTest {

    @Test
    void shouldReadContextWhenBound() {
        RequestContext context = new RequestContext(
                "trace-001",
                "user-001",
                "tenant-001",
                "web",
                "127.0.0.1"
        );

        RequestContextHolder.runWith(context, () -> {
            RequestContext current = RequestContextHolder.getRequired();

            Assertions.assertEquals("trace-001", current.traceId());
            Assertions.assertEquals("user-001", current.userId());
            Assertions.assertEquals("tenant-001", current.tenantId());
            Assertions.assertEquals("web", current.source());
        });
    }

    @Test
    void shouldThrowExceptionWhenContextNotBound() {
        IllegalStateException exception = Assertions.assertThrows(
                IllegalStateException.class,
                RequestContextHolder::getRequired
        );

        Assertions.assertEquals("当前请求上下文未绑定", exception.getMessage());
    }

    @Test
    void shouldUnbindAfterScopeFinished() {
        RequestContext context = new RequestContext(
                "trace-002",
                "user-002",
                "tenant-002",
                "web",
                "127.0.0.1"
        );

        RequestContextHolder.runWith(context, () -> {
            Assertions.assertTrue(RequestContextHolder.isBound());
            Assertions.assertEquals("trace-002", RequestContextHolder.getRequired().traceId());
        });

        Assertions.assertFalse(RequestContextHolder.isBound());
    }

}
```

如果项目使用 Maven，可以引入 JUnit 5 测试依赖。Spring Boot 项目通常已经通过 `spring-boot-starter-test` 间接提供 JUnit 5。

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

运行测试命令如下：

```bash
mvn test
```

如果项目使用 JDK 25，可以直接运行测试。如果项目仍使用 JDK 21 到 JDK 24 的 preview API，需要确保 Maven 编译和测试运行阶段都开启 `--enable-preview`。

### 作用域隔离验证

作用域隔离验证用于确认 Scoped Value 的绑定不会污染外层或其他作用域。重点验证两个场景：第一，内层作用域可以临时覆盖外层值；第二，内层作用域结束后，外层值会自动恢复。

该测试类验证嵌套作用域之间的隔离关系。

```java
package io.github.atengk.scoped.context;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 请求上下文作用域隔离测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class RequestContextScopeIsolationTest {

    @Test
    void shouldRestoreOuterContextAfterNestedScopeFinished() {
        RequestContext outerContext = new RequestContext(
                "trace-outer",
                "user-outer",
                "tenant-outer",
                "web",
                "127.0.0.1"
        );

        RequestContext innerContext = new RequestContext(
                "trace-inner",
                "user-inner",
                "tenant-inner",
                "job",
                "127.0.0.2"
        );

        RequestContextHolder.runWith(outerContext, () -> {
            Assertions.assertEquals("trace-outer", RequestContextHolder.getRequired().traceId());
            Assertions.assertEquals("tenant-outer", RequestContextHolder.getRequired().tenantId());

            RequestContextHolder.runWith(innerContext, () -> {
                Assertions.assertEquals("trace-inner", RequestContextHolder.getRequired().traceId());
                Assertions.assertEquals("tenant-inner", RequestContextHolder.getRequired().tenantId());
                Assertions.assertEquals("job", RequestContextHolder.getRequired().source());
            });

            Assertions.assertEquals("trace-outer", RequestContextHolder.getRequired().traceId());
            Assertions.assertEquals("tenant-outer", RequestContextHolder.getRequired().tenantId());
            Assertions.assertEquals("web", RequestContextHolder.getRequired().source());
        });

        Assertions.assertFalse(RequestContextHolder.isBound());
    }

    @Test
    void shouldNotLeakContextBetweenIndependentScopes() {
        RequestContext firstContext = new RequestContext(
                "trace-first",
                "user-first",
                "tenant-first",
                "web",
                "127.0.0.1"
        );

        RequestContext secondContext = new RequestContext(
                "trace-second",
                "user-second",
                "tenant-second",
                "app",
                "127.0.0.2"
        );

        RequestContextHolder.runWith(firstContext, () -> {
            Assertions.assertEquals("trace-first", RequestContextHolder.getRequired().traceId());
        });

        RequestContextHolder.runWith(secondContext, () -> {
            Assertions.assertEquals("trace-second", RequestContextHolder.getRequired().traceId());
            Assertions.assertEquals("tenant-second", RequestContextHolder.getRequired().tenantId());
        });

        Assertions.assertFalse(RequestContextHolder.isBound());
    }

}
```

作用域隔离测试可以帮助确认 Scoped Value 不会像清理不当的 ThreadLocal 一样残留数据。只要绑定作用域结束，上下文就应恢复为未绑定状态，或者恢复到外层作用域的绑定值。

实际项目中建议将以下隔离场景都纳入测试：

| 测试场景       | 验证目标                               |
| -------------- | -------------------------------------- |
| 单层作用域     | 作用域内可读，作用域外不可读           |
| 嵌套作用域     | 内层覆盖后，退出内层恢复外层           |
| 异常作用域     | 作用域内部抛出异常后，上下文仍自动失效 |
| 多次独立作用域 | 前一次绑定不会影响后一次绑定           |
| 未绑定读取     | 抛出明确异常或返回降级值               |

异常作用域也建议补充测试。该测试用于验证业务异常不会导致上下文泄漏。

```java
package io.github.atengk.scoped.context;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 请求上下文异常隔离测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class RequestContextExceptionIsolationTest {

    @Test
    void shouldUnbindContextWhenExceptionThrown() {
        RequestContext context = new RequestContext(
                "trace-exception",
                "user-exception",
                "tenant-exception",
                "web",
                "127.0.0.1"
        );

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () ->
                RequestContextHolder.runWith(context, () -> {
                    Assertions.assertTrue(RequestContextHolder.isBound());
                    throw new RuntimeException("模拟业务异常");
                })
        );

        Assertions.assertEquals("模拟业务异常", exception.getMessage());
        Assertions.assertFalse(RequestContextHolder.isBound());
    }

}
```

### 并发场景验证

并发场景验证用于确认多个线程、多个虚拟线程或多个任务同时执行时，上下文不会互相串扰。Scoped Value 的并发测试需要重点区分两类情况：同步调用链中直接读取，跨线程任务中显式重新绑定。

普通线程池或虚拟线程任务不应假设可以自动读取提交方的上下文。推荐做法是在提交任务前捕获当前不可变上下文对象，并在任务内部通过 `RequestContextHolder.runWith(...)` 重新绑定。

该测试类验证多个虚拟线程任务中显式绑定的上下文互不影响。

```java
package io.github.atengk.scoped.context;

import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 请求上下文并发测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class RequestContextConcurrentTest {

    @Test
    void shouldIsolateContextInVirtualThreads() throws Exception {
        int taskCount = 20;
        List<Future<String>> futures = new ArrayList<>();

        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                int index = i;
                RequestContext context = new RequestContext(
                        StrUtil.format("trace-{}", index),
                        StrUtil.format("user-{}", index),
                        StrUtil.format("tenant-{}", index),
                        "test",
                        "127.0.0.1"
                );

                Future<String> future = executorService.submit(() ->
                        RequestContextHolder.callWith(context, () -> {
                            RequestContext current = RequestContextHolder.getRequired();

                            Assertions.assertEquals(StrUtil.format("trace-{}", index), current.traceId());
                            Assertions.assertEquals(StrUtil.format("tenant-{}", index), current.tenantId());

                            return current.traceId();
                        })
                );

                futures.add(future);
            }

            for (int i = 0; i < taskCount; i++) {
                Assertions.assertEquals(StrUtil.format("trace-{}", i), futures.get(i).get());
            }
        }

        Assertions.assertFalse(RequestContextHolder.isBound());
    }

}
```

如果项目仍然使用平台线程池，也可以用固定线程池验证上下文隔离。由于平台线程会复用，更应显式重新绑定上下文。

该测试类验证固定线程池复用线程时，不同任务之间的上下文不会串扰。

```java
package io.github.atengk.scoped.context;

import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 平台线程上下文隔离测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class RequestContextPlatformThreadTest {

    @Test
    void shouldIsolateContextInPlatformThreadPool() throws Exception {
        int taskCount = 30;
        List<Future<String>> futures = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        try {
            for (int i = 0; i < taskCount; i++) {
                int index = i;
                RequestContext context = new RequestContext(
                        StrUtil.format("trace-platform-{}", index),
                        StrUtil.format("user-platform-{}", index),
                        StrUtil.format("tenant-platform-{}", index),
                        "thread-pool",
                        "127.0.0.1"
                );

                Future<String> future = executorService.submit(() ->
                        RequestContextHolder.callWith(context, () -> {
                            RequestContext current = RequestContextHolder.getRequired();

                            Assertions.assertEquals(StrUtil.format("trace-platform-{}", index), current.traceId());
                            Assertions.assertEquals(StrUtil.format("tenant-platform-{}", index), current.tenantId());

                            return current.traceId();
                        })
                );

                futures.add(future);
            }

            for (int i = 0; i < taskCount; i++) {
                Assertions.assertEquals(StrUtil.format("trace-platform-{}", i), futures.get(i).get());
            }
        } finally {
            executorService.shutdown();
            boolean terminated = executorService.awaitTermination(10, TimeUnit.SECONDS);
            Assertions.assertTrue(terminated, "线程池未按预期关闭");
        }

        Assertions.assertFalse(RequestContextHolder.isBound());
    }

}
```

并发测试建议覆盖以下目标：

| 测试项         | 验证目标                           |
| -------------- | ---------------------------------- |
| 多线程独立绑定 | 不同任务读取各自上下文             |
| 线程池复用     | 前一个任务上下文不会污染后一个任务 |
| 虚拟线程任务   | 大量短生命周期任务中上下文隔离正常 |
| 异常任务       | 任务抛出异常后上下文自动失效       |
| 显式重新绑定   | 跨线程任务不依赖隐式上下文传播     |

如果项目使用 `StructuredTaskScope`，还可以增加结构化并发测试，验证子任务能读取父作用域中的 Scoped Value 绑定。由于 `StructuredTaskScope` 在 JDK 25 中仍属于 preview API，测试和运行时需要额外开启 `--enable-preview`。在普通业务项目中，可以先完成同步调用、平台线程池和虚拟线程三类测试，再根据是否使用结构化并发补充专项测试。

测试结论应重点关注两点：第一，Scoped Value 的作用域边界是否清晰；第二，并发任务之间是否存在上下文串扰。只要这两点得到验证，Scoped Value 就可以较安全地用于请求上下文、用户信息、TraceId 和租户信息等业务场景。



## 使用规范

本节用于规范 Scoped Value 在项目中的命名、封装、异常处理和使用边界。Scoped Value 虽然使用简单，但如果缺少统一约束，容易被误用为全局变量、隐式参数仓库或线程上下文替代品，最终降低代码可读性和可维护性。

### 命名规范

Scoped Value 的命名应体现其承载的是“上下文绑定值”，而不是普通变量或业务状态。命名应清晰、稳定、具备领域含义，避免使用过于宽泛的名称。

Scoped Value 变量本身建议使用大写下划线命名，并放在对应的 Holder 类中。例如：

| 类型       | 推荐命名          | 不推荐命名                 |
| ---------- | ----------------- | -------------------------- |
| 请求上下文 | `REQUEST_CONTEXT` | `CONTEXT`、`VALUE`、`DATA` |
| 用户上下文 | `USER_CONTEXT`    | `USER`、`CURRENT_USER`     |
| 租户上下文 | `TENANT_CONTEXT`  | `TENANT`、`TENANT_DATA`    |
| TraceId    | `TRACE_ID`        | `ID`、`TRACE`              |
| 任务上下文 | `TASK_CONTEXT`    | `TASK`、`PARAMS`           |

如果上下文字段较多，优先定义一个上下文对象，例如 `RequestContext`，再定义一个 `ScopedValue<RequestContext>`。不建议为每一个字段都定义独立的 Scoped Value，例如同时定义 `TRACE_ID`、`USER_ID`、`TENANT_ID`、`CLIENT_IP`。字段过多时，分散定义会增加绑定复杂度，也不利于后续扩展。

上下文对象命名应以 `Context` 结尾，表示它是某个调用范围内的上下文摘要。

| 场景           | 上下文对象命名   |
| -------------- | ---------------- |
| Web 请求上下文 | `RequestContext` |
| 用户上下文     | `UserContext`    |
| 租户上下文     | `TenantContext`  |
| 任务上下文     | `TaskContext`    |
| 审计上下文     | `AuditContext`   |
| 消息消费上下文 | `MessageContext` |

Holder 类建议以 `Holder` 结尾，表示它是上下文绑定和读取入口。

| 上下文对象       | Holder 类              |
| ---------------- | ---------------------- |
| `RequestContext` | `RequestContextHolder` |
| `UserContext`    | `UserContextHolder`    |
| `TenantContext`  | `TenantContextHolder`  |
| `TaskContext`    | `TaskContextHolder`    |

方法命名应直接表达绑定、读取和判断语义。

| 方法名               | 说明                                |
| -------------------- | ----------------------------------- |
| `runWith(...)`       | 绑定上下文并执行无返回值逻辑        |
| `callWith(...)`      | 绑定上下文并执行有返回值逻辑        |
| `getRequired()`      | 获取当前上下文，未绑定时抛出异常    |
| `getOrNull()`        | 获取当前上下文，未绑定时返回 `null` |
| `orElse(...)`        | 获取当前上下文，未绑定时返回默认值  |
| `isBound()`          | 判断当前作用域是否已绑定上下文      |
| `runAsSystem(...)`   | 使用系统身份创建嵌套作用域          |
| `runWithTenant(...)` | 使用指定租户创建嵌套作用域          |

命名时应避免使用 `setContext(...)`、`clearContext(...)` 这类 ThreadLocal 风格的方法名。Scoped Value 的重点是作用域绑定和自动失效，不是手动设置和手动清理。

### 封装规范

Scoped Value 不应在业务代码中直接散落使用，应通过统一 Holder 类进行封装。业务代码只依赖上下文读取方法，不直接操作 `ScopedValue.where(...)`、`ScopedValue.get()` 或底层静态变量。

推荐封装方式如下：

| 封装点     | 规范                                               |
| ---------- | -------------------------------------------------- |
| 变量定义   | `private static final ScopedValue<T>`              |
| 构造方法   | Holder 类使用私有构造方法，禁止实例化              |
| 绑定入口   | 提供 `runWith(...)` 和 `callWith(...)`             |
| 读取入口   | 提供 `getRequired()`、`getOrNull()`、`isBound()`   |
| 异常策略   | 在 Holder 内统一处理未绑定异常                     |
| 业务使用   | Service、组件、工具类只调用 Holder 方法            |
| 作用域创建 | 优先在 Filter、Interceptor、任务入口、消息入口创建 |

封装的目标是将 Scoped Value 作为基础设施能力使用，而不是让业务类直接感知底层 API。这样可以降低后续迁移成本，也便于统一扩展日志、校验、默认值和兼容策略。

推荐的调用边界如下：

| 层级                 | 是否建议绑定上下文 | 是否建议读取上下文 |
| -------------------- | ------------------ | ------------------ |
| Filter / Interceptor | 建议               | 可以               |
| Controller           | 一般不建议         | 可以               |
| Service              | 不建议随意绑定     | 可以               |
| Repository / Mapper  | 不建议绑定         | 谨慎读取           |
| 审计组件             | 不建议绑定         | 建议读取           |
| 日志组件             | 不建议绑定         | 建议读取           |
| 远程调用组件         | 不建议绑定         | 建议读取并透传     |
| 线程池任务入口       | 建议显式重新绑定   | 可以               |
| MQ 消费入口          | 建议创建独立上下文 | 可以               |
| 定时任务入口         | 建议创建独立上下文 | 可以               |

绑定上下文应尽量发生在“调用入口”，例如 Web 请求入口、消息消费入口、定时任务入口、异步任务入口。业务中间层原则上只读取上下文，不创建新的上下文作用域。

如果确实需要在业务中创建嵌套作用域，必须有明确的业务语义，例如系统代执行、租户切换、子任务隔离。此类方法应封装为语义化方法，例如 `runAsSystem(...)` 或 `runWithTenant(...)`，不要在业务逻辑中直接散落底层绑定代码。

上下文对象也应统一封装，不应直接使用可变 Map 承载上下文。推荐使用 `record` 或不可变类。上下文字段应保持轻量，只承载标识和摘要信息。

### 异常处理规范

Scoped Value 的异常处理重点在于“未绑定上下文”的处理。未绑定通常有三类原因：第一，调用代码不在预期作用域内；第二，入口层没有正确绑定上下文；第三，代码运行在异步线程、定时任务或测试环境中，但没有重新创建上下文。

对于强依赖上下文的业务逻辑，应使用 `getRequired()`，在上下文不存在时立即抛出异常。这样可以快速暴露调用链设计问题，避免业务在上下文缺失时继续执行。

适合使用 `getRequired()` 的场景包括：

| 场景                 | 原因                                  |
| -------------------- | ------------------------------------- |
| 数据权限判断         | 缺少用户或租户上下文会导致权限错误    |
| 租户数据查询         | 缺少租户 ID 可能导致越权访问          |
| 审计日志记录         | 缺少用户信息会影响审计完整性          |
| 关键远程调用         | 缺少 TraceId 或租户 ID 会影响链路追踪 |
| 需要登录态的业务操作 | 缺少用户上下文应直接失败              |

对于非强依赖上下文的通用能力，可以使用 `getOrNull()` 或默认值方式降级。例如通用日志工具、单元测试辅助代码、部分本地任务可以在上下文不存在时使用默认 TraceId 或跳过非关键字段。

适合使用降级方式的场景包括：

| 场景           | 建议                             |
| -------------- | -------------------------------- |
| 通用日志打印   | 无上下文时只打印业务日志         |
| 本地开发测试   | 使用默认上下文或允许为空         |
| 定时任务       | 创建任务上下文，不依赖请求上下文 |
| MQ 消费        | 从消息 Header 创建新上下文       |
| 非关键审计字段 | 可以记录为 `unknown`             |

异常信息应清晰说明问题，不建议只抛出空指针异常或通用运行时异常。推荐的异常信息包括：

| 场景               | 推荐异常信息                                       |
| ------------------ | -------------------------------------------------- |
| 请求上下文未绑定   | `当前请求上下文未绑定`                             |
| 用户上下文未绑定   | `当前用户上下文未绑定`                             |
| 租户上下文未绑定   | `当前租户上下文未绑定`                             |
| TraceId 缺失       | `当前链路追踪ID未绑定`                             |
| 异步任务上下文缺失 | `异步任务上下文未绑定，请在任务入口显式绑定上下文` |

异常处理还需要关注作用域内抛出异常的情况。Scoped Value 的绑定会随着作用域退出而自动失效，即使作用域内部抛出业务异常，也不需要像 ThreadLocal 一样手动 `remove()`。但是业务代码仍然应该记录必要日志，并将异常转换为项目统一异常模型。

建议在入口层、任务层或异步执行层捕获异常并记录 TraceId、用户 ID、租户 ID 等上下文信息。这样可以确保异常日志具备排查价值。

异常处理原则如下：

1. 强依赖上下文的业务，未绑定时直接失败。
2. 非强依赖上下文的工具类，可以使用默认值或降级逻辑。
3. 不要吞掉上下文缺失异常，否则会掩盖调用链设计问题。
4. 异步任务、MQ、定时任务应在入口创建独立上下文。
5. 异常日志应尽量输出 TraceId、租户 ID、用户 ID 等关键字段。
6. 不需要手动清理 Scoped Value，作用域结束后会自动失效。

### 禁止滥用场景

Scoped Value 不能作为全局变量、缓存容器、参数黑洞或线程池上下文自动传播工具使用。它的核心价值是“限定作用域内的只读上下文传递”，超出这个边界后会降低系统可维护性。

禁止将普通业务参数全部放入 Scoped Value。业务参数应通过方法签名显式表达，例如订单 ID、商品 ID、分页参数、查询条件等。如果为了减少参数数量而把所有参数都放进上下文对象，会导致接口语义不清晰，调用关系难以理解，测试也更困难。

禁止将可变对象放入 Scoped Value。可变 Map、可变 DTO、集合原始引用、业务状态对象都不适合作为上下文对象。下游代码一旦修改这些对象，其他调用链中的代码读取到的值就可能发生变化，破坏 Scoped Value 的只读上下文语义。

禁止将数据库连接、网络连接、文件句柄、事务对象等资源对象放入 Scoped Value。这类对象有自己的生命周期管理规则，应该由连接池、事务管理器或资源管理组件负责，不应依赖 Scoped Value 传递和释放。

禁止将 Scoped Value 当作跨请求缓存。Scoped Value 的绑定只在当前动态作用域内有效，作用域结束后应立即失效。需要跨请求复用的数据应使用缓存组件，例如 Caffeine、Redis、本地缓存或数据库，而不是上下文对象。

禁止在普通线程池中假设上下文会自动传播。线程池任务应显式捕获当前不可变上下文，并在任务内部重新绑定。对于脱离当前请求生命周期的任务，应创建独立任务上下文，而不是复用请求上下文。

禁止在业务代码中随意创建嵌套作用域。嵌套作用域必须有明确业务语义，例如系统代执行、租户切换、子任务隔离。如果大量业务方法随意嵌套绑定，会导致上下文值难以追踪，排查问题时很难判断当前读取到的值来自哪个作用域。

常见禁止场景如下：

| 滥用场景           | 问题                               |
| ------------------ | ---------------------------------- |
| 替代所有方法参数   | 破坏方法签名表达能力               |
| 存放可变 Map       | 下游可随意修改，数据流向不清晰     |
| 存放完整用户实体   | 对象过重，且可能被修改             |
| 存放权限树         | 数据过大，不适合每次请求上下文传递 |
| 存放数据库连接     | 资源生命周期不应由上下文控制       |
| 存放事务对象       | 容易破坏事务边界                   |
| 存放缓存数据       | Scoped Value 不是缓存容器          |
| 在线程池中隐式依赖 | 线程复用会导致边界不清             |
| 随意嵌套覆盖       | 增加上下文追踪难度                 |
| 跨请求共享         | 违背作用域生命周期模型             |

使用 Scoped Value 时应始终保持一个判断标准：该数据是否由入口统一创建、在当前调用范围内只读、下游不应修改、作用域结束后必须失效。如果不满足这些条件，就不适合放入 Scoped Value。

## 总结

本节对 Scoped Value 的适用边界和开发注意事项进行归纳。Scoped Value 适合解决 Java 项目中的上下文传递问题，但它不是通用状态管理工具，也不是 ThreadLocal 的无条件替代品。

### 适用边界

Scoped Value 的适用边界可以概括为：在明确的动态作用域内，将不可变上下文从调用方传递给被调用方。它适合读多写少、生命周期短、边界明确、上下文稳定的场景。

适合使用 Scoped Value 的场景包括：

| 场景             | 适用原因                                   |
| ---------------- | ------------------------------------------ |
| 请求上下文       | 请求入口创建，下游多处读取，请求结束后失效 |
| 用户信息         | 当前登录用户在一次请求内保持稳定           |
| TraceId          | 需要贯穿日志、异常、远程调用和审计         |
| 租户信息         | 一次请求内租户标识应保持一致               |
| 审计上下文       | 操作人、租户、来源等字段需要多层读取       |
| 结构化并发子任务 | 父任务上下文需要被受控子任务读取           |
| 消息消费上下文   | 消费入口从消息 Header 创建独立上下文       |
| 定时任务上下文   | 任务入口创建任务级上下文                   |

不适合使用 Scoped Value 的场景包括：

| 场景                 | 不适用原因                            |
| -------------------- | ------------------------------------- |
| 普通业务参数         | 应通过方法签名显式传递                |
| 可变业务状态         | 下游修改会破坏上下文稳定性            |
| 跨请求共享数据       | Scoped Value 生命周期只属于当前作用域 |
| 缓存数据             | 应使用专门缓存组件                    |
| 连接、事务、文件句柄 | 资源生命周期需要专门管理              |
| 大型对象图           | 增加内存压力和理解成本                |
| 需要频繁修改的数据   | Scoped Value 更适合只读传递           |
| 非结构化异步链路     | 需要显式创建新的上下文作用域          |

在项目选型时，可以使用以下判断规则：

1. 如果数据是业务方法的核心入参，使用方法参数。
2. 如果数据是当前调用链的公共上下文，且下游只读，使用 Scoped Value。
3. 如果数据需要在当前线程中频繁修改，谨慎使用 ThreadLocal 或重新设计数据流。
4. 如果数据需要跨请求复用，使用缓存、数据库或配置中心。
5. 如果数据需要跨异步任务传递，优先在任务入口显式重新绑定上下文。
6. 如果并发任务具备父子生命周期关系，优先考虑 `StructuredTaskScope + Scoped Value`。

Scoped Value 的价值不在于隐藏参数，而在于为上下文传递提供更安全、更清晰、更受约束的生命周期模型。

### 开发注意事项

开发中使用 Scoped Value 时，应优先关注上下文边界、对象不可变性、封装方式和并发模型。只要这些约束设计清楚，Scoped Value 可以有效降低 ThreadLocal 清理遗漏和上下文污染的风险。

第一，优先使用 JDK 25 或更高版本。JDK 25 中 `java.lang.ScopedValue` 已经是正式 API，更适合正式项目落地。如果项目仍使用 JDK 21 到 JDK 24，需要开启 preview 特性，并评估长期维护成本。

第二，统一封装上下文访问入口。项目中应通过 `RequestContextHolder`、`UserContextHolder`、`TenantContextHolder` 等类统一管理绑定和读取，不要在业务代码中直接散落 `ScopedValue` 变量。

第三，上下文对象应保持不可变。推荐使用 `record` 或只有 `final` 字段的不可变类。上下文中只放轻量摘要信息，例如 TraceId、用户 ID、租户 ID、来源、客户端 IP，不放完整业务对象。

第四，绑定应发生在入口层。Web 请求在 Filter 或 Interceptor 中绑定，MQ 消费在 Listener 入口绑定，定时任务在任务入口绑定，异步任务在线程执行入口绑定。业务中间层原则上只读取上下文。

第五，普通线程池任务需要显式重新绑定。不要假设提交任务时的上下文会自动传递到线程池任务中。应在提交任务前捕获当前不可变上下文对象，并在任务内部创建新的作用域。

第六，嵌套作用域要有明确语义。只有在系统代执行、租户切换、子任务隔离等场景下才建议使用嵌套绑定。嵌套方法应通过语义化封装表达意图，避免业务代码中直接散落底层 API。

第七，异常处理要区分强依赖和弱依赖。强依赖上下文的业务应使用 `getRequired()`，上下文缺失时立即失败；弱依赖工具类可以使用 `getOrNull()` 或默认值降级。

第八，不要把 Scoped Value 当作 ThreadLocal 的简单替换。ThreadLocal 适合线程私有可变状态，Scoped Value 适合作用域内只读上下文。二者关注点不同，迁移时应结合业务语义重新设计，而不是机械替换 API。

第九，与日志 MDC 可以配合使用。Scoped Value 适合业务代码读取上下文，MDC 适合日志框架自动输出字段。请求入口可以同时绑定 Scoped Value 和写入 MDC，但 MDC 仍需要按日志框架规范进行清理。

第十，测试必须覆盖隔离场景。至少应验证单层作用域、嵌套作用域、异常退出、线程池任务、虚拟线程任务和上下文缺失等场景，确保上下文不会泄漏或串扰。

最终建议是：将 Scoped Value 定位为“作用域内不可变上下文传递工具”。它适合承载请求上下文、用户信息、TraceId、租户信息等横切数据，但不应承载普通业务参数、可变状态或长期资源。项目中只要坚持统一封装、入口绑定、下游只读、作用域自动结束这几个原则，就可以较安全地将 Scoped Value 应用于 Java 后端开发。
