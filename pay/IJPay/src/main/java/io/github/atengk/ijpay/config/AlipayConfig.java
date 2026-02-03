package io.github.atengk.ijpay.config;

import com.ijpay.alipay.AliPayApiConfig;
import com.ijpay.alipay.AliPayApiConfigKit;
import lombok.Data;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 支付宝配置（IJPay + 证书模式）
 *
 * <p>
 * 负责：
 * <ul>
 *     <li>读取 application.yml 中的支付宝配置</li>
 *     <li>初始化 IJPay 的 AliPayApiConfig</li>
 * </ul>
 *
 * @author 孔余
 * @since 2026-02-03
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    /** 应用 APP_ID */
    private String appId;

    /** 应用私钥 */
    private String privateKey;

    /** 应用公钥证书路径（classpath） */
    private String appCertPath;

    /** 支付宝公钥证书路径（classpath） */
    private String alipayCertPath;

    /** 支付宝公钥证书真实文件路径（用于验签） */
    private String realAlipayCertPath;

    /** 支付宝根证书路径（classpath） */
    private String alipayRootCertPath;

    /** 支付宝网关地址 */
    private String serverUrl;

    /** 异步通知地址 */
    private String notifyUrl;

    /** 同步返回地址 */
    private String returnUrl;

    /** 编码格式 */
    private String charset;

    /** 签名类型（RSA2） */
    private String signType;

    /**
     * 应用启动完成后初始化 IJPay 配置
     *
     * <p>
     * 证书模式下，证书必须是「真实文件路径」，
     * 因此需要先从 classpath 拷贝到临时文件。
     * </p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initAliPayApiConfig() throws Exception {

        // 将证书从 classpath 拷贝到临时文件
        String appCert = copyCertToTemp(appCertPath);
        String alipayCert = copyCertToTemp(alipayCertPath);
        String rootCert = copyCertToTemp(alipayRootCertPath);

        // 保存支付宝公钥证书真实路径（用于回调验签）
        this.realAlipayCertPath = alipayCert;

        // 构建 IJPay 支付宝证书模式配置
        AliPayApiConfig aliPayApiConfig = AliPayApiConfig.builder()
                .setAppId(appId)
                .setPrivateKey(privateKey)
                .setAppCertPath(appCert)
                .setAliPayCertPath(alipayCert)
                .setAliPayRootCertPath(rootCert)
                .setServiceUrl(serverUrl)
                .setCharset(charset)
                .setSignType(signType)
                .buildByCert();

        // 注册配置到 IJPay
        AliPayApiConfigKit.putApiConfig(aliPayApiConfig);
        AliPayApiConfigKit.setThreadLocalAliPayApiConfig(aliPayApiConfig);
    }

    /**
     * 将 classpath 下的证书文件拷贝到临时目录
     *
     * @param classpathLocation classpath 路径
     * @return 临时文件的绝对路径
     */
    private String copyCertToTemp(String classpathLocation) throws Exception {

        // 提取证书文件名
        String fileName = classpathLocation.substring(
                classpathLocation.lastIndexOf('/') + 1
        );

        // 创建临时证书文件
        File tempFile = File.createTempFile(fileName, ".crt");
        tempFile.deleteOnExit();

        // 拷贝证书内容
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream();
             OutputStream out = new FileOutputStream(tempFile)) {
            in.transferTo(out);
        }

        return tempFile.getAbsolutePath();
    }
}
