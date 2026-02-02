package io.github.atengk.alipay.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.*;
import com.alipay.api.response.*;
import io.github.atengk.alipay.config.AlipayConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pay/alipay")
@RequiredArgsConstructor
public class AlipayController {

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;

    /** 1️⃣ 创建支付（扫码） */
    @PostMapping("/create")
    public String create() throws Exception {

        String outTradeNo = "ORDER_" + System.currentTimeMillis();

        AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();

        JSONObject biz = new JSONObject();
        biz.put("out_trade_no", outTradeNo);
        biz.put("total_amount", "0.01");
        biz.put("subject", "测试订单");
        biz.put("timeout_express", "5m");

        request.setBizContent(biz.toJSONString());
        request.setNotifyUrl(alipayConfig.getNotifyUrl());

        AlipayTradePrecreateResponse response =
                alipayClient.certificateExecute(request);

        if (response.isSuccess()) {
            return response.getQrCode();
        }

        throw new RuntimeException(response.getSubMsg());
    }

    /** 2️⃣ 支付宝异步通知 */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) throws Exception {

        Map<String, String> params = new HashMap<>();
        request.getParameterMap()
                .forEach((k, v) -> params.put(k, v[0]));

        boolean signVerified = AlipaySignature.rsaCertCheckV1(
                params,
                alipayConfig.getRealAlipayCertPath(),
                alipayConfig.getCharset(),
                alipayConfig.getSignType()
        );

        if (!signVerified) {
            return "failure";
        }

        if ("TRADE_SUCCESS".equals(params.get("trade_status"))) {
            log.info("支付成功 outTradeNo={}", params.get("out_trade_no"));
            // 更新订单状态（幂等）
        }

        return "success";
    }

    /** 3️⃣ 查询订单状态 */
    @GetMapping("/query")
    public String query(@RequestParam String outTradeNo) throws Exception {

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

        JSONObject biz = new JSONObject();
        biz.put("out_trade_no", outTradeNo);
        request.setBizContent(biz.toJSONString());

        AlipayTradeQueryResponse response =
                alipayClient.certificateExecute(request);

        return JSON.toJSONString(response);
    }

    /** 4️⃣ 关闭订单 */
    @PostMapping("/close")
    public String close(@RequestParam String outTradeNo) throws Exception {

        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();

        JSONObject biz = new JSONObject();
        biz.put("out_trade_no", outTradeNo);
        request.setBizContent(biz.toJSONString());

        AlipayTradeCloseResponse response =
                alipayClient.certificateExecute(request);

        return JSON.toJSONString(response);
    }

    /** 5️⃣ 退款 */
    @PostMapping("/refund")
    public String refund(
            @RequestParam String outTradeNo,
            @RequestParam String amount
    ) throws Exception {

        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

        JSONObject biz = new JSONObject();
        biz.put("out_trade_no", outTradeNo);
        biz.put("refund_amount", amount);
        request.setBizContent(biz.toJSONString());

        AlipayTradeRefundResponse response =
                alipayClient.certificateExecute(request);

        return JSON.toJSONString(response);
    }

    /** 6️⃣ 查询退款 */
    @GetMapping("/refund/query")
    public String refundQuery(
            @RequestParam String outTradeNo,
            @RequestParam String refundRequestNo
    ) throws Exception {

        AlipayTradeFastpayRefundQueryRequest request =
                new AlipayTradeFastpayRefundQueryRequest();

        JSONObject biz = new JSONObject();
        biz.put("out_trade_no", outTradeNo);
        biz.put("out_request_no", refundRequestNo);
        request.setBizContent(biz.toJSONString());

        AlipayTradeFastpayRefundQueryResponse response =
                alipayClient.certificateExecute(request);

        return JSON.toJSONString(response);
    }

    /** 7️⃣ 对账 / 补偿（主动同步） */
    @PostMapping("/sync")
    public String sync(@RequestParam String outTradeNo) throws Exception {

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

        JSONObject biz = new JSONObject();
        biz.put("out_trade_no", outTradeNo);
        request.setBizContent(biz.toJSONString());

        AlipayTradeQueryResponse response =
                alipayClient.certificateExecute(request);

        if (response.isSuccess()
                && "TRADE_SUCCESS".equals(response.getTradeStatus())) {
            log.info("补偿成功 outTradeNo={}", outTradeNo);
        }

        return JSON.toJSONString(response);
    }
}
