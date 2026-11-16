# Web yeniden tasarım — uygulama ve doğrulama raporu

**Tarih:** 16 Kasım 2026 · **Durum:** IMPLEMENTED — VERIFICATION PENDING

## A. Yönetici özeti

Sessiz Klinik tasarımı, mevcut Spring MVC/Thymeleaf web arayüzünün tamamına uygulandı. Ortak gezinme kabuğu, hasta, personel, yönetim, giriş ve dört hata ekranı kapsandı. Masaüstünde sol gezinme, sakin yüzeyler, anlamlı form bölümleri ve bilgi odaklı tablolar tercih edildi. Yeni iş özelliği veya frontend platformu eklenmedi.

Başlangıç: `main`, `579179539790cb254722aee2360adb0ac6c5fb33`, temiz çalışma ağacı. Java 21, Spring Boot 3.4.2, Bootstrap 5.3.3 yerel dosyaları ve PostgreSQL 17 tanımı kaynaklardan teyit edildi. Maven bağımlılıkları çözülmedi; Thymeleaf/Security'nin minor sürümleri için runtime iddiası yok. GitHub kamu deposu incelendi; remote HEAD eşitliği fetch ile kontrol edilmedi. S10'un 501 PASS sonucu tarihsel kanıttır.

## B. Araştırma

[Web araştırması](ui-ux-web-research.md), doğrulanmış kamu bilgilerini ve erişim sınırlarını ayırır. MHRS kamu sayfasının doğrudan isteği 502 verdi; resmi hastane yönergeleri web giriş, hastane/hekim seçimi, geçmiş ve iptali doğruladı. e-Nabız yardımının kategoriler ve son kayıtlar yaklaşımı; NHS'nin etiketli alan, hata özeti, tablo ve durum bileşenleri incelendi. Kimlik doğrulanmış MHRS/e-Nabız ekranları görülmüş gibi raporlanmadı. Markalar, kimlik sağlayıcıları ve iş kuralları kopyalanmadı.

## C. Tasarım sistemi

[Tasarım sistemi](ui-design-system.md): petrol yeşili ana eylem, mürekkep metin, beyaz yüzey, gri-yeşil sayfa; yerel sistem fontu; 32/22/18/16/14/13 px ölçek; 4 px aralık ritmi; küçük köşeler, gölgesiz çalışma alanları. 1200 px ve üstünde 232 px gezinme rayı; altında Bootstrap üst menüsü. Renkli KPI kartları yerine metrik şeridi ve yönetim bağlantı satırları var. Özet yalnız geniş/yüksek ekranda yapışkan; kısa/dar ekranda normal akışta.

## D–E. Ekranlar ve değişen dosyalar

Yollar depo köküne göredir. Toplam **20 uygulama dosyası + 4 yeni belge**.

| Dosya | Uygulanan değişiklik |
|---|---|
| `src/main/resources/static/css/clinic.css` | Tasarım tokenları, gezinme, tablolar, formlar, semantik durumlar, radyo odağı, duyarlı düzen, azaltılmış hareket |
| `src/main/resources/static/js/clinic.js` | Üç eski inline GET gönderimini dış dinleyiciye taşıma; hata özeti/alan odağı. Mevcut işlev gövdeleri aynı |
| `src/main/resources/templates/fragments/layout.html` | Sol gezinme ve üst bağlam, görünür oturum, aria-current, içerik atlama; teknik footer metninin sadeleşmesi |
| `src/main/resources/templates/patient/dashboard.html` | Birincil randevu eylemi ve ikincil geçmiş alanının farklılaşması; yaklaşan kayıt tablosu |
| `src/main/resources/templates/patient/book.html` | İki fieldset, saat seçimi, ayrı özet, hata bağlantıları, mevcut talebin doğru adlandırılması |
| `src/main/resources/templates/patient/appointments.html` | Aktif/geçmiş tablo başlıkları, erişilebilir kaydırma ve sayfalama, iptal modalı |
| `src/main/resources/templates/staff/dashboard.html` | Mevcut takvime geçiş için tutarlı yedek ekran; controller yönlendirmesi aynı |
| `src/main/resources/templates/staff/schedule.html` | Sade metrik şeridi, görünüm/tarih araçları, kronolojik vurgu, günlük/haftalık/bekleyen tablolar, ret modalı |
| `src/main/resources/templates/admin/dashboard.html` | Dört gerçek sayacın şeridi; kart ızgarası yerine yönetim dizini |
| `src/main/resources/templates/admin/branches.html` | Kurum ve şube tabloları, ayrı kayıt paneli, modal ve hata sunumu |
| `src/main/resources/templates/admin/branch-hours.html` | Şube seçimi araç çubuğu, gün/saat tablosu, mesai formu |
| `src/main/resources/templates/admin/departments.html` | Şube filtresi, bölüm tablosu, ayrı kayıt paneli |
| `src/main/resources/templates/admin/services.html` | Şube ve kurum kataloğunun ayrılması, süre/ücret kontrolleri, atama ve tanım formları |
| `src/main/resources/templates/admin/staff.html` | Personel tablosu, kayıt/atama/yetkinlik bölümleri, Türkçe rol adları |
| `src/main/resources/templates/admin/staff-availability.html` | Haftalık çalışma ve tarih bazlı istisna ayrımı, Türkçe izin türleri, ayrı formlar |
| `src/main/resources/templates/auth/login.html` | İki sütunlu kimlik/giriş düzeni, mevcut mesajlar ve credential formu |
| `src/main/resources/templates/error/400.html` | Kompakt hata kimliği ve mevcut güvenli dönüşler |
| `src/main/resources/templates/error/403.html` | Aynı hata sistemi; erişim reddi mesajı korunur |
| `src/main/resources/templates/error/404.html` | Aynı hata sistemi; bulunamayan kaynak mesajı korunur |
| `src/main/resources/templates/error/500.html` | Aynı hata sistemi; teknik detay açığa çıkarılmaz |
| `docs/05-arayuz-api/ui-ux-web-research.md` | Resmi kaynaklar, karşılaştırma, sınırlar ve ekran önerileri |
| `docs/05-arayuz-api/ui-design-system.md` | Tokenlar, bileşenler, yerleşim ve kabul hedefleri |
| `docs/05-arayuz-api/ui-contract-inventory.md` | Başlangıç form, alan, ID, rota ve JS bağımlılık envanteri |
| `docs/05-arayuz-api/ui-redesign-report.md` | Bu rapor, statik kanıtlar ve sahibin kabul matrisi |

## F. Korunan sözleşmeler

[Başlangıç envanteri](ui-contract-inventory.md) ile HEAD karşılaştırması yapıldı. 18 şablondaki 626 sözleşme taşıyan öğe karşılaştırıldı: **623 öğenin korunan nitelikleri birebir aynı**; üç `th:text` ifadesi yalnız personel türü ve izin türünü Türkçeleştirir. `StaffProfileResponse.staffType` String ve `AvailabilityExceptionResponse.kind` enum kaynakları kontrol edildi; seçenek `value` değerleri ve form payload'ları değiştirilmedi.

39 formun action/method/th:object ve kontrol ID/name/type/required/min/max/length/step/autocomplete/binding envanterleri aynı. 203 form/kontrol için `th:if`, `th:unless`, `th:each`, `th:object` üst koşul zincirleri aynı. CSRF alanları ve navbar rol koşulları korundu. Modal tetikleyicileri, hedefleri, dinamik veri öznitelikleri, görünür işlem koşulları ve pagination parametreleri korundu.

`addMinutesToTimeString`, `initPatientBookingWizard`, `initPatientCancellationModal`, `initStaffRejectModal`, `initAdminModalsAndFilters` kaynak gövdeleri HEAD ile birebir aynı. Slot üretimi, tarih/ücret hesapları, fetch davranışı, modal action üretimi ve filtre kuralları değiştirilmedi. `scheduleDateInput`, `filterBranchId`, `serviceBranchFilter` için aynı `control.form.submit()` çağrısı yeni dış dinleyiciye taşındı. Bu, inline event handler bırakmadan mevcut GET davranışını korur. Hata özeti ve bağlantıları yalnız odağı yönetir.

Backend, DTO, migration, güvenlik, Maven, CI ve mevcut test dosyalarında değişiklik yok. Commit/push veya stage yapılmadı. Geçici analiz dosyaları depo dışında tutuldu.

## G. Erişilebilirlik ve statik kalite

Yapılanlar: dengeli HTML etiketleri, yinelenen statik ID kontrolü, görünür label/for ilişkilerinin korunması, 14 tablo caption'ı, 69 scope=col başlığı; alan-hata bağlantıları, aria-invalid, hata özetinden alana odak, skip link, aktif gezinme, radyo etiketinde odak, renge ek seçili işareti, metinli durumlar, azaltılmış hareket, küçük butonlarda 36 px ve normalde 44 px yükseklik.

Hesap yöntemi: sRGB doğrusal bağıl parlaklık, `(Lmax + 0.05) / (Lmin + 0.05)`. Bunlar CSS token çiftlerinin matematiksel oranlarıdır; render edilmiş tüm piksel/bileşen durumlarının testi değildir.

| Çift | Oran |
|---|---:|
| Mürekkep / beyaz | 13.00:1 |
| İkincil metin / beyaz | 6.02:1 |
| Ana eylem / beyaz | 8.15:1 |
| Hover / beyaz | 9.25:1 |
| Başarı / açık başarı zemini | 5.84:1 |
| Uyarı / açık uyarı zemini | 5.61:1 |
| Hata / açık hata zemini | 5.47:1 |
| Nötr durum / açık zemin | 5.24:1 |
| Kontrol sınırı / beyaz | 3.64:1 |
| Ana eylem / yumuşak yüzey | 7.09:1 |
| İkincil metin / sayfa | 5.64:1 |

`node --check` JavaScript sözdizim denetimi başarılı. Mevcut ortamın tinycss2 ayrıştırıcısı CSS sözdizim hatası bulmadı. HTML kaynak ayrıştırmasında açık kalan/dengesiz etiket, nested form veya yeni yinelenen statik ID bulunmadı. `git diff --check` temiz. Özel CSS'de gradient/backdrop-filter yok; şablonlarda inline event/style kalmadı. Bunlar statik inceleme sonuçlarıdır, Maven veya tarayıcı test sonucu değildir.

## H. Gerçek görsel inceleme durumu

Uygulama veya Docker başlatılmadı. Port 8080'de zaten dinleyen bir Java süreci görüldü. In-app browser bağlantısı zaman aşımına uğradı. Mevcut Chrome üzerinden `http://localhost:8080/login` açıldığında klinik ekranı yerine `401 / AUTH_TOKEN_EXPIRED` JSON yanıtı görüldü; servisin bu checkout'u sunduğu doğrulanamadı. Servis, oturum ve yapılandırma değiştirilmedi. Klinik sayfalarının ekran görüntüsü veya başarılı render kanıtı **yoktur**.

| Hedef | Kodda hazırlanan düzen | Gerçek inceleme |
|---|---|---|
| 1280×800 | Sol ray, geniş tablolar, kısa ekranda akış içi özet | Bekliyor |
| 1440×900 | Sol ray, iki sütun booking, uygun yükseklikte sticky özet | Bekliyor |
| 1920×1080 | Maksimum içerik genişliği, okunabilir satır uzunluğu | Bekliyor |
| 768 / 1024 px | Üst menü, yönetimde tek sütun, iç tablo kaydırma | Bekliyor |
| 360 / 390 px | Tek sütun, normal akış, yerel tablo kaydırma | Bekliyor |
| %200 / %400 zoom | CSS reflow ve menü breakpoint hazırlığı | Bekliyor |
| Chrome / Edge / Firefox / Safari | Standart Grid/Flex/Bootstrap | Klinik ekranları bekliyor |
| Klavye / ekran okuyucu | Semantik ve odak hazırlığı | Canlı davranış bekliyor |

## I. Regresyon kabul matrisi

Aşağıdaki tüm satırlar **runtime: NOT_RUN / AWAITING_OWNER_TEST**. Statik sütun yalnız kaynak/sözleşme incelemesini gösterir. Sentetik `dev` gösterim verisi kullanılmalı; gerçek hasta verisi kullanılmamalı.

| Alan | Sahibin doğrulayacağı senaryo | Statik durum |
|---|---|---|
| Kimlik | Başarılı giriş, geçersiz giriş, çıkış, role yönlendirme, erişim reddi | Form/action/security kaynakları korundu |
| Hasta paneli | Yaklaşan kayıtlar, boş panel, mevcut eylemler | Model koşulları aynı |
| Booking | Şube seçince hizmetin GET ile yenilenmesi | ID/data/URL aynı |
| Booking | Hizmet seçince yetkin hekim listesinin yenilenmesi | Filtre ve URL aynı |
| Booking | Hekim/tarih seçince uygunluk JSON alınması | Fetch ve query aynı |
| Booking | Saat seçimi, özetin tüm alanları, talep gönderimi | Radio/name/summary/submission aynı |
| Booking | Hatalı alan, çakışma, boş uygunluk, fetch hatası, yükleme | Eski davranış aynı; ARIA ve görünüm yeni |
| Hasta kayıtları | Aktif/geçmiş tablo, sayfalama, query korunması | each/if/href aynı |
| İptal | Uygunluk koşulu, modal aç/kapat, gerekçe, submit, CSRF | Aynı |
| Personel | Günlük, haftalık yedi grup, bekleyen görünüm | date/view ve model aynı |
| Personel | Önceki/sonraki tarih, bugün, tarih alanı | GET aynı; inline dinleyici dışarıda |
| Personel | Onayla, gerekçeli reddet, tamamla, durum sonrası dönüş | canApprove/canComplete/action aynı |
| Yönetici | Gerçek sayaçlar, modüllere erişim | Model ve linkler aynı |
| Kurum | Kurum adı düzenleme, şube yönetici kısıtı | Koşullar/CSRF/modal aynı |
| Şube | Oluştur/düzenle/aktifleştir/pasife al | Aynı |
| Çalışma saati | Şube geçişi, ekle/güncelle/kaldır, hatalı aralık | Alanlar ve server validation aynı |
| Bölüm | Şube filtresi, oluştur/düzenle/durum | Aynı GET/POST ve gizli branchId |
| Hizmet | Tanım oluştur/düzenle/durum, şubeye ata | Yetki koşulları aynı |
| Hizmet | Süre/ücret güncelle, şube filtresi, negatif değer hatası | Numeric kısıtlar ve payload aynı |
| Personel yönetimi | Kayıt, tür/uzmanlık gerekliliği, şube/bölüm ataması | JS filtreleri aynı; görünür rol çevirisi yeni |
| Yetkinlik | Görevlendirmeye göre hizmet filtreleme, gizli checkbox devre dışı | Aynı |
| Uygunluk | Görevlendirme seçimi, haftalık aralık ekle/sil | Aynı |
| İstisna | Tam/kısmi izin, fazla mesai, saat alanı gerekliliği, silme | Aynı; görünür tür çevirisi yeni |
| Ortak | Rol menüleri, bütün modallar, flash kapatma, geri/ileri | Bağlantı/koşullar aynı |
| Hatalar | 400/403/404/500 ve mevcut dönüşler | HTTP backend ve güvenli metinler aynı |
| Erişilebilirlik | Tab/Shift+Tab, Escape, modal odak dönüşü, radio ok tuşları | Gerçek tarayıcıda kontrol edilmeli |
| Erişilebilirlik | Hata özeti odağı, alan linki, ekran okuyucu duyurusu | Yeni sunum davranışı; runtime gerekli |
| Duyarlılık | Her hedefte boş/tek/çok kayıt, uzun Türkçe metin, modal, disabled/selected | CSS hazır; görsel kanıt gerekli |

S10: **366 Surefire + 135 Failsafe = 501 PASS** geçmiş durumdur. Bu redesign için Maven testleri, build, migrasyon, Docker doğrulaması veya uygulama başlangıcı çalıştırılmadı. Yeni test sonucu üretilmedi, mevcut testler değiştirilmedi.

## J. Test devri

[Depo çalışma yönergesi](../09-ajana-talimat/01-production-master-prompt.md) testleri sahibine bırakır: “DO NOT run Maven tests, CI, scripts that execute tests, application startup or Docker validation automatically.”

Depo kökünde sahibi çalıştırır:

```bash
./mvnw clean verify
```

Başarılı çıktıdan sonra bile H–I matrisindeki gerçek tarayıcı ve klavye kabulü ayrıca gereklidir. Yeni font, Node build gereksinimi, CDN veya frontend bağımlılığı eklenmedi.

## K. Kalan riskler

1. Canlı Thymeleaf render ve sunucu doğrulama hata ekranları gözlenmedi. Statik parser Spring/Thymeleaf ifadelerini çalıştırmaz.
2. Tüm viewport ve tarayıcı kombinasyonları, uzun kayıtlar, modal kaydırma/odak ve ekran okuyucu testleri bekliyor. WCAG 2.2 AA uygunluğu ilan edilmez.
3. Eski günlük personel tablosundaki `btn-approve-appointment` ID'si tekrarlı `th:each` içinde aynı kalır. Mevcut test/seçici sözleşmesini bozmamak için yeniden adlandırılmadı; statik ID tekilliği runtime tekrarını kanıtlamaz.
4. Mevcut JS'nin slot fetch sıralaması, sunucu yönlendirmeleri ve doğrulama politikaları bu sunum işinde yeniden tasarlanmadı; görünüm denetimi bunların doğruluğunu kanıtlamaz.
5. Tarihsel ekran/endpoint belgelerindeki hasta dashboard yönlendirme notu güncel kaynakla çelişiyor; gerçek controller/template esas alındı, sprint tarihçesi değiştirilmedi.

## L. Sonuç

**IMPLEMENTED — VERIFICATION PENDING.** Uygulama değişiklikleri sahibin doğrulaması için hazır. Sıfır fonksiyonel regresyon ve üretim doğrulaması henüz kanıtlanmış değildir. Commit veya yayın yapılmadı.
