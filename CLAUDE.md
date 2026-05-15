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
```

## Architecture

### Request Flow
```
Request → ruuter routing → middleware chain → view handlers → Hiccup2 HTML

Middleware chain (in core.clj):
  wrap-rate-limit → wrap-auth → wrap-anti-forgery → wrap-flash → wrap-session → wrap-multipart-params → wrap-params
```

### Key Files
- `src/core.clj` - Entry point, server startup, middleware composition
- `src/routes.clj` - Route definitions using ruuter DSL
- `src/database/dtlv.clj` - Datalevin operations (schema, queries, transactions)
- `src/middleware/auth.clj` - Auth checks, privilege-based authorization
- `src/middleware/rate_limit.clj` - Login rate limiting (per-IP attempt tracking)
- `src/view/core.clj` - Reusable UI components (badge, card, form-input, stat-card, etc.)
- `src/view/layout.clj` - Base HTML layout with navbar, pagination, dashboard

### Database Schema
Defined in `schema.edn` using Datalevin's schema format. Current entities:
- `user` - email (unique), name, password, role, privs (cardinality/many)

### Authorization Model
Users have a `user/role` (keyword) and `user/privs` (set of keywords). Roles define privilege sets in `middleware/auth.clj` (`:switch`, `:pon`, `:project`, `:alert`, `:search`, `:admin`).

### Session Management
Sessions store the current user entity. Auth middleware checks `session/current-user` to authorize requests against `restricted-pages`.

### View Pattern
Views return Hiccup vectors which are converted to HTML strings in the layout. CSRF tokens via `view.core/csrf-token`, flash messages via `view.core/alert`.

## Configuration
- `resources/config.edn` - App configuration (loaded via cprop)
- `schema.edn` - Database schema definition

## Clojure/Hiccup Best Practices

### 避免括号匹配错误
- **分解为小函数**：将复杂 Hiccup 结构拆分为独立函数，减少单函数内嵌套层数
- **分批次添加**：每添加少量代码就用 `clj-kondo --lint` 检查，避免堆积错误
- **缩进规范**：每个子元素独立缩进，避免行尾堆积 `]`
- **用 Write 而非 Edit**：大幅修改或新建文件时用 Write 工具完整重写
- **写完即查**：完成修改后立即运行 `clj-kondo --lint`，有错立刻修复

```bash
# 检查括号匹配
clj-kondo --lint src/view/layout.clj
```