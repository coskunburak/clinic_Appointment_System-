# HTTP/MVC endpoint taslağı

> Bu proje ilk aşamada REST API projesi değil; Spring MVC + Thymeleaf ile server-rendered uygulamadır. Aşağıdaki URL'ler kontrolör sözleşmesidir. İleride REST API tasarlanabilir.

| Method | Route | Auth | İşlev | Başarı | Hata |
|---|---|---|---|---|---|
| GET | `/login` | public | oturum formu | 200 | — |
| POST | `/login` | public, CSRF | Spring form-login | 302 | 302/mesaj |
| GET | `/patient/dashboard` | PATIENT | `/patient/appointments` yönlendirmesi | 302 | 403 |
| GET | `/patient/book` | PATIENT | şube/hizmet/hekim/tarih/slot sihirbazı | 200 | 403/400 |
| GET | `/patient/slots` | PATIENT | JSON boş saat listesi (`staffId`, `branchServiceId`, `date`) | 200 | 400/403 |
| POST | `/patient/book` | PATIENT, CSRF | yeni `PENDING` randevu (`BookingForm`) | 302 | 200 (form/çakışma mesajı) |
| GET | `/patient/appointments` | PATIENT | yaklaşan & sayfalı geçmiş randevular | 200 | 403 |
| POST | `/patient/appointments/{id}/cancel` | owner, CSRF | gerekçeli iptal (`CancellationForm`) | 302 | 302/flash, 403 |
| GET | `/staff/dashboard` | STAFF | `/staff/schedule` yönlendirmesi | 302 | 403 |
| GET | `/staff/schedule` | STAFF | `view=daily\|weekly\|pending` takvimi | 200 | 403/400 |
| POST | `/staff/appointments/{id}/approve` | STAFF, scope, CSRF | randevu onayı | 302 | 302/flash, 403 |
| POST | `/staff/appointments/{id}/reject` | STAFF, scope, CSRF | gerekçeli red (`RejectionForm`) | 302 | 302/flash, 403 |
| POST | `/staff/appointments/{id}/complete` | STAFF, scope, CSRF | randevu tamamlama | 302 | 302/flash, 403 |
| GET | `/admin`, `/admin/dashboard` | ADMIN scoped | yetki kapsamı özet metrikleri | 200 | 403 |
| POST | `/admin/organizations/{id}/edit` | ORG_ADMIN, CSRF | kurum adı güncelleme | 302 | 403/flash |
| GET/POST | `/admin/branches/**` | ADMIN scoped, CSRF | şube listesi, ekleme, düzenleme, aktifleştirme/pasifleştirme | 200/302 | 403/400 |
| GET/POST | `/admin/hours`, `/admin/branches/{id}/hours/**` | ADMIN scoped, CSRF | şube çalışma saatleri görüntüleme, kaydetme, silme | 200/302 | 403/400 |
| GET/POST | `/admin/departments/**` | ADMIN scoped, CSRF | bölüm listesi, ekleme, düzenleme, aktifleştirme/pasifleştirme | 200/302 | 403/400 |
| GET/POST | `/admin/services/**` | ADMIN scoped, CSRF | kurumsal tanımlar, şube atama, süre/fiyat ve durum güncelleme | 200/302 | 403/400 |
| GET/POST | `/admin/staff/**` | ADMIN scoped, CSRF | kapsam içi personel kaydı, atama, yetkinlik, mesai/izin ekleme-silme | 200/302 | 403/400 |
| GET | `/error` | permitAll (forward/error) | özel `400`, `403`, `404`, `500` hata sayfaları | 400–500 | — |

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
