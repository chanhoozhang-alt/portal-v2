# Portal-v2 请求调用链

以 portal-service 为例，说明用户请求 `GET /api/portal/init` 时的完整调用链。

## 调用链总览

```
浏览器发请求
  │
  ▼
① Tomcat 接收请求
  │
  ▼
② InternalTokenFilter（Filter，Servlet 层）
  │  路径不以 /internal 开头 → 直接放行
  │
  ▼
③ DispatcherServlet（Spring MVC 入口）
  │
  ▼
④ AuthInterceptor.preHandle()（Interceptor，Spring MVC 层）
  │  拦截 /api/portal/** 路径，执行认证逻辑
  │  ├─ 从 Header 提取 Token
  │  ├─ 查 Redis 缓存（Token→userId，userId→身份信息）
  │  ├─ 缓存未命中 → Feign 调用 server-service 验证
  │  └─ 存入 UserContext（ThreadLocal）
  │
  ▼
⑤ PortalController.init()（Controller 层）
  │  调用 portalService.init()
  │
  ▼
⑥ PortalServiceImpl.init()（Service 层）
  │  从 UserContext 拿用户信息，从 Redis 读可见应用，分组排序后返回
  │
  ▼
⑦ 结果原路返回 → Controller → JSON 响应给浏览器
  │
  ▼
⑧ AuthInterceptor.afterCompletion()
     清理 UserContext ThreadLocal，防止内存泄漏
```

## AuthInterceptor 如何被各服务使用

`AuthInterceptor` 定义在 `portal-common` 中，但它**没有加 `@Component` 注解**，不会自动生效。各服务需要手动创建并注册。

### 注册机制

**启动类扫描范围：**

```java
// PortalApplication.java
@SpringBootApplication(
    scanBasePackages = {"com.example.portal.portal", "com.example.portal.common"}
)
```

`scanBasePackages` 包含了 `com.example.portal.common`，Spring 能扫描到 common 包下的 `@Component`、`@Configuration` 等类。但 `AuthInterceptor` 是普通类，需要手动注册。

**WebMvcConfig 手动创建并注册：**

```java
// portal-service 的 WebMvcConfig.java
@Configuration
public class WebMvcConfig {

    @Bean
    public MappedInterceptor portalAuthInterceptor(PermissionCacheManager cacheManager,
                                                    ServerFeignClient serverFeignClient) {
        // 手动 new，不是 Spring 自动扫描
        AuthInterceptor interceptor = new AuthInterceptor(cacheManager, token -> {
            // Lambda 就是 fallbackHandler.initAuth(token) 的具体实现
            Result<AuthInitResponse> result = serverFeignClient.initAuth(token);
            // ... 转换为 AuthInitResult 返回
        });

        // 注册到 Spring MVC，绑定拦截路径
        return new MappedInterceptor(new String[]{"/api/portal/**"}, null, interceptor);
    }
}
```

### 启动时注册链路

```
PortalApplication.main()
  │
  ▼
SpringApplication.run()
  │
  ▼
① 创建 ApplicationContext（Spring 容器）
  │
  ▼
② 包扫描（根据 scanBasePackages）
  │  com.example.portal.portal   → 扫描所有类
  │  com.example.portal.common   → 扫描所有类
  │
  ▼
③ 识别 Bean 定义（哪些类要创建实例）
  │
  ▼
④ 创建并注入 Bean（按依赖顺序）
  │
  ▼
⑤ 容器就绪，Tomcat 开始接收请求
```

#### 第 ② 步 — 包扫描

Spring 扫描两个包下的所有类，识别带注解的类：

```
com.example.portal.portal
  ├── config/WebMvcConfig.java           ← @Configuration ✅ 识别为配置类
  ├── controller/PortalController.java   ← @RestController ✅
  ├── service/impl/PortalServiceImpl.java ← @Service ✅
  ├── feign/ServerFeignClient.java       ← @FeignClient ✅
  └── ...

com.example.portal.common
  ├── security/InternalTokenFilter.java   ← @Component ✅
  ├── security/AuthInterceptor.java       ← 什么注解都没有 ❌ 跳过
  ├── cache/PermissionCacheManager.java   ← @Component ✅
  ├── context/UserContext.java            ← 普通类 ❌ 跳过
  └── ...
```

AuthInterceptor 没有任何注解，Spring 扫描时会跳过，不会自动创建。

#### 第 ④ 步 — 创建 Bean（核心）

Spring 按依赖顺序创建 Bean。`WebMvcConfig` 的创建过程：

```
Spring 发现 WebMvcConfig 是 @Configuration
  │
  ▼
处理 @Bean 方法 portalAuthInterceptor()
  │
  │  方法签名需要两个参数：
  │  - PermissionCacheManager cacheManager
  │  - ServerFeignClient serverFeignClient
  │
  ▼
Spring 从容器中找这两个参数（依赖注入）：
  │
  ├─ PermissionCacheManager → 已有（@Component 扫描时创建的）
  │
  └─ ServerFeignClient → 已有（@FeignClient 扫描时创建的代理对象）
  │
  ▼
两个参数都齐了，开始执行方法体
```

方法体执行过程：

```java
@Bean
public MappedInterceptor portalAuthInterceptor(PermissionCacheManager cacheManager,
                                                ServerFeignClient serverFeignClient) {
    // ① new AuthInterceptor，传入缓存管理器和一个 Lambda
    AuthInterceptor interceptor = new AuthInterceptor(cacheManager, token -> {

        // ② 这个 Lambda 是 AuthFallbackHandler 接口的实现
        //    启动时不执行，等请求来了、缓存没命中时才执行
        Result<AuthInitResponse> result = serverFeignClient.initAuth(token);

        if (result == null || result.getCode() != 200 || result.getData() == null) {
            return null;
        }

        // ③ 转换为 AuthInitResult 返回给 AuthInterceptor
        AuthInitResponse data = result.getData();
        AuthInitResult r = new AuthInitResult();
        r.setUserId(data.getUserId());
        r.setUserName(data.getUserName());
        // ... 其他字段
        return r;
    });

    // ④ 包装成 MappedInterceptor，绑定拦截路径
    return new MappedInterceptor(new String[]{"/api/portal/**"}, null, interceptor);
}
```

**两个时间点要区分：**

| 时间点 | 执行内容 | 说明 |
|--------|---------|------|
| **启动时** | `new AuthInterceptor(cacheManager, lambda)` | 把 cacheManager 和 lambda 存为成员变量，Lambda 内部代码不执行 |
| **请求时** | `fallbackHandler.initAuth(token)` | 缓存未命中时才执行 Lambda 内部的 Feign 远程调用 |

方法返回后，Spring 拿到 `MappedInterceptor` 对象：
- 内部包含 `AuthInterceptor` 实例
- Spring MVC 自动识别，将其注册为拦截器
- 绑定路径 `/api/portal/**`

### 完整时间线

```
┌─────────────────────────────────────────────────┐
│  启动阶段（只执行一次）                            │
│                                                   │
│  PortalApplication.main()                         │
│    → Spring 扫描包                                │
│    → 创建 PermissionCacheManager Bean             │
│    → 创建 ServerFeignClient 代理 Bean              │
│    → 创建 WebMvcConfig                            │
│    → 执行 portalAuthInterceptor() @Bean 方法       │
│      → new AuthInterceptor(cacheManager, lambda)  │
│      → new MappedInterceptor(["/api/portal/**"])  │
│    → Spring MVC 注册拦截器                        │
│    → Tomcat 启动，监听 8081 端口                   │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  请求阶段（每次请求执行）                           │
│                                                   │
│  请求到达                                         │
│    → InternalTokenFilter.doFilterInternal()       │
│    → DispatcherServlet 分发                       │
│    → AuthInterceptor.preHandle()                  │
│      → 查缓存                                     │
│      → 缓存未命中 → lambda.initAuth(token)        │
│        → serverFeignClient.initAuth(token)        │
│        → 远程调用 server-service                   │
│      → 存入 UserContext                           │
│    → Controller 处理业务                          │
│    → AuthInterceptor.afterCompletion() 清理       │
└─────────────────────────────────────────────────┘
```

### fallbackHandler 回源调用链

```
AuthInterceptor.preHandle() 缓存未命中
  │
  │  调用 fallbackHandler.initAuth(token)
  │  （fallbackHandler 是构造时注入的 Lambda）
  │
  ▼
WebMvcConfig 中的 Lambda
  │  serverFeignClient.initAuth(token)
  │
  ▼
ServerFeignClient（Feign 远程调用）
  │  携带 X-Internal-Token 请求头
  │
  ▼
server-service 的 /internal/auth/init 接口
  │  InternalTokenFilter 校验内部 Token
  │  验证用户 Token → 返回用户身份信息
  │
  ▼
结果原路返回 → Lambda 转换为 AuthInitResult → AuthInterceptor 存入 UserContext
```

### 为什么这样设计

- `AuthInterceptor` 在 common 中只定义**认证流程骨架**（查缓存 → 回源 → 设上下文），不知道具体怎么验证 Token
- 各服务通过 `WebMvcConfig` 注入**不同的验证策略**（Lambda），决定调哪个 Feign 客户端、调哪个接口
- common 保持通用，各服务保持灵活

---

## 各步骤详细说明

### 第 ② 步 — InternalTokenFilter（过滤器）

**类位置：** `portal-common/src/main/java/com/example/portal/common/security/InternalTokenFilter.java`

- `@Order(Ordered.HIGHEST_PRECEDENCE)` 保证最先执行
- 以 `/internal` 开头的路径：校验 `X-Internal-Token` 请求头，不匹配则抛 403
- 其他路径：调用 `filterChain.doFilter()` 放行

**作用：** 保护服务间内部接口，外部请求无法访问 `/internal/**`。

### 第 ④ 步 — AuthInterceptor（认证拦截器）

#### 拦截器注册

**类位置：** `portal-service/src/main/java/com/example/portal/portal/config/WebMvcConfig.java`

通过 `@Bean` 返回 `MappedInterceptor`，将 `AuthInterceptor` 绑定到 `/api/portal/**` 路径。

构造时传入两个依赖：
- `PermissionCacheManager` — Redis 缓存读写
- `AuthFallbackHandler`（Lambda）— 缓存未命中时通过 Feign 调 server-service 验证 Token

> 采用 `MappedInterceptor` 而非 `WebMvcConfigurer`，因为后者在 MVC 初始化早期收集，此时 Feign 客户端还未就绪，会形成循环依赖。

#### 认证逻辑

**类位置：** `portal-common/src/main/java/com/example/portal/common/security/AuthInterceptor.java`

`preHandle()` 执行流程：

1. 从 `Authorization` 请求头提取 Token（支持 `Bearer xxx` 和裸 Token 两种格式）
2. 查 Redis 缓存：`portal:token:{token}` → userId
   - 命中 → 继续查身份缓存
   - 未命中 → 调用 `fallbackHandler.initAuth(token)` 回源
3. 查 Redis 缓存：`portal:identity:{userId}` → 身份信息 JSON
   - 命中 → 反序列化为 UserContext
   - 未命中 → 调用 `fallbackHandler.initAuth(token)` 回源
4. 将用户信息存入 `UserContext`（ThreadLocal），供后续业务代码使用
5. 缓存命中时调用 `cacheManager.renewTTL()` 续期

#### 清理逻辑

`afterCompletion()` 调用 `UserContext.clear()` 清理 ThreadLocal，防止内存泄漏。

### 第 ⑤ 步 — PortalController（控制器）

**类位置：** `portal-service/src/main/java/com/example/portal/portal/controller/PortalController.java`

```java
@GetMapping("/init")
public Result<PortalInitResponse> init() {
    return Result.success(portalService.init());
}
```

门户首页接口，所有已认证用户均可访问，无需额外权限校验。

### 第 ⑥ 步 — PortalServiceImpl（业务逻辑）

**类位置：** `portal-service/src/main/java/com/example/portal/portal/service/impl/PortalServiceImpl.java`

`init()` 方法核心逻辑：

1. `UserContext.get()` 从 ThreadLocal 获取当前用户信息
2. `cacheManager.getVisibleApps(userId)` 从 Redis 读取用户可见应用列表
3. 按分组（groupCode）分组，组内按 appSortNo 排序，组间按 groupSortNo 排序
4. 组装用户信息、管理员标识、应用列表，返回 `PortalInitResponse`

## 三种服务的区别

| 服务 | 拦截路径 | 端口 | 特殊处理 |
|------|---------|------|---------|
| portal-service | `/api/portal/**` | 8081 | 无额外权限校验，所有已认证用户可访问 |
| console-service | `/api/admin/**` | 8082 | Controller 内调用 `PermissionChecker` 做权限校验；有 `@OperationLog` 的方法会被 AOP 记录操作日志 |
| server-service | `/api/auth/**` | 8083 | 认证服务，被其他服务通过 Feign 调用；`/internal/**` 接口受 InternalTokenFilter 保护 |

## 关键类索引

| 类 | 所属模块 | 职责 |
|----|---------|------|
| `InternalTokenFilter` | portal-common | 内部接口安全过滤，校验 X-Internal-Token |
| `AuthInterceptor` | portal-common | 认证拦截，Token 验证 + UserContext 设置/清理 |
| `WebMvcConfig` | 各服务 | 注册 AuthInterceptor，绑定拦截路径和 Token 验证策略 |
| `UserContext` | portal-common | ThreadLocal 用户上下文，存储当前请求的用户信息 |
| `PermissionCacheManager` | portal-common | Redis 缓存读写，管理 token/identity/visible 三个命名空间 |
| `PermissionChecker` | portal-common | 权限校验静态方法（系统管理员/应用管理员/业务管理员） |
| `GlobalExceptionHandler` | portal-common | 全局异常处理，将 BusinessException/UnauthorizedException 等转为统一 Result 响应 |
