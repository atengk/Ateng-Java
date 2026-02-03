package io.github.atengk.alipay.controller;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import io.github.atengk.alipay.config.AlipayConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝页面支付控制器
 *
 * <p>
 * 负责：
 * <ul>
 *   <li>创建支付宝页面支付订单（跳转至支付宝收银台）</li>
 *   <li>接收并处理支付宝异步通知回调</li>
 * </ul>
 * </p>
 *
 * <p>
 * 说明：
 * <ul>
 *   <li>支付结果以 {@code notify} 异步通知为准</li>
 *   <li>同步 {@code return_url} 仅用于页面跳转展示</li>
 *   <li>异步通知处理必须保证业务幂等</li>
 * </ul>
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/pay/alipay")
@RequiredArgsConstructor
public class AlipayController {

    /**
     * 支付宝 SDK 客户端
     * <p>
     * 由配置类统一初始化（证书模式 / 密钥模式）
     * </p>
     */
    private final AlipayClient alipayClient;

    /**
     * 支付宝配置（appId、回调地址、证书路径等）
     */
    private final AlipayConfig alipayConfig;

    /**
     * 创建页面支付订单（跳转到支付宝收银台）
     *
     * <p>
     * 该接口会返回一段 HTML form，
     * 浏览器渲染后会自动提交到支付宝网关。
     * </p>
     */
    @GetMapping("/create")
    public void create(HttpServletResponse response) throws Exception {

        // 商户订单号（必须全局唯一）
        String outTradeNo = "ORDER_" + IdUtil.getSnowflakeNextIdStr();

        // 页面支付请求对象
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();

        // 异步通知地址（以此为准处理业务结果）
        request.setNotifyUrl(alipayConfig.getNotifyUrl());

        // 同步跳转地址（仅用于页面跳转展示）
        request.setReturnUrl(alipayConfig.getReturnUrl());

        // 页面支付业务参数（推荐使用 Model，避免签名问题）
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(outTradeNo);
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        model.setTotalAmount("0.01");
        model.setSubject("测试订单");

        request.setBizModel(model);

        log.info("[ALIPAY-CREATE] outTradeNo={}", outTradeNo);

        // 调用 SDK 生成自动提交的 HTML 表单
        String form = alipayClient.pageExecute(request).getBody();

        // 直接将表单写回浏览器
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write(form);
        response.getWriter().flush();
    }

    /**
     * 支付宝异步通知回调
     *
     * <p>
     * 注意：
     * 1. 必须进行验签
     * 2. 必须保证业务处理幂等
     * 3. 处理成功后返回 "success"
     * </p>
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) throws Exception {

        // 将支付宝回调参数转换为 Map
        Map<String, String> params = new HashMap<>();
        request.getParameterMap()
                .forEach((k, v) -> params.put(k, v[0]));

        log.info("[ALIPAY-NOTIFY] params={}", JSON.toJSONString(params));

        // 证书模式验签
        boolean signVerified = AlipaySignature.rsaCertCheckV1(
                params,
                alipayConfig.getRealAlipayCertPath(),
                alipayConfig.getCharset(),
                alipayConfig.getSignType()
        );

        if (!signVerified) {
            log.warn("[ALIPAY-NOTIFY] sign verify fail");
            return "failure";
        }

        // 仅处理支付成功状态
        if ("TRADE_SUCCESS".equals(params.get("trade_status"))) {
            log.info("[ALIPAY-BIZ] pay success outTradeNo={}",
                    params.get("out_trade_no"));

            // TODO 更新订单状态（必须做幂等控制）
        }

        // 返回 success 告知支付宝已正确处理
        return "success";
    }
}
