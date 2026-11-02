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
