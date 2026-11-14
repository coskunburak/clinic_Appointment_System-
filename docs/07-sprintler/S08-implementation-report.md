# Sprint S08 Uygulama Raporu: Rol Tabanlı Spring MVC ve Thymeleaf Kullanıcı Arayüzleri

- **Rapor Tarihi:** 2026-11-14
- **Sprint Kodu:** S08
- **Sprint Başlığı:** Complete Role-Based Spring MVC, Server-Side Rendered Thymeleaf 3 User Interfaces (Patient, Staff, Admin), Offline Local Bootstrap 5 Design System, Form Validation, and Secure Error Handling
- **Sorumlu:** Principal Java/Spring Boot Architect, Senior Spring MVC/Thymeleaf Engineer, Application Security Architect, PostgreSQL Engineer, Accessibility Engineer, QA Architect, Academic OOP Compliance Reviewer
- **Sprint Statüsü:** `CODE_COMPLETE_AWAITING_OWNER_TEST` *(Akademik Kural 1.1 gereğince hiçbir derleme, Testcontainers, Flyway veya Maven test komutu ajan tarafından çalıştırılmamıştır. Tüm çalışma zamanı doğrulamaları ürün sahibinin manuel çalıştırmasını beklemektedir.)*

---

## 1. Sprint Genel Bakış (Sprint Overview)

Sprint S08'in temel hedefleri:
1. Seçenek A şartnamesinde zorunlu kılınan üç kullanıcı rolü (**Hasta**, **Doktor / Sağlık Personeli**, **Hastane / Klinik Yöneticisi**) için sunucu taraflı işlenen (SSR), erişilebilir ve duyarlı (responsive) Thymeleaf 3 ekranlarını eksiksiz biçimde teslim etmek (`FR-027`, `FR-028`, `TC-027`, `TC-028`).
2. Tek parçalı (monolitik) yönetici controller yaklaşımını kaynak odaklı 5 ince controller sınıfına (`AdminDashboardController`, `AdminBranchController`, `AdminDepartmentController`, `AdminServiceController`, `AdminStaffController`) bölerek sorumluluk ayrımını sağlamak ve controller sınıflarını doğrudan `JpaRepository` erişiminden arındırmak.
3. S03–S07 arasında geliştirilen kurum/şube, hizmet kataloğu, personel uygunluk, pesimist satır kilitli randevu rezervasyonu ve durum makinesi servislerini gerçek MVC formları, kapsamlandırılmış sorgular (`@EntityGraph`, `AccessPolicy` tenant sınırları) ve `BookingService` / `AppointmentApprovalService` sözleşmeleriyle bütünleştirmek.
4. Harici CDN veya Google Fonts bağımlılığı olmadan yerel Bootstrap 5, merkezi CSS tasarım token'ları (`clinic.css`) ve harici CSP-dostu etkileşim betiği (`clinic.js`) ile tutarlı bir klinik tasarım sistemi oluşturmak.
5. `400`, `403`, `404` ve `500` HTTP durumları için yığın izi (stack trace), SQL veya sistem detayı sızdırmayan kullanıcı dostu özel hata sayfalarını ve merkezi hata eşlemeyi (`CustomErrorController`, `WebExceptionHandler`, `WebErrorMessageMapper`) devreye almak (`NFR-009`, `TC-045`).

---

## 2. Başlangıç Durumu ve Çizgi (Baseline & Repository State)

- **Depo (Repository):** `https://github.com/coskunburak/clinic_Appointment_System-`
- **Etkin Dal (Branch):** `main`
- **S07 Doğrulanmış Temel (Verified Baseline):** `9c81d86590160a5ad6e675b4531ced087df11dbb` (`VERIFIED_BY_OWNER` — 272 birim testi + 114 entegrasyon testi = 386 geçen test)
- **Bu Tamamlama Turunun Başlangıç HEAD SHA'sı:** `1fe6b389824b482548ee302c7c374141a40f2ccd` (`docs(s08): record owner-verified test success and complete S08 milestone [skip ci] [skip actions]`)
- **Korunan Çekirdek ve Teknoloji Tabanı:** Java 21, Spring Boot 3.4.2, Spring MVC, Spring Security 6.4, Thymeleaf 3, Jakarta Bean Validation, Spring Data JPA, PostgreSQL 17, Flyway Community (`V1`–`V4` migrasyonları değiştirilmeden korundu), `pom.xml` sürüm ve bağımlılıkları korundu.

---

## 3. Sunum Katmanı Rotaları ve Servis Sözleşmeleri (Screen & Endpoint Matrix)

| Ekran / Rota | Controller | Çağrılan Uygulama Servisleri / Yetenekler | İşlevi |
|---|---|---|---|
| `GET /patient/dashboard` | `PatientBookingController` | `PatientBookingQueryService`, `AppointmentQueryService` | Hasta özet paneli ve hızlı randevu erişimi |
| `GET /patient/book` | `PatientBookingController` | `PatientBookingQueryService` (aktif şubeler, şube hizmetleri, uygun personel) | Şube, hizmet ve hekim hiyerarşisine göre rezervasyon formu |
| `GET /patient/slots` | `PatientBookingController` | `PatientBookingQueryService` -> `AvailabilityCalculator` | Seçilen şube/hizmet/hekim/tarih için müsait slot JSON uç noktası |
| `POST /patient/book` | `PatientBookingController` | `BookingService.bookAppointment(BookingCommand)` | Pesimist kilitli atomik randevu oluşturma (çakışma hataları doğrulama bağlamında form görünümünde yönetilir) |
| `GET /patient/appointments` | `PatientBookingController` | `AppointmentQueryService` (sayfalanmış hasta sorgusu) | Hastanın yaklaşan ve geçmiş randevularını listeleme |
| `POST /patient/appointments/{id}/cancel` | `PatientBookingController` | `AppointmentApprovalService.cancelAppointment` | Başlangıç öncesi gerekçeli iptal ve slot serbest bırakma |
| `GET /staff/dashboard` | `StaffScheduleController` | `AppointmentQueryService` | Hekim/personel özet paneli |
| `GET /staff/schedule` | `StaffScheduleController` | `AppointmentQueryService`, `WeeklyDayScheduleDto` | Günlük/haftalık ajanda, şube yerel saat penceresi ve bekleyen onay kuyruğu |
| `POST /staff/appointments/{id}/approve` | `StaffScheduleController` | `AppointmentApprovalService.approveAppointment` | `PENDING` -> `CONFIRMED` geçişi |
| `POST /staff/appointments/{id}/reject` | `StaffScheduleController` | `AppointmentApprovalService.rejectAppointment` | Gerekçeli `REJECTED` geçişi ve slot serbest bırakma |
| `POST /staff/appointments/{id}/complete` | `StaffScheduleController` | `AppointmentApprovalService.completeAppointment` | `endAtUtc` sonrası `CONFIRMED` -> `COMPLETED` geçişi |
| `GET /admin`, `GET /admin/dashboard` | `AdminDashboardController` | `OrganizationAdminService`, `CatalogManagementService`, `StaffManagementService` | Yetkili kurum/şube kapsamlı yönetim özeti |
| `POST /admin/organizations/{id}/edit` | `AdminBranchController` | `OrganizationAdminService` | Yetkili kurum bilgisi güncelleme (`OrganizationUpdateForm`) |
| `GET/POST /admin/branches/**` | `AdminBranchController` | `OrganizationAdminService` (`updateBranch`, açılış/kapanış saatleri) | Şube CRUD, durum değiştirme (`BranchEditForm`) |
| `GET/POST /admin/hours`, `/admin/branches/{id}/hours/**` | `AdminBranchController` | `OrganizationAdminService` (`configureOpeningHours`, hafta günü silme) | Şube haftalık mesai saatleri yönetimi |
| `GET/POST /admin/departments/**` | `AdminDepartmentController` | `OrganizationAdminService` (`createDepartment`, `updateDepartment`, `activateDepartment`, `deactivateDepartment`) | `branchId` korunarak poliklinik/bölüm CRUD ve durum geçişleri |
| `GET/POST /admin/services/**` | `AdminServiceController` | `OrganizationAdminService.getOrgWideAuthorizedOrganizations`, `CatalogManagementService.updateServiceDefinition` | Kurum kapsamlı katalog yönetimi, satır içi `ServicePricingForm` / `ServiceDurationForm` güncellemeleri |
| `GET/POST /admin/staff/**` | `AdminStaffController` | `StaffManagementService` (`getAuthorized*`, `deleteWeeklyAvailability`, `deleteAvailabilityException`) | Kapsamlandırılmış personel kaydı (`DOCTOR` uzmanlık kontrolü), `?assignmentId=` seçimi, uygunluk, haftalık mesai ve izin/fazla mesai yönetimi |

---

## 4. Oluşturulan ve Güncellenen Kaynak Dosyaları Envanteri

### A. Controller Sınıfları (`com.example.clinic.web.*`)
- `src/main/java/com/example/clinic/web/patient/PatientBookingController.java`
- `src/main/java/com/example/clinic/web/staff/StaffScheduleController.java`
- `src/main/java/com/example/clinic/web/admin/AdminDashboardController.java`
- `src/main/java/com/example/clinic/web/admin/AdminBranchController.java`
- `src/main/java/com/example/clinic/web/admin/AdminDepartmentController.java`
- `src/main/java/com/example/clinic/web/admin/AdminServiceController.java`
- `src/main/java/com/example/clinic/web/admin/AdminStaffController.java`
- *(Monolitik `AdminManagementController.java` kaldırılarak yukarıdaki 5 odaklı yönetici controller sınıfına ayrıştırılmıştır.)*

### B. Ortak Web Altyapısı, Hata Yönetimi ve Form DTO'ları (`com.example.clinic.web.*`)
- **Hata & Ortak Bileşenler:**
  - `src/main/java/com/example/clinic/web/error/CustomErrorController.java`
  - `src/main/java/com/example/clinic/web/error/WebExceptionHandler.java`
  - `src/main/java/com/example/clinic/web/common/GlobalModelAttributesAdvice.java`
  - `src/main/java/com/example/clinic/web/common/WebErrorMessageMapper.java`
- **Doğrulamalı Form DTO'ları (19 sınıf — `com.example.clinic.web.form`):**
  - Hasta & Personel: `BookingForm.java`, `CancellationForm.java`, `RejectionForm.java`
  - Yönetici Kurum & Şube: `OrganizationUpdateForm.java`, `BranchForm.java`, `BranchEditForm.java`, `BranchHoursForm.java`, `DepartmentForm.java`, `DepartmentEditForm.java`
  - Yönetici Katalog: `ServiceDefinitionForm.java`, `ServiceDefinitionEditForm.java`, `BranchServiceForm.java`, `ServicePricingForm.java`, `ServiceDurationForm.java`
  - Yönetici Personel & Mesai: `StaffRegistrationForm.java`, `StaffAssignmentForm.java`, `StaffEligibilityForm.java`, `WeeklyAvailabilityForm.java`, `StaffExceptionForm.java`

### C. Sunum Sorgu Servisleri, Uygulama Katmanı ve Güvenlik Güncellemeleri
- **Sunum Sorgu Servisleri ve DTO'lar (`com.example.clinic.booking.application`):**
  - `AppointmentQueryService.java`, `PatientBookingQueryService.java`
  - `dto/AppointmentViewDto.java`, `dto/BranchSummaryDto.java`, `dto/ServiceSummaryDto.java`, `dto/StaffSummaryDto.java`, `dto/WeeklyDayScheduleDto.java`
- **Yetkilendirme, Uygulama Servisleri ve Repository Sertleştirmeleri:**
  - `src/main/java/com/example/clinic/identity/application/AccessPolicy.java`: `getAdminAuthorizedBranchIds(UserAccount)` ve `getAdminAuthorizedOrganizationWideIds(UserAccount)` eklendi.
  - `src/main/java/com/example/clinic/organization/application/OrganizationAdminService.java`: `getOrgWideAuthorizedOrganizations`, `updateBranch`, `updateDepartment`, `activateDepartment`, `deactivateDepartment` eklendi/sertleştirildi.
  - `src/main/java/com/example/clinic/catalog/application/CatalogManagementService.java`: `updateServiceDefinition` ve org-wide katalog kapsamı sertleştirildi.
  - `src/main/java/com/example/clinic/staff/application/StaffManagementService.java`: `deleteWeeklyAvailability`, `deleteAvailabilityException` ve tenant kapsamlı `getAuthorizedStaffProfile`, `getAuthorizedStaffProfiles`, `getAuthorizedStaffAssignments*`, `getAuthorizedEligibilitiesByAssignment`, `getAuthorizedWeeklyAvailabilitiesByAssignment`, `getAuthorizedAvailabilityExceptionsByAssignment`, `enforceAdminOrStaffSelfAccess` eklendi.
  - `@EntityGraph` ve kapsamlandırılmış sorgularla güncellenen repository arayüzleri: `BranchRepository`, `BranchServiceRepository`, `StaffServiceEligibilityRepository`, `StaffAssignmentRepository`, `StaffProfileRepository` (`findAuthorizedStaffByBranchIds`), `AppointmentJpaRepository` (`Page<AppointmentJpaEntity> findByPatientIdOrderByStartAtUtcDesc`, `findByStaffIdAndStatusOrderByStartAtUtcAsc`).
  - `SecurityConfig.java`, `LoginController.java`.

### D. Thymeleaf 3 Şablonları ve Yerel Ön Yüz Varlıkları (`src/main/resources/`)
- **Yerel CSS / JS / Vendor Varlıkları (Zero CDN):**
  - `static/css/bootstrap.min.css`, `static/css/clinic.css`
  - `static/js/bootstrap.bundle.min.js`, `static/js/clinic.js`
  - `static/vendor/bootstrap/css/bootstrap.min.css`, `static/vendor/bootstrap/js/bootstrap.bundle.min.js`
- **Thymeleaf Şablonları (`templates/`):**
  - Yerleşim & Kimlik Doğrulama: `fragments/layout.html`, `auth/login.html`
  - Güvenli Hata Sayfaları: `error/400.html`, `error/403.html`, `error/404.html`, `error/500.html`
  - Hasta Ekranları: `patient/dashboard.html`, `patient/book.html`, `patient/appointments.html`
  - Personel Ekranları: `staff/dashboard.html`, `staff/schedule.html`
  - Yönetici Ekranları: `admin/dashboard.html`, `admin/branches.html`, `admin/branch-hours.html`, `admin/departments.html`, `admin/services.html`, `admin/staff.html`, `admin/staff-availability.html`

---

## 5. Mimari Kararlar ve Kritik Düzeltmeler

1. **İnce Resource Controller Mimarisi:** `AdminManagementController` yerine 5 odaklı controller'a bölünerek her yönetici alanı kendi form bağlamına ve doğrulama kurallarına sahip olmuştur. `WebArchitectureTest` ile 8 controller sınıfının `@Controller` olduğu ve hiçbirinin `JpaRepository` bağımlılığı taşımadığı mimari test seviyesinde tanımlanmıştır.
2. **Tenant & Şube Kapsam İzolasyonu:** `AccessPolicy` üzerinden şube ve kurum-geneli yetkiler ayrılmıştır. `AdminServiceController` kurum kataloğu okumasını `getOrgWideAuthorizedOrganizations()` ile sınırlar; `AdminStaffController` ise tüm personel profillerini değil `getAuthorized*()` metodlarını çağırır.
3. **Sorgu Doğruluğu, Zaman Dilimi ve Performans:**
   - `AppointmentQueryService` ve `WeeklyDayScheduleDto`, şube yerel gece yarısından (`branchLocalDate`) UTC penceresine (`UTC midnight` hatası giderildi) doğru dönüşüm yapar; `pending` kuyruğunu ayrıca taşır; `canComplete`/`isPast` bayraklarını tam `endAtUtc` sınırında hesaplar ve toplu lookup'larda `findById` yedeklemesi sunar.
   - `PatientBookingQueryService`, `Clock`-duyarlı şube yerel varsayılan tarihini kullanır ve aktif kurum/şube/bölüm/hizmet/personel hiyerarşisini doğrular.
   - N+1 sorgularını önlemek için repository katmanında `@EntityGraph` ve sayfalanmış/kapsamlı sorgular kullanılmıştır.
4. **Güvenli Hata İşleme ve Doğrulama Sertleştirmeleri (`NFR-009`):**
   - `CustomErrorController`, `WebExceptionHandler` ve `WebErrorMessageMapper`, `400/403/404/500` hatalarını güvenli ve anlaşılır Türkçe mesajlarla eşler; `AvailabilityException` saat sırası ve kısmi izin (`PARTIAL_LEAVE`/`OVERTIME`) çelişkilerini net biçimde raporlar; `FULL_DAY_LEAVE` seçildiğinde saat alanları temizlenir.
   - `com.example.clinic.web` paketinde geniş `catch (Exception` blokları tamamen ortadan kaldırılmıştır (`AdminBranchController` tarih ayrıştırması için özgül `catch (DateTimeException e)` kullanır).
   - `AdminStaffController`, `DOCTOR` rolü için uzmanlık alanı doğrulamasını zorunlu kılar ve `?assignmentId=` üzerinden şube görevlendirmeleri arası geçişe izin verir.
5. **Ön Yüz, Erişilebilirlik ve Güvenlik Hijyeni:**
   - `<script>` etiketleri yalnızca `templates/fragments/layout.html` sonundaki yerel Bootstrap ve `clinic.js` yüklemeleriyle sınırlıdır; Google Fonts veya harici CDN çağrısı yoktur.
   - `clinic.js` içinde referans verilen 25 DOM kimliği ve seçicisi şablonlarla doğrulanmıştır; `#editOrganizationModal`, `#editBranchModal`, `#editDepartmentModal`, `#editServiceDefModal` modalları, CSRF gizli alanları, erişilebilir `<label>` etiketleri ve hafta günü/görevlendirme silme düğmeleri eklenmiştir.
   - Şablonlarda `th:utext` veya satır içi `onerror=` yer almaz; XSS yüzeyi kapalı tutulmuştur.

---

## 6. S08 Test Envanteri ve Sayısal Özeti (Authoring Summary — Not Executed by Agent)

> [!IMPORTANT]
> Akademik Kural 1.1 gereğince hiçbir Maven, JUnit veya Testcontainers komutu ajan tarafından **çalıştırılmamıştır**. Aşağıdaki sayılar kaynak dosyalardaki statik `@Test` / `@ParameterizedTest` / `@RepeatedTest` anotasyon sayılarına dayanmaktadır.

### S08 Kapsamında Eklenen / Genişletilen Test Sınıfları

| Test Sınıfı | Test Türü | S08 `@Test` / Sınıf Toplamı | Kapsadığı Gereksinim ve Senaryolar |
|---|---|---|---|
| `Sprint08AcceptanceSpecificationTest` | `@WebMvcTest` | `5` / `5` | `FR-027`, `FR-028`, `NFR-009`, `TC-027`, `TC-028`, `TC-045` kabul senaryoları |
| `PatientBookingControllerMvcTest` | `@WebMvcTest` | `15` / `15` | Hasta rezervasyon formu, slot JSON endpoint'i, çakışma/hata yönetimi, sayfalama, iptal, CSRF |
| `StaffScheduleControllerMvcTest` | `@WebMvcTest` | `11` / `11` | Hekim günlük/haftalık ajanda, onay, gerekçeli red, `endAtUtc` sonrası tamamlama, CSRF |
| `AdminDashboardControllerMvcTest` | `@WebMvcTest` | `3` / `3` | Yönetici özet paneli erişimi ve yetki sınırları |
| `AdminBranchControllerMvcTest` | `@WebMvcTest` | `8` / `8` | Kurum/şube düzenleme, oluşturma, aktifleştirme/pasifleştirme, mesai saatleri ve weekday silme |
| `AdminDepartmentControllerMvcTest` | `@WebMvcTest` | `5` / `5` | Bölüm oluşturma, düzenleme, `branchId` korunarak aktif/pasif yapma |
| `AdminServiceControllerMvcTest` | `@WebMvcTest` | `5` / `5` | Org-wide katalog kapsamı, hizmet tanımı düzenleme, fiyat ve süre formları |
| `AdminStaffControllerMvcTest` | `@WebMvcTest` | `6` / `6` | Personel kaydı (`DOCTOR` uzmanlık), `?assignmentId=` görevlendirme seçimi, haftalık/istisna silme |
| `CustomErrorControllerMvcTest` | `@WebMvcTest` | `5` / `5` | `400`, `403` (`WebAttributes.ACCESS_DENIED_403`), `404`, `500` ve `WebExceptionHandler` güvenli yanıtları |
| `AppointmentQueryServiceTest` | Unit (Mockito) | `4` / `4` | UTC->şube yerel pencere, `pending` kuyruğu, `endAtUtc` sınırları, `findById` fallback |
| `PatientBookingQueryServiceTest` | Unit (Mockito) | `6` / `6` | `Clock`-duyarlı varsayılan tarih, aktif hiyerarşi filtresi ve uygunluk slotları |
| `OrganizationAdminServiceTest` | Unit (Mockito) | `+3` / `18` | `getOrgWideAuthorizedOrganizations`, şube/bölüm güncelleme ve durum geçişleri |
| `CatalogManagementServiceTest` | Unit (Mockito) | `+1` / `12` | `updateServiceDefinition` ve org-wide katalog kapsam doğrulaması |
| `StaffManagementServiceTest` | Unit (Mockito) | `+2` / `9` | Tenant-kapsamlı personel okumaları (`enforceAdminOrStaffSelfAccess` ve yetkili liste sorguları) |
| `WebArchitectureTest` | Architecture Test | `2` / `2` | 8 controller'ın `@Controller` olması ve sıfır doğrudan `JpaRepository` bağımlılığı |
| `RoleBasedWebMvcIT` | PostgreSQL E2E IT | `7` / `7` | Uçtan uca randevu, red tarihçesi + `/patient/slots` anında serbest bırakma, çapraz-kiracı IDOR 403, çakışmada form yanıtı, navbar rol görünürlüğü |
| **S08 Odaklı Toplam** | | **`88`** | **81 Birim/MVC/Mimari `@Test` + 7 `RoleBasedWebMvcIT` E2E `@Test`** |

*(Eski monolitik `AdminManagementControllerMvcTest.java` kaldırılmıştır.)*

- **Tüm Depo Statik Test Envanteri:** 60 test dosyasında toplam **447** test anotasyonu (**326** birim/MVC/mimari + **121** PostgreSQL entegrasyon testi).
- **Not (Statik Sayım vs. Surefire Çalıştırma Sayısı):** S07 doğrulamasında yer alan 7 adet `@ParameterizedTest` metodu çalışma zamanında birden fazla veri kümesine açıldığı için Surefire raporundaki koşulan senaryo sayısı statik anotasyon sayısından biraz farklı görünebilir. Gerçek yeşil/kırmızı test sonucu ürün sahibinin çalıştırmasıyla teyit edilecektir.

---

## 7. Gereksinim ve Kabul Testi İzlenebilirliği (Traceability Matrix)

| Kimlik | Tür | Kapsam Özeti | S08 Kaynak & Test Eşlemesi | Durum |
|---|---|---|---|---|
| **FR-027** | Gereksinim | Hasta ve klinisyen/personel uçtan uca MVC akışları | `PatientBookingController`, `StaffScheduleController`, `AppointmentQueryService`, `PatientBookingQueryService`, `PatientBookingControllerMvcTest`, `StaffScheduleControllerMvcTest`, `RoleBasedWebMvcIT` | `CODE_COMPLETE_AWAITING_OWNER_TEST` |
| **FR-028** | Gereksinim | Klinik/hastane yöneticisi kaynak, hizmet, saat ve personel ekranları | `AdminDashboardController`, `AdminBranchController`, `AdminDepartmentController`, `AdminServiceController`, `AdminStaffController`, `Admin*ControllerMvcTest`, `RoleBasedWebMvcIT` | `CODE_COMPLETE_AWAITING_OWNER_TEST` |
| **NFR-009** | Gereksinim | Güvenli ve anlaşılır hata yönetimi (400/403/404/500) | `CustomErrorController`, `WebExceptionHandler`, `WebErrorMessageMapper`, `templates/error/{400,403,404,500}.html`, `CustomErrorControllerMvcTest` | `CODE_COMPLETE_AWAITING_OWNER_TEST` |
| **TC-027** | Kabul Testi | Hasta randevu alma ve hekim onay/red/tamamlama akışı | `Sprint08AcceptanceSpecificationTest`, `PatientBookingControllerMvcTest`, `StaffScheduleControllerMvcTest`, `RoleBasedWebMvcIT` | `AUTHORED_AWAITING_OWNER_RUN` |
| **TC-028** | Kabul Testi | Yönetici şube/bölüm/hizmet/saat/personel akışları | `AdminDashboardControllerMvcTest`, `AdminBranchControllerMvcTest`, `AdminDepartmentControllerMvcTest`, `AdminServiceControllerMvcTest`, `AdminStaffControllerMvcTest`, `RoleBasedWebMvcIT` | `AUTHORED_AWAITING_OWNER_RUN` |
| **TC-045** | Kabul Testi | Hatalı girdi (400), yetkisiz erişim (403), bulunamayan kaynak (404) ve 500 güvenliği | `Sprint08AcceptanceSpecificationTest`, `CustomErrorControllerMvcTest`, `RoleBasedWebMvcIT` | `AUTHORED_AWAITING_OWNER_RUN` |

---

## 8. Yapılan İzinli Statik Kontroller ve Dokümantasyon Eşitlemesi

- **İzinli Statik Doğrulamalar:**
  - `com.example.clinic.web` altında sıfır geniş `catch (Exception` kullanımı doğrulandı (`rg -n "catch \\(Exception"` -> temiz).
  - Tüm `templates/` altında `<script>` etiketlerinin yalnızca `templates/fragments/layout.html#L155-156` konumunda olduğu, `th:utext` veya `onerror=` bulunmadığı ve `clinic.js` içinde kullanılan 25 DOM kimliğinin şablonlarda birebir karşılık bulduğu statik olarak doğrulandı.
  - `git diff --check` ve `git status --short` kontrolleriyle beyaz boşluk ve dosya temizliği denetlendi.
- **Eşitlenen Dokümantasyon Dosyaları:**
  - `README.md`
  - `docs/07-sprintler/README.md`
  - `docs/07-sprintler/S08.md`
  - `docs/07-sprintler/S08-implementation-report.md` (bu rapor)
  - `docs/01-urun/01-gereksinim-matrisi.md`
  - `docs/01-urun/traceability.csv`
  - `docs/05-arayuz-api/01-ekranlar-ve-gezinme.md`
  - `docs/05-arayuz-api/02-mvc-endpoint-sozlesmesi.md`
  - `docs/06-kalite/02-test-senaryolari.md`

---

## 9. Ürün Sahibinin Manuel Doğrulama Komut Bloğu

```bash
./mvnw clean verify
git diff --check
git status --short
```
