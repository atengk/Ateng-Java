package local.ateng.java.hutool;

import cn.hutool.system.oshi.CpuInfo;
import cn.hutool.system.oshi.OshiUtil;
import org.junit.jupiter.api.Test;
import oshi.hardware.*;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.*;

public class OshiUtilTests {

    @Test
    public void test(){
        Map<String, Object> systemInfo = collectSystemInfo();
        System.out.println(systemInfo);
    }


    /**
     * 收集完整系统信息
     */
    public Map<String, Object> collectSystemInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("timestamp", System.currentTimeMillis());
        info.put("computer", collectComputerInfo());
        info.put("operating_system", collectOsInfo());
        info.put("cpu", collectCpuInfo());
        info.put("memory", collectMemoryInfo());
        info.put("disk", collectDiskInfo());
        info.put("network", collectNetworkInfo());
        info.put("process", collectProcessInfo());

        return info;
    }

    /**
     * 收集计算机信息
     */
    private Map<String, Object> collectComputerInfo() {
        Map<String, Object> info = new HashMap<>();

        ComputerSystem system = OshiUtil.getSystem();
        info.put("manufacturer", system.getManufacturer());
        info.put("model", system.getModel());
        info.put("serial_number", system.getSerialNumber());
        info.put("uuid", system.getHardwareUUID());

        Firmware firmware = system.getFirmware();
        info.put("firmware", Map.of(
                "name", firmware.getName(),
                "version", firmware.getVersion(),
                "release_date", firmware.getReleaseDate()
        ));

        Baseboard baseboard = system.getBaseboard();
        info.put("baseboard", Map.of(
                "manufacturer", baseboard.getManufacturer(),
                "model", baseboard.getModel(),
                "serial_number", baseboard.getSerialNumber()
        ));

        return info;
    }

    /**
     * 收集操作系统信息
     */
    private Map<String, Object> collectOsInfo() {
        Map<String, Object> info = new HashMap<>();

        OperatingSystem os = OshiUtil.getOs();
        info.put("family", os.getFamily());
        info.put("manufacturer", os.getManufacturer());
        info.put("version", os.getVersionInfo().toString());
        info.put("bitness", os.getBitness());
        info.put("boot_time", new Date(os.getSystemBootTime() * 1000));
        info.put("uptime_seconds", os.getSystemUptime());
        info.put("process_count", os.getProcessCount());
        info.put("thread_count", os.getThreadCount());

        return info;
    }

    /**
     * 收集 CPU 信息
     */
    private Map<String, Object> collectCpuInfo() {
        Map<String, Object> info = new HashMap<>();

        CentralProcessor processor = OshiUtil.getProcessor();
        CentralProcessor.ProcessorIdentifier id = processor.getProcessorIdentifier();

        info.put("name", id.getName());
        info.put("vendor", id.getVendor());
        info.put("family", id.getFamily());
        info.put("model", id.getModel());
        info.put("stepping", id.getStepping());
        info.put("microarchitecture", id.getMicroarchitecture());
        info.put("physical_cores", processor.getPhysicalProcessorCount());
        info.put("logical_cores", processor.getLogicalProcessorCount());
        info.put("max_frequency_hz", processor.getMaxFreq());

        CpuInfo cpuInfo = OshiUtil.getCpuInfo();
        info.put("usage", Map.of(
                "total", cpuInfo.getUsed(),
                "system", cpuInfo.getSys(),
                "user", cpuInfo.getUser(),
                "free", cpuInfo.getFree()
        ));

        return info;
    }

    /**
     * 收集内存信息
     */
    private Map<String, Object> collectMemoryInfo() {
        Map<String, Object> info = new HashMap<>();

        GlobalMemory memory = OshiUtil.getMemory();
        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;

        info.put("total_bytes", total);
        info.put("used_bytes", used);
        info.put("available_bytes", available);
        info.put("usage_percent", used * 100.0 / total);

        VirtualMemory virtualMemory = memory.getVirtualMemory();
        info.put("swap", Map.of(
                "total_bytes", virtualMemory.getSwapTotal(),
                "used_bytes", virtualMemory.getSwapUsed(),
                "available_bytes", virtualMemory.getSwapTotal() - virtualMemory.getSwapUsed()
        ));

        return info;
    }

    /**
     * 收集磁盘信息
     */
    private Map<String, Object> collectDiskInfo() {
        Map<String, Object> info = new HashMap<>();

        List<HWDiskStore> diskStores = OshiUtil.getDiskStores();
        List<Map<String, Object>> disks = new ArrayList<>();

        for (HWDiskStore disk : diskStores) {
            Map<String, Object> diskInfo = new HashMap<>();
            diskInfo.put("name", disk.getName());
            diskInfo.put("model", disk.getModel());
            diskInfo.put("serial", disk.getSerial());
            diskInfo.put("size_bytes", disk.getSize());
            diskInfo.put("reads", disk.getReads());
            diskInfo.put("writes", disk.getWrites());
            diskInfo.put("read_bytes", disk.getReadBytes());
            diskInfo.put("write_bytes", disk.getWriteBytes());

            List<Map<String, Object>> partitions = new ArrayList<>();
            for (HWPartition partition : disk.getPartitions()) {
                partitions.add(Map.of(
                        "name", partition.getName(),
                        "mount_point", partition.getMountPoint(),
                        "size_bytes", partition.getSize(),
                        "type", partition.getType()
                ));
            }
            diskInfo.put("partitions", partitions);

            disks.add(diskInfo);
        }

        info.put("disks", disks);
        return info;
    }

    /**
     * 收集网络信息
     */
    private Map<String, Object> collectNetworkInfo() {
        Map<String, Object> info = new HashMap<>();

        List<NetworkIF> networkIFs = OshiUtil.getNetworkIFs();
        List<Map<String, Object>> interfaces = new ArrayList<>();

        for (NetworkIF net : networkIFs) {
            net.updateAttributes();

            Map<String, Object> netInfo = new HashMap<>();
            netInfo.put("name", net.getName());
            netInfo.put("display_name", net.getDisplayName());
            netInfo.put("mac_address", net.getMacaddr());
            netInfo.put("speed_bps", net.getSpeed());
            netInfo.put("mtu", net.getMTU());
            netInfo.put("ipv4_addresses", Arrays.asList(net.getIPv4addr()));
            netInfo.put("ipv6_addresses", Arrays.asList(net.getIPv6addr()));
            netInfo.put("bytes_received", net.getBytesRecv());
            netInfo.put("bytes_sent", net.getBytesSent());
            netInfo.put("packets_received", net.getPacketsRecv());
            netInfo.put("packets_sent", net.getPacketsSent());

            interfaces.add(netInfo);
        }

        info.put("interfaces", interfaces);
        return info;
    }

    /**
     * 收集当前进程信息
     */
    private Map<String, Object> collectProcessInfo() {
        Map<String, Object> info = new HashMap<>();

        OSProcess process = OshiUtil.getCurrentProcess();
        info.put("pid", process.getProcessID());
        info.put("name", process.getName());
        info.put("path", process.getPath());
        info.put("state", process.getState().toString());
        info.put("threads", process.getThreadCount());
        info.put("virtual_memory_bytes", process.getVirtualSize());
        info.put("resident_memory_bytes", process.getResidentSetSize());
        info.put("user_time_ms", process.getUserTime());
        info.put("kernel_time_ms", process.getKernelTime());
        info.put("uptime_ms", process.getUpTime());

        return info;
    }

}
