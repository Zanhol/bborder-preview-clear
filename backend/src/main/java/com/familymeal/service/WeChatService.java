package com.familymeal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WeChatService {

    private final RestTemplate rest;
    private final ObjectMapper mapper;
    private final String appId;
    private final String secret;
    private final String templateId;
    private final String templateCookDoneId;
    private final String miniprogramState;

    private String accessToken;
    private long tokenExpiresAt;
    private static final Map<String, String> userOpenIds = new ConcurrentHashMap<>();

    public WeChatService(@Value("${app.wechat.app-id}") String appId,
                         @Value("${app.wechat.secret}") String secret,
                         @Value("${app.wechat.template-id}") String templateId,
                         @Value("${app.wechat.template-cook-done-id:}") String templateCookDoneId,
                         @Value("${app.wechat.miniprogram-state:trial}") String miniprogramState) {
        this.appId = appId;
        this.secret = secret;
        this.templateId = templateId;
        this.templateCookDoneId = templateCookDoneId;
        this.miniprogramState = miniprogramState;
        // Spring Boot 3.x 默认 chunked 传输, 微信要求 Content-Length
        this.rest = new RestTemplate(new org.springframework.http.client.BufferingClientHttpRequestFactory(
                new org.springframework.http.client.SimpleClientHttpRequestFactory()));
        this.mapper = new ObjectMapper();
    }

    public String codeToOpenId(String code) {
        if (code == null || code.isEmpty()) return null;
        // 演示/免凭据环境（WECHAT_APPID 未配置）：返回稳定测试 openid，
        // 避免把每次变化的 wx.login code 当 openid 写入而撑爆夫妻两个席位。
        if (isDemoWechatNotConfigured()) {
            return "wx_demo_test_openid";
        }
        if (code.startsWith("wx_")) return code;
        try {
            String url = String.format("https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code", appId, secret, code);
            String resp = rest.getForObject(url, String.class);
            JsonNode json = mapper.readTree(resp);
            if (json.has("openid")) return json.get("openid").asText();
            // 换取失败（code 无效/AppID·secret 不匹配/限频等）：打印错误码便于定位，返回 null 不污染席位
            System.out.println("[WeChat] jscode2session failed, appId=" + appId + " resp=" + resp);
        } catch (Exception ex) {
            System.out.println("[WeChat] jscode2session error: " + ex.getMessage());
        }
        return null;
    }

    /** 微信凭据是否未配置（演示/免凭据环境）。appId 为空、或为 Spring 未解析的占位符字面量。 */
    private boolean isDemoWechatNotConfigured() {
        if (appId == null || appId.isEmpty()) return true;
        String placeholder = "${app.wechat.app-id}";
        if (appId.equals(placeholder)) return true;
        return appId.startsWith("$");
    }

    public void saveUserOpenId(Long userId, String openid) {
        if (openid != null) userOpenIds.put("u_" + userId, openid);
    }

    /** 清除内存中的 openid 绑定缓存（重置绑定时调用） */
    public void clearUserOpenId(Long userId) {
        userOpenIds.remove("u_" + userId);
    }

    /** 下单通知 → 做饭人（现有模板：订单号/菜名/时间/辣度） */
    public void sendOrderNotify(Long userId, String orderId, String dishNames, String spiciness, String time) {
        Map<String, Object> dataMap = new java.util.HashMap<>();
        dataMap.put("character_string1", Map.of("value", "#" + orderId));
        dataMap.put("thing2", Map.of("value", clip(dishNames)));
        dataMap.put("time4", Map.of("value", time));
        dataMap.put("thing9", Map.of("value", clip(spiciness)));
        sendSubscribe(userId, templateId, dataMap, "/pages/wife/orders");
    }

    /** 老婆已收到订单 → 下单人（老公），复用现有订单模板 */
    public void sendReceivedNotify(Long userId, String orderNumber, String dishNames, String time) {
        Map<String, Object> dataMap = new java.util.HashMap<>();
        dataMap.put("character_string1", Map.of("value", "#" + orderNumber));
        dataMap.put("thing2", Map.of("value", "老婆已收到订单"));
        dataMap.put("time4", Map.of("value", time));
        dataMap.put("thing9", Map.of("value", clip(dishNames)));
        sendSubscribe(userId, templateId, dataMap, "/pages/husband/menu");
    }

    /** 做完饭 → 点餐端（老公），新模板 date7 完成时间 / thing5 温馨提示 */
    public void sendCookDoneNotify(Long userId, String timeText, String tips) {
        Map<String, Object> dataMap = new java.util.HashMap<>();
        dataMap.put("date7", Map.of("value", timeText));
        dataMap.put("thing5", Map.of("value", clip(tips)));
        sendSubscribe(userId, templateCookDoneId, dataMap, "/pages/husband/menu");
    }

    private String clip(String s) {
        if (s == null) return "";
        return s.length() > 20 ? s.substring(0, 19) + "…" : s;
    }

    private void sendSubscribe(Long userId, String tplId, Map<String, Object> dataMap, String page) {
        String openid = userOpenIds.get("u_" + userId);
        if (openid == null) {
            System.out.println("[WeChat] No openid for user " + userId);
            return;
        }
        System.out.println("[WeChat] Sending to openid: " + openid);

        String token = getAccessToken();
        if (token == null) {
            System.out.println("[WeChat] Failed to get access_token");
            return;
        }

        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("touser", openid);
            body.put("template_id", tplId);
            body.put("page", page);
            body.put("miniprogram_state", miniprogramState);
            body.put("lang", "zh_CN");
            body.put("data", dataMap);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = rest.exchange(
                "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + token,
                HttpMethod.POST, entity, String.class);
            System.out.println("[WeChat] HTTP " + resp.getStatusCodeValue() + " Body: " + resp.getBody());
        } catch (Exception e) {
            System.out.println("[WeChat] Error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            if (e instanceof HttpClientErrorException) {
                System.out.println("[WeChat] Body: " + ((HttpClientErrorException) e).getResponseBodyAsString());
            }
        }
    }

    private String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiresAt) return accessToken;
        try {
            String url = String.format("https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s", appId, secret);
            String resp = rest.getForObject(url, String.class);
            JsonNode json = mapper.readTree(resp);
            if (json.has("access_token")) {
                accessToken = json.get("access_token").asText();
                int expiresIn = json.get("expires_in").asInt(7200);
                tokenExpiresAt = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
                return accessToken;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
