package io.github.atengk.ijpay.controller;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alipay.api.domain.*;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.ijpay.alipay.AliPayApi;
import io.github.atengk.ijpay.config.AlipayConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付 Controller（IJPay + 证书模式）
 *
 * <p>
 * 主要职责：
 * <ul>
 *     <li>发起支付宝当面付（扫码支付）</li>
 *     <li>接收并处理支付宝异步通知</li>
 * </ul>
 *
 * <p>
 * 说明：
 * <ul>
 *     <li>支付能力由 IJPay 提供，内部通过 AliPayApiConfigKit 获取证书配置</li>
 *     <li>本类只负责「协议交互」，不建议在此直接写复杂业务逻辑</li>
 * </ul>
 *
 * @author 孔余
 * @since 2026-02-03
 */
@Slf4j
@RestController
@RequestMapping("/api/pay/alipay")
@RequiredArgsConstructor
public class AlipayController {

    /**
     * 支付宝配置（证书模式）
     */
    private final AlipayConfig alipayConfig;

    /**
     * 创建支付宝扫码支付订单（当面付）
     *
     * <p>
     * 返回二维码字符串，前端自行生成二维码图片。
     * </p>
     */
    @PostMapping("/tradePrecreatePay")
    public String tradePrecreatePay() throws Exception {

        // 构建扫码支付业务模型
        AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
        model.setSubject("测试订单");                       // 订单标题
        model.setTotalAmount("99.00");                    // 订单金额（单位：元）
        model.setTimeoutExpress("5m");                     // 超时时间
        model.setOutTradeNo("ORDER_" + IdUtil.getSnowflakeNextIdStr()); // 商户订单号

        // 调用 IJPay 封装的当面付接口
        return AliPayApi
                .tradePrecreatePayToResponse(model, alipayConfig.getNotifyUrl())
                .getQrCode();
    }

    /**
     * 扫码支付（预下单 / 当面付）
     *
     * <p>
     * 返回支付宝二维码地址，
     * 前端负责生成二维码展示
     * </p>
     */
    @PostMapping("/qr")
    public String qrPay() {
        try {
            // 1. 构建预下单模型
            AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
            model.setSubject("扫码支付订单");
            model.setTotalAmount("0.01");
            model.setTimeoutExpress("5m");
            model.setOutTradeNo("ORDER_" + IdUtil.getSnowflakeNextIdStr());

            // 可选：门店号
            model.setStoreId("STORE_001");

            // 2. 调用 IJPay 预下单接口
            AlipayTradePrecreateResponse alipayTradePrecreateResponse = AliPayApi
                    .tradePrecreatePayToResponse(model, alipayConfig.getNotifyUrl());

            // 3. 解析二维码地址
            String qrCode = alipayTradePrecreateResponse.getQrCode();

            // 4. 返回给前端
            return qrCode;

        } catch (Exception e) {
            log.error("[ALIPAY-QR] precreate error", e);
            return null;
        }
    }

    /**
     * 条码 / 声波支付（付款码支付）
     *
     * <p>
     * 商户扫用户付款码，
     * 支付结果同步返回
     * </p>
     */
    @PostMapping("/tradePay")
    public String tradePay(
            @RequestParam("authCode") String authCode,
            @RequestParam("scene") String scene
    ) {

        try {
            // bar_code / wave_code
            String subject;
            if ("bar_code".equals(scene)) {
                subject = "条码支付订单";
            } else if ("wave_code".equals(scene)) {
                subject = "声波支付订单";
            } else {
                return "非法支付场景";
            }

            // 1. 构建支付模型
            AlipayTradePayModel model = new AlipayTradePayModel();
            model.setOutTradeNo("ORDER_" + IdUtil.getSnowflakeNextIdStr());
            model.setSubject(subject);
            model.setTotalAmount("99.99");
            model.setAuthCode(authCode); // 用户付款码
            model.setScene(scene);       // bar_code / wave_code

            // 2. 发起支付
            String body = AliPayApi
                    .tradePayToResponse(model, alipayConfig.getNotifyUrl())
                    .getBody();

            // 3. 返回同步结果
            return body;

        } catch (Exception e) {
            log.error("[ALIPAY-TRADE-PAY] error", e);
            return "付款码支付失败";
        }
    }

    /**
     * PC 网站支付
     *
     * <p>
     * 浏览器访问该接口后，
     * 会直接跳转到支付宝 PC 收银台页面
     * </p>
     */
    @GetMapping("/tradePage")
    public void tradePage(HttpServletResponse response) {

        try {
            // 1. 商户订单号（必须唯一）
            String outTradeNo = "ORDER_" + IdUtil.getSnowflakeNextIdStr();
            log.info("[ALIPAY-PC] outTradeNo={}", outTradeNo);

            // 2. 构建 PC 网站支付模型
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(outTradeNo);
            model.setProductCode("FAST_INSTANT_TRADE_PAY");
            model.setTotalAmount("99.00");
            model.setSubject("PC 网站支付测试订单");
            model.setBody("IJPay + SpringBoot3 PC 网站支付测试");

            // 可选：回传参数（异步通知会原样返回）
            model.setPassbackParams("pc_pay_test");

            // 3. 调用 IJPay PC 网站支付
            //    内部会输出 HTML form 并自动提交
            AliPayApi.tradePage(
                    response,
                    model,
                    alipayConfig.getNotifyUrl(),
                    alipayConfig.getReturnUrl()
            );

        } catch (Exception e) {
            log.error("[ALIPAY-PC] pay error", e);
        }
    }

    /**
     * WAP 支付（手机网页）
     *
     * <p>
     * 适用于手机浏览器 / WebView，
     * 会跳转到支付宝 H5 收银台
     * </p>
     */
    @GetMapping("/wap")
    public void wapPay(HttpServletResponse response) {

        try {
            // 1. 商户订单号
            String outTradeNo = "ORDER_" + IdUtil.getSnowflakeNextIdStr();
            log.info("[ALIPAY-WAP] outTradeNo={}", outTradeNo);

            // 2. 构建 WAP 支付模型
            AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
            model.setOutTradeNo(outTradeNo);
            model.setTotalAmount("99.00");
            model.setSubject("WAP 支付测试订单");
            model.setBody("IJPay + SpringBoot3 WAP 支付测试");

            // WAP 支付固定值
            model.setProductCode("QUICK_WAP_WAY");

            // 可选：超时时间
            model.setTimeoutExpress("5m");

            // 3. 调用 IJPay WAP 支付
            //    返回 HTML form 并自动提交
            AliPayApi.wapPay(
                    response,
                    model,
                    alipayConfig.getNotifyUrl(),
                    alipayConfig.getReturnUrl()
            );

        } catch (Exception e) {
            log.error("[ALIPAY-WAP] pay error", e);
        }
    }

    /**
     * APP 支付
     *
     * <p>
     * 返回给 App 的是 orderString，
     * 由 App 端调用支付宝 SDK 发起支付
     * </p>
     */
    @PostMapping("/app")
    public String appPay() {

        try {
            // 1. 构建 APP 支付模型
            AlipayTradeAppPayModel model = new AlipayTradeAppPayModel();
            model.setBody("IJPay APP 支付测试");
            model.setSubject("APP 支付订单");
            model.setOutTradeNo("ORDER_" + IdUtil.getSnowflakeNextIdStr());
            model.setTimeoutExpress("30m");
            model.setTotalAmount("0.01");

            // APP 支付固定值
            model.setProductCode("QUICK_MSECURITY_PAY");

            // 2. 调用 IJPay，生成 orderString
            String orderString = AliPayApi
                    .appPayToResponse(model, alipayConfig.getNotifyUrl())
                    .getBody();

            // 3. 返回给 App
            return orderString;

        } catch (Exception e) {
            log.error("[ALIPAY-APP] pay error", e);
            return null;
        }
    }

    /**
     * 支付宝异步通知回调接口
     *
     * <p>
     * 重要说明：
     * <ul>
     *     <li>必须使用证书验签（rsaCertCheckV1）</li>
     *     <li>返回值只能是 "success" 或 "failure"</li>
     *     <li>接口中不能有任何多余输出</li>
     * </ul>
     * </p>
     */
    @GetMapping("/notify")
    public String notify(HttpServletRequest request) throws Exception {

        log.info("[ALIPAY-NOTIFY] receive");

        // 1. 将请求参数转换为 Map<String, String>
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach(
                (key, value) -> params.put(key, value[0])
        );

        log.info("[ALIPAY-NOTIFY] params={}", JSON.toJSONString(params));

        // 2. 使用支付宝公钥证书进行验签（证书模式）
        boolean signVerified = AlipaySignature.rsaCertCheckV1(
                params,
                alipayConfig.getRealAlipayCertPath(), // 支付宝公钥证书真实路径
                alipayConfig.getCharset(),
                alipayConfig.getSignType()
        );

        log.info("[ALIPAY-NOTIFY] signVerified={}", signVerified);

        // 验签失败，直接返回 failure
        if (!signVerified) {
            log.warn("[ALIPAY-NOTIFY] sign verify fail");
            return "failure";
        }

        // 3. 解析核心业务参数
        String tradeStatus = params.get("trade_status");
        String outTradeNo = params.get("out_trade_no");
        String totalAmount = params.get("total_amount");
        String tradeNo = params.get("trade_no");

        log.info(
                "[ALIPAY-BIZ] tradeStatus={}, outTradeNo={}, tradeNo={}, totalAmount={}",
                tradeStatus, outTradeNo, tradeNo, totalAmount
        );

        // 4. 只处理成功状态（实际业务中需要做幂等校验）
        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            // TODO:
            // 1. 校验订单是否存在
            // 2. 校验金额是否一致
            // 3. 校验订单是否已处理（幂等）
            // 4. 更新订单状态
            log.info("[ALIPAY-BIZ] success outTradeNo={}", outTradeNo);
        } else {
            // 其他状态直接忽略，但仍需返回 success，避免支付宝重复通知
            log.warn("[ALIPAY-BIZ] ignore tradeStatus={}, outTradeNo={}",
                    tradeStatus, outTradeNo);
        }

        log.info("[ALIPAY-NOTIFY] return success");
        return "success";
    }
}
