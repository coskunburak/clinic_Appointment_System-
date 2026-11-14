# Demo seed, test fixture ve veri güvenliği

## Kurgusal örnek veri (`DemoDataCatalog` / `DemoDataSeeder`)
**Kuruluş:** `Örnek Sağlık Grubu`; **şubeler:** `Merkez Hastanesi` (`Kadıköy, İstanbul`, `Europe/Istanbul`) ve `Sahil Polikliniği` (`Beşiktaş, İstanbul`, `Europe/Istanbul`).

| Şube | Bölüm | Hizmet | Süre | Ücret | Atanan yetkili hekim |
|---|---|---|---|---|---|
| Merkez Hastanesi | Kardiyoloji | Genel Muayene | 30 dk | 1500.00 TRY | Dr. Ahmet Kaya (`doctor.ahmet@clinic.test`) |
| Merkez Hastanesi | Kardiyoloji | Eko Çekimi | 45 dk | 2500.00 TRY | Dr. Ahmet Kaya (`doctor.ahmet@clinic.test`) |
| Merkez Hastanesi | Radyoloji | Radyoloji Akciğer Grafisi | 20 dk | 1200.00 TRY | Dr. Ahmet Kaya (`doctor.ahmet@clinic.test`) |
| Sahil Polikliniği | Dahiliye | Genel Muayene | 30 dk | 1500.00 TRY | Dr. Ayşe Demir (`doctor.ayse@clinic.test`) |
| Sahil Polikliniği | Laboratuvar | Kan Tahlili | 15 dk | 600.00 TRY | Dr. Ayşe Demir (`doctor.ayse@clinic.test`) |

**Demo Kullanıcıları (`clinic.demo.credentials.*` -> `BCryptPasswordEncoder(12)`):**
- Yönetici (`ROLE_ADMIN`, kurum geneli): `admin@clinic.test` (`DEMO_ADMIN_PASSWORD`, `dev` varsayılanı: `Admin123!`)
- Hekimler (`ROLE_STAFF`, şube kapsamlı): `doctor.ahmet@clinic.test`, `doctor.ayse@clinic.test` (`DEMO_DOCTOR_PASSWORD`, `dev` varsayılanı: `Doctor123!`)
- Hastalar (`ROLE_PATIENT`): `patient.ali@clinic.test` (Ali Yılmaz), `patient.zeynep@clinic.test` (Zeynep Kaya) (`DEMO_PATIENT_PASSWORD`, `dev` varsayılanı: `Patient123!`)
- Gerçek kişi bilgisi veya düz metin parola asla veritabanına yazılmaz ve uygulama loglarına basılmaz.

## Üretim izolasyonu (`DemoEnvironmentGuard`)
- Varsayılan `application.yml` profili `dev` değildir (`spring.profiles.default` kaldırılmıştır); `clinic.demo.seed.enabled` ve `clinic.demo.reset.enabled` varsayılan olarak `false`'tur.
- `DemoDataSeeder` (`@Profile("dev")` + `@ConditionalOnExpression`) yalnızca `-Dspring-boot.run.profiles=dev` açıkça verildiğinde etkinleşir.
- `DemoEnvironmentGuard`, aktif profilde `dev` zorunluluğunu, `prod`/`production`/`staging`/`live`/`preprod`/`release` profillerinin yokluğunu, opt-in bayrağını ve JDBC URL'sinin yerel PostgreSQL (`localhost`, `127.0.0.1`, `[::1]`, yerel container/bridge veya `jdbc:tc:postgresql:`) olmasını denetleyerek üretim hedeflerinde fail-closed hata fırlatır.

## Fixture takvimi ve örnek randevular
- **Şube Saatleri:** `Merkez Hastanesi` Pzt–Cum `08:30–18:00`, Cmt `09:00–14:00`; `Sahil Polikliniği` Pzt–Cum `09:00–18:00`.
- **Hekim Mesaisi ve İstisnalar:** Dr. Ahmet Pzt–Cum `09:00–17:00`, Cmt `09:00–13:00`; Dr. Ayşe Pzt–Cum `09:00–17:00`. Gelecek haftalarda `PARTIAL_LEAVE` (+7 gün 12:00–13:00), `OVERTIME` (+7 gün 17:00–18:00) ve `FULL_DAY_LEAVE` (+14 gün) örnekleri yer alır.
- **5 Randevu Durumu (`Clock` + saf `Appointment` geçişleri):** Önceki iş gününe 2 `COMPLETED`, sonraki iş gününe `CONFIRMED`, 2 `PENDING`, 1 `REJECTED` ve 1 `CANCELLED` randevu ile uyumlu append-only `appointment_status_history` kayıtları deterministik adlandırılmış UUID'lerle idempotent oluşturulur; var olan ilişkisiz kayıtlara dokunulmaz.

## Güvenli demo sıfırlama stratejisi (`DemoDataResetService`)
- HTTP/UI üzerinden sıfırlama uç noktası bulunmaz; sıfırlama `application-dev.yml` içinde bile varsayılan olarak `false`'tur.
- `DemoDataResetService`, FK kısıtlarını devre dışı bırakmadan ve `TRUNCATE` kullanmadan tek PostgreSQL `@Transactional` sınırında yalnızca `DemoDataCatalog` ve demo kurum/hesaplara bağlı alt kayıtları ters bağımlılık sırasıyla (`appointment_status_history` -> `appointment` -> `availability_exception` -> `weekly_availability` -> `staff_service_eligibility` -> `staff_assignment` -> `branch_service` -> `service_definition` -> `department` -> `branch_opening_hours` -> `membership` -> `patient_profile` -> `staff_profile` -> `user_account` -> `branch` -> `organization`) siler; ilişkisiz kiracı verilerini korur:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments="--spring.main.web-application-type=none --clinic.demo.reset.enabled=true --clinic.demo.reset.reseed=true"
```
Yerel konteyner hacmini tamamen sıfırlamak için ise yalnızca geliştirici onayıyla `docker compose down -v` kullanılabilir.
