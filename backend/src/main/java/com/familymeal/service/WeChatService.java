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
    private final String miniprogramState;

    private String accessToken;
    private long tokenExpiresAt;
    private static final Map<String, String> userOpenIds = new ConcurrentHashMap<>();

    public WeChatService(@Value("${app.wechat.app-id}") String appId,
                         @Value("${app.wechat.secret}") String secret,
                         @Value("${app.wechat.template-id}") String templateId,
                         @Value("${app.wechat.miniprogram-state:trial}") String miniprogramState) {
        this.appId = appId;
        this.secret = secret;
        this.templateId = templateId;
        this.miniprogramState = miniprogramState;
        // Spring Boot 3.x 默认 chunked 传输, 微信要求 Content-Length
        this.rest = new RestTemplate(new org.springframework.http.client.BufferingClientHttpRequestFactory(
                new org.springframework.http.client.SimpleClientHttpRequestFactory()));
        this.mapper = new ObjectMapper();
    }

    public String codeToOpenId(String code) {
        if (code == null || code.isEmpty()) return null;
        if (code.startsWith("wx_")) return code;
        try {
            String url = String.format("https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code", appId, secret, code);
            String resp = rest.getForObject(url, String.class);
            JsonNode json = mapper.readTree(resp);
            if (json.has("openid")) return json.get("openid").asText();
        } catch (Exception ignored) {}
        return code;
    }

    public void saveUserOpenId(Long userId, String openid) {
        if (openid != null) userOpenIds.put("u_" + userId, openid);
    }

    /** 清除内存中的 openid 绑定缓存（重置绑定时调用） */
    public void clearUserOpenId(Long userId) {
        userOpenIds.remove("u_" + userId);
    }

    public void sendOrderNotify(Long wifeUserId, String orderId, String dishNames, String spiciness, String time) {
        String openid = userOpenIds.get("u_" + wifeUserId);
        if (openid == null) {
            System.out.println("[WeChat] No openid for user " + wifeUserId);
            return;
        }
        System.out.println("[WeChat] Sending to openid: " + openid);

        String token = getAccessToken();
        if (token == null) {
            System.out.println("[WeChat] Failed to get access_token");
            return;
        }
        System.out.println("[WeChat] Token: " + token.substring(0, 20) + "...");

        try {
            Map<String, Object> dataMap = new java.util.HashMap<>();
            dataMap.put("character_string1", Map.of("value", "#" + orderId));
            dataMap.put("thing2", Map.of("value", dishNames.length() > 20 ? dishNames.substring(0, 19) + "…" : dishNames));
            dataMap.put("time4", Map.of("value", time));
            dataMap.put("thing9", Map.of("value", spiciness.length() > 20 ? spiciness.substring(0, 19) + "…" : spiciness));

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("touser", openid);
            body.put("template_id", templateId);
            body.put("page", "/pages/wife/orders");
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
