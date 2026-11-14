# Sprint S10 — Sözlü Savunma ve OOP / SOLID Jüri Hazırlık Rehberi (30 Soru & Cevap)

> **Kaynak Dayanağı:** Bu rehberdeki tüm sınıf, metot, tablo ve metrik ifadeleri doğrudan mevcut kaynak kod, `V1`–`V4` Flyway migrasyonları ve tarihsel S09 (`8b70de2`) test çıktılarından doğrulanmıştır.

---

## A. Teknoloji Seçimi ve Sistem Mimarisi (Soru 1–2)

### 1. Neden Java 21 ve Spring Boot 3.4.2 tercih edildi?
`pom.xml` üzerinde `java.version=21` (LTS) ve `spring-boot-starter-parent:3.4.2` kullanılmıştır. Java 21 immutable `record` yapıları (`BookingCommand`, DTO'lar), modern `java.time` (`Instant`, `ZonedDateTime`, `Clock`) API'si ve tip güvenliği sunar. Spring Boot 3.4.2 ise Spring MVC, Spring Security 6, Spring Data JPA (Hibernate 6) ve Flyway entegrasyonunu tek kararlı çatı altında toplar.

### 2. Neden mikroservisler yerine Modüler Monolit seçildi?
Bir randevu rezervasyonu şube açılış saati, hekim yetkinliği, mesai istisnası ve çakışma kontrolünü **aynı ACID veritabanı işlemi** (`@Transactional`) içinde kilit altında doğrulamak zorundadır. Mikroservis yaklaşımı dağıtık işlem (Saga/2PC) ve ağ gecikmesi maliyeti eklerdi. Modüler Monolit (`com.example.clinic.{organization, identity, catalog, staff, scheduling, booking, common, web}`) alan sınırlarını `DomainArchitectureTest` ile korurken dağıtım basitliğini korur.

---

## B. OOP İlkeleri, SOLID ve Tasarım Desenleri (Soru 3–10)

### 3. Domain varlıklarında kapsülleme (Encapsulation) nasıl sağlandı?
`booking.domain.Appointment` sınıfı anemic getter/setter modelini reddeder: `public void setStatus(...)` metodu bilinçli olarak **yoktur** (ve `AppointmentArchitectureTest.appointmentMustHaveNoPublicStatusSetter` testi bunun eklenmesini engeller). Durum yalnızca iş değişmezlerini koruyan davranışsal metotlarla değişir: `approve(...)`, `reject(...)`, `cancel(...)`, `complete(...)`. Parola/kimlik tarafında ise `UserAccount.toString()` asla `passwordHash` içermez; `DemoDataProperties.toString()` parolaları `[REDACTED]` olarak maskeler.

### 4. `PersonProfile` kalıtımı (Inheritance) nerede kullanıldı, bileşim (Composition) neden/nerede tercih edildi?
`identity.domain.PersonProfile`, `common.domain.BaseEntity`'den türeyen soyut bir `@MappedSuperclass` sınıfıdır; hasta (`PatientProfile`) ve personel (`StaffProfile`) için ortak olan `fullName` (ad doğrulama) ve `phone` (E.164 biçim doğrulama) sözleşmesini tek merkezde tutar. Buna karşılık şube atamaları (`StaffAssignment`), uzmanlık hizmet yetkileri (`StaffServiceEligibility`) ve haftalık mesailer (`WeeklyAvailability`) alt sınıf patlamasını (ör. `CardiologyBranchStaff`) önlemek için kalıtım yerine **bileşim (composition)** ile modellenmiştir.

### 5. Polimorfizm (Polymorphism) `SchedulingConstraint` içinde `instanceof` olmadan nasıl çalışıyor?
`scheduling.domain.SchedulingConstraint` tek metotlu (`void validate(BookingValidationContext context)`) bir arayüzdür. Bunu uygulayan dört strateji sınıfı vardır: `OpeningHoursConstraint`, `StaffAvailabilityConstraint`, `StaffServiceEligibilityConstraint` ve `NoOverlapConstraint`. `SchedulingValidator` kurucuda `List<SchedulingConstraint>` alır (`List.copyOf` ile savunmacı kopya) ve `validate(context)` içinde her kısıtı ortak arayüz üzerinden çağırır; hiçbir `if/else` veya `instanceof` dalı içermez.

### 6. `AppointmentSchedulePort` ve `Clock` soyutlamaları (Abstraction) nasıl kullanıldı?
`scheduling` modülü doğrudan JPA veya veritabanına bağımlı olmamak için `AppointmentSchedulePort` arayüzünü tanımlar; `booking.infrastructure.adapter.DatabaseAppointmentSchedulePort` bu portu `AppointmentJpaRepository` ile gerçekler (Port/Adapter). Tüm tarih-saat kontrolleri de sistem saatine sabitlenmek (`Instant.now()`) yerine enjekte edilen `java.time.Clock` (`ClockConfig`) üzerinden yapılır; böylece birim ve entegrasyon testlerinde saat deterministik biçimde ileri/geri kaydırılabilir.

### 7. SOLID prensipleri projede hangi somut sınıflarda görülür?
- **SRP (Tek Sorumluluk):** Slot hesaplama `AvailabilityCalculator`, kilitli rezervasyon `BookingService`, onay geçişleri `AppointmentApprovalService`, web sunumu `*Controller` sınıflarındadır.
- **OCP (Açık/Kapalı):** Yeni bir randevu kısıtı gerektiğinde `SchedulingValidator` değiştirilmez; yalnızca `SchedulingConstraint` uygulayan yeni sınıf eklenir.
- **LSP (Liskov İkamesi):** `PatientProfile` ve `StaffProfile`, `PersonProfile` üst sınıfının ad/telefon değişmezlerini bozmadan ikame edilebilir.
- **ISP (Arayüz Ayrımı):** Tek şişkin servis/depo arayüzü yerine `AppointmentSchedulePort` ve modüle özel dar JPA repository arayüzleri tanımlanmıştır.
- **DIP (Bağımlılık Tersine Çevirme):** Üst modüller somut altyapıya değil arayüz portlarına (`AppointmentSchedulePort`, Spring Data arayüzleri) bağımlıdır.

### 8. Spring Data JPA repository arayüzleri neden arayüz olarak tanımlanır ve katmanlı mimariye nasıl oturur?
`JpaRepository<T, ID>` türevleri yalnızca veri erişim sözleşmesini bildirir; somut vekil (proxy) sınıflar çalışma zamanında Spring Data JPA tarafından üretilir. Üstelik `WebArchitectureTest` hiçbir MVC denetleyicisinin (`@Controller`) doğrudan `*JpaRepository` enjekte etmesine izin vermez; denetleyiciler yalnızca uygulama servisleriyle konuşur.

### 9. `Money` ve `TimeInterval` değişmez değer nesneleri (Immutable Value Objects) neden `final` tasarlandı?
`Money` (`BigDecimal amount`, `Currency currency`) ve `TimeInterval` (`Instant startAt`, `Instant endAt`) `public final class` ve tüm alanları `private final` şeklindedir. `final` bildirimi alt sınıflama yoluyla değişmezlerin (immutability) bozulmasını engeller. `Money` negatif tutarı ve 2'den fazla ondalık basamağı reddeder, farklı para birimi işlemlerinde `CurrencyMismatchException` fırlatır. `TimeInterval` ise sıfır veya ters aralıkları kurucuda reddeder.

### 10. Parasal tutarlar için neden `double`/`float` değil `BigDecimal` kullanıldı?
IEEE-754 kayan noktalı sayılar (`double`/`float`) `0.1 + 0.2` gibi ondalık kesirleri ikili sistemde tam temsil edemediğinden finansal yuvarlama hataları üretir. `Money` sınıfında `BigDecimal` (veritabanında `NUMERIC(12, 2)`), `scale > 2` denetimi ve `RoundingMode.HALF_UP` çarpım kuralı ile kuruş hassasiyeti garanti altına alınmıştır.

---

## C. Veritabanı, Flyway ve Doğruluk Garantileri (Soru 11–19)

### 11. Neden H2 belleği yerine gerçek PostgreSQL 17 kullanıldı?
H2 gibi bellek içi veritabanları `SELECT ... FOR UPDATE` kilit semantiğini, PostgreSQL `TIMESTAMPTZ` davranışını, `CHECK` kısıtlarını ve kompozit yabancı anahtarların birebir davranışını tam yansıtmaz. Gerçek PostgreSQL 17 hem geliştirme ortamında (`compose.yaml`) hem de entegrasyon testlerinde (`Testcontainers PostgreSQLContainer`) birebir üretim motoru doğruluğu sağlar.

### 12. Flyway `V1`–`V4` migrasyonları ve `ddl-auto: validate` neden önemlidir?
Şema oluşturma işi Hibernate'e (`ddl-auto: update/create`) bırakılmaz; tüm DDL değişiklikleri `src/main/resources/db/migration/` altındaki 4 deterministik Flyway betiğiyle (`V1` kurum/kimlik, `V2` katalog, `V3` personel/mesai, `V4` randevu/tarihçe — toplam 16 tablo) uygulanır. `application.yml`'de `spring.jpa.hibernate.ddl-auto: validate` açıktır; JPA entity'leri ile SQL şeması uyuşmazsa uygulama hiç başlamaz.

### 13. Yarı açık `[start, end)` zaman aralığı nasıl çalışır ve bitişik slotlarda neden çakışma vermez?
Her randevu başlangıç anını içerip bitiş anını dışlar (`[startAtUtc, endAtUtc)`). İki aralığın çakışma koşulu `s1 < e2 && s2 < e1` şeklindedir (`TimeInterval.overlapsWith` ve `AppointmentJpaRepository` JPQL sorguları). Örn. `[09:00, 09:30)` ile `[09:30, 10:00)` karşılaştırıldığında `09:30 < 09:30` yanlış çıktığı için bitişik randevulara izin verilir.

### 14. Çift rezervasyon (Double-Booking) yarış durumu tam olarak nasıl engellenir?
Yalnızca bellek kontrolü veya tek başına basit kontrol eşzamanlı thread'lerde yarış durumuna açıktır. `BookingService.bookAppointment` metodu `@Transactional(isolation = Isolation.READ_COMMITTED)` içinde ilgili hasta ve hekim satırlarını PostgreSQL'de `SELECT ... FOR UPDATE` (`PESSIMISTIC_WRITE`) ile kilitler; ikinci thread birinci işlem `commit` veya `rollback` yapana kadar bekler, ardından güncel `PENDING/CONFIRMED/COMPLETED` kayıtları görüp `AppointmentConflictException` ile reddedilir.

### 15. Pesimist kilitlemede kilitlenme (Deadlock) nasıl önlenir?
İki eşzamanlı işlem aynı iki kaynağı ters sırada kilitlerse deadlock oluşabilir. Sistemde `BookingService` daima küresel sabit kilit sırası uygular: **1) `patientProfileRepository.findByIdForUpdate(patientId)` &rarr; 2) `staffProfileRepository.findByIdForUpdate(command.staffId())`**. Sıra hiç değişmediği için döngüsel bekleme (circular wait) oluşmaz.

### 16. `@Transactional` sınırları hangi katmandadır ve `open-in-view: false` neden açıktır?
İşlem sınırları uygulama servislerindedir (`BookingService`, `AppointmentApprovalService`). Denetleyicilerde (controller) işlem yoktur. Ayrıca `spring.jpa.open-in-view: false` ayarlıdır; böylece Thymeleaf şablonları işlenirken gizli tembel yükleme (lazy SQL) sorgularının atılması engellenir (bölgesel N+1 ve kilit tutma süresi önlenir).

### 17. Bir hizmetin fiyatı veya süresi sonradan değişirse eski randevular neden etkilenmez?
`BookingService` randevuyu oluştururken `branchService.toSnapshot()` metoduyla o anki `serviceId`, `serviceName`, `durationMinutes` ve `Money fee` değerlerini `ServiceSnapshot` (`@Embedded` olarak `appointment` satırına) kopyalar. Sonradan katalog ücreti güncellense bile mevcut randevular sözleşme anındaki fiyat ve süreyi korur (`FR-032`).

### 18. Randevu yaşam döngüsü durum makinesi nasıl işler? Hangi durumlar slotu tekrar açar?
Yeni randevu yalnızca `PENDING` doğar. `AppointmentApprovalService` randevu satırını `findByIdForUpdate` ile kilitleyerek geçişleri yapar:
- `PENDING -> CONFIRMED` (yetkili hekim/yönetici, başlangıçtan önce) &rarr; Slot bloke (`isBlocking=true`).
- `PENDING -> REJECTED` (zorunlu gerekçe, 1–500 karakter) &rarr; Terminal, **slot serbest** (`isBlocking=false`).
- `PENDING | CONFIRMED -> CANCELLED` (hasta/hekim/yönetici, gerekçeli, başlangıçtan önce) &rarr; Terminal, **slot serbest** (`isBlocking=false`).
- `CONFIRMED -> COMPLETED` (hekim/yönetici, bitiş zamanı geldikten/geçtikten sonra) &rarr; Terminal, slot bloke.

### 19. `appointment_status_history` tablosunun eklemeli (Append-Only) yapısı nasıl garanti edilir?
`AppointmentStatusHistoryJpaEntity` varlığının tüm alanları `@Column(..., updatable = false)` ile tanımlanmıştır, setter metotları yoktur ve `AppointmentArchitectureTest.statusHistoryMustBeAppendOnly` bu değişmezliği statik testle doğrular. Her oluşturma (`null -> PENDING`) ve geçiş aynı veritabanı işlemi içinde yeni bir tarihçe satırı yazar.

---

## D. Güvenlik ve Çok Kiracılı Bütünlük (Soru 20–22)

### 20. Web formlarında CSRF ve kimlik güvenliği nasıl uygulanır?
`SecurityConfig` Spring Security 6 varsayılan CSRF korumasını aktif tutar (yalnızca `/h2` gibi harici bir muafiyet yoktur; tüm `/patient/**`, `/staff/**`, `/admin/**` `POST` formları gizli `_csrf` token gönderir). Parolalar `BCryptPasswordEncoder(12)` ile özetlenir ve başarılı oturumda session fixation koruması uygulanır.

### 21. IDOR (Insecure Direct Object Reference) saldırılarına karşı `AccessPolicy` nasıl çalışır?
Kullanıcının form veya URL parametresinde başka bir hastanın/şubenin/kurumun UUID'sini göndermesi `ROLE_*` kontrolüyle tek başına engellenemez. `AccessPolicy` servis katmanında aktif kullanıcının `Membership` kayıtlarını kontrol eder (`enforcePatientSelfAccess`, `enforceStaffBranchAccess`, `enforceAdminOrganizationAccess`, `enforceAdminBranchAccess`); sahiplik veya şube kapsamı tutmazsa `AccessDeniedException` (HTTP 403) fırlatılır.

### 22. Çok kiracılı (Multi-Tenant) veri bütünlüğü veritabanında nasıl korunur?
Uygulama kontrollerine ek olarak PostgreSQL'de kompozit yabancı anahtarlar vardır: `V1` içinde `membership(branch_id, organization_id)` başka kuruma ait şube atanmasını engeller; `V2` ve `V4` içinde ise `branch_service(id, branch_id)` tekil kısıtı ve `appointment(branch_service_id, branch_id)` kompozit FK'sı bir şubeye tanımlanmamış hizmetin o şubede randevulanmasını imkânsız kılar.

---

## E. Test, Stres, Performans, Demo ve Kapsam (Soru 23–30)

### 23. Birim testler (Surefire) ile Testcontainers entegrasyon testleri (Failsafe) arasındaki fark nedir?
`src/test` altındaki `*Test.java` sınıfları Surefire aşamasında Mockito/AssertJ/MockMvc ile hızlı koşar (S09: 46 sınıf / 366 test). `*IT.java` sınıfları ise Failsafe aşamasında `AbstractIntegrationTest` aracılığıyla gerçek PostgreSQL 17 Docker konteyneri üzerinde Flyway migrasyonlarını, SQL kilitlerini ve güvenlik zincirini uçtan uca doğrular (S09: 20 sınıf / 135 test).

### 24. 5-Thread eşzamanlılık stres testi (`ConcurrentBookingIT`) neyi kanıtlar?
Dışarıdan `@Transactional` açılmadan 5 ayrı thread `ExecutorService` ve `CountDownLatch` bariyeriyle milisaniye eşzamanlılığında tetiklenir:
- **Senaryo 1 (Aynı Slot):** 5 farklı hasta aynı hekimin aynı 30 dk slotuna istek atar &rarr; tam **1 randevu `PENDING`** olur, diğer **4 istek `AppointmentConflictException`** ile reddedilir.
- **Senaryo 2 (Farklı Slotlar):** 5 farklı hasta aynı günkü 5 farklı slota istek atar &rarr; **5/5 başarılı** olur (gereksiz kaba kilitleme/yanlış ret yoktur).

### 25. Performans kıyaslama testi (`PerformanceBenchmarkIT`) sonuçları nasıl yorumlanmalıdır?
Testcontainers PostgreSQL 17 içinde 50 hekim, 20 hasta ve 1.000 arka plan randevusu üzerinde `System.nanoTime()` ile ölçülen S09 (`8b70de2`) sonucunda `availability-slot-search` (40 istek) **p95 = 21.86 ms**, `booking-tx` (24 istek) **p95 = 14.74 ms** ve her ikisinde de **hata oranı 0.0000** çıkmıştır (`NFR-008` hedefi olan `< 1000 ms` fazlasıyla sağlanmıştır). Not: Bu değerler yerel Docker ortamına ait kontrollü mikro-kıyaslama ölçümüdür.

### 26. Demo veri tohumlaması (`DemoDataSeeder`) üretim ortamından nasıl yalıtılmıştır?
`DemoDataSeeder` sınıfı `@Profile("dev")` ve `@ConditionalOnProperty(prefix = "clinic.demo.seed", name = "enabled", havingValue = "true")` ile koşulludur; varsayılan profilde hiç bean olarak oluşmaz. Üstelik `DemoEnvironmentGuard`, yasaklı profilleri (`prod`, `production`, `staging` vb.) ve localhost dışındaki veritabanı bağlantılarını reddeder.

### 27. Deterministik demo sıfırlama (`DemoDataResetService`) neden HTTP ucu yerine CLI bayrağıyla çalışır?
Yanlışlıkla veya yetkisiz bir istekle tarayıcı üzerinden tüm demo verisinin silinmesini önlemek için Web/HTTP arayüzünde reset uç noktası açılmamıştır. Sıfırlama yalnızca `--spring.main.web-application-type=none --clinic.demo.reset.enabled=true` bayraklarıyla explicit CLI oturumunda ve `DemoEnvironmentGuard` onayıyla çalışır.

### 28. Projenin bilinen kısıtları ve ertelenen gereksinimleri nelerdir?
- **Akademik kapsam dışı alanlar:** Gerçek hasta TCKN/PII verisi, tıbbi tanı, reçete, kredi kartı/POS ödeme ve e-Nabız/MHRS entegrasyonu bulunmaz.
- **Ertelenen gereksinim (`FR-031`):** Form tekrar gönderiminde idempotency anahtarı ertelenmiştir; ancak sıralı kilit ve hasta çakışma önleme (`FR-029`) aynı slota mükerrer randevu oluşumunu engeller.
- **İsteğe bağlı (`COULD`):** `FR-035` ve `FR-036` (uygulama içi bildirim/gelişmiş rapor filtresi) kapsam dondurması nedeniyle eklenmemiştir.

### 29. Sistem gerçek üretime (Production) alınacak olsaydı ilk hangi iyileştirmeler yapılırdı?
1. `FR-031` HTTP idempotency anahtarı eklenmesi (çift tıklamada temiz geri dönüş için).
2. Randevu onay/iptal bildirimleri için **Transactional Outbox Pattern** ile asenkron e-posta/SMS gönderimi.
3. Micrometer + Prometheus + OpenTelemetry metrikleri ve yapılandırılmış merkezi loglama.
4. Hasta kendi kendine kayıt (self-registration), e-posta doğrulama ve şifre sıfırlama akışı.

### 30. GitHub 10 farklı gün commit şartı (`NFR-006 / TC-042`) neden ve nasıl doğrulandı?
Ders gereksinimi gereği projenin son güne sıkıştırılmadan düzenli geliştirildiği en az 10 farklı gerçek takvim gününde atılmış commitlerle kanıtlanmalıdır. `git log --format=%as | sort -u | wc -l` denetimi **2026-11-02** ile **2026-11-14** arasında **10 tekil takvim günü** ve **28 gerçek commit** olduğunu, boş commit veya tarih değişikliği olmadığını doğrulamıştır.
