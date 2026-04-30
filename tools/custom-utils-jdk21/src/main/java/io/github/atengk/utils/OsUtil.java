package io.github.atengk.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 系统运行环境工具类，提供操作系统、JVM、进程、环境变量、路径、命令、网络、容器和诊断等通用能力。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class OsUtil {

    private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
    private static final int DEFAULT_COMMAND_TIMEOUT_MILLIS = 60_000;
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 1_000;
    private static final Pattern CONTAINER_ID_PATTERN = Pattern.compile("[0-9a-fA-F]{64}");
    private static final AtomicInteger SHUTDOWN_HOOK_INDEX = new AtomicInteger(1);

    private OsUtil() {
        throw new UnsupportedOperationException("OsUtil 是静态工具类，禁止实例化");
    }

    /**
     * 获取操作系统名称。
     *
     * @return 操作系统名称
     */
    public static String getOsName() {
        return getProperty("os.name", "");
    }

    /**
     * 获取操作系统版本。
     *
     * @return 操作系统版本
     */
    public static String getOsVersion() {
        return getProperty("os.version", "");
    }

    /**
     * 获取操作系统架构。
     *
     * @return 操作系统架构
     */
    public static String getOsArch() {
        return getProperty("os.arch", "");
    }

    /**
     * 获取操作系统大类。
     *
     * @return 操作系统大类，常见值为 windows、linux、mac、unix、unknown
     */
    public static String getOsFamily() {
        String os = getOsName().toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return "mac";
        }
        if (os.contains("linux")) {
            return "linux";
        }
        if (os.contains("nix") || os.contains("nux") || os.contains("aix")
                || os.contains("sunos") || os.contains("freebsd") || os.contains("openbsd") || os.contains("netbsd")) {
            return "unix";
        }
        return "unknown";
    }

    /**
     * 判断当前操作系统是否为 Windows。
     *
     * @return 是 Windows 返回 true，否则返回 false
     */
    public static boolean isWindows() {
        return "windows".equals(getOsFamily());
    }

    /**
     * 判断当前操作系统是否为 Linux。
     *
     * @return 是 Linux 返回 true，否则返回 false
     */
    public static boolean isLinux() {
        return "linux".equals(getOsFamily());
    }

    /**
     * 判断当前操作系统是否为 macOS。
     *
     * @return 是 macOS 返回 true，否则返回 false
     */
    public static boolean isMac() {
        return "mac".equals(getOsFamily());
    }

    /**
     * 判断当前操作系统是否为类 Unix 系统。
     *
     * @return 是类 Unix 系统返回 true，否则返回 false
     */
    public static boolean isUnixLike() {
        return isLinux() || isMac() || "unix".equals(getOsFamily());
    }

    /**
     * 判断当前系统架构是否为 x86_64 或 amd64。
     *
     * @return 是 x64 架构返回 true，否则返回 false
     */
    public static boolean isX64() {
        String arch = getOsArch().toLowerCase(Locale.ROOT);
        return arch.contains("x86_64") || arch.contains("amd64");
    }

    /**
     * 判断当前系统架构是否为 arm64 或 aarch64。
     *
     * @return 是 arm64 架构返回 true，否则返回 false
     */
    public static boolean isArm64() {
        String arch = getOsArch().toLowerCase(Locale.ROOT);
        return arch.contains("arm64") || arch.contains("aarch64");
    }

    /**
     * 判断当前系统架构是否为 32 位。
     *
     * @return 是 32 位返回 true，否则返回 false
     */
    public static boolean is32Bit() {
        String arch = getOsArch().toLowerCase(Locale.ROOT);
        return !is64Bit() && (arch.contains("86") || arch.contains("32") || arch.contains("i386") || arch.contains("i686"));
    }

    /**
     * 判断当前系统架构是否为 64 位。
     *
     * @return 是 64 位返回 true，否则返回 false
     */
    public static boolean is64Bit() {
        return getOsArch().toLowerCase(Locale.ROOT).contains("64");
    }

    /**
     * 获取当前系统换行符。
     *
     * @return 换行符
     */
    public static String getLineSeparator() {
        return System.lineSeparator();
    }

    /**
     * 获取当前系统文件路径分隔符。
     *
     * @return 文件路径分隔符
     */
    public static String getFileSeparator() {
        return File.separator;
    }

    /**
     * 获取当前系统路径变量分隔符。
     *
     * @return 路径变量分隔符
     */
    public static String getPathSeparator() {
        return File.pathSeparator;
    }

    /**
     * 获取系统临时目录。
     *
     * @return 系统临时目录路径
     */
    public static String getTempDir() {
        return getProperty("java.io.tmpdir", "");
    }

    /**
     * 获取当前用户主目录。
     *
     * @return 当前用户主目录路径
     */
    public static String getUserHomeDir() {
        return getProperty("user.home", "");
    }

    /**
     * 获取当前应用工作目录。
     *
     * @return 当前应用工作目录路径
     */
    public static String getUserDir() {
        return getProperty("user.dir", "");
    }

    /**
     * 获取当前用户桌面目录。
     *
     * @return 当前用户桌面目录路径
     */
    public static String getDesktopDir() {
        return Path.of(getUserHomeDir(), "Desktop").toString();
    }

    /**
     * 获取当前用户下载目录。
     *
     * @return 当前用户下载目录路径
     */
    public static String getDownloadDir() {
        return Path.of(getUserHomeDir(), "Downloads").toString();
    }

    /**
     * 获取 Java 版本。
     *
     * @return Java 版本
     */
    public static String getJavaVersion() {
        return getProperty("java.version", "");
    }

    /**
     * 获取 Java 供应商。
     *
     * @return Java 供应商
     */
    public static String getJavaVendor() {
        return getProperty("java.vendor", "");
    }

    /**
     * 获取 Java 安装目录。
     *
     * @return Java 安装目录路径
     */
    public static String getJavaHome() {
        return getProperty("java.home", "");
    }

    /**
     * 获取 JVM 名称。
     *
     * @return JVM 名称
     */
    public static String getJvmName() {
        return getProperty("java.vm.name", "");
    }

    /**
     * 获取 JVM 版本。
     *
     * @return JVM 版本
     */
    public static String getJvmVersion() {
        return getProperty("java.vm.version", "");
    }

    /**
     * 获取 JVM 供应商。
     *
     * @return JVM 供应商
     */
    public static String getJvmVendor() {
        return getProperty("java.vm.vendor", "");
    }

    /**
     * 获取 JVM 启动参数。
     *
     * @return JVM 启动参数列表
     */
    public static List<String> getJvmInputArgs() {
        return List.copyOf(ManagementFactory.getRuntimeMXBean().getInputArguments());
    }

    /**
     * 获取 Java classpath。
     *
     * @return classpath 字符串
     */
    public static String getClassPath() {
        return getProperty("java.class.path", "");
    }

    /**
     * 获取 native library path。
     *
     * @return native library path 字符串
     */
    public static String getLibraryPath() {
        return getProperty("java.library.path", "");
    }

    /**
     * 判断当前 Java 版本是否为 21 或更高版本。
     *
     * @return 是 JDK 21 或更高版本返回 true，否则返回 false
     */
    public static boolean isJava21OrLater() {
        return parseJavaMajorVersion(getProperty("java.specification.version", getJavaVersion())) >= 21;
    }

    /**
     * 判断当前运行环境是否支持虚拟线程。
     *
     * @return 支持虚拟线程返回 true，否则返回 false
     */
    public static boolean isVirtualThreadSupported() {
        try {
            Thread thread = Thread.ofVirtual().unstarted(() -> { });
            return thread.isVirtual();
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * 获取当前进程 ID。
     *
     * @return 当前进程 ID
     */
    public static long getPid() {
        return ProcessHandle.current().pid();
    }

    /**
     * 获取当前进程名称。
     *
     * @return 当前进程名称，无法获取时返回空字符串
     */
    public static String getProcessName() {
        String command = getProcessCommand();
        if (isBlank(command)) {
            return "";
        }
        try {
            Path path = Path.of(command);
            Path fileName = path.getFileName();
            return fileName == null ? command : fileName.toString();
        } catch (RuntimeException ex) {
            int index = Math.max(command.lastIndexOf('/'), command.lastIndexOf('\\'));
            return index >= 0 && index + 1 < command.length() ? command.substring(index + 1) : command;
        }
    }

    /**
     * 获取当前进程启动命令。
     *
     * @return 当前进程启动命令，无法获取时返回空字符串
     */
    public static String getProcessCommand() {
        return ProcessHandle.current().info().command().orElse("");
    }

    /**
     * 获取当前进程完整命令行。
     *
     * @return 当前进程完整命令行，无法获取时返回空字符串
     */
    public static String getProcessCommandLine() {
        ProcessHandle.Info info = ProcessHandle.current().info();
        return info.commandLine().orElseGet(() -> {
            String command = info.command().orElse("");
            String[] args = info.arguments().orElse(new String[0]);
            if (isBlank(command)) {
                return String.join(" ", args);
            }
            if (args.length == 0) {
                return command;
            }
            return command + " " + String.join(" ", args);
        });
    }

    /**
     * 获取当前进程启动参数。
     *
     * @return 当前进程启动参数列表
     */
    public static List<String> getProcessArgs() {
        String[] args = ProcessHandle.current().info().arguments().orElse(new String[0]);
        return List.copyOf(Arrays.asList(args));
    }

    /**
     * 获取当前进程启动时间。
     *
     * @return 当前进程启动时间，无法获取时返回 Optional.empty
     */
    public static Optional<Instant> getProcessStartTime() {
        return ProcessHandle.current().info().startInstant();
    }

    /**
     * 获取当前进程运行时长。
     *
     * @return 当前进程运行时长，无法获取启动时间时返回 Duration.ZERO
     */
    public static Duration getProcessUptime() {
        return getProcessStartTime()
                .map(start -> Duration.between(start, Instant.now()))
                .filter(duration -> !duration.isNegative())
                .orElse(Duration.ZERO);
    }

    /**
     * 获取父进程 ID。
     *
     * @return 父进程 ID，无法获取时返回 OptionalLong.empty
     */
    public static OptionalLong getParentPid() {
        Optional<ProcessHandle> parent = ProcessHandle.current().parent();
        return parent.map(processHandle -> OptionalLong.of(processHandle.pid())).orElseGet(OptionalLong::empty);
    }

    /**
     * 获取父进程名称。
     *
     * @return 父进程名称，无法获取时返回 Optional.empty
     */
    public static Optional<String> getParentProcessName() {
        return ProcessHandle.current().parent()
                .flatMap(parent -> parent.info().command())
                .map(command -> {
                    try {
                        Path fileName = Path.of(command).getFileName();
                        return fileName == null ? command : fileName.toString();
                    } catch (RuntimeException ex) {
                        return command;
                    }
                });
    }

    /**
     * 判断指定进程是否存活。
     *
     * @param pid 进程 ID
     * @return 进程存活返回 true，否则返回 false
     */
    public static boolean isProcessAlive(long pid) {
        if (pid <= 0) {
            return false;
        }
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    /**
     * 终止指定进程。
     *
     * @param pid 进程 ID
     * @return 发出终止信号成功返回 true，否则返回 false
     */
    public static boolean killProcess(long pid) {
        validatePid(pid);
        if (pid == getPid()) {
            throw new IllegalArgumentException("不允许通过 killProcess 终止当前进程");
        }
        return ProcessHandle.of(pid).filter(ProcessHandle::isAlive).map(ProcessHandle::destroy).orElse(false);
    }

    /**
     * 终止指定进程及其子进程。
     *
     * @param pid 进程 ID
     * @return 发出终止信号成功返回 true，否则返回 false
     */
    public static boolean killProcessTree(long pid) {
        validatePid(pid);
        if (pid == getPid()) {
            throw new IllegalArgumentException("不允许通过 killProcessTree 终止当前进程");
        }
        Optional<ProcessHandle> optional = ProcessHandle.of(pid).filter(ProcessHandle::isAlive);
        if (optional.isEmpty()) {
            return false;
        }
        ProcessHandle handle = optional.get();
        handle.descendants().forEach(ProcessHandle::destroy);
        return handle.destroy();
    }

    /**
     * 获取指定环境变量。
     *
     * @param name 环境变量名称
     * @return 环境变量值，不存在时返回 null
     */
    public static String getEnv(String name) {
        return System.getenv(requireText(name, "环境变量名称不能为空"));
    }

    /**
     * 获取指定环境变量，不存在时返回默认值。
     *
     * @param name 环境变量名称
     * @param defaultValue 默认值
     * @return 环境变量值或默认值
     */
    public static String getEnv(String name, String defaultValue) {
        String value = getEnv(name);
        return isBlank(value) ? defaultValue : value;
    }

    /**
     * 获取所有环境变量。
     *
     * @return 不可变环境变量 Map
     */
    public static Map<String, String> getEnvMap() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(System.getenv()));
    }

    /**
     * 判断指定环境变量是否存在。
     *
     * @param name 环境变量名称
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasEnv(String name) {
        if (isBlank(name)) {
            return false;
        }
        return System.getenv().containsKey(name);
    }

    /**
     * 获取必填环境变量，不存在或为空时抛出异常。
     *
     * @param name 环境变量名称
     * @return 环境变量值
     */
    public static String getRequiredEnv(String name) {
        String value = getEnv(name);
        if (isBlank(value)) {
            throw new IllegalStateException("缺少必填环境变量: " + name);
        }
        return value;
    }

    /**
     * 获取整数类型环境变量。
     *
     * @param name 环境变量名称
     * @param defaultValue 默认值
     * @return 转换后的整数值，变量不存在或格式错误时返回默认值
     */
    public static int getEnvAsInt(String name, int defaultValue) {
        return parseInt(getEnv(name), defaultValue);
    }

    /**
     * 获取长整数类型环境变量。
     *
     * @param name 环境变量名称
     * @param defaultValue 默认值
     * @return 转换后的长整数值，变量不存在或格式错误时返回默认值
     */
    public static long getEnvAsLong(String name, long defaultValue) {
        return parseLong(getEnv(name), defaultValue);
    }

    /**
     * 获取布尔类型环境变量。
     *
     * @param name 环境变量名称
     * @param defaultValue 默认值
     * @return 转换后的布尔值，变量不存在或格式错误时返回默认值
     */
    public static boolean getEnvAsBoolean(String name, boolean defaultValue) {
        return parseBoolean(getEnv(name), defaultValue);
    }

    /**
     * 获取当前激活环境标识。
     *
     * @return 环境标识，不存在时返回空字符串
     */
    public static String getActiveProfile() {
        String profile = getProperty("spring.profiles.active", null);
        if (!isBlank(profile)) {
            return profile;
        }
        profile = System.getenv("SPRING_PROFILES_ACTIVE");
        if (!isBlank(profile)) {
            return profile;
        }
        profile = System.getenv("APP_ENV");
        if (!isBlank(profile)) {
            return profile;
        }
        profile = System.getenv("PROFILE");
        return isBlank(profile) ? "" : profile;
    }

    /**
     * 判断当前激活环境是否为生产环境。
     *
     * @return 是生产环境返回 true，否则返回 false
     */
    public static boolean isProdEnv() {
        String profile = getActiveProfile().toLowerCase(Locale.ROOT);
        return profile.contains("prod") || profile.contains("production");
    }

    /**
     * 判断当前激活环境是否为开发环境。
     *
     * @return 是开发环境返回 true，否则返回 false
     */
    public static boolean isDevEnv() {
        String profile = getActiveProfile().toLowerCase(Locale.ROOT);
        return profile.contains("dev") || profile.contains("local");
    }

    /**
     * 判断当前激活环境是否为测试环境。
     *
     * @return 是测试环境返回 true，否则返回 false
     */
    public static boolean isTestEnv() {
        String profile = getActiveProfile().toLowerCase(Locale.ROOT);
        return profile.contains("test") || profile.contains("sit") || profile.contains("uat");
    }

    /**
     * 获取指定系统属性。
     *
     * @param key 系统属性键
     * @return 系统属性值，不存在时返回 null
     */
    public static String getProperty(String key) {
        return System.getProperty(requireText(key, "系统属性键不能为空"));
    }

    /**
     * 获取指定系统属性，不存在时返回默认值。
     *
     * @param key 系统属性键
     * @param defaultValue 默认值
     * @return 系统属性值或默认值
     */
    public static String getProperty(String key, String defaultValue) {
        return System.getProperty(requireText(key, "系统属性键不能为空"), defaultValue);
    }

    /**
     * 获取所有系统属性。
     *
     * @return 不可变系统属性 Map
     */
    public static Map<String, String> getPropertyMap() {
        Properties properties = System.getProperties();
        Map<String, String> map = new LinkedHashMap<>();
        for (String name : properties.stringPropertyNames()) {
            map.put(name, properties.getProperty(name));
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * 判断指定系统属性是否存在。
     *
     * @param key 系统属性键
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasProperty(String key) {
        if (isBlank(key)) {
            return false;
        }
        return System.getProperties().containsKey(key);
    }

    /**
     * 获取必填系统属性，不存在或为空时抛出异常。
     *
     * @param key 系统属性键
     * @return 系统属性值
     */
    public static String getRequiredProperty(String key) {
        String value = getProperty(key);
        if (isBlank(value)) {
            throw new IllegalStateException("缺少必填系统属性: " + key);
        }
        return value;
    }

    /**
     * 获取整数类型系统属性。
     *
     * @param key 系统属性键
     * @param defaultValue 默认值
     * @return 转换后的整数值，属性不存在或格式错误时返回默认值
     */
    public static int getPropertyAsInt(String key, int defaultValue) {
        return parseInt(getProperty(key), defaultValue);
    }

    /**
     * 获取长整数类型系统属性。
     *
     * @param key 系统属性键
     * @param defaultValue 默认值
     * @return 转换后的长整数值，属性不存在或格式错误时返回默认值
     */
    public static long getPropertyAsLong(String key, long defaultValue) {
        return parseLong(getProperty(key), defaultValue);
    }

    /**
     * 获取布尔类型系统属性。
     *
     * @param key 系统属性键
     * @param defaultValue 默认值
     * @return 转换后的布尔值，属性不存在或格式错误时返回默认值
     */
    public static boolean getPropertyAsBoolean(String key, boolean defaultValue) {
        return parseBoolean(getProperty(key), defaultValue);
    }

    /**
     * 设置系统属性。
     *
     * @param key 系统属性键
     * @param value 系统属性值
     * @return 原属性值，不存在时返回 null
     */
    public static String setProperty(String key, String value) {
        return System.setProperty(requireText(key, "系统属性键不能为空"), Objects.requireNonNull(value, "系统属性值不能为空"));
    }

    /**
     * 清除系统属性。
     *
     * @param key 系统属性键
     * @return 被清除的属性值，不存在时返回 null
     */
    public static String clearProperty(String key) {
        return System.clearProperty(requireText(key, "系统属性键不能为空"));
    }

    /**
     * 获取当前系统用户名。
     *
     * @return 当前系统用户名
     */
    public static String getUserName() {
        return getProperty("user.name", "");
    }

    /**
     * 获取当前用户主目录。
     *
     * @return 当前用户主目录路径
     */
    public static String getUserHome() {
        return getUserHomeDir();
    }

    /**
     * 获取主机名。
     *
     * @return 主机名，无法获取时返回 unknown
     */
    public static String getHostName() {
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            if (!isBlank(hostName)) {
                return hostName;
            }
        } catch (Exception ignored) {
            // fallback below
        }
        String envName = isWindows() ? System.getenv("COMPUTERNAME") : System.getenv("HOSTNAME");
        return isBlank(envName) ? "unknown" : envName;
    }

    /**
     * 获取本机主要 IP 地址。
     *
     * @return 本机主要 IP 地址，无法获取时返回 127.0.0.1
     */
    public static String getHostAddress() {
        return getLocalIp();
    }

    /**
     * 获取本机所有 IP 地址。
     *
     * @return 本机 IP 地址列表
     */
    public static List<String> getLocalAddressList() {
        return getLocalIps();
    }

    /**
     * 获取本机第一个可用 MAC 地址。
     *
     * @return MAC 地址，无法获取时返回 Optional.empty
     */
    public static Optional<String> getMacAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return Optional.empty();
            }
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                byte[] hardwareAddress = networkInterface.getHardwareAddress();
                if (hardwareAddress != null && hardwareAddress.length > 0) {
                    return Optional.of(formatMacAddress(hardwareAddress));
                }
            }
        } catch (SocketException ignored) {
            // ignore
        }
        return Optional.empty();
    }

    /**
     * 获取机器标识。
     *
     * @return 基于主机名、MAC 地址和系统架构生成的稳定标识
     */
    public static String getMachineId() {
        String raw = getHostName() + "|" + getMacAddress().orElse("") + "|" + getOsArch();
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
    }

    /**
     * 获取当前应用实例 ID。
     *
     * @return 应用实例 ID
     */
    public static String getInstanceId() {
        String property = getProperty("app.instance.id", null);
        if (!isBlank(property)) {
            return property;
        }
        String env = System.getenv("INSTANCE_ID");
        if (!isBlank(env)) {
            return env;
        }
        String hostname = System.getenv("HOSTNAME");
        if (!isBlank(hostname)) {
            return hostname;
        }
        return getMachineId() + "-" + getPid();
    }

    /**
     * 判断当前用户是否为 root 用户。
     *
     * @return 是 root 用户返回 true，否则返回 false
     */
    public static boolean isRootUser() {
        return "root".equals(getUserName());
    }

    /**
     * 判断当前进程是否具备管理员权限。
     *
     * @return 具备管理员权限返回 true，否则返回 false
     */
    public static boolean isAdministrator() {
        if (!isWindows()) {
            return isRootUser();
        }
        try {
            return executeQuietly("net session >nul 2>&1");
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * 获取可用 CPU 核心数。
     *
     * @return 可用 CPU 核心数
     */
    public static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * 获取系统平均负载。
     *
     * @return 系统平均负载，无法获取时返回 -1
     */
    public static double getSystemLoadAverage() {
        return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
    }

    /**
     * 获取 JVM 最大可用内存。
     *
     * @return JVM 最大可用内存字节数
     */
    public static long getJvmMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    /**
     * 获取 JVM 当前总内存。
     *
     * @return JVM 当前总内存字节数
     */
    public static long getJvmTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    /**
     * 获取 JVM 当前空闲内存。
     *
     * @return JVM 当前空闲内存字节数
     */
    public static long getJvmFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    /**
     * 获取 JVM 当前已用内存。
     *
     * @return JVM 当前已用内存字节数
     */
    public static long getJvmUsedMemory() {
        return Math.max(0, getJvmTotalMemory() - getJvmFreeMemory());
    }

    /**
     * 获取 JVM 内存使用信息。
     *
     * @return JVM 内存使用信息 Map
     */
    public static Map<String, Object> getJvmMemoryUsage() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("max", getJvmMaxMemory());
        map.put("total", getJvmTotalMemory());
        map.put("free", getJvmFreeMemory());
        map.put("used", getJvmUsedMemory());
        map.put("usageRate", getMemoryUsageRate());
        map.put("heap", getHeapMemoryUsage());
        map.put("nonHeap", getNonHeapMemoryUsage());
        return Collections.unmodifiableMap(map);
    }

    /**
     * 获取堆内存使用信息。
     *
     * @return 堆内存使用信息
     */
    public static MemoryUsage getHeapMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    }

    /**
     * 获取非堆内存使用信息。
     *
     * @return 非堆内存使用信息
     */
    public static MemoryUsage getNonHeapMemoryUsage() {
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
    }

    /**
     * 获取 JVM 内存使用率。
     *
     * @return JVM 内存使用率，范围通常为 0 到 1
     */
    public static double getMemoryUsageRate() {
        long max = getJvmMaxMemory();
        if (max <= 0) {
            return 0D;
        }
        return Math.min(1D, Math.max(0D, (double) getJvmUsedMemory() / max));
    }

    /**
     * 获取文件系统根目录列表。
     *
     * @return 根目录列表
     */
    public static List<Path> getRootDirs() {
        List<Path> paths = new ArrayList<>();
        for (File root : File.listRoots()) {
            paths.add(root.toPath());
        }
        return List.copyOf(paths);
    }

    /**
     * 获取指定路径所在磁盘总空间。
     *
     * @param path 路径
     * @return 总空间字节数
     */
    public static long getDiskTotalSpace(String path) {
        return existingFile(path).getTotalSpace();
    }

    /**
     * 获取指定路径所在磁盘剩余空间。
     *
     * @param path 路径
     * @return 剩余空间字节数
     */
    public static long getDiskFreeSpace(String path) {
        return existingFile(path).getFreeSpace();
    }

    /**
     * 获取指定路径所在磁盘可用空间。
     *
     * @param path 路径
     * @return 可用空间字节数
     */
    public static long getDiskUsableSpace(String path) {
        return existingFile(path).getUsableSpace();
    }

    /**
     * 获取指定路径所在磁盘已用空间。
     *
     * @param path 路径
     * @return 已用空间字节数
     */
    public static long getDiskUsedSpace(String path) {
        long total = getDiskTotalSpace(path);
        long free = getDiskFreeSpace(path);
        return Math.max(0, total - free);
    }

    /**
     * 获取指定路径所在磁盘使用率。
     *
     * @param path 路径
     * @return 磁盘使用率，范围通常为 0 到 1
     */
    public static double getDiskUsageRate(String path) {
        long total = getDiskTotalSpace(path);
        if (total <= 0) {
            return 0D;
        }
        return Math.min(1D, Math.max(0D, (double) getDiskUsedSpace(path) / total));
    }

    /**
     * 判断指定路径所在磁盘空间是否足够。
     *
     * @param path 路径
     * @param requiredBytes 需要的字节数
     * @return 空间足够返回 true，否则返回 false
     */
    public static boolean isDiskSpaceEnough(String path, long requiredBytes) {
        if (requiredBytes < 0) {
            throw new IllegalArgumentException("requiredBytes 不能小于 0");
        }
        return getDiskUsableSpace(path) >= requiredBytes;
    }

    /**
     * 标准化路径。
     *
     * @param path 路径
     * @return 标准化后的绝对路径字符串
     */
    public static String normalizePath(String path) {
        return Path.of(requireText(path, "路径不能为空")).toAbsolutePath().normalize().toString();
    }

    /**
     * 拼接路径。
     *
     * @param paths 路径片段
     * @return 拼接后的路径字符串
     */
    public static String joinPath(String... paths) {
        if (paths == null || paths.length == 0) {
            throw new IllegalArgumentException("路径片段不能为空");
        }
        Path result = null;
        for (String item : paths) {
            if (isBlank(item)) {
                continue;
            }
            result = result == null ? Path.of(item) : result.resolve(item);
        }
        return result == null ? "" : result.normalize().toString();
    }

    /**
     * 将路径转换为当前系统风格。
     *
     * @param path 路径
     * @return 当前系统风格路径
     */
    public static String toSystemPath(String path) {
        String value = requireText(path, "路径不能为空");
        return value.replace("/", File.separator).replace("\\", File.separator);
    }

    /**
     * 判断路径是否为绝对路径。
     *
     * @param path 路径
     * @return 是绝对路径返回 true，否则返回 false
     */
    public static boolean isAbsolutePath(String path) {
        if (isBlank(path)) {
            return false;
        }
        return Path.of(path).isAbsolute();
    }

    /**
     * 确保目录存在。
     *
     * @param path 目录路径
     * @return 目录 Path
     */
    public static Path ensureDir(String path) {
        try {
            return Files.createDirectories(Path.of(requireText(path, "目录路径不能为空")).toAbsolutePath().normalize());
        } catch (IOException ex) {
            throw new IllegalStateException("创建目录失败: " + path, ex);
        }
    }

    /**
     * 执行系统命令。
     *
     * @param command 命令字符串
     * @return 命令执行结果
     */
    public static CommandResult execute(String command) {
        return execute(command, DEFAULT_COMMAND_TIMEOUT_MILLIS);
    }

    /**
     * 执行系统命令并设置超时时间。
     *
     * @param command 命令字符串
     * @param timeoutMillis 超时时间，单位毫秒
     * @return 命令执行结果
     */
    public static CommandResult execute(String command, long timeoutMillis) {
        return execute(buildShellCommand(command), timeoutMillis);
    }

    /**
     * 使用参数列表执行系统命令。
     *
     * @param command 命令参数列表
     * @return 命令执行结果
     */
    public static CommandResult execute(List<String> command) {
        return execute(command, DEFAULT_COMMAND_TIMEOUT_MILLIS);
    }

    /**
     * 使用参数列表执行系统命令并设置超时时间。
     *
     * @param command 命令参数列表
     * @param timeoutMillis 超时时间，单位毫秒
     * @return 命令执行结果
     */
    public static CommandResult execute(List<String> command, long timeoutMillis) {
        validateCommand(command);
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis 必须大于 0");
        }
        Instant start = Instant.now();
        Process process = null;
        try {
            process = new ProcessBuilder(command).start();
            Process runningProcess = process;
            CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> readStream(runningProcess.getInputStream()));
            CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> readStream(runningProcess.getErrorStream()));
            boolean completed = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                waitQuietly(process);
                String stdout = getFutureValue(stdoutFuture);
                String stderr = getFutureValue(stderrFuture);
                return new CommandResult(-1, false, stdout, stderr, Duration.between(start, Instant.now()), true);
            }
            String stdout = getFutureValue(stdoutFuture);
            String stderr = getFutureValue(stderrFuture);
            int exitCode = process.exitValue();
            return new CommandResult(exitCode, exitCode == 0, stdout, stderr, Duration.between(start, Instant.now()), false);
        } catch (IOException ex) {
            throw new IllegalStateException("命令启动失败: " + command, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            if (process != null) {
                process.destroyForcibly();
            }
            throw new IllegalStateException("命令执行被中断: " + command, ex);
        }
    }

    /**
     * 静默执行系统命令。
     *
     * @param command 命令字符串
     * @return 退出码为 0 返回 true，否则返回 false
     */
    public static boolean executeQuietly(String command) {
        try {
            return execute(command).isSuccess();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * 执行系统命令并返回标准输出。
     *
     * @param command 命令字符串
     * @return 标准输出内容
     */
    public static String executeAndGetOutput(String command) {
        return execute(command).getStdout();
    }

    /**
     * 执行系统命令并按行返回标准输出。
     *
     * @param command 命令字符串
     * @return 标准输出行列表
     */
    public static List<String> executeAndGetLines(String command) {
        String output = executeAndGetOutput(command);
        if (isBlank(output)) {
            return List.of();
        }
        return output.lines().toList();
    }

    /**
     * 执行脚本文件。
     *
     * @param scriptPath 脚本路径
     * @return 命令执行结果
     */
    public static CommandResult executeScript(String scriptPath) {
        return executeScript(scriptPath, List.of());
    }

    /**
     * 执行脚本文件并传入参数。
     *
     * @param scriptPath 脚本路径
     * @param args 脚本参数
     * @return 命令执行结果
     */
    public static CommandResult executeScript(String scriptPath, List<String> args) {
        Path script = Path.of(requireText(scriptPath, "脚本路径不能为空")).toAbsolutePath().normalize();
        if (!Files.isRegularFile(script)) {
            throw new IllegalArgumentException("脚本文件不存在: " + script);
        }
        List<String> command = new ArrayList<>();
        if (isWindows()) {
            String lower = script.getFileName().toString().toLowerCase(Locale.ROOT);
            if (lower.endsWith(".ps1")) {
                command.add("powershell.exe");
                command.add("-ExecutionPolicy");
                command.add("Bypass");
                command.add("-File");
            } else {
                command.add("cmd.exe");
                command.add("/c");
            }
            command.add(script.toString());
        } else if (Files.isExecutable(script)) {
            command.add(script.toString());
        } else {
            command.add("sh");
            command.add(script.toString());
        }
        if (args != null) {
            for (String arg : args) {
                command.add(Objects.requireNonNull(arg, "脚本参数不能为 null"));
            }
        }
        return execute(command);
    }

    /**
     * 判断命令是否可执行。
     *
     * @param command 命令名称或路径
     * @return 可执行返回 true，否则返回 false
     */
    public static boolean canExecute(String command) {
        return findCommand(command).isPresent();
    }

    /**
     * 查找命令路径。
     *
     * @param command 命令名称或路径
     * @return 命令路径，未找到时返回 Optional.empty
     */
    public static Optional<Path> findCommand(String command) {
        if (isBlank(command)) {
            return Optional.empty();
        }
        Path input = Path.of(command);
        if (command.contains("/") || command.contains("\\") || input.isAbsolute()) {
            return Files.isRegularFile(input) && Files.isExecutable(input) ? Optional.of(input.toAbsolutePath().normalize()) : Optional.empty();
        }
        String pathEnv = System.getenv("PATH");
        if (isBlank(pathEnv)) {
            return Optional.empty();
        }
        List<String> extensions = executableExtensions();
        for (String dir : pathEnv.split(Pattern.quote(File.pathSeparator))) {
            if (isBlank(dir)) {
                continue;
            }
            for (String extension : extensions) {
                Path candidate = Path.of(dir, command + extension);
                if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                    return Optional.of(candidate.toAbsolutePath().normalize());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 根据当前系统构建 Shell 命令。
     *
     * @param command 命令字符串
     * @return Shell 命令参数列表
     */
    public static List<String> buildShellCommand(String command) {
        String value = requireText(command, "命令不能为空");
        if (isWindows()) {
            return List.of("cmd.exe", "/c", value);
        }
        String shell = System.getenv("SHELL");
        if (isBlank(shell)) {
            shell = "/bin/sh";
        }
        return List.of(shell, "-c", value);
    }

    /**
     * 判断当前是否存在控制台。
     *
     * @return 存在控制台返回 true，否则返回 false
     */
    public static boolean isConsoleAvailable() {
        return System.console() != null;
    }

    /**
     * 判断当前是否运行在终端环境。
     *
     * @return 是终端环境返回 true，否则返回 false
     */
    public static boolean isTerminal() {
        return isConsoleAvailable() || !isBlank(System.getenv("TERM")) || !isBlank(System.getenv("WT_SESSION"));
    }

    /**
     * 判断当前终端是否支持 ANSI 控制字符。
     *
     * @return 支持 ANSI 返回 true，否则返回 false
     */
    public static boolean isAnsiSupported() {
        String term = System.getenv("TERM");
        if ("dumb".equalsIgnoreCase(term)) {
            return false;
        }
        if (isWindows()) {
            return !isBlank(System.getenv("ANSICON")) || !isBlank(System.getenv("WT_SESSION")) || !isBlank(System.getenv("ConEmuANSI"));
        }
        return isTerminal();
    }

    /**
     * 获取当前 Shell。
     *
     * @return Shell 路径或名称，无法获取时返回 Optional.empty
     */
    public static Optional<String> getShell() {
        String shell = isWindows() ? System.getenv("ComSpec") : System.getenv("SHELL");
        return isBlank(shell) ? Optional.empty() : Optional.of(shell);
    }

    /**
     * 判断当前 Shell 是否为 Bash。
     *
     * @return 是 Bash 返回 true，否则返回 false
     */
    public static boolean isBash() {
        return getShell().map(value -> value.toLowerCase(Locale.ROOT).contains("bash")).orElse(false);
    }

    /**
     * 判断当前 Shell 是否为 Zsh。
     *
     * @return 是 Zsh 返回 true，否则返回 false
     */
    public static boolean isZsh() {
        return getShell().map(value -> value.toLowerCase(Locale.ROOT).contains("zsh")).orElse(false);
    }

    /**
     * 判断当前 Shell 是否为 PowerShell。
     *
     * @return 是 PowerShell 返回 true，否则返回 false
     */
    public static boolean isPowerShell() {
        return getShell().map(value -> value.toLowerCase(Locale.ROOT).contains("powershell") || value.toLowerCase(Locale.ROOT).contains("pwsh")).orElse(false);
    }

    /**
     * 判断当前 Shell 是否为 Windows CMD。
     *
     * @return 是 CMD 返回 true，否则返回 false
     */
    public static boolean isCmd() {
        return getShell().map(value -> value.toLowerCase(Locale.ROOT).endsWith("cmd.exe") || value.toLowerCase(Locale.ROOT).contains("cmd")).orElse(false);
    }

    /**
     * 获取终端宽度。
     *
     * @return 终端宽度，无法获取时返回 -1
     */
    public static int getTerminalWidth() {
        return parseInt(System.getenv("COLUMNS"), -1);
    }

    /**
     * 获取终端高度。
     *
     * @return 终端高度，无法获取时返回 -1
     */
    public static int getTerminalHeight() {
        return parseInt(System.getenv("LINES"), -1);
    }

    /**
     * 判断本机端口是否可用。
     *
     * @param port 端口号，允许 0 表示随机端口
     * @return 可用返回 true，否则返回 false
     */
    public static boolean isPortAvailable(int port) {
        validatePort(port, true);
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(false);
            serverSocket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * 判断远程 TCP 端口是否可连接。
     *
     * @param host 主机名或 IP
     * @param port 端口号
     * @return 可连接返回 true，否则返回 false
     */
    public static boolean isTcpPortOpen(String host, int port) {
        validatePort(port, false);
        String target = requireText(host, "主机不能为空");
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target, port), DEFAULT_CONNECT_TIMEOUT_MILLIS);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * 查找一个可用端口。
     *
     * @return 可用端口
     */
    public static int findAvailablePort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException ex) {
            throw new IllegalStateException("查找可用端口失败", ex);
        }
    }

    /**
     * 从指定端口开始查找可用端口。
     *
     * @param startPort 起始端口
     * @return 可用端口
     */
    public static int findAvailablePort(int startPort) {
        return findAvailablePort(startPort, 65535);
    }

    /**
     * 在指定范围内查找可用端口。
     *
     * @param startPort 起始端口
     * @param endPort 结束端口
     * @return 可用端口
     */
    public static int findAvailablePort(int startPort, int endPort) {
        validatePort(startPort, false);
        validatePort(endPort, false);
        if (startPort > endPort) {
            throw new IllegalArgumentException("startPort 不能大于 endPort");
        }
        for (int port = startPort; port <= endPort; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        throw new IllegalStateException("指定端口范围内没有可用端口: " + startPort + "-" + endPort);
    }

    /**
     * 获取本机主要 IP。
     *
     * @return 本机主要 IP，无法获取时返回 127.0.0.1
     */
    public static String getLocalIp() {
        List<String> ips = getLocalIps();
        for (String ip : ips) {
            if (!ip.startsWith("127.")) {
                return ip;
            }
        }
        return ips.isEmpty() ? "127.0.0.1" : ips.getFirst();
    }

    /**
     * 获取本机所有 IP。
     *
     * @return 本机 IP 列表
     */
    public static List<String> getLocalIps() {
        Set<String> addresses = new LinkedHashSet<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = interfaces.nextElement();
                    if (!networkInterface.isUp() || networkInterface.isVirtual()) {
                        continue;
                    }
                    Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                    while (inetAddresses.hasMoreElements()) {
                        InetAddress address = inetAddresses.nextElement();
                        addresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
            // fallback below
        }
        if (addresses.isEmpty()) {
            addresses.add("127.0.0.1");
        }
        return List.copyOf(addresses);
    }

    /**
     * 判断主机是否为本机地址。
     *
     * @param host 主机名或 IP
     * @return 是本机地址返回 true，否则返回 false
     */
    public static boolean isLocalAddress(String host) {
        if (isBlank(host)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            if (address.isAnyLocalAddress() || address.isLoopbackAddress()) {
                return true;
            }
            Set<String> locals = new HashSet<>(getLocalIps());
            return locals.contains(address.getHostAddress());
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 判断 IP 是否为内网地址。
     *
     * @param ip IP 地址
     * @return 是内网地址返回 true，否则返回 false
     */
    public static boolean isPrivateIp(String ip) {
        if (isBlank(ip)) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address.isSiteLocalAddress()) {
                return true;
            }
            if (address instanceof Inet4Address) {
                byte[] bytes = address.getAddress();
                int first = bytes[0] & 0xFF;
                int second = bytes[1] & 0xFF;
                return first == 10 || (first == 172 && second >= 16 && second <= 31) || (first == 192 && second == 168);
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 判断主机是否为回环地址。
     *
     * @param host 主机名或 IP
     * @return 是回环地址返回 true，否则返回 false
     */
    public static boolean isLoopbackAddress(String host) {
        if (isBlank(host)) {
            return false;
        }
        try {
            return InetAddress.getByName(host).isLoopbackAddress();
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 判断当前进程是否运行在 Docker 容器中。
     *
     * @return 是 Docker 环境返回 true，否则返回 false
     */
    public static boolean isDocker() {
        if (Files.exists(Path.of("/.dockerenv"))) {
            return true;
        }
        String cgroup = readSmallFile(Path.of("/proc/1/cgroup"));
        String lower = cgroup.toLowerCase(Locale.ROOT);
        return lower.contains("docker") || lower.contains("containerd");
    }

    /**
     * 判断当前进程是否运行在 Kubernetes 环境中。
     *
     * @return 是 Kubernetes 环境返回 true，否则返回 false
     */
    public static boolean isKubernetes() {
        return !isBlank(System.getenv("KUBERNETES_SERVICE_HOST"))
                || Files.exists(Path.of("/var/run/secrets/kubernetes.io/serviceaccount"));
    }

    /**
     * 判断当前进程是否运行在容器环境中。
     *
     * @return 是容器环境返回 true，否则返回 false
     */
    public static boolean isContainer() {
        return isDocker() || isKubernetes() || !isBlank(System.getenv("CONTAINER"));
    }

    /**
     * 获取容器 ID。
     *
     * @return 容器 ID，无法获取时返回 Optional.empty
     */
    public static Optional<String> getContainerId() {
        String cgroup = readSmallFile(Path.of("/proc/self/cgroup"));
        Matcher matcher = CONTAINER_ID_PATTERN.matcher(cgroup);
        if (matcher.find()) {
            return Optional.of(matcher.group());
        }
        return Optional.empty();
    }

    /**
     * 获取 Kubernetes Pod 名称。
     *
     * @return Pod 名称，无法获取时返回 Optional.empty
     */
    public static Optional<String> getPodName() {
        String podName = System.getenv("POD_NAME");
        if (isBlank(podName) && isKubernetes()) {
            podName = System.getenv("HOSTNAME");
        }
        return isBlank(podName) ? Optional.empty() : Optional.of(podName);
    }

    /**
     * 获取 Kubernetes Namespace。
     *
     * @return Namespace，无法获取时返回 Optional.empty
     */
    public static Optional<String> getNamespace() {
        String namespace = System.getenv("POD_NAMESPACE");
        if (!isBlank(namespace)) {
            return Optional.of(namespace);
        }
        String fileValue = readSmallFile(Path.of("/var/run/secrets/kubernetes.io/serviceaccount/namespace")).trim();
        return isBlank(fileValue) ? Optional.empty() : Optional.of(fileValue);
    }

    /**
     * 获取 Kubernetes Node 名称。
     *
     * @return Node 名称，无法获取时返回 Optional.empty
     */
    public static Optional<String> getNodeName() {
        String nodeName = System.getenv("NODE_NAME");
        return isBlank(nodeName) ? Optional.empty() : Optional.of(nodeName);
    }

    /**
     * 判断当前进程是否运行在 CI 环境中。
     *
     * @return 是 CI 环境返回 true，否则返回 false
     */
    public static boolean isCiEnv() {
        return getCiName().isPresent() || parseBoolean(System.getenv("CI"), false);
    }

    /**
     * 获取 CI 平台名称。
     *
     * @return CI 平台名称，无法识别时返回 Optional.empty
     */
    public static Optional<String> getCiName() {
        if (!isBlank(System.getenv("GITHUB_ACTIONS"))) {
            return Optional.of("GitHub Actions");
        }
        if (!isBlank(System.getenv("GITLAB_CI"))) {
            return Optional.of("GitLab CI");
        }
        if (!isBlank(System.getenv("JENKINS_HOME")) || !isBlank(System.getenv("JENKINS_URL"))) {
            return Optional.of("Jenkins");
        }
        if (!isBlank(System.getenv("TEAMCITY_VERSION"))) {
            return Optional.of("TeamCity");
        }
        if (!isBlank(System.getenv("CIRCLECI"))) {
            return Optional.of("CircleCI");
        }
        if (!isBlank(System.getenv("TRAVIS"))) {
            return Optional.of("Travis CI");
        }
        if (!isBlank(System.getenv("BUILDKITE"))) {
            return Optional.of("Buildkite");
        }
        return Optional.empty();
    }

    /**
     * 获取系统默认时区。
     *
     * @return 系统默认时区
     */
    public static TimeZone getDefaultTimeZone() {
        return TimeZone.getDefault();
    }

    /**
     * 获取系统默认 ZoneId。
     *
     * @return 系统默认 ZoneId
     */
    public static ZoneId getDefaultZoneId() {
        return ZoneId.systemDefault();
    }

    /**
     * 获取系统默认语言区域。
     *
     * @return 系统默认语言区域
     */
    public static Locale getDefaultLocale() {
        return Locale.getDefault();
    }

    /**
     * 获取当前默认国家或地区。
     *
     * @return 国家或地区代码
     */
    public static String getCountry() {
        return getDefaultLocale().getCountry();
    }

    /**
     * 获取当前默认语言。
     *
     * @return 语言代码
     */
    public static String getLanguage() {
        return getDefaultLocale().getLanguage();
    }

    /**
     * 获取系统默认字符集。
     *
     * @return 默认字符集
     */
    public static Charset getCharset() {
        return DEFAULT_CHARSET;
    }

    /**
     * 获取文件编码。
     *
     * @return 文件编码名称
     */
    public static String getFileEncoding() {
        return getProperty("file.encoding", DEFAULT_CHARSET.name());
    }

    /**
     * 获取当前系统时间。
     *
     * @return 当前系统时间
     */
    public static LocalDateTime getNow() {
        return LocalDateTime.now();
    }

    /**
     * 获取当前时间戳毫秒值。
     *
     * @return 当前时间戳毫秒值
     */
    public static long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 获取高精度时间值。
     *
     * @return 高精度时间值，适合用于计算耗时
     */
    public static long getNanoTime() {
        return System.nanoTime();
    }

    /**
     * 获取当前工作目录。
     *
     * @return 当前工作目录路径
     */
    public static String getWorkingDir() {
        return getUserDir();
    }

    /**
     * 获取应用所在目录。
     *
     * @return 应用所在目录路径
     */
    public static String getAppDir() {
        Path location = getCodeLocation().orElse(Path.of(getWorkingDir()));
        if (Files.isRegularFile(location)) {
            Path parent = location.getParent();
            return parent == null ? location.toAbsolutePath().normalize().toString() : parent.toAbsolutePath().normalize().toString();
        }
        return location.toAbsolutePath().normalize().toString();
    }

    /**
     * 获取当前 jar 包路径。
     *
     * @return jar 包路径，非 jar 运行时返回 Optional.empty
     */
    public static Optional<Path> getJarPath() {
        return getCodeLocation().filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"));
    }

    /**
     * 获取当前 jar 包所在目录。
     *
     * @return jar 包所在目录，非 jar 运行时返回 Optional.empty
     */
    public static Optional<Path> getJarDir() {
        return getJarPath().map(Path::getParent);
    }

    /**
     * 获取 classpath 根路径。
     *
     * @return classpath 根路径
     */
    public static String getClassPathRoot() {
        String classPath = getClassPath();
        if (isBlank(classPath)) {
            return getWorkingDir();
        }
        String first = classPath.split(Pattern.quote(File.pathSeparator), 2)[0];
        return Path.of(first).toAbsolutePath().normalize().toString();
    }

    /**
     * 获取资源文件路径。
     *
     * @param name 资源名称
     * @return 资源文件路径，无法转换为文件路径时返回 Optional.empty
     */
    public static Optional<Path> getResourcePath(String name) {
        String resourceName = requireText(name, "资源名称不能为空");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader == null ? OsUtil.class.getClassLoader().getResource(resourceName) : classLoader.getResource(resourceName);
        if (resource == null) {
            return Optional.empty();
        }
        try {
            URI uri = resource.toURI();
            if (!"file".equalsIgnoreCase(uri.getScheme())) {
                return Optional.empty();
            }
            return Optional.of(Path.of(uri).toAbsolutePath().normalize());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    /**
     * 获取配置目录。
     *
     * @return 配置目录路径
     */
    public static String getConfigDir() {
        return getDirectoryByPropertyOrEnv("app.config.dir", "APP_CONFIG_DIR", "config");
    }

    /**
     * 获取日志目录。
     *
     * @return 日志目录路径
     */
    public static String getLogDir() {
        return getDirectoryByPropertyOrEnv("app.log.dir", "APP_LOG_DIR", "logs");
    }

    /**
     * 获取数据目录。
     *
     * @return 数据目录路径
     */
    public static String getDataDir() {
        return getDirectoryByPropertyOrEnv("app.data.dir", "APP_DATA_DIR", "data");
    }

    /**
     * 获取缓存目录。
     *
     * @return 缓存目录路径
     */
    public static String getCacheDir() {
        return getDirectoryByPropertyOrEnv("app.cache.dir", "APP_CACHE_DIR", "cache");
    }

    /**
     * 判断路径是否可读。
     *
     * @param path 路径
     * @return 可读返回 true，否则返回 false
     */
    public static boolean canRead(String path) {
        return !isBlank(path) && Files.isReadable(Path.of(path));
    }

    /**
     * 判断路径是否可写。
     *
     * @param path 路径
     * @return 可写返回 true，否则返回 false
     */
    public static boolean canWrite(String path) {
        return !isBlank(path) && Files.isWritable(Path.of(path));
    }

    /**
     * 判断文件是否可执行。
     *
     * @param path 文件路径
     * @return 可执行返回 true，否则返回 false
     */
    public static boolean canExecuteFile(String path) {
        return !isBlank(path) && Files.isExecutable(Path.of(path));
    }

    /**
     * 判断系统是否存在指定命令。
     *
     * @param command 命令名称
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasCommand(String command) {
        return findCommand(command).isPresent();
    }

    /**
     * 判断系统是否存在 Java 命令。
     *
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasJava() {
        return hasCommand("java");
    }

    /**
     * 判断系统是否存在 Git 命令。
     *
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasGit() {
        return hasCommand("git");
    }

    /**
     * 判断系统是否存在 Docker 命令。
     *
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasDocker() {
        return hasCommand("docker");
    }

    /**
     * 判断系统是否存在 curl 命令。
     *
     * @return 存在返回 true，否则返回 false
     */
    public static boolean hasCurl() {
        return hasCommand("curl");
    }

    /**
     * 检查必需命令是否存在，缺失时抛出异常。
     *
     * @param commands 命令名称数组
     */
    public static void checkRequiredCommands(String... commands) {
        if (commands == null || commands.length == 0) {
            throw new IllegalArgumentException("必需命令不能为空");
        }
        List<String> missing = new ArrayList<>();
        for (String command : commands) {
            if (!hasCommand(command)) {
                missing.add(String.valueOf(command));
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("缺少必需命令: " + String.join(", ", missing));
        }
    }

    /**
     * 检查必需目录是否存在，缺失时抛出异常。
     *
     * @param dirs 目录路径数组
     */
    public static void checkRequiredDirs(String... dirs) {
        if (dirs == null || dirs.length == 0) {
            throw new IllegalArgumentException("必需目录不能为空");
        }
        List<String> missing = new ArrayList<>();
        for (String dir : dirs) {
            if (isBlank(dir) || !Files.isDirectory(Path.of(dir))) {
                missing.add(String.valueOf(dir));
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("缺少必需目录: " + String.join(", ", missing));
        }
    }

    /**
     * 添加 JVM 关闭钩子。
     *
     * @param runnable 关闭时执行的逻辑
     * @return 已注册的关闭钩子线程
     */
    public static Thread addShutdownHook(Runnable runnable) {
        Objects.requireNonNull(runnable, "关闭钩子逻辑不能为空");
        Thread thread = new Thread(runnable, "os-util-shutdown-hook-" + SHUTDOWN_HOOK_INDEX.getAndIncrement());
        Runtime.getRuntime().addShutdownHook(thread);
        return thread;
    }

    /**
     * 移除 JVM 关闭钩子。
     *
     * @param thread 关闭钩子线程
     * @return 移除成功返回 true，否则返回 false
     */
    public static boolean removeShutdownHook(Thread thread) {
        if (thread == null) {
            return false;
        }
        try {
            return Runtime.getRuntime().removeShutdownHook(thread);
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    /**
     * 退出当前 JVM。
     *
     * @param status 退出码
     */
    public static void exit(int status) {
        System.exit(status);
    }

    /**
     * 以成功状态退出当前 JVM。
     */
    public static void exitSuccess() {
        exit(0);
    }

    /**
     * 以失败状态退出当前 JVM。
     */
    public static void exitFailure() {
        exit(1);
    }

    /**
     * 强制终止当前 JVM。
     *
     * @param status 退出码
     */
    public static void halt(int status) {
        Runtime.getRuntime().halt(status);
    }

    /**
     * 建议 JVM 执行垃圾回收。
     */
    public static void gc() {
        System.gc();
    }

    /**
     * 建议 JVM 执行对象终结。
     */
    public static void runFinalization() {
        System.runFinalization();
    }

    /**
     * 获取系统信息。
     *
     * @return 系统信息 Map
     */
    public static Map<String, Object> getSystemInfo() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("osName", getOsName());
        map.put("osVersion", getOsVersion());
        map.put("osArch", getOsArch());
        map.put("osFamily", getOsFamily());
        map.put("lineSeparator", getLineSeparator());
        map.put("fileSeparator", getFileSeparator());
        map.put("pathSeparator", getPathSeparator());
        map.put("tempDir", getTempDir());
        map.put("userHomeDir", getUserHomeDir());
        map.put("userDir", getUserDir());
        return Collections.unmodifiableMap(map);
    }

    /**
     * 获取 JVM 信息。
     *
     * @return JVM 信息 Map
     */
    public static Map<String, Object> getJvmInfo() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("javaVersion", getJavaVersion());
        map.put("javaVendor", getJavaVendor());
        map.put("javaHome", getJavaHome());
        map.put("jvmName", getJvmName());
        map.put("jvmVersion", getJvmVersion());
        map.put("jvmVendor", getJvmVendor());
        map.put("jvmInputArgs", getJvmInputArgs());
        map.put("classPath", getClassPath());
        map.put("libraryPath", getLibraryPath());
        map.put("java21OrLater", isJava21OrLater());
        map.put("virtualThreadSupported", isVirtualThreadSupported());
        map.put("memory", getJvmMemoryUsage());
        return Collections.unmodifiableMap(map);
    }

    /**
     * 获取进程信息。
     *
     * @return 进程信息 Map
     */
    public static Map<String, Object> getProcessInfo() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("pid", getPid());
        map.put("processName", getProcessName());
        map.put("processCommand", getProcessCommand());
        map.put("processCommandLine", getProcessCommandLine());
        map.put("processArgs", getProcessArgs());
        map.put("processStartTime", getProcessStartTime().map(Instant::toString).orElse(""));
        map.put("processUptimeMillis", getProcessUptime().toMillis());
        map.put("parentPid", getParentPid().isPresent() ? getParentPid().getAsLong() : null);
        map.put("parentProcessName", getParentProcessName().orElse(""));
        return Collections.unmodifiableMap(map);
    }

    /**
     * 获取运行时信息。
     *
     * @return 运行时信息 Map
     */
    public static Map<String, Object> getRuntimeInfo() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("availableProcessors", getAvailableProcessors());
        map.put("systemLoadAverage", getSystemLoadAverage());
        map.put("currentTimeMillis", getCurrentTimeMillis());
        map.put("nanoTime", getNanoTime());
        map.put("timeZone", getDefaultZoneId().toString());
        map.put("locale", getDefaultLocale().toString());
        map.put("charset", getCharset().name());
        return Collections.unmodifiableMap(map);
    }

    /**
     * 获取主机信息。
     *
     * @return 主机信息 Map
     */
    public static Map<String, Object> getHostInfo() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userName", getUserName());
        map.put("userHome", getUserHome());
        map.put("hostName", getHostName());
        map.put("hostAddress", getHostAddress());
        map.put("localAddressList", getLocalAddressList());
        map.put("macAddress", getMacAddress().orElse(""));
        map.put("machineId", getMachineId());
        map.put("instanceId", getInstanceId());
        map.put("rootUser", isRootUser());
        map.put("administrator", isAdministrator());
        return Collections.unmodifiableMap(map);
    }

    /**
     * 获取磁盘信息。
     *
     * @param path 路径
     * @return 磁盘信息 Map
     */
    public static Map<String, Object> getDiskInfo(String path) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("path", normalizePath(path));
        map.put("total", getDiskTotalSpace(path));
        map.put("free", getDiskFreeSpace(path));
        map.put("usable", getDiskUsableSpace(path));
        map.put("used", getDiskUsedSpace(path));
        map.put("usageRate", getDiskUsageRate(path));
        return Collections.unmodifiableMap(map);
    }

    /**
     * 获取网络信息。
     *
     * @return 网络信息 Map
     */
    public static Map<String, Object> getNetworkInfo() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("localIp", getLocalIp());
        map.put("localIps", getLocalIps());
        map.put("hostName", getHostName());
        map.put("hostAddress", getHostAddress());
        return Collections.unmodifiableMap(map);
    }

    /**
     * 获取环境信息。
     *
     * @return 环境信息 Map
     */
    public static Map<String, Object> getEnvironmentInfo() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("activeProfile", getActiveProfile());
        map.put("prodEnv", isProdEnv());
        map.put("devEnv", isDevEnv());
        map.put("testEnv", isTestEnv());
        map.put("docker", isDocker());
        map.put("kubernetes", isKubernetes());
        map.put("container", isContainer());
        map.put("containerId", getContainerId().orElse(""));
        map.put("podName", getPodName().orElse(""));
        map.put("namespace", getNamespace().orElse(""));
        map.put("nodeName", getNodeName().orElse(""));
        map.put("ciEnv", isCiEnv());
        map.put("ciName", getCiName().orElse(""));
        return Collections.unmodifiableMap(map);
    }

    /**
     * 获取系统摘要文本。
     *
     * @return 系统摘要文本
     */
    public static String getSystemSummary() {
        return "OS=" + getOsName() + " " + getOsVersion()
                + ", Arch=" + getOsArch()
                + ", Java=" + getJavaVersion()
                + ", JVM=" + getJvmName()
                + ", PID=" + getPid()
                + ", Host=" + getHostName()
                + ", IP=" + getLocalIp();
    }

    /**
     * 打印系统信息到标准输出。
     */
    public static void printSystemInfo() {
        System.out.println(getSystemSummary());
    }

    /**
     * 转换为完整诊断 Map。
     *
     * @return 诊断 Map
     */
    public static Map<String, Object> toDiagnosticMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("system", getSystemInfo());
        map.put("jvm", getJvmInfo());
        map.put("process", getProcessInfo());
        map.put("runtime", getRuntimeInfo());
        map.put("host", getHostInfo());
        map.put("network", getNetworkInfo());
        map.put("environment", getEnvironmentInfo());
        return Collections.unmodifiableMap(map);
    }

    /**
     * 转换为完整诊断 JSON。
     *
     * @return 诊断 JSON 字符串
     */
    public static String toDiagnosticJson() {
        return toJson(toDiagnosticMap());
    }

    /**
     * 命令执行结果。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static final class CommandResult {
        private final int exitCode;
        private final boolean success;
        private final String stdout;
        private final String stderr;
        private final Duration duration;
        private final boolean timeout;

        private CommandResult(int exitCode, boolean success, String stdout, String stderr, Duration duration, boolean timeout) {
            this.exitCode = exitCode;
            this.success = success;
            this.stdout = stdout == null ? "" : stdout;
            this.stderr = stderr == null ? "" : stderr;
            this.duration = duration == null ? Duration.ZERO : duration;
            this.timeout = timeout;
        }

        /**
         * 获取命令退出码。
         *
         * @return 命令退出码
         */
        public int getExitCode() {
            return exitCode;
        }

        /**
         * 判断命令是否执行成功。
         *
         * @return 执行成功返回 true，否则返回 false
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * 获取标准输出。
         *
         * @return 标准输出内容
         */
        public String getStdout() {
            return stdout;
        }

        /**
         * 获取错误输出。
         *
         * @return 错误输出内容
         */
        public String getStderr() {
            return stderr;
        }

        /**
         * 获取命令执行耗时。
         *
         * @return 命令执行耗时
         */
        public Duration getDuration() {
            return duration;
        }

        /**
         * 判断命令是否超时。
         *
         * @return 超时返回 true，否则返回 false
         */
        public boolean isTimeout() {
            return timeout;
        }
    }

    private static int parseJavaMajorVersion(String version) {
        if (isBlank(version)) {
            return 0;
        }
        String value = version.trim();
        if (value.startsWith("1.")) {
            value = value.substring(2);
        }
        int index = 0;
        while (index < value.length() && Character.isDigit(value.charAt(index))) {
            index++;
        }
        if (index == 0) {
            return 0;
        }
        return parseInt(value.substring(0, index), 0);
    }

    private static int parseInt(String value, int defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static long parseLong(String value, long defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "y", "on" -> true;
            case "false", "0", "no", "n", "off" -> false;
            default -> defaultValue;
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String requireText(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static void validatePid(long pid) {
        if (pid <= 0) {
            throw new IllegalArgumentException("pid 必须大于 0");
        }
    }

    private static void validatePort(int port, boolean allowZero) {
        int min = allowZero ? 0 : 1;
        if (port < min || port > 65535) {
            throw new IllegalArgumentException("端口号范围必须为 " + min + "-65535");
        }
    }

    private static File existingFile(String path) {
        Path current = Path.of(requireText(path, "路径不能为空")).toAbsolutePath().normalize();
        while (current != null && !Files.exists(current)) {
            current = current.getParent();
        }
        if (current == null) {
            current = Path.of(getWorkingDir()).toAbsolutePath().normalize();
        }
        return current.toFile();
    }

    private static String formatMacAddress(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                builder.append(':');
            }
            builder.append(String.format(Locale.ROOT, "%02X", bytes[i]));
        }
        return builder.toString();
    }

    private static void validateCommand(List<String> command) {
        if (command == null || command.isEmpty()) {
            throw new IllegalArgumentException("命令参数列表不能为空");
        }
        for (String item : command) {
            if (isBlank(item)) {
                throw new IllegalArgumentException("命令参数不能包含空值");
            }
        }
    }

    private static String readStream(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, DEFAULT_CHARSET))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append(System.lineSeparator());
            }
            return builder.toString();
        } catch (IOException ex) {
            return "";
        }
    }

    private static String getFutureValue(CompletableFuture<String> future) {
        try {
            return future.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "";
        } catch (ExecutionException | TimeoutException ex) {
            return "";
        }
    }

    private static void waitQuietly(Process process) {
        try {
            process.waitFor(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static List<String> executableExtensions() {
        if (!isWindows()) {
            return List.of("");
        }
        String pathExt = System.getenv("PATHEXT");
        if (isBlank(pathExt)) {
            return List.of("", ".exe", ".bat", ".cmd", ".ps1");
        }
        List<String> extensions = new ArrayList<>();
        extensions.add("");
        for (String item : pathExt.split(Pattern.quote(File.pathSeparator))) {
            if (!isBlank(item)) {
                extensions.add(item.toLowerCase(Locale.ROOT));
            }
        }
        return List.copyOf(extensions);
    }

    private static String readSmallFile(Path path) {
        try {
            if (Files.isRegularFile(path) && Files.isReadable(path)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
            // ignore
        }
        return "";
    }

    private static Optional<Path> getCodeLocation() {
        try {
            CodeSource codeSource = OsUtil.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return Optional.empty();
            }
            return Optional.of(Path.of(codeSource.getLocation().toURI()).toAbsolutePath().normalize());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static String getDirectoryByPropertyOrEnv(String propertyName, String envName, String defaultDirName) {
        String property = getProperty(propertyName, null);
        if (!isBlank(property)) {
            return normalizePath(property);
        }
        String env = System.getenv(envName);
        if (!isBlank(env)) {
            return normalizePath(env);
        }
        return Path.of(getAppDir(), defaultDirName).toAbsolutePath().normalize().toString();
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return quoteJson(string);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof MemoryUsage memoryUsage) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("init", memoryUsage.getInit());
            map.put("used", memoryUsage.getUsed());
            map.put("committed", memoryUsage.getCommitted());
            map.put("max", memoryUsage.getMax());
            return toJson(map);
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                builder.append(quoteJson(String.valueOf(entry.getKey()))).append(':').append(toJson(entry.getValue()));
                first = false;
            }
            return builder.append('}').toString();
        }
        if (value instanceof Collection<?> collection) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : collection) {
                if (!first) {
                    builder.append(',');
                }
                builder.append(toJson(item));
                first = false;
            }
            return builder.append(']').toString();
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(toJson(java.lang.reflect.Array.get(value, i)));
            }
            return builder.append(']').toString();
        }
        return quoteJson(String.valueOf(value));
    }

    private static String quoteJson(String value) {
        StringBuilder builder = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 32) {
                        builder.append(String.format(Locale.ROOT, "\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        return builder.append('"').toString();
    }
}
