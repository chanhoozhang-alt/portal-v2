# 统一认证对接文档

## 一、认证登录

登录跳转接口使用浏览器直接访问或地址重定向（GET 请求），将用户导向认证登录页面，认证完成后返回 code。

### 1. 各环境访问地址

| 环境 | 地址 |
| :--- | :--- |
| UAT 环境 | `https://one-account-gateway.paasuat.cmbchina.cn/auth-server/auth?client_id=xxx&redirect_uri=xxx&response_type=code&scope=xxx&state=xxx` |
| 生产环境（办公网、互联网访问） | `https://oauth-paas.cmbchina.com/auth-server/auth?client_id=xxx&redirect_uri=xxx&response_type=code&scope=xxx&state=xxx` |
| 生产环境（业务网） | `https://oauth-paas.cmbchina.cn/auth-server/auth?client_id=xxx&redirect_uri=xxx&response_type=code&scope=xxx&state=xxx` |

### 2. 参数说明

| 参数 | 格式 | 说明 | 要求 |
| :--- | :--- | :--- | :--- |
| client_id | string | 客户端 id | 必填 |
| response_type | string | 固定值 `code` | 必填 |
| redirect_uri | string | 登录成功后的回调地址，用于接收授权码；需经过 urlencode 编码处理，否则存在参数被截取丢失的情况。注意：回调地址不允许携带 `#` 符号，`#` 符号及之后内容会被丢弃 | 选填 |
| scope | string | 申请授权范围 | 选填 |
| state | string | 随机值（IE 可能会缓存请求导致问题，建议加上以兼容 IE） | 选填 |

**调用示例**：

```
https://one-account-gateway.paasuat.cmbchina.cn/auth-server/auth?client_id=f8bad92eb4&redirect_uri=http%3A%2F%2Ftest.paas.cmbchina.com%2FTest%2Flogin&response_type=code&scope=default&state=afo0e83ew
```

### 3. 回调说明

用户登录成功后，认证中心将根据 `redirect_uri` 参数跳回应用，并携带授权码信息：

```
HTTP/1.1 302 Found
Location: http://test.paas.cmbchina.com/login?code=5pr9w8f9cwe6g9rh62Wer&state=afo0e83ew
```

### 4. 回调参数

| 参数 | 格式 | 说明 | 要求 |
| :--- | :--- | :--- | :--- |
| code | string | 授权码，用于下一步获取 token | 必填 |
| state | string | 值为上一步调用登录跳转接口传递的 state，用于预防 CSRF 攻击 | 选填 |

---

## 二、Token 获取接口

Token 获取接口基于 OIDC 协议，目前支持**授权码模式**和**客户端凭据模式**。调用该接口需要采用国密算法 SM2WithSM3 对接口进行签名。

> **注意**：后端调用接口使用 **http** 协议，不要用 https。

### 2.1 授权码模式（常用于用户登录场景）

客户端应用通过上述步骤获取到授权码后，调用该接口获取 token 信息，以便拿到登录用户信息。

#### 1. 访问地址

| 环境 | 备注 | 地址 |
| :--- | :--- | :--- |
| UAT 环境 | - | `POST http://one-account-gateway.paasuat.cmbchina.cn/auth-server/token?client_id=xxx&grant_type=authorization_code&code=xxx` |
| 生产环境（支持 DMZ 区调用） | 推荐部署在 DMZ 区域的应用调用 | `POST https://oauth-paas.cmbchina.com/auth-server/token?...` |
| 生产环境（支持业务网、DMZ 区调用） | 推荐部署在 BIZ 区域的应用调用 | `POST https://oauth-paas.cmbchina.cn/auth-server/token?...` |

> **注**：部署在办公网环境的应用，招行统一认证暂不开放对接，如有此类需求请联系周鸿宇 / 80362449 咨询。

#### 2. 参数说明

| 参数 | 格式 | 说明 | 要求 |
| :--- | :--- | :--- | :--- |
| client_id | string | 客户端 id 值 | 必填 |
| grant_type | string | 固定值：`authorization_code` | 必填 |
| code | string | 通过单点登录接口获取的授权码 | 必填 |
| 其他签名参数 | - | 请参考 2.4 示例 | 必填 |

#### 3. 返回值字段说明

| 字段 | 格式 | 说明 |
| :--- | :--- | :--- |
| access_token | jwt | 用于后续调用其他接口的凭据 |
| refresh_token | jwt | 用于刷新 access_token，默认有效期 7 天 |
| id_token | jwt | 用户基本信息 |
| token_type | string | 固定值：`Bearer` |
| expires_in | number | accessToken 的有效期时长，单位秒 |

#### 4. 返回值样例

```json
{
  "access_token": "eyJhbGc10IJUzI1NiJ9.eyJ0b2tlb16IntcVnZ4g6XJzZWFwOmi51.......................",
  "refresh_token": "eyJhbGc10IJUzI1NiJ9.eyJ0b2tlb16IntcVngd3ZXJzZWFwOmi51.......................",
  "id_token": "eyJhbGc10IJUzI1NiJ9.eyJ0b2tlb16IntcVnZXJhf4zZWFwOmi51.......................",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

### 2.2 刷新 Token

当 access_token 已过期时，可使用 refresh_token 重新获取完整的 token 对象。refresh_token 的有效期比 access_token 更长（默认 7 天），如果 refresh_token 也已过期，则必须要求用户重新登录。

#### 1. 访问地址

| 环境 | 地址 |
| :--- | :--- |
| UAT 环境 | `POST http://one-account-gateway.paasuat.cmbchina.cn/auth-server/token?client_id=xxx&grant_type=refresh_token&refresh_token=xxx` |
| 生产环境（支持 DMZ 区调用） | `POST https://oauth-paas.cmbchina.com/auth-server/token?...` |
| 生产环境（支持业务网、DMZ 区调用） | `POST https://oauth-paas.cmbchina.cn/auth-server/token?...` |

#### 2. 参数说明

| 参数 | 格式 | 说明 | 要求 |
| :--- | :--- | :--- | :--- |
| client_id | string | 客户端 id 值 | 必填 |
| grant_type | string | 固定值：`refresh_token` | 必填 |
| refresh_token | string | 从之前获取的 token 中得到 | 必填 |
| 其他签名参数 | - | 请参考 2.4 示例 | 必填 |

#### 3. 返回值字段说明

同授权码模式，见 [2.1 节返回值字段说明](#3-返回值字段说明)。

> accessToken 默认有效期 3 小时（10800s），可在注册应用配置中修改。

### 2.3 示例

以客户端凭据模式获取 token 为例（授权码模式主要是参数不一样）。如何设置签名用的 header，如使用工具包帮助进行签名，请参考《接口调用鉴权说明》，如自实现签名请参考《Http 请求签名》。

---

## 三、Java 代码解析

### 依赖

```xml
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>3.10.0</version>
</dependency>
```

### 解析 id_token 获取用户信息

```java
// 根据应用选择算法：
//   国密：new CMBSM2WithSM3(null, centerPublicKey)
//   RSA 512位：new RS256(null, centerRsaPublicKey)
//   RSA 2048位：new RS256(null, center2048RsaPublicKey)

String centerPublicKey;        // 认证中心国密算法公钥
String centerRsaPublicKey;     // 认证中心 RSA 512 位公钥
String center2048RsaPublicKey; // 认证中心 RSA 2048 位公钥
String clientId;               // 当前应用注册后获得的 clientId

// 1. 解码 token，判断签名算法
DecodedJWT decodedJWT = JWT.decode(token.getIdToken());
String algorithmName = decodedJWT.getAlgorithm();

Algorithm centerAlgorithm;
if (Objects.equals("SM3WithSM2", algorithmName)) {
    centerAlgorithm = new CMBSM2WithSM3(null, centerPublicKey);
} else {
    if (512 == decodedJWT.getHeaderClaim("len").asInt()) {
        centerAlgorithm = new RS256(null, centerRsaPublicKey);
    } else {
        centerAlgorithm = new RS256(null, center2048RsaPublicKey);
    }
}

// 2. 验签
DecodedJWT verifiedJWT = JWT.require(centerAlgorithm)
        .acceptLeeway(60) // 时间校验容差（秒），不同服务器可能存在时钟不一致
        .build()
        .verify(token.getIdToken());

// 3. 校验 token 是否颁发给本应用（防止用其他应用的 token 访问本应用）
final ClientInfo clientInfo = JsonUtil.readValue(verifiedJWT.getAudience().get(0), ClientInfo.class);
if (!clientInfo.getId().equals(clientId)) {
    throw new RuntimeException("非本应用token");
}

// 4. 提取用户信息
String openid = verifiedJWT.getClaim("openId").asString();
String username = verifiedJWT.getClaim("userName").asString();
```

---

## 四、Token 返回用户信息字段说明

> 未出现在下表中的字段为非标准 token 字段，此类字段不做保障维护。如有额外字段业务需求，可单独申请接口。

| 字段名 | 类型 | 说明 |
| :--- | :--- | :--- |
| openId | string | 用户唯一标识 |
| userName | string | 用户姓名 |
| sapId | string | 人事系统 ID |
| employeeId | string | 员工编号 |
| ytstd | string | 一号通 ID |
| rtclId | string | devops 账号 ID |
| platformUserType | string | 账号类型：0（SAP 关联账号）、1（独立账号）、2（临时账号）、9（虚拟账号） |
| userType | string | 用户类型：1（行员）、2（子公司员工）、3（总行 IT 外包）、9（默认用户）、98（实习生）、99（数据中心合作方）、110（信用卡中心 IT 外包员工）、120（网络经合作方） |
| pathName | string | 机构路径 |
| pathId | string | 机构路径 ID |
| orgId | string | 机构 ID（32 位） |
| orgName | string | 机构名称 |
| originPathId | string | 原始机构路径 |
| originOrgId | string | 原始机构 ID |
| enterpriseId | string | 企业 ID |
| defaultEnterpriseId | string | 默认企业 ID |
| joinedEnterprisesIds | string | 加入的企业 ID（取用户所属企业身份与当前访问应用授权企业的交集） |
| enterpriseName | string | 企业名称 |
| passedAuthTypes | string | 已通过的认证方式及时间，如：`{"verifyCode":"1687329910059","qrCode":"1687329310058"}` |
| independentUser | string | 真实用户对象信息（虚拟用户登录时才有该字段） |
| netEnv | int | 登录时所在网络环境：0（办公网）、1（互联网）、2（业务网） |
| iat | int | token 签发时间 |
| exp | int | token 过期时间（令牌默认 3 小时，刷新令牌默认 7 天） |
| iss | string | 签发者：`oa-auth.paas.cmbchina.com` |
| sub | string | 值同 openId |
| aud | string | token 接收者（应用基本信息） |
