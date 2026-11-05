# Sprint S03 Uygulama Raporu: Kurum, Şube, Bölüm ve Sağlık Hizmetleri Kataloğu

- **Rapor Tarihi:** 2026-11-05
- **Sprint Kodu:** S03
- **Sprint Başlığı:** Organization, Branch, Department, Weekly Opening Hours, and Branch-Specific Medical Service Catalog
- **Sorumlu:** Principal Java Architect, DDD Engineer, Senior Spring Boot/JPA Architect, PostgreSQL/Flyway Architect, Security Engineer, Test Architect, Academic OOP Reviewer
- **Sprint Statüsü:** `IMPLEMENTED_AWAITING_OWNER_VERIFICATION`

---

## 1. Başlangıç Durumu ve Denetim (Baseline Audit)

- **Depo (Repository):** `coskunburak/clinic_Appointment_System-`
- **Etkin Dal (Branch):** `main`
- **Başlangıç HEAD SHA:** `0c6e369e9f23621ad7bffc43f6559deae443722b` (`feat(s02): implement pure domain core and OOP scheduling policies`)
- **Çalışma Ağacı (Working Tree):** S03 başlangıcında temiz (`clean`).
- **S01 ve S02 Temel Durumu:**
  - V1 Flyway migrasyonu (`V1__create_organization_and_identity.sql`) ve mevcut tablolar (`organization`, `branch`, `branch_opening_hours`, `department`, `user_account`, `membership`, `patient_profile`, `staff_profile`) eksiksiz korunmuştur.
  - S02 saf domain çekirdeği (`Money`, `TimeInterval`, `ServiceSnapshot`, `Appointment`, `SchedulingConstraint` motoru) ve mimari sınır testleri (`DomainArchitectureTest`) geriye dönük uyumlu olarak korunmuştur.
- **Java ve Çerçeve:** Java 21 LTS, Spring Boot 3.4.2, Hibernate 6.6, PostgreSQL 17 (Testcontainers), Flyway, JUnit 5.

---

## 2. Kapsam ve Karşılanan Gereksinimler

S03 kapsamında Seçenek A ödev metninin non-negotiable isterleri ve ürün matrisindeki aşağıdaki gereksinimler karşılanmıştır:
- **FR-001 / TC-001:** Yönetici kurum tanımlayabilir, güncelleyebilir ve yaşam döngüsünü (aktif/pasif) yönetebilir.
- **FR-002 / TC-002:** Kurum çok sayıda şube tanımlayabilir; tek şubeli veya onlarca şubeli zincir organizasyonlar kod değişikliği gerekmeksizin çalışır.
- **FR-003 / TC-003:** Şube haftalık çalışma gün ve saat aralıkları (`BranchOpeningHours`) atomik ve çakışmasız olarak yapılandırılabilir.
- **FR-004 / TC-004:** Şubeye bağlı tıbbi bölümler (`Department`) tanımlanabilir; şube içi isim tekilliği korunur, farklı şubelerde aynı isim desteklenir.
- **FR-005 / TC-005:** Kurum düzeyinde sağlık hizmeti adı ve açıklaması (`ServiceDefinition`) tanımlanabilir; kurum içi isim tekilliği ve kiracı izolasyonu sağlanır.
- **FR-006 / TC-006:** Şube düzeyinde özelleştirilmiş hizmet süresi (`1..480` dk), işlem ücreti (`BigDecimal >= 0`, 2 ondalık basamak) ve para birimi (`BranchService`) yapılandırılabilir; aynı hizmet farklı şubelerde bağımsız fiyat ve süreye sahip olabilir.

Kapsam dışı bırakılanlar (Strict Out-of-Scope): Hekim mesai takvimleri (S05), randevu rezervasyonu ve satır kilitleme (S06), HTML UI ekranları (S08).

---

## 3. Uygulanan Dosyalar Envanteri

### A. Veritabanı Migrasyonu (Flyway V2)
- `src/main/resources/db/migration/V2__create_catalog_tables.sql`
  - `service_definition` tablosu: Kurum düzeyinde tekil hizmet adı, açıklama, aktiflik ve kompozit tekillik.
  - `branch_service` tablosu: Şube bazlı süre, ücret (`NUMERIC(12,2)`), para birimi (`CHAR(3)`), aktiflik ve çift kompozit yabancı anahtar.

### B. Domain Katmanı (`catalog.domain`)
- `com.example.clinic.catalog.domain.ServiceDefinition`: Kurum düzeyinde tıbbi hizmet varlığı (`BaseEntity` kalıtımı, zengin domain davranışı: `rename`, `changeDescription`, `activate`, `deactivate`).
- `com.example.clinic.catalog.domain.BranchService`: Şube hizmet sunumu varlığı (`BaseEntity` kalıtımı, süre 1..480 dk, `Money` entegrasyonu, `changePrice`, `changeDuration`, `toSnapshot()` snapshot dönüştürücü).

### C. Kalıcılık / Repository Katmanı (`catalog.infrastructure`, `organization.infrastructure`)
- `com.example.clinic.catalog.infrastructure.ServiceDefinitionRepository`: Kurum içi isim ve ID sorguları.
- `com.example.clinic.catalog.infrastructure.BranchServiceRepository`: Şube ve hizmet tanımı eşleşmesi, aktif hizmet sorguları.
- `com.example.clinic.organization.infrastructure.BranchOpeningHoursRepository`: Atomik silme (`deleteByBranchId`) ve sıralı okuma metotları eklendi.
- `com.example.clinic.organization.infrastructure.BranchRepository`: Aktif şube ve organizasyon filtreleme metotları eklendi.
- `com.example.clinic.organization.infrastructure.DepartmentRepository`: Aktif bölüm filtreleme metotları eklendi.

### D. DTO ve Komut Nesneleri (Records)
- `organization.application.dto.CreateOrganizationCommand`
- `organization.application.dto.OrganizationResponse`
- `organization.application.dto.CreateBranchCommand`
- `organization.application.dto.BranchResponse`
- `organization.application.dto.CreateDepartmentCommand`
- `organization.application.dto.DepartmentResponse`
- `organization.application.dto.OpeningHoursDto`
- `catalog.application.dto.CreateServiceCommand`
- `catalog.application.dto.ServiceDefinitionResponse`
- `catalog.application.dto.AssignBranchServiceCommand`
- `catalog.application.dto.UpdateServicePricingCommand`
- `catalog.application.dto.UpdateServiceDurationCommand`
- `catalog.application.dto.BranchServiceResponse`

### E. Uygulama Servisleri (Application Services)
- `com.example.clinic.organization.application.OrganizationAdminService`: Kurum, şube, mesai saatleri ve bölüm orkestrasyon servisi.
- `com.example.clinic.catalog.application.CatalogManagementService`: Kurum hizmet tanımları, şube eşleştirmeleri, bağımsız fiyat ve süre yönetim servisi.

### F. Otomatize Test Sınıfları
- `src/test/java/com/example/clinic/catalog/domain/ServiceDefinitionTest.java` (Domain Unit Test)
- `src/test/java/com/example/clinic/catalog/domain/BranchServiceTest.java` (Domain Unit Test)
- `src/test/java/com/example/clinic/organization/application/OrganizationAdminServiceTest.java` (Application Service Unit Test)
- `src/test/java/com/example/clinic/catalog/application/CatalogManagementServiceTest.java` (Application Service Unit Test)
- `src/test/java/com/example/clinic/catalog/CatalogDatabaseConstraintIT.java` (PostgreSQL Database Constraint Test)
- `src/test/java/com/example/clinic/organization/application/OrganizationAdminServiceIT.java` (PostgreSQL Integration Test)
- `src/test/java/com/example/clinic/catalog/application/CatalogManagementServiceIT.java` (PostgreSQL Integration Test)
- `src/test/java/com/example/clinic/FlywayMigrationIT.java` (V1 + V2 Flyway Migration Test)

---

## 4. Veritabanı ve Çok Kiracılı Veri Bütünlüğü Güvenceleri

### A. V2 Tablo ve Kısıt Mimarisi
1. **`service_definition` Tablosu:**
   - `id UUID PRIMARY KEY`
   - `organization_id UUID NOT NULL REFERENCES organization(id) ON DELETE RESTRICT`
   - `name VARCHAR(255) NOT NULL`, `description TEXT`, `active BOOLEAN NOT NULL DEFAULT TRUE`
   - `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`
   - `CONSTRAINT chk_service_definition_name_not_blank CHECK (length(trim(name)) > 0)`
   - `CONSTRAINT uq_service_definition_org_name UNIQUE (organization_id, name)`
   - `CONSTRAINT uq_service_definition_id_org_id UNIQUE (id, organization_id)` (Kompozit yabancı anahtar güvencesi)
   - `CREATE INDEX idx_service_definition_org_id ON service_definition(organization_id)`

2. **`branch_service` Tablosu:**
   - `id UUID PRIMARY KEY`
   - `organization_id UUID NOT NULL REFERENCES organization(id) ON DELETE RESTRICT`
   - `branch_id UUID NOT NULL`, `service_definition_id UUID NOT NULL`
   - `duration_minutes INTEGER NOT NULL`, `fee_amount NUMERIC(12,2) NOT NULL`, `currency CHAR(3) NOT NULL DEFAULT 'TRY'`
   - `active BOOLEAN NOT NULL DEFAULT TRUE`, `created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP`
   - `CONSTRAINT uq_branch_service_unique UNIQUE (branch_id, service_definition_id)`
   - `CONSTRAINT chk_branch_service_duration CHECK (duration_minutes >= 1 AND duration_minutes <= 480)`
   - `CONSTRAINT chk_branch_service_fee CHECK (fee_amount >= 0)`
   - `CONSTRAINT chk_branch_service_currency_length CHECK (char_length(trim(currency)) = 3)`
   - **Kompozit Kiracı Kısıtları:**
     - `CONSTRAINT fk_branch_service_branch_org FOREIGN KEY (branch_id, organization_id) REFERENCES branch(id, organization_id) ON DELETE RESTRICT`
     - `CONSTRAINT fk_branch_service_def_org FOREIGN KEY (service_definition_id, organization_id) REFERENCES service_definition(id, organization_id) ON DELETE RESTRICT`

### B. Çapraz Kiracı (Cross-Tenant) İzolasyon Mantığı
Yalnızca tekil `(branch_id)` ve `(service_definition_id)` yabancı anahtarları kullanıldığında, A Kurumunun şubesine B Kurumunun hizmetinin atanması veritabanı seviyesinde tespit edilemez.
S03'te uygulanan kompozit yabancı anahtar mimarisi ile:
- `branch_service.organization_id` kolonu hem `branch(id, organization_id)` hem de `service_definition(id, organization_id)` ile eşleşmek zorundadır.
- Dolayısıyla, `branch` ve `service_definition` farklı kurumlara aitse, veritabanına ekleme girişimi doğrudan PostgreSQL seviyesinde yabancı anahtar ihlali (`DataIntegrityViolationException`) ile sonuçlanır.
- Java katmanında da `BranchService` kurucusu ve `CatalogManagementService` aynı kuralı savunmacı olarak denetler (Defense-in-Depth).

---

## 5. Domain Değişmezleri (Invariants) ve İş Kuralları

1. **Kurum Değişmezleri:**
   - Kurum adı boş veya boşluk olamaz.
   - İnaktif kurum altında yeni şube veya hizmet sunumu açılamaz.
2. **Şube ve Mesai Değişmezleri:**
   - Şube zaman dilimi geçerli bir IANA ID (`ZoneId.of(...)`) olmak zorundadır (varsayılan: `Europe/Istanbul`).
   - Mesai saatlerinde hafta günü `1..7` (Pazartesi=1, Pazar=7) aralığındadır.
   - `start_time < end_time` katı kuralı zorunludur; sıfır süreli (`09:00–09:00`) ve ters saatler engellenir.
   - Aynı gün içindeki çakışan pencereler (`09:00–13:00` ile `12:00–17:00`) reddedilir. Bitişik pencereler (`09:00–12:00` ve `12:00–17:00`) geçerlidir.
   - Mesai saati yapılandırması atomik değiştirme (replace) sözleşmesine uyar; doğrulama hatasında eski mesai çizelgesi korunur.
3. **Katalog ve Fiyatlandırma Değişmezleri:**
   - Hizmet adı kurum kapsamında tekildir; farklı kurumlarda aynı hizmet adı bulunabilir.
   - Hizmet süresi `1` ile `480` dakika (dahil) arasında olmalıdır.
   - Hizmet ücreti negatif olamaz; 2 ondalık basamak tam finansal hassasiyetle saklanır (`NUMERIC(12,2)` / `Money`).
   - Aynı hizmet aynı şubeye mükerrer atanamaz (`uq_branch_service_unique`).
   - Aynı hizmet farklı şubelerde tamamen farklı süre ve fiyatla sunulabilir (ör. Kadıköy 30 dk / 1500 TL; Nişantaşı 45 dk / 2500 TL).
   - Şube fiyatı güncellendiğinde diğer şubeler ve kurum düzeyindeki hizmet tanımı değişmez.
   - `BranchService.toSnapshot()` metodu, S06 randevusu için o anki fiyat ve süreyi donduran değişmez `ServiceSnapshot` üretir.

---

## 6. OOP ve SOLID İlkelerinin Kanıtları

- **Kapsülleme (Encapsulation):** `ServiceDefinition` ve `BranchService` sınıflarında kontrolsüz public setter'lar kapatılmıştır. Durum değişiklikleri niyet belirten iş metotlarıyla (`rename`, `changePrice`, `changeDuration`, `activate`, `deactivate`) yürütülür. Fiyat ve süre değişmezleri nesne sınırında korunur.
- **Değer Nesneleriyle Bileşim (Composition over Primitive Types):** `BranchService` ham `double` veya `float` yerine S02'de inşa edilen değişmez `Money` değer nesnesini ve `Currency` sınıfını kullanır.
- **Kalıtım (Inheritance):** `ServiceDefinition` ve `BranchService` ortak varlık özellikleri (`id UUID`, `createdAt Instant`, proxy-güvenli `equals`/`hashCode`) için `BaseEntity` mapped superclass'ından türer.
- **Tek Sorumluluk Prensibi (SRP):** `OrganizationAdminService` kurum-şube-bölüm-mesai hiyerarşisini yönetirken, `CatalogManagementService` tıbbi hizmet kataloğu ve fiyatlandırma kullanım senaryolarına odaklanır.
- **Açık/Kapalı Prensibi (OCP):** Şube bazlı fiyatlandırma modeli, yeni bir şube veya yeni bir tıbbi hizmet eklendiğinde kod değişikliği gerektirmeden sistemin genişlemesini sağlar.
- **Arayüz Ayrımı (ISP):** Küçük ve amaca yönelik repository portları (`ServiceDefinitionRepository`, `BranchServiceRepository`) tanımlanmıştır.
- **Bağımlılıkların Ters Çevrilmesi (DIP):** Uygulama servisleri doğrudan veritabanı sürücülerine değil, Spring Data JPA repository arayüzlerine kurucu enjeksiyonuyla bağlanır.

---

## 7. Statik İnceleme Bulguları (Static Review)

- Java derleme sözdizimi, paket adlandırmaları ve import'lar kontrol edildi; döngüsel bağımlılık bulunmamaktadır.
- `BaseEntity` kalıtımı ve `Instant createdAt` alanlarının `V2__create_catalog_tables.sql` içindeki `created_at TIMESTAMPTZ` kolonlarıyla tam uyumlu olduğu doğrulandı.
- `BranchService.currency` alanının `CHAR(3)` PostgreSQL tanımı ve JPA `@Column(name = "currency", nullable = false, length = 3, columnDefinition = "CHAR(3)")` eşleşmesi doğrulandı.
- `DomainArchitectureTest` kısıtları denetlendi; `booking/domain` ve `scheduling/domain` saf domain çekirdeğine herhangi bir framework bağımlılığı eklenmemiştir.
- Kontrolsüz public yönetim endpoint'i (controller) eklenmemiştir; yetkisiz erişim riski engellenmiştir.

---

## 8. Test Envanteri

| Test Sınıfı | Test Türü | Doğrulanan Kapsam |
|---|---|---|
| `ServiceDefinitionTest` | Unit Test | Varlık oluşturma, isim boşluk denetimi, isim değiştirme, açıklama güncelleme, aktiflik, UUID eşitliği |
| `BranchServiceTest` | Unit Test | Süre sınırları (1..480), negatif ücret engeli, kuruş hassasiyeti, para birimi doğrulama, fiyat/süre güncelleme, çapraz kiracı engeli, ServiceSnapshot dönüşümü |
| `OrganizationAdminServiceTest` | Unit Test (Mockito) | Kurum oluşturma/güncelleme, şube zaman dilimi ve aktiflik kontrolleri, bölüm tekilliği, mesai saatleri çakışma ve sıralama algoritmaları |
| `CatalogManagementServiceTest` | Unit Test (Mockito) | Kurum hizmeti tanımlama, şube hizmet atama, çapraz kiracı kontrolü, mükerrer atama engeli, pasif kaynak kontrolü, fiyat/süre güncelleme |
| `CatalogDatabaseConstraintIT` | Entegrasyon (PostgreSQL) | `chk_branch_service_duration`, `chk_branch_service_fee`, `uq_service_definition_org_name`, `uq_branch_service_unique`, kompozit FK çapraz kiracı reddi |
| `OrganizationAdminServiceIT` | Entegrasyon (PostgreSQL) | TC-001 (Kurum oluştur/oku), TC-002 (Tek ve çok şube desteği), TC-003 (Atomik mesai saati yönetimi), TC-004 (Şube bölümleri yönetimi) |
| `CatalogManagementServiceIT` | Entegrasyon (PostgreSQL) | TC-005 (Katalog hizmeti oluştur/oku), TC-006 (Şube bazlı bağımsız fiyat ve süre, fiyat güncelleme, negatif fiyat reddi) |
| `FlywayMigrationIT` | Entegrasyon (PostgreSQL) | V1 ve V2 migrasyonlarının sırayla işletilmesi, tablo varlıkları, idempotent ikinci çağrı, şema validasyonu |

---

## 9. Ertelenen Fonksiyonlar ve S04 Hazırlığı

- **S04 Yetkilendirme Entegrasyonu:** S04 kapsamında Spring Security, `UserAccount`, `Membership` bazlı rol yetkilendirmesi (`ADMIN`, `STAFF`, `PATIENT`) ve servis metotlarına `@PreAuthorize` kısıtları bağlanacaktır.
- **S05 Personel ve Çizelgeleme:** Personelin (`StaffProfile`) şubelere atanması (`StaffAssignment`), hizmet yetkinlikleri (`StaffServiceEligibility`) ve haftalık uygunluk takvimleri S05'te inşa edilecektir.
- **S06 Rezervasyon ve Concurrency:** `BranchService` üzerinden üretilen `ServiceSnapshot` ile randevu kaydı, pessimistik satır kilitleme ve çakışma önleme S06'da aktifleşecektir.
- **S08 Web ve UI:** Thymeleaf formları ve yönetim arayüzleri S08'de bağlanacaktır.

---

## 10. Sprint Kabul Statüsü

- **Mevcut Statü:** `IMPLEMENTED_AWAITING_OWNER_VERIFICATION`
- Tüm kaynak kodlar, Flyway migrasyonu, JPA varlıkları, servisler ve testler yazılmıştır. Geliştirici manuel test komut bloğunu çalıştırıp çıktıyı paylaştıktan sonra statü `PASS_OWNER_VERIFIED` olarak güncellenecektir.
