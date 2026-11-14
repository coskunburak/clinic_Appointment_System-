# Akademik Dönem Projesi Rapor Şablonu (SABİS Teslim Formatı)

Bu belge, **Sakarya Üniversitesi Bilgisayar ve Bilişim Bilimleri Fakültesi, Nesneye Dayalı Programlama Dersi** dönem projesi için SABİS sistemine yüklenecek olan nihai PDF proje raporunun eksiksiz metin iskeletini ve bölüm yapısını tanımlar.

> **S10 Üretilen Nihai Rapor:** Bu şablon esas alınarak oluşturulan 16 sayfalık akademik PDF raporu `deliverables/Clinic_Appointment_System_Final_Report.pdf` ve HTML kaynağı [Clinic_Appointment_System_Final_Report.html](./report/Clinic_Appointment_System_Final_Report.html) konumundadır (teslim tarihi `2026-11-14`, referans SHA `8b70de2`; öğrenci numarası ve öğretim elemanı alanları teslim öncesi öğrenci tarafından doldurulacaktır).

---

# T.C. SAKARYA ÜNİVERSİTESİ
## BİLGİSAYAR VE BİLİŞİM BİLİMLERİ FAKÜLTESİ
### NESNEYE DAYALI PROGRAMLAMA DERSİ DÖNEM PROJESİ RAPORU

- **PROJE BAŞLIĞI:** Hastane ve Çok Şubeli Klinik Randevu Yönetim Sistemi
- **PROJE YOLU:** Seçenek A — Uygulama Geliştirme
- **ÖĞRENCİ ADI SOYADI:** Burak Coşkun
- **ÖĞRENCİ NUMARASI:** *(Öğrenci Numarası)*
- **ÖĞRETİM ELEMANI:** *(Dersin Öğretim Elemanı)*
- **TESLİM TARİHİ:** 2026-11-02
- **GİTHUB REPO ADRESİ:** [https://github.com/coskunburak/clinic_Appointment_System-](https://github.com/coskunburak/clinic_Appointment_System-)

---

## ÖZET
Bu dönem projesinde, tek klinikli muayenehanelerden çok şubeli entegre sağlık kuruluşlarına kadar ölçeklenebilen, kurumsal seviyede bir Hastane/Klinik Randevu Yönetim Sistemi geliştirilmiştir. Proje, Nesneye Dayalı Programlama (OOP) prensiplerini (kapsülleme, kalıtım, çok biçimlilik, soyutlama) ve SOLID tasarım ilkelerini somut bir yazılım ürünü üzerinde doğrulamayı amaçlamaktadır. Sistem; Java 21, Spring Boot 3.4.2, Spring MVC, Thymeleaf, Spring Data JPA ve PostgreSQL 17 teknolojileriyle Modüler Monolit mimarisinde inşa edilmiştir. Veritabanı şeması Flyway ile sürümlenmiş, test süreçleri Testcontainers PostgreSQL ile gerçek veritabanı konteynerleri üzerinde otomatikleştirilmiştir. Sistemde üç ana rol (Hasta, Sağlık Personeli/Doktor, Klinik Yöneticisi) tanımlanmış; randevu çakışmalarını sıfıra indirmek amacıyla pesimist satır kilitleme (`PESSIMISTIC_WRITE`) ve anlık fiyat/süre dondurma (`Snapshot`) desenleri uygulanmıştır.

**Anahtar Kelimeler:** Nesneye Dayalı Programlama, Java 21, Spring Boot, Modüler Monolit, PostgreSQL, Randevu Çakışma Önleme, Pesimist Kilitleme, Testcontainers.

---

## BÖLÜM 1: GİRİŞ VE PROBLEM TANIMI
- **1.1. Sağlık Sektöründe Randevu Yönetim Problemi:** Çok şubeli sağlık kuruluşlarında hekimlerin farklı günlerde farklı şubelerde görev alması, mesai aralıkları, randevu onay süreçleri ve eşzamanlı rezervasyon isteklerinde yaşanan çift rezervasyon (double-booking) yarış durumları (race condition).
- **1.2. Projenin Amacı ve Seçenek A Tercihi:** Dil ve platform bağımsız akademik ödev şartnamesinde yer alan Seçenek A seçilmiş; saf nesneye yönelik programlama yetkinliklerini endüstriyel standartta kanıtlamak üzere Java ekosistemi tercih edilmiştir.
- **1.3. Kapsam ve Temel Kabuller:** Sistemin sınırları, tekil ve çok şubeli kurum desteği, kurmaca veri ilkesi ve tıbbi teşhis/reçete sistemlerinin kapsam dışı bırakılması.

---

## BÖLÜM 2: SİSTEM GEREKSİNİMLERİ VE ANALİZ
- **2.1. Kullanıcı Rolleri:**
  - *Hasta:* Hekim ve hizmet seçimi, randevu talebi, durum takibi, iptal.
  - *Doktor / Sağlık Personeli:* Mesai takvimi görüntüleme, randevu onay/ret işlemleri.
  - *Hastane / Klinik Yöneticisi:* Kurum, şube, bölüm, hizmet kataloğu ve mesai şablonları yönetimi.
- **2.2. İşlevsel Gereksinimler (Functional Requirements — FR):**
  - Kurum ve şube tanımlamaları (FR-001, FR-002, FR-003).
  - Çalışan yönetimi, uzmanlık ve mesai aralıkları (FR-004, FR-005, FR-006).
  - Randevu alma, çakışma kontrolü ve onay mekanizması (FR-007, FR-008, FR-009, FR-010).
  - Rol tabanlı erişim kontrolü (FR-011, FR-012, FR-013).
- **2.3. İşlevsel Olmayan Gereksinimler (Non-Functional Requirements — NFR):**
  - Sıfırdan tekrar üretilebilir veritabanı (NFR-003 / Flyway V1).
  - Eşzamanlı isteklerde veri tutarlılığı ve ACID izolasyonu (NFR-004).
  - GitHub üzerinde en az 10 farklı takvim gününde commit geçmişi (NFR-006).

---

## BÖLÜM 3: NESNEYE DAYALI TASARIM (OOP) VE MİMARİ
- **3.1. Dört Temel OOP İlkesinin Uygulanışı:**
  - *Kapsülleme (Encapsulation):* Zengin domain entity'leri (`Appointment`), değişmez değer nesneleri (`Money`, `TimeSlot`), doğrudan setter yasağı.
  - *Kalıtım (Inheritance):* `PersonProfile` soyut temel sınıfı ve ondan türeyen `PatientProfile` ile `StaffProfile` sınıfları.
  - *Çok Biçimlilik (Polymorphism):* `SchedulingConstraint` arayüzü ve mesai, uygunluk, yetkinlik, çakışma strateji sınıfları.
  - *Soyutlama (Abstraction):* Repository portları ve `Clock` soyutlaması ile altyapı bağımsızlığı.
- **3.2. SOLID Prensiplerine Bağlılık:** Her bir prensibin (SRP, OCP, LSP, ISP, DIP) kaynak kod içerisindeki somut karşılıkları ve sınıf bağımlılıkları.
- **3.3. Uygulanan Tasarım Desenleri (Design Patterns):**
  - Strategy Pattern (`SchedulingConstraint`)
  - State Pattern (`AppointmentStatus` yaşam döngüsü)
  - Repository Pattern (`JpaRepository`)
  - Snapshot Pattern (İşlem süresi ve ücretin dondurulması)
- **3.4. UML Sınıf Diyagramı:** Sistemin tüm varlıklarını, kalıtım ve bileşim ilişkilerini gösteren detaylı sınıf diyagramı.

---

## BÖLÜM 4: SİSTEM MİMARİSİ VE VERİTABANI MODELİ
- **4.1. Modüler Monolit ve Package-by-Feature Mimarisi:** Katmanlı monolit yerine alan odaklı dikey dilimleme (`organization`, `identity`, `catalog`, `staff`, `scheduling`, `booking`).
- **4.2. İlişkisel Veri Modeli (ERD):** Tablolar arası ilişkiler, birincil ve yabancı anahtarlar.
- **4.3. Çok Kiracılı Veri Bütünlüğü (Tenant Integrity):** `membership` tablosundaki `(branch_id, organization_id)` kompozit yabancı anahtar kısıtı ile kurum sınırlarının veritabanı seviyesinde korunması.
- **4.4. Flyway Migrasyon Stratejisi:** `ddl-auto=validate` prensibi ve şemanın sürümlenmesi.

---

## BÖLÜM 5: ÇAKIŞMA ÖNLEME VE ONAY ALGORİTMALARI
- **5.1. Yarı Açık Aralık (`[start, end)`) Çakışma Formülü:** Matematiksel aralık kesişim kuralı ve bitişik randevuların çakışma sayılmaması mantığı.
- **5.2. Pesimist Satır Kilitleme Protokolü:** `PESSIMISTIC_WRITE` ile eşzamanlı iki istekte yarış durumunun engellenmesi.
- **5.3. Randevu Yaşam Döngüsü ve Durum Makinesi:** `PENDING` durumunun slottan düşmesi, hekim onayı (`CONFIRMED`), red veya iptal geçişleri.
- **5.4. Finansal Snapshot Koruması:** Katalogdaki fiyat değişikliklerinin geçmiş randevuları etkilememesi.

---

## BÖLÜM 6: DOĞRULAMA, TEST VE KALİTE GÜVENCESİ
- **6.1. Birim Testler:** Domain kuralları ve durum makinesi testleri (JUnit 5, AssertJ).
- **6.2. Entegrasyon Testleri (Testcontainers PostgreSQL 17):**
  - `FlywayMigrationIT`: Sıfırdan kurulum, idempotency ve doğrulama testi (TC-039).
  - `OrganizationRepositoryIT` & `IdentityRepositoryIT`: Veri tabanına yazma ve okuma testleri.
  - `DatabaseConstraintIT`: Negatif benzersizlik, yabancı anahtar ve kısıt ihlal testleri.
- **6.3. Sürekli Entegrasyon (GitHub Actions CI):** `.github/workflows/ci.yml` üzerinden her push ve PR'da otomatik derleme ve test doğrulaması.

---

## BÖLÜM 7: PROJE YÖNETİMİ VE GİTHUB COMMİT DİSİPLİNİ
- **7.1. Sprint Metodolojisi (S00 – S10):** Aşamalı geliştirme adımları ve Definition of Done kriterleri.
- **7.2. 10 Farklı Gün Commit Takvimi:** Akademik kural gereği farklı takvim günlerine yayılan commit tarihçesi tablosu ve SHA dökümü.

---

## BÖLÜM 8: SONUÇ, KISITLAR VE GELECEK ÇALIŞMALAR
- **8.1. Elde Edilen Başarılar:** OOP ilkelerine tam uyumlu, çakışmasız, kurumsal seviyede randevu altyapısı.
- **8.2. Sistemin Kısıtları:** Gerçek sağlık sistemleri (e-Nabız, MHRS) veya kredi kartı entegrasyonu içermemesi; kurmaca veri ilkesi.
- **8.3. Gelecek Geliştirmeler:** E-posta/SMS bildirimleri, mobil API uçları, ileri analitik raporlama ekranları.

---

## KAYNAKÇA
1. Gamma, E., Helm, R., Johnson, R., & Vlissides, J. (1994). *Design Patterns: Elements of Reusable Object-Oriented Software*. Addison-Wesley.
2. Martin, R. C. (2017). *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Prentice Hall.
3. Evans, E. (2003). *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Addison-Wesley.
4. Spring Framework Documentation. (2026). *Spring Boot Reference Guide (v3.4.2)*. https://docs.spring.io/spring-boot/
5. PostgreSQL Global Development Group. (2026). *PostgreSQL 17 Documentation: Concurrency Control and Explicit Locking*. https://www.postgresql.org/docs/17/
6. Redgate. (2026). *Flyway Database Migrations Documentation*. https://documentation.red-gate.com/fd
7. Testcontainers. (2026). *PostgreSQL Module Guide*. https://java.testcontainers.org/modules/databases/postgres/
