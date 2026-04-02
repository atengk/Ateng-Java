package local.ateng.java.hutool;

import local.ateng.java.hutool.util.MachineFingerprintUtil;

public class MachineFingerprintTest {

    public static void main(String[] args) {
        String cpuId = MachineFingerprintUtil.getCpuId();
        System.out.println("CPU ID      : " + cpuId);
        String mac = MachineFingerprintUtil.getMainMac();
        System.out.println("MAC         : " + mac);
        String disk = MachineFingerprintUtil.getDiskSerial();
        System.out.println("DISK SERIAL : " + disk);
        String host = MachineFingerprintUtil.getHostName();
        System.out.println("HOST NAME   : " + host);

        String fingerprint = MachineFingerprintUtil.getFingerprint();
        System.out.println("FINGERPRINT : " + fingerprint);
    }

}
