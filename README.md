# Minweb - Min's web appliction framework

## 主要功能

Babashka web application framework base. Includes the following features:
- UI layout/styling (Hiccup2 + Tailwind CSS)
- User authentication/administration with role-based privileges
- Login rate limiting (per-IP tracking)
- Dashboard layout with sidebar navigation

## 技术栈

- 前端：Htmx/Hyperscript + Tailwind CSS v4
- 后端：Babashka (babashka.cli, babashka.fs, http-kit, hiccup2)
- 数据库：Datalevin

## 目录结构

```
minweb/
  ├── src/
  │   ├── core.clj              # Main entry point, middleware composition
  │   ├── common.clj            # Shared utilities (env config)
  │   ├── routes.clj            # Route definitions using ruuter DSL
  │   ├── database/
  │   │   └── dtlv.clj          # Datalevin operations
  │   ├── middleware/
  │   │   ├── auth.clj          # Auth checks, privilege-based authorization
  │   │   └── rate_limit.clj    # Login rate limiting (per-IP)
  │   ├── utils/
  │   │   ├── encryption.clj    # Password hashing
  │   │   ├── response.clj      # Response helpers
  │   │   └── session.clj       # Session management
  │   └── view/
  │       ├── core.clj          # Reusable UI components (badge, card, form-input, etc.)
  │       └── layout.clj        # Base layout, navbar, pagination, dashboard
  ├── resources/
  │   ├── config.edn            # Configuration
  │   ├── public/               # Static assets (css, js, img)
  │   ├── tw_input.css          # Tailwind CSS input with @theme variables
  │   └── tw_out.css            # Compiled Tailwind CSS output
  ├── test/                     # Test files
  ├── db/                       # Datalevin database
  ├── bb.edn                    # Babashka build config
  └── CLAUDE.md                 # Claude Code guidance
```

## View Components

### view.core - 通用 UI 组件

| Component | Description |
|-----------|-------------|
| `badge` | 徽章 (variant: primary/info/success/warning/danger/secondary) |
| `callout` | 提示框 (severity: info/success/warning/danger) |
| `form-input` | 表单输入框 (type: text/textarea/select/checkbox/radio/toggle) |
| `form-select` | 下拉选择框 |
| `form-checkbox` / `form-radio` | 多选/单选框 |
| `form-toggle` | 开关 |
| `card` | 卡片 (title/content/footer) |
| `stat-card` | 统计卡片 (label/value/trend) |
| `breadcrumb` | 面包屑导航 |
| `empty-state` | 空状态占位 |
| `loading-spinner` | 加载动画 |
| `progress-bar` | 进度条 |
| `toast` | 提示消息 |
| `confirm-dialog` | 确认对话框 |
| `grid` / `container` | 布局容器 |
| `sidebar` / `accordion` / `tree-view` | 导航组件 |
| `tabs-view` | 标签页视图 |
| `page-header` / `welcome-banner` | 页面头部 |
| `activity-item` / `activity-feed` | 活动列表 |
| `quick-action` / `stats-icon` | 快捷操作 |
| `page-title` | 页面标题 |

### view.layout - 布局组件

| Component | Description |
|-----------|-------------|
| `layout` | 基础页面布局 (nav + body + footer) |
| `modal` | 模态框 |
| `navbar` / `nav-view` | 导航栏 |
| `search-form` | 搜索表单 |
| `paginator` | 分页器 |
| `dashboard-sidebar` | 仪表盘侧边栏 |
| `dashboard-layout` | 仪表盘布局 |

## Commands

```bash
# Start web application (depends on tailwindcss)
bb start-web

# Generate tailwindcss
bb tailwindcss

# Seed database with test data
bb db-seed

# Run a single bb task
bb <task-name>
```
