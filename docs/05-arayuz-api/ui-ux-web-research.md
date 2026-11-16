# Web arayüz araştırması — Sessiz Klinik

Erişim tarihi: **16 Kasım 2026**. Kapsam: mevcut Spring MVC/Thymeleaf web uygulaması. Bu çalışma bir kullanılabilirlik deneyi veya referans ürünlerin erişilebilirlik sertifikasyonu değildir.

## Kaynaklar ve gözlem sınırları

- [MHRS kamu sitesi](https://www.mhrs.gov.tr/): doğrudan erişim 502 verdi. Arama dizinindeki resmi sayfa özeti giriş, randevu oluşturma, geçmiş ve iptal girişlerini doğruluyor; ana sayfanın canlı görünümü incelenemedi. Tanıtım sitesi ile oturum gerektiren randevu uygulaması farklı yüzeylerdir.
- [Araç Devlet Hastanesi — Nasıl Randevu Alırım?](https://aracdh.saglik.gov.tr/TR-1535782/nasil-randevu-alirim.html): resmi web yönergesi hesapla giriş, hastane/hekim seçimi, randevu geçmişi ve kayıt yanındaki iptal eylemini açıklıyor. Hastane/bölüm/tarih/saat seçicilerinin güncel görsel tasarımı, uygunluk ve hata durumları oturum içinde doğrulanmadı. MHRS'nin onay ve iptal politikaları bu projeye aktarılmayacak.
- [e-Nabız giriş](https://www.enabiz.gov.tr/) ve [resmi yardım](https://enabiz.gov.tr/yardim/): kamu giriş alanları ve yardım metni incelendi. Yardım; kategorili gezinme, son muayeneler, takvimde geçmiş/randevu bilgisi, ayrıntı erişimi ve MHRS web yönlendirmesini belgeliyor. Canlı kimlik doğrulanmış ekranlar incelenmedi; kılavuz görsellerinin güncel sürümü temsil ettiği varsayılmadı.
- [NHS bileşenleri](https://service-manual.nhs.uk/design-system/components), [örüntüler](https://service-manual.nhs.uk/design-system/patterns): başlık, gezinme, etiketli alanlar, ipucu, tarih, bildirim, özet, tablo ve onay desenleri incelendi. [Hata özeti](https://service-manual.nhs.uk/design-system/components/error-summary) alanlara bağlantı veren sayfa başı hata listesini; [tablo](https://service-manual.nhs.uk/design-system/components/table) başlık/caption ilişkisini; [durum etiketi](https://service-manual.nhs.uk/design-system/components/tag) durumun eylemden ayrılmasını destekliyor. NHS kütüphanesi veya görünümü aktarılmayacak.
- [WCAG 2.2](https://www.w3.org/TR/WCAG22/): 1.3.1 ilişkiler, 1.4.3 metin kontrastı, 1.4.10 yeniden akış, 1.4.11 kontrol kontrastı, 2.1.1 klavye, 2.4.7 görünür odak, 2.4.11 odağın örtülmemesi, 2.5.8 hedef boyutu, 3.3.1/2 hatalar ve etiketler, 4.1.3 durum mesajları değerlendirme ölçütü olarak seçildi.

## Karşılaştırma ve kararlar

| Boyut | MHRS Web | e-Nabız Web | NHS Web | Klinik sistemi kararı |
|---|---|---|---|---|
| Gezinme | Kamu girişinden randevu alanına geçiş belgeli | Yardımda kayıt kategorileri belgeli | Header, breadcrumb, skip link | Rol menülerini masaüstünde sabit sol rayda, dar tarayıcıda açılır üst menüde sun |
| Randevu | Hastane/hekim, geçmiş ve iptal belgeli; canlı seçimler erişilemedi | MHRS'ye yönlendirme belgeli | Yanıt kontrolü ve onay örüntüleri | Mevcut şube → hizmet → hekim → tarih/saat; aynı sayfada özet, yeni adım yok |
| Hiyerarşi | Canlı masaüstü hiyerarşisi doğrulanmadı | Son kayıtlar ve detaylar kılavuzda | Başlıklar ve görev odaklı sayfalar | Kısa bağlam, tek h1, başlık yanında mevcut eylem |
| Form | Kimlik bilgileriyle giriş belgeli | Kamu giriş alanları gözlendi | Etiket, ipucu, fieldset, hata özeti | Native alanlar, gerçek zorunluluk, hata bağlantıları |
| Tablo | Geçmiş kaydı/iptal belgeli; sütunlar bilinmiyor | Tarihli erişim/geçmiş listeleri belgeli | Caption ve sütun başlıkları | Başlıklandırılmış tablolar, sayfa yerine yerel yatay kaydırma |
| Durum | Güncel renk/etiket sistemi bilinmiyor | Canlı durum tasarımı doğrulanmadı | Etiket durum bildirir, buton değildir | Türkçe mevcut durum metni + düşük doygunluklu semantik zemin |
| Hata | Kimlik doğrulanmış hata ekranı incelenmedi | Canlı kayıt hataları incelenmedi | Alan hatası ve sayfa özeti | Mevcut doğrulama mesajlarını alana bağla; güvenli hata sayfaları |
| Erişilebilirlik | Uygunluk iddiası yok | Uygunluk iddiası yok | Açık erişilebilir bileşen rehberi | WCAG AA hedefi; gerçek tarayıcı/yardımcı teknoloji kontrolü ayrıca gerekli |

## Mevcut uygulama bulguları

Yerel kaynaklar `579179539790cb254722aee2360adb0ac6c5fb33` üzerinden incelendi. 18 HTML şablonu var. Bootstrap 5.3.3 yerel, CSS tek dosya, JavaScript vanilla. Geniş yatay yönetici menüsü; tekrar eden renkli metrik kartları; büyük yuvarlak köşeler/gölgeler; eksik `.badge-status` görsel tanımı; 12 px büyük harf tablo başlıkları; global yatay taşma gizleme; radyo etiketinde açık odak eksikliği; hatalarla alanlar arasında eksik ARIA bağlantıları görüldü. Bazı GET seçimlerinde inline `onchange` var. Personel dashboard controller'ı takvime yönlendiriyor; hasta dashboard'u gerçekten render ediliyor (eski endpoint belgesinin yönlendirme açıklaması güncel kaynakla uyuşmuyor).

## Ekran önerileri ve uygulanacak yön

**Sessiz Klinik:** koyu mürekkep metin, petrol yeşili eylem, beyaz çalışma yüzeyi ve açık gri-yeşil sayfa. Sistem fontu, 4 px ritim, 6–8 px köşe, gölgesiz paneller. Görseller ve dekoratif grafikler yok.

| Ekran | Öneri |
|---|---|
| Ortak kabuk | Sakin sol gezinme, görünür oturum bilgisi, içerik atlama, mevcut aktif menü koşulları |
| Hasta paneli | Yaklaşan randevulara güçlü yer, tek ana randevu çağrısı, ikincil geçmiş bağlantısı |
| Randevu al | İki anlamlı alan grubu ve bağımsız özet sütunu; saat seçiminde sınır/işaret/odak |
| Randevularım | Aktif ve geçmiş kayıtlar ayrı tablo; uygun iptal eylemi ve sayfalama |
| Personel | Kompakt sayaç şeridi, görünüm sekmeleri, zaman sütunu güçlü ajanda; yedi gün grubu korunur |
| Yönetici özet | Metrik şeridi ve satır düzeninde modül dizini |
| Şube/bölüm | Liste yanında ölçülü kayıt formu, filtre ayrı araç çubuğu |
| Çalışma saatleri | Gün ve saat hizalı tablo, ayrı düzenleme alanı |
| Hizmetler | Şube fiyat/süre tablosu ile kurumsal tanımlar açık bölüm başlıklarıyla ayrılır |
| Personel yönetimi | Liste ardından kayıt, atama ve yetkinlik formları; mevcut bağımlı filtreler korunur |
| Uygunluk | Haftalık mesai ve tarih bazlı izinler ayrı çalışma bölümleri |
| Giriş | Tipografik kimlik ve sade kimlik bilgisi formu; yeni giriş yöntemi yok |
| Hatalar | Kompakt hata kodu, güvenli açıklama ve mevcut ana sayfa/giriş bağlantıları |

## Riskler ve reddedilen aktarımlar

Referans markaları, e-Devlet entegrasyonu, sağlık kayıtları, yeni onay/iptal kuralları, takvim kütüphanesi, SPA, mobil uygulama, PWA, CDN, yeni backend verisi ve sahte metrikler kapsam dışı. Thymeleaf koşulları, form alanları ve modal ID'leri görünüm değişikliğinde riskli; başlangıç envanteriyle karşılaştırılacak. Gerçek Java/Thymeleaf render ve tarayıcı QA olmadan sıfır regresyon veya WCAG uygunluğu ilan edilmeyecek. Proje yönetimi uygulama başlatma ve testleri sahibine bırakıyor.
