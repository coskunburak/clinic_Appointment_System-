# Architecture Decision Records (ADR)

## ADR-001 — Modular Monolith + Java Spring MVC
**Durum:** önerilen / kabul bekliyor. **Bağlam:** akademik süre, 1–2 geliştirici, üç rol. **Karar:** tek dağıtım, mantıksal modüller, server-side render. **Alternatif:** JavaFX veya REST + React. **Bedel:** Thymeleaf görsel esnekliği düşüktür; teslim hızı ve Java ağırlığı yüksektir. **Geri dönüş:** web katmanı değişebilir.

## ADR-002 — PostgreSQL + migrations
**Karar:** gerçek PostgreSQL, `Flyway` ve `ddl-auto=validate`; veriler elle silinmez. **Alternatif:** H2 memory. **Gerekçe:** kilitleme/transaction davranışı H2'de farklı olabilir. **Bedel:** Docker veya lokal PostgreSQL gerekir.

## ADR-003 — PENDING de zaman aralığını bloke eder
**Karar:** `PENDING`, `CONFIRMED` ve tarihsel `COMPLETED` kayıtları çakışma sorgusunda korunur; `CANCELLED`, `REJECTED` bloke etmez. **Gerekçe:** onay beklerken çift rezervasyon önlenir. **Bedel:** onay bekleyen talep slotu işgal eder; ileride süre aşımı geliştirilebilir. **Test:** TC-014/TC-015.

## ADR-004 — Personel satır kilidiyle serializasyon
**Karar:** `BookingService` transaction başlangıcında ilgili `StaffProfile` veya ayrı `StaffBookingLock` satırına `PESSIMISTIC_WRITE` kilidi alır, sonra [start,end) sorgusu yapar ve kaydeder. **ÖNEMLİ:** aynı personeli ilgilendiren **tüm** randevu oluşturma, iptal, saat değiştirme gibi yazma yolları aynı protokolü izlemelidir. İkincil savunma opsiyonu Postgres EXCLUDE constraint'tir. **Gerekçe:** yarış durumunda okuma+insert güvenliği. **Risk:** lock timeout; kontrollü hata ve test gerekir.

## ADR-005 — Para/tarih snapshot
**Karar:** `BranchService` ücret/dakika kaynağı; randevu oluşturulunca `serviceNameSnapshot`, `durationMinutesSnapshot`, `feeAmountSnapshot`, `currencySnapshot`, `start/end` saklanır. **Alternatif:** sadece foreign key; geçmişte fiyat değişmesi sorunlu. **Gerekçe:** geçmiş randevu doğruluğu.

## ADR-006 — Organizasyon ve şube yetkisi
**Karar:** erişim hem route hem application service katmanında kontrol edilir, `Membership` kaynak organization/branch scope ile eşleşmelidir. **Alternatif:** yalnız role annotation; IDOR riskini karşılamaz.

## ADR-007 — Hassas veri azaltımı
**Karar:** demosu kurmaca hastalar ile yapılır. Kimlik numarası, tanı, reçete, gerçek sağlık kaydı, kart bilgisi saklanmaz. **Gerekçe:** ödev kapsamı ve mahremiyet. Resmî sağlık hizmeti veya KVKK uyumluluğu iddiası yoktur.

## ADR-008 — Testleri öğrenci çalıştırır
**Karar:** AI ajan kod ve test dosyalarını hazırlar ama komutları otomatik çalıştırmaz; tek `bash` kod bloğu içinde yorumsuz komutlar sunar. Öğrenci çıktıyı geri gönderir; ajan düzeltir. **Gerekçe:** kontrollü maliyet ve kanıta dayalı kabul. CI farklı bir mekanizma olarak ayrıca çalışabilir.
