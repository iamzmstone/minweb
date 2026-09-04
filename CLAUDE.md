# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Minweb is a Babashka-based web application framework using:
- **Backend**: Babashka with http-kit server, ruuter for routing
- **Database**: Datalevin (accessed via `dtlv` babashka pod)
- **Frontend**: Htmx + Hyperscript + Tailwind CSS, rendered with Hiccup2

## Commands

```bash
# Start web application (generates Tailwind CSS first)
bb start-web

# Generate tailwindcss (required before start-web if editing CSS)
bb tailwindcss

# Seed database with default admin user
bb db-seed

# Run server on custom port
bb start-web --port 3000

# Run tests
bb test
```

## Architecture

### Request Flow
```
Request → ruuter routing → middleware chain → view handlers → Hiccup2 HTML

Middleware chain (in core.clj):
  wrap-request-logging → wrap-security-headers → wrap-rate-limit → wrap-auth → wrap-anti-forgery → wrap-flash → wrap-session → wrap-session-create → wrap-session-timeout → wrap-multipart-params → wrap-params → wrap-not-found → wrap-error-handler
```

### Key Files
- `src/minweb/core.clj` - Entry point, server startup, middleware composition
- `src/minweb/routes.clj` - Route definitions using ruuter DSL, includes catch-all for 404
- `src/minweb/database/dtlv.clj` - Datalevin operations (schema, queries, transactions)
- `src/minweb/middleware/auth.clj` - Auth checks, privilege-based authorization
- `src/minweb/middleware/rate_limit.clj` - Login rate limiting (per-IP attempt tracking)
- `src/minweb/middleware/security.clj` - Security headers (X-Frame-Options, CSP, etc.)
- `src/minweb/middleware/request_log.clj` - Request logging and tracing (X-Request-Id)
- `src/minweb/middleware/session.clj` - Session timeout handling (default 30 min)
- `src/minweb/middleware/error.clj` - Error handling (404/500 pages)
- `src/minweb/database/migration.clj` - Schema version management and migrations
- `src/minweb/view/health.clj` - Health check endpoint (/health) — probes the DB; 200 `ok` / 503 `degraded`
- `src/minweb/view/core.clj` - Reusable UI components (badge, card, stat-card, etc.), re-exports from view.config, view.icons, and view.form
- `src/minweb/view/config.clj` - Component configuration maps (badge-variant-classes, badge-size-classes, input-size-classes, merge-classes)
- `src/minweb/view/icons.clj` - SVG icon components (icon-paths, icon, user-menu)
- `src/minweb/view/form.clj` - Form components (form-input, form-select, form-checkbox, form-radio, form-toggle, form-submit-btn)
- `src/minweb/view/layout.clj` - Base HTML layout with navbar, pagination, dashboard
- `src/minweb/utils/oss_aliyun.clj` - 阿里云 OSS provider(自实现 HmacSHA1 签名,无 SDK 依赖;put-object / get-object)
- `src/minweb/utils/oss_ctyun.clj` - 天翼云 ZOS provider(AWS SigV4 + path-style URL;put-object / get-object / put-bucket-policy!)
- `src/minweb/utils/oss_sniff.clj` - 图片 magic byte 嗅探(JPEG/PNG/GIF/WebP,跨三方「multipart 撒谎」 bug 用)

### Database Schema
Defined in `schema.edn` using Datalevin's schema format. Current entities:
- `user` - email (unique), name, password, role, privs (cardinality/many)

### Authorization Model
Users have a `user/role` (keyword) and `user/privs` (set of keywords). Roles define privilege sets in `minweb.middleware.auth` (`:switch`, `:pon`, `:project`, `:alert`, `:search`, `:admin`).

### Session Management
Sessions store the current user entity. Auth middleware checks `session/current-user` to authorize requests against `restricted-pages`. Sessions expire after `:session-ttl` seconds (default 1800 = 30 minutes).

### View Pattern
Views return Hiccup vectors which are converted to HTML strings in the layout. CSRF tokens via `minweb.view.core/csrf-token`, flash messages via `minweb.view.core/alert`.

## Configuration

Configuration is loaded via `cprop` with environment variable + `.env` file override support.
Layers(后者覆盖前者):
1. `resources/config.edn`(基线)
2. 项目根目录 `.env`(cprop `from-env-file` 源,缺失静默跳)
3. 真实 `System/getenv`(只接受 `MINWEB_` / `DINGTALK_` / `OSS_` 前缀)

### config.edn
```clojure
{:app-name "MyApp"
 :title "My Application"
 :dtlv-opts "db/dtlv.db"          ;; Datalevin database path
 :page-size 20                     ;; Pagination size
 :session-ttl 1800                ;; Session timeout in seconds (30 min)
 :init-email "admin@example.com"  ;; Default admin email
 :init-name "Admin"               ;; Default admin name
 :init-role :admin}               ;; Default admin role
```

### Environment Variable Override
Set `MINWEB_` / `DINGTALK_` / `OSS_` prefixed environment variables to override config values:
```bash
export MINWEB_PORT=3000
export MINWEB_APP_NAME="Production App"
export MINWEB_SESSION_TTL=3600    ;; 1 hour session timeout
export DINGTALK_APP_KEY=...
export OSS_ACCESS_KEY_ID=...
bb start-web
```

Available override keys: `MINWEB_PORT`, `MINWEB_APP_NAME`, `MINWEB_TITLE`, `DINGTALK_APP_KEY`, `DINGTALK_APP_SECRET`, `DINGTALK_AGENT_ID`, `DINGTALK_CORP_ID`, `OSS_ACCESS_KEY_ID`, `OSS_BUCKET`, etc.

### .env File
项目根的 `.env` 文件被自动 source(Docker env 格式:`KEY=VALUE` 一行一条)。cprop 不会自动 source,这里手动加了 `cprop.source/from-env-file ".env"`。缺失或无文件静默跳过(部署环境走真实环境变量更标准)。

```bash
# .env 示例(不提交,见 .gitignore)
DINGTALK_APP_KEY=dingxxxxxxxx
DINGTALK_APP_SECRET=xxxxx
OSS_BUCKET=my-bucket
OSS_ENDPOINT=oss-cn-hangzhou.aliyuncs.com
```

注意:加新 key 后,如果 DB `config` 表已经初始化,可能需要在调用方跑一次同步命令(本项目是 `bb db-seed :force`)。

## Testing

```bash
# Run all tests
bb test

# Run with clj-kondo lint
clj-kondo --lint src/minweb/
```

Test files are located in `test/` directory with namespace pattern `*-test` or `*_test.clj`.

## Clojure/Hiccup Best Practices

### 避免括号匹配错误
- **分解为小函数**：将复杂 Hiccup 结构拆分为独立函数，减少单函数内嵌套层数
- **分批次添加**：每添加少量代码就用 `clj-kondo --lint` 检查，避免堆积错误
- **缩进规范**：每个子元素独立缩进，避免行尾堆积 `]`
- **用 Write 而非 Edit**：大幅修改或新建文件时用 Write 工具完整重写
- **写完即查**：完成修改后立即运行 `clj-kondo --lint`，有错立刻修复

```bash
# 检查括号匹配
clj-kondo --lint src/minweb/view/layout.clj
```