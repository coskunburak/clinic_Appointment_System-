# Architecture Decision Records (ADR)

## ADR-001 — Modular Monolith + Java Spring MVC
**Durum:** Kabul Edildi (Sprint S00 kararı).
**Bağlam:** Akademik takvim süresi, 1–2 geliştirici, 3 rol (Hasta, Personel, Yönetici).
**Karar:** Tek dağıtılabilir bir Spring Boot monoliti, mantıksal modüller ve sunucu taraflı HTML (Thymeleaf).
**Alternatifler:** JavaFX masaüstü uygulaması veya REST API + React/Vue SPA.
**Bedel & Takas:** Thymeleaf'in zengin dinamik bileşen esnekliği SPA'ya göre daha düşüktür; buna karşılık JavaScript/Node derleme yükü yoktur, teslim hızı yüksektir ve proje saf Java/Spring ekosisteminde kalarak OOP değerlendirmesine tam odaklanır.
**Geri Dönüş Yolu:** Domain ve servis katmanları web arayüzünden bağımsız tasarlandığı için ileride REST controller veya masaüstü arayüzü eklenebilir.

## ADR-002 — PostgreSQL + Flyway Migrations
**Durum:** Kabul Edildi (Sprint S01'de uygulanacak).
**Karar:** Gerçek ilişkisel veritabanı olarak PostgreSQL 16+ kullanılacak, şema `Flyway` ile sürümlenecek ve Hibernate `ddl-auto=validate` modunda çalışacaktır.
**Alternatifler:** H2 In-Memory Database.
**Gerekçe:** PostgreSQL'in satır kilitleme (`SELECT FOR UPDATE`), zaman aralığı (tsrange) ve transaction izolasyon davranışları H2'den farklıdır. Çakışma testlerinin gerçekçi olması için PostgreSQL zorunludur.
**Bedel:** Geliştiricinin yerel makinesinde Docker veya kurulu PostgreSQL çalıştırması gerekir.

## ADR-003 — PENDING Durumunun da Zaman Aralığını Bloke Etmesi
**Durum:** Kabul Edildi (Sprint S06'da uygulanacak).
**Karar:** `PENDING`, `CONFIRMED` ve geçmiş `COMPLETED` randevuları çakışma sorgularında zaman aralığını dolu kabul eder. Yalnızca `CANCELLED` ve `REJECTED` durumundaki randevular aralığı serbest bırakır.
**Gerekçe:** Yönetici veya personel onayı beklenirken başka bir hastanın aynı slota rezervasyon yapması engellenir.
**Bedel:** Onay bekleyen randevu slotu işgal eder; kötüye kullanımı önlemek adına ilerleyen aşamalarda zaman aşımı mekanizması eklenebilir. **Test:** TC-014, TC-015.

## ADR-004 — Personel Satır Kilidiyle Serializasyon (Pessimistic Locking)
**Durum:** Kabul Edildi (Sprint S06'da uygulanacak).
**Karar:** `BookingService` transaction başlangıcında ilgili personelin rezervasyon satırına `PESSIMISTIC_WRITE` kilidi alır, ardından `[start, end)` aralık çakışması sorgusu yapar ve kaydeder.
**Önemli Kural:** İlgili personeli ilgilendiren tüm randevu yazma, onaylama, iptal ve saat değiştirme işlemleri aynı kilit protokolünü izlemelidir. İkincil savunma olarak PostgreSQL seviyesinde exclusion constraint değerlendirilebilir.
**Gerekçe:** Eşzamanlı iki HTTP isteğinde yarış durumunu (race condition) ve çift rezervasyonu kesin olarak engellemek.
**Risk & Önlem:** Kilit bekleme süresi (lock timeout); kontrollü hata mesajı ve entegrasyon testi gerekir.

## ADR-005 — Ücret ve Süre Bilgilerinin Randevuda Snapshot Olarak Saklanması
**Durum:** Kabul Edildi (Sprint S06/S07'de uygulanacak).
**Karar:** Hizmet kataloğundaki ücret ve süre tanımları zamanla değişebileceğinden, randevu oluşturulduğu anda `serviceNameSnapshot`, `durationMinutesSnapshot`, `feeAmountSnapshot` ve `currencySnapshot` alanları randevu kaydına kopyalanır.
**Alternatif:** Randevudan yalnızca katalog servisine foreign key bağlamak.
**Gerekçe:** Katalogda 6 ay sonra fiyat güncellendiğinde geçmiş randevuların finansal ve tarihsel doğruluğunun bozulmaması.

## ADR-006 — Organizasyon ve Şube Düzeyinde Çok Kiracılı Yetki Kapsamı
**Durum:** Kabul Edildi (Sprint S04'te uygulanacak).
**Karar:** Erişim kontrolleri sadece URL katmanında (`@PreAuthorize`) değil, servis katmanında da doğrulanır. Kullanıcının `Membership` bilgisi sorgulanan kurum ve şube kimliğiyle eşleşmelidir.
**Alternatif:** Yalnızca rol adına (`ROLE_MANAGER`) bakmak.
**Gerekçe:** IDOR (Insecure Direct Object Reference) açığını önlemek; A şubesinin yöneticisinin B şubesinin randevularını onaylamasını engellemek.

## ADR-007 — Hassas Sağlık Verilerinin Azaltımı ve Kurmaca Veri İlkesi
**Durum:** Kabul Edildi (Sprint S00 kararı).
**Karar:** Sistemde yalnızca kurmaca demo verisi kullanılır. T.C. Kimlik Numarası, tıbbi tanı/teşhis, reçete, epikriz raporu, kredi kartı bilgisi saklanmaz.
**Gerekçe:** Akademik ödev kapsamı ve hasta mahremiyeti. Yazılımın gerçek bir hastane entegrasyonu veya KVKK/HIPAA üretim sertifikasyonu iddiası yoktur.

## ADR-008 — Testlerin Geliştirici Tarafından Manuel Çalıştırılması İlkesi
**Durum:** Kabul Edildi (Sprint S00 kararı).
**Karar:** Yapay zekâ ajanı kaynak kod ve test sınıflarını hazırlar, statik analizini yapar; ancak test komutlarını doğrudan otomatik olarak çalıştırmaz. Geliştiriciye tek bir yorumsuz `bash` bloğu halinde komut listesi sunar. Geliştirici çıktıyı terminalden alıp ajana iletir.
**Gerekçe:** Kontrollü maliyet, deterministik test kanıtı ve öğrencinin geliştirme ortamına tam hakimiyeti.

## ADR-009 — Saf Domain Çekirdeği, Değişmez Değer Nesneleri ve Polimorfik Kural Motoru
**Durum:** Kabul Edildi (Sprint S02 kararı).
**Bağlam:** Sprint S02 saf nesneye dayalı programlama (OOP), zengin domain modeli ve kalıtım/polimorfizm ilkelerini kanıtlamayı hedefler. S01'de uygulanan PostgreSQL V1 şeması ve JPA kalıcılık altyapısı mevcuttur.
**Karar:**
1. **Saf POJO ve Kalıcılık Ayrımı:** `Money`, `TimeInterval`, `ServiceSnapshot`, `Appointment` ve `SchedulingConstraint` motoru Spring, Hibernate ve JPA bağımlılığı taşımayan saf POJO olarak inşa edilmiştir. `BaseEntity` ve `PersonProfile` ise mevcut V1 şeması ile uyumlu `@MappedSuperclass` hiyerarşisi olarak modellenmiştir; yeni bir `person_profile` tablosu veya discriminator kolonu eklenmemiştir. `branch_opening_hours` tablosunda `created_at` kolonu bulunmadığından `BranchOpeningHours` sınıfı `BaseEntity`'den türetilmemiştir.
2. **Kanonik Zaman Aralığı:** `TimeSlot` yerine mutlak zaman aralıkları için `[startAt, endAt)` yarı açık kuralını işleten `TimeInterval` kanonik sınıf olarak seçilmiştir.
3. **İptal Zaman Politikası:** S02 önerisinde geçen "2 saat kala iptal engeli" henüz onaylı bir ADR ile karara bağlanmadığı için genel kural olan "başlamadan önce iptal edilebilir" politikası aggregate seviyesinde işletilmiştir.
4. **Durum Makinesi:** `AppointmentStatus` enum'ı ve `Appointment` içindeki iş davranış metotları (`approve`, `reject`, `cancel`, `complete`) ile kontrolsüz setter'lar engellenmiş, durum geçişleri deterministik `Clock` ile korunmuştur.
5. **Polimorfizm:** `SchedulingConstraint` arayüzü ve `SchedulingValidator` koordinatörü ile Açık/Kapalı Prensibi (OCP) uygulanmış, `instanceof` veya `switch` kullanılmaksızın kural işletimi sağlanmıştır.

## ADR-010 — Kompozit İlişkisel Bütünlük ile Çok Kiracılı İzolasyon ve Hizmet Kataloğu Mimarisi
**Durum:** Kabul Edildi (Sprint S03 kararı).
**Bağlam:** Sprint S03 kurum, şube, haftalık mesai saatleri, tıbbi bölümler ve şube bazlı ücret/süreli hizmet kataloğunu inşa eder. Yalnızca tekil yabancı anahtarlar (`branch_id`, `service_definition_id`) kullanıldığında, A Kurumunun şubesine B Kurumunun hizmetinin bağlanması veritabanı seviyesinde engellenemez.
**Karar:**
1. **İlişkisel Çok Kiracılı Güvence:** `branch` tablosundaki mevcut `uq_branch_id_org_id (id, organization_id)` kısıtına paralel olarak `service_definition` tablosuna `uq_service_definition_id_org_id (id, organization_id)` tekillik kısıtı tanımlanmıştır. `branch_service` tablosunda `organization_id` kolonu tutulmuş ve hem `branch(id, organization_id)` hem de `service_definition(id, organization_id)` tablolarına kompozit foreign key (`fk_branch_service_branch_org`, `fk_branch_service_def_org`) bağlanmıştır. Böylece Java katmanı baypas edilse dahi veritabanı motoru seviyesinde çapraz kiracı (cross-tenant) eşleşmesi kesin olarak imkânsız kılınmıştır.
2. **Zengin Domain Modeli ve Değer Nesnesi Entegrasyonu:** `ServiceDefinition` ve `BranchService` varlıkları `BaseEntity` mapped superclass'ından türetilmiş, kontrolsüz setter'lar engellenerek durum değişiklikleri niyet belirten iş metotlarıyla (`rename`, `changePrice`, `changeDuration`, `activate`, `deactivate`) kapsüllenmiştir. Fiyatlandırmada S02'de inşa edilen `Money` değer nesnesi yeniden kullanılarak kuruş hassasiyeti ve negatif tutar engeli garanti altına alınmıştır.
3. **Snapshot Deseni Uyumluluğu:** `BranchService.toSnapshot()` metodu ile katalog varlığı, S06 randevu modülü için değişmez `ServiceSnapshot` nesnesine dönüştürülebilir hale getirilmiştir.
4. **Atomik Mesai Güncelleme:** `OrganizationAdminService.configureOpeningHours` operasyonunda tüm girdiler (çakışma, ters saat, hafta günü aralığı) kalıcı silme işleminden önce doğrulanmakta, ardından `@Transactional` sınırında atomik değiştirme (replace) gerçekleştirilmektedir.


## ADR-011 — Personel Görevlendirme, Yetkinlik, Müsaitlik ve İstisnaların Kalıcılık Mimarisi ve Saf Çizelgeleme Ayrımı
**Durum:** Kabul Edildi (Sprint S05 kararı).
**Bağlam:** Sprint S05 doktor ve sağlık personeli kaydı (`StaffProfile`), şube/bölüm görevlendirmesi (`StaffAssignment`), şube bazlı hizmet yetkinliği (`StaffServiceEligibility`), haftalık mesai takvimi (`WeeklyAvailability`) ve takvim istisnalarını (`AvailabilityException`) inşa eder. S02'de kabul edilen ADR-009 gereğince `scheduling.domain` saf POJO olmalı; `DomainArchitectureTest` uyarınca hiçbir Spring/JPA bağımlılığı içermemelidir.
**Karar:**
1. **İlişkisel Bütünlük ve Görevlendirme Kökü:** `staff_assignment` tablosu `(staff_id, branch_id, department_id)` tekilliği ile görevlendirmenin aggregate root'u olarak konumlandırılmıştır. `staff_service_eligibility`, `weekly_availability` ve `availability_exception` tablolarının tamamı doğrudan `staff_assignment(id)` yabancı anahtarına (`ON DELETE CASCADE`) bağlanmıştır.
2. **Kalıcılık ve Saf Domain Ayrımı:** JPA varlıkları (`StaffAssignment`, `StaffServiceEligibility`, `WeeklyAvailability`, `AvailabilityException`), `com.example.clinic.staff.domain` paketi altında konumlandırılmıştır. Böylece `com.example.clinic.scheduling.domain` paketinin saf POJO saflığı (`DomainArchitectureTest`) korunmuş; aday slot temsili için saf `TimeSlot(LocalTime startTime, LocalTime endTime)` record'u scheduling domain'e eklenmiştir.
3. **Müsaitlik ve Slot Hesaplama Motoru (`AvailabilityCalculator`):** `com.example.clinic.scheduling.application` katmanında konumlandırılmış, şube mesai saatleri (SCH-01), personel haftalık aralığı (SCH-02), izin istisnaları (SCH-03), hizmet yetkinliği (STA-02) ve randevu çakışmalarını (ADR-003) saf bir aralık cebiriyle (interval algebra) çözerek boş slotları üretecek şekilde inşa edilmiştir.
4. **Randevu Çizelgeleme Portu (`AppointmentSchedulePort`):** S06'da veritabanına bağlanacak olan `Appointment` kalıcılığını S05'ten yalıtmak adına ikincil bir port tanımlanmış, `DefaultAppointmentSchedulePort` ile bellek-içi test ve genişletilebilirlik sağlanmıştır.
5. **Uzmanlık Zorunluluğu (STA-01 / TC-008):** `StaffProfile` üzerinde `DOCTOR` çalışan tipi için `specialty` alanı zorunlu kılınmış, boş/null değerler doğrudan `IllegalArgumentException` ile engellenmiştir. `NURSE` ve `TECHNICIAN` tipleri için uzmanlık opsiyonel bırakılmıştır.
