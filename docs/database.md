# MedVision — Database

## Schema
V1__create_initial_schema.sql uchta jadval yaratadi:
users, medical_images, analyses.

ID: UUID.
Vaqt: TIMESTAMPTZ va Java Instant.
Analysis parameters: JSONB object.

## Relationships
User -> ko‘p MedicalImage.
MedicalImage -> ko‘p Analysis.
JPA bog‘lanishlari ManyToOne, LAZY.
Parent entitylarga cascade remove qo‘llanmaydi.

## Email
User constructor emailni trim va lowercase qiladi.
Database normalized email va unique constraintni tekshiradi.
Email format validatsiyasi authentication request DTO’da qo‘shiladi.

## Indexes
- medical_images(user_id, created_at DESC, id DESC)
- analyses(image_id, created_at DESC, id DESC)
- analyses(status, created_at DESC, id DESC)

## DTO
UserResponse passwordHashni chiqarmaydi.
MedicalImageResponse storageKeyni chiqarmaydi.
AnalysisResponse maskKey va overlayKeyni chiqarmaydi.
DTO mapping service transaction ichida bajariladi.
Database entitylari controllerdan bevosita qaytarilmaydi.

## Analysis lifecycle
PROCESSING -> COMPLETED yoki FAILED.
Yakunlangan analysis qayta yakunlanmaydi.
COMPLETED holatida result keylar va statistika majburiy.
FAILED holatida errorCode va completedAt majburiy.

## Tasvirni o‘chirish siyosati
Database foreign keylari ON DELETE RESTRICT.
Bog‘langan analysis mavjud bo‘lsa, rasmni bevosita o‘chirish bloklanadi.

Upload va analysis modullarida quyidagi oqim amalga oshiriladi:
1. Owner ruxsati tekshiriladi.
2. PROCESSING analysis mavjud bo‘lsa, o‘chirish HTTP 409 bilan rad etiladi.
3. Asl rasm, mask va overlay storage kalitlari yig‘iladi.
4. Transaction ichida analysis yozuvlari, keyin image yozuvi o‘chiriladi.
5. Commitdan so‘ng fayllar storage’dan o‘chiriladi.
6. Fayl o‘chirish muvaffaqiyatsiz bo‘lsa, retry va orphan cleanup qo‘llanadi.

Database va filesystem bitta atomik transaction emas.
Fayllar database commitidan oldin o‘chirilmaydi.
Fayl cleanup mexanizmi storage phase’da amalga oshiriladi.
Phase 3’da ushbu siyosat belgilangan, DELETE API hali yaratilmagan.

## Test database
scripts/setup-test-db.ps1 medvision_test mavjudligini ta’minlaydi.
Test profili: application-test.yaml.
DatabaseMappingTest har bir testni transaction rollback bilan yakunlaydi.

## Migration qoidasi
Qo‘llangan V1 o‘zgartirilmaydi.
Keyingi o‘zgarishlar V2, V3 kabi yangi migrationlarda yoziladi.
ddl-auto: validate saqlanadi.