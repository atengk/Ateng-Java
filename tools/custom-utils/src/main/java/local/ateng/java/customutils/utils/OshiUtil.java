package local.ateng.java.customutils.utils;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.util.concurrent.TimeUnit;

/**
 * OSHI 系统监控工具类
 * <p>
 * 基于 oshi-core 实现的系统信息获取工具，
 *
 * @author Ateng
 * @since 2026-02-12
 */
public final class OshiUtil {

    /**
     * OSHI 系统信息核心对象（全局单例）
     */
    private static final SystemInfo SYSTEM_INFO = new SystemInfo();

    /**
     * 硬件抽象层
     */
    private static final HardwareAbstractionLayer HARDWARE =
            SYSTEM_INFO.getHardware();

    /**
     * 操作系统对象
     */
    private static final OperatingSystem OS =
            SYSTEM_INFO.getOperatingSystem();

    /**
     * CPU 处理器对象（缓存，避免重复获取）
     */
    private static final CentralProcessor PROCESSOR =
            HARDWARE.getProcessor();

    /**
     * 内存对象（缓存）
     */
    private static final GlobalMemory MEMORY =
            HARDWARE.getMemory();

    /**
     * 禁止实例化
     */
    private OshiUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    // ==============================
    //          CPU 相关
    // ==============================

    /**
     * 获取系统 CPU 使用率
     * <p>
     * 注意：
     * OSHI 获取 CPU 使用率必须基于两次采样计算，
     * 默认间隔 1 秒。
     *
     * @return CPU 使用率（0.0 - 1.0）
     */
    public static double getCpuUsage() {
        try {
            long[] prevTicks = PROCESSOR.getSystemCpuLoadTicks();
            TimeUnit.SECONDS.sleep(1);
            return PROCESSOR.getSystemCpuLoadBetweenTicks(prevTicks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0D;
        } catch (Exception e) {
            return 0D;
        }
    }

    /**
     * 获取 CPU 使用率（百分比形式）
     *
     * @return 百分比（0-100）
     */
    public static double getCpuUsagePercent() {
        return getCpuUsage() * 100;
    }

    /**
     * 获取 CPU 核心数（逻辑核心）
     *
     * @return 核心数
     */
    public static int getCpuLogicalCount() {
        return PROCESSOR.getLogicalProcessorCount();
    }

    /**
     * 获取 CPU 物理核心数
     *
     * @return 物理核心数
     */
    public static int getCpuPhysicalCount() {
        return PROCESSOR.getPhysicalProcessorCount();
    }

    /**
     * 获取 CPU 型号
     *
     * @return CPU 型号字符串
     */
    public static String getCpuModel() {
        return PROCESSOR.getProcessorIdentifier().getName();
    }

    // ==============================
    //           内存相关
    // ==============================

    /**
     * 获取系统总内存（字节）
     *
     * @return 总内存
     */
    public static long getTotalMemory() {
        return MEMORY.getTotal();
    }

    /**
     * 获取系统可用内存（字节）
     *
     * @return 可用内存
     */
    public static long getAvailableMemory() {
        return MEMORY.getAvailable();
    }

    /**
     * 获取系统已用内存（字节）
     *
     * @return 已用内存
     */
    public static long getUsedMemory() {
        return getTotalMemory() - getAvailableMemory();
    }

    /**
     * 获取系统内存使用率
     *
     * @return 使用率（0.0 - 1.0）
     */
    public static double getMemoryUsage() {
        long total = getTotalMemory();
        if (total == 0) {
            return 0D;
        }
        return (double) getUsedMemory() / total;
    }

    /**
     * 获取系统内存使用率（百分比）
     *
     * @return 百分比（0 - 100）
     */
    public static double getMemoryUsagePercent() {
        return getMemoryUsage() * 100;
    }

    // ==============================
    //           JVM 内存相关
    // ==============================

    /**
     * 获取 JVM 最大可用内存（字节）
     *
     * @return 最大内存
     */
    public static long getJvmMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    /**
     * 获取 JVM 当前已分配内存（字节）
     *
     * @return 已分配内存
     */
    public static long getJvmTotalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    /**
     * 获取 JVM 当前空闲内存（字节）
     *
     * @return 空闲内存
     */
    public static long getJvmFreeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    /**
     * 获取 JVM 实际已使用内存（字节）
     *
     * @return 已使用内存
     */
    public static long getJvmUsedMemory() {
        return getJvmTotalMemory() - getJvmFreeMemory();
    }

    /**
     * 获取 JVM 内存使用率
     *
     * @return 使用率（0.0 - 1.0）
     */
    public static double getJvmMemoryUsage() {
        long max = getJvmMaxMemory();
        if (max == 0) {
            return 0D;
        }
        return (double) getJvmUsedMemory() / max;
    }

    /**
     * 获取 JVM 内存使用率（百分比）
     *
     * @return 百分比
     */
    public static double getJvmMemoryUsagePercent() {
        return getJvmMemoryUsage() * 100;
    }

    // ==============================
    //         字节格式化
    // ==============================

    /**
     * 将字节数格式化为可读字符串
     * <p>
     * 示例：
     * 1024 -> 1 KB
     * 1048576 -> 1 MB
     *
     * @param bytes 字节数
     * @return 格式化字符串
     */
    public static String formatBytes(long bytes) {
        if (bytes < 0) {
            return "0 B";
        }

        final long KB = 1024L;
        final long MB = KB * 1024;
        final long GB = MB * 1024;
        final long TB = GB * 1024;

        if (bytes < KB) {
            return bytes + " B";
        } else if (bytes < MB) {
            return String.format("%.2f KB", bytes / (double) KB);
        } else if (bytes < GB) {
            return String.format("%.2f MB", bytes / (double) MB);
        } else if (bytes < TB) {
            return String.format("%.2f GB", bytes / (double) GB);
        } else {
            return String.format("%.2f TB", bytes / (double) TB);
        }
    }

    // ==============================
    //         磁盘信息 DTO
    // ==============================

    /**
     * 磁盘信息对象
     */
    public static final class DiskInfo {

        /**
         * 盘符 / 挂载点
         */
        private final String mount;

        /**
         * 文件系统类型
         */
        private final String type;

        /**
         * 总容量（字节）
         */
        private final long total;

        /**
         * 可用容量（字节）
         */
        private final long usable;

        /**
         * 已用容量（字节）
         */
        private final long used;

        /**
         * 使用率（0-1）
         */
        private final double usage;

        private DiskInfo(String mount,
                         String type,
                         long total,
                         long usable) {

            this.mount = mount;
            this.type = type;
            this.total = total;
            this.usable = usable;
            this.used = total - usable;
            this.usage = total == 0 ? 0D : (double) used / total;
        }

        public String getMount() {
            return mount;
        }

        public String getType() {
            return type;
        }

        public long getTotal() {
            return total;
        }

        public long getUsable() {
            return usable;
        }

        public long getUsed() {
            return used;
        }

        public double getUsage() {
            return usage;
        }

        public double getUsagePercent() {
            return usage * 100;
        }

        @Override
        public String toString() {
            return "DiskInfo{" +
                    "mount='" + mount + '\'' +
                    ", type='" + type + '\'' +
                    ", total=" + formatBytes(total) +
                    ", usable=" + formatBytes(usable) +
                    ", used=" + formatBytes(used) +
                    ", usage=" + String.format("%.2f%%", getUsagePercent()) +
                    '}';
        }
    }

    // ==============================
    //         磁盘相关
    // ==============================

    /**
     * 获取所有有效磁盘信息
     * <p>
     * 过滤规则：
     * 1. 总容量为 0 的忽略
     * 2. 挂载点为空忽略
     *
     * @return 磁盘列表
     */
    public static java.util.List<DiskInfo> getDiskInfos() {
        java.util.List<DiskInfo> list = new java.util.ArrayList<>();

        try {
            java.util.List<oshi.software.os.OSFileStore> fileStores =
                    OS.getFileSystem().getFileStores();

            for (oshi.software.os.OSFileStore store : fileStores) {

                long total = store.getTotalSpace();
                long usable = store.getUsableSpace();

                if (total <= 0) {
                    continue;
                }

                String mount = store.getMount();
                if (mount == null || mount.trim().isEmpty()) {
                    continue;
                }

                list.add(new DiskInfo(
                        mount,
                        store.getType(),
                        total,
                        usable
                ));
            }
        } catch (Exception ignored) {
        }

        return list;
    }

    /**
     * 获取根目录磁盘信息
     *
     * @return 根磁盘信息
     */
    public static DiskInfo getRootDisk() {
        java.util.List<DiskInfo> disks = getDiskInfos();
        if (disks.isEmpty()) {
            return null;
        }
        return disks.get(0);
    }

    /**
     * 获取磁盘总容量（所有磁盘累加）
     *
     * @return 总容量
     */
    public static long getTotalDiskSpace() {
        long total = 0L;
        for (DiskInfo disk : getDiskInfos()) {
            total += disk.getTotal();
        }
        return total;
    }

    /**
     * 获取磁盘已使用空间（所有磁盘累加）
     *
     * @return 已用空间
     */
    public static long getUsedDiskSpace() {
        long used = 0L;
        for (DiskInfo disk : getDiskInfos()) {
            used += disk.getUsed();
        }
        return used;
    }

    /**
     * 获取磁盘使用率（所有磁盘平均）
     *
     * @return 使用率（0-1）
     */
    public static double getDiskUsage() {
        long total = getTotalDiskSpace();
        if (total == 0) {
            return 0D;
        }
        return (double) getUsedDiskSpace() / total;
    }

    /**
     * 获取磁盘使用率（百分比）
     *
     * @return 百分比
     */
    public static double getDiskUsagePercent() {
        return getDiskUsage() * 100;
    }

    /**
     * 网络信息对象
     */
    public static final class NetworkInfo {

        /**
         * 网卡名称
         */
        private final String name;

        /**
         * 接收字节
         */
        private final long receivedBytes;

        /**
         * 发送字节
         */
        private final long sentBytes;

        private NetworkInfo(String name,
                            long receivedBytes,
                            long sentBytes) {
            this.name = name;
            this.receivedBytes = receivedBytes;
            this.sentBytes = sentBytes;
        }

        public String getName() {
            return name;
        }

        public long getReceivedBytes() {
            return receivedBytes;
        }

        public long getSentBytes() {
            return sentBytes;
        }

        @Override
        public String toString() {
            return "NetworkInfo{" +
                    "name='" + name + '\'' +
                    ", received=" + formatBytes(receivedBytes) +
                    ", sent=" + formatBytes(sentBytes) +
                    '}';
        }
    }

    /**
     * 上一次网络采样数据缓存
     */
    private static final java.util.Map<String, long[]> NETWORK_PREVIOUS =
            new java.util.concurrent.ConcurrentHashMap<>();

    // ==============================
    //          网络相关
    // ==============================

    /**
     * 获取当前网卡流量增量（单位：字节）
     * <p>
     * 返回的是：
     * 当前采样 - 上次采样
     * <p>
     * 如果是第一次调用，则返回 0
     *
     * @return 网络信息列表
     */
    public static java.util.List<NetworkInfo> getNetworkInfos() {

        java.util.List<NetworkInfo> list = new java.util.ArrayList<>();

        try {
            java.util.List<oshi.hardware.NetworkIF> networks =
                    HARDWARE.getNetworkIFs();

            for (oshi.hardware.NetworkIF net : networks) {

                net.updateAttributes();

                String name = net.getName();

                long received = net.getBytesRecv();
                long sent = net.getBytesSent();

                long[] previous = NETWORK_PREVIOUS.get(name);

                long deltaRecv = 0L;
                long deltaSent = 0L;

                if (previous != null) {
                    deltaRecv = received - previous[0];
                    deltaSent = sent - previous[1];
                }

                NETWORK_PREVIOUS.put(name, new long[]{received, sent});

                list.add(new NetworkInfo(name, deltaRecv, deltaSent));
            }

        } catch (Exception ignored) {
        }

        return list;
    }

    // ==============================
    //         系统状态
    // ==============================

    /**
     * 获取系统启动时间（毫秒）
     *
     * @return 启动时间戳
     */
    public static long getSystemBootTime() {
        return OS.getSystemBootTime() * 1000;
    }

    /**
     * 获取系统运行时长（毫秒）
     *
     * @return 运行时长
     */
    public static long getSystemUptime() {
        return OS.getSystemUptime() * 1000;
    }

    /**
     * 获取当前进程数量
     *
     * @return 进程数
     */
    public static int getProcessCount() {
        return OS.getProcessCount();
    }

    /**
     * 获取当前线程数量
     *
     * @return 线程数
     */
    public static int getThreadCount() {
        return OS.getThreadCount();
    }

    /**
     * 获取系统负载（1分钟平均）
     *
     * @return 负载
     */
    public static double getSystemLoadAverage() {
        double[] load = PROCESSOR.getSystemLoadAverage(1);
        if (load == null || load.length == 0) {
            return 0D;
        }
        return load[0];
    }

    /**
     * CPU 硬件信息
     */
    public static final class CpuHardwareInfo {

        private final String name;
        private final String vendor;
        private final String identifier;
        private final String microArchitecture;
        private final int physicalPackageCount;
        private final int physicalProcessorCount;
        private final int logicalProcessorCount;

        private CpuHardwareInfo() {
            oshi.hardware.CentralProcessor.ProcessorIdentifier pi =
                    PROCESSOR.getProcessorIdentifier();

            this.name = pi.getName();
            this.vendor = pi.getVendor();
            this.identifier = pi.getIdentifier();
            this.microArchitecture = pi.getMicroarchitecture();
            this.physicalPackageCount = PROCESSOR.getPhysicalPackageCount();
            this.physicalProcessorCount = PROCESSOR.getPhysicalProcessorCount();
            this.logicalProcessorCount = PROCESSOR.getLogicalProcessorCount();
        }

        public String getName() {
            return name;
        }

        public String getVendor() {
            return vendor;
        }

        public String getIdentifier() {
            return identifier;
        }

        public String getMicroArchitecture() {
            return microArchitecture;
        }

        public int getPhysicalPackageCount() {
            return physicalPackageCount;
        }

        public int getPhysicalProcessorCount() {
            return physicalProcessorCount;
        }

        public int getLogicalProcessorCount() {
            return logicalProcessorCount;
        }
    }

    /**
     * 获取 CPU 硬件信息
     */
    public static CpuHardwareInfo getCpuHardwareInfo() {
        return new CpuHardwareInfo();
    }

    /**
     * 主板信息
     */
    public static final class BaseboardInfo {

        private final String manufacturer;
        private final String model;
        private final String version;
        private final String serialNumber;

        private BaseboardInfo() {
            oshi.hardware.Baseboard baseboard =
                    HARDWARE.getComputerSystem().getBaseboard();

            this.manufacturer = baseboard.getManufacturer();
            this.model = baseboard.getModel();
            this.version = baseboard.getVersion();
            this.serialNumber = baseboard.getSerialNumber();
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public String getModel() {
            return model;
        }

        public String getVersion() {
            return version;
        }

        public String getSerialNumber() {
            return serialNumber;
        }
    }

    /**
     * BIOS 信息
     */
    public static final class BiosInfo {

        private final String manufacturer;
        private final String version;
        private final String releaseDate;

        private BiosInfo() {
            oshi.hardware.Firmware fw =
                    HARDWARE.getComputerSystem().getFirmware();

            this.manufacturer = fw.getManufacturer();
            this.version = fw.getVersion();
            this.releaseDate = fw.getReleaseDate();
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public String getVersion() {
            return version;
        }

        public String getReleaseDate() {
            return releaseDate;
        }
    }

    public static BaseboardInfo getBaseboardInfo() {
        return new BaseboardInfo();
    }

    public static BiosInfo getBiosInfo() {
        return new BiosInfo();
    }

    /**
     * 物理内存条信息
     */
    public static final class PhysicalMemoryInfo {

        private final String bankLabel;
        private final String manufacturer;
        private final long capacity;
        private final long clockSpeed;

        private PhysicalMemoryInfo(oshi.hardware.PhysicalMemory memory) {
            this.bankLabel = memory.getBankLabel();
            this.manufacturer = memory.getManufacturer();
            this.capacity = memory.getCapacity();
            this.clockSpeed = memory.getClockSpeed();
        }

        public String getBankLabel() {
            return bankLabel;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public long getCapacity() {
            return capacity;
        }

        public long getClockSpeed() {
            return clockSpeed;
        }
    }

    /**
     * 获取所有物理内存条
     */
    public static java.util.List<PhysicalMemoryInfo> getPhysicalMemoryInfos() {
        java.util.List<PhysicalMemoryInfo> list = new java.util.ArrayList<>();
        for (oshi.hardware.PhysicalMemory pm : MEMORY.getPhysicalMemory()) {
            list.add(new PhysicalMemoryInfo(pm));
        }
        return list;
    }

    /**
     * 物理磁盘信息
     */
    public static final class DiskHardwareInfo {

        private final String name;
        private final String model;
        private final long size;
        private final String serial;

        private DiskHardwareInfo(oshi.hardware.HWDiskStore disk) {
            this.name = disk.getName();
            this.model = disk.getModel();
            this.size = disk.getSize();
            this.serial = disk.getSerial();
        }

        public String getName() {
            return name;
        }

        public String getModel() {
            return model;
        }

        public long getSize() {
            return size;
        }

        public String getSerial() {
            return serial;
        }
    }

    /**
     * 获取所有物理磁盘
     */
    public static java.util.List<DiskHardwareInfo> getDiskHardwareInfos() {
        java.util.List<DiskHardwareInfo> list = new java.util.ArrayList<>();
        for (oshi.hardware.HWDiskStore disk : HARDWARE.getDiskStores()) {
            list.add(new DiskHardwareInfo(disk));
        }
        return list;
    }

    /**
     * 传感器信息
     */
    public static final class SensorInfo {

        private final double cpuTemperature;
        private final int[] fanSpeeds;
        private final double cpuVoltage;

        private SensorInfo() {
            oshi.hardware.Sensors sensors = HARDWARE.getSensors();
            this.cpuTemperature = sensors.getCpuTemperature();
            this.fanSpeeds = sensors.getFanSpeeds();
            this.cpuVoltage = sensors.getCpuVoltage();
        }

        public double getCpuTemperature() {
            return cpuTemperature;
        }

        public int[] getFanSpeeds() {
            return fanSpeeds;
        }

        public double getCpuVoltage() {
            return cpuVoltage;
        }
    }

    public static SensorInfo getSensorInfo() {
        return new SensorInfo();
    }

}
