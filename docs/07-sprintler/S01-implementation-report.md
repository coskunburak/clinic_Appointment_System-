# Sprint S01 Uygulama Raporu: Java Altyapısı, PostgreSQL, Flyway, JPA ve CI

- **Rapor Tarihi:** 2026-11-02
- **Sprint Kodu:** S01
- **Sprint Başlığı:** Java Bootstrapping ve Veritabanı Temeli
- **Sorumlu:** Principal Java Architect, Senior Spring Boot Engineer, PostgreSQL Database Architect, DevOps & QA Lead
- **Sprint Durumu:** `IMPLEMENTED — AWAITING_OWNER_TEST`

---

## 1. Başlangıç Durumu ve Denetim (Baseline Audit)

- **Depo (Repository):** `coskunburak/clinic_Appointment_System-`
- **Etkin Dal (Branch):** `main`
- **Başlangıç HEAD SHA:** `23b7d28` (`docs(s00): establish repository foundation, academic scope and fix build dependencies`)
- **Uzak Depo Durumu:** `origin/main` commit'i `3b2dd1c` ("docs") üzerindedir; yerel dal 1 commit ileridedir (`ahead 1`). S00 yerelde commit edilmiştir ancak henüz `git push` yapılmamıştır.
- **Çalışma Ağacı (Working Tree):** S01 öncesinde tamamen temiz (`clean`).
- **Java Sürümü:**
  - Terminal varsayılanı: OpenJDK 17 (`/opt/homebrew/Cellar/openjdk@17/17.0.16`).
  - Proje gereksinimi: Java 21 LTS (`/opt/homebrew/opt/openjdk@21` kurulu ve doğrulanabilir).
- **Maven Sürümü:** Apache Maven Wrapper 3.9.9 (`mvnw`).
- **Spring Boot Sürümü:** 3.4.2 LTS (`spring-boot-starter-parent`).

---

## 2. Sprint S01 Kapsamı ve Uygulama Bileşenleri

### A. Eklenen ve Yapılandırılan Bağımlılıklar (`pom.xml`)
- `spring-boot-starter-data-jpa`: Spring Data JPA ve Hibernate 6 ORM altyapısı.
- `org.postgresql:postgresql`: PostgreSQL resmî JDBC sürücüsü (runtime scope).
- `org.flywaydb:flyway-core` ve `org.flywaydb:flyway-database-postgresql`: Sürüm kontrollü veritabanı migrasyonu.
- `org.springframework.boot:spring-boot-starter-actuator`: Sistem sağlık ve hazır oluş denetimi (`/actuator/health`).
- `org.springframework.boot:spring-boot-testcontainers`: Spring Boot 3.4 ServiceConnection entegrasyonu (test scope).
- `org.testcontainers:postgresql` ve `org.testcontainers:junit-jupiter`: Gerçek PostgreSQL 17 üzerinde izole test çalıştırma (test scope).
- `maven-failsafe-plugin`: `*IT.java` entegrasyon testlerinin `mvn verify` yaşam döngüsünde koşulması için entegre edildi (`-XX:+EnableDynamicAgentLoading` argümanı ile).

### B. Konteyner ve Ortam Yapılandırması
- `compose.yaml`:
  - PostgreSQL 17 (`postgres:17-alpine`) imajı.
  - Kalıcı veri alanı (`clinic_postgres_data` named volume).
  - Sağlık kontrolü: `pg_isready -U ${POSTGRES_USER:-clinic_user} -d ${POSTGRES_DB:-clinic_db}` (5 sn aralık).
  - Yerel ağ güvenliği: Sadece yerel makineye bağlandı (`127.0.0.1:${POSTGRES_PORT:-5433}:5432` - ana makinedeki varsayılan 5432 çakışmalarını önlemek amacıyla 5433 varsayılanıyla parametrelendirildi).
- `.env.example`:
  - Güvenli yerel geliştirme değişkenleri şablonu.
  - `.env` dosyasının versiyon kontrolüne dahil edilmemesi `.gitignore` ile garanti altına alındı.

### C. Profil ve Konfigürasyon Mimarisi
- `src/main/resources/application.yml`:
  - Ortak baz konfigürasyon.
  - `spring.jpa.hibernate.ddl-auto=validate` (Hibernate otomatik şema üretmez, Flyway sahipliğindedir).
  - `spring.jpa.open-in-view=false` (Anti-pattern engellendi).
  - `spring.flyway.clean-disabled=true` (Veri kaybı koruması).
  - `management.endpoints.web.exposure.include=health` (Yalnızca sağlık endpoint'i açık; hassas actuator bilgileri kapalı).
  - Eski `application.properties` dosyası çelişki üretmemesi adına kaldırıldı.
- `src/main/resources/application-dev.yml`:
  - Yerel PostgreSQL bağlantısı ve SQL formatlama logları.
- `src/main/resources/application-test.yml`:
  - Testcontainers dinamik datasource bağlantısı ve test log seviyeleri.

### D. Flyway V1 Temel Şeması (`V1__create_organization_and_identity.sql`)
Aşağıdaki 8 tablo, ilişkisel bütünlük kuralları, kontrol kısıtları (CHECK), indeksler ve benzersizlik kısıtlarıyla oluşturulmuştur:
1. `organization`: Kurum tablosu (`id UUID PRIMARY KEY`, `name`, `active`, `created_at`).
2. `branch`: Şube tablosu (`id`, `organization_id FK`, `name`, `time_zone`, `address`, `active`, composite `uq_branch_id_org_id`).
3. `branch_opening_hours`: Şube mesai aralıkları (`id`, `branch_id FK CASCADE`, `weekday`, `start_time`, `end_time`, `start_time < end_time` CHECK).
4. `department`: Tıbbi bölüm tablosu (`id`, `branch_id FK`, `name`, `active`, `uq_department_branch_name`).
5. `user_account`: Kullanıcı hesap tablosu (`id`, `email UNIQUE`, `password_hash`, `enabled`, `created_at`).
6. `membership`: Kurum/şube yetki tablosu (`id`, `user_id FK`, `organization_id FK`, `branch_id FK`, `role`, `fk_membership_branch_org` composite FK).
7. `patient_profile`: Hasta profil tablosu (`id`, `user_id FK UNIQUE`, `full_name`, `phone`, `created_at`).
8. `staff_profile`: Sağlık personeli profil tablosu (`id`, `user_id FK UNIQUE`, `full_name`, `phone`, `staff_type`, `specialty`, `active`, `created_at`).

> **Çok Kiracılı Güvenlik Mimarisi (Tenant Integrity):** `branch` tablosundaki `(id, organization_id)` kompozit tekilliği üzerinden `membership` tablosuna `fk_membership_branch_org FOREIGN KEY (branch_id, organization_id) REFERENCES branch(id, organization_id)` tanımlanmıştır. Bu sayede A kurumuna ait bir kullanıcının B kurumuna bağlı bir şubeye yetkilendirilmesi veritabanı seviyesinde fiziksel olarak imkânsız kılınmıştır.

### E. JPA Kalıcılık Tabanı ve Repository'ler
- **Domain Modeli:**
  - `com.example.clinic.organization.domain.Organization`
  - `com.example.clinic.organization.domain.Branch`
  - `com.example.clinic.organization.domain.Department`
  - `com.example.clinic.organization.domain.BranchOpeningHours`
  - `com.example.clinic.identity.domain.UserAccount`
  - `com.example.clinic.identity.domain.Membership`
  - `com.example.clinic.identity.domain.PatientProfile`
  - `com.example.clinic.identity.domain.StaffProfile`
- **Tasarım Disiplini:**
  - UUID kimlikler kurucu seviyesinde oluşturulabilir / atanabilir; `equals`/`hashCode` yalnızca değişmez `id` karşılaştırması yapar, döngüsel veya dinamik koleksiyonlara bağımlı değildir.
  - Hassas veriler (`password_hash`) `UserAccount.toString()` metodundan kesin olarak çıkarılmıştır.
  - Tüm ilişkiler `@ManyToOne(fetch = FetchType.LAZY)` ve `@OneToOne(fetch = FetchType.LAZY)` olarak tanımlanmıştır; tehlikeli cascade silme politikaları engellenmiştir.
- **Repository Katmanı:**
  - `OrganizationRepository`, `BranchRepository`, `DepartmentRepository`, `BranchOpeningHoursRepository`
  - `UserAccountRepository`, `MembershipRepository`, `PatientProfileRepository`, `StaffProfileRepository`

### F. Entegrasyon Testleri (`src/test/java/com/example/clinic/`)
1. `AbstractIntegrationTest`: Testcontainers Singleton Container Pattern (`static { postgres.start(); }` ve `@DynamicPropertySource`) ile tek bir PostgreSQL 17 Alpine konteyneri ayağa kaldıran, Spring Context önbelleklemesiyle uyumlu, tüm `*IT` test sınıfları boyunca konteyneri canlı tutan paylaşımlı soyut temel sınıf.
2. `ClinicAppointmentApplicationTests`: Context yükleme ve Hibernate `ddl-auto=validate` doğrulama duman testi.
3. `FlywayMigrationIT` (TC-039 / NFR-003):
   - Temiz PostgreSQL üzerinde V1 migrasyonunun başarıyla çalıştığı ve 9 tablonun oluştuğu doğrulanır.
   - İkinci kez `flyway.migrate()` çağrıldığında 0 migrasyon işletildiği ve şemanın bozulmadığı (idempotentlik) kanıtlanır.
   - `flyway.validate()` ile şema checksum doğrulaması yapılır.
4. `OrganizationRepositoryIT`: Kurum, şube, bölüm ve mesai saatlerinin PostgreSQL'e yazılması ve ilişkisel okunması testi.
5. `IdentityRepositoryIT`: Kullanıcı, yetki, hasta ve personel profillerinin yazılması, okunması ve `toString()` güvenlik kontrolü testi.
6. `DatabaseConstraintIT`: Negatif veritabanı kısıt testleri:
   - Mükerrer e-posta reddi (`uq_user_account_email`).
   - Aynı şubede mükerrer bölüm adı reddi (`uq_department_branch_name`).
   - Aynı kullanıcı için mükerrer hasta profili reddi (`uq_patient_profile_user_id`).
   - Farklı kurum şubesine üyelik bağlama denemesi reddi (`fk_membership_branch_org`).
   - Bitiş saatinden büyük/eşit başlangıç saati mesai kaydı reddi (`chk_opening_hours_time_order`).

### G. GitHub Actions CI (`.github/workflows/ci.yml`)
- `push` ve `pull_request` (dal: `main`) tetikleyicisi.
- `ubuntu-latest` çalıştırıcısı (Docker destekli).
- Eclipse Temurin JDK 21 kurulumu ve Maven bağımlılık önbelleği.
- `mvnw` çalıştırılabilirlik izni (`chmod +x`).
- `./mvnw -B verify` adımı (Birim ve Testcontainers entegrasyon testlerinin tamamını çalıştırır).
- 15 dakika çalışma zaman aşımı (timeout).
- Sıkılaştırılmış izinler (`permissions: contents: read`).

---

## 3. Gereksinim ve Kabul Kriterleri Matrisi

| Kriter No | Açıklama | Statik Durum | Doğrulama Statüsü | Kanıt / Dayanak |
|---|---|---|---|---|
| **AC-S01-01** | S00 önkoşulu doğrulandı | `IMPLEMENTED` | `PASS_AUDIT` | Yerel HEAD `23b7d28`, temiz çalışma ağacı. |
| **AC-S01-02** | Java 21 ve Maven Wrapper uyumu | `IMPLEMENTED` | `STATICALLY_REVIEWED` | `pom.xml` (`<java.version>21</java.version>`), Maven Wrapper 3.9.9. |
| **AC-S01-03** | Bağımlılık koordinatları ve Boot 3.4.2 uyumu | `IMPLEMENTED` | `STATICALLY_REVIEWED` | Resmi starter koordinatları, çelişkisiz dependency seti. |
| **AC-S01-04** | PostgreSQL 17 Docker Compose | `IMPLEMENTED` | `STATICALLY_REVIEWED` | `compose.yaml` (PostgreSQL 17 Alpine, healthcheck, volume). |
| **AC-S01-05** | Veritabanı sırları dışsallaştırıldı | `IMPLEMENTED` | `STATICALLY_REVIEWED` | `.env.example` hazırlandı, `.env` gitignore ile korundu. |
| **AC-S01-06** | Flyway V1 temel şemayı oluşturur | `IMPLEMENTED` | `STATICALLY_REVIEWED` | `V1__create_organization_and_identity.sql` (8 tablo, indeksler). |
| **AC-S01-07** | Migrasyon idempotenttir (TC-039) | `IMPLEMENTED` | `AWAITING_OWNER_TEST` | `FlywayMigrationIT.subsequentMigration_isIdempotent`. |
| **AC-S01-08** | Hibernate ddl-auto=validate eşleşir | `IMPLEMENTED` | `AWAITING_OWNER_TEST` | `application.yml` ve 8 entity sınıfı tam uyumlu. |
| **AC-S01-09** | Gerçek PostgreSQL repository testleri | `IMPLEMENTED` | `AWAITING_OWNER_TEST` | `OrganizationRepositoryIT`, `IdentityRepositoryIT`. |
| **AC-S01-10** | Negatif kısıt ve bütünlük testleri | `IMPLEMENTED` | `AWAITING_OWNER_TEST` | `DatabaseConstraintIT` (5 ayrı kısıt senaryosu). |
| **AC-S01-11** | Uygulama context başarıyla başlar | `IMPLEMENTED` | `AWAITING_OWNER_TEST` | `ClinicAppointmentApplicationTests.contextLoads`. |
| **AC-S01-12** | Maven verify entegrasyon testlerini kapsar | `IMPLEMENTED` | `STATICALLY_REVIEWED` | `maven-failsafe-plugin` verify hedefine bağlandı. |
| **AC-S01-13** | GitHub Actions iş akışı | `IMPLEMENTED` | `STATICALLY_REVIEWED` | `.github/workflows/ci.yml` oluşturuldu. |
| **AC-S01-14** | Gizli bilgi ve gerçek veri sızıntısı yok | `IMPLEMENTED` | `PASS_AUDIT` | Sabit şifre yok, demo kurmaca veriler, toString hash koruması. |
| **AC-S01-15** | Dokümantasyon gerçeği yansıtır | `IMPLEMENTED` | `STATICALLY_REVIEWED` | `README.md`, `S01.md`, mimari ve veri dokümanları güncellendi. |
| **AC-S01-16** | Yalnızca S01 kapsamı uygulandı | `IMPLEMENTED` | `PASS_AUDIT` | S02..S10 iş mantığı veya ekranları eklenmedi. |
| **AC-S01-17** | Geliştiriciye yorumsuz tek bash komut seti | `IMPLEMENTED` | `READY_FOR_HANDOFF` | Tek fenced blok, sıfır yorum satırı. |
| **AC-S01-18** | S01 uygulama raporu oluşturuldu | `IMPLEMENTED` | `DOCUMENTED` | Bu rapor belgesi hazırlandı. |

---

## 4. Güvenlik, Gizlilik ve Uyumluluk Notları

1. **Parola ve Kimlik Bilgileri:** `user_account` tablosunda ve entity sınıfında parolalar için `password_hash` alanı kullanılmıştır. Entity `toString()` metodu şifre hash'ini loglara veya hata mesajlarına sızdırmaz.
2. **Konteyner Port İzolasyonu:** PostgreSQL portu `0.0.0.0` yerine `127.0.0.1:${POSTGRES_PORT:-5433}:5432` şeklinde yalnızca yerel makineye bağlanmıştır (ana makinedeki olası 5432 çakışmalarını bertaraf etmek için 5433 varsayılanı atanmıştır).
3. **Actuator Güvenliği:** `/actuator/env`, `/actuator/beans` gibi bilgi ifşa riski taşıyan endpoint'ler devre dışıdır; yalnızca `/actuator/health` endpoint'i yetkisizleştirilmiş genel kontrole açılmıştır.
4. **Flyway Güvenliği:** `spring.flyway.clean-disabled=true` yapılandırmasıyla olası bir yanlışlıkla veritabanı silme işlemi engellenmiştir.

---

## 5. Kalan Riskler ve Geliştirici Aksiyonları

1. **Docker Servisi:** Testlerin çalışabilmesi için yerel makinede Docker motorunun (Docker Desktop) açık olması gerekmektedir.
2. **macOS Java Yolu:** Terminal ortamında `java -version` varsayılan olarak Java 17 gelebilir. Test ve derleme komutları çalıştırılırken `JAVA_HOME` değişkeni `/opt/homebrew/opt/openjdk@21` olarak belirlenmelidir.
3. **Commit Takvimi Kuralı:** S00 commit'i (`23b7d28`) 2026-11-02 tarihinde atılmıştır. S01 doğrulandıktan sonra öğrenci tarafından yapılacak commit akademik takvimde planlanan güne denk gelmelidir.

---

## 6. S02 Sprintine Geçiş Hazırlığı (S02 Readiness)

S02 sprintine (OOP Domain Çekirdeği ve Temel Varlıklar) geçebilmek için aşağıdaki koşullar sağlanmalıdır:
1. Geliştiricinin sunduğumuz tek blokluk komut setini çalıştırıp `BUILD SUCCESS` aldığını teyit etmesi.
2. S01 entegrasyon testlerinin (TC-039 ve kısıt testleri) yeşil olduğunun bildirilmesi.
3. S01 için `feat(s01): establish PostgreSQL, Flyway, JPA and CI foundation` commit'inin yapılması.
