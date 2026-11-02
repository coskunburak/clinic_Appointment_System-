# Java Hastane / Klinik Randevu Sistemi — Dokümantasyon Merkezi

**Belge sürümü:** 1.0 · **Planlama tarihi:** 2026-11-02 · **Proje:** OOP dersi / Seçenek A · **Durum:** Uygulama başlamadan önceki onaylanabilir tasarım taslağı

> Bu ZIP **uygulama kaynak kodu içermez**. Uygulamayı geliştirirken izlenecek kapsamı, mimariyi, veri modelini, testleri, günlük commit stratejisini, sprintleri, sunumu ve yapay zekâ ajanı talimatlarını içerir. Belge içindeki örnek yol ve sınıflar tasarım önerisidir; uygulanmış özellik iddiası değildir.

## 1. Başlangıç sırası
1. [Kararlar ve varsayımlar](00-baslangic/01-kararlar-ve-varsayimlar.md) — belirsizlikleri sabitle.
2. [Ödev gereksinimleri](01-urun/01-gereksinim-matrisi.md) — değerlendirmenin kaynak belgesi.
3. [Kapsam ve kullanıcı akışları](01-urun/02-kapsam-ve-kullanici-hikayeleri.md).
4. [Mimari ve sınıf tasarımı](02-mimari/01-teknoloji-ve-katmanlar.md), [OOP kanıtları](02-mimari/03-oop-ilkeleri-ve-uml.md).
5. [İş kuralları](03-domain/01-domain-kurallari.md), [randevu ve eşzamanlılık](03-domain/02-randevu-algoritmasi.md).
6. [Sprint master planı](07-sprintler/README.md) ve her sprintin kabul kriterleri.
7. [GitHub/teslim](08-teslim/01-github-commit-takvimi.md), [sunum](08-teslim/02-sunum-ve-demo.md).

## 2. Karar özeti
- **Birincil teslim:** çalışan Java Spring Boot **web monoliti**; ekranlar Thymeleaf, PostgreSQL.
- **MVP:** bir hastane, en az iki şube için gösterim verisi; şube ve bölüm/servis/doktor çalışma saatleri; üç rol; çalışan-hizmet uyumu; boş zaman gösterimi; çakışmasız bekleyen randevu; yönetici/personel onayı; hasta randevuları.
- **Kapsam dışı:** gerçek hastane entegrasyonu, e-Nabız/MHRS, ödeme alma, reçete, teşhis/veri dosyası, SMS, mobil uygulama ve mikroservisler.
- **OOP:** domain kalıtımı; kapsülleme; interface tabanlı kural zinciri; çok biçimli kural uygulaması; bağımlılıkları ters çevirme.
- **Test:** unit + servis entegrasyon + Postgres eşzamanlılık + MVC yetkilendirme + manuel sunum kabulü.
- **Ekip:** 1 veya 2 kişi; **en az 10 farklı günde gerçek commit**; GitHub ve SABİS teslimi; sunum zorunlu.

## 3. Dosya haritası
| Klasör | İçerik |
|---|---|
| `00-baslangic` | çalışma varsayımları, terimler, amaç, izleme |
| `01-urun` | FR/NFR, kapsam, kullanıcı hikâyeleri, kabul senaryoları |
| `02-mimari` | stack, paket mimarisi, OOP/UML, ADR, kalite nitelikleri |
| `03-domain` | iş kuralları, çakışma, durum geçişleri, yetki matrisi |
| `04-veri` | ER diyagramı, tablolar, migrasyon, seed |
| `05-arayuz-api` | ekran akışları, MVC endpoint sözleşmeleri, doğrulamalar |
| `06-kalite` | test planı, senaryolar, güvenlik, CI, manuel kabul |
| `07-sprintler` | S00–S10 görevleri, bağımlılıklar, sprint kabul kanıtları |
| `08-teslim` | gerçek commit takvimi, README/sunum/rapor şablonu |
| `09-ajana-talimat` | üretim seviyesi master prompt + sprint ve inceleme protokolü |
| `10-referanslar` | resmî kaynaklar, sürüm teyidi, kapsam değişiklikleri |

## 4. Öncelik kuralı
Çelişkide öncelik: **dersin öğretim elemanının güncel yönergesi** > `01-urun/01-gereksinim-matrisi.md` > `03-domain` kuralları > ADR > sprint planları > ajan promptu. Yeni bir gereksinim kod ile birlikte dokümanlara işlenmeden tamamlandı sayılmaz.

## 5. Geliştirme başlatma koşulu
[ADR kayıtlarını](02-mimari/04-mimari-kararlar.md) gözden geçir; `pom.xml` oluşturup `mvnw` ekle; `docs/` dizinini repo köküne kopyala. Sprint S00 ile başla, ileri sprint kodunu erkenden ekleme.
