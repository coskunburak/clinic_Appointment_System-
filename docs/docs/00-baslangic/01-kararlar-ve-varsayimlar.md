# Kararlar, varsayımlar ve belirsizlikler

## Onaylanan taslak kararlar
| ID | Karar | Gerekçe / durum |
|---|---|---|
| D-01 | Java 21 + Spring Boot 4.1.2 | Java OOP ile akademik uyum; sürümler uygulamaya geçerken tekrar doğrulanır |
| D-02 | Tek deploy edilebilir Spring MVC monoliti | Tek/iki geliştirici için düşük operasyon yükü |
| D-03 | Thymeleaf + Bootstrap | React/Node ekosistemi olmadan Java odaklı demo |
| D-04 | PostgreSQL + Flyway | kalıcı ilişkisel veri ve gerçek transaction testleri |
| D-05 | Çok şubeli veri modeli, tek uygulama | tek şubeyi destekler, aynı organizasyonda çok şube demo |
| D-06 | Onay manuel (`PENDING` → `CONFIRMED`) | ödevdeki onay mekanizmasını doğrudan gösterir |
| D-07 | Saat dilimi şubeye bağlı IANA zone | UTC veri + yerel görüntüleme; DST hatalarını önler |
| D-08 | Fiyat ve süre randevuya snapshot | katalog değişince geçmiş bozulmaz |
| D-09 | Yalnızca kurmaca demo verisi | hasta verisi hassasiyetini düşürür |
| D-10 | Bir kişi varsayılan; 2 kişi için görev bölüşümü | her ikisinde de sürdürülebilir |

## Öğrencinin isterse sonradan seçebileceği konular
- **Teslim tarihi:** bilinmiyor; 11 sprintin takvim karşılığı akademik takvime göre dağıtılmalı.
- **Ekip:** 1 mi 2 mi? Sprint dosyaları her iki durumu destekler.
- **Kullanıcı arayüzü:** varsayılan web arayüzüdür. Desktop JavaFX de yapılabilir, ancak bu paket web için hazırlanmıştır.
- **Dil:** ekranlar ve dokümanlar Türkçe; kod kimlikleri/commit mesajları İngilizce.
- **Randevu iptal penceresi:** MVP'de randevu başlangıcından önce hasta iptali; ek 24 saat kısıtı istenirse ADR gerektirir.
- **Çalışan türü:** doktor / hemşire / teknisyen. Her randevu bir yetkili çalışan ile ilişkilidir.
- **Hastane sayısı:** örnek seed 1 organizasyon, 2 şube; temel model farklı organizasyonları da ayırır.

## Açık sorular (geliştirmeyi durdurmaz)
1. Hocanın çalıştırılabilir web uygulaması dışında masaüstü/CLI beklentisi var mı?
2. Proje 1 veya 2 kişilik mi? Öğrencilerin GitHub hesapları hangileri?
3. Teslim/SABİS/sunum kesin tarihleri ne?
4. Üniversitenin Java sürümü veya rapor formatı zorunlu mu?

## Değişiklik yönetimi
Karar değişirse: (1) ADR kaydı, (2) FR/NFR etki analizi, (3) sprint görev ve test güncellemesi, (4) CHANGELOG girişi. Sessiz kapsam genişletme yapılmaz.
