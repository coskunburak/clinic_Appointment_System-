# Sessiz Klinik — web tasarım sistemi

16 Kasım 2026 · yalnız sunum katmanı · araştırma: [Web araştırması](ui-ux-web-research.md)

## Temel kararlar

- Masaüstü kabuk: 232 px sol gezinme, içerik için en fazla 1560 px; 1200 px altında üstte açılır Bootstrap gezinmesi. Menü rol koşullarına bağlıdır. Hasta, personel ve yönetici aynı kabuğu kullanır.
- Sayfa başı: küçük alan adı, 30–32 px h1, açıklama ve mevcut birincil eylem. İçerik bölümleri 18–22 px h2. İşlem açıklamaları 65 karakter civarı satır genişliğinde.
- Liste ve düzenleme: ana tablolar geniş sütunda; form bölümü en az okunabilir kontrol genişliğinde. 1200 px altında yönetim sütunları üst üste. Personel üç form düzeni yalnız yeterli genişlikte yan yana.
- Metrikler: tek sınırlandırılmış şerit ve ayırıcılar. Yönetim modülleri satır dizini; dekoratif istatistik/grafik yok.

## Tokenlar

| Amaç | Değer |
|---|---|
| Mürekkep | `#20343B` |
| Ana eylem | `#20575B` |
| Eylem hover/active | `#174E59` / `#123E47` |
| Sayfa / yüzey / yumuşak yüzey | `#F6F8F7` / `#FFFFFF` / `#E8F1F0` |
| İkincil metin | `#54666B` |
| Ayırıcı / kontrol sınırı | `#DFE7E4` / `#778A88` |
| Başarı / uyarı / hata | `#24694A` / `#85551A` / `#A33D39` |
| Odak | `#174E59`, 3 px outline + 3 px offset |
| Font | system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, sans-serif |
| Metin ölçeği | 32 / 22 / 18 / 16 / 14 / 13 px; sayılar tabular-nums |
| Aralıklar | 4 / 8 / 12 / 16 / 24 / 32 / 40 / 48 px |
| Köşe | Kontrol 6 px; panel 8 px; modal 10 px |

Renk çiftlerinin hesaplanan kontrastları ve doğrulama sınırları [uygulama raporunda](ui-redesign-report.md) tutulur. Açık ayırıcı rengi alan kontrolü sınırı olarak kullanılmaz.

## Bileşen kuralları

- Butonlar: petrol ana işlem, beyaz ikincil işlem, kırmızı yıkıcı işlem. Küçük butonlarda en az 36 px yükseklik; normalde 44 px. Hover, active, focus ve disabled ayrı. Durum etiketleri buton görünümü taşımaz.
- Formlar: görünür label, mevcut zorunluluk ve limitler; hata metni `aria-describedby` ile bağlı, `aria-invalid` mevcut BindingResult'tan. Yeni zorunluluk veya validation kuralı yok.
- Randevu: şube/hizmet/hekim ve tarih/saat iki fieldset; özet aynı form içinde. Radyo seçimi dolu zemin, iç sınır ve işaretle belirtilir; klavye odağı label üzerinde görünür. Yükleme/sonuç alanı canlı bildirim sağlar. Kısa ekranlarda özet sticky değildir.
- Tablo: küçük harfli okunabilir sütun başlıkları, `scope=col`, açıklayıcı caption, sabit sayısal hizalama. İç kaydırıcı klavyeyle erişilebilir; sayfanın taşması gizlenmez.
- Modal: Bootstrap focus trap, Escape ve kapatma davranışı korunur. Başlık, gerekçe ve geri dönüş/ana işlem ayrılır.
- Hata/boş durum: teknik veri veya uydurma kayıt yok. Mevcut mesaj ve koşullarla sade açıklama. Form hata özeti alanlara götürür.
- Hareket: animasyon eklenmez, `prefers-reduced-motion` altında mevcut geçişler azaltılır.

## Duyarlı düzen ve kabul kapısı

1280×800, 1440×900, 1920×1080 masaüstü; 768/1024 tablet; 360/390 dar tarayıcı hedefleri. Sol menü 1200 px altında standart üst menüye dönüşür; alt navigasyon yok. Tablo kendi içinde kayar, form ve özet tek sütuna iner. %200/%400 yakınlaştırma, klavye, ekran okuyucu ve Chrome/Edge/Firefox/Safari çalıştırma kanıtı gereklidir. Bu belge hedef belirtir; tamamlanmış tarayıcı doğrulaması değildir.
