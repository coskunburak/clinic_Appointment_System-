# Sprint S05 Uygulama Raporu: Personel Yönetimi, Yetkinlik Eşleştirmesi ve Müsaitlik Takvimi

- **Rapor Tarihi:** 2026-11-08
- **Sprint Kodu:** S05
- **Sprint Başlığı:** Staff Profile, Medical Specialty, Branch & Department Assignment, Service Eligibility, Weekly Working Schedule, Leave/Overtime Exceptions, and Availability Slot Engine
- **Sorumlu:** Principal Java Architect, Domain-Driven Design Engineer, PostgreSQL Schema Specialist, Application Security Specialist, Senior Test Automation Engineer, Academic OOP Course Reviewer
- **Sprint Statüsü:** `PASS_OWNER_ACCEPTED`

---

## 1. Başlangıç Durumu ve Denetim (Baseline Audit)

- **Depo (Repository):** `https://github.com/coskunburak/clinic_Appointment_System-`
- **Etkin Dal (Branch):** `main`
- **Başlangıç HEAD SHA:** `328b297` (`feat(s04): implement authentication, spring security, password hashing and multi-tenant authorization`)
- **Çalışma Ağacı (Working Tree):** S05 başlangıcında temiz (`clean`), S01–S04 geliştirmeleri eksiksiz devralındı.
- **S01–S04 Temel Uyumluluğu:**
  - V1 Flyway migrasyonu (`V1__create_organization_and_identity.sql`) ve V2 migrasyonu (`V2__create_catalog_tables.sql`) şemaları dokunulmadan korundu.
  - S02 saf domain çekirdeği (`Money`, `TimeInterval`, `ServiceSnapshot`, `Appointment`, `SchedulingConstraint` motoru) ve `DomainArchitectureTest` mimari bağımsızlık kuralı korundu.
  - S03 kurum, şube, haftalık açılış saatleri, tıbbi bölümler ve katalog servisleri korundu.
  - S04 kimlik, Spring Security 6.4, parola hashleme (BCrypt 12), oturum güvenliği ve `AccessPolicy` servis seviyesi IDOR savunması devralındı.
- **Java ve Çerçeve:** Java 21 LTS, Spring Boot 3.4.2, Spring Security 6.4, PostgreSQL 17 (Testcontainers), Flyway, JUnit 5, Mockito, AssertJ.

---

## 2. Kapsam ve Karşılanan Gereksinimler

Seçenek A mimarisinin ve ürün matrisinin S05 çalışan yönetimi gereksinimleri eksiksiz karşılandı:
- **FR-007 / TC-007:** Hekim ve personelin sisteme kaydı (`StaffProfile`), çalışan tipi (`DOCTOR`, `NURSE`, `TECHNICIAN`) ayrımı ve şube/bölüm görevlendirmesi (`StaffAssignment`) sağlandı.
- **FR-008 / TC-008:** `DOCTOR` çalışan tipi için tıbbi uzmanlık alanı (`specialty`) zorunlu kılındı; boş veya null uzmanlıkla doktor kaydı/güncellemesi `IllegalArgumentException` ile engellendi (STA-01 kuralı). Diğer personel tipleri için uzmanlık opsiyonel bırakıldı.
- **FR-009 / TC-009:** Hekimlerin sunabileceği hizmetler şube bazında `StaffServiceEligibility` ile yetkilendirildi. Hekimin yetkili olmadığı veya farklı şubeye ait bir hizmete rezervasyon açması kesin olarak engellendi (STA-02 kuralı).
- **FR-010 / TC-010:** Personelin şube bazlı haftalık rutin çalışma takvimi (`WeeklyAvailability`) 1..7 (Pazartesi..Pazar) hafta günleri ve `start_time < end_time` zaman aralıklarıyla modellendi; çakışan pencereler engellendi (SCH-02 kuralı).
- **FR-011 / TC-011:** Belirli takvim günlerinde tam gün izin (`FULL_DAY_LEAVE`), kısmi saatlik izin (`PARTIAL_LEAVE`) ve fazla mesai / nöbet (`OVERTIME`) istisnaları (`AvailabilityException`) sisteme dahil edildi. Tam gün izinli olunan günlerde tüm slotlar kapatıldı; kısmi izin saatleri mesai aralığından eksiltildi (SCH-03 kuralı).
- **WP-S05-02 Slot Hesaplama Motoru (`AvailabilityCalculator`):** Verilen hekim, şube hizmeti ve takvim tarihi için:
  1. Şube açılış saatlerini (`BranchOpeningHours`),
  2. Hekimin haftalık mesaisini (`WeeklyAvailability`),
  3. Tarih özelindeki izin/mesai istisnalarını (`AvailabilityException`),
  4. Hizmet süresi (`durationMinutes`) adımlı potansiyel slot üretimini,
  5. Hekimin mevcut randevu çakışmalarını (`AppointmentSchedulePort` üzerinden `[start, end)` aralıkları)
  saf bir aralık cebiriyle (interval algebra) değerlendirip boş slot listesini (`List<TimeSlot>`) deterministik olarak hesaplayan saf motor tamamlandı.

---

## 3. Uygulanan Dosyalar Envanteri

### A. Veritabanı ve Şema Sürümleme (Flyway)
- `src/main/resources/db/migration/V3__create_staff_and_availability.sql`:
  - `staff_assignment` tablosu (`id`, `staff_id`, `branch_id`, `department_id`, `active`, `created_at`). `uq_staff_branch_department` tekilliği ve cascade foreign key'ler.
  - `staff_service_eligibility` tablosu (`id`, `assignment_id`, `branch_service_id`, `created_at`). `uq_assignment_service` tekilliği.
  - `weekly_availability` tablosu (`id`, `assignment_id`, `weekday`, `start_time`, `end_time`, `created_at`). `chk_weekly_weekday_range` (1..7) ve `chk_weekly_time_order` (`start_time < end_time`).
  - `availability_exception` tablosu (`id`, `assignment_id`, `local_date`, `kind`, `start_time`, `end_time`, `reason`, `created_at`). `chk_exception_kind_valid` ve `chk_exception_kind_times`.
  - Performans ve foreign key aramaları için optimize edilmiş 7 adet B-tree indeks.

### B. Kimlik ve Personel Domain Modeli (`identity.domain` & `staff.domain`)
- `com.example.clinic.identity.domain.StaffType`: Tip-güvenli `DOCTOR`, `NURSE`, `TECHNICIAN` enum modeli. `isDoctor()`, `isSpecialtyRequired()` ve `Locale.ROOT` korumalı güvenli ayrıştırma.
- `com.example.clinic.identity.domain.StaffProfile`: Güncellendi; `DOCTOR` tipi için `specialty` zorunlu kılındı (STA-01 / TC-008).
- `com.example.clinic.staff.domain.AvailabilityExceptionKind`: `FULL_DAY_LEAVE`, `PARTIAL_LEAVE`, `OVERTIME` tip-güvenli enum modeli.
- `com.example.clinic.staff.domain.StaffAssignment`: `BaseEntity` mapped superclass'ından türetilen görevlendirme varlığı. Şube ve bölüm tutarlılığını (`department.branch == branch`) aggregate seviyesinde denetler.
- `com.example.clinic.staff.domain.StaffServiceEligibility`: `BaseEntity`'den türetilen yetkinlik varlığı. Hizmetin şubesi ile atamanın şubesinin birebir eşleşmesini (STA-02) denetler.
- `com.example.clinic.staff.domain.WeeklyAvailability`: `BaseEntity`'den türetilen haftalık mesai varlığı. `weekday` (1..7) ve `startTime < endTime` kurallarını kapsüller.
- `com.example.clinic.staff.domain.AvailabilityException`: `BaseEntity`'den türetilen takvim istisnası varlığı. Tam gün izinlerde saatlerin boş kalmasını, kısmi izin ve mesailerde ise saatlerin geçerli sıralanmasını zorunlu kılar.

### C. Saf Çizelgeleme Domain Modeli (`scheduling.domain`)
- `com.example.clinic.scheduling.domain.TimeSlot`: Değişmez (immutable) `(LocalTime startTime, LocalTime endTime)` saf record nesnesi. `DomainArchitectureTest` saflık kurallarına uygun olarak sıfır Spring/JPA/Hibernate bağımlılığı ile üretildi. `overlaps()`, `fitsInside()` ve `Comparable<TimeSlot>` yetenekleriyle donatıldı.

### D. Veri Tabanı ve Altyapı Portları (`staff.infrastructure`)
- `com.example.clinic.staff.infrastructure.StaffAssignmentRepository`: Şube, hekim ve departman bazlı atama sorguları ve varlık denetimleri.
- `com.example.clinic.staff.infrastructure.StaffServiceEligibilityRepository`: Atama ve şube hizmeti bazlı yetkinlik sorguları ve atomik temizleme.
- `com.example.clinic.staff.infrastructure.WeeklyAvailabilityRepository`: Atama ve hafta günü bazlı mesai pencereleri yönetimi.
- `com.example.clinic.staff.infrastructure.AvailabilityExceptionRepository`: Atama ve tarih bazlı izin/mesai istisnaları yönetimi.

### E. Ortak Saat Yapılandırması (`common.clock`)
- `com.example.clinic.common.clock.ClockConfig`: Deterministik zaman kontrolleri ve test edilebilirlik için `@ConditionalOnMissingBean` ile `Clock.systemUTC()` sağlayan Spring yapılandırması.

### F. Çizelgeleme Uygulama ve Entegrasyon Katmanı (`scheduling.application`)
- `com.example.clinic.scheduling.application.port.AppointmentSchedulePort`: S06 randevu kalıcılığını S05'ten yalıtan ikincil arayüz portu.
- `com.example.clinic.scheduling.application.port.DefaultAppointmentSchedulePort`: Bellek-içi ve test amaçlı kararlı port adaptörü.
- `com.example.clinic.scheduling.application.AvailabilityCalculator`: WP-S05-02 müsaitlik motoru. Şube mesai kesişimi, hekim haftalık mesaisi, izin eksiltmesi, fazla mesai birleşimi, randevu çakışma filtrelemesi ve şube zaman diliminde geçmiş slot elemesini atomik olarak yürütür.

### G. Personel Yönetimi Uygulama Servisi (`staff.application`)
- `com.example.clinic.staff.application.StaffManagementService`: Personel kaydı, şube ataması, yetkinlik yapılandırması, haftalık takvim yönetimi ve takvim istisnalarını yöneten `@Transactional` uygulama servisi.
- `com.example.clinic.staff.application.dto.*`:
  - `CreateStaffCommand`, `StaffProfileResponse`
  - `AssignStaffCommand`, `StaffAssignmentResponse`
  - `ConfigureEligibilityCommand`, `StaffServiceEligibilityResponse`
  - `WeeklyAvailabilityItem`, `ConfigureWeeklyAvailabilityCommand`, `WeeklyAvailabilityResponse`
  - `CreateAvailabilityExceptionCommand`, `AvailabilityExceptionResponse`

---

## 4. Mimari Kararlar ve Tasarım Detayları (ADR-011)

1. **Saf Domain Saflığı ile JPA Kalıcılığının Ayrılması (ADR-011):**
   - Sprint S02'de kabul edilen ADR-009 ve `DomainArchitectureTest` doğrulaması uyarınca `scheduling.domain` ve `booking.domain` paketleri saf POJO kalmak zorundadır.
   - Bu nedenle `StaffAssignment`, `StaffServiceEligibility`, `WeeklyAvailability` ve `AvailabilityException` JPA varlıkları, ilişkisel görevlendirme aggregate root'u olan `com.example.clinic.staff.domain` paketinde konumlandırılmıştır.
   - `scheduling.domain` paketinde ise slot hesaplama sonuçlarını temsil eden saf `TimeSlot` record'u yer almıştır.
2. **Görevlendirme Odaklı Hiyerarşi (StaffAssignment as Root):**
   - Bir hekim birden fazla şubede veya aynı şubede farklı bölümlerde çalışabilir.
   - Bu esneklik nedeniyle yetkinlikler (`StaffServiceEligibility`), haftalık mesai (`WeeklyAvailability`) ve izinler (`AvailabilityException`), hekimin şahsına değil; şube/bölüm görevlendirmesine (`staff_assignment`) bağlanmıştır.
3. **Çok Kiracılı Güvenlik ve IDOR Koruması:**
   - Personel kaydı ve şube atamalarında `AccessPolicy.enforceAdminBranchAccess` işletilir; yöneticinin yetkisi olmayan şubeye hekim ataması engellenir.
   - Mesai ve izin yönetiminde hem şube yöneticisi hem de hekimin kendisi (`isStaffOwner`) güvenli şekilde yetkilendirilir.
4. **Çift Kademeli Çakışma ve İzin Yönetimi (Interval Algebra):**
   - `AvailabilityCalculator` içinde zaman dilimleri ayrık pencereler halinde modellenmiş; kısmi izinler (`PARTIAL_LEAVE`) mesai pencerelerini bölerek (`splitRange`) izin aralığına denk gelen randevu slotlarının oluşmasını matematiksel kesinlikle engellemiştir.

---

## 5. Test Envanteri

| Test Sınıfı | Test Türü | Doğrulanan Kapsam |
|---|---|---|
| `StaffProfileDomainTest` | Unit Test | DOCTOR, NURSE, TECHNICIAN tipleri (TC-007), doktor için zorunlu uzmanlık (TC-008), geçersiz tip reddi |
| `StaffEntitiesDomainTest` | Unit Test | Bölüm-şube uyumsuzluğu engeli, yetkinlik şube kontrolü (STA-02), hafta günü 1..7 kuralı, izin kuralları |
| `TimeSlotTest` | Unit Test | Slot sıralaması, çakışma (`overlaps`) denetimi, aralık içine sığma (`fitsInside`) kontrolü |
| `AvailabilityCalculatorTest` | Unit Test (Mockito) | 09:00-12:00 / 30dk slot üretimi (`6 slot`), tam gün izin dışlama (`0 slot`), 10:00-10:30 randevu dışlama (`5 slot`), 10:00-11:00 kısmi izin dışlama (`4 slot`), yetkisiz hizmet engeli, geçmiş tarih engeli |
| `StaffManagementServiceTest` | Unit Test (Mockito) | Doktor kaydı ve `ROLE_STAFF` üyeliği, boş uzmanlık reddi, şube ataması, yetkisiz yönetici engeli, yetkinlik bağlama, çakışan haftalık mesai reddi, izin ekleme |
| `StaffManagementServiceIT` | Entegrasyon (PostgreSQL) | TC-007 (Doktor/teknisyen kaydı ve atama), TC-008 (Uzmanlık kontrolü), TC-009 (Yetkili hizmet eşleştirme ve yetkisiz hizmet engeli), TC-010 & TC-011 (Haftalık mesai, izin istisnası ve veritabanı destekli slot hesaplama) |
| `StaffDatabaseConstraintIT` | Entegrasyon (PostgreSQL) | Veritabanı seviyesinde `uq_staff_branch_department`, `uq_assignment_service`, check constraint'ler ve cascade davranışları |
| `FlywayMigrationIT` | Entegrasyon (PostgreSQL) | V1, V2 ve V3 migrasyonlarının PostgreSQL üzerinde hatasız çalışması ve 15 tablonun doğrulanması |
| `DomainArchitectureTest` | Mimari Sınır Testi | `scheduling/domain` ve `booking/domain` paketlerinin saf POJO kalması ve hiçbir Spring/JPA/Hibernate bağımlılığı içermemesi |

---

## 6. Ertelenen Fonksiyonlar ve Gelecek Sprint Bağımlılıkları

- **S06 Randevu Alma ve Eşzamanlılık:** Randevu varlığı (`Appointment`) veritabanı tablosu (`V4__create_appointments.sql`), JPA entity/repository katmanı, `PESSIMISTIC_WRITE` satır kilidi ve çoklu iş parçacığı (concurrency) testleri S06'da inşa edilecektir.
- **S07 Randevu Yaşam Döngüsü:** Randevu durum makinesinin (`approve`, `reject`, `cancel`, `complete`) servis katmanında yetkilendirilmesi ve durum geçmişi tablosu S07'de tamamlanacaktır.
- **S08 Web ve Thymeleaf:** `/admin/staff` ve `/patient/available-slots` kullanıcı arayüzleri S08'de son kullanıcıya açılacaktır.

---

## 7. Sprint Kabul Statüsü

- **Mevcut Statü:** `AWAITING_OWNER_TEST`
- **Tüm kaynak kod, migrasyon, entity, servis ve test sınıfları statik olarak hazırlanmış ve doğrulanmıştır.**
- **Test çalıştırma yasağı kuralı uyarınca testler ajan tarafından çalıştırılmamıştır; geliştiricinin manuel doğrulamasına sunulmuştur.**
