# Minweb - Min's web appliction framework

## 主要功能

Babashka web application framework base. Includes the following features:
- UI layout/styling
- User authentication/administration
- Routing

## 技术栈

- 前端：Htmx/Hyperscript + Tailwind CSS + 微信小程序
- 后端：Babashka(babashka.cli, babashka.fs, http-kit, hiccup2)
- 数据库：Datalevin

## 目录结构
minweb/
  ├── src/
  │   ├── core.clj           # Main entry point
  │   ├── common.clj         # Shared utilities
  │   ├── static.clj         # Static file serving
  │   ├── database/          # DB layer (dtlv, user)
  │   ├── middleware/        # Auth middleware
  │   ├── utils/            # Encryption, response, session helpers
  │   └── view/             # View handlers (admin, core, login, index)
  ├── resources/
  │   ├── config.edn        # Configuration
  │   ├── public/           # Static assets (css, js, img)
  │   └── templates/        # HTML templates
  ├── db/                   # SQLite database
  ├── bb.edn                # Babashka build config
  └── schema.edn            # Database schema definition

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
