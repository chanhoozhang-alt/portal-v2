package com.example.portal.server.client;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 外部身份平台 HTTP 客户端，用于 Token 验证和获取用户身份信息。 */
@Slf4j
@Component
public class IdentityPlatformClient {

    @Value("${portal.identity-platform.url:http://localhost:9090/api}")
    private String platformUrl;

    /**
     * 调用外部身份平台验证 Token，返回用户身份信息。
     * 注意：当前使用字符串拼接构建 JSON，Token 含特殊字符时可能存在风险，建议改为 JSONUtil 序列化。
     */
    public IdentityInfo verifyToken(String token) {
        String url = platformUrl + "/auth/verify";
        String body = "{\"token\":\"" + token + "\"}";
        try {
            String resp = HttpUtil.post(url, body);
            JSONObject json = JSONUtil.parseObj(resp);
            if (json.getInt("code") != 200) {
                log.warn("身份平台Token验证失败: {}", resp);
                return null;
            }
            JSONObject data = json.getJSONObject("data");
            IdentityInfo info = new IdentityInfo();
            info.setUserId(data.getStr("userId"));
            info.setUserName(data.getStr("userName"));
            info.setOrgId(data.getStr("orgId"));
            info.setOrgName(data.getStr("orgName"));
            info.setDeptId(data.getStr("deptId"));
            info.setDeptName(data.getStr("deptName"));
            return info;
        } catch (Exception e) {
            log.error("调身份平台异常", e);
            return null;
        }
    }

    @lombok.Data
    public static class IdentityInfo {
        private String userId;
        private String userName;
        private String orgId;
        private String orgName;
        private String deptId;
        private String deptName;
    }
}
