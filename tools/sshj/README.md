# SSHJ

**SSHJ** 是一个用于 Java 的 **SSH 客户端库**，全称通常指 **Java SSH Library（SSHJ）**，主要用于在 Java 程序中实现通过 SSH 协议进行远程操作。

- [Github](https://github.com/hierynomus/sshj)



## 基础配置

**添加依赖**

```xml
<!-- 项目属性 -->
<properties>
    <sshj.version>0.40.0</sshj.version>
</properties>
<!-- 项目依赖 -->
<dependencies>
    <!-- SSHv2 library for Java -->
    <dependency>
        <groupId>com.hierynomus</groupId>
        <artifactId>sshj</artifactId>
        <version>${sshj.version}</version>
    </dependency>
</dependencies>
```



## 使用



```
已完成的 SSHJ 测试方法列表：

1. testSshConnection
   - 基础连接测试（用户名 + 密码）
   - 验证 SSH 是否能正常建立连接

2. testExecCommandBasic
   - 执行单条命令（exec）
   - 获取标准输出 / 错误输出 / 退出状态码

3. testShellInteractive
   - 启动 Shell 交互模式
   - 连续发送命令并读取返回结果

4. testSshConnectionWithPrivateKey
   - 使用私钥进行 SSH 登录
   - 支持无密码 / 有密码私钥

5. testHostKeyVerifierWithKnownHosts
   - 使用 known_hosts 做主机指纹校验
   - 提升连接安全性（避免中间人攻击）
   
   
```



```
接下来建议补全的 SSHJ 测试方法列表（按重要性排序）：

一、连接与会话增强
1. testExecMultiCommand
   - 多命令执行（循环 exec）
   - 每个命令独立 session

2. testExecWithTimeout
   - 命令执行超时控制
   - 防止命令卡死

3. testReuseConnection
   - 复用 SSHClient 多次执行命令
   - 减少频繁连接开销

二、Shell 进阶（WebTerminal 核心）
4. testShellContinuousRead
   - 持续读取 Shell 输出（循环流式读取）
   - 模拟真实终端输出流

5. testShellSendCommandWithDelay
   - 控制命令发送节奏
   - 解决粘包/输出混乱问题

6. testShellExit
   - 发送 exit 关闭 Shell
   - 验证连接释放

三、认证与安全
7. testPasswordAndKeyFallback
   - 密码 + 私钥双方式尝试
   - 提高连接成功率

8. testHostKeyVerifierCustom
   - 自定义 HostKeyVerifier（指纹校验）
   - 精细化安全控制

```

