package io.github.atengk.utils.oshi;

import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.*;

import java.io.IOException;
import java.lang.management.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;

/**
 * OSHI 系统监控静态工具类，封装操作系统、CPU、内存、磁盘、网络、进程、硬件、JVM 和运行环境信息。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class OshiUtil {

    private static final Object LOCK = new Object();
    private static final double DEFAULT_WARN_THRESHOLD = 80.0D;
    private static final double DEFAULT_CRITICAL_THRESHOLD = 90.0D;
    private static volatile SystemInfo systemInfo = new SystemInfo();
    private static volatile HardwareAbstractionLayer hardware = systemInfo.getHardware();
    private static volatile OperatingSystem operatingSystem = systemInfo.getOperatingSystem();

    private OshiUtil() {
        throw new UnsupportedOperationException("OshiUtil 是静态工具类，禁止实例化");
    }

    /**
     * 获取 OSHI 核心入口对象。
     *
     * @return SystemInfo 对象
     */
    public static SystemInfo getSystemInfo() {
        return systemInfo;
    }

    /**
     * 获取硬件抽象对象。
     *
     * @return 硬件抽象对象
     */
    public static HardwareAbstractionLayer getHardware() {
        return hardware;
    }

    /**
     * 获取操作系统对象。
     *
     * @return 操作系统对象
     */
    public static OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    /**
     * 重新初始化 OSHI 上下文对象。
     */
    public static void refresh() {
        synchronized (LOCK) {
            systemInfo = new SystemInfo();
            hardware = systemInfo.getHardware();
            operatingSystem = systemInfo.getOperatingSystem();
        }
    }

    /**
     * 清理当前工具类缓存并重新加载 OSHI 上下文。
     */
    public static void clearCache() {
        refresh();
    }

    /**
     * 获取 OSHI 实现包版本。
     *
     * @return OSHI 版本，无法获取时返回 unknown
     */
    public static String getOshiVersion() {
        Package pkg = SystemInfo.class.getPackage();
        String version = pkg == null ? null : pkg.getImplementationVersion();
        return isBlank(version) ? "unknown" : version;
    }

    /**
     * 获取系统整体概要信息。
     *
     * @return 系统概要信息
     */
    public static SystemOverview getSystemOverview() {
        return new SystemOverview(getOsInfo(), getHostSummary(), getResourceSummary(), getUptime(), getBootTime(), isSystemHealthy());
    }

    /**
     * 获取系统完整快照信息。
     *
     * @return 系统快照信息
     */
    public static SystemSnapshot snapshotSystem() {
        return new SystemSnapshot(getSystemOverview(), snapshotCpu(), snapshotMemory(), snapshotDisk(), snapshotNetwork(), snapshotProcesses(), snapshotJvm());
    }

    /**
     * 获取主机摘要信息。
     *
     * @return 主机摘要信息
     */
    public static HostSummary getHostSummary() {
        ComputerSystem cs = hardware.getComputerSystem();
        return new HostSummary(getHostname(), getDomainName(), getOsName(), getOsVersion(), getSystemArchitecture(), safeString(cs.getManufacturer()),
                safeString(cs.getModel()), safeString(cs.getSerialNumber()));
    }

    /**
     * 获取资源使用摘要信息。
     *
     * @return 资源摘要信息
     */
    public static ResourceSummary getResourceSummary() {
        return new ResourceSummary(getCpuLoadPercent(), getMemoryUsagePercent(), getDiskUsage(), listNetworkInterfaces().size(), getProcessCount(), getThreadCount());
    }

    /**
     * 获取系统健康状态。
     *
     * @return 健康状态
     */
    public static HealthStatus getHealthStatus() {
        List<WarningItem> warnings = getWarningItems();
        List<WarningItem> criticals = getCriticalItems();
        return new HealthStatus(criticals.isEmpty(), List.copyOf(warnings), List.copyOf(criticals));
    }

    /**
     * 判断系统是否处于健康状态。
     *
     * @return 健康返回 true，否则返回 false
     */
    public static boolean isSystemHealthy() {
        return getCriticalItems().isEmpty();
    }

    /**
     * 获取系统运行时长，单位秒。
     *
     * @return 系统运行时长
     */
    public static long getUptime() {
        return Math.max(0L, operatingSystem.getSystemUptime());
    }

    /**
     * 获取系统启动时间。
     *
     * @return 系统启动时间
     */
    public static LocalDateTime getBootTime() {
        long bootTime = Math.max(0L, operatingSystem.getSystemBootTime());
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(bootTime), ZoneId.systemDefault());
    }

    /**
     * 获取操作系统完整信息。
     *
     * @return 操作系统信息
     */
    public static OsInfo getOsInfo() {
        OperatingSystem.OSVersionInfo versionInfo = operatingSystem.getVersionInfo();
        return new OsInfo(getOsManufacturer(), getOsFamily(), getOsVersion(), safeString(versionInfo.getCodeName()), safeString(versionInfo.getBuildNumber()),
                getOsBitness(), getSystemArchitecture(), getUptime(), getBootTime());
    }

    /**
     * 获取操作系统名称。
     *
     * @return 操作系统名称
     */
    public static String getOsName() {
        String family = getOsFamily();
        String version = getOsVersion();
        if (isBlank(version)) {
            return family;
        }
        return family + " " + version;
    }

    /**
     * 获取操作系统族系。
     *
     * @return 操作系统族系
     */
    public static String getOsFamily() {
        return safeString(operatingSystem.getFamily());
    }

    /**
     * 获取操作系统版本。
     *
     * @return 操作系统版本
     */
    public static String getOsVersion() {
        return safeString(operatingSystem.getVersionInfo().getVersion());
    }

    /**
     * 获取操作系统厂商。
     *
     * @return 操作系统厂商
     */
    public static String getOsManufacturer() {
        return safeString(operatingSystem.getManufacturer());
    }

    /**
     * 获取操作系统位数。
     *
     * @return 操作系统位数
     */
    public static int getOsBitness() {
        return Math.max(0, operatingSystem.getBitness());
    }

    /**
     * 获取系统架构。
     *
     * @return 系统架构
     */
    public static String getSystemArchitecture() {
        return safeString(System.getProperty("os.arch"));
    }

    /**
     * 获取系统登录会话列表。
     *
     * @return 登录会话列表
     */
    public static List<SessionInfo> listSessions() {
        List<SessionInfo> result = new ArrayList<>();
        for (OSSession session : nullToEmpty(operatingSystem.getSessions())) {
            result.add(new SessionInfo(safeString(session.getUserName()), safeString(session.getTerminalDevice()), safeString(session.getHost()),
                    session.getLoginTime()));
        }
        return List.copyOf(result);
    }

    /**
     * 获取当前系统用户。
     *
     * @return 当前系统用户
     */
    public static String getCurrentUser() {
        return safeString(System.getProperty("user.name"));
    }

    /**
     * 判断当前系统是否为 Windows。
     *
     * @return 是 Windows 返回 true，否则返回 false
     */
    public static boolean isWindows() {
        return getOsFamily().toLowerCase(Locale.ROOT).contains("windows");
    }

    /**
     * 判断当前系统是否为 Linux。
     *
     * @return 是 Linux 返回 true，否则返回 false
     */
    public static boolean isLinux() {
        return getOsFamily().toLowerCase(Locale.ROOT).contains("linux");
    }

    /**
     * 判断当前系统是否为 macOS。
     *
     * @return 是 macOS 返回 true，否则返回 false
     */
    public static boolean isMac() {
        String os = getOsFamily().toLowerCase(Locale.ROOT);
        return os.contains("mac") || os.contains("darwin");
    }

    /**
     * 获取 CPU 基础信息。
     *
     * @return CPU 信息
     */
    public static CpuInfo getCpuInfo() {
        CentralProcessor processor = hardware.getProcessor();
        CentralProcessor.ProcessorIdentifier identifier = processor.getProcessorIdentifier();
        return new CpuInfo(safeString(identifier.getName()), safeString(identifier.getVendor()), safeString(identifier.getIdentifier()),
                safeString(identifier.getProcessorID()), getPhysicalProcessorCount(), getLogicalProcessorCount(), getCpuMaxFrequency(),
                safeLongArray(getCpuCurrentFrequencies()));
    }

    /**
     * 获取 CPU 名称。
     *
     * @return CPU 名称
     */
    public static String getCpuName() {
        return safeString(hardware.getProcessor().getProcessorIdentifier().getName());
    }

    /**
     * 获取 CPU 厂商。
     *
     * @return CPU 厂商
     */
    public static String getCpuVendor() {
        return safeString(hardware.getProcessor().getProcessorIdentifier().getVendor());
    }

    /**
     * 获取物理 CPU 核心数。
     *
     * @return 物理核心数
     */
    public static int getPhysicalProcessorCount() {
        return Math.max(0, hardware.getProcessor().getPhysicalProcessorCount());
    }

    /**
     * 获取逻辑 CPU 数。
     *
     * @return 逻辑 CPU 数
     */
    public static int getLogicalProcessorCount() {
        return Math.max(0, hardware.getProcessor().getLogicalProcessorCount());
    }

    /**
     * 获取 CPU 最大频率，单位 Hz。
     *
     * @return CPU 最大频率
     */
    public static long getCpuMaxFrequency() {
        return Math.max(0L, hardware.getProcessor().getMaxFreq());
    }

    /**
     * 获取 CPU 当前频率数组，单位 Hz。
     *
     * @return CPU 当前频率数组
     */
    public static long[] getCpuCurrentFrequencies() {
        return safeLongArray(hardware.getProcessor().getCurrentFreq());
    }

    /**
     * 获取 CPU 总使用率百分比，取值范围 0 到 100。
     *
     * @return CPU 使用率百分比
     */
    public static double getCpuLoad() {
        long[] oldTicks = getCpuTicks();
        sleepQuietly(200L);
        return calcCpuLoad(oldTicks);
    }

    /**
     * 获取 CPU 总使用率百分比，取值范围 0 到 100。
     *
     * @return CPU 使用率百分比
     */
    public static double getCpuLoadPercent() {
        return getCpuLoad();
    }

    /**
     * 获取每个逻辑 CPU 的使用率百分比，取值范围 0 到 100。
     *
     * @return 每个逻辑 CPU 使用率数组
     */
    public static double[] getPerCpuLoad() {
        CentralProcessor processor = hardware.getProcessor();
        long[][] oldTicks = processor.getProcessorCpuLoadTicks();
        sleepQuietly(200L);
        double[] loads = processor.getProcessorCpuLoadBetweenTicks(oldTicks);
        return toPercentArray(loads);
    }

    /**
     * 获取 CPU tick 数据。
     *
     * @return CPU tick 数组
     */
    public static long[] getCpuTicks() {
        return safeLongArray(hardware.getProcessor().getSystemCpuLoadTicks());
    }

    /**
     * 基于上一次 CPU tick 计算当前 CPU 使用率百分比。
     *
     * @param oldTicks 上一次 CPU tick 数组
     * @return CPU 使用率百分比
     */
    public static double calcCpuLoad(long[] oldTicks) {
        if (oldTicks == null || oldTicks.length != CentralProcessor.TickType.values().length) {
            throw new IllegalArgumentException("oldTicks 不能为空且长度必须等于 CPU TickType 数量");
        }
        double load = hardware.getProcessor().getSystemCpuLoadBetweenTicks(oldTicks) * 100.0D;
        return normalizePercent(load);
    }

    /**
     * 获取 CPU 当前快照。
     *
     * @return CPU 快照
     */
    public static CpuSnapshot snapshotCpu() {
        return new CpuSnapshot(getCpuInfo(), getCpuLoadPercent(), safeDoubleArray(getPerCpuLoad()), getCpuTicks(), System.currentTimeMillis());
    }

    /**
     * 获取内存完整信息。
     *
     * @return 内存信息
     */
    public static MemoryInfo getMemoryInfo() {
        GlobalMemory memory = hardware.getMemory();
        VirtualMemory virtualMemory = memory.getVirtualMemory();
        return new MemoryInfo(getTotalMemory(), getAvailableMemory(), getUsedMemory(), getMemoryUsage(), getSwapTotal(), getSwapUsed(), getSwapUsage(),
                Math.max(0L, virtualMemory.getVirtualMax()), Math.max(0L, virtualMemory.getVirtualInUse()));
    }

    /**
     * 获取物理内存总量，单位字节。
     *
     * @return 物理内存总量
     */
    public static long getTotalMemory() {
        return Math.max(0L, hardware.getMemory().getTotal());
    }

    /**
     * 获取可用物理内存，单位字节。
     *
     * @return 可用物理内存
     */
    public static long getAvailableMemory() {
        return Math.max(0L, hardware.getMemory().getAvailable());
    }

    /**
     * 获取已用物理内存，单位字节。
     *
     * @return 已用物理内存
     */
    public static long getUsedMemory() {
        return Math.max(0L, getTotalMemory() - getAvailableMemory());
    }

    /**
     * 获取物理内存使用率百分比，取值范围 0 到 100。
     *
     * @return 物理内存使用率百分比
     */
    public static double getMemoryUsage() {
        return safePercent(getUsedMemory(), getTotalMemory());
    }

    /**
     * 获取物理内存使用率百分比，取值范围 0 到 100。
     *
     * @return 物理内存使用率百分比
     */
    public static double getMemoryUsagePercent() {
        return getMemoryUsage();
    }

    /**
     * 获取交换区总量，单位字节。
     *
     * @return 交换区总量
     */
    public static long getSwapTotal() {
        return Math.max(0L, hardware.getMemory().getVirtualMemory().getSwapTotal());
    }

    /**
     * 获取交换区已用量，单位字节。
     *
     * @return 交换区已用量
     */
    public static long getSwapUsed() {
        return Math.max(0L, hardware.getMemory().getVirtualMemory().getSwapUsed());
    }

    /**
     * 获取交换区使用率百分比，取值范围 0 到 100。
     *
     * @return 交换区使用率百分比
     */
    public static double getSwapUsage() {
        return safePercent(getSwapUsed(), getSwapTotal());
    }

    /**
     * 获取内存快照。
     *
     * @return 内存快照
     */
    public static MemorySnapshot snapshotMemory() {
        return new MemorySnapshot(getMemoryInfo(), System.currentTimeMillis());
    }

    /**
     * 获取物理磁盘列表。
     *
     * @return 物理磁盘列表
     */
    public static List<DiskInfo> listDisks() {
        List<DiskInfo> result = new ArrayList<>();
        for (HWDiskStore disk : nullToEmpty(hardware.getDiskStores())) {
            result.add(toDiskInfo(disk));
        }
        return List.copyOf(result);
    }

    /**
     * 获取指定物理磁盘信息。
     *
     * @param diskName 磁盘名称
     * @return 磁盘信息，不存在时返回 null
     */
    public static DiskInfo getDiskInfo(String diskName) {
        if (isBlank(diskName)) {
            throw new IllegalArgumentException("diskName 不能为空");
        }
        for (DiskInfo disk : listDisks()) {
            if (disk.name().equalsIgnoreCase(diskName.trim())) {
                return disk;
            }
        }
        return null;
    }

    /**
     * 获取磁盘分区列表。
     *
     * @return 分区列表
     */
    public static List<PartitionInfo> listPartitions() {
        List<PartitionInfo> result = new ArrayList<>();
        for (HWDiskStore disk : nullToEmpty(hardware.getDiskStores())) {
            for (HWPartition partition : nullToEmpty(disk.getPartitions())) {
                result.add(toPartitionInfo(partition));
            }
        }
        return List.copyOf(result);
    }

    /**
     * 获取文件系统列表。
     *
     * @return 文件系统列表
     */
    public static List<FileStoreInfo> listFileStores() {
        FileSystem fileSystem = operatingSystem.getFileSystem();
        List<FileStoreInfo> result = new ArrayList<>();
        for (OSFileStore store : nullToEmpty(fileSystem.getFileStores())) {
            result.add(toFileStoreInfo(store));
        }
        return List.copyOf(result);
    }

    /**
     * 获取指定挂载点的文件系统信息。
     *
     * @param mount 挂载点
     * @return 文件系统信息，不存在时返回 null
     */
    public static FileStoreInfo getFileStore(String mount) {
        if (isBlank(mount)) {
            throw new IllegalArgumentException("mount 不能为空");
        }
        String target = mount.trim();
        for (FileStoreInfo store : listFileStores()) {
            if (store.mount().equals(target)) {
                return store;
            }
        }
        return null;
    }

    /**
     * 获取文件系统总空间，单位字节。
     *
     * @return 总空间
     */
    public static long getTotalDiskSpace() {
        long total = 0L;
        for (FileStoreInfo store : listFileStores()) {
            total = safeAdd(total, store.totalSpace());
        }
        return total;
    }

    /**
     * 获取文件系统可用空间，单位字节。
     *
     * @return 可用空间
     */
    public static long getUsableDiskSpace() {
        long total = 0L;
        for (FileStoreInfo store : listFileStores()) {
            total = safeAdd(total, store.usableSpace());
        }
        return total;
    }

    /**
     * 获取文件系统已用空间，单位字节。
     *
     * @return 已用空间
     */
    public static long getUsedDiskSpace() {
        return Math.max(0L, getTotalDiskSpace() - getUsableDiskSpace());
    }

    /**
     * 获取整体磁盘使用率百分比，取值范围 0 到 100。
     *
     * @return 整体磁盘使用率百分比
     */
    public static double getDiskUsage() {
        return safePercent(getUsedDiskSpace(), getTotalDiskSpace());
    }

    /**
     * 获取指定挂载点磁盘使用率百分比，取值范围 0 到 100。
     *
     * @param mount 挂载点
     * @return 磁盘使用率百分比
     */
    public static double getDiskUsage(String mount) {
        FileStoreInfo store = getFileStore(mount);
        if (store == null) {
            throw new IllegalArgumentException("未找到指定挂载点: " + mount);
        }
        return safePercent(store.usedSpace(), store.totalSpace());
    }

    /**
     * 获取磁盘 I/O 统计列表。
     *
     * @return 磁盘 I/O 统计列表
     */
    public static List<DiskIoStats> listDiskIoStats() {
        List<DiskIoStats> result = new ArrayList<>();
        for (HWDiskStore disk : nullToEmpty(hardware.getDiskStores())) {
            result.add(new DiskIoStats(safeString(disk.getName()), Math.max(0L, disk.getReads()), Math.max(0L, disk.getReadBytes()),
                    Math.max(0L, disk.getWrites()), Math.max(0L, disk.getWriteBytes()), Math.max(0L, disk.getTransferTime())));
        }
        return List.copyOf(result);
    }

    /**
     * 获取磁盘快照。
     *
     * @return 磁盘快照
     */
    public static DiskSnapshot snapshotDisk() {
        return new DiskSnapshot(listDisks(), listPartitions(), listFileStores(), listDiskIoStats(), getDiskUsage(), System.currentTimeMillis());
    }

    /**
     * 获取网卡列表。
     *
     * @return 网卡列表
     */
    public static List<NetworkInterfaceInfo> listNetworkInterfaces() {
        List<NetworkInterfaceInfo> result = new ArrayList<>();
        for (NetworkIF networkIF : nullToEmpty(hardware.getNetworkIFs())) {
            networkIF.updateAttributes();
            result.add(toNetworkInterfaceInfo(networkIF));
        }
        return List.copyOf(result);
    }

    /**
     * 获取指定网卡信息。
     *
     * @param name 网卡名称
     * @return 网卡信息，不存在时返回 null
     */
    public static NetworkInterfaceInfo getNetworkInterface(String name) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("name 不能为空");
        }
        String target = name.trim();
        for (NetworkInterfaceInfo item : listNetworkInterfaces()) {
            if (item.name().equalsIgnoreCase(target) || item.displayName().equalsIgnoreCase(target)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 获取本机全部 IP 地址。
     *
     * @return IP 地址列表
     */
    public static List<String> listIpAddresses() {
        Set<String> result = new HashSet<>();
        for (NetworkInterfaceInfo item : listNetworkInterfaces()) {
            result.addAll(item.ipv4Addresses());
            result.addAll(item.ipv6Addresses());
        }
        return List.copyOf(result);
    }

    /**
     * 获取本机主 IP 地址。
     *
     * @return 主 IP 地址，无法获取时返回空字符串
     */
    public static String getPrimaryIpAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            String address = localHost.getHostAddress();
            if (!isBlank(address) && !address.startsWith("127.")) {
                return address;
            }
        } catch (Exception ignored) {
            // 忽略主机名解析异常，继续从网卡列表查找
        }
        for (String ip : listIpAddresses()) {
            if (!isBlank(ip) && !ip.startsWith("127.") && !ip.equals("::1") && !ip.startsWith("fe80")) {
                return ip;
            }
        }
        return "";
    }

    /**
     * 获取本机全部 MAC 地址。
     *
     * @return MAC 地址列表
     */
    public static List<String> listMacAddresses() {
        Set<String> result = new HashSet<>();
        for (NetworkInterfaceInfo item : listNetworkInterfaces()) {
            if (!isBlank(item.macAddress())) {
                result.add(item.macAddress());
            }
        }
        return List.copyOf(result);
    }

    /**
     * 获取主机名。
     *
     * @return 主机名
     */
    public static String getHostname() {
        String host = safeString(operatingSystem.getNetworkParams().getHostName());
        if (!isBlank(host)) {
            return host;
        }
        try {
            return safeString(InetAddress.getLocalHost().getHostName());
        } catch (Exception ignored) {
            return "";
        }
    }

    /**
     * 获取域名。
     *
     * @return 域名
     */
    public static String getDomainName() {
        return safeString(operatingSystem.getNetworkParams().getDomainName());
    }

    /**
     * 获取 DNS 服务器列表。
     *
     * @return DNS 服务器列表
     */
    public static List<String> getDnsServers() {
        String[] servers = operatingSystem.getNetworkParams().getDnsServers();
        return servers == null ? List.of() : List.copyOf(Arrays.asList(servers));
    }

    /**
     * 获取默认网关。
     *
     * @return 默认网关，优先返回 IPv4 网关
     */
    public static String getGateway() {
        NetworkParams params = operatingSystem.getNetworkParams();
        String ipv4 = safeString(params.getIpv4DefaultGateway());
        if (!isBlank(ipv4)) {
            return ipv4;
        }
        return safeString(params.getIpv6DefaultGateway());
    }

    /**
     * 获取网络统计列表。
     *
     * @return 网络统计列表
     */
    public static List<NetworkStats> listNetworkStats() {
        List<NetworkStats> result = new ArrayList<>();
        for (NetworkIF networkIF : nullToEmpty(hardware.getNetworkIFs())) {
            networkIF.updateAttributes();
            result.add(new NetworkStats(safeString(networkIF.getName()), Math.max(0L, networkIF.getBytesRecv()), Math.max(0L, networkIF.getBytesSent()),
                    Math.max(0L, networkIF.getPacketsRecv()), Math.max(0L, networkIF.getPacketsSent()), Math.max(0L, networkIF.getInErrors()),
                    Math.max(0L, networkIF.getOutErrors()), Math.max(0L, networkIF.getSpeed())));
        }
        return List.copyOf(result);
    }

    /**
     * 获取网络吞吐量估算结果，单位字节每秒。
     *
     * @return 网络吞吐量
     */
    public static NetworkThroughput getNetworkThroughput() {
        List<NetworkStats> before = listNetworkStats();
        sleepQuietly(1000L);
        List<NetworkStats> after = listNetworkStats();
        long recv = 0L;
        long sent = 0L;
        for (NetworkStats item : after) {
            NetworkStats old = findNetworkStats(before, item.name());
            if (old != null) {
                recv = safeAdd(recv, Math.max(0L, item.bytesReceived() - old.bytesReceived()));
                sent = safeAdd(sent, Math.max(0L, item.bytesSent() - old.bytesSent()));
            }
        }
        return new NetworkThroughput(recv, sent, System.currentTimeMillis());
    }

    /**
     * 获取 TCP 统计信息。
     *
     * @return TCP 统计信息
     */
    public static TcpStats getTcpStats() {
        InternetProtocolStats stats = operatingSystem.getInternetProtocolStats();
        InternetProtocolStats.TcpStats tcp4 = stats.getTCPv4Stats();
        InternetProtocolStats.TcpStats tcp6 = stats.getTCPv6Stats();
        return new TcpStats(toTcpStatsInfo(tcp4), toTcpStatsInfo(tcp6));
    }

    /**
     * 获取 UDP 统计信息。
     *
     * @return UDP 统计信息
     */
    public static UdpStats getUdpStats() {
        InternetProtocolStats stats = operatingSystem.getInternetProtocolStats();
        InternetProtocolStats.UdpStats udp4 = stats.getUDPv4Stats();
        InternetProtocolStats.UdpStats udp6 = stats.getUDPv6Stats();
        return new UdpStats(toUdpStatsInfo(udp4), toUdpStatsInfo(udp6));
    }

    /**
     * 获取网络快照。
     *
     * @return 网络快照
     */
    public static NetworkSnapshot snapshotNetwork() {
        return new NetworkSnapshot(listNetworkInterfaces(), listNetworkStats(), getTcpStats(), getUdpStats(), getHostname(), getGateway(), getDnsServers(),
                System.currentTimeMillis());
    }

    /**
     * 获取进程列表。
     *
     * @return 进程列表
     */
    public static List<ProcessInfo> listProcesses() {
        return listProcesses(0);
    }

    /**
     * 获取指定数量的进程列表。
     *
     * @param limit 返回数量，0 表示不限制
     * @return 进程列表
     */
    public static List<ProcessInfo> listProcesses(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit 不能小于 0");
        }
        List<OSProcess> processes = operatingSystem.getProcesses(process -> true, Comparator.comparingLong(OSProcess::getProcessID), limit);
        List<ProcessInfo> result = new ArrayList<>();
        for (OSProcess process : nullToEmpty(processes)) {
            result.add(toProcessInfo(process));
        }
        return List.copyOf(result);
    }

    /**
     * 获取指定 PID 的进程信息。
     *
     * @param pid 进程 ID
     * @return 进程信息，不存在时返回 null
     */
    public static ProcessInfo getProcess(int pid) {
        if (pid <= 0) {
            throw new IllegalArgumentException("pid 必须大于 0");
        }
        OSProcess process = operatingSystem.getProcess(pid);
        return process == null ? null : toProcessInfo(process);
    }

    /**
     * 获取当前 Java 进程信息。
     *
     * @return 当前 Java 进程信息
     */
    public static ProcessInfo getCurrentProcess() {
        OSProcess process = operatingSystem.getCurrentProcess();
        return process == null ? null : toProcessInfo(process);
    }

    /**
     * 获取当前 Java 进程 ID。
     *
     * @return 当前进程 ID
     */
    public static int getCurrentPid() {
        return Math.toIntExact(ProcessHandle.current().pid());
    }

    /**
     * 获取系统进程数量。
     *
     * @return 进程数量
     */
    public static int getProcessCount() {
        return Math.max(0, operatingSystem.getProcessCount());
    }

    /**
     * 获取系统线程数量。
     *
     * @return 线程数量
     */
    public static int getThreadCount() {
        return Math.max(0, operatingSystem.getThreadCount());
    }

    /**
     * 获取 CPU 占用最高的进程列表。
     *
     * @param limit 返回数量
     * @return CPU 占用最高的进程列表
     */
    public static List<ProcessInfo> listTopCpuProcesses(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit 必须大于 0");
        }
        Comparator<OSProcess> comparator = Comparator.comparingDouble(OSProcess::getProcessCpuLoadCumulative).reversed();
        List<OSProcess> processes = operatingSystem.getProcesses(process -> true, comparator, limit);
        List<ProcessInfo> result = new ArrayList<>();
        for (OSProcess process : nullToEmpty(processes)) {
            result.add(toProcessInfo(process));
        }
        return List.copyOf(result);
    }

    /**
     * 获取内存占用最高的进程列表。
     *
     * @param limit 返回数量
     * @return 内存占用最高的进程列表
     */
    public static List<ProcessInfo> listTopMemoryProcesses(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit 必须大于 0");
        }
        Comparator<OSProcess> comparator = Comparator.comparingLong(OSProcess::getResidentSetSize).reversed();
        List<OSProcess> processes = operatingSystem.getProcesses(process -> true, comparator, limit);
        List<ProcessInfo> result = new ArrayList<>();
        for (OSProcess process : nullToEmpty(processes)) {
            result.add(toProcessInfo(process));
        }
        return List.copyOf(result);
    }

    /**
     * 按进程名称查找进程列表。
     *
     * @param name 进程名称关键字
     * @return 匹配的进程列表
     */
    public static List<ProcessInfo> findProcessesByName(String name) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("name 不能为空");
        }
        String keyword = name.trim().toLowerCase(Locale.ROOT);
        Predicate<OSProcess> filter = process -> safeString(process.getName()).toLowerCase(Locale.ROOT).contains(keyword);
        List<OSProcess> processes = operatingSystem.getProcesses(filter, Comparator.comparingLong(OSProcess::getProcessID), 0);
        List<ProcessInfo> result = new ArrayList<>();
        for (OSProcess process : nullToEmpty(processes)) {
            result.add(toProcessInfo(process));
        }
        return List.copyOf(result);
    }

    /**
     * 判断指定进程是否运行。
     *
     * @param pid 进程 ID
     * @return 运行中返回 true，否则返回 false
     */
    public static boolean isProcessRunning(int pid) {
        if (pid <= 0) {
            return false;
        }
        return operatingSystem.getProcess(pid) != null;
    }

    /**
     * 获取进程快照。
     *
     * @return 进程快照
     */
    public static ProcessSnapshot snapshotProcesses() {
        return new ProcessSnapshot(getProcessCount(), getThreadCount(), listTopCpuProcesses(10), listTopMemoryProcesses(10), System.currentTimeMillis());
    }

    /**
     * 获取系统服务列表。
     *
     * @return 系统服务列表
     */
    public static List<ServiceInfo> listServices() {
        List<ServiceInfo> result = new ArrayList<>();
        for (OSService service : nullToEmpty(operatingSystem.getServices())) {
            result.add(toServiceInfo(service));
        }
        return List.copyOf(result);
    }

    /**
     * 获取运行中的服务列表。
     *
     * @return 运行中的服务列表
     */
    public static List<ServiceInfo> listRunningServices() {
        return listServices().stream().filter(service -> "RUNNING".equalsIgnoreCase(service.state())).toList();
    }

    /**
     * 获取已停止的服务列表。
     *
     * @return 已停止的服务列表
     */
    public static List<ServiceInfo> listStoppedServices() {
        return listServices().stream().filter(service -> !"RUNNING".equalsIgnoreCase(service.state())).toList();
    }

    /**
     * 按名称查找服务。
     *
     * @param name 服务名称
     * @return 服务信息，不存在时返回 null
     */
    public static ServiceInfo findService(String name) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("name 不能为空");
        }
        String target = name.trim().toLowerCase(Locale.ROOT);
        for (ServiceInfo service : listServices()) {
            if (service.name().toLowerCase(Locale.ROOT).contains(target)) {
                return service;
            }
        }
        return null;
    }

    /**
     * 判断服务是否运行。
     *
     * @param name 服务名称
     * @return 运行中返回 true，否则返回 false
     */
    public static boolean isServiceRunning(String name) {
        ServiceInfo service = findService(name);
        return service != null && "RUNNING".equalsIgnoreCase(service.state());
    }

    /**
     * 获取服务状态。
     *
     * @param name 服务名称
     * @return 服务状态，不存在时返回 UNKNOWN
     */
    public static String getServiceStatus(String name) {
        ServiceInfo service = findService(name);
        return service == null ? "UNKNOWN" : service.state();
    }

    /**
     * 获取整机信息。
     *
     * @return 整机信息
     */
    public static ComputerSystemInfo getComputerSystemInfo() {
        ComputerSystem cs = hardware.getComputerSystem();
        return new ComputerSystemInfo(safeString(cs.getManufacturer()), safeString(cs.getModel()), safeString(cs.getSerialNumber()), safeString(cs.getHardwareUUID()),
                getFirmwareInfo(), getBaseboardInfo());
    }

    /**
     * 获取设备厂商。
     *
     * @return 设备厂商
     */
    public static String getManufacturer() {
        return safeString(hardware.getComputerSystem().getManufacturer());
    }

    /**
     * 获取设备型号。
     *
     * @return 设备型号
     */
    public static String getModel() {
        return safeString(hardware.getComputerSystem().getModel());
    }

    /**
     * 获取设备序列号。
     *
     * @return 设备序列号
     */
    public static String getSerialNumber() {
        return safeString(hardware.getComputerSystem().getSerialNumber());
    }

    /**
     * 获取固件信息。
     *
     * @return 固件信息
     */
    public static FirmwareInfo getFirmwareInfo() {
        Firmware firmware = hardware.getComputerSystem().getFirmware();
        return new FirmwareInfo(safeString(firmware.getManufacturer()), safeString(firmware.getName()), safeString(firmware.getDescription()),
                safeString(firmware.getVersion()), safeString(firmware.getReleaseDate()));
    }

    /**
     * 获取主板信息。
     *
     * @return 主板信息
     */
    public static BaseboardInfo getBaseboardInfo() {
        Baseboard baseboard = hardware.getComputerSystem().getBaseboard();
        return new BaseboardInfo(safeString(baseboard.getManufacturer()), safeString(baseboard.getModel()), safeString(baseboard.getVersion()),
                safeString(baseboard.getSerialNumber()));
    }

    /**
     * 获取硬件 UUID。
     *
     * @return 硬件 UUID
     */
    public static String getHardwareUuid() {
        return safeString(hardware.getComputerSystem().getHardwareUUID());
    }

    /**
     * 获取机器唯一标识。
     *
     * @return 机器唯一标识
     */
    public static String getMachineId() {
        String id = firstNotBlank(getHardwareUuid(), getSerialNumber(), getBaseboardInfo().serialNumber(), getPrimaryMacAddress());
        return isBlank(id) ? getDeviceFingerprint() : id;
    }

    /**
     * 获取设备指纹。
     *
     * @return 设备指纹
     */
    public static String getDeviceFingerprint() {
        String raw = String.join("|", getManufacturer(), getModel(), getSerialNumber(), getHardwareUuid(), getCpuVendor(), getCpuName(), getPrimaryMacAddress());
        return sha256Hex(raw);
    }

    /**
     * 获取硬件快照。
     *
     * @return 硬件快照
     */
    public static HardwareSnapshot snapshotHardware() {
        return new HardwareSnapshot(getComputerSystemInfo(), getCpuInfo(), listDisks(), listGraphicsCards(), listUsbDevices(false), listSoundCards(), getSensorsInfo(),
                System.currentTimeMillis());
    }

    /**
     * 获取传感器信息。
     *
     * @return 传感器信息
     */
    public static SensorsInfo getSensorsInfo() {
        Sensors sensors = hardware.getSensors();
        return new SensorsInfo(nonNegativeOrZero(sensors.getCpuTemperature()), safeIntArray(sensors.getFanSpeeds()), nonNegativeOrZero(sensors.getCpuVoltage()));
    }

    /**
     * 获取 CPU 温度。
     *
     * @return CPU 温度，无法获取时返回 0
     */
    public static double getCpuTemperature() {
        return getSensorsInfo().cpuTemperature();
    }

    /**
     * 获取风扇转速数组。
     *
     * @return 风扇转速数组
     */
    public static int[] getFanSpeeds() {
        return safeIntArray(hardware.getSensors().getFanSpeeds());
    }

    /**
     * 获取 CPU 电压。
     *
     * @return CPU 电压，无法获取时返回 0
     */
    public static double getCpuVoltage() {
        return getSensorsInfo().cpuVoltage();
    }

    /**
     * 获取电源信息列表。
     *
     * @return 电源信息列表
     */
    public static List<PowerSourceInfo> listPowerSources() {
        List<PowerSourceInfo> result = new ArrayList<>();
        for (PowerSource source : nullToEmpty(hardware.getPowerSources())) {
            result.add(new PowerSourceInfo(safeString(source.getName()), safeString(source.getDeviceName()), normalizePercent(source.getRemainingCapacityPercent() * 100.0D),
                    source.isPowerOnLine(), source.isCharging(), source.isDischarging(), nonNegativeOrZero(source.getPowerUsageRate()), source.getTimeRemainingEstimated()));
        }
        return List.copyOf(result);
    }

    /**
     * 获取主电池信息。
     *
     * @return 电池信息，不存在时返回 null
     */
    public static BatteryInfo getBatteryInfo() {
        List<PowerSourceInfo> sources = listPowerSources();
        if (sources.isEmpty()) {
            return null;
        }
        PowerSourceInfo first = sources.getFirst();
        return new BatteryInfo(first.name(), first.deviceName(), first.remainingCapacityPercent(), first.powerOnLine(), first.charging(), first.discharging(),
                first.timeRemainingEstimated());
    }

    /**
     * 获取电池电量百分比。
     *
     * @return 电池电量百分比，无电池时返回 0
     */
    public static double getBatteryPercent() {
        BatteryInfo battery = getBatteryInfo();
        return battery == null ? 0.0D : battery.remainingCapacityPercent();
    }

    /**
     * 判断主电池是否正在充电。
     *
     * @return 充电中返回 true，否则返回 false
     */
    public static boolean isCharging() {
        BatteryInfo battery = getBatteryInfo();
        return battery != null && battery.charging();
    }

    /**
     * 获取传感器快照。
     *
     * @return 传感器快照
     */
    public static SensorsSnapshot snapshotSensors() {
        return new SensorsSnapshot(getSensorsInfo(), listPowerSources(), System.currentTimeMillis());
    }

    /**
     * 获取显卡列表。
     *
     * @return 显卡列表
     */
    public static List<GraphicsCardInfo> listGraphicsCards() {
        List<GraphicsCardInfo> result = new ArrayList<>();
        for (GraphicsCard card : nullToEmpty(hardware.getGraphicsCards())) {
            result.add(new GraphicsCardInfo(safeString(card.getName()), safeString(card.getDeviceId()), safeString(card.getVendor()), safeString(card.getVersionInfo()),
                    Math.max(0L, card.getVRam())));
        }
        return List.copyOf(result);
    }

    /**
     * 获取显示器列表。
     *
     * @return 显示器列表
     */
    public static List<DisplayInfo> listDisplays() {
        List<DisplayInfo> result = new ArrayList<>();
        for (Display display : nullToEmpty(hardware.getDisplays())) {
            byte[] edid = display.getEdid();
            result.add(new DisplayInfo(edid == null ? 0 : edid.length, bytesToHex(edid)));
        }
        return List.copyOf(result);
    }

    /**
     * 获取 USB 设备列表，默认不展开树形子节点。
     *
     * @return USB 设备列表
     */
    public static List<UsbDeviceInfo> listUsbDevices() {
        return listUsbDevices(false);
    }

    /**
     * 获取 USB 设备列表。
     *
     * @param tree 是否按树形结构获取
     * @return USB 设备列表
     */
    public static List<UsbDeviceInfo> listUsbDevices(boolean tree) {
        List<UsbDeviceInfo> result = new ArrayList<>();
        for (UsbDevice device : nullToEmpty(hardware.getUsbDevices(tree))) {
            result.add(toUsbDeviceInfo(device));
        }
        return List.copyOf(result);
    }

    /**
     * 获取声卡列表。
     *
     * @return 声卡列表
     */
    public static List<SoundCardInfo> listSoundCards() {
        List<SoundCardInfo> result = new ArrayList<>();
        for (SoundCard card : nullToEmpty(hardware.getSoundCards())) {
            result.add(new SoundCardInfo(safeString(card.getName()), safeString(card.getCodec()), safeString(card.getDriverVersion())));
        }
        return List.copyOf(result);
    }

    /**
     * 获取主显卡信息。
     *
     * @return 主显卡信息，不存在时返回 null
     */
    public static GraphicsCardInfo getPrimaryGraphicsCard() {
        List<GraphicsCardInfo> cards = listGraphicsCards();
        return cards.isEmpty() ? null : cards.getFirst();
    }

    /**
     * 获取外设快照。
     *
     * @return 外设快照
     */
    public static DeviceSnapshot snapshotDevices() {
        return new DeviceSnapshot(listGraphicsCards(), listDisplays(), listUsbDevices(false), listSoundCards(), System.currentTimeMillis());
    }

    /**
     * 获取 JVM 信息。
     *
     * @return JVM 信息
     */
    public static JvmInfo getJvmInfo() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        return new JvmInfo(safeString(System.getProperty("java.version")), safeString(System.getProperty("java.vendor")), safeString(System.getProperty("java.home")),
                safeString(runtime.getVmName()), safeString(runtime.getVmVendor()), safeString(runtime.getVmVersion()), runtime.getStartTime(), getJvmUptime(),
                runtime.getInputArguments());
    }

    /**
     * 获取 JVM 内存信息。
     *
     * @return JVM 内存信息
     */
    public static JvmMemoryInfo getJvmMemoryInfo() {
        MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = bean.getHeapMemoryUsage();
        MemoryUsage nonHeap = bean.getNonHeapMemoryUsage();
        return new JvmMemoryInfo(heap.getInit(), heap.getUsed(), heap.getCommitted(), heap.getMax(), nonHeap.getInit(), nonHeap.getUsed(), nonHeap.getCommitted(),
                nonHeap.getMax(), Runtime.getRuntime().availableProcessors());
    }

    /**
     * 获取 JVM 线程信息。
     *
     * @return JVM 线程信息
     */
    public static JvmThreadInfo getJvmThreadInfo() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return new JvmThreadInfo(bean.getThreadCount(), bean.getDaemonThreadCount(), bean.getPeakThreadCount(), bean.getTotalStartedThreadCount());
    }

    /**
     * 获取 JVM GC 信息。
     *
     * @return JVM GC 信息
     */
    public static JvmGcInfo getJvmGcInfo() {
        List<GcCollectorInfo> collectors = new ArrayList<>();
        long totalCount = 0L;
        long totalTime = 0L;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = Math.max(0L, bean.getCollectionCount());
            long time = Math.max(0L, bean.getCollectionTime());
            totalCount = safeAdd(totalCount, count);
            totalTime = safeAdd(totalTime, time);
            collectors.add(new GcCollectorInfo(safeString(bean.getName()), count, time, bean.getMemoryPoolNames() == null ? List.of() : Arrays.asList(bean.getMemoryPoolNames())));
        }
        return new JvmGcInfo(totalCount, totalTime, List.copyOf(collectors));
    }

    /**
     * 获取 JVM 运行时长，单位毫秒。
     *
     * @return JVM 运行时长
     */
    public static long getJvmUptime() {
        return Math.max(0L, ManagementFactory.getRuntimeMXBean().getUptime());
    }

    /**
     * 获取 Java 版本。
     *
     * @return Java 版本
     */
    public static String getJavaVersion() {
        return safeString(System.getProperty("java.version"));
    }

    /**
     * 获取 Java Home 路径。
     *
     * @return Java Home 路径
     */
    public static String getJavaHome() {
        return safeString(System.getProperty("java.home"));
    }

    /**
     * 获取当前 Java 进程 CPU 占用率百分比。
     *
     * @return 当前 Java 进程 CPU 占用率百分比
     */
    public static double getCurrentProcessCpuLoad() {
        OSProcess process = operatingSystem.getCurrentProcess();
        if (process == null) {
            return 0.0D;
        }
        return normalizePercent(process.getProcessCpuLoadCumulative() * 100.0D);
    }

    /**
     * 获取当前 Java 进程常驻内存占用，单位字节。
     *
     * @return 当前 Java 进程常驻内存占用
     */
    public static long getCurrentProcessMemoryUsage() {
        OSProcess process = operatingSystem.getCurrentProcess();
        return process == null ? 0L : Math.max(0L, process.getResidentSetSize());
    }

    /**
     * 获取 JVM 快照。
     *
     * @return JVM 快照
     */
    public static JvmSnapshot snapshotJvm() {
        return new JvmSnapshot(getJvmInfo(), getJvmMemoryInfo(), getJvmThreadInfo(), getJvmGcInfo(), getCurrentProcessCpuLoad(), getCurrentProcessMemoryUsage(),
                System.currentTimeMillis());
    }

    /**
     * 判断当前是否处于容器环境。
     *
     * @return 容器环境返回 true，否则返回 false
     */
    public static boolean isContainerEnvironment() {
        return Files.exists(Path.of("/.dockerenv")) || Files.exists(Path.of("/run/.containerenv")) || containsAny(readFirstExistingFile(List.of("/proc/1/cgroup", "/proc/self/cgroup")),
                "docker", "kubepods", "containerd", "podman", "lxc");
    }

    /**
     * 判断当前是否处于虚拟机环境。
     *
     * @return 虚拟机环境返回 true，否则返回 false
     */
    public static boolean isVirtualMachine() {
        String manufacturer = getManufacturer().toLowerCase(Locale.ROOT);
        String model = getModel().toLowerCase(Locale.ROOT);
        String joined = manufacturer + " " + model;
        return containsAny(joined, "vmware", "virtualbox", "kvm", "qemu", "hyper-v", "xen", "parallels", "bochs", "bhyve");
    }

    /**
     * 获取容器环境信息。
     *
     * @return 容器环境信息
     */
    public static ContainerInfo getContainerInfo() {
        return new ContainerInfo(isContainerEnvironment(), isVirtualMachine(), getCgroupMemoryLimit(), getCgroupCpuLimit(), getEffectiveMemoryLimit(), getEffectiveCpuLimit());
    }

    /**
     * 获取 cgroup 内存限制，单位字节。
     *
     * @return cgroup 内存限制，无法获取时返回 -1
     */
    public static long getCgroupMemoryLimit() {
        String v2 = readTrimmed(Path.of("/sys/fs/cgroup/memory.max"));
        if (!isBlank(v2) && !"max".equalsIgnoreCase(v2)) {
            return parseLongOrDefault(v2, -1L);
        }
        String v1 = readTrimmed(Path.of("/sys/fs/cgroup/memory/memory.limit_in_bytes"));
        long value = parseLongOrDefault(v1, -1L);
        if (value <= 0L || value >= Long.MAX_VALUE / 4L) {
            return -1L;
        }
        return value;
    }

    /**
     * 获取 cgroup CPU 限制，单位核心数。
     *
     * @return cgroup CPU 限制，无法获取时返回 -1
     */
    public static double getCgroupCpuLimit() {
        String cpuMax = readTrimmed(Path.of("/sys/fs/cgroup/cpu.max"));
        if (!isBlank(cpuMax)) {
            String[] parts = cpuMax.split("\\s+");
            if (parts.length >= 2 && !"max".equalsIgnoreCase(parts[0])) {
                long quota = parseLongOrDefault(parts[0], -1L);
                long period = parseLongOrDefault(parts[1], -1L);
                if (quota > 0L && period > 0L) {
                    return safeDivide(quota, period);
                }
            }
        }
        long quota = parseLongOrDefault(readTrimmed(Path.of("/sys/fs/cgroup/cpu/cpu.cfs_quota_us")), -1L);
        long period = parseLongOrDefault(readTrimmed(Path.of("/sys/fs/cgroup/cpu/cpu.cfs_period_us")), -1L);
        if (quota > 0L && period > 0L) {
            return safeDivide(quota, period);
        }
        return -1.0D;
    }

    /**
     * 获取当前进程实际可用内存上限，单位字节。
     *
     * @return 实际可用内存上限
     */
    public static long getEffectiveMemoryLimit() {
        long cgroupLimit = getCgroupMemoryLimit();
        long physical = getTotalMemory();
        if (cgroupLimit > 0L && physical > 0L) {
            return Math.min(cgroupLimit, physical);
        }
        return physical;
    }

    /**
     * 获取当前进程实际可用 CPU 上限，单位核心数。
     *
     * @return 实际可用 CPU 上限
     */
    public static double getEffectiveCpuLimit() {
        double cgroupLimit = getCgroupCpuLimit();
        int logical = getLogicalProcessorCount();
        if (cgroupLimit > 0.0D && logical > 0) {
            return Math.min(cgroupLimit, logical);
        }
        return logical;
    }

    /**
     * 获取运行环境快照。
     *
     * @return 运行环境快照
     */
    public static RuntimeEnvironmentSnapshot snapshotRuntimeEnvironment() {
        return new RuntimeEnvironmentSnapshot(getContainerInfo(), getJvmInfo(), getCurrentProcess(), System.currentTimeMillis());
    }

    /**
     * 格式化字节大小。
     *
     * @param bytes 字节数
     * @return 格式化后的容量文本
     */
    public static String formatBytes(long bytes) {
        if (bytes <= 0L) {
            return "0 B";
        }
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        double value = bytes;
        int index = 0;
        while (value >= 1024.0D && index < units.length - 1) {
            value /= 1024.0D;
            index++;
        }
        return round(value, 2).stripTrailingZeros().toPlainString() + " " + units[index];
    }

    /**
     * 格式化百分比。
     *
     * @param value 百分比数值
     * @return 格式化后的百分比文本
     */
    public static String formatPercent(double value) {
        return round(normalizePercent(value), 2).stripTrailingZeros().toPlainString() + "%";
    }

    /**
     * 格式化频率。
     *
     * @param hz 频率，单位 Hz
     * @return 格式化后的频率文本
     */
    public static String formatFrequency(long hz) {
        if (hz <= 0L) {
            return "0 Hz";
        }
        String[] units = {"Hz", "KHz", "MHz", "GHz", "THz"};
        double value = hz;
        int index = 0;
        while (value >= 1000.0D && index < units.length - 1) {
            value /= 1000.0D;
            index++;
        }
        return round(value, 2).stripTrailingZeros().toPlainString() + " " + units[index];
    }

    /**
     * 格式化运行时长。
     *
     * @param seconds 秒数
     * @return 格式化后的时长文本
     */
    public static String formatDuration(long seconds) {
        if (seconds <= 0L) {
            return "0秒";
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("天");
        }
        if (hours > 0) {
            builder.append(hours).append("小时");
        }
        if (minutes > 0) {
            builder.append(minutes).append("分钟");
        }
        if (secs > 0 || builder.isEmpty()) {
            builder.append(secs).append("秒");
        }
        return builder.toString();
    }

    /**
     * 将字节转换为 MB。
     *
     * @param bytes 字节数
     * @return MB 数值
     */
    public static BigDecimal toMegabytes(long bytes) {
        return BigDecimal.valueOf(Math.max(0L, bytes)).divide(BigDecimal.valueOf(1024L * 1024L), 2, RoundingMode.HALF_UP);
    }

    /**
     * 将字节转换为 GB。
     *
     * @param bytes 字节数
     * @return GB 数值
     */
    public static BigDecimal toGigabytes(long bytes) {
        return BigDecimal.valueOf(Math.max(0L, bytes)).divide(BigDecimal.valueOf(1024L * 1024L * 1024L), 2, RoundingMode.HALF_UP);
    }

    /**
     * 对数值进行四舍五入。
     *
     * @param value 数值
     * @param scale 小数位数
     * @return 四舍五入后的数值
     */
    public static BigDecimal round(double value, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("scale 不能小于 0");
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    /**
     * 安全计算百分比，取值范围 0 到 100。
     *
     * @param used  已用值
     * @param total 总值
     * @return 百分比
     */
    public static double safePercent(long used, long total) {
        if (used <= 0L || total <= 0L) {
            return 0.0D;
        }
        return normalizePercent((used * 100.0D) / total);
    }

    /**
     * 安全除法。
     *
     * @param dividend 被除数
     * @param divisor  除数
     * @return 计算结果，除数为 0 时返回 0
     */
    public static double safeDivide(long dividend, long divisor) {
        if (divisor == 0L) {
            return 0.0D;
        }
        return dividend * 1.0D / divisor;
    }

    /**
     * 判断 CPU 是否超过阈值。
     *
     * @param threshold 阈值百分比
     * @return 超过阈值返回 true，否则返回 false
     */
    public static boolean isCpuOverloaded(double threshold) {
        validateThreshold(threshold);
        return getCpuLoadPercent() >= threshold;
    }

    /**
     * 判断内存是否超过阈值。
     *
     * @param threshold 阈值百分比
     * @return 超过阈值返回 true，否则返回 false
     */
    public static boolean isMemoryOverloaded(double threshold) {
        validateThreshold(threshold);
        return getMemoryUsagePercent() >= threshold;
    }

    /**
     * 判断指定挂载点磁盘是否超过阈值。
     *
     * @param mount     挂载点
     * @param threshold 阈值百分比
     * @return 超过阈值返回 true，否则返回 false
     */
    public static boolean isDiskOverloaded(String mount, double threshold) {
        validateThreshold(threshold);
        return getDiskUsage(mount) >= threshold;
    }

    /**
     * 判断网络是否不可用。
     *
     * @return 网络不可用返回 true，否则返回 false
     */
    public static boolean isNetworkUnavailable() {
        return getPrimaryIpAddress().isEmpty() && listNetworkInterfaces().isEmpty();
    }

    /**
     * 根据资源阈值检查当前资源状态。
     *
     * @param threshold 资源阈值
     * @return 阈值检查结果
     */
    public static ThresholdCheckResult checkResourceThreshold(ResourceThreshold threshold) {
        Objects.requireNonNull(threshold, "threshold 不能为 null");
        validateThreshold(threshold.cpuThreshold());
        validateThreshold(threshold.memoryThreshold());
        validateThreshold(threshold.diskThreshold());
        List<WarningItem> items = new ArrayList<>();
        double cpu = getCpuLoadPercent();
        double memory = getMemoryUsagePercent();
        double disk = isBlank(threshold.mount()) ? getDiskUsage() : getDiskUsage(threshold.mount());
        addWarningIfExceeded(items, "CPU", cpu, threshold.cpuThreshold());
        addWarningIfExceeded(items, "MEMORY", memory, threshold.memoryThreshold());
        addWarningIfExceeded(items, "DISK", disk, threshold.diskThreshold());
        return new ThresholdCheckResult(items.isEmpty(), cpu, memory, disk, List.copyOf(items));
    }

    /**
     * 根据健康检查规则检查系统健康状态。
     *
     * @param rule 健康检查规则
     * @return 健康检查结果
     */
    public static HealthCheckResult checkSystemHealth(HealthRule rule) {
        Objects.requireNonNull(rule, "rule 不能为 null");
        ThresholdCheckResult result = checkResourceThreshold(new ResourceThreshold(rule.cpuThreshold(), rule.memoryThreshold(), rule.diskThreshold(), rule.mount()));
        boolean networkOk = !rule.checkNetwork() || !isNetworkUnavailable();
        List<WarningItem> items = new ArrayList<>(result.items());
        if (!networkOk) {
            items.add(new WarningItem("CRITICAL", "NETWORK", "网络不可用", 100.0D, 0.0D));
        }
        return new HealthCheckResult(result.ok() && networkOk, List.copyOf(items), System.currentTimeMillis());
    }

    /**
     * 获取当前告警项。
     *
     * @return 告警项列表
     */
    public static List<WarningItem> getWarningItems() {
        return checkResourceThreshold(new ResourceThreshold(DEFAULT_WARN_THRESHOLD, DEFAULT_WARN_THRESHOLD, DEFAULT_WARN_THRESHOLD, null)).items();
    }

    /**
     * 获取当前严重告警项。
     *
     * @return 严重告警项列表
     */
    public static List<WarningItem> getCriticalItems() {
        return checkResourceThreshold(new ResourceThreshold(DEFAULT_CRITICAL_THRESHOLD, DEFAULT_CRITICAL_THRESHOLD, DEFAULT_CRITICAL_THRESHOLD, null)).items();
    }

    /**
     * 获取资源快照。
     *
     * @return 资源快照
     */
    public static ResourceSnapshot snapshotResource() {
        return new ResourceSnapshot(snapshotCpu(), snapshotMemory(), snapshotDisk(), snapshotNetwork(), System.currentTimeMillis());
    }

    /**
     * 获取运行时快照。
     *
     * @return 运行时快照
     */
    public static RuntimeSnapshot snapshotRuntime() {
        return new RuntimeSnapshot(snapshotJvm(), getCurrentProcess(), getContainerInfo(), System.currentTimeMillis());
    }

    /**
     * 获取全量系统监控快照。
     *
     * @return 全量监控快照
     */
    public static AllSnapshot snapshotAll() {
        return new AllSnapshot(snapshotSystem(), snapshotHardware(), snapshotSensors(), snapshotDevices(), snapshotRuntimeEnvironment(), System.currentTimeMillis());
    }

    private static DiskInfo toDiskInfo(HWDiskStore disk) {
        return new DiskInfo(safeString(disk.getName()), safeString(disk.getModel()), safeString(disk.getSerial()), Math.max(0L, disk.getSize()),
                Math.max(0L, disk.getReads()), Math.max(0L, disk.getReadBytes()), Math.max(0L, disk.getWrites()), Math.max(0L, disk.getWriteBytes()),
                Math.max(0L, disk.getTransferTime()), disk.getPartitions() == null ? 0 : disk.getPartitions().size());
    }

    private static PartitionInfo toPartitionInfo(HWPartition partition) {
        return new PartitionInfo(safeString(partition.getIdentification()), safeString(partition.getName()), safeString(partition.getType()), safeString(partition.getUuid()),
                Math.max(0L, partition.getSize()), partition.getMajor(), partition.getMinor(), safeString(partition.getMountPoint()));
    }

    private static FileStoreInfo toFileStoreInfo(OSFileStore store) {
        long total = Math.max(0L, store.getTotalSpace());
        long usable = Math.max(0L, store.getUsableSpace());
        long free = Math.max(0L, store.getFreeSpace());
        long used = Math.max(0L, total - usable);
        return new FileStoreInfo(safeString(store.getName()), safeString(store.getVolume()), safeString(store.getLabel()), safeString(store.getMount()),
                safeString(store.getDescription()), safeString(store.getType()), safeString(store.getOptions()), safeString(store.getUUID()), total, usable, free, used,
                safePercent(used, total));
    }

    private static NetworkInterfaceInfo toNetworkInterfaceInfo(NetworkIF networkIF) {
        return new NetworkInterfaceInfo(safeString(networkIF.getName()), safeString(networkIF.getDisplayName()), safeString(networkIF.getMacaddr()),
                toStringList(networkIF.getIPv4addr()), toStringList(networkIF.getIPv6addr()), Math.max(0L, networkIF.getSpeed()), Math.max(0L, networkIF.getBytesRecv()),
                Math.max(0L, networkIF.getBytesSent()), Math.max(0L, networkIF.getPacketsRecv()), Math.max(0L, networkIF.getPacketsSent()));
    }

    private static ProcessInfo toProcessInfo(OSProcess process) {
        return new ProcessInfo((int) process.getProcessID(), (int) process.getParentProcessID(), safeString(process.getName()), safeString(process.getPath()),
                safeString(process.getCommandLine()), safeString(process.getUser()), safeString(process.getState().name()), Math.max(0, process.getThreadCount()),
                Math.max(0, process.getPriority()), Math.max(0L, process.getVirtualSize()), Math.max(0L, process.getResidentSetSize()), Math.max(0L, process.getKernelTime()),
                Math.max(0L, process.getUserTime()), Math.max(0L, process.getUpTime()), Math.max(0L, process.getStartTime()),
                normalizePercent(process.getProcessCpuLoadCumulative() * 100.0D));
    }

    private static ServiceInfo toServiceInfo(OSService service) {
        return new ServiceInfo(safeString(service.getName()), service.getProcessID(), safeString(service.getState().name()));
    }

    private static UsbDeviceInfo toUsbDeviceInfo(UsbDevice device) {
        List<UsbDeviceInfo> children = new ArrayList<>();
        for (UsbDevice child : nullToEmpty(device.getConnectedDevices())) {
            children.add(toUsbDeviceInfo(child));
        }
        return new UsbDeviceInfo(safeString(device.getName()), safeString(device.getVendor()), safeString(device.getVendorId()), safeString(device.getProductId()),
                safeString(device.getSerialNumber()), safeString(device.getUniqueDeviceId()), List.copyOf(children));
    }

    private static TcpStatsInfo toTcpStatsInfo(InternetProtocolStats.TcpStats stats) {
        return new TcpStatsInfo(Math.max(0L, stats.getConnectionsEstablished()), Math.max(0L, stats.getConnectionsActive()), Math.max(0L, stats.getConnectionsPassive()),
                Math.max(0L, stats.getConnectionFailures()), Math.max(0L, stats.getConnectionsReset()), Math.max(0L, stats.getSegmentsSent()),
                Math.max(0L, stats.getSegmentsReceived()), Math.max(0L, stats.getSegmentsRetransmitted()), Math.max(0L, stats.getInErrors()), Math.max(0L, stats.getOutResets()));
    }

    private static UdpStatsInfo toUdpStatsInfo(InternetProtocolStats.UdpStats stats) {
        return new UdpStatsInfo(Math.max(0L, stats.getDatagramsSent()), Math.max(0L, stats.getDatagramsReceived()), Math.max(0L, stats.getDatagramsNoPort()),
                Math.max(0L, stats.getDatagramsReceivedErrors()));
    }

    private static NetworkStats findNetworkStats(List<NetworkStats> list, String name) {
        if (list == null || name == null) {
            return null;
        }
        for (NetworkStats item : list) {
            if (name.equals(item.name())) {
                return item;
            }
        }
        return null;
    }

    private static String getPrimaryMacAddress() {
        return listMacAddresses().stream().filter(value -> !isBlank(value)).findFirst().orElse("");
    }

    private static void addWarningIfExceeded(List<WarningItem> items, String item, double value, double threshold) {
        if (value >= threshold) {
            items.add(new WarningItem(value >= DEFAULT_CRITICAL_THRESHOLD ? "CRITICAL" : "WARN", item, item + " 使用率超过阈值", value, threshold));
        }
    }

    private static void validateThreshold(double threshold) {
        if (Double.isNaN(threshold) || Double.isInfinite(threshold) || threshold < 0.0D || threshold > 100.0D) {
            throw new IllegalArgumentException("threshold 必须在 0 到 100 之间");
        }
    }

    private static double normalizePercent(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0D) {
            return 0.0D;
        }
        return Math.min(100.0D, value);
    }

    private static double nonNegativeOrZero(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0D) {
            return 0.0D;
        }
        return value;
    }

    private static double[] toPercentArray(double[] values) {
        if (values == null) {
            return new double[0];
        }
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = normalizePercent(values[i] * 100.0D);
        }
        return result;
    }

    private static long[] safeLongArray(long[] values) {
        return values == null ? new long[0] : Arrays.copyOf(values, values.length);
    }

    private static int[] safeIntArray(int[] values) {
        return values == null ? new int[0] : Arrays.copyOf(values, values.length);
    }

    private static double[] safeDoubleArray(double[] values) {
        return values == null ? new double[0] : Arrays.copyOf(values, values.length);
    }

    private static List<String> toStringList(String[] values) {
        if (values == null || values.length == 0) {
            return List.of();
        }
        return Arrays.stream(values).filter(value -> !isBlank(value)).toList();
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static <T> List<T> nullToEmpty(Collection<T> collection) {
        return collection == null ? List.of() : new ArrayList<>(collection);
    }

    private static String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String firstNotBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static boolean containsAny(String text, String... keywords) {
        if (text == null || keywords == null) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String keyword : keywords) {
            if (!isBlank(keyword) && lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static long safeAdd(long left, long right) {
        if (right <= 0L) {
            return left;
        }
        if (Long.MAX_VALUE - left < right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(Math.max(0L, millis));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String readFirstExistingFile(List<String> paths) {
        for (String path : paths) {
            String content = readTrimmed(Path.of(path));
            if (!isBlank(content)) {
                return content;
            }
        }
        return "";
    }

    private static String readTrimmed(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return "";
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8).trim();
        } catch (IOException ignored) {
            return "";
        }
    }

    private static long parseLongOrDefault(String value, long defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String sha256Hex(String value) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(safeString(value).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(bytes);
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(value));
        }
    }

    private static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format(Locale.ROOT, "%02x", b));
        }
        return builder.toString();
    }

    public record SystemOverview(OsInfo osInfo, HostSummary hostSummary, ResourceSummary resourceSummary, long uptime,
                                 LocalDateTime bootTime, boolean healthy) {
    }

    public record SystemSnapshot(SystemOverview overview, CpuSnapshot cpu, MemorySnapshot memory, DiskSnapshot disk,
                                 NetworkSnapshot network, ProcessSnapshot process,
                                 JvmSnapshot jvm) {
    }

    public record HostSummary(String hostname, String domainName, String osName, String osVersion, String architecture,
                              String manufacturer, String model, String serialNumber) {
    }

    public record ResourceSummary(double cpuUsagePercent, double memoryUsagePercent, double diskUsagePercent,
                                  int networkInterfaceCount, int processCount, int threadCount) {
    }

    public record HealthStatus(boolean healthy, List<WarningItem> warnings, List<WarningItem> criticalItems) {
    }

    public record OsInfo(String manufacturer, String family, String version, String codeName, String buildNumber,
                         int bitness, String architecture, long uptime,
                         LocalDateTime bootTime) {
    }

    public record SessionInfo(String username, String terminalDevice, String host, long loginTime) {
    }

    public record CpuInfo(String name, String vendor, String identifier, String processorId, int physicalProcessorCount,
                          int logicalProcessorCount, long maxFrequency,
                          long[] currentFrequencies) {
    }

    public record CpuSnapshot(CpuInfo info, double loadPercent, double[] perCpuLoadPercent, long[] ticks,
                              long timestamp) {
    }

    public record MemoryInfo(long total, long available, long used, double usagePercent, long swapTotal, long swapUsed,
                             double swapUsagePercent, long virtualMax,
                             long virtualInUse) {
    }

    public record MemorySnapshot(MemoryInfo info, long timestamp) {
    }

    public record DiskInfo(String name, String model, String serial, long size, long reads, long readBytes, long writes,
                           long writeBytes, long transferTime,
                           int partitionCount) {
    }

    public record PartitionInfo(String identification, String name, String type, String uuid, long size, int major,
                                int minor, String mountPoint) {
    }

    public record FileStoreInfo(String name, String volume, String label, String mount, String description, String type,
                                String options, String uuid, long totalSpace,
                                long usableSpace, long freeSpace, long usedSpace, double usagePercent) {
    }

    public record DiskIoStats(String name, long reads, long readBytes, long writes, long writeBytes,
                              long transferTime) {
    }

    public record DiskSnapshot(List<DiskInfo> disks, List<PartitionInfo> partitions, List<FileStoreInfo> fileStores,
                               List<DiskIoStats> ioStats,
                               double usagePercent, long timestamp) {
    }

    public record NetworkInterfaceInfo(String name, String displayName, String macAddress, List<String> ipv4Addresses,
                                       List<String> ipv6Addresses, long speed,
                                       long bytesReceived, long bytesSent, long packetsReceived, long packetsSent) {
    }

    public record NetworkStats(String name, long bytesReceived, long bytesSent, long packetsReceived, long packetsSent,
                               long inErrors, long outErrors, long speed) {
    }

    public record NetworkThroughput(long bytesReceivedPerSecond, long bytesSentPerSecond, long timestamp) {
    }

    public record TcpStats(TcpStatsInfo ipv4, TcpStatsInfo ipv6) {
    }

    public record TcpStatsInfo(long connectionsEstablished, long connectionsActive, long connectionsPassive,
                               long connectionFailures, long connectionsReset,
                               long segmentsSent, long segmentsReceived, long segmentsRetransmitted, long inErrors,
                               long outResets) {
    }

    public record UdpStats(UdpStatsInfo ipv4, UdpStatsInfo ipv6) {
    }

    public record UdpStatsInfo(long datagramsSent, long datagramsReceived, long datagramsNoPort,
                               long datagramsReceivedErrors) {
    }

    public record NetworkSnapshot(List<NetworkInterfaceInfo> interfaces, List<NetworkStats> stats, TcpStats tcpStats,
                                  UdpStats udpStats, String hostname,
                                  String gateway, List<String> dnsServers, long timestamp) {
    }

    public record ProcessInfo(int pid, int parentPid, String name, String path, String commandLine, String user,
                              String state, int threadCount, int priority,
                              long virtualSize, long residentSetSize, long kernelTime, long userTime, long upTime,
                              long startTime, double cpuLoadPercent) {
    }

    public record ProcessSnapshot(int processCount, int threadCount, List<ProcessInfo> topCpuProcesses,
                                  List<ProcessInfo> topMemoryProcesses, long timestamp) {
    }

    public record ServiceInfo(String name, int processId, String state) {
    }

    public record ComputerSystemInfo(String manufacturer, String model, String serialNumber, String hardwareUuid,
                                     FirmwareInfo firmware, BaseboardInfo baseboard) {
    }

    public record FirmwareInfo(String manufacturer, String name, String description, String version,
                               String releaseDate) {
    }

    public record BaseboardInfo(String manufacturer, String model, String version, String serialNumber) {
    }

    public record HardwareSnapshot(ComputerSystemInfo computerSystem, CpuInfo cpu, List<DiskInfo> disks,
                                   List<GraphicsCardInfo> graphicsCards,
                                   List<UsbDeviceInfo> usbDevices, List<SoundCardInfo> soundCards, SensorsInfo sensors,
                                   long timestamp) {
    }

    public record SensorsInfo(double cpuTemperature, int[] fanSpeeds, double cpuVoltage) {
    }

    public record PowerSourceInfo(String name, String deviceName, double remainingCapacityPercent, boolean powerOnLine,
                                  boolean charging, boolean discharging,
                                  double powerUsageRate, double timeRemainingEstimated) {
    }

    public record BatteryInfo(String name, String deviceName, double remainingCapacityPercent, boolean powerOnLine,
                              boolean charging, boolean discharging,
                              double timeRemainingEstimated) {
    }

    public record SensorsSnapshot(SensorsInfo sensors, List<PowerSourceInfo> powerSources, long timestamp) {
    }

    public record GraphicsCardInfo(String name, String deviceId, String vendor, String versionInfo, long vram) {
    }

    public record DisplayInfo(int edidLength, String edidHex) {
    }

    public record UsbDeviceInfo(String name, String vendor, String vendorId, String productId, String serialNumber,
                                String uniqueDeviceId,
                                List<UsbDeviceInfo> connectedDevices) {
    }

    public record SoundCardInfo(String name, String codec, String driverVersion) {
    }

    public record DeviceSnapshot(List<GraphicsCardInfo> graphicsCards, List<DisplayInfo> displays,
                                 List<UsbDeviceInfo> usbDevices, List<SoundCardInfo> soundCards,
                                 long timestamp) {
    }

    public record JvmInfo(String javaVersion, String javaVendor, String javaHome, String vmName, String vmVendor,
                          String vmVersion, long startTime, long uptime,
                          List<String> inputArguments) {
    }

    public record JvmMemoryInfo(long heapInit, long heapUsed, long heapCommitted, long heapMax, long nonHeapInit,
                                long nonHeapUsed, long nonHeapCommitted,
                                long nonHeapMax, int availableProcessors) {
    }

    public record JvmThreadInfo(int threadCount, int daemonThreadCount, int peakThreadCount,
                                long totalStartedThreadCount) {
    }

    public record JvmGcInfo(long totalCollectionCount, long totalCollectionTime, List<GcCollectorInfo> collectors) {
    }

    public record GcCollectorInfo(String name, long collectionCount, long collectionTime,
                                  List<String> memoryPoolNames) {
    }

    public record JvmSnapshot(JvmInfo info, JvmMemoryInfo memory, JvmThreadInfo thread, JvmGcInfo gc,
                              double currentProcessCpuLoadPercent,
                              long currentProcessMemoryUsage, long timestamp) {
    }

    public record ContainerInfo(boolean containerEnvironment, boolean virtualMachine, long cgroupMemoryLimit,
                                double cgroupCpuLimit, long effectiveMemoryLimit,
                                double effectiveCpuLimit) {
    }

    public record RuntimeEnvironmentSnapshot(ContainerInfo containerInfo, JvmInfo jvmInfo, ProcessInfo currentProcess,
                                             long timestamp) {
    }

    public record ResourceThreshold(double cpuThreshold, double memoryThreshold, double diskThreshold, String mount) {
    }

    public record HealthRule(double cpuThreshold, double memoryThreshold, double diskThreshold, String mount,
                             boolean checkNetwork) {
    }

    public record WarningItem(String level, String item, String message, double value, double threshold) {
    }

    public record ThresholdCheckResult(boolean ok, double cpuUsagePercent, double memoryUsagePercent,
                                       double diskUsagePercent, List<WarningItem> items) {
    }

    public record HealthCheckResult(boolean healthy, List<WarningItem> items, long timestamp) {
    }

    public record ResourceSnapshot(CpuSnapshot cpu, MemorySnapshot memory, DiskSnapshot disk, NetworkSnapshot network,
                                   long timestamp) {
    }

    public record RuntimeSnapshot(JvmSnapshot jvm, ProcessInfo currentProcess, ContainerInfo containerInfo,
                                  long timestamp) {
    }

    public record AllSnapshot(SystemSnapshot system, HardwareSnapshot hardware, SensorsSnapshot sensors,
                              DeviceSnapshot devices,
                              RuntimeEnvironmentSnapshot runtimeEnvironment, long timestamp) {
    }
}
