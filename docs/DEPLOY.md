# bborder 预览版 · 部署与流程文档

> 适用：bborder-preview（本地部署版，固定角色）/ bborder-preview-clear（开源版）/ bborder-regular 与 bborder-regular-clear（多家庭版）
> 更新：2026-09-06（双向微信通知 + 做饭状态）

## 1. 版本与仓库

| 目录 | 定位 | 仓库 |
|---|---|---|
| `bborder-preview` | 预览版本地部署版（固定角色：老公点菜 / 老婆做饭），用于本机更新与真机体验 | 本地 git（私有，不推远端） |
| `bborder-preview-clear` | 预览版开源仓库（去隐私：appid/secret/域名/模板 ID 均占位符化） | Gitee 公开仓库 |
| `bborder-regular` | 正式多家庭版（吃饭人/做饭人动态角色，已备案域名 tianmwsys.net） | Gitee 私有 `bborder-regular-preview` |
| `bborder-regular-clear` | regular 干净导出快照（固定版销售包） | Gitee 私有 `bborder-regular-clear` |

## 2. 业务闭环流程

### 预览版（固定角色：老公=吃饭人，老婆=做饭人）

```
老公下单 ─①→ 老婆收通知 ─②→ 老婆接单做菜 ─③→ 老婆完成推进 ─④→ 老公收通知
```

| 步骤 | 动作 | 接口 | 微信通知 | 模板 |
|---|---|---|---|---|
| ① | 老公菜单页下单 | `POST /api/orders` | → 老婆（做饭人） | 订单通知模板（订单号/菜名/时间/辣度） |
| ② | 老婆订单页「标记已收到」 | `PUT /api/orders/{id}/received` | → 老公「老婆已收到订单」 | 同上 |
| ③④ | 老婆菜品页「做完饭了」（cooking→done） | `POST /api/cook/status {status:"done"}` | → 老公「饭做好啦，快来吃饭吧」 | 订单完成模板（完成时间 date7 / 温馨提示 thing5） |
| 展示 | 老公菜单页横幅：做饭中 / 饭好啦 | `GET /api/cook/current` | — | — |

- 「重新做饭」回到 cooking（不发通知）；切换做饭人自动重置 cooking。
- 订阅：老婆登录订 1 个模板（收下单）；老公登录订 2 个模板（收已收到 + 饭好了）。

### regular 版（动态角色：吃饭人 / 做饭人）

同一流程，角色替换：吃饭人下单 → 做饭人收通知 → 做饭人接单/做菜 → 做完饭通知吃饭人。通知对象按组内关系解析（`cookUserId` / `order.userId` / `partner`），不写死老公老婆。

## 3. 端口与容器（预览版 docker）

| 服务 | 容器 | 端口 |
|---|---|---|
| MySQL 8.4.6 | `bbopen-preview-mysql` | 宿主 3318 → 3306 |
| Spring Boot 后端 | `bbopen-preview-backend` | 8088 |
| nginx 入口 | `bbopen-preview-nginx` | 8080（反代 `/` → backend:8088） |

数据库 `family_meal`，后端首启自动执行 `backend/src/main/resources/db/init.sql` 建表 + 幂等种子。

## 4. 部署步骤（本机 docker）

```powershell
# 1. 环境变量（docker/.env 不入库）
#    JWT_SECRET（>=32 ASCII 必填）、WECHAT_APPID、WECHAT_SECRET、
#    WECHAT_TEMPLATE_ID（订单通知）、WECHAT_TEMPLATE_COOK_DONE_ID（订单完成）

# 2. 构建后端 jar（Dockerfile 用预编译 jar）
cd backend; mvn package -DskipTests; cd ..

# 3. 启动
docker compose -f docker/compose.yml up -d --build

# 4. 前端
cd frontend; npm ci; npm run build:mp-weixin
# 微信开发者工具导入 frontend（miniprogramRoot=dist/build/mp-weixin）
```

## 5. 微信小程序配置

- appid：`frontend/project.config.json` 与 `frontend/src/manifest.json` 两处一致。
- 订阅消息模板：
  - 订单通知模板：字段 `character_string1`(订单号) + `thing2`(菜名) + `time4`(时间) + `thing9`(辣度)
  - 订单完成模板：字段 `date7`(完成时间) + `thing5`(温馨提示)
- 登录名额：固定两席位 husband/wife；同一 openid 复用席位。换 AppID 后需 `UPDATE users SET openid=NULL`。

## 6. 域名与真机

- 预览版本地联调用 `https://pc.tail2ef6b2.ts.net`（Tailscale funnel 443→8088），真机体验版需右上角「…」→「打开调试」跳过域名校验。
- `ts.net` 无法 ICP 备案，**正式上线必须换备案域名**（如 `tianmwsys.net`）并在微信后台登记 request/uploadFile/downloadFile 三类合法域名（填裸域名，不带协议）。
- 开发者工具模拟器可用 `http://127.0.0.1:8088` + 勾选「不校验合法域名」。

## 7. 常见坑

1. **登录名额被占**：微信凭据未配置时后端返回稳定 `wx_demo_test_openid`（只占 1 席）；失败返回 null，不再把一次性 code 当 openid。
2. **tab/子tab 不显示**：`categories`/`subcategories` 种子已并入 `init.sql`（幂等），且 `spring.sql.init.encoding=UTF-8` 防中文乱码。
3. **Docker 容器出站失败**：Docker Desktop 注入的代理 `127.0.0.1:7897` 未开时，容器访问微信 API 会失败——需开代理或关闭 Docker Desktop 代理设置。
4. **`/api/auth/me` 404**：已补 `GET /me` 与 `PUT /me/background`（DIY 背景）。

## 8. 验证清单

```powershell
curl http://localhost:8088/api/categories          # 返回 2 分类 + 9 子分类
curl http://localhost:8088/api/cook/current        # 返回 cookWho + cookStatus
curl http://localhost:8080/api/categories          # nginx 反代 200
docker exec bbopen-preview-mysql mysql -uroot family_meal -e 'SHOW TABLES;'   # 7 表
```
