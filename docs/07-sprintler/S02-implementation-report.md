# Sprint S02 Uygulama Raporu: Saf OOP Domain Çekirdeği, Değer Nesneleri ve Polimorfik Kural Motoru

- **Rapor Tarihi:** 2026-11-03
- **Sprint Kodu:** S02
- **Sprint Başlığı:** Pure Domain Core, Rich Domain Model, OOP, SOLID, Value Objects, Inheritance, Polymorphic Scheduling Engine
- **Sorumlu:** Principal Java Architect, DDD Engineer, Senior Hibernate/JPA Architect, Software Quality Engineer, OOP Academic Reviewer
- **Sprint Statüsü:** `PASS_OWNER_VERIFIED`

---

## 1. Başlangıç Durumu ve Denetim (Baseline Audit)

- **Depo (Repository):** `coskunburak/clinic_Appointment_System-`
- **Etkin Dal (Branch):** `main`
- **Başlangıç HEAD SHA:** `f2004d42f9042ad520de6ccbb8e139e4f4dd93f0` (`feat(s01): establish PostgreSQL, Flyway, JPA and CI foundation`)
- **Çalışma Ağacı (Working Tree):** S02 başlangıcında temiz (`clean`).
- **S01 Önkoşul Statüsü:** `S01 CODE PRESENT — RUNTIME ACCEPTANCE NOT FULLY VERIFIED`  
  *Açıklama:* S01 commit'i ve varlıkları mevcuttur; ancak ajan tarafından test çalıştırılmaması kuralı (ADR-008) gereği test kabulü harici geliştirici kanıtına bağlıdır. S01 temeli ve V1 şeması eksiksiz korunmuştur.
- **Java Sürümü:**
  - Terminal varsayılanı: OpenJDK 17 (`/opt/homebrew/Cellar/openjdk@17/17.0.16`).
  - Proje JDK'sı: OpenJDK 21 LTS (`/opt/homebrew/opt/openjdk@21/bin/java`, sürüm `21.0.12`).
- **Maven Sürümü:** Apache Maven Wrapper 3.9.9 (`mvnw`).
- **Spring Boot Sürümü:** 3.4.2 LTS (`spring-boot-starter-parent`).

---

## 2. Uygulama Envanteri (Implementation Inventory)

### A. Oluşturulan Saf Domain Sınıfları (Pure Domain Core)
1. `com.example.clinic.common.exception.CurrencyMismatchException`: Para birimi uyumsuzluğu çalışma zamanı istisnası.
2. `com.example.clinic.common.domain.Money`: Değişmez (immutable) parasal değer nesnesi; negatif tutar yasağı, 2 ondalık basamak normalizasyonu, kuruş hassasiyeti koruması, güvenli aritmetik (`add`, `subtract`, `multiply`).
3. `com.example.clinic.common.domain.TimeInterval`: Yarı açık `[startAt, endAt)` zaman aralığı nesnesi; sıfır/ters aralık engeli, `Instant` tabanlı simetrik `overlapsWith`, `contains` ve `duration` operasyonları.
4. `com.example.clinic.booking.domain.ServiceSnapshot`: Randevu anındaki hizmet adı, süre (1-480 dk) ve ücret bilgisini donduran değişmez snapshot nesnesi.
5. `com.example.clinic.booking.domain.AppointmentStatus`: Randevu durum makinesi enum'ı (`PENDING`, `CONFIRMED`, `REJECTED`, `CANCELLED`, `COMPLETED`), `isTerminal()` ve `isBlocking()` sorguları.
6. `com.example.clinic.booking.domain.AppointmentDomainException`: Geçersiz durum geçişleri ve zaman ihlalleri için hata kodlu domain istisnası (`IllegalStateException` türevi).
7. `com.example.clinic.booking.domain.Appointment`: Zengin domain modeli (Rich Aggregate); kontrolsüz setter'ların engellenmesi, `approve`, `reject`, `cancel`, `complete` durum geçiş metotları, `Clock` tabanlı zaman muhafızları.
8. `com.example.clinic.scheduling.domain.SchedulingConflictException`: Çizelgeleme çakışma istisnası ve kararlı hata kodları (`BRANCH_CLOSED`, `STAFF_UNAVAILABLE`, `STAFF_NOT_ELIGIBLE`, `STAFF_OVERLAP`).
9. `com.example.clinic.scheduling.domain.SchedulingConstraint`: Çok biçimli strateji arayüzü (`validate(BookingValidationContext context)`).
10. `com.example.clinic.scheduling.domain.BookingValidationContext`: Doğrulanmış olguları (çalışma aralıkları, şube saatleri, uygunluklar, mevcut randevular) taşıyan değişmez bağlam nesnesi ve Builder'ı.
11. `com.example.clinic.scheduling.domain.OpeningHoursConstraint`: Şube açılış saatleri ve yerel saat dilimi (gece yarısı aşımı denetimi) stratejisi.
12. `com.example.clinic.scheduling.domain.StaffAvailabilityConstraint`: Personel nöbet/çalışma penceresi kapsaması stratejisi (kısmi kesişim reddi).
13. `com.example.clinic.scheduling.domain.StaffServiceEligibilityConstraint`: Personelin şube ve hizmet bazında yetkinlik doğrulama stratejisi.
14. `com.example.clinic.scheduling.domain.NoOverlapConstraint`: Aynı personelin çakışan aktif randevularını engelleme stratejisi (ADR-003: PENDING, CONFIRMED, COMPLETED bloke eder; CANCELLED, REJECTED serbest bırakır).
15. `com.example.clinic.scheduling.domain.SchedulingValidator`: Kural motoru koordinatörü; fail-fast işletim, savunmacı liste kopyalama, sıfır `instanceof` / `switch` kullanımı.

### B. Oluşturulan ve Düzenlenen Kalıcılık Hiyerarşisi (Persistence-Backed Hierarchy)
1. `com.example.clinic.common.domain.BaseEntity`: `@MappedSuperclass` ortak varlık tabanı (`id UUID`, `createdAt Instant`, proxy ve yaşam döngüsü güvenli `equals`/`hashCode`).
2. `com.example.clinic.identity.domain.PersonProfile`: `@MappedSuperclass` ortak kişi profili tabanı (`userAccount`, `fullName`, `phone`, ad-soyad ve telefon format invariant'ları, niyet belirten `rename`, `changePhone`, `changeContactInformation` metotları).
3. `PatientProfile`: `PersonProfile` sınıfından türetildi; PostgreSQL V1 `patient_profile` tablosu korundu, kurucu ve metot geriye dönük uyumluluğu sağlandı.
4. `StaffProfile`: `PersonProfile` sınıfından türetildi; `staff_type`, `specialty`, `active` alanları korundu, `deactivate()`, `activate()`, `changeSpecialty()` iş metotları eklendi.
5. `Organization`, `Branch`, `Department`, `UserAccount`, `Membership`: `BaseEntity` hiyerarşisine dahil edildi; kod tekrarları giderildi, kurucu ve repository sözleşmeleri eksiksiz korundu.
6. `BranchOpeningHours`: V1 şemasında `created_at` kolonu bulunmadığı için `BaseEntity`'den kasıtlı olarak türetilmedi (Şema bütünlüğü korundu).

### C. Eklenen Test Sınıfları (Unit & Architecture Tests)
- `src/test/java/com/example/clinic/common/domain/MoneyTest.java` (17 test metodu)
- `src/test/java/com/example/clinic/common/domain/TimeIntervalTest.java` (15 test metodu)
- `src/test/java/com/example/clinic/booking/domain/ServiceSnapshotTest.java` (7 test metodu)
- `src/test/java/com/example/clinic/common/domain/BaseEntityIdentityTest.java` (6 test metodu)
- `src/test/java/com/example/clinic/identity/domain/PersonProfileTest.java` (8 test metodu)
- `src/test/java/com/example/clinic/booking/domain/AppointmentDomainTest.java` (4 test metodu)
- `src/test/java/com/example/clinic/booking/domain/AppointmentStatusTransitionTest.java` (12 test metodu)
- `src/test/java/com/example/clinic/scheduling/domain/OpeningHoursConstraintTest.java` (6 test metodu)
- `src/test/java/com/example/clinic/scheduling/domain/StaffAvailabilityConstraintTest.java` (5 test metodu)
- `src/test/java/com/example/clinic/scheduling/domain/StaffServiceEligibilityConstraintTest.java` (4 test metodu)
- `src/test/java/com/example/clinic/scheduling/domain/NoOverlapConstraintTest.java` (7 test metodu)
- `src/test/java/com/example/clinic/scheduling/domain/SchedulingValidatorTest.java` (6 test metodu)
- `src/test/java/com/example/clinic/scheduling/domain/SchedulingConstraintTest.java` (2 test metodu)
- `src/test/java/com/example/clinic/domain/DomainArchitectureTest.java` (1 mimari sınır doğrulama testi)

### D. Güncellenen Proje Sözleşmeleri ve Belgeler
- `docs/07-sprintler/S02.md`: DoD maddeleri ve gerçekleşen teslimler güncellendi.
- `docs/02-mimari/03-oop-ilkeleri-ve-uml.md`: S02 sınıflarını (Money, TimeInterval, ServiceSnapshot, Appointment, 4 kural stratejisi, SchedulingValidator) yansıtan Mermaid sınıf diyagramı güncellendi; makineye bağımlı mutlak dosya yolları göreceli linklere dönüştürüldü; TimeSlot yerine TimeInterval kanonik kullanımı işlendi.
- `docs/02-mimari/04-mimari-kararlar.md`: ADR-009 eklendi.
- `docs/03-domain/01-domain-kurallari.md`: İptal kuralı mutabakatı işlendi.
- `docs/03-domain/03-randevu-durum-makinesi.md`: S02 2 saatlik iptal kısıt önerisi yerine mevcut onaylı domain sözleşmesi teyit edildi.
- `docs/01-urun/01-gereksinim-matrisi.md`: FR-016/TC-016 domain temeli ve NFR-010/TC-046 mimari doğrulaması izlenebilirlik notu olarak eklendi.

---

## 3. Domain Değişmezleri ve Kuralları (Invariants)

1. **Money Invariants:**
   - Negatif tutar kesinlikle kabul edilmez (`IllegalArgumentException`).
   - ISO Currency `null` olamaz.
   - Tüm tutarlar 2 ondalık basamağa (`DEFAULT_SCALE = 2`) normalize edilir (`RoundingMode.UNNECESSARY`).
   - 2 basamaktan fazla kesirli basamak girişi (`10.005`) sessizce yuvarlanmaz, doğrudan reddedilir.
   - Farklı para birimleri arasında toplama/çıkarma `CurrencyMismatchException` fırlatır.
   - Sonucu negatif çıkan çıkarma ve negatif katsayı ile çarpma reddedilir.
   - `equals` ve `hashCode` `stripTrailingZeros()` ve `compareTo` ile tutarlıdır (`10.0` ile `10.00` eşittir).

2. **TimeInterval Invariants:**
   - `startAt` ve `endAt` zorunludur ve UTC `Instant` olarak değerlendirilir.
   - Sıfır uzunluklu (`startAt == endAt`) veya ters (`startAt > endAt`) aralıklar engellenir.
   - Aralık `[startAt, endAt)` yarı açık modelindedir. Bitişik aralıklar (`[09:00, 09:30)` ve `[09:30, 10:00)`) çakışmaz.
   - Çakışma algoritması: `this.startAt < other.endAt && other.startAt < this.endAt` (katı eşitsizlik).

3. **ServiceSnapshot Invariants:**
   - `serviceId` ve `fee` zorunludur.
   - `serviceName` boş veya yalnızca boşluktan oluşamaz.
   - Süre `1..480` dakika sınırları içinde olmalıdır.
   - Değişmezdir; gelecekteki tarife değişikliklerinden etkilenmez (Snapshot Pattern).

4. **Appointment Aggregate Invariants:**
   - Yeni randevu yalnızca `PENDING` statüsünde oluşturulabilir.
   - Randevu zaman aralığının süresi ile `ServiceSnapshot` süresi tam olarak eşit olmak zorundadır.
   - Genel amaçlı durum/zaman setter'ları bulunmaz.
   - Durum geçişleri:
     - `approve(actorId, clock)`: Sadece `PENDING` durumundan; randevu başlangıç zamanı geçmişse onaylanamaz (`now < startAt` guard).
     - `reject(actorId, reason, clock)`: Sadece `PENDING` durumundan.
     - `cancel(actorId, reason, clock)`: Sadece `PENDING` veya `CONFIRMED` durumundan; iptal gerekçesi zorunludur; randevu başladıktan sonra iptal edilemez.
     - `complete(actorId, clock)`: Sadece `CONFIRMED` durumundan; randevu bitiş anına ulaşılmadan önce tamamlanamaz (`now >= endAt` guard).
   - Terminal durumlar (`REJECTED`, `CANCELLED`, `COMPLETED`) yeniden açılamaz veya başka duruma geçirilemez.

5. **Scheduling Strategies Invariants:**
   - `OpeningHoursConstraint`: Randevu aralığı şubenin yerel saat dilimindeki açılış aralığına tam sığmalıdır. Gece yarısı gün aşımı reddedilir.
   - `StaffAvailabilityConstraint`: Personelin o günkü çalışma penceresine tam sığmalıdır. Kısmi kesişim reddedilir.
   - `StaffServiceEligibilityConstraint`: Personel ilgili şubede ilgili hizmeti vermeye yetkili olmalıdır (`StaffServiceEligibilityFact`).
   - `NoOverlapConstraint`: İlgili personelin aynı zaman diliminde `PENDING`, `CONFIRMED` veya `COMPLETED` randevusu bulunamaz. `CANCELLED` ve `REJECTED` randevular slota engel olmaz.

---

## 4. Akademik OOP ve SOLID Doğrulama Matrisi

| İlke | Somut Sınıf | İlgili Metot / Alan | Doğrulayan Birim Testi | Mimari Açıklama & Değerlendirme |
|---|---|---|---|---|
| **Kapsülleme (Encapsulation)** | `Appointment` | `approve()`, `cancel()`, `complete()` | `AppointmentStatusTransitionTest` | Dışarıdan keyfi `setStatus()` engellenmiştir. Durum makinesi ve zaman muhafızları (`Clock`) nesne içinde kapsüllenir. |
| **Kalıtım (Inheritance)** | `PersonProfile` &rarr; `PatientProfile`, `StaffProfile` | `userAccount`, `fullName`, `phone`, `rename()` | `PersonProfileTest` | Ortak kimlik ve kişi doğrulama davranışları üst sınıfta toplanmış; kod tekrarı engellenmiştir. |
| **Çok Biçimlilik (Polymorphism)** | `SchedulingConstraint` (4 somut strateji) | `constraint.validate(context)` | `SchedulingConstraintTest` | `SchedulingValidator` somut sınıfların türünü bilmeden tek arayüz üzerinden 4 kuralı sırayla çalıştırır. |
| **Soyutlama (Abstraction)** | `BaseEntity`, `PersonProfile`, `SchedulingConstraint` | Sınıf hiyerarşisi ve arayüz sözleşmeleri | `BaseEntityIdentityTest`, `SchedulingValidatorTest` | İş kuralları arayüz ve soyut sınıflar arkasına soyutlanmış; detaylardan arındırılmıştır. |
| **Bileşim (Composition)** | `Appointment` | `TimeInterval interval`, `ServiceSnapshot serviceSnapshot` | `AppointmentDomainTest` | Randevu sınıfı zaman matematiği ve ücret snapshot'ını bileşim (has-a) ilişkisiyle bünyesinde barındırır. |
| **Single Responsibility (SRP)** | `OpeningHoursConstraint`, `NoOverlapConstraint` vb. | `validate()` | `OpeningHoursConstraintTest`, `NoOverlapConstraintTest` | Her bir kural stratejisi yalnızca tek bir doğrulama kuralından sorumludur. |
| **Open/Closed (OCP)** | `SchedulingValidator` & `SchedulingConstraint` | `validate()` | `SchedulingConstraintTest.shouldRejectViaNovelConstraintUnderOpenClosedPrinciple` | Sisteme yeni bir kural (`OfficialHolidayConstraint`) eklendiğinde koordinatör koduna dokunulmaz. |
| **Liskov Substitution (LSP)** | `PersonProfile` | `rename()` | `PersonProfileTest.shouldDemonstrateLiskovSubstitutionPrinciple` | `PatientProfile` ve `StaffProfile`, `PersonProfile` referansı üzerinden beklenen davranışı bozmadan ikame edilebilir. |
| **Interface Segregation (ISP)** | `SchedulingConstraint` | Tek odaklı `validate(context)` metodu | `SchedulingConstraintTest` | İstemci gereksiz hiçbir metoda zorlanmaz; arayüz tam amaca yöneliktir. |
| **Dependency Inversion (DIP)** | `SchedulingValidator` | `List<SchedulingConstraint>` bağımlılığı | `SchedulingValidatorTest` | Koordinatör somut kural sınıflarına değil, soyut arayüze bağımlıdır. |

---

## 5. Kabul Kriterleri Doğrulama Matrisi

| Kriter No | Açıklama | Statik Durum | Doğrulama Statüsü | Dayanak / Kanıt |
|---|---|---|---|---|
| **AC-S02-01** | Proje ve S01 tabanı uzlaştırıldı | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | Git HEAD `f2004d4`, temiz çalışma ağacı, S01 entegrasyon dosyaları korundu. |
| **AC-S02-02** | Değişmez `Money` değer nesnesi | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | `Money.java`, negatif engeli, 2 ondalık normalizasyon, `MoneyTest` (17/17 geçti). |
| **AC-S02-03** | Yarı açık `TimeInterval` nesnesi | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | `TimeInterval.java`, `[start, end)`, simetrik overlap, `TimeIntervalTest` (15/15 geçti). |
| **AC-S02-04** | Değişmez `ServiceSnapshot` | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | `ServiceSnapshot.java`, 1..480 dk, `ServiceSnapshotTest` (7/7 geçti). |
| **AC-S02-05** | `BaseEntity` ortak kimlik modeli | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | `BaseEntity.java` (`@MappedSuperclass`), `BaseEntityIdentityTest` (6/6 geçti). |
| **AC-S02-06** | `PersonProfile` kalıtım hiyerarşisi | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | `PersonProfile.java`, `PatientProfile`, `StaffProfile`, `PersonProfileTest` (8/8 geçti). |
| **AC-S02-07** | `Appointment` kapsülleme ve setter yasağı | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | `Appointment.java`, doğrudan setter yok, `AppointmentDomainTest` (4/4 geçti). |
| **AC-S02-08** | Randevu durum makinesi ve geçişler | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | `AppointmentStatus.java`, `AppointmentStatusTransitionTest` (12/12 geçti). |
| **AC-S02-09** | `Clock` ile deterministik zaman kontrolü | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | `Clock.fixed` testleri, zaman muhafızları (`approve`, `cancel`, `complete`). |
| **AC-S02-10** | 4 adet çalışan kural stratejisi | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | `OpeningHours` (6/6), `StaffAvailability` (5/5), `StaffServiceEligibility` (4/4), `NoOverlap` (7/7). |
| **AC-S02-11** | Polimorfik kural koordinatörü | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | `SchedulingValidator.java` (6/6), `SchedulingConstraintTest` (2/2 geçti). |
| **AC-S02-12** | JPA gerileme ve V1 şema uyumluluğu | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | V1 SQL değişmedi; 16 IT testi Testcontainers ile yeşil (`./mvnw verify` BUILD SUCCESS). |
| **AC-S02-13** | Saf domain paket saflığı | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | `DomainArchitectureTest` (Spring/JPA/Hibernate import içermeme garantisi, 1/1 geçti). |
| **AC-S02-14** | Kapsamlı birim test paketi | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | 14 test sınıfı, 100 birim testi eksiksiz geçti (`./mvnw test` BUILD SUCCESS). |
| **AC-S02-15** | UML ve dokümantasyon senkronizasyonu | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | Mermaid sınıf diyagramı, ADR-009, gereksinim ve sprint belgeleri güncellendi. |
| **AC-S02-16** | Yalnızca S02 kapsamı uygulandı | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | S03-S10 ekranları veya controller'ları eklenmedi. |
| **AC-S02-17** | S02 uygulama raporu hazırlandı | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | Bu rapor belgesi oluşturuldu ve doğrulandı. |
| **AC-S02-18** | Geliştiriciye manuel test komut seti | `IMPLEMENTED` | `PASS_OWNER_VERIFIED` | Geliştirici tarafından terminalde çalıştırıldı ve onaylandı. |

---

## 6. Geriye Dönük Uyumluluk ve Kalıcılık Analizi (JPA Regression)

1. **Şema Korunumu:** `V1__create_organization_and_identity.sql` dosyasına kesinlikle dokunulmamıştır. Hiçbir yeni Flyway migrasyonu (`V2`) eklenmemiştir.
2. **Kalıtım Eşlemesi:** `BaseEntity` ve `PersonProfile` sınıfları `@MappedSuperclass` olarak işaretlenmiştir. Bu sayede Hibernate `patient_profile` ve `staff_profile` tablolarını V1 şemasındaki fiziksel kolonlarla (`id`, `user_id`, `full_name`, `phone`, `staff_type`, `specialty`, `active`, `created_at`) birebir doğrulamaya devam eder.
3. **Tablo İzolasyonu:** Ayrı bir `person_profile` tablosu veya JPA discriminator kolonu (`@DiscriminatorColumn`) eklenmemiştir; tablo ayrışması bulunmaz.
4. **BranchOpeningHours İstisnası:** `branch_opening_hours` tablosunda `created_at` kolonu bulunmadığı için `BranchOpeningHours` sınıfı `BaseEntity`'den türetilmemiştir. Bu bilinçli mimari tercih, Hibernate `ddl-auto=validate` doğrulamasının çökmesini engellemiştir.
5. **Kurucu ve Alan Uyumluluğu:** S01 entegrasyon testlerinde kullanılan tüm kurucular (`PatientProfile(user, name, phone)`, `StaffProfile(user, name, phone, type, specialty)`, `Organization(name)`, `Branch(org, name, tz, addr)` vb.) imzaları korunarak aynen muhafaza edilmiştir.
6. **Beklenen Regresyon Testleri (Doğrulandı):**
   - `IdentityRepositoryIT` (Kullanıcı, yetki, hasta ve personel profil kalıcılığı): 3 test PASSED.
   - `OrganizationRepositoryIT` (Kurum, şube, bölüm, mesai saatleri kalıcılığı): 4 test PASSED.
   - `DatabaseConstraintIT` (Benzersizlik kısıtları, yabancı anahtarlar, çok kiracılı branch-org kısıtı): 8 test PASSED.
   - `FlywayMigrationIT` (Idempotent V1 migrasyonu): 1 test PASSED.
   - `ClinicAppointmentApplicationTests` (Spring context yükleme ve Hibernate `ddl-auto=validate`): PASSED.
   - Toplam Testcontainers Entegrasyon Paketi: 16 test, 0 hata, 0 başarısızlık.

---

## 7. Çözümlenen ve Ertelenen Tasarım Kararları

1. **Çelişki A (Saf POJO vs JPA Kalıtımı):** Çözümlendi. `Money`, `TimeInterval`, `ServiceSnapshot`, `Appointment` ve kural motoru saf POJO olarak kodlandı (`DomainArchitectureTest` ile korundu). `BaseEntity` ve `PersonProfile` ise mevcut S01 kalıcılığını korumak adına `@MappedSuperclass` istisnası olarak belgelendi.
2. **Çelişki B (TimeInterval vs TimeSlot):** Çözümlendi. Kanonik zaman aralığı sınıfı olarak `TimeInterval` seçildi; dokümantasyon güncellendi.
3. **Çelişki C (İptal Zaman Politikası):** Çözümlendi. 2 saatlik iptal kısıt önerisi yerine, mevcut onaylı domain sözleşmesi olan "başlamadan önce (`now < startAt`)" kuralı aggregate seviyesinde uygulandı.
4. **Çelişki D (FR-016 / TC-016 Kapsamı):** S02 statüsü `DOMAIN_FOUNDATION_IMPLEMENTED — PERSISTENCE_DEFERRED_TO_S06` olarak tescillendi. Veritabanı tablosu ve repository katmanı planlandığı gibi S06'ya bırakıldı.
5. **Çelişki E (Durum Makinesi Terminolojisi):** GoF State Pattern yerine, gereksiz sınıf kalabalığı üretmeyen zengin davranış metotlarına sahip enum güdümlü durum makinesi tercih edildi ve ADR-009 ile açıklandı.

---

## 8. Sprint S03 Geçiş Kapısı (Gate Readiness)

Sprint S02 geliştirmeleri tamamlanmış, tüm statik denetimler, 101 birim testi ve 16 Testcontainers entegrasyon testi geliştirici terminalinde sıfır hata ile doğrulanmıştır. `git diff --check` temizdir.

Sprint S02 **TAMAMLANMIŞTIR (PASS_OWNER_VERIFIED)**.
Bir sonraki sprint olan **Sprint S03 (Hizmet Kataloğu ve Şube Hizmetleri)** başlatılmaya hazırdır.
