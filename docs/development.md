# MedVision — Local Development

## Talablar
Docker Desktop ishlayotgan bo‘lishi kerak.
Buyruqlar repository root papkasida bajariladi.

## Ishga tushirish
    docker compose up -d --build
    docker compose logs -f backend frontend

Backendda Started ServerApplication, frontendda Ready chiqishini kuting.
Log kuzatuvidan Ctrl+C bilan chiqish containerlarni to‘xtatmaydi.

## Manzillar
- Frontend: http://localhost:3000
- Backend health: http://localhost:18080/api/health
- Frontend orqali health: http://localhost:3000/api/health
- PostgreSQL: localhost:5433

Docker ichida database: postgres:5432.
Docker ichida backend: backend:8080.

## HTTP tekshirish
IntelliJ’da server/requests/health.http faylini oching.
Ikkala request HTTP 200 va medvision-server javobini qaytarishi kerak.

## Java kodini compile qilish
    docker compose exec backend ./mvnw compile

DevTools compiled class o‘zgarishlaridan keyin backendni restart qiladi.

## Backend test
    docker compose exec backend ./mvnw test

Hozirgi context testi development database’dan foydalanadi.
Ma’lumot yozadigan testlardan oldin alohida test database sozlanadi.

## Frontend tekshirish
    docker compose exec frontend npm run lint
    docker compose exec frontend npx tsc --noEmit
    docker compose exec frontend npm audit --omit=dev

## To‘xtatish
    docker compose stop

PostgreSQL named volume’da saqlanadi.
docker compose down -v database volume’ini o‘chiradi.

## Environment
Compose qiymatlarni compose.yaml orqali beradi.
.env.example faqat konfiguratsiya namunasidir.
Spring Boot .env faylini avtomatik o‘qimaydi.

## Dependency audit — 2026-10-13
Production audit: 0 vulnerabilities.
ESLint development zanjirida braces@3.0.3 muammosi ochiq.
Advisory: GHSA-vfj7-8cjw-p6xm.
## Test database setup
Yangi muhitda testlardan oldin:
    .\scripts\setup-test-db.ps1

Context va database testlari test profilidagi medvision_test bilan ishlaydi.
Database testlari transaction rollback orqali namunaviy yozuvlarni tozalaydi.