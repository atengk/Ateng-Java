package local.ateng.java.customutils.utils;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HWDiskStore;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.List;
import java.util.Locale;

/**
 * 机器指纹工具类
 *
 * <p>组成：
 * CPU + 主网卡MAC + 磁盘序列号 + 主机名
 *
 * <p>特性：
 * 1. 自动容错（字段缺失不影响整体）
 * 2. 多磁盘合并（避免随机性）
 * 3. 统一标准化（保证hash一致）
 * 4. 内置缓存（避免重复计算）
 *
 * @author
 */
public class MachineFingerprintUtil {

    private static final SystemInfo SYSTEM_INFO = new SystemInfo();

    /**
     * 指纹缓存（进程级）
     */
    private static volatile String CACHE;

    /**
     * 获取机器唯一指纹（SHA-256）
     */
    public static String getFingerprint() {
        if (StrUtil.isNotBlank(CACHE)) {
            return CACHE;
        }

        synchronized (MachineFingerprintUtil.class) {
            if (StrUtil.isNotBlank(CACHE)) {
                return CACHE;
            }

            String cpu = getCpuId();
            String mac = getMainMac();
            String disk = getDiskSerial();
            String host = getHostName();

            String raw = StrUtil.join("|",
                    safe(cpu),
                    safe(mac),
                    safe(disk),
                    safe(host)
            );

            CACHE = DigestUtil.sha256Hex(raw);
            return CACHE;
        }
    }

    /**
     * 获取 CPU ID
     */
    public static String getCpuId() {
        try {
            CentralProcessor processor = SYSTEM_INFO.getHardware().getProcessor();
            String id = processor.getProcessorIdentifier().getProcessorID();
            return normalize(id);
        } catch (Exception e) {
            return StrUtil.EMPTY;
        }
    }

    /**
     * 获取主网卡 MAC（基于出口IP推断）
     */
    public static String getMainMac() {
        try {
            InetAddress address = getRealLocalAddress();
            NetworkInterface ni = NetworkInterface.getByInetAddress(address);

            if (ni == null) return StrUtil.EMPTY;

            byte[] mac = ni.getHardwareAddress();
            if (mac == null) return StrUtil.EMPTY;

            StringBuilder sb = new StringBuilder();
            for (byte b : mac) {
                sb.append(String.format("%02X", b));
            }
            return normalize(sb.toString());

        } catch (Exception e) {
            return StrUtil.EMPTY;
        }
    }

    /**
     * 获取磁盘序列号（多盘合并）
     */
    public static String getDiskSerial() {
        try {
            List<HWDiskStore> disks = SYSTEM_INFO.getHardware().getDiskStores();

            StringBuilder sb = new StringBuilder();

            for (HWDiskStore disk : disks) {
                String serial = disk.getSerial();
                if (isValid(serial)) {
                    sb.append(serial);
                }
            }

            return normalize(sb.toString());

        } catch (Exception e) {
            return StrUtil.EMPTY;
        }
    }

    /**
     * 获取主机名
     */
    public static String getHostName() {
        try {
            return normalize(NetUtil.getLocalhost().getHostName());
        } catch (Exception e) {
            return StrUtil.EMPTY;
        }
    }

    // ===================== 内部方法 =====================

    /**
     * 获取真实出口IP（UDP推断）
     */
    private static InetAddress getRealLocalAddress() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress();
        } catch (Exception e) {
            return NetUtil.getLocalhost();
        }
    }

    /**
     * 判断序列号有效性
     */
    private static boolean isValid(String str) {
        if (StrUtil.isBlank(str)) {
            return false;
        }
        String s = str.trim().toLowerCase(Locale.ROOT);
        return !(s.equals("unknown")
                || s.equals("none")
                || s.equals("00000000"));
    }

    /**
     * 标准化（去空格 + 大写）
     */
    private static String normalize(String str) {
        if (StrUtil.isBlank(str)) {
            return StrUtil.EMPTY;
        }
        return StrUtil.trim(str)
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
    }

    private static String safe(String str) {
        return StrUtil.nullToEmpty(str);
    }
}
