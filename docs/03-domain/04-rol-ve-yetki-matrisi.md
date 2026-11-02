# Roller, erişim politikası ve IDOR koruması

| İşlem | Hasta | Personel | Şube/kurum yöneticisi |
|---|---|---|---|
| Kendi hesabını gör | evet | evet | evet |
| Kurum/şube listele | yalnız aktif ve görünür | yalnız yetkili | yetkili |
| Kurum/şube oluştur/düzenle | hayır | hayır | kapsamı dahilinde |
| Bölüm/hizmet/süre/ücret düzenle | hayır | hayır | kapsamı dahilinde |
| Personel/uzmanlık/hizmet atanması düzenle | hayır | hayır | kapsamı dahilinde |
| Personel çalışma saati düzenle | hayır | öneri/opsiyonel | evet |
| Uygun saat ara | evet | evet | evet |
| Randevu talep et | yalnız kendi adına | hayır | hayır (MVP) |
| Kendi randevusunu gör/iptal et | evet | — | kapsamı dahilinde |
| Kendi atanmış randevularını gör/onayla/reddet | hayır | evet | kapsamı dahilinde |
| Tamamlandı işaretle | hayır | kendi randevusu | kapsamı dahilinde |
| Tüm hasta verisini listele | hayır | hayır | yalnız iş için gerekli ve kapsamı dahilinde |

**Kimlik modeli:** `UserAccount` kimlik doğrular; global `Role` ve kaynak bazlı `OrganizationMembership`/`BranchMembership` yetki kapsamını belirler. `StaffProfile` veya `PatientProfile` gerçek iş profilleridir; profile ID'leri oturumdaki kullanıcıyla eşleştirilir. Tek kullanıcı birden çok rol alabilecekse yetki birleşimi yalnız o anki kaynağa göre değerlendirilir.

## Zorunlu sunucu kontrolleri
- `GET /patient/appointments/{id}` kaynak patientId = authenticatedPatientId değilse erişim yok.
- `POST /staff/appointments/{id}/approve`: yalnız appointment.staff.userId veya appointment.branch içinde yetkili yönetici.
- `POST /admin/branches/{id}`: org + branch membership scope eşleşir; formdan gelen org id güvenilmez.
- Staff birden çok branch'te olabilir; sadece atanmış branch içindeki randevular görünür.
- Başka tenant ID'si ile sorgu yapıldığında endpoint varlığı ifşa edilmemeli; uygulama tutarlı 404/403 stratejisi seçmeli.

## Test edilecek kötüye kullanımlar
Oturumsuz erişim, başka hastanın UUID'si, başka şube yönetim UUID'si, personelin başka doktora ait randevuyu onaylaması, rol parametresi manipülasyonu, gizlenmiş form alanı değiştirme, URL'de orgId oynama.
