# Proje Künyesi, Akademik Sözleşme ve Başarı Ölçütleri

Bu belge, **Sakarya Üniversitesi Bilgisayar ve Bilişim Bilimleri Fakültesi, Nesneye Dayalı Programlama (OOP)** dersi dönem projesinin resmî künyesini, başarı ölçütlerini ve teslimat sözleşmesini tanımlar.

---

## 1. Proje Kimlik Kartı

| Alan | Bilgi |
|---|---|
| **Ders Adı** | Nesneye Dayalı / Yönelik Programlama (OOP / NDP) |
| **Seçilen Proje Yolu** | **Seçenek A — Hastane / Klinik Randevu Sistemi Geliştirme** |
| **Uygulama Kapsamı** | Hem **tek bir klinik/hastane** için hem de **çok şubeli sağlık kuruluşlarını** destekleyecek modüler mimari |
| **Temel Amaç** | Nesneye dayalı programlama ilkelerini (sınıf yapısı, kalıtım, çok biçimlilik, kapsülleme vb.) somut, çalışan ve üretim kalitesinde bir yazılım ürününe dönüştürmek |
| **Ekip Yapısı** | Bireysel veya Maksimum 2 Kişilik Grup (Öğrenci: Burak Coşkun) |
| **Konu Seçim Tescili** | 13 Ekim 2026 tarihine kadar Google E-Tablo üzerinden tescil edilmiştir |
| **Geliştirme Dili & Sürüm** | Java 21 (LTS — Eclipse Temurin / OpenJDK 21) |
| **Platform & Çerçeve** | Spring Boot 3.4.2 (Spring MVC, Thymeleaf, Spring Data JPA, Actuator) |
| **Veritabanı & Migrasyon** | PostgreSQL 17 (Docker Compose) & Flyway Community (V1..V5) |
| **Test Altyapısı** | JUnit Jupiter 5, Mockito, AssertJ, Spring Boot Test, Testcontainers PostgreSQL 17 |
| **Kod Deposu (GitHub)** | [https://github.com/coskunburak/clinic_Appointment_System-](https://github.com/coskunburak/clinic_Appointment_System-) |
| **Varsayılan Dal** | `main` |
| **Teslim Kanalları** | GitHub Repository + SABİS Sistemi Yüklemesi + Canlı Akademik Sunum |

---

## 2. Öğretim Elemanı Resmî Gereksinimleri ve Karşılıkları

Ders izlencesinde Seçenek A için zorunlu kılınan tüm işlevsel maddelerin projedeki mimari karşılıkları:

### A. Hastane / Klinik Tanımlamaları
1. **Yetkili Kurum/Klinik Tanımı:** Uygulamada hastaneler veya klinikler yetki çerçevesinde tanımlanabilecektir (`Organization`, `Branch`).
2. **Çalışma Saatleri ve Bölümler:** Her klinik/hastane için çalışma günleri, saat aralıkları (`BranchOpeningHours`) ve tıbbi bölümler (`Department`) tanımlanabilecektir.
3. **Sağlık Hizmetleri Kataloğu:** Kurumun sunduğu sağlık hizmetleri (örneğin: **genel muayene, radyoloji, laboratuvar**), her bir hizmetin süresi (dakika) ve ücreti (para birimi ve tutar) detaylı şekilde tanımlanabilecektir (`ServiceDefinition`, `BranchService`).

### B. Çalışan Yönetimi
1. **Personel Kaydı:** Doktor ve sağlık personeli sisteme kayıt edilebilecektir (`StaffProfile`, `StaffAssignment`).
2. **Uzmanlık ve Hizmet Yetkinliği:** Her doktorun uzmanlık alanı ve sunabileceği hizmetler tanımlanabilecektir (`StaffServiceEligibility`).
3. **Müsaitlik / Uygunluk Zaman Dilimleri:** Doktorların uygunluk zaman dilimleri (haftalık şablon ve istisnalar) sisteme tanımlanacak; **hastalar sadece bu zaman aralıklarına randevu alabilecektir** (`WeeklyAvailability`, `AvailabilityException`).

### C. Randevu Sistemi
1. **İşlem ve Çalışan Seçimi:** Kullanıcılar uygun çalışan ve işlem seçerek randevu oluşturabilecektir.
2. **Çakışma Kontrolü (Collision Prevention):** Randevu oluşturulurken aynı doktora aynı zaman diliminde çakışan ikinci bir rezervasyon yapılması kesin olarak engellenecektir (pesimist satır kilidi ve PostgreSQL izolasyonu).
3. **Randevu Detay Kaydı (Snapshot):** Randevu detayları (**işlem adı, süre, ücret, çalışan, tarih/saat**) işlem anındaki değerleriyle dondurularak (snapshot) kaydedilecektir (`Appointment`).
4. **Onay Mekanizması:** Randevular için onay mekanizması bulunacaktır (`PENDING` -> `CONFIRMED` / `REJECTED` / `CANCELLED` durum makinesi).

### D. Kullanıcı Rolleri
Sistemde en az üç rol yer alacaktır:
- **Hasta (Patient):** Uygun doktor ve hizmet seçerek randevu alır, onay durumunu izler, randevusunu iptal eder.
- **Doktor / Sağlık Personeli (Doctor / Staff):** Kendi çalışma takvimini görüntüler, kendisine atanan randevuları onaylar veya reddeder.
- **Hastane / Klinik Yöneticisi (Admin / Manager):** Kurum, şube, bölüm, hizmet kataloğu, personel mesai saatleri ve tüm randevu operasyonlarını yönetir.

---

## 3. Akademik Teslimat ve Geçme Koşulları

1. **GitHub Üzerinde Minimum 10 Farklı Gün Commit Şartı:**
   - Depo üzerinde **en az 10 adet commit, birbirinden tamamen farklı takvim günlerinde** yapılmış olmalıdır.
   - Aynı gün atılan birden fazla commit tek 1 gün sayılır.
   - Commit tarihlerini tahrif etmek (backdating) veya boş commit atmak kesinlikle yasaktır; gerçek geliştirme günleri belgelenmelidir.
2. **SABİS Teslimatı:**
   - Proje kaynak kodları ve proje raporu son yükleme tarihinden önce Sakarya Üniversitesi SABİS sistemine eksiksiz yüklenmelidir.
3. **Canlı Sunum Zorunluluğu:**
   - Proje sunumları 7. haftadan itibaren (son 4 hafta) başlayacaktır.
   - **Kritik Kural:** Proje dosyalarını yüklemiş olsalar dahi sunuma gelmeyen/katılmayan öğrenciler projeden puan alamazlar.
   - Sunum sırasında öğretim elemanına canlı demo yapılacak ve nesneye yönelik tasarım (OOP) soruları cevaplanacaktır.

---

## 4. Ölçülebilir Çıkış Hedefleri (Definition of Success)

1. Tüm MUST gereksinimlerinin (FR-001..FR-028, NFR-001..NFR-007) testlerle veya demo ile kanıtlanması.
2. Eşzamanlı iki istekte aynı doktora aynı saatte çakışan randevu verilmesinin imkânsız olduğunun Testcontainers PostgreSQL testi ile ispatlanması.
3. Üç rolün ekran ve yetki akışlarının canlı demoda başarıyla sergilenmesi.
4. Çevrimdışı (offline) çalışabilirlik: Sunum sırasında internet kesilse dahi yerel Docker PostgreSQL ve statik kaynaklar üzerinden sistemin kesintisiz çalışması.
5. GitHub üzerinde en az 10 farklı günde yapılmış gerçek geliştirme commit'lerinin mevcut olması.
