# Sprint S04 Uygulama Raporu: Kimlik Doğrulama, Spring Security ve Rol Tabanlı Yetki Kapsamı

- **Rapor Tarihi:** 2026-11-06
- **Sprint Kodu:** S04
- **Sprint Başlığı:** Authentication, Spring Security 6, Password Hashing, Session/CSRF Protection, and Multi-Tenant Service Authorization (IDOR Defense)
- **Sorumlu:** Principal Java Architect, Spring Security Engineer, Domain-Driven Design Engineer, PostgreSQL Architect, Application Security Auditor, Senior Test Engineer, Academic OOP Reviewer
- **Sprint Statüsü:** `PASS_OWNER_VERIFIED`

---

## 1. Başlangıç Durumu ve Denetim (Baseline Audit)

- **Depo (Repository):** `https://github.com/coskunburak/clinic_Appointment_System-`
- **Etkin Dal (Branch):** `main`
- **Başlangıç HEAD SHA:** `3825db10f686bed01eea22308f047f73c777508e` (`feat(s03): implement organization, branch, department and medical service catalog`)
- **Çalışma Ağacı (Working Tree):** S04 başlangıcında temiz (`clean`), S03 geliştirmeleri eksiksiz devralındı.
- **S01–S03 Temel Uyumluluğu:**
  - V1 Flyway migrasyonu (`V1__create_organization_and_identity.sql`) ve V2 migrasyonu (`V2__create_catalog_tables.sql`) şemaları dokunulmadan korundu.
  - S02 saf domain çekirdeği (`Money`, `TimeInterval`, `ServiceSnapshot`, `Appointment`, `SchedulingConstraint` motoru) ve `DomainArchitectureTest` framework bağımsızlık kuralı korundu.
  - S03 kurum, şube, haftalık mesai ve katalog servisleri korundu ve yetkilendirme katmanıyla güçlendirildi.
- **Java ve Çerçeve:** Java 21 LTS, Spring Boot 3.4.2, Spring Security 6.4, PostgreSQL 17 (Testcontainers), Flyway, JUnit 5, MockMvc, AssertJ.
- **Yerel ve Şema Doğrulama Düzeltmeleri:**
  - Türkçe işletim sistemi ortamlarında (Turkish locale `tr_TR`) büyük/küçük harf dönüşümünde yaşanan `ı/i` problemi (`Turkish I problem`), tüm güvenlik ve kimlik sınıflarında (`Role`, `ClinicUserDetailsService`, `AccountCredentialService`, `SecurityContextFacade`, katalog DTO'ları) `Locale.ROOT` kullanılarak kökten çözülmüştür.
  - PostgreSQL 17 `bpchar` (`Types#CHAR`) kolonu ile Hibernate 6.6 şema doğrulayıcısı arasındaki uyumsuzluk, `BranchService.currency` alanına `@JdbcTypeCode(SqlTypes.CHAR)` eklenerek giderilmiştir.
  - `AccountCredentialServiceTest` sınıfında boş parola ile kısa parola istisna mesajları ayrıştırılmıştır.

---

## 2. Kapsam ve Karşılanan Gereksinimler

Seçenek A mimarisinin ve ürün matrisinin S04 gereksinimleri eksiksiz karşılandı:
- **FR-022 / TC-022:** Hasta rolü (`ROLE_PATIENT`) yalnızca kendi profilini ve randevularını yönetebilir; başka hastanın profil veya verilerine erişim servis katmanında `AccessDeniedException` ile engellenir (IDOR koruması).
- **FR-023 / TC-023:** Personel rolü (`ROLE_STAFF`) yalnızca yetkili/atanmış olduğu şube ve kendi çalışma takvimi sınırlarında işlem yapabilir; çapraz şube işlemleri engellenir.
- **FR-024 / TC-024:** Yönetici rolü (`ROLE_ADMIN`) üyelik (`Membership`) ile yetkilendirildiği kurum ve şube kapsamında yönetimsel işlemler (hizmet kataloğu, mesai, bölüm) yürütür.
- **FR-025 / TC-025:** Kimlik doğrulama, kullanıcı hesabı (`UserAccount`), BCrypt parola hashleme mekanizması (maliyet faktörü 12) uygulanmıştır; veritabanında asla plaintext şifre saklanmaz.
- **FR-026 / TC-026:** Yetkisiz kuruma veya farklı organizasyon şubesine erişim/müdahale doğrudan servis katmanında engellenir; istemciden gelen ID'lere güvenilmez.
- **NFR-001 / TC-037:** Oturum güvenliği, session fixation koruması (`changeSessionId`), çıkışta oturum geçersiz kılma (`invalidateHttpSession`) ve tüm durum değiştiren isteklerde zorunlu CSRF koruması devrededir.
- **NFR-002 / TC-038:** Müşteri/kurum düzeyinde yetkilendirme kontrolleri MVC controller seviyesiyle sınırlı kalmayıp servis katmanında (`AccessPolicy`) zorunlu kılınmıştır; controller bypass edilse dahi yetkisiz erişim engellenir.

---

## 3. Uygulanan Dosyalar Envanteri

### A. Bağımlılık Yönetimi (Maven)
- `pom.xml`: Spring Boot ebeveyni tarafından yönetilen `spring-boot-starter-security` ve test kapsamındaki `spring-security-test` bağımlılıkları eklendi.

### B. Güvenlik Domain Modeli (`identity.domain`)
- `com.example.clinic.identity.domain.Role`: `ROLE_PATIENT`, `ROLE_STAFF`, `ROLE_ADMIN` tip-güvenli enum modeli. Bilinmeyen, null veya boş roller deterministik olarak reddedilir; varsayılan süper yetki atanmaz.
- `com.example.clinic.identity.domain.Membership`: `Role` enum entegrasyonu sağlandı; String ve Role aşırı yüklemeleriyle geriye dönük tam uyumluluk korundu.

### C. Altyapı ve Veri Tabanı Portları (`identity.infrastructure`)
- `com.example.clinic.identity.infrastructure.MembershipRepository`: `findByUserAccountIdAndRole`, `findByUserAccountIdAndOrganizationId`, `findByUserAccountIdAndBranchId` sorgu metotları eklendi.

### D. Güvenlik Çekirdeği (`common.security`)
- `com.example.clinic.common.security.ClinicPrincipal`: Spring Security `UserDetails` arayüzünü uygulayan değişmez (immutable) kimlik nesnesi. Kararlı veritabanı ID'sini (`UserAccount.id UUID`) taşır; parola hash'ini asla `toString()` veya loglara sızdırmaz.
- `com.example.clinic.common.security.ClinicAuthenticationSuccessHandler`: Giriş sonrası deterministik rol bazlı yönlendirme (`ROLE_ADMIN` -> `/admin/dashboard`, `ROLE_STAFF` -> `/staff/dashboard`, `ROLE_PATIENT` -> `/patient/dashboard`).
- `com.example.clinic.common.security.SecurityConfig`: `SecurityFilterChain`, `BCryptPasswordEncoder(12)`, CSRF koruması, oturum sabitleme önlemleri, açık rota tanımları (`/`, `/login`, `/css/**`, `/js/**`, `/actuator/health`) ve rol bazlı koruma kuralları (`/patient/**`, `/staff/**`, `/admin/**`).
- `com.example.clinic.common.security.SecurityContextFacade`: `SecurityContextHolder` üzerinden kimliği doğrulanmış aktif kullanıcıyı (`UserAccount`) çözümleyen güvenli köprü.

### E. Kimlik ve Yetki Uygulama Servisleri (`identity.application`)
- `com.example.clinic.identity.application.ClinicUserDetailsService`: Spring Security `UserDetailsService` sözleşmesini uygulayan servis. E-posta normalizasyonu yapar, pasif hesapları engeller, yetkisiz/üyeliksiz hesapları fail-closed politikasıyla reddeder, kullanıcı varlığını dışarıya sızdırmaz.
- `com.example.clinic.identity.application.AccountCredentialService`: Parola yönetimi ve hesap oluşturma sınır servisi. BCrypt (12) hashleme, en az 8 en fazla 128 karakter parola karmaşıklığı denetimi, çift hashleme koruması, hesap aktif/pasif yönetimi sağlar.
- `com.example.clinic.identity.application.AccessPolicy`: Hasta öz-erişim denetimi (`enforcePatientSelfAccess`), personel şube yetki denetimi (`enforceStaffBranchAccess`), yönetici kurum denetimi (`enforceAdminOrganizationAccess`) ve yönetici şube denetimi (`enforceAdminBranchAccess`) servis seviyesinde IDOR koruması sağlar.

### F. Güvenlik Altına Alınan Mevcut Servisler
- `com.example.clinic.organization.application.OrganizationAdminService`: Tüm yönetimsel metotlara (`updateOrganization`, `createBranch`, `configureOpeningHours`, `createDepartment`, vb.) `AccessPolicy` denetimi entegre edildi. İlk kurum oluşturma adımında oluşturucu aktöre kurum yöneticisi üyeliği (`ROLE_ADMIN`) bağlayan güvenli bootstrap sözleşmesi uygulandı.
- `com.example.clinic.catalog.application.CatalogManagementService`: Hizmet tanımı ve şube bazlı fiyatlandırma/süre operasyonlarına `AccessPolicy` denetimi eklendi.

### G. Web Arayüzü ve Thymeleaf Şablonları (`identity.web`, `templates`)
- `com.example.clinic.identity.web.LoginController`: `GET /login`, hata/çıkış parametreleri ve korumalı rota doğrulamaları için minimal dashboard placeholder uçları (`/admin/dashboard`, `/staff/dashboard`, `/patient/dashboard`).
- `src/main/resources/templates/auth/login.html`: CSRF korumalı, semantik form login şablonu.
- `src/main/resources/templates/admin/dashboard.html`: Yönetici paneli korumalı şablonu.
- `src/main/resources/templates/staff/dashboard.html`: Personel paneli korumalı şablonu.
- `src/main/resources/templates/patient/dashboard.html`: Hasta paneli korumalı şablonu.
- `src/main/resources/templates/error/403.html`: Standart 403 erişim reddedildi hata sayfası.

### H. Yeni ve Güncellenen Test Sınıfları
- `src/test/java/com/example/clinic/identity/domain/RoleTest.java` (Domain Unit Test)
- `src/test/java/com/example/clinic/identity/application/AccountCredentialServiceTest.java` (Application Service Unit Test)
- `src/test/java/com/example/clinic/identity/application/AccessPolicyTest.java` (Access Policy Unit Test)
- `src/test/java/com/example/clinic/identity/application/ClinicUserDetailsServiceTest.java` (UserDetailsService Unit Test)
- `src/test/java/com/example/clinic/common/security/SecurityMvcTest.java` (MockMvc Security & Slice Test)
- `src/test/java/com/example/clinic/identity/SecurityAuthorizationIT.java` (PostgreSQL Multi-Tenant Integration Test)
- `src/test/java/com/example/clinic/IdentityRepositoryIT.java` (Role enum uyarlaması)
- `src/test/java/com/example/clinic/organization/application/OrganizationAdminServiceTest.java` (Mock uyarlaması)
- `src/test/java/com/example/clinic/organization/application/OrganizationAdminServiceIT.java` (Kimlik doğrulama bağlamı uyarlaması)
- `src/test/java/com/example/clinic/catalog/application/CatalogManagementServiceTest.java` (Mock uyarlaması)
- `src/test/java/com/example/clinic/catalog/application/CatalogManagementServiceIT.java` (Kimlik doğrulama bağlamı uyarlaması)

---

## 4. Mimari Kararlar ve Tasarım Detayları

1. **İki Seviyeli Güvenlik Kalkanı (Defense-in-Depth):**
   - *Seviye 1 (Web / URL Kapsamı):* `SecurityConfig` ile URL desenleri (`/patient/**`, `/staff/**`, `/admin/**`) temel rollerle sınırlandırılır.
   - *Seviye 2 (Servis / Veri Kapsamı - IDOR Koruması):* Yalnızca role sahip olmak yeterli değildir. `AccessPolicy` kullanıcının oturumdaki kararlı UUID'si ile veritabanındaki `Membership` ve `PatientProfile` kayıtlarını eşleştirir. Bir yöneticinin başka bir kurumu veya farklı bir şubeyi yönetmesi engellenir.
2. **Kararlı Kimlik İlkesi (Stable Identity):**
   - Güvenlik ve yetki kararlarında istemcinin HTTP gövdesinde gönderdiği `userId`, `patientId`, `organizationId` veya `role` parametrelerine asla güvenilmez.
   - Tüm izinler oturumdaki `ClinicPrincipal.getUserId()` üzerinden doğrulanır.
3. **Parola ve Kimlik Güvenliği:**
   - Parolalar BCrypt (maliyet 12) ile hashlenir.
   - `UserAccount` ve `ClinicPrincipal` sınıflarının `toString()` metotlarında parola hash'i gizlenmiştir.
   - `ClinicUserDetailsService` bulunamayan kullanıcılar için de genel hata ("Invalid credentials") vererek kullanıcı varlığı keşfini (user enumeration) engeller.
   - Pasif hesaplar (`enabled = false`) anında reddedilir.
4. **Bootstrap ve İlk Kurum Yönetimi:**
   - İlk kurumun oluşturulması aşamasında, kimliği doğrulanmış kullanıcı ilgili kurumun `ROLE_ADMIN` üyesi olarak atanır.
   - Herkese açık kontrolsüz kurum açma veya varsayılan global süper yönetici tanımlama açığı engellenmiştir.

---

## 5. Güvenlik Denetimi Özeti

| Güvenlik Başlığı | Durum | Alınan Önlem / Kanıt |
|---|---|---|
| Plaintext Parola Saklama | ENGELLENDİ | Yalnızca `AccountCredentialService` üzerinden BCrypt 12 hashlenerek DB'ye yazılır |
| User Enumeration | ENGELLENDİ | `loadUserByUsername` e-posta varlığından bağımsız jenerik hata döner |
| Insecure Direct Object Reference (IDOR) | ENGELLENDİ | `AccessPolicy` servis seviyesinde profil sahipliği ve şube üyeliklerini zorunlu kılar |
| Cross-Site Request Forgery (CSRF) | ENGELLENDİ | Spring Security CSRF aktiftir; token içermeyen POST istekleri `403 Forbidden` alır |
| Session Fixation | ENGELLENDİ | Başarılı girişte `changeSessionId()` uygulanır; oturum çerezleri `HttpOnly`'dir |
| Session Invalidation on Logout | ENGELLENDİ | Çıkışta oturum geçersiz kılınır, `SecurityContext` temizlenir, `JSESSIONID` silinir |
| Privilege Escalation | ENGELLENDİ | `Role` enum dışı roller reddedilir; üyelik olmadan yetki verilmez |
| Direct Service Bypass | ENGELLENDİ | MVC atlanarak servis doğrudan çağrılsa bile `AccessPolicy` yetkisiz istekleri reddeder |

---

## 6. Test Envanteri

| Test Sınıfı | Test Türü | Doğrulanan Kapsam |
|---|---|---|
| `RoleTest` | Unit Test | Rol isimleri, yetki metinleri, büyük/küçük harf normalizasyonu, geçersiz/bilinmeyen rol reddi |
| `AccountCredentialServiceTest` | Unit Test | BCrypt 12 hash üretimi, eşleşme doğrulaması, en az 8 karakter kuralı, hazır hash reddi, parola güncelleme ve reset |
| `AccessPolicyTest` | Unit Test (Mockito) | Hasta öz-erişimi, çapraz hasta engeli, personel şube kısıtı, kurum yöneticisi izolasyonu, şube yöneticisi kardeş şube engeli |
| `ClinicUserDetailsServiceTest` | Unit Test (Mockito) | Normalleştirilmiş e-posta arama, üyelikten rol çıkarma, hasta profilinden rol türetme, pasif kullanıcı reddi, yetkisiz hesap fail-closed |
| `SecurityMvcTest` | MVC Slice Test (MockMvc) | Başarılı login ve yönlendirme, yanlış şifre reddi, pasif hesap engeli, oturumsuz istek yönlendirmesi, rol bazlı 403 sayfaları, CSRF zorunluluğu, logout oturum iptali |
| `SecurityAuthorizationIT` | Entegrasyon (PostgreSQL) | TC-022, TC-023, TC-024, TC-025, TC-026, TC-037, TC-038: DB'de hash doğrulaması, çapraz kurum/şube reddi, servis bypass engeli, üyelik iptalinin anında yansıması |
| `OrganizationAdminServiceTest` | Unit Test | Kurum ve şube yönetiminde yönetici güvenlik bağlamı entegrasyonu |
| `OrganizationAdminServiceIT` | Entegrasyon (PostgreSQL) | TC-001..TC-004 senaryolarının aktif admin güvenlik bağlamıyla doğrulanması |
| `CatalogManagementServiceTest` | Unit Test | Tıbbi hizmet ve şube fiyatlandırma yönetiminde yönetici yetki entegrasyonu |
| `CatalogManagementServiceIT` | Entegrasyon (PostgreSQL) | TC-005..TC-006 senaryolarının aktif admin güvenlik bağlamıyla doğrulanması |
| `IdentityRepositoryIT` | Entegrasyon (PostgreSQL) | UserAccount, Membership (Role enum), PatientProfile, StaffProfile kalıcılık doğrulaması |

*Not: Test çalıştırma yasağı kuralı uyarınca testler ajan tarafından çalıştırılmamıştır; tüm testler geliştiricinin manuel doğrulamasına sunulmuştur.*

---

## 7. Ertelenen Fonksiyonlar ve Gelecek Sprint Bağımlılıkları

- **S05 Personel ve Çizelgeleme:** `StaffProfile` ile şubeler arasındaki `StaffAssignment` eşleştirmesi ve hekim çalışma uygunluk takvimleri S05'te inşa edilecektir.
- **S06 Randevu ve Eşzamanlılık:** Randevu oluşturma (`Appointment`), çakışma kontrolü ve pessimistik satır kilitleme S06'da aktifleşecektir.
- **S07 Randevu Yaşam Döngüsü:** Randevu onay, ret, iptal ve tamamlanma durum geçişleri S07'de yetkilendirilecektir.
- **S08 Web ve Thymeleaf:** Zenginleştirilmiş son kullanıcı ve yönetim HTML panelleri S08'de tamamlanacaktır.

---

## 8. Sprint Kabul Statüsü

- **Mevcut Statü:** `PASS_OWNER_VERIFIED`
- **Doğrulama Tarihi:** 2026-11-06 16:35:40
- **Doğrulama Sonuçları:**
  - `./mvnw clean test-compile`: **BUILD SUCCESS**
  - `./mvnw test` (Surefire): **220 test, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**
  - `./mvnw verify` (Failsafe PostgreSQL Integration Tests): **40 test, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**
  - `git diff --check`: **Temiz (0 hata)**
  - Toplam 260 testin tamamı sıfır hata ile geçmiştir. S04 hedefleri tam olarak tamamlanmış ve kabul edilmiştir.
