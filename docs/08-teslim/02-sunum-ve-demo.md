# Akademik Sunum, Canlı Demo ve Savunma Kılavuzu

Bu belge, **Sakarya Üniversitesi Nesneye Dayalı Programlama (OOP)** dersi Seçenek A dönem projesinin canlı sunum ve değerlendirme protokolünü ayrıntılandırır.

> **S10 Üretilen Teslimat Bağlantıları:** Nihai 7 slaytlık sunum (`deliverables/Clinic_Appointment_System_Final_Presentation.pptx`), ayrıntılı zamanlı gösterim kılavuzu için [S10-live-demo-runbook.md](./S10-live-demo-runbook.md) ve 30 soruluk savunma rehberi için [S10-oral-defense.md](./S10-oral-defense.md) dosyalarına bakınız.

---

## 1. Resmî Akademik Sunum Kuralları

- **Sunum Tarihleri:** Dönemin **7. haftasından itibaren (son 4 hafta)** öğretim elemanının ilan edeceği takvime göre yapılacaktır.
- **Kritik Kural (Sıfır Puan Uyarısı):** Proje dosyalarını GitHub ve SABİS sistemine eksiksiz yüklemiş olsalar dahi, **canlı sunuma katılmayan öğrenciler dersten puan alamazlar**.
- **Dosya Yükleme Önkoşulu:** Proje kaynak kodlarını ve raporunu son teslim tarihinden önce sisteme yüklemeyen öğrenciler sunuma kabul edilmezler.
- **Süre Planlaması:** Sunum ve canlı demo toplam **5 – 7 dakika** olarak hedeflenmelidir (öğretim elemanının yönlendirmesine göre 3–5 dakikaya indirilebilir).

---

## 2. 7 Slaytlık Profesyonel Sunum Şablonu

| Slayt No | Başlık | İçerik ve Vurgulanacak Noktalar | Süre |
|---|---|---|---|
| **Slayt 1** | **Proje Kimliği ve Giriş** | Proje Adı: Hastane / Klinik Randevu Sistemi. Öğrenci bilgileri, ders adı (OOP Seçenek A), GitHub repo adresi. Tek ve çok şubeli klinik desteği. | 30 sn |
| **Slayt 2** | **İş İhtiyacı ve Proje Kapsamı** | Kurum/şube yönetimi, tıbbi bölümler, hizmet süre/ücret tanımları, doktor uzmanlık ve müsaitlik takvimleri, randevu onay yaşam döngüsü, 3 kullanıcı rolü. | 45 sn |
| **Slayt 3** | **Teknoloji ve Mimari Tercihler** | Java 21 LTS, Spring Boot 3.4.2, Spring MVC + Thymeleaf, PostgreSQL 17, Flyway, Testcontainers. Modüler Monolit ve Package-by-Feature mimari ayrımı. | 45 sn |
| **Slayt 4** | **Nesneye Dayalı Tasarım (OOP) Kanıtları** | Kapsülleme (`Appointment`, `Money`), Kalıtım (`PersonProfile` -> `PatientProfile`, `StaffProfile`), Çok Biçimlilik (`SchedulingConstraint` stratejisi), SOLID prensipleri. | 60 sn |
| **Slayt 5** | **Çakışma Önleme ve Eşzamanlılık (Concurrency)** | Pesimist satır kilitleme (`PESSIMISTIC_WRITE`), yarı açık zaman aralığı (`[start, end)`), PostgreSQL izolasyonu ve ücret/süre Snapshot deseni. | 60 sn |
| **Slayt 6** | **Test Stratejisi ve GitHub Commit Disiplini** | JUnit 5 birim testleri, Testcontainers gerçek PostgreSQL entegrasyon testleri, 10 farklı günde yapılmış gerçek geliştirme commit grafiği. | 45 sn |
| **Slayt 7** | **Canlı Demo ve Çıkış** | Tarayıcı üzerinden hasta, hekim ve yönetici akışlarının gösterilmesi, sınırların açıklanması. | 15 sn |

---

## 3. Canlı Demo Akış Senaryosu (Adım Adım 5 Dakika)

Canlı sunumda öğretim elemanına gösterilecek deterministik akış:

### Adım 1: Yönetici (Admin) Rolü ile Başlangıç (1 dk)
1. Uygulamayı `dev` profiliyle başlatın (`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`), tarayıcıda `http://localhost:8080/login` adresine gidin ve `admin@clinic.test` (dev varsayılanı: `Admin123!`) ile giriş yapın.
2. **Kurum ve Şube Tanımı:** `Örnek Sağlık Grubu` altında **"Merkez Hastanesi (Kadıköy)"** ve **"Sahil Polikliniği (Beşiktaş)"** detaylarını, farklı çalışma saatlerini (`08:30–18:00` + Cmt `09:00–14:00` ve `09:00–18:00`) sergileyin.
3. **Bölüm ve Hizmet Kataloğu:** "Kardiyoloji", "Radyoloji", "Dahiliye", "Laboratuvar" bölümlerini; "Genel Muayene" (30 dk, 1500 TL), "Eko Çekimi" (45 dk, 2500 TL), "Radyoloji Akciğer Grafisi" (20 dk, 1200 TL) ve "Kan Tahlili" (15 dk, 600 TL) tanımlarını gösterin.

### Adım 2: Doktor Tanımlama ve Müsaitlik Takvimi (1 dk)
1. "Dr. Ahmet Kaya" personel kaydını açın; uzmanlık alanının Kardiyoloji olduğunu ve Genel Muayene hizmetine yetkilendirildiğini (`StaffServiceEligibility`) gösterin.
2. Hekimin haftalık mesai şablonunu (`Pazartesi-Cuma 09:00 - 17:00`) ve öğle arası istisnasını sergileyin.

### Adım 3: Hasta Rolü ile Randevu Alma ve Çakışma Testi (1.5 dk)
1. Gizli sekme (Incognito) açarak Hasta hesabıyla giriş yapın.
2. Dr. Ahmet Kaya'dan önümüzdeki Pazartesi günü saat `10:00 - 10:30` aralığına Genel Muayene randevusu alın.
3. Randevunun `PENDING` (Onay Bekliyor) durumunda oluştuğunu ve o anki muayene ücretinin (1500 TL) randevuya snapshot olarak dondurulduğunu gösterin.
4. **Çakışma Önleme Gösterimi (Kritik Kanıt):** Başka bir hasta hesabı açarak aynı hekime aynı gün saat `10:15 - 10:45` aralığına randevu almaya çalışın. Sistemin slot çakışması tespit edip randevuyu engellediğini (`Bu saat aralığında hekim doludur`) gösterin.

### Adım 4: Hekim Rolü ile Onay Mekanizması (1 dk)
1. Hekim hesabı ile giriş yapın; hekim kontrol panelinde gelen `10:00 - 10:30` randevusunu görüntüleyin.
2. **"Onayla" (Approve)** butonuna basarak randevu durumunu `CONFIRMED` yapın.
3. Hasta ekranına dönerek durumun onaylandığını ve durum tarihçesine kayıt düştüğünü sergileyin.

### Adım 5: Kod Üzerinden OOP İspatı (30 sn)
1. IDE'ye geçerek `PersonProfile` kalıtımını, `SchedulingConstraint` polimorfizmini ve `DatabaseConstraintIT` entegrasyon testlerini gösterin.

---

## 4. Öğretim Elemanının Muhtemel Soruları ve Profesyonel Cevapları

### Soru 1: "Çok Biçimliliği (Polymorphism) nerede ve neden kullandınız?"
> **Cevap:** Randevu rezervasyon sürecindeki kural motorunda (`SchedulingConstraint`) kullandık. Şube mesai saati kontrolü (`OpeningHoursConstraint`), hekimin haftalık uygunluğu (`StaffAvailabilityConstraint`) ve çakışma kontrolü (`NoOverlapConstraint`) aynı `validate(context)` arayüzünü uygular. `BookingService` somut sınıfların hiçbirini bilmeden, polimorfik bir kural listesi üzerinden tek bir çağrıyla tüm kuralları işletir. Yeni bir kural (örneğin bayram tatili kuralı) eklendiğinde mevcut rezervasyon koduna dokunmadan sadece yeni bir kural sınıfı eklememiz yeterlidir (Open/Closed prensibi).

### Soru 2: "İki hasta aynı milisaniyede aynı hekime tıklarsa çift rezervasyonu nasıl engelliyorsunuz?"
> **Cevap:** Spring Transaction seviyesinde ilgili personelin rezervasyon satırına pesimist satır kilidi (`PESSIMISTIC_WRITE`) alıyoruz. İlk gelen istek kilit alıp aralık çakışması kontrolü (`[start, end)`) yaparken, ikinci eşzamanlı istek kilit bekler. İlk istek randevuyu `PENDING` durumunda kaydettiği an aralık rezerve edilmiş olur; ikinci istek kilit kalkıp aralığı kontrol ettiğinde çakışmayı görerek kontrollü bir hata ile reddedilir. Bunu H2 ile değil, gerçek PostgreSQL Testcontainers üzerinde çok iş parçacıklı (multithreaded) entegrasyon testiyle ispatladık.

### Soru 3: "Hekim muayene ücretini 6 ay sonra artırırsa eski randevu kayıtları ne olur?"
> **Cevap:** Snapshot tasarım desenini kullandık. Randevu oluşturulduğu anda o günkü hizmet adı (`serviceNameSnapshot`), işlem süresi (`durationMinutesSnapshot`) ve muayene ücreti (`feeAmountSnapshot`, `currencySnapshot`) doğrudan `Appointment` tablosuna kopyalanır. Hekim veya hastane yönetimi katalogdaki fiyatı değiştirse bile geçmiş randevuların finansal doğruluğu ve makbuz değerleri asla bozulmaz.

### Soru 4: "Tek klinik ve çok şubeli klinik gereksinimini mimaride nasıl çözdünüz?"
> **Cevap:** Çok kiracılı (multi-tenant) hiyerarşik organizasyon modeli kurduk. `Organization` (Kurum) ve ona bağlı `Branch` (Şube) yapıları mevcuttur. Tek klinik durumunda 1 Kurum ve 1 Şube tanımlanır. Çok şubeli sağlık kuruluşlarında ise aynı kuruma bağlı onlarca şube tanımlanabilir. Doktorlar birden fazla şubede görev alabilir (`StaffAssignment`), ancak randevu çakışma kontrolü doktor kimliği (`staffId`) üzerinden global olarak yapılır.

### Soru 5: "Kalıtımı (Inheritance) nerede kullandınız?"
> **Cevap:** `PersonProfile` soyut temel sınıfımızda kullandık. `PatientProfile` ve `StaffProfile` sınıfları ortak ad, soyad, telefon ve kullanıcı hesabı eşleşmesini bu sınıftan miras alır. Böylece kod tekrarı önlenmiş ve nesne tabanlı ortak kimlik sözleşmesi kurulmuştur.

---

## 5. Çevrimdışı (Offline) Hazırlık ve Hata Önleme Kontrol Listesi

Sunum günü internet bağlantısı kesilebilir veya yavaşlayabilir. Aşağıdaki önlemler eksiksiz alınmalıdır:

- [ ] **Docker PostgreSQL Hazırlığı:** `docker compose up -d` komutu sunumdan önce çalıştırılmalı ve container'ın sağlıklı (`healthy`) olduğu `docker compose ps` ile teyit edilmelidir.
- [ ] **İnternetsiz Çalışma (Offline Test):** Tarayıcıda CDN kaynaklı CSS/JS kırılmalarını önlemek için statik varlıklar (Bootstrap vb.) yerel `src/main/resources/static/` dizininde hazır bulunmalıdır.
- [ ] **Önceden Hazır Demo Hesapları (`dev` profili):**
  - Admin: `admin@clinic.test`
  - Doktorlar: `doctor.ahmet@clinic.test` (Kardiyoloji — Merkez), `doctor.ayse@clinic.test` (Dahiliye — Sahil)
  - Hastalar: `patient.ali@clinic.test`, `patient.zeynep@clinic.test`
  *(Parolalar `.env` veya `application-dev.yml` üzerinden `DEMO_ADMIN_PASSWORD` / `DEMO_DOCTOR_PASSWORD` / `DEMO_PATIENT_PASSWORD` değişkenleriyle tanımlanır.)*
- [ ] **Test Kanıtlarının Hazır Bulundurulması:** Terminalde `./mvnw verify` çıktısı ve Testcontainers test raporları bir sekmede açık tutulmalıdır.
- [ ] **GitHub 10 Gün Commit Geçmişi:** `git log --format='%h %as %s' --reverse` çıktısı hazırda bekletilmelidir.
