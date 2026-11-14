# Gereksinim ve izlenebilirlik matrisi

**Kaynak:** öğretim elemanının iletilen Seçenek A ödev metni. **MUST** = zorunlu akademik/mimari hedef; **SHOULD** = güçlü öneri; **COULD** = zaman kalırsa.

| ID | Seviye | Doğrulanabilir gereksinim | Sprint | Test |
|---|---|---|---|---|
| FR-001 | MUST | Yönetici kurum tanımlayabilir ve güncelleyebilir | S03 | TC-001 |
| FR-002 | MUST | Kurum çok sayıda şube tanımlayabilir; tek şube de çalışır | S03 | TC-002 |
| FR-003 | MUST | Şube haftalık çalışma saatleri yönetilir | S03 | TC-003 |
| FR-004 | MUST | Şubede bölümler tanımlanır | S03 | TC-004 |
| FR-005 | MUST | Katalog hizmeti adı ve açıklaması tanımlanır | S03 | TC-005 |
| FR-006 | MUST | Şubede hizmet süresi dakika ve ücret BigDecimal olarak tanımlanır | S03 | TC-006 |
| FR-007 | MUST | Doktor/sağlık personeli kaydı ve personel türü tutulur | S05 | TC-007 |
| FR-008 | MUST | Doktorun uzmanlık alanı tutulur | S05 | TC-008 |
| FR-009 | MUST | Çalışanın sağlayabileceği hizmetler şube bazında tanımlanır | S05 | TC-009 |
| FR-010 | MUST | Çalışanın haftalık uygunluk aralıkları tanımlanır | S05 | TC-010 |
| FR-011 | MUST | Tarih özelinde izin ve mesai istisnası uygulanır | S05 | TC-011 |
| FR-012 | MUST | Hasta yalnızca uygun saat aralığında rezervasyon yapar | S06 | TC-012 |
| FR-013 | MUST | Hasta hizmet, çalışan, şube ve tarih/saat seçer | S06 | TC-013 |
| FR-014 | MUST | Çalışan için zaman çakışması engellenir | S06 | TC-014 |
| FR-015 | MUST | Eşzamanlı iki istekte çift rezervasyon engellenir | S06 | TC-015 |
| FR-016 | MUST | Randevuda işlem/süre/ücret/çalışan/tarih-saat saklanır | S06 | TC-016 |
| FR-017 | MUST | Randevu başlangıç durumunda PENDING olur | S06 | TC-017 |
| FR-018 | MUST | Yetkili çalışan/yönetici randevuyu onaylar | S07 | TC-018 |
| FR-019 | MUST | Yetkili kişi randevuyu reddedebilir | S07 | TC-019 |
| FR-020 | MUST | Hasta kendi randevusunu iptal edebilir | S07 | TC-020 |
| FR-021 | MUST | Personel geçmiş randevuyu tamamlandı işaretleyebilir | S07 | TC-021 |
| FR-022 | MUST | Hasta rolü yalnız kendi randevularını görür | S04 | TC-022 |
| FR-023 | MUST | Personel rolü yalnız yetkili şube/kendi programını görür | S04 | TC-023 |
| FR-024 | MUST | Yönetici rolü şube/kurum yönetimini yapar | S04 | TC-024 |
| FR-025 | MUST | Kimlik doğrulama ve parola hash sistemi vardır | S04 | TC-025 |
| FR-026 | MUST | Yetkisiz kuruma/şubeye erişim engellenir | S04 | TC-026 |
| FR-027 | MUST | Hasta ve personel için ekran akışları vardır | S08 | TC-027 |
| FR-028 | MUST | Yönetim ekranları kurum/şube/hizmet/saat/personel yönetir | S08 | TC-028 |
| FR-029 | SHOULD | Hasta aynı anda iki randevuda bulunamaz | S06 | TC-029 |
| FR-030 | SHOULD | Randevu ve onay hareketi olay geçmişine yazılır | S07 | TC-030 |
| FR-031 | SHOULD | Randevu tekrar gönderiminde idempotency desteklenir | S06 | TC-031 |
| FR-032 | SHOULD | İşlem fiyat/süre snapshot değerleri değişmeden korunur | S06 | TC-032 |
| FR-033 | SHOULD | Zaman dilimi doğru dönüştürülür | S06 | TC-033 |
| FR-034 | SHOULD | Demo seed ve demo reset prosedürü vardır | S09 | TC-034 |
| FR-035 | COULD | Kullanıcı için uygulama içi bildirimler vardır | S10 | TC-035 |
| FR-036 | COULD | Hizmet/şube rapor filtreleri vardır | S10 | TC-036 |
| NFR-001 | MUST | Şifre plaintext saklanmaz, oturum ve CSRF korunur | S04 | TC-037 |
| NFR-002 | MUST | Müşteri/kurum düzeyinde erişim kontrolleri servis katmanında uygulanır | S04 | TC-038 |
| NFR-003 | MUST | Flyway ile sıfırdan şema kurulabilir ve migration tekrarlanabilir | S01 | TC-039 |
| NFR-004 | MUST | Kritik iş kuralı unit ve PostgreSQL integration testleri bulunur | S09 | TC-040 |
| NFR-005 | MUST | Sistem baştan demo verisi ile ayağa kaldırılabilir | S09 | TC-041 |
| NFR-006 | MUST | En az 10 farklı gerçek günde commit kanıtı vardır | S10 | TC-042 |
| NFR-007 | MUST | GitHub + SABİS + canlı sunum teslimleri yapılır | S10 | TC-043 |
| NFR-008 | SHOULD | Randevu p95 yanıt süresi yerel örnek yükte 1sn altı hedeflenir | S09 | TC-044 |
| NFR-009 | SHOULD | Hata yanıtları güvenli ve kullanıcıya açıklayıcıdır | S08 | TC-045 |
| NFR-010 | SHOULD | Kod test edilebilir ve katman sınırları doğrulanır | S09 | TC-046 |

## Kabul yönetimi
- Öğretim elemanının gereksinimleri (kurum, saat, bölümler, hizmet ücret/süre, çalışan, uzmanlık/yetki, uygunluk, seçme, çakışma, randevu kaydı, onay, üç rol) MUST olarak modellenmiştir.
- Görev ID ve test ID bir uygulama oluşturulduğu anlamına gelmez; geliştirme sırasında kanıt bağlantısı işlenir.
- `06-kalite/02-test-senaryolari.md` dosyasındaki TC kayıtları ile çapraz kontrol yapılır.
- `07-sprintler` dosyalarından her bir sprintin Done kriterlerine bağlanır.
- **Sprint S02 İzlenebilirlik Notu:** S02 kapsamında `Appointment`, `TimeInterval`, `ServiceSnapshot` ve `Money` ile FR-016 ve TC-016'nın bellek-içi zengin domain temeli (`DOMAIN_FOUNDATION_IMPLEMENTED`) kurulmuş; veritabanı şeması ve repository kalıcılığı ise planlandığı gibi S06'ya (`PERSISTENCE_DEFERRED_TO_S06`) bırakılmıştır. NFR-010/TC-046 için paket mimari sınır testi (`DomainArchitectureTest`) ile saf domain çekirdeği doğrulanmıştır.
- **Sprint S03 İzlenebilirlik Notu:** S03 kapsamında FR-001..FR-006 ve TC-001..TC-006 için V2 Flyway migrasyonu (`V2__create_catalog_tables.sql`), domain modelleri (`ServiceDefinition`, `BranchService`), kalıcılık repository'leri (`ServiceDefinitionRepository`, `BranchServiceRepository`), uygulama servisleri (`OrganizationAdminService`, `CatalogManagementService`) ve çok kiracılı kompozit veri bütünlüğü güvenceleri inşa edilmiştir (`DOMAIN_IMPLEMENTED`, `PERSISTENCE_IMPLEMENTED`, `APPLICATION_SERVICE_IMPLEMENTED`, `UI_DEFERRED`). Test sınıfları yazılmış olup geliştirici manuel doğrulaması beklenmektedir (`OWNER_TEST_PENDING`).
- **Sprint S07 İzlenebilirlik Notu:** S07 kapsamında FR-018, FR-019, FR-020, FR-021 ve FR-030 için durum makinesi (`Appointment` saf domain metotları `approve`, `reject`, `cancel`, `complete`), uygulama servisi (`AppointmentApprovalService`), PostgreSQL satır kilidi (`AppointmentJpaRepository.findByIdForUpdate`), deterministik ikincil anahtarlı append-only denetim geçmişi (`AppointmentStatusHistoryJpaRepository`), slot serbest bırakma entegrasyonu (ADR-003) ve AccessPolicy çok kiracılı yetkilendirmesi inşa edilmiştir (`DOMAIN_IMPLEMENTED`, `APPLICATION_SERVICE_IMPLEMENTED`, `PERSISTENCE_IMPLEMENTED`, `UI_DEFERRED_TO_S08`). Birim, entegrasyon, mimari ve eşzamanlılık testleri yazılmış olup ürün sahibinin manuel doğrulaması beklenmektedir (`CODE_COMPLETE_AWAITING_OWNER_TEST`).
- **Sprint S08 İzlenebilirlik Notu:** S08 kapsamında FR-027, FR-028 ve NFR-009 için üç rolün (Hasta, Hekim, Yönetici) sunucu taraflı render edilen (SSR) Thymeleaf kullanıcı arayüzleri, ince kaynak bazlı Spring MVC denetleyicileri (`PatientBookingController`, `StaffScheduleController`, `AdminDashboardController`, `AdminBranchController`, `AdminDepartmentController`, `AdminServiceController`, `AdminStaffController`, `CustomErrorController`), form doğrulama modelleri (`BookingForm`, `CancellationForm`, `RejectionForm`, `OrganizationUpdateForm`, `BranchForm`, `BranchEditForm`, `BranchHoursForm`, `DepartmentForm`, `DepartmentEditForm`, `ServiceDefinitionForm`, `ServiceDefinitionEditForm`, `BranchServiceForm`, `ServicePricingForm`, `ServiceDurationForm`, `StaffRegistrationForm`, `StaffAssignmentForm`, `StaffEligibilityForm`, `WeeklyAvailabilityForm`, `StaffExceptionForm`), sunum sorgu ve hata eşleme servisleri (`AppointmentQueryService`, `PatientBookingQueryService`, `WebErrorMessageMapper`), yerel Bootstrap 5 + `clinic.css`/`clinic.js` varlıkları ve güvenli hata sayfaları (`400`, `403`, `404`, `500`) inşa edilmiştir (`VERIFIED_BY_OWNER`). Depoda toplam 88 S08 test metodu (81 birim/MVC/mimari + 7 `RoleBasedWebMvcIT` entegrasyon testi) ve tüm proje paketi ürün sahibi tarafından `9c35715` commit'inde `./mvnw clean verify` ile doğrulanmıştır (`BUILD SUCCESS`, Failsafe `121/121` yeşil, Surefire kapısı hatasız geçildi).
- **Sprint S09 İzlenebilirlik Notu:** S09 kapsamında FR-034, NFR-004, NFR-005, NFR-008 ve NFR-010 için varsayılan profilden yalıtılmış `@Profile("dev")` demo altyapısı (`DemoDataSeeder`, `DemoDataCatalog`, `DemoDataProperties`, `DemoEnvironmentGuard`, `DemoDataSeedService`, `DemoDataResetService`, `DemoDataResetCompletedEvent`), 5 iş parçacıklı `ConcurrentBookingIT`, 50 personel / 1.000 randevulu `PerformanceBenchmarkIT` + `PerformanceMetrics`/`PerformanceMetricsTest`, genişletilmiş mimari testleri (`DomainArchitectureTest`, `WebArchitectureTest`) ve `DemoDataSeederIT` + `DemoEnvironmentGuardTest`/`DemoDataSeederUnitTest` uygulanmıştır (`VERIFIED_BY_OWNER`). Ürün sahibi tarafından çalıştırılan `./mvnw clean verify` sonucunda Surefire `366/366` ve Failsafe PostgreSQL 17 Testcontainers `135/135` (`DemoDataSeederIT` 11/11, `ConcurrentBookingIT` 2/2, `PerformanceBenchmarkIT` 1/1) sıfır hata ve sıfır atlamayla geçmiş; `availability-slot-search p95=21.86 ms` ve `booking-tx p95=14.74 ms` (`errorRate=0.0000`) ile `NFR-008` `< 1000 ms` hedefi doğrulanmıştır.
- **Sprint S10 İzlenebilirlik Notu (Final — `VERIFIED_BY_OWNER`):** S10 kapsamında `NFR-006 / TC-042` (10 farklı gerçek takvim gününde GitHub commit denetimi) **2026-11-02 .. 2026-11-14 arasında 10 tekil gün** ile doğrulanmıştır (`GIT_10_DAY_REQUIREMENT: PASS`). `2026-11-14 12:39:21–12:40:07+03:00` tarihinde ürün sahibi tarafından çalıştırılan `./mvnw clean verify` çıktısı ve diskteki Surefire/Failsafe XML raporlarıyla **46/46 Surefire (`366` test)** + **20/20 Failsafe PostgreSQL 17 Testcontainers (`135` test)** = **`501` test `0` hata/`0` atlama** ile doğrulanmıştır (`ConcurrentBookingIT` `2/2` `0.177 s`, `PerformanceBenchmarkIT` `availability-slot-search p95=14.83 ms`, `booking-tx p95=14.40 ms`, `errorRate=0.0000`). `NFR-007 / TC-043` teslimatları üretilip GitHub `main` dalına senkronize edilmiştir; SABİS yükleme ve canlı savunma öğrenci aksiyonundadır. `FR-031` ertelenmiş (`DEFERRED`), `COULD` öncelikli `FR-035` ve `FR-036` kapsam dondurması nedeniyle uygulanmamıştır.
