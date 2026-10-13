# Phase 4 — Authentication va foydalanuvchi ruxsatlari

Status: COMPLETE — local single-backend MVP.

## Bajarilgan ishlar

- [x] Register, login, logout va current user endpointlari.
- [x] BCrypt password hashing, cost 12.
- [x] Email normalizatsiyasi va unique constraint.
- [x] Noto'g'ri email/parol uchun umumiy login xabari.
- [x] Session ID login paytida yangilanadi.
- [x] MEDVISION_SESSION: HttpOnly, SameSite=Lax, Path=/.
- [x] Session timeout: 30 daqiqa.
- [x] CSRF token olish va frontend POST requestlarida yuborish.
- [x] Frontend same-origin cookie requestlari.
- [x] Local CORS: faqat http://localhost:3000.
- [x] Tasvir va tahlil metadata endpointlarida owner tekshiruvi.
- [x] Login limiti: email uchun 10, ulanish IP uchun 100 urinish / 15 daqiqa.
- [x] Logoutdan keyin eski session cookie ishlamasligi tekshirildi.

## Tekshiruvlar

- Backend: 18 test, 0 failure, 0 error.
- A/B HTTP testi: boshqa owner resurslariga 404.
- Ro'yxatlar faqat sessiondagi foydalanuvchining resurslarini qaytaradi.
- Frontend lint va TypeScript tekshiruvi o'tgan.
- npm audit --omit=dev: 0 vulnerabilities.
- HTTP cookie, CORS va login 429 tekshiruvlari o'tgan.
- Brauzer: register, login, refresh, logout, refresh o'tgan.

## Endpointlar

POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
GET  /api/auth/me
GET  /api/auth/csrf
GET  /api/images
GET  /api/images/{id}
GET  /api/analyses
GET  /api/analyses/{id}
GET  /api/images/{id}/analyses

## Amaldagi chegaralar

- Bu bosqich tasvir va tahlil metadatasini himoyalaydi.
  Keyingi upload, download, mask va overlay endpointlarida ham owner
  tekshiruvi majburiy.
- Limiter bitta backend jarayonining xotirasida ishlaydi.
  Restartda sanagichlar yangilanadi; ko'p instance uchun umumiy
  limiter saqlash tizimi kerak.
- Frontend proksisi orqali kelgan ulanishlar IP limitini bo'lishishi
  mumkin. Email limiti alohida ishlaydi.
- Local HTTP uchun Secure cookie o'chirilgan.
  HTTPS deploymentda SESSION_COOKIE_SECURE=true berilishi kerak.
- Production uchun development DB credentials almashtiriladi,
  frontend origin sozlanadi va DevTools ishlatilmaydi.
