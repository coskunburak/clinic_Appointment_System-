# HTTP/MVC endpoint taslağı

> Bu proje ilk aşamada REST API projesi değil; Spring MVC + Thymeleaf ile server-rendered uygulamadır. Aşağıdaki URL'ler kontrolör sözleşmesidir. İleride REST API tasarlanabilir.

| Method | Route | Auth | İşlev | Başarı | Hata |
|---|---|---|---|---|---|
| GET | `/login` | public | oturum formu | 200 | — |
| POST | `/login` | public, CSRF | Spring form-login | 302 | 302/mesaj |
| GET | `/patient/book` | PATIENT | filtre formu | 200 | 403 |
| GET | `/patient/available-slots` | PATIENT | branchService,staff,date filtreleri | 200 | 400/404 |
| POST | `/patient/appointments` | PATIENT, CSRF | yeni randevu | 303 | 409/422 |
| GET | `/patient/appointments` | PATIENT | kendi liste | 200 | 403 |
| GET | `/patient/appointments/{id}` | owner | kendi detay | 200 | 404 |
| POST | `/patient/appointments/{id}/cancel` | owner, CSRF | iptal | 303 | 409/403 |
| GET | `/staff/appointments` | STAFF | kendi programı | 200 | 403 |
| POST | `/staff/appointments/{id}/approve` | STAFF/ADMIN, scope | onay | 303 | 409/403 |
| POST | `/staff/appointments/{id}/reject` | STAFF/ADMIN, scope | ret | 303 | 409/403 |
| POST | `/staff/appointments/{id}/complete` | STAFF/ADMIN, scope | tamamla | 303 | 409/403 |
| GET/POST | `/admin/branches` | ADMIN scoped | şube liste/ekle | 200/303 | 403/422 |
| GET/POST | `/admin/services` | ADMIN scoped | hizmet liste/ekle | 200/303 | 403/422 |
| GET/POST | `/admin/staff` | ADMIN scoped | personel liste/ekle | 200/303 | 403/422 |
| GET/POST | `/admin/availability` | ADMIN scoped | program ve izin | 200/303 | 403/422 |

## `CreateAppointmentForm` sözleşmesi
```text
branchServiceId: UUID (required)
staffId: UUID (required)
localDate: ISO yyyy-MM-dd (required)
localTime: HH:mm (required)
requestKey: UUID/string (SHOULD; duplicate form submit)
```
`patientId`, `amount`, `duration`, `status`, `organizationId` formda güvenilir alanlar değildir; server karar verir.

## `AvailableSlots` gösterim modeli
`[{localDateTime, utcOffset, startInstant, durationMinutes, feeFormatted}]`. Tarih seçiminde offset belirsizliği varsa kullanıcıya ayrıntılı karar gösterilir; server istenen saati tekrar doğrular.

## Hata sözlüğü
| Kod | HTTP | Durum |
|---|---:|---|
| AUTH_REQUIRED | 302/401 | kullanıcı giriş yapmalı |
| ACCESS_DENIED | 403 | yetki yok |
| RESOURCE_NOT_FOUND | 404 | kaynak yok/gizli |
| INVALID_FORM | 422 | alan doğrulama hatası |
| BOOKING_CONFLICT | 409 | aynı slot dolu |
| STAFF_NOT_AVAILABLE | 409 | vardiya/izin uyumsuz |
| SERVICE_NOT_OFFERED | 422 | çalışan servis sağlayamıyor |
| INVALID_STATUS_TRANSITION | 409 | geçersiz onay/iptal |
| RETRYABLE_BOOKING_LOCK | 409/503 | kontrollü kilit yarışı, sonra tekrar deneyin |

UI uygulamasında `303 See Other` PRG (Post/Redirect/Get) modeli kullanılır; Spring controller gerçek response durumuna göre uygun redirect/render davranışı uygular.
