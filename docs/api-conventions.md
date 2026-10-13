# MedVision — API Conventions

## Backend tuzilmasi

Kod feature bo‘yicha ajratiladi:
- auth
- image
- analysis
- storage
- health
- common

Har bir feature kerak bo‘lganda controller, service, repository
va DTO klasslarini oladi. Hozir ishlatilmaydigan bo‘sh klasslar yaratilmaydi.

Controller HTTP request va response bilan ishlaydi.
Service biznes qoidalarini bajaradi.
Repository database bilan ishlaydi.
DTO tashqi API ma’lumotlarini belgilaydi.
Dependency injection constructor orqali amalga oshiriladi.
Entity API javobida bevosita qaytarilmaydi.

## Request va response

JSON maydonlari camelCase formatida.
Identifikatorlar UUID bo‘ladi.
Sanalar Instant orqali UTC ISO-8601 formatida qaytariladi.
Fayllar multipart/form-data orqali qabul qilinadi.

## Status code

- 200: muvaffaqiyatli o‘qish yoki yangilash
- 201: yangi resurs yaratildi
- 204: muvaffaqiyatli o‘chirish, response body yo‘q
- 400: noto‘g‘ri request yoki validation
- 401: login talab qilinadi
- 403: ruxsat berilmagan
- 404: resurs topilmadi
- 405: HTTP metod qo‘llab-quvvatlanmaydi
- 409: ma’lumotlar ziddiyati
- 413: upload hajmi limitdan katta
- 415: request media type qo‘llab-quvvatlanmaydi
- 500: kutilmagan server xatosi

## Xato formati

- timestamp
- status
- code
- message
- path
- fieldErrors: field va message elementlari ro‘yxati

Frontend qarorlarni code orqali qabul qiladi.
Message foydalanuvchiga ko‘rsatiladi.
Rejected value, parol va tokenlar javobga kiritilmaydi.
Kutilmagan exception tafsilotlari HTTP javobiga chiqarilmaydi.

## Pagination — endpointlar uchun belgilangan qoida

- page: 0 dan boshlanadi
- size: default 20, minimum 1, maximum 100
- Noto‘g‘ri page yoki size: HTTP 400
- Default tartib: createdAt DESC, id DESC
- Sort maydonlari har bir endpointda allowlist orqali belgilanadi

Response:
items, page, size, totalElements, totalPages, first, last.

Chegaralarni tekshirish va sort allowlist ro‘yxat endpointlari
yaratilganda amalga oshiriladi. PageResponse faqat javob formatidir.

## Local API manzillari

- To‘g‘ridan-to‘g‘ri: http://localhost:18080/api
- Next.js orqali: http://localhost:3000/api

Browser requestlari /api/... manzilidan foydalanadi.
Next.js ularni Spring Boot’ga uzatadi.

## Hujjat va tekshiruv

- API qoidalari docs/ ichida saqlanadi.
- Endpoint misollari server/requests/*.http ichida saqlanadi.
- Xato handleri MockMvc testlari bilan tekshiriladi.
- Security filter xatolari authentication phase’da moslashtiriladi.

## Log qoidalari

Parollar, session cookie, tokenlar va tasvir mazmuni loglanmaydi.
Request body to‘liq loglanmaydi.
Exception xabarlariga secret va foydalanuvchi ma’lumotlari qo‘shilmaydi.

## Analysis options endpoints

GET /api/analysis-options
- Rejalashtirilgan algoritmlar, formatlar va limitlarni qaytaradi.
- segmentationAvailable hozir false.
- Upload o‘lcham limitlari upload phase’da amalga oshiriladi.

POST /api/analysis-options/validate
- Parametrlarni tekshiradi; segmentatsiya bajarmaydi.
- algorithm: OTSU, majburiy.
- blurKernelSize: 1–15 oralig‘idagi toq butun son, majburiy.
- invert: boolean, majburiy.
- Valid request: HTTP 200 va tekshirilgan parametrlar.
- DTO xatosi: HTTP 400, VALIDATION_ERROR va fieldErrors.
- Juft kernel: HTTP 400, INVALID_BLUR_KERNEL.

HTTP misollar:
- server/requests/analysis-options.http
- server/requests/analysis-validation.http