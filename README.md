# Hastane ve Klinik Randevu Yönetim Sistemi (Clinic Appointment System)

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Build Tool](https://img.shields.io/badge/Maven%20Wrapper-3.9.9-blue.svg)](https://maven.apache.org/)
[![Sprint](https://img.shields.io/badge/Sprint-S00%20(Kapsam%20Dondurma)-yellow.svg)](docs/07-sprintler/S00.md)
[![Academic Status](https://img.shields.io/badge/Academic%20Project-OOP%20Seçenek%20A-informational.svg)](docs/00-baslangic/02-proje-kunyesi-ve-basari.md)

Bu proje, üniversite **Nesneye Dayalı Programlama (OOP)** dersi kapsamında geliştirilen, tek ve çok şubeli sağlık kuruluşlarının randevu süreçlerini uçtan uca yönetmeyi amaçlayan web tabanlı bir hastane/klinik randevu yönetim sistemidir.

> **Mevcut Proje Durumu:** **Sprint S00 — Kapsam Dondurma ve GitHub Hazırlığı** tamamlanmış olup depo temeli, dokümantasyon hiyerarşisi, Maven bağımlılıkları ve akademik sözleşmeler üretim seviyesine (production-ready) standardize edilmiştir. Sistem şu anda doğrulanmış Spring Boot 3.4.2 temelindedir; iş mantığı ve veritabanı katmanları sonraki sprintlerde aşamalı olarak eklenecektir.

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
| **Kapsülleme (Encapsulation)** | Varlık durumlarının `private` alanlar, `final` değer nesneleri (Value Object: ör. `TimeSlot`, `Money`) ve değişmezlik (immutability) kurallarıyla korunması; kontrolsüz setter yerine doğrulanmış iş metotları. | Tasarlandı (S02) |
| **Kalıtım (Inheritance)** | Ortak varlık tabanları (`BaseEntity`, `AuditableEntity`) ve çalışan hiyerarşisi (`StaffMember` soyut sınıfından `Doctor`, `Nurse`, `Technician` özelleştirmeleri). | Tasarlandı (S02, S05) |
| **Çok Biçimlilik (Polymorphism)** | Farklı randevu onaylama kuralları, uygunluk doğrulama stratejileri ve bildirim mekanizmalarının polimorfik arayüzler (`AppointmentRule`, `SlotValidator`) üzerinden yürütülmesi. | Tasarlandı (S03, S06) |
| **Soyutlama (Abstraction)** | Veri erişim (`Repository`), randevu çakışma çözümü (`BookingService`), zaman dilimi ve bildirim mekanizmalarının arayüz sözleşmeleri arkasına gizlenmesi. | Tasarlandı (S02, S06) |
| **Arayüz Tabanlı Tasarım (Interface-Based)** | Servisler arası doğrudan somut bağımlılık yerine arayüz (`interface`) tanımları üzerinden Bağımlılıkların Ters Çevrilmesi (DIP). | Tasarlandı (S02, S04) |
| **Bileşim (Composition)** | `Organization` -> `Branch` -> `Department` hiyerarşisi ve randevularda saklanan anlık fiyat/süre `Snapshot` bileşenleri. | Tasarlandı (S03, S06) |
| **SOLID Prensipleri** | Tek Sorumluluk (SRP), Açık/Kapalı (OCP), Liskov İkamesi (LSP), Arayüz Ayrımı (ISP) ve Bağımlılıkların Ters Çevrilmesi (DIP) kurallarına sıkı bağlılık. | Tasarlandı (Tüm Sprintler) |

> *Not:* Yukarıdaki ilkeler mimari dokümanlarda tanımlanmıştır. İlgili Java sınıfları S02, S03, S05 ve S06 sprintlerinde repoya dahil edilecektir.

---

## 3. Teknoloji Yığını

Projede kullanılan teknolojiler **mevcut doğrulanmış bağımlılıklar** ve **gelecek sprintlerde planlanan bağımlılıklar** olarak net şekilde ayrılmıştır:

### A. Mevcut Kurulu Bağımlılıklar (`pom.xml` teyitli)
- **Dil:** Java 21 (LTS)
- **Çerçeve:** Spring Boot 3.4.2 (`spring-boot-starter-parent` — Resmî kararlı LTS sürümü)
- **Web:** `spring-boot-starter-web` (Spring Web MVC ve gömülü Apache Tomcat)
- **Şablon Motoru:** `spring-boot-starter-thymeleaf` (Sunucu taraflı HTML render)
- **Doğrulama:** `spring-boot-starter-validation` (Jakarta Bean Validation / Hibernate Validator)
- **Geliştirici Araçları:** `spring-boot-devtools` (Runtime/Optional)
- **Test:** `spring-boot-starter-test` (JUnit Jupiter 5, Mockito, Spring Test, AssertJ, Hamcrest)
- **Derleme Aracı:** Apache Maven Wrapper (`mvnw`, `mvnw.cmd` — Maven 3.9.9)

### B. Gelecek Sprintlerde Planlanan Teknolojiler
- **Veritabanı:** PostgreSQL 16+ (S01 sprintinde planlandı)
- **Veritabanı Migrasyonu:** Flyway (`flyway-core`, `flyway-database-postgresql`, S01 sprintinde planlandı)
- **Kalıcılık (ORM):** Spring Data JPA / Hibernate (S01 sprintinde planlandı)
- **Güvenlik & Yetkilendirme:** Spring Security (S04 sprintinde planlandı)
- **Arayüz Bileşenleri:** Bootstrap 5 (S08 sprintinde planlandı)

---

## 4. Uygulama Özellikleri (Fonksiyonel Kapsam)

Sistemin tam teslimatta destekleyeceği ana modüller ve mevcut durumları:

1. **Kurum ve Şube Yönetimi:** Çok şubeli sağlık organizasyonu tanımı, şube çalışma gün/saatleri ve zaman dilimi yönetimi *(Planlandı — S03)*.
2. **Bölüm ve Hizmet Kataloğu:** Tıbbi bölümler, hizmet adları, işlem süreleri ve ücret tanımları *(Planlandı — S03)*.
3. **Doktor ve Sağlık Personeli:** Personel türleri, uzmanlık alanları ve personelin verebileceği hizmet eşleştirmeleri *(Planlandı — S05)*.
4. **Uygunluk ve Çalışma Takvimi:** Haftalık mesai şablonları, özel izinler ve müsaitlik aralıkları *(Planlandı — S05)*.
5. **Randevu Rezervasyonu:** Şube, hizmet, doktor ve geçerli zaman aralığı seçerek randevu alma *(Planlandı — S06)*.
6. **Çakışma ve Çift Rezervasyon Önleme:** Eşzamanlı isteklerde satır kilitleme ve çakışma kontrolü *(Planlandı — S06)*.
7. **Randevu Yaşam Döngüsü:** `PENDING` -> `CONFIRMED` / `REJECTED` / `CANCELLED` / `COMPLETED` geçişleri ve işlem geçmişi *(Planlandı — S07)*.
8. **Rol ve Erişim Yönetimi:** Hasta, Doktor/Sağlık Personeli ve Yönetici rolleri *(Planlandı — S04 ve S08)*.

---

## 5. Sistem Gereksinimleri

- **Java Development Kit:** JDK 21 (OpenJDK / Eclipse Temurin önerilir)
- **IDE:** IntelliJ IDEA (Community veya Ultimate) / VS Code / Eclipse
- **Derleyici / Paket Yöneticisi:** Proje içerisindeki Maven Wrapper (`./mvnw`) veya sistemdeki Maven 3.9+ kullanılır
- **Git:** Sürüm 2.30 veya üzeri
- **Ağ Bağlantısı:** İlk çalıştırmada Maven bağımlılıklarının indirilmesi için internet erişimi gereklidir
- **Veritabanı (İleride):** S01 ve sonrasında yerel PostgreSQL veya Docker Desktop gerekecektir (S00 aşamasında veritabanı kurulumu gerekmez).

---

## 6. Yerel Kurulum ve Çalıştırma

### 1. Depoyu Klonlayın
```bash
git clone https://github.com/coskunburak/clinic_Appointment_System-.git
cd clinic_Appointment_System-
```

### 2. IntelliJ IDEA ile Projeyi Açın
1. IntelliJ IDEA'da **File -> Open...** seçeneğiyle proje dizinini seçin.
2. **Project Structure (File -> Project Structure)** menüsünde:
   - **SDK:** JDK 21
   - **Project language level:** 21 (veya SDK default) olarak ayarlayın.
3. Maven sekmesinden **Reload All Maven Projects** ikonuna tıklayarak bağımlılıkları senkronize edin.

### 3. Testleri Çalıştırın
Harici Maven gerekmeksizin Maven Wrapper kullanılabilir:
```bash
./mvnw clean test
```
Sistemde kurulu Maven ile:
```bash
mvn clean test
```
Windows ortamında:
```cmd
mvnw.cmd clean test
```

### 4. Uygulamayı Başlatın
```bash
./mvnw spring-boot:run
```
Uygulama varsayılan olarak `http://localhost:8080` adresinde başlayacaktır.

> **Önemli Not:** S00 aşamasında yalnızca bootstrap ve context testi yer almaktadır; web controller ve HTML şablonları S08'de eklenecektir. Bu nedenle `http://localhost:8080/` adresine tarayıcı ile gidildiğinde Spring Boot'un varsayılan `404 Not Found` (Whitelabel Error Page) yanıtı vermesi **beklenen ve normal** bir durumdur.

---

## 7. Depo ve Dokümantasyon Yapısı

Sprint S00 kapsamında dokümantasyon ağacı `docs/` altında normalize edilmiş, gereksiz katmanlar temizlenmiştir:

```text
clinic-appointment/
├── .mvn/wrapper/                 # Maven Wrapper yapılandırması (Maven 3.9.9)
├── docs/                         # Kapsamlı teknik ve akademik dokümantasyon
│   ├── README.md                 # Dokümantasyon indeksi ve başlangıç rehberi
│   ├── BASLA_BURADAN.txt         # Hızlı başlangıç özeti
│   ├── 00-baslangic/             # Kararlar, varsayımlar, künye ve terimler sözlüğü
│   ├── 01-urun/                  # FR/NFR gereksinim matrisi, kullanıcı hikâyeleri, senaryolar
│   ├── 02-mimari/                # Mimari katmanlar, paketler, OOP analizi, ADR kayıtları
│   ├── 03-domain/                # İş kuralları, çakışma algoritması, durum makinesi, yetkiler
│   ├── 04-veri/                  # Veri modeli (ERD), tablolar, migrasyon ve seed verisi
│   ├── 05-arayuz-api/            # Ekran akışları, MVC endpoint sözleşmeleri, form doğrulamaları
│   ├── 06-kalite/                # Test stratejisi, test senaryoları, güvenlik, manuel checklist
│   ├── 07-sprintler/             # S00–S10 sprint hedefleri, DoD kriterleri ve sprint raporları
│   ├── 08-teslim/                # GitHub commit takvimi, sunum rehberi, rapor şablonu, riskler
│   ├── 09-ajana-talimat/         # Geliştirme kuralları, prompt şablonları, governance ilkeleri
│   └── 10-referanslar/           # Resmî kaynaklar, sürüm teyitleri ve değişiklik günlüğü
├── src/
│   ├── main/
│   │   ├── java/com/example/clinic/
│   │   │   └── ClinicAppointmentApplication.java   # Spring Boot giriş noktası
│   │   └── resources/
│   │       └── application.properties             # Temel yapılandırma
│   └── test/
│       └── java/com/example/clinic/
│           └── ClinicAppointmentApplicationTests.java # Context yükleme testi
├── .gitattributes
├── .gitignore                    # Mac, IDE ve gizli dosya korumaları güncellendi
├── HELP.md
├── mvnw
├── mvnw.cmd
├── pom.xml                       # Maven proje nesne modeli (Java 21, Spring Boot 3.4.2)
└── README.md                     # Bu ana doküman
```

---

## 8. Sprint Yol Haritası (S00 – S10)

| Sprint | Başlık | Durum | Temel Çıktı |
|---|---|---|---|
| **S00** | Kapsam Dondurma ve GitHub Hazırlığı | **Uygulandı (Test Bekliyor)** | Depo temeli, docs normalizasyonu, README, git hygiene, izlenebilirlik, bağımlılık onarımı |
| **S01** | Altyapı, Katmanlar, CI ve Veritabanı Temeli | *Planlandı* | PostgreSQL, Flyway V1, CI workflow, paket iskeleti |
| **S02** | OOP Domain Çekirdeği ve Temel Varlıklar | *Planlandı* | BaseEntity, Value Object'ler, Entity hiyerarşisi, domain testleri |
| **S03** | Kurum, Şube, Bölüm ve Hizmet Kataloğu | *Planlandı* | Çok şubeli katalog yapısı, süre/ücret tanımları, CRUD servisleri |
| **S04** | Kimlik Doğrulama, Roller ve Yetkilendirme | *Planlandı* | Spring Security, 3 rol (Hasta, Personel, Yönetici), oturum güvenliği |
| **S05** | Personel Yönetimi ve Çalışma Saatleri | *Planlandı* | Doktor/personel uygunluk aralıkları, izinler, haftalık mesai |
| **S06** | Randevu Rezervasyonu ve Çakışma Önleme | *Planlandı* | Slot arama, satır kilitleme (pessimistic lock), çakışmasız PENDING randevu |
| **S07** | Randevu Onay Yaşam Döngüsü ve Geçmiş | *Planlandı* | Onay/ret/iptal akışı, durum makinesi, snapshot veri saklama |
| **S08** | Spring MVC ve Thymeleaf Kullanıcı Ekranları | *Planlandı* | Hasta, personel ve yönetici arayüzleri, form doğrulama, hata sayfaları |
| **S09** | Test Regresyonu, Eşzamanlılık ve Demo Seed | *Planlandı* | Entegrasyon testleri, eşzamanlı rezervasyon testi, kurmaca seed verisi |
| **S10** | Nihai Kalite Kapısı, Sunum ve Akademik Teslim | *Planlandı* | 10 farklı gün commit kontrolü, sunum hazırlığı, akademik rapor |

---

## 9. Dokümantasyon Gezintisi

Detaylı teknik dokümantasyona aşağıdaki bağlantılardan erişebilirsiniz:

- [Dokümantasyon Ana Dizini](docs/README.md)
- [Gereksinim ve İzlenebilirlik Matrisi (FR/NFR/TC)](docs/01-urun/01-gereksinim-matrisi.md)
- [Mimari Katmanlar ve Teknoloji Kararları](docs/02-mimari/01-teknoloji-ve-katmanlar.md)
- [Nesneye Dayalı Tasarım ve OOP Kanıtları](docs/02-mimari/03-oop-ilkeleri-ve-uml.md)
- [Mimari Karar Kayıtları (ADR)](docs/02-mimari/04-mimari-kararlar.md)
- [Test Stratejisi ve Kalite Kapıları](docs/06-kalite/01-test-stratejisi.md)
- [GitHub 10 Farklı Gün Commit Takvimi](docs/08-teslim/01-github-commit-takvimi.md)
- [Akademik Teslim ve Sunum Kılavuzu](docs/08-teslim/02-sunum-ve-demo.md)
- [Sprint S00 Uygulama Raporu](docs/07-sprintler/S00-implementation-report.md)

---

## 10. Akademik Teslimat Kuralları ve Commit Disiplini

Ders yönergesi uyarınca teslimatın geçerli sayılması için aşağıdaki koşulların eksiksiz sağlanması zorunludur:

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
