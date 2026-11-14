# Sprint S09 Uygulama Raporu: Regresyon, Eşzamanlılık Stres Testi, Güvenli Demo Veri Tohumu ve Yerel Performans Kıyaslaması

- **Rapor Tarihi:** 2026-11-14
- **Sprint Kodu:** S09
- **Sprint Başlığı:** Regression, Concurrency Stress Verification, Deterministic Dev Demo Seeding & Reset, Local p95 Performance Verification, and Architectural Boundary Enforcement
- **İlişkili Gereksinimler:** `FR-034`, `NFR-004`, `NFR-005`, `NFR-008`, `NFR-010` (`TC-034`, `TC-040`, `TC-041`, `TC-044`, `TC-046`)
- **Sprint Statüsü:** `VERIFIED_BY_OWNER` · **Performans Statüsü:** `VERIFIED_BY_OWNER` *(Ürün sahibi tarafından `2026-11-14 12:05–12:06+03:00` arasında çalıştırılan `./mvnw clean verify` ile Surefire `366/366` ve Failsafe PostgreSQL 17 Testcontainers `135/135` yeşil doğrulandı.)*

---

## 1. Sprint Genel Bakış (Sprint Overview)

Sprint S09 hedefleri doğrultusunda:
1. Varsayılan çalışma profilinden (`application.yml`) yalıtılmış, yalnızca açık `dev` profili ve opt-in yapılandırmasıyla çalışan deterministik, idempotent demo veri tohumlayıcı (`DemoDataSeeder`, `DemoDataSeedService`, `DemoDataCatalog`, `DemoDataProperties`) geliştirildi (`FR-034`, `NFR-005`).
2. Yanlışlıkla üretim/uzak veritabanlarına uygulanmasını fail-closed biçimde engelleyen `DemoEnvironmentGuard` ve FK-güvenli, yalnızca demo kayıtlarını ters bağımlılık sırasıyla silip ilişkisiz kayıtları koruyan `DemoDataResetService` uygulandı (`FR-034`).
3. Dış `@Transactional` sarmalı olmadan 5 paralel iş parçacığının gerçek PostgreSQL 17 Testcontainers üzerinde aynı hekim/slot ve farklı slotlar üzerinde yarıştığı `ConcurrentBookingIT` stres testi yazıldı (`WP-S09-02`, `FR-015`, `NFR-004`).
4. 50 personel ve 1.000 randevuluk yerel veri setinde `System.nanoTime()` ile `p95 < 1000 ms` hedefini doğrulamaya yönelik `PerformanceBenchmarkIT` ve `PerformanceMetrics` / `PerformanceMetricsTest` kıyaslama altyapısı kuruldu (`NFR-008`, `TC-044`).
5. Mevcut Surefire + Failsafe tam regresyon kapsamı denetlendi ve `DomainArchitectureTest` ile `WebArchitectureTest` katman sınır kontrolleri genişletildi (`NFR-010`, `TC-046`).

---

## 2. Başlangıç Çizgisi (Baseline State)

- **Depo:** `https://github.com/coskunburak/clinic_Appointment_System-`
- **Etkin Dal:** `main`
- **Başlangıç `HEAD`:** `b21ee750d049bd4952b22ed6dcddd5b1fb6e5ab3` (`docs(s08): record owner-verified test success and complete S08 milestone [skip ci] [skip actions]`)
- **Doğrulanmış S08 Kod Tabanı Referansı:** `9c35715d8ff9fa93f514e4fab99d90db8e9bbec9` (`VERIFIED_BY_OWNER`, `./mvnw clean verify` ile `BUILD SUCCESS`, `121` Failsafe IT + Surefire suite yeşil)
- **Korunan Çekirdek:** Java 21, Spring Boot 3.4.2, PostgreSQL 17, Flyway `V1`–`V4` şema migrasyonları (değiştirilmedi), `pom.xml` bağımlılıkları, `BookingService` pesimist satır kilidi (`Patient` -> `Staff`), saf domain sınıfları.

---

## 3. S09 Değiştirilen ve Oluşturulan Dosyalar

### A. Ana Uygulama Kaynakları ve Yapılandırma (`src/main`)
- `src/main/resources/application.yml` — `spring.profiles.default: dev` kaldırıldı; `clinic.demo.seed.enabled: false`, `clinic.demo.reset.enabled: false`, `clinic.demo.reset.reseed: false` eklendi.
- `src/main/resources/application-dev.yml` — `clinic.demo.seed.enabled: true`, `reset.enabled: false`, `reset.reseed: true` ve ortam değişkeni destekli (`DEMO_ADMIN_PASSWORD`, `DEMO_DOCTOR_PASSWORD`, `DEMO_PATIENT_PASSWORD`) dev kimlik bilgileri eklendi.
- `src/main/resources/application-test.yml` — Açıkça `clinic.demo.seed.enabled: false`, `reset.enabled: false`, `reset.reseed: false` tanımlandı.
- `src/main/java/com/example/clinic/ClinicAppointmentApplication.java` — `@ConfigurationPropertiesScan` eklendi.
- `src/main/java/com/example/clinic/identity/application/AccountCredentialService.java` — Parola politikası tek noktadan korunarak `encodeVerifiedPassword` ve `normalizeEmail` yardımcıları açıldı.
- `src/main/java/com/example/clinic/booking/infrastructure/persistence/AppointmentJpaRepository.java` — Kapsamlı sıfırlama sorguları için `findIdsByBranchIdIn`, `findIdsByBranchServiceIdIn`, `findIdsByPatientIdIn`, `findIdsByStaffIdIn` eklendi.
- `src/main/java/com/example/clinic/booking/infrastructure/persistence/AppointmentStatusHistoryJpaRepository.java` — `findIdsByAppointmentIdIn` ve `findIdsByActorUserIdIn` eklendi.
- `src/main/java/com/example/clinic/common/infrastructure/DemoDataProperties.java` — Redacted `toString()` ile `@ConfigurationProperties("clinic.demo")` sınıfı.
- `src/main/java/com/example/clinic/common/infrastructure/DemoDataCatalog.java` — `clinic-demo:*` ad alanıyla deterministik UUID'ler ve doğal anahtar sabitleri.
- `src/main/java/com/example/clinic/common/infrastructure/DemoEnvironmentGuard.java` — Aktif `dev` profili, yasaklı profil reddi, opt-in bayrağı ve yerel PostgreSQL JDBC URL doğrulaması.
- `src/main/java/com/example/clinic/common/infrastructure/DemoDataResetCompletedEvent.java` — Transaction commit öncesi test/gözlem olayı.
- `src/main/java/com/example/clinic/common/infrastructure/DemoDataSeedService.java` — Transactional, idempotent ve kısmi veri uyumlu demo tohumlayıcı servisi.
- `src/main/java/com/example/clinic/common/infrastructure/DemoDataResetService.java` — Ters FK sırasıyla kapsamlandırılmış silme yapan transactional sıfırlama servisi.
- `src/main/java/com/example/clinic/common/infrastructure/DemoDataSeeder.java` — `@Component`, `@Profile("dev")`, `@ConditionalOnExpression`, `CommandLineRunner`.

### B. Testler ve Performans Kıyaslama (`src/test`)
- `src/test/java/com/example/clinic/common/infrastructure/DemoEnvironmentGuardTest.java` (4 test)
- `src/test/java/com/example/clinic/common/infrastructure/DemoDataSeederUnitTest.java` (6 test)
- `src/test/java/com/example/clinic/common/infrastructure/DemoDataSeederIT.java` (11 IT — izole `PostgreSQLContainer`)
- `src/test/java/com/example/clinic/ConcurrentBookingIT.java` (2 çok iş parçacıklı IT)
- `src/test/java/com/example/clinic/performance/PerformanceMetrics.java` & `PerformanceMetricsTest.java` (2 birim testi)
- `src/test/java/com/example/clinic/PerformanceBenchmarkIT.java` (1 IT — 50 hekim / 1.000 randevu p95 < 1000 ms doğrulaması)
- `src/test/java/com/example/clinic/domain/DomainArchitectureTest.java` (güncellendi — `common.infrastructure` ve `.web` bağımlılık yasağı)
- `src/test/java/com/example/clinic/web/WebArchitectureTest.java` (güncellendi — `LoginController` dahil, `DemoData*` enjeksiyon yasağı ve `@Autowired` alan enjeksiyonu denetimi)

### C. Dokümantasyon
- `README.md`, `docs/07-sprintler/README.md`, `docs/07-sprintler/S09.md`, `docs/07-sprintler/S09-implementation-report.md`, `docs/01-urun/01-gereksinim-matrisi.md`, `docs/01-urun/traceability.csv`, `docs/04-veri/03-seed-ve-ornek-veri.md`, `docs/06-kalite/02-test-senaryolari.md`, `docs/08-teslim/02-sunum-ve-demo.md`.

---

## 4. Demo Veri Modeli ve Kurmaca Kayıtlar (`FR-034`, `NFR-005`)

- **Kurum:** `Örnek Sağlık Grubu` (`active = true`)
- **Şubeler (`Europe/Istanbul`):**
  - `Merkez Hastanesi` (`Kadıköy, İstanbul`) — Pzt–Cum `08:30–18:00`, Cmt `09:00–14:00`
  - `Sahil Polikliniği` (`Beşiktaş, İstanbul`) — Pzt–Cum `09:00–18:00`
- **Bölümler:** Merkez (`Kardiyoloji`, `Radyoloji`, `Dahiliye`), Sahil (`Dahiliye`, `Laboratuvar`)
- **Hizmetler ve Şube Fiyat/Süreleri (`TRY`):**
  - `Merkez Hastanesi`: `Genel Muayene` (30 dk / `1500.00`), `Eko Çekimi` (45 dk / `2500.00`), `Radyoloji Akciğer Grafisi` (20 dk / `1200.00`)
  - `Sahil Polikliniği`: `Genel Muayene` (30 dk / `1500.00`), `Kan Tahlili` (15 dk / `600.00`)
- **Kullanıcılar (`AccountCredentialService` BCrypt hash):**
  - `admin@clinic.test` (`ROLE_ADMIN`, kurum geneli)
  - `doctor.ahmet@clinic.test` (Dr. Ahmet Kaya — `Kardiyoloji`, Merkez Hastanesi, Pzt–Cum `09:00–17:00`, Cmt `09:00–13:00`)
  - `doctor.ayse@clinic.test` (Dr. Ayşe Demir — `Dahiliye`, Sahil Polikliniği, Pzt–Cum `09:00–17:00`)
  - `patient.ali@clinic.test` (Ali Yılmaz, `ROLE_PATIENT`)
  - `patient.zeynep@clinic.test` (Zeynep Kaya, `ROLE_PATIENT`)
- **İstisnalar ve Randevular:**
  - `PARTIAL_LEAVE`, `OVERTIME`, `FULL_DAY_LEAVE` gelecek hafta aralıklarına konumlandırılmıştır.
  - 7 randevu tüm 5 statüyü (`COMPLETED` x2 geçmiş iş günü; `CONFIRMED` x1, `PENDING` x2, `REJECTED` x1, `CANCELLED` x1 sonraki iş günü) kapsar ve `Appointment` saf domain metotları (`approve`, `reject`, `cancel`, `complete`) + `Clock` ile üretilen kronolojik `AppointmentStatusHistoryJpaEntity` kayıtlarıyla tutarlıdır.

---

## 5. Güvenlik ve Üretim İzolasyonu (`DemoEnvironmentGuard`)

- `application.yml` içinden `spring.profiles.default: dev` kaldırıldığı için uygulama varsayılan profille başlatıldığında `DemoDataSeeder` kesinlikle çalışmaz.
- `DemoDataSeeder` ve servisleri `@Profile("dev")` ve `@ConditionalOnExpression` ile korunur.
- `DemoEnvironmentGuard`, aktif profilde `"dev"` bulunmasını, `prod`/`production`/`staging`/`stage`/`live`/`preprod`/`release` profillerinin bulunmamasını, `clinic.demo.seed.enabled` veya `clinic.demo.reset.enabled` opt-in bayrağını ve JDBC URL'sinin `prod`/`production`/`staging`/bulut RDS/Azure/Supabase/Neon anahtar kelimeleri içermeyen yerel PostgreSQL (`localhost`, `127.0.0.1`, `[::1]`, yerel container/bridge veya `jdbc:tc:postgresql:`) olmasını zorunlu kılar.
- `DemoDataProperties` ve `Credentials` `toString()` çıktıları `[REDACTED]` döndürür; hiçbir parola loglanmaz.

---

## 6. Güvenli Demo Sıfırlama Mekanizması

- HTTP/UI üzerinde hiçbir reset uç noktası açılmamıştır (`WebArchitectureTest` tarafından doğrulanır).
- `clinic.demo.reset.enabled` varsayılan olarak `false`'tur (`application.yml`, `application-dev.yml`, `application-test.yml`).
- Sıfırlama açıkça `dev` profili ve `--clinic.demo.reset.enabled=true` ile çalıştırılır; yalnızca `DemoDataCatalog` ve demo organizasyon/hesaplarına bağlı kayıtları tek `@Transactional` sınırında yabancı anahtar ters bağımlılık sırasıyla siler (`TRUNCATE` veya FK kapatma yoktur), diğer kurum/kullanıcı/randevu kayıtlarını korur ve hata durumunda tamamen rollback yapar.

---

## 7. Eşzamanlı Rezervasyon Stres Doğrulaması (`WP-S09-02`)

- **Test:** `src/test/java/com/example/clinic/ConcurrentBookingIT.java` (`AbstractIntegrationTest` PostgreSQL 17 Testcontainers).
- **Senaryo 1 (`fiveConcurrentThreadsCompetingForSameSlotProduceSingleBookingAndFourConflicts`):** 5 bağımsız hasta iş parçacığı `ExecutorService(5)` ve `CountDownLatch` kapılarıyla aynı anda aynı doktorun aynı `10:00` slotuna rezervasyon gönderdi; `1` adet `PENDING` randevu + `1` oluşturma audit geçmişi, `4` kontrollü `AppointmentConflictException` ve `0` beklenmeyen hata doğrulandı (`0.041 s`).
- **Senaryo 2 (`fiveConcurrentThreadsForDistinctSlotsAllSucceedWithoutErrors`):** 5 bağımsız hasta iş parçacığı aynı doktorun `09:00`..`11:00` arası 5 ardışık/çakışmasız slotuna aynı anda rezervasyon gönderdi; 5/5 başarı ve `0` hata doğrulandı (`0.058 s`).
- **Doğrulama Durumu:** `PASSED` (`ConcurrentBookingIT`: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`, toplam `0.102 s`).

---

## 8. Tam Regresyon Kapsam Envanteri ve Doğrulama Sonucu (`NFR-004`)

*Not: Aşağıdaki yürütme sayımları, ürün sahibinin kaynak kod değişikliği olmadan önce son düzenlemelerle tam uyumlu biçimde (`12:01:50` sonrası, `12:05:49–12:06:35` arasında) çalıştırdığı `./mvnw clean verify` terminal çıktısı ile diskteki `target/surefire-reports/` ve `target/failsafe-reports/` XML raporlarından birebir alınmıştır. Sohbet yapıştırması arayüz uzunluk sınırı nedeniyle Failsafe SQL günlükleri sırasında `<truncated 3120043 bytes>` noktasında kırpıldığından Failsafe detayları aynı çalıştırmanın diskteki `failsafe-summary.xml` ve `TEST-*IT.xml` raporlarından doğrulanmıştır.*

- **Derleme (`main` & `test`):** `144` ana kaynak dosyası ve `68` test kaynak dosyası Java 21 ile hatasız derlendi; `jar` ve `spring-boot:repackage` tamamlandı.
- **Surefire (`*Test.java`, `*Tests.java`):** `46` test sınıfı (`339` statik `@Test`/`@ParameterizedTest`/`@RepeatedTest` metot tanımı) — **`Tests run: 366, Failures: 0, Errors: 0, Skipped: 0`**
  - S09 Surefire sınıfları: `DemoEnvironmentGuardTest` (`4/4`, `0.052 s`), `DemoDataSeederUnitTest` (`6/6`, `0.022 s`), `PerformanceMetricsTest` (`2/2`, `0.003 s`), `WebArchitectureTest` (`3/3`, `0.003 s`), `DomainArchitectureTest` (`1/1`, `0.004 s`), `AppointmentArchitectureTest` (`5/5`, `0.009 s`), `ClinicAppointmentApplicationTests` (`1/1`, `6.458 s`).
- **Failsafe (`*IT.java` — PostgreSQL 17 Testcontainers):** `20` entegrasyon test sınıfı — `failsafe-summary.xml` üzerinde **`completed: 135, failures: 0, errors: 0, skipped: 0, flakes: 0`**
  - `DemoDataSeederIT`: `11/11` yeşil (`11.944 s` — tam veri grafiği, idempotentlik, BCrypt, kısmi veri, opt-in kapalıyken red, kapsamlı reset, non-demo koruma, hata rollback, uçtan uca booking/approval/cancellation).
  - `ConcurrentBookingIT`: `2/2` yeşil (`0.102 s`).
  - `PerformanceBenchmarkIT`: `1/1` yeşil (`1.454 s`).
  - Mevcut S01–S08 IT sınıfları (`OrganizationAdminServiceIT`: 4, `SecurityAuthorizationIT`: 8, `BookingServiceIT`: 13, `AppointmentApprovalServiceIT`: 32, `AppointmentConcurrencyIT`: 6, `AppointmentLifecycleConcurrencyIT`: 5, `AppointmentDatabaseConstraintIT`: 6, `AppointmentRepositoryIT`: 3, `CatalogManagementServiceIT`: 4, `CatalogDatabaseConstraintIT`: 8, `RoleBasedWebMvcIT`: 7, `DatabaseConstraintIT`: 5, `FlywayMigrationIT`: 3, `IdentityRepositoryIT`: 4, `OrganizationRepositoryIT`: 4, `StaffDatabaseConstraintIT`: 4, `StaffManagementServiceIT`: 5): tümü `0` hata / `0` atlama ile yeşil.
- **Toplam JUnit Yürütme:** `501` test koşumu (`366` Surefire + `135` Failsafe), `0` başarısızlık, `0` hata, `0` atlanan test, `0` `@Disabled`/`@Ignore`.

---

## 9. Yerel Performans Kıyaslama Sonuçları (`NFR-008`, `TC-044`)

- **Kıyaslama Sınıfı:** `src/test/java/com/example/clinic/PerformanceBenchmarkIT.java` (`localP95LatencyMeetsTargetUnderBoundedLoad`) + `PerformanceMetrics.java`
- **Veri Yükü:** 50 hekim, 20 hasta, 1.000 arka plan randevusu (PostgreSQL 17 Testcontainers).
- **Ölçüm Yöntemi:** JVM/Hibernate/Hikari ısınması sonrasında 4 eşzamanlı iş parçacığıyla 40 `AvailabilityCalculator.calculateAvailableSlots` çağrısı ve 24 çakışmasız `BookingService.bookAppointment` işlemi `System.nanoTime()` ile ölçülmüş, en yakın sıra (`nearest-rank`) yüzdelikleri hesaplanmıştır.
- **Ölçülen Yerel Kıyaslama Çıktısı (`TEST-com.example.clinic.PerformanceBenchmarkIT.xml` `<system-out>`):**
  - `availability-slot-search`: `total=40, success=40, failures=0, errorRate=0.0000, minMs=6.60, meanMs=9.21, p50Ms=7.34, p95Ms=21.86, p99Ms=23.73, maxMs=23.73` (`p95 = 21.86 ms < 1000.0 ms` ✅)
  - `booking-tx`: `total=24, success=24, failures=0, errorRate=0.0000, minMs=8.64, meanMs=11.64, p50Ms=11.17, p95Ms=14.74, p99Ms=15.82, maxMs=15.82` (`p95 = 14.74 ms < 1000.0 ms` ✅)
- **Performans Statüsü:** `VERIFIED_BY_OWNER` *(Her iki işlem de 0 hata oranı ve `< 25 ms` yerel p95 gecikmeyle `1000 ms` hedefinin çok altındadır.)*

---

## 10. Mimari Sınır Doğrulaması (`NFR-010`, `TC-046`)

- `DomainArchitectureTest`: `booking/domain`, `scheduling/domain` ve `common/domain/{Money,TimeInterval}.java` sınıflarının Spring, Jakarta Persistence, Hibernate, JDBC, `common.infrastructure` ve `.web` içe aktarımları taşımadığını doğrular.
- `AppointmentArchitectureTest`: `Appointment` aggregate kökünde güvensiz `setStatus` bulunmadığını, uygulama servislerinin web denetleyicilerine bağımlı olmadığını ve `AppointmentStatusHistoryJpaEntity` sütunlarının `updatable=false` olduğunu doğrular.
- `WebArchitectureTest`: `LoginController` dahil 9 MVC denetleyicisinin `@Controller`/`@RestController` taşıdığını, `JpaRepository` veya `DemoData*` servislerini enjekte etmediğini ve `@Autowired` alan enjeksiyonu kullanmadığını doğrular.

---

## 11. Ürün Sahibi Doğrulama Kanıtı ve Hijyen Kontrolü

```bash
./mvnw clean verify
git diff --check
git status --short
```

- `git diff --check`: çıkış kodu `0` (boşluk / birleştirme hatası yok).
- Surefire: `Tests run: 366, Failures: 0, Errors: 0, Skipped: 0`.
- Failsafe: `failsafe-summary.xml` -> `<completed>135</completed>`, `<errors>0</errors>`, `<failures>0</failures>`, `<skipped>0</skipped>`, `<flakes>0</flakes>`.
- Performans: `availability-slot-search p95Ms=21.86`, `booking-tx p95Ms=14.74`, `failures=0`.

---

## 12. Teslim Kapısı (Release Gate)

- **Git Commit & Push Durumu:** Tüm S09 kabul kapıları doğrulandı (`VERIFIED_BY_OWNER`); `src/` ve yapılandırma üzerinde test sonrası değişiklik yapılmadan yalnızca dokümantasyon doğrulama bulgularıyla tamamlanıp `main` dalına `[skip ci] [skip actions]` ile commit ve push aşamasına geçilmiştir.
