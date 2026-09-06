# bborder-preview（bb点单预览版 · 开源版）

> **多 agent 阅读**：全工作区 agent 共享；前先读 `E:\memory\README.md`、`E:\memory\统一规则.md` D 域、`E:\memory\02-B项目-bborder\bborder-归档档案.md`。

夫妻档bb点单预览版微信小程序「bb点单预览版」+ Spring Boot 后端。固定角色：老婆做饭（管菜品、接单、做饭状态推进），老公点菜（看菜单、下单、收通知）。

> small open-source (foss) counterpart of a private family-meal mini-program.
> 本仓库为**干净开源版**：不含任何真实微信凭据、无真实用户 openid、无私有公网域名——相关项一律以占位符/环境变量注入。

## 功能

- 固定双角色：老婆=做饭端（管菜品、接订单、做饭状态推进），老公=点菜端（看菜单、下单、收通知）
- 菜品管理：分类/子分类、辣度（不辣/男宝辣/女宝辣）、备注、拍照/选图裁剪
- 双向微信订阅通知：老公下单→老婆；老婆「已收到」「做完饭」→老公
- 微信登录（code2session）+ JWT
- `DEMO_ENABLED=true` 时提供 H5 免密登录与演示数据重置（便于无微信凭据体验）

## 技术栈

- 后端：Java 17 / Spring Boot 3.2.5 / MyBatis-Plus / MySQL 8.4
- 前端：uni-app（Vue3）编译微信小程序
- 部署：Docker Compose（mysql + backend）

## 目录

```
bborder-preview-clear/
├── backend/   Spring Boot 后端（src/main/resources/db/init.sql 自建表+种子）
├── frontend/  uni-app 小程序源码（dist 由微信开发者工具编译预览）
├── docker/    compose.yml + .env.example + backend.Dockerfile
└── README.md
```

## 快速开始（Docker）

```bash
# 1. 准备环境变量（JWT_SECRET 必填，>=32 ASCII；WeChat 凭据可选）
cp docker/.env.example docker/.env
#    编辑 docker/.env：填 JWT_SECRET；如需微信登录填 WECHAT_APPID/SECRET

# 2. 若改过后端源码，先在本机构建 jar（Dockerfile 采用预编译 jar）
cd backend && mvn package -DskipTests && cd ..

# 3. 启动
docker compose -f docker/compose.yml up -d --build

# MySQL 空首启；backend 启动自动执行 backend/src/main/resources/db/init.sql 建表+种子（spring.sql.init.schema-locations=classpath:db/init.sql）
```

后端就绪后 `http://localhost:8088`。

- 追日志：`docker compose -f docker/compose.yml logs -f backend`
- 停止：`docker compose -f docker/compose.yml stop`（保留数据）
- 彻底清理卷：`docker compose -f docker/compose.yml down -v`（**会删库**）

## 本地非容器运行（可选）

```bash
# backend（需本机 MySQL 3308? 见 backend application.yml DB_URL 与 jar 打 标）
cd backend && mvn spring-boot:run
# frontend
cd frontend && npm install && npm run dev:mp-weixin   # 微信开发者工具导入 dist/dev/mp-weixin
```

## 微信小程序配置

- 在微信公众平台申请小程序，将 `appid` 填入：
  - `frontend/project.config.json` → `appid`
  - `frontend/src/manifest.json` → `"mp-weixin"."appid"`
- 配置请求域名：
  - `frontend/src/api/request.js` 顶部 `BASE_URL`
  - `frontend/src/config.js` 顶部 `BG_IMAGE_URL`
  - 均需为**合法备案的 HTTPS 域名**（小程序生产校验「request 合法域名」）。本地联调可用局域网 IP + 开发者工具勾选「不校验合法域名」。
- `docker/.env` 配置 `WECHAT_APPID/WECHAT_SECRET` 使后端能拿到 code2session。

## 环境变量（docker/.env，参考 docker/.env.example）

| 变量 | 说明 |
|---|---|
| `MYSQL_PASSWORD` | MySQL root 密码；留空 = 容器空密码 |
| `JWT_SECRET` | 后端签名密钥，**>=32 个 ASCII，必填** |
| `DEMO_ENABLED` | `true`：H5 免密登录 + demo 数据（无微信凭据时体验用） |
| `WECHAT_APPID/WECHAT_SECRET` | 小程序微信登录（code2session） |
| `WECHAT_TEMPLATE_ID` | 订单通知模板（下单/已收到） |
| `WECHAT_TEMPLATE_COOK_DONE_ID` | 订单完成通知模板（做完饭） |
| `ADMIN_PASSWORD/ADMIN_RESET_KEY` | 管理后台口令 / 重置身份密钥（可留空禁用） |

## 说明与免责

- 本项目按开源惯例剔除隐私：仓库不含任何真实 `openid`、微信 `appid/secret`、私有域名或本机路径。
- 数据库 schema 由后端 `backend/src/main/resources/db/init.sql` 在首次启动自动建立；业务数据请自行备份。

## 版权

Copyright © 2026 深圳市甜梦屋科技有限公司（Shenzhen Tianmengwu Technology Co., Ltd.）

本项目按 **Apache License 2.0** 开源，详见 [`LICENSE`](./LICENSE)。源码仅供学习与合规使用，使用时须保留本版权声明。
