# Hastane ve Klinik Randevu Yönetim Sistemi (Clinic Appointment System)

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Build Tool](https://img.shields.io/badge/Maven%20Wrapper-3.9.9-blue.svg)](https://maven.apache.org/)
[![Database](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org/)
[![Migration](https://img.shields.io/badge/Flyway-V4-red.svg)](https://flywaydb.org/)
[![Sprint](https://img.shields.io/badge/Sprint-S08%20(Spring%20MVC%20ve%20Thymeleaf%20Ekranları)-brightgreen.svg)](docs/07-sprintler/S08.md)
[![Academic Status](https://img.shields.io/badge/Academic%20Project-OOP%20Seçenek%20A-informational.svg)](docs/00-baslangic/02-proje-kunyesi-ve-basari.md)

Bu proje, üniversite **Nesneye Dayalı Programlama (OOP)** dersi kapsamında geliştirilen, tek ve çok şubeli sağlık kuruluşlarının randevu süreçlerini uçtan uca yönetmeyi amaçlayan web tabanlı bir hastane/klinik randevu yönetim sistemidir.

> **Mevcut Proje Durumu:** **Sprint S08 — Spring MVC ve Thymeleaf Kullanıcı Ekranları (3 Rol)** sunucu taraflı render edilen (SSR) dinamik HTML arayüzleri, ince (thin) rol bazlı Spring MVC denetleyicileri (`PatientBookingController`, `StaffScheduleController`, `AdminDashboardController`, `AdminBranchController`, `AdminDepartmentController`, `AdminServiceController`, `AdminStaffController`, `CustomErrorController`), `@Valid` form DTO doğrulaması, yerel Bootstrap 5 + `clinic.css`/`clinic.js` varlıkları, güvenli özel hata sayfaları (`400`, `403`, `404`, `500`) ve 88 S08 otomatik test metodu (81 birim/MVC/mimari + 7 `RoleBasedWebMvcIT` PostgreSQL Testcontainers entegrasyon testi) ile tamamlanmış ve depo sahibi tarafından `./mvnw clean verify` (`BUILD SUCCESS`, `121` Failsafe entegrasyon testi sıfır hata/atlama + Surefire kapısı geçildi) ile doğrulanmıştır (Statü: `VERIFIED_BY_OWNER`, commit `9c35715`). S07 tabanı (`386` test) da `VERIFIED_BY_OWNER` olarak doğrulanmıştır. Detaylar için [S08 Uygulama Raporu](docs/07-sprintler/S08-implementation-report.md) incelenebilir.

---

## 1. Proje Kimliği ve Akademik Bağlam

- **Ders:** Nesneye Yönelik / Nesneye Dayalı Programlama (Seçenek A — Hastane/Klinik Randevu Sistemi)
- **Depo (GitHub):** [https://github.com/coskunburak/clinic_Appointment_System-](https://github.com/coskunburak/clinic_Appointment_System-)
- **Varsayılan Dal (Default Branch):** `main`
- **Hedef Kitle:** 1 veya 2 kişilik akademik proje ekibi ve dersin değerlendiricisi / öğretim elemanı.

---

## 2. Akademik Hedefler ve OOP İlkeleri

Projenin temel amacı yalnızca bir web uygulaması sunmak değil, nesneye yönelik programlamanın temel yapı taşlarını ve SOLID ilkelerini üretim kalitesinde modellemektir:

| OOP İlkesi | Planlanan Mimari ve Domain Uygulaması | Durum |
|---|---|---|
| **Kapsülleme (Encapsulation)** | Varlık durumlarının `private` alanlar, `final` değer nesneleri (Value Object: ör. `TimeSlot`, `Money`) ve değişmezlik (immutability) kurallarıyla korunması; kontrolsüz setter yerine doğrulanmış iş metotları. | Modelleniyor (S01/S02) |
| **Kalıtım (Inheritance)** | Ortak varlık tabanları (`BaseEntity`, `AuditableEntity`) ve çalışan hiyerarşisi (`StaffMember` soyut sınıfından `Doctor`, `Nurse`, `Technician` özelleştirmeleri). | Tasarlandı (S02, S05) |
| **Çok Biçimlilik (Polymorphism)** | Farklı randevu onaylama kuralları, uygunluk doğrulama stratejileri ve bildirim mekanizmalarının polimorfik arayüzler (`AppointmentRule`, `SlotValidator`) üzerinden yürütülmesi. | Tasarlandı (S03, S06) |
| **Soyutlama (Abstraction)** | Veri erişim (`Repository`), randevu çakışma çözümü (`BookingService`), zaman dilimi ve bildirim mekanizmalarının arayüz sözleşmeleri arkasına gizlenmesi. | Uygulandı (S01/S06) |
| **Arayüz Tabanlı Tasarım (Interface-Based)** | Servisler arası doğrudan somut bağımlılık yerine arayüz (`interface`) tanımları üzerinden Bağımlılıkların Ters Çevrilmesi (DIP). | Uygulandı (S01/S04) |
| **Bileşim (Composition)** | `Organization` -> `Branch` -> `Department` hiyerarşisi ve randevularda saklanan anlık fiyat/süre `Snapshot` bileşenleri. | Uygulandı (S01/S03) |
| **SOLID Prensipleri** | Tek Sorumluluk (SRP), Açık/Kapalı (OCP), Liskov İkamesi (LSP), Arayüz Ayrımı (ISP) ve Bağımlılıkların Ters Çevrilmesi (DIP) kurallarına sıkı bağlılık. | Uygulandı (Tüm Sprintler) |

---

## 3. Teknoloji Yığını

Projede kullanılan teknolojiler **mevcut kurulu bağımlılıklar** ve **gelecek sprintlerde planlanan bağımlılıklar** olarak yapılandırılmıştır:

### A. Mevcut Kurulu Bağımlılıklar (`pom.xml` teyitli)
- **Dil:** Java 21 (LTS — Eclipse Temurin / OpenJDK)
- **Çerçeve:** Spring Boot 3.4.2 (`spring-boot-starter-parent` — Kararlı LTS tabanı)
- **Web:** `spring-boot-starter-web` (Spring Web MVC ve gömülü Apache Tomcat)
- **Şablon Motoru:** `spring-boot-starter-thymeleaf` (Sunucu taraflı HTML render)
- **Doğrulama:** `spring-boot-starter-validation` (Jakarta Bean Validation / Hibernate Validator)
- **Kalıcılık (ORM):** `spring-boot-starter-data-jpa` (Spring Data JPA / Hibernate 6)
- **Veritabanı Sürücüsü:** `org.postgresql:postgresql` (PostgreSQL JDBC Driver)
- **Şema Migrasyonu:** `org.flywaydb:flyway-core` ve `org.flywaydb:flyway-database-postgresql` (Flyway Community)
- **Gözlem ve Teşhis:** `spring-boot-starter-actuator` (Sadece `/actuator/health` aktif)
- **Geliştirici Araçları:** `spring-boot-devtools` (Runtime/Optional)
- **Konteyner ve Sanallaştırma:** Docker Compose (PostgreSQL 17 Alpine)
- **Test Altyapısı:**
  - `spring-boot-starter-test` (JUnit Jupiter 5, Mockito, AssertJ)
  - `org.testcontainers:postgresql` ve `org.testcontainers:junit-jupiter` (Gerçek PostgreSQL Testcontainers)
  - `org.springframework.boot:spring-boot-testcontainers` (ServiceConnection entegrasyonu)
- **Derleme Aracı:** Apache Maven Wrapper (`mvnw`, `mvnw.cmd` — Maven 3.9.9, Failsafe entegrasyonu)
- **CI / CD:** GitHub Actions (`.github/workflows/ci.yml` — Java 21 & Maven verify)

### B. Uygulanan Ek Ekosistem Bileşenleri (S04 / S08)
- **Güvenlik & Yetkilendirme:** Spring Security 6 (`spring-boot-starter-security` — S04 sprintinde uygulandı)
- **Arayüz Bileşenleri:** Bootstrap 5.3.3 yerel statik CSS/JS paketi (`clinic.css`, `clinic.js` — S08 sprintinde uygulandı)

---

## 4. Uygulama Özellikleri (Fonksiyonel Kapsam)

Sistemin tam teslimatta destekleyeceği ana modüller ve mevcut durumları:

1. **Kurum ve Şube Altyapısı:** Çok şubeli sağlık organizasyonu, şube çalışma gün/saatleri ve zaman dilimi modeli *(S01'de veritabanı şeması ve entity katmanı hazırlandı; yönetim servisleri S03)*.
2. **Kimlik ve Profil Altyapısı:** Kullanıcı hesapları, hasta/personel profilleri ve kurum/şube yetki kapsamı *(S01'de V1 tablosu ve entity katmanı hazırlandı; kimlik doğrulama S04)*.
3. **Bölüm ve Hizmet Kataloğu:** Tıbbi bölümler, hizmet adları, işlem süreleri ve ücret tanımları *(Planlandı — S03)*.
4. **Doktor ve Sağlık Personeli:** Personel türleri, uzmanlık alanları ve personelin verebileceği hizmet eşleştirmeleri *(Planlandı — S05)*.
5. **Uygunluk ve Çalışma Takvimi:** Haftalık mesai şablonları, özel izinler ve müsaitlik aralıkları *(Planlandı — S05)*.
6. **Randevu Rezervasyonu:** Şube, hizmet, doktor ve geçerli zaman aralığı seçerek randevu alma *(Planlandı — S06)*.
7. **Çakışma ve Çift Rezervasyon Önleme:** Eşzamanlı isteklerde satır kilitleme ve çakışma kontrolü *(Planlandı — S06)*.
8. **Randevu Yaşam Döngüsü:** `PENDING` -> `CONFIRMED` / `REJECTED` / `CANCELLED` / `COMPLETED` geçişleri ve append-only denetim geçmişi *(Uygulandı — S07)*.
9. **Rol ve Erişim Yönetimi & Kullanıcı Ekranları:** Hasta, Doktor/Sağlık Personeli ve Yönetici rolleri için sunucu taraflı dinamik Thymeleaf kullanıcı arayüzleri *(Uygulandı — S08)*.

---

## 5. Sistem Gereksinimleri

- **Java Development Kit:** JDK 21 (Eclipse Temurin veya OpenJDK 21)
- **Konteyner Motoru:** Docker / Docker Desktop (PostgreSQL 17 yerel geliştirme ve Testcontainers için)
- **IDE:** IntelliJ IDEA (Community veya Ultimate) / VS Code
- **Derleyici / Paket Yöneticisi:** Proje içerisindeki Maven Wrapper (`./mvnw`)
- **Git:** Sürüm 2.30 veya üzeri

---

## 6. Yerel Kurulum ve Çalıştırma

### 1. Depoyu Klonlayın
```bash
git clone https://github.com/coskunburak/clinic_Appointment_System-.git
cd clinic_Appointment_System-
```

### 2. Yerel Ortam Değişkenlerini Hazırlayın (.env)
```bash
cp .env.example .env
```
*(Gerekirse `.env` içerisindeki port veya şifre ayarlarını yerel ortamınıza göre düzenleyin).*

### 3. PostgreSQL Veritabanını Başlatın (Docker Compose)
```bash
docker compose up -d
docker compose ps
```

### 4. Testleri ve Entegrasyon Testlerini Çalıştırın
Tüm birim ve Testcontainers tabanlı PostgreSQL entegrasyon testlerini çalıştırmak için:
```bash
./mvnw clean verify
```

### 5. Uygulamayı Başlatın
```bash
./mvnw spring-boot:run
```
Uygulama varsayılan olarak `http://localhost:8080` adresinde başlayacaktır.

Sağlık denetimi endpoint'i:
```bash
curl http://localhost:8080/actuator/health
```

> **Önemli Not:** S08 ile birlikte giriş (`/login`), hasta (`/patient/**`), personel (`/staff/**`), yönetici (`/admin/**`) ekranları ve `/error` sayfaları eklenmiştir. Sağlık denetimi için `/actuator/health` aktiftir.

---

## 7. Depo ve Dokümantasyon Yapısı

```text
clinic-appointment/
├── .github/
│   └── workflows/
│       └── ci.yml                        # GitHub Actions CI iş akışı (Java 21, verify)
├── .mvn/wrapper/                         # Maven Wrapper yapılandırması (Maven 3.9.9)
├── compose.yaml                          # PostgreSQL 17 yerel Docker Compose tanımı
├── .env.example                          # Yerel ortam değişkenleri şablonu
├── docs/                                 # Kapsamlı teknik ve akademik dokümantasyon
│   ├── README.md                         # Dokümantasyon indeksi
│   ├── 00-baslangic/                     # Künye, kararlar ve sözlük
│   ├── 01-urun/                          # FR/NFR gereksinim matrisi, TC test senaryoları
│   ├── 02-mimari/                        # Mimari katmanlar, paketler, ADR kayıtları
│   ├── 03-domain/                        # İş kuralları, çakışma algoritması, durum makinesi
│   ├── 04-veri/                          # ERD, tablolar ve Flyway migrasyon planı
│   ├── 05-arayuz-api/                    # Ekran akışları ve form sözleşmeleri
│   ├── 06-kalite/                        # Test stratejisi, CI kalitesi, hata ayıklama rehberi
│   ├── 07-sprintler/                     # S00–S10 sprint hedefleri ve uygulama raporları
│   ├── 08-teslim/                        # Commit takvimi, sunum rehberi, akademik teslim
│   ├── 09-ajana-talimat/                 # Geliştirme kuralları ve governance ilkeleri
│   └── 10-referanslar/                   # Resmî kaynaklar ve sürüm teyitleri
├── src/
│   ├── main/
│   │   ├── java/com/example/clinic/
│   │   │   ├── ClinicAppointmentApplication.java   # Spring Boot ana sınıfı
│   │   │   ├── organization/                       # Organizasyon, şube, bölüm modülü
│   │   │   │   ├── domain/                         # Organization, Branch, Department entity'leri
│   │   │   │   └── infrastructure/                 # JPA repository port ve adapter'ları
│   │   │   └── identity/                           # Kimlik ve profil modülü
│   │   │       ├── domain/                         # UserAccount, Membership, Profile entity'leri
│   │   │       └── infrastructure/                 # JPA repository arayüzleri
│   │   └── resources/
│   │       ├── application.yml                     # Temel Spring Boot konfigürasyonu
│   │       ├── application-dev.yml                 # Yerel geliştirme profili
│   │       ├── application-test.yml                # Entegrasyon test profili
│   │       └── db/migration/
│   │           └── V1__create_organization_and_identity.sql # Flyway baseline şeması
│   └── test/
│       └── java/com/example/clinic/
│           ├── AbstractIntegrationTest.java        # Testcontainers PostgreSQL ortak tabanı
│           ├── ClinicAppointmentApplicationTests.java # Context smoke testi
│           ├── FlywayMigrationIT.java              # TC-039 Flyway migrasyon testi
│           ├── OrganizationRepositoryIT.java       # Kurum/şube persistence testi
│           ├── IdentityRepositoryIT.java           # Kimlik/profil persistence testi
│           └── DatabaseConstraintIT.java           # Negatif kısıt ve bütünlük testleri
├── .gitignore
├── mvnw / mvnw.cmd
├── pom.xml                               # Java 21, Boot 3.4.2, JPA, Flyway, Testcontainers
└── README.md                             # Bu ana doküman
```

---

## 8. Sprint Yol Haritası (S00 – S10)

| Sprint | Başlık | Durum | Temel Çıktı |
|---|---|---|---|
| **S00** | Kapsam Dondurma ve GitHub Hazırlığı | **Uygulandı (Doğrulandı)** | Depo temeli, docs normalizasyonu, README, git hygiene, izlenebilirlik, bağımlılık onarımı |
| **S01** | Altyapı, Katmanlar, CI ve Veritabanı Temeli | **Uygulandı (Doğrulandı)** | PostgreSQL 17, Flyway V1, JPA validate, Testcontainers, CI workflow |
| **S02** | OOP Domain Çekirdeği ve Temel Varlıklar | **Uygulandı (Doğrulandı)** | BaseEntity, Value Object'ler, Entity hiyerarşisi, domain testleri |
| **S03** | Kurum, Şube, Bölüm ve Hizmet Kataloğu | **Uygulandı (Doğrulandı)** | Çok şubeli katalog yapısı, süre/ücret tanımları, CRUD servisleri |
| **S04** | Kimlik Doğrulama, Roller ve Yetkilendirme | **Uygulandı (Doğrulandı)** | Spring Security, 3 rol (Hasta, Personel, Yönetici), oturum güvenliği |
| **S05** | Personel Yönetimi ve Çalışma Saatleri | **Uygulandı (Doğrulandı)** | Doktor/personel uygunluk aralıkları, izinler, haftalık mesai |
| **S06** | Randevu Rezervasyonu ve Çakışma Önleme | **Uygulandı (Doğrulandı)** | Slot arama, satır kilitleme (pessimistic lock), çakışmasız PENDING randevu |
| **S07** | Randevu Onay Yaşam Döngüsü ve Geçmiş | **Uygulandı (Doğrulandı)** | Onay/ret/iptal akışı, durum makinesi, append-only denetim geçmişi |
| **S08** | Spring MVC ve Thymeleaf Kullanıcı Ekranları | **Uygulandı (Doğrulandı)** | Hasta, personel ve yönetici arayüzleri, form doğrulama, hata sayfaları |
| **S09** | Test Regresyonu, Eşzamanlılık ve Demo Seed | *Planlandı* | Entegrasyon testleri, eşzamanlı rezervasyon testi, kurmaca seed verisi |
| **S10** | Nihai Kalite Kapısı, Sunum ve Akademik Teslim | *Planlandı* | 10 farklı gün commit kontrolü, sunum hazırlığı, akademik rapor |

---

## 9. Dokümantasyon Gezintisi

- [Dokümantasyon Ana Dizini](docs/README.md)
- [Gereksinim ve İzlenebilirlik Matrisi (FR/NFR/TC)](docs/01-urun/01-gereksinim-matrisi.md)
- [Mimari Katmanlar ve Teknoloji Kararları](docs/02-mimari/01-teknoloji-ve-katmanlar.md)
- [Nesneye Dayalı Tasarım ve OOP Kanıtları](docs/02-mimari/03-oop-ilkeleri-ve-uml.md)
- [Mimari Karar Kayıtları (ADR)](docs/02-mimari/04-mimari-kararlar.md)
- [Veri Modeli ve Tablo Sözleşmeleri](docs/04-veri/02-tablolar-ve-migrasyon.md)
- [Test Stratejisi ve Kalite Kapıları](docs/06-kalite/01-test-stratejisi.md)
- [Sprint S01 Görev ve Kabul Kriterleri](docs/07-sprintler/S01.md)
- [Sprint S01 Uygulama Raporu](docs/07-sprintler/S01-implementation-report.md)
- [GitHub 10 Farklı Gün Commit Takvimi](docs/08-teslim/01-github-commit-takvimi.md)

---

## 10. Akademik Teslimat Kuralları ve Commit Disiplini

1. **Farklı Günlerde Commit Şartı:** GitHub üzerinde **en az 10 commit'in, birbirinden farklı takvim günlerinde** yapılmış olması gerekmektedir. Aynı gün içinde atılan birden fazla commit tek bir gün olarak sayılır. Sahte/geçmişe dönük (backdated) commit kesinlikle yasaktır.
2. **GitHub ve SABİS Gönderimi:** Projenin son kaynak kodu GitHub'a gönderilmeli ve SABİS sistemine süresi içinde teslim edilmelidir.
3. **Canlı Sunum:** Proje dönem sonunda öğretim elemanına canlı olarak sunulacak, nesneye yönelik tasarım soruları cevaplanacaktır.

---

## 11. Güvenlik, Gizlilik ve Yasal Uyarı

- **Kurmaca Veri:** Sistemde kullanılan tüm hasta, doktor, kurum ve randevu verileri tamamen kurmacadır. Gerçek kişi bilgisi, T.C. Kimlik Numarası veya gerçek sağlık verisi kullanılmaz ve saklanmaz.
- **Tıbbi Uyarı:** Bu yazılım bir üniversite eğitim projesi olup, gerçek tıbbi teşhis, tedavi veya klinik süreçlerde kullanılamaz. Sağlık Bakanlığı, e-Nabız veya MHRS ile herhangi bir resmî entegrasyonu, tıbbi cihaz/yazılım sertifikasyonu veya KVKK/HIPAA üretim seviyesi uyumluluk iddiası bulunmamaktadır.

---

## 12. Lisans

Lisans seçimi henüz kesinleşmemiş olup repo sahibinin / öğretim elemanının onayına bağlıdır (`PENDING_OWNER_CONFIRMATION`).
