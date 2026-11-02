# Kısa proje raporu / README içerik taslağı

## Kapak
Ders: Object Oriented Programming Language; seçilen konu: Hastane/Klinik Randevu Sistemi; öğrenci(ler), tarih, öğretim elemanı, GitHub URL.

## Bölüm 1 — Problem ve gereksinimler
Kullanıcılar, çalışma saatleri, süre/fiyat, yetkili personel, randevu çakışması, onay mekanizması, üç rol. `FR` tablosuna referans ver.

## Bölüm 2 — Kullanılan teknolojiler
Java sürümü, Spring Boot, Thymeleaf, PostgreSQL, Flyway, JUnit, Maven. Seçim gerekçesi ve alternatiflere göre avantaj/bedel.

## Bölüm 3 — Nesneye dayalı tasarım
- UML inheritance: `PersonProfile` alt sınıfları.
- Encapsulation: `Appointment` yalnız davranış metotlarıyla geçiş yapar.
- Polymorphism: `SchedulingConstraint` stratejileri.
- Abstraction/SOLID/DI: repository port, `Clock` ve application service.
- Sınıf diyagramıyla **kaynak kodu bağdaştır**; gerçek uygulanmayan özellik rapora geçmiş gibi yazılmaz.

## Bölüm 4 — Sistem mimarisi ve veri modeli
Katmanlar, modüller, ER, ilişkiler, branch isolation, tarih/saat ve ücret snapshot.

## Bölüm 5 — Çakışma ve onay algoritması
Yarı açık interval formülü, `PESSIMISTIC_WRITE` staff lock transaction ve randevu durum diyagramı; uygunluk sorgusunun rezervasyon garantisi olmadığı.

## Bölüm 6 — Testler ve sonuçlar
MUST FR → TC → rapor, JUnit çıktıları, 2 transaction concurrency testi, negatif yetki testleri, ekran görüntüleri. Başarısız/çalıştırılmamış testler dürüstçe belirtilir.

## Bölüm 7 — Proje yönetimi / GitHub
Gerçek 10+ ayrı gün commit listesi, Sprint S00–S10 tamamlananlar, kalan riskler.

## Bölüm 8 — Sınırlılıklar / gelecek işler
Ödeme, gerçek hasta kaydı, e-Nabız/MHRS, SMS, çoklu klinik karmaşık üyelik modelleri gerçek sistem entegrasyonu olarak uygulanmadı (uygulamada farklıysa düzelt).

## Ekler
Kurulum README, kaynak kodu ve GitHub URL, sunum, migration şeması, test raporu, demo screenshot klasörü.
