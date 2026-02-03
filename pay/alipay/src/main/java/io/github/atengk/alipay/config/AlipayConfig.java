package io.github.atengk.alipay.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.CertAlipayRequest;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 支付宝证书模式配置类
 *
 * <p>
 * 该配置基于支付宝开放平台「证书模式」进行初始化，
 * 通过 CertAlipayRequest 构建 AlipayClient 实例。
 * </p>
 *
 * <p>
 * 由于支付宝 SDK 仅支持读取文件系统中的证书路径，
 * 因此 classpath 下的证书文件会在启动时复制到系统临时目录，
 * 再将生成的绝对路径交由 SDK 使用。
 * </p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    /**
     * 支付宝应用 APP_ID
     */
    private String appId;

    /**
     * 商户应用私钥
     *
     * <p>
     * 用于请求加签，必须与支付宝后台配置的应用公钥成对
     * </p>
     */
    private String privateKey;

    /**
     * 应用公钥证书路径（classpath 相对路径）
     */
    private String appCertPath;

    /**
     * 支付宝公钥证书路径（classpath 相对路径）
     */
    private String alipayCertPath;

    /**
     * 支付宝公钥证书路径
     */
    private String realAlipayCertPath;

    /**
     * 支付宝根证书路径（classpath 相对路径）
     */
    private String alipayRootCertPath;

    /**
     * 支付宝网关地址
     *
     * <p>
     * 沙箱环境与正式环境不同
     * </p>
     */
    private String gatewayUrl;

    /**
     * 支付宝异步通知地址
     */
    private String notifyUrl;

    /**
     * 支付宝支付成功跳转页面地址
     */
    private String returnUrl;

    /**
     * 请求与响应字符集
     */
    private String charset;

    /**
     * 签名算法类型
     *
     * <p>
     * 证书模式下通常使用 RSA2
     * </p>
     */
    private String signType;

    /**
     * 初始化支付宝客户端（证书模式）
     *
     * <p>
     * 启动时会将 classpath 下的证书复制到系统临时目录，
     * 并使用临时文件的绝对路径构建 CertAlipayRequest。
     * </p>
     *
     * @return AlipayClient 实例
     * @throws Exception 证书读取或初始化异常
     */
    @Bean
    public AlipayClient alipayClient() throws Exception {

        String appCert = copyCertToTemp(appCertPath);
        String alipayCert = copyCertToTemp(alipayCertPath);
        String rootCert = copyCertToTemp(alipayRootCertPath);

        this.realAlipayCertPath = alipayCert;

        CertAlipayRequest certRequest = new CertAlipayRequest();
        certRequest.setServerUrl(gatewayUrl);
        certRequest.setAppId(appId);
        certRequest.setPrivateKey(privateKey);
        certRequest.setFormat("json");
        certRequest.setCharset(charset);
        certRequest.setSignType(signType);
        certRequest.setCertPath(appCert);
        certRequest.setAlipayPublicCertPath(alipayCert);
        certRequest.setRootCertPath(rootCert);

        return new DefaultAlipayClient(certRequest);
    }

    /**
     * 将 classpath 下的证书文件复制到系统临时目录
     *
     * <p>
     * 支付宝 SDK 证书模式仅支持通过文件系统路径读取证书，
     * 无法直接使用 classpath 资源或 InputStream，
     * 因此需要在运行时生成临时文件。
     * </p>
     *
     * @param classpathLocation classpath 相对路径
     * @return 临时证书文件的绝对路径
     * @throws Exception 证书读取或文件写入异常
     */
    private String copyCertToTemp(String classpathLocation) throws Exception {

        String fileName = classpathLocation.substring(
                classpathLocation.lastIndexOf('/') + 1
        );

        File tempFile = File.createTempFile(fileName, ".crt");
        tempFile.deleteOnExit();

        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream();
             OutputStream out = new FileOutputStream(tempFile)) {
            in.transferTo(out);
        }

        return tempFile.getAbsolutePath();
    }

}