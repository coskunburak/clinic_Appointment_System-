# Sprint S10 — 5 Dakikalık Canlı Gösterim (Demo) ve Çevrimdışı Hazırlık Runbook'u

> **Kaynak Referans:** `docs/08-teslim/02-sunum-ve-demo.md` ve `README.md`
> **Kapsam Notu:** Bu doküman sunum günü jüri önünde yürütülecek canlı gösterimi ve internetsiz ortam hazırlığını tanımlar. Gerçek hasta verisi içermez; tüm hesaplar `dev` profili altında `DemoDataSeeder` tarafından üretilen kurmaca kayıtlardır.

---

## 1. Önkoşullar ve Başlatma Prosedürü

1. **Docker & PostgreSQL 17:** Docker Desktop çalışır durumda olmalı, `postgres:17-alpine` imajı ve `.m2` Maven bağımlılıkları yerel makinede hazır bulunmalıdır.
2. **Ortam ve Veritabanı Başlatma:**
   ```bash
   cp .env.example .env
   docker compose up -d
   docker compose ps
   ```
3. **Uygulamayı `dev` Profiliyle Başlatma:**
   Güvenlik gereği demo veri tohumlayıcısı varsayılan profilde kapalıdır. Sunum için `dev` profili açıkça verilmelidir:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   > Çevrimdışı ortamda yerel Maven önbelleği hazırsa `./mvnw -o spring-boot:run -Dspring-boot.run.profiles=dev` kullanılabilir.
4. **Sağlık Denetimi:** Tarayıcıdan `http://localhost:8080/login` veya uçbirimden `curl http://localhost:8080/actuator/health` kontrol edilir.

---

## 2. Kurmaca Demo Hesapları (`dev` Profili)

| Rol | E-posta | Varsayılan Parola | Ortam Değişkeni | Kapsam / Senaryo Rolü |
|---|---|---|---|---|
| Yönetici (`ROLE_ADMIN`) | `admin@clinic.test` | `Admin123!` | `DEMO_ADMIN_PASSWORD` | `Örnek Sağlık Grubu` (Merkez & Sahil şubeleri) |
| Hekim (`ROLE_STAFF`) | `doctor.ahmet@clinic.test` | `Doctor123!` | `DEMO_DOCTOR_PASSWORD` | Dr. Ahmet Kaya — Kardiyoloji (`Merkez Hastanesi`) |
| Hekim (`ROLE_STAFF`) | `doctor.ayse@clinic.test` | `Doctor123!` | `DEMO_DOCTOR_PASSWORD` | Dr. Ayşe Demir — Dahiliye (`Sahil Polikliniği`) |
| Hasta (`ROLE_PATIENT`) | `patient.ali@clinic.test` | `Patient123!` | `DEMO_PATIENT_PASSWORD` | Ali Yılmaz (Hasta randevu alma & görüntüleme) |
| Hasta (`ROLE_PATIENT`) | `patient.zeynep@clinic.test` | `Patient123!` | `DEMO_PATIENT_PASSWORD` | Zeynep Kaya (İkinci hasta / çakışma testi) |

---

## 3. 5 Dakikalık Zamanlı Gösterim Akışı (00:00 – 05:00)

| Zaman Aralığı | Adım | Ekran / Eylem | Gösterilecek Temel Kanıt |
|---|---|---|---|
| **00:00 – 00:45** | **1. Açılış & Güvenlik Kapısı** | `http://localhost:8080/login` açılır; oturumsuz `/admin` veya `/patient` erişiminin `/login` sayfasına yönlendirildiği gösterilir. | Spring Security 6 form girişi, `BCrypt-12`, CSRF koruması ve `DemoEnvironmentGuard` yalıtımı. |
| **00:45 – 01:45** | **2. Yönetici Görünümü (`/admin`)** | `admin@clinic.test` ile giriş yapılır. `Örnek Sağlık Grubu` altında **Merkez Hastanesi (Kadıköy)** ve **Sahil Polikliniği (Beşiktaş)** şubeleri, çalışma saatleri (`Europe/Istanbul`), 4 hizmet tanımı ve personel yetkinlikleri gösterilir. | Çok şubeli yapı, şubeye özel dakika/ücret (`Money`), `Organization` &rarr; `Branch` &rarr; `BranchService`. |
| **01:45 – 02:45** | **3. Hekim Takvimi (`/staff`)** | `doctor.ahmet@clinic.test` ile giriş yapılır. Hazır tohumlanmış **5 farklı randevu statüsü** (`PENDING`, `CONFIRMED`, `REJECTED`, `CANCELLED`, `COMPLETED`) hekim takviminde gösterilir. | Rol tabanlı yetkilendirme ve şube/doktor kapsam sınırı (`AccessPolicy`). |
| **02:45 – 03:45** | **4. Hasta Rezervasyonu & Çakışma Reddi (`/patient`)** | `patient.ali@clinic.test` ile giriş yapılır. Kardiyoloji / uygun hizmet ve Dr. Ahmet için boş bir slot seçilerek yeni `PENDING` randevu oluşturulur. Ardından dolu veya izin/kapanış dışı saate rezervasyon engeli vurgulanır. | `BookingService` sıralı `PESSIMISTIC_WRITE`, `[start, end)` matematiği ve `ServiceSnapshot`. |
| **03:45 – 04:30** | **5. Onay Döngüsü & Denetim İz** | Tekrar `doctor.ahmet@clinic.test` oturumuna geçilir; yeni oluşan `PENDING` randevu onaylanır (`CONFIRMED`) veya gerekçeli reddedilir (`REJECTED`). Hasta oturumunda güncel statü ve saat/ücret anlık görüntüsü doğrulanır. | `Appointment` kapsüllenmiş durum geçişi ve `appointment_status_history` (append-only) denetim izi. |
| **04:30 – 05:00** | **6. Mimari & OOP Özeti** | Tarayıcıdan ayrılıp slayt veya IDE üzerinde `Appointment.java`, `SchedulingConstraint.java` ve `BookingService.java` yapıları kısaca özetlenir. | Anemic olmayan OOP domain çekirdeği, Strategy polimorfizmi ve Testcontainers güvencesi. |

---

## 4. Deterministik Demo Sıfırlama (Reset) Prosedürü

Sunum provası sonrasında veya jüri öncesi veriyi ilk tertemiz hâline döndürmek için HTTP/UI ucu bulunmaz; yalnızca explicit `dev` CLI modu çalıştırılır:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments="--spring.main.web-application-type=none --clinic.demo.reset.enabled=true --clinic.demo.reset.reseed=true"
```

Bu komut `DemoDataResetService` aracılığıyla yalnızca kurmaca demo kayıtlarını yabancı anahtar sırasına uygun biçimde temizler (`DemoEnvironmentGuard` kontrolünden geçerek) ve `DemoDataCatalog` üzerinden tüm tohum verisini yeniden yükler.

---

## 5. Çevrimdışı (Offline) Gösterim Hazırlığı ve Sınırları

### Çalışma Zamanında (Runtime) Tam Çevrimdışı Doğrulama
- Tüm MVC şablonlarında (`src/main/resources/templates/`) ve özel CSS/JS dosyalarında (`clinic.css`, `clinic.js`) **hiçbir dış CDN, Google Fonts veya harici JavaScript çağrısı yoktur**.
- Bootstrap 5.3.3 CSS ve JS paketleri doğrudan `src/main/resources/static/vendor/` altında yerel olarak sunulmaktadır.
- E-posta/SMS geçidi, harici OAuth/OIDC sunucusu veya bulut veritabanı bağlantısı yoktur; yerel Docker PostgreSQL 17 yeterlidir.

### İlk Kurulum ve Önkoşul Ayrımı (Açık İfade)
- **Çalışma zamanı internetsizdir**; ancak sıfır Maven önbelleği (`~/.m2/repository` boş) ve Docker imajları bulunmayan tamamen yeni bir makinede ilk `verify` / konteyner çekimi için başlangıçta internet erişimi veya önceden önbelleklenmiş ortam gerekir.
- Sunum gününden önce salonda internet olmayabileceği varsayılarak dizüstü bilgisayarda `./mvnw dependency:go-offline` + `postgres:17-alpine` imajının önceden indirilmiş olması zorunludur.

---

## 6. Acil Sorun Giderme (Troubleshooting)

| Belirti | Olası Neden | Hızlı Çözüm |
|---|---|---|
| Uygulama başlarken PostgreSQL bağlantı hatası | Docker daemon veya `compose.yaml` konteyneri kapalı | `docker compose up -d` çalıştırın, `docker compose ps` ile `healthy` durumunu bekleyin. |
| Demo kullanıcılar yok / giriş başarısız | Uygulama `dev` profili olmadan başlatılmış | Durdurup `-Dspring-boot.run.profiles=dev` parametresiyle yeniden başlatın. |
| Slotta eski prova randevuları birikti | Önceki prova oturumu kalıntıları | Yukarıdaki Bölüm 4'teki deterministik CLI `reset` komutunu çalıştırın. |
| Port `8080` kullanımda hatası | Önceki Spring Boot süreci açık kalmış | `lsof -i :8080` ile eski süreci sonlandırın veya `-Dspring-boot.run.arguments=--server.port=8081` kullanın. |
