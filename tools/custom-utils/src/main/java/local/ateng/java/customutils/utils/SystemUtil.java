package local.ateng.java.customutils.utils;


import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

/**
 * 系统工具类
 * 提供常用的系统相关操作方法
 * <p>包括操作系统、JVM、系统属性等信息的获取</p>
 *
 * @author Ateng
 * @since 2025-07-30
 */
public final class SystemUtil {

    /**
     * 禁止实例化工具类
     */
    private SystemUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 获取操作系统名称
     *
     * @return 操作系统名称
     */
    public static String getOsName() {
        return System.getProperty("os.name");
    }

    /**
     * 判断当前操作系统是否为 Windows
     *
     * @return 是 Windows 系统返回 true，否则返回 false
     */
    public static boolean isWindows() {
        return getOsName().toLowerCase(Locale.ENGLISH).contains("win");
    }

    /**
     * 判断当前操作系统是否为 Linux
     *
     * @return 是 Linux 系统返回 true，否则返回 false
     */
    public static boolean isLinux() {
        return getOsName().toLowerCase(Locale.ENGLISH).contains("linux");
    }

    /**
     * 判断当前操作系统是否为 MacOS
     *
     * @return 是 MacOS 系统返回 true，否则返回 false
     */
    public static boolean isMac() {
        return getOsName().toLowerCase(Locale.ENGLISH).contains("mac");
    }

    /**
     * 获取当前用户名
     *
     * @return 当前用户的用户名
     */
    public static String getUserName() {
        return System.getProperty("user.name");
    }

    /**
     * 获取用户主目录
     *
     * @return 用户主目录路径
     */
    public static String getUserHome() {
        return System.getProperty("user.home");
    }

    /**
     * 获取当前工作目录
     *
     * @return 当前工作目录路径
     */
    public static String getUserDir() {
        return System.getProperty("user.dir");
    }

    /**
     * 获取 Java 运行时名称
     *
     * @return Java 运行时名称
     */
    public static String getJavaRuntimeName() {
        return System.getProperty("java.runtime.name");
    }

    /**
     * 获取 Java 版本
     *
     * @return Java 版本号
     */
    public static String getJavaVersion() {
        return System.getProperty("java.version");
    }

    /**
     * 获取 Java 安装路径
     *
     * @return Java 安装路径
     */
    public static String getJavaHome() {
        return System.getProperty("java.home");
    }

    /**
     * 获取 JVM 启动时间（毫秒）
     *
     * @return JVM 启动时间
     */
    public static long getJvmStartTime() {
        return ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    /**
     * 获取所有系统属性
     *
     * @return 所有系统属性对象
     */
    public static Properties getSystemProperties() {
        return System.getProperties();
    }

    /**
     * 获取系统换行符
     *
     * @return 换行符
     */
    public static String getLineSeparator() {
        return System.lineSeparator();
    }

    /**
     * 获取文件分隔符（如 Windows 为 \，Linux 为 /）
     *
     * @return 文件分隔符
     */
    public static String getFileSeparator() {
        return File.separator;
    }

    /**
     * 获取路径分隔符（如 Windows 为 ;，Linux 为 :）
     *
     * @return 路径分隔符
     */
    public static String getPathSeparator() {
        return File.pathSeparator;
    }

    /**
     * 获取可用的处理器核心数（逻辑核心数）
     *
     * @return CPU 核心数
     */
    public static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * 获取 JVM 最大可用内存（以字节为单位）
     *
     * @return 最大内存
     */
    public static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    /**
     * 获取 JVM 已分配的总内存（以字节为单位）
     *
     * @return 总内存
     */
    public static long getTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    /**
     * 获取 JVM 空闲内存（以字节为单位）
     *
     * @return 空闲内存
     */
    public static long getFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    /**
     * 获取当前进程 ID
     *
     * @return 当前进程 ID
     */
    public static long getCurrentProcessId() {
        // 格式通常为 pid@host，例如 12345@localhost
        String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        try {
            return Long.parseLong(jvmName.split("@")[0]);
        } catch (Exception e) {
            return -1L;
        }
    }

    /**
     * 获取指定环境变量的值
     *
     * @param name 环境变量名称
     * @return 环境变量值，不存在返回 null
     */
    public static String getEnv(String name) {
        return System.getenv(name);
    }

    /**
     * 获取所有环境变量
     *
     * @return 环境变量键值对集合
     */
    public static java.util.Map<String, String> getAllEnv() {
        return System.getenv();
    }

    /**
     * 获取 JVM 启动参数（-Xmx、-Dxx 等）
     *
     * @return JVM 启动参数列表
     */
    public static List<String> getJvmArguments() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        return runtimeMxBean.getInputArguments();
    }

    /**
     * 获取 JVM 运行时长（毫秒）
     *
     * @return JVM 运行时间
     */
    public static long getJvmUptime() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    /**
     * 判断是否为 64 位操作系统
     *
     * @return 是 64 位系统返回 true，否则 false
     */
    public static boolean is64BitOs() {
        String arch = System.getProperty("os.arch");
        return arch != null && arch.contains("64");
    }

    /**
     * 判断是否为 64 位 JVM
     *
     * @return 是 64 位 JVM 返回 true，否则 false
     */
    public static boolean is64BitJvm() {
        String dataModel = System.getProperty("sun.arch.data.model");
        return "64".equals(dataModel);
    }

    /**
     * 判断当前环境是否为 Docker 容器（基于 cgroup 文件）
     *
     * @return 是 Docker 环境返回 true，否则 false
     */
    public static boolean isRunningInDocker() {
        File cgroup = new File("/proc/1/cgroup");
        if (cgroup.exists()) {
            try (java.util.Scanner scanner = new java.util.Scanner(cgroup)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.contains("docker") || line.contains("containerd")) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    /**
     * 判断是否运行在 WSL（Windows Subsystem for Linux）环境
     *
     * @return 是 WSL 返回 true，否则 false
     */
    public static boolean isRunningInWSL() {
        String osName = getOsName().toLowerCase(Locale.ENGLISH);
        if (osName.contains("linux")) {
            try {
                File versionFile = new File("/proc/version");
                if (versionFile.exists()) {
                    String content = new String(java.nio.file.Files.readAllBytes(versionFile.toPath()));
                    return content.contains("Microsoft") || content.contains("WSL");
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    /**
     * 获取当前系统时间（包含时区信息）
     *
     * @return 当前系统时间
     */
    public static ZonedDateTime getSystemDateTime() {
        return ZonedDateTime.now();
    }

    /**
     * 获取当前系统默认时区 ID
     *
     * @return 时区 ID，例如 Asia/Shanghai
     */
    public static String getSystemTimeZoneId() {
        return TimeZone.getDefault().getID();
    }

    /**
     * 获取当前系统默认语言
     *
     * @return 系统语言，例如 zh、en
     */
    public static String getSystemLanguage() {
        return Locale.getDefault().getLanguage();
    }

    /**
     * 获取当前系统默认区域（语言 + 国家）
     *
     * @return 系统区域，例如 zh_CN、en_US
     */
    public static String getSystemLocale() {
        Locale locale = Locale.getDefault();
        return locale.getLanguage() + "_" + locale.getCountry();
    }

    /**
     * 获取操作系统版本信息
     *
     * @return 操作系统版本，例如 10.0、5.15.0-105
     */
    public static String getOsVersion() {
        return System.getProperty("os.version");
    }

    /**
     * 获取操作系统架构信息
     *
     * @return 架构名称，例如 amd64、x86
     */
    public static String getOsArch() {
        return System.getProperty("os.arch");
    }

    /**
     * 获取当前线程 ID
     *
     * @return 当前线程 ID
     */
    public static long getCurrentThreadId() {
        return Thread.currentThread().getId();
    }

    /**
     * 获取当前线程名称
     *
     * @return 当前线程名称
     */
    public static String getCurrentThreadName() {
        return Thread.currentThread().getName();
    }

    /**
     * 获取当前线程状态
     *
     * @return 当前线程状态
     */
    public static Thread.State getCurrentThreadState() {
        return Thread.currentThread().getState();
    }

}