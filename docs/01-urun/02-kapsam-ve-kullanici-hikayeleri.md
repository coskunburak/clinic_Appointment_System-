# Ürün kapsamı, MVP ve kullanıcı hikâyeleri

## MVP — sözleşmeye dahil
- Yönetici: organizasyon, 1+ şube, haftalık mesai, bölüm, hizmet, şube fiyat/süre, çalışan, uzmanlık, hizmet yetkinliği, uygunluk yönetimi.
- Hasta: kayıt/giriş, şube + bölüm/hizmet + çalışan + tarih sırasıyla boş zaman sorgulama, PENDING randevu alma, kendi listesi, iptal.
- Personel: kendi programını, PENDING talepleri görme; onay, ret, tamamla.
- Teknik: DB kalıcılığı, kuruma/şubeye erişim kontrolü, fiyat/süre snapshot, çift rezervasyonu engelleme, en az 10 farklı gün commit'i.

## MUST/SHOULD/COULD dışındaki gereksiz karmaşıklıklar
- Mobil istemci, REST-first ayrı frontend, Kafka, Kubernetes, Redis, mikroservis, ödeme, e-reçete ve gerçek hasta verisi **eklenmez**.
- Gerçek fiyatlandırma değişkenliği, kupon, sigorta, vergilendirme ve doktor performans raporları MVP dışında.
- Onay sonrası e-posta/SMS gerekli değil; arayüzde durumu görünmesi yeterli.

## Kullanıcı hikâyeleri (story → senaryo → kabul)
| ID | Hikâye | Kabul özeti |
|---|---|---|
| US-01 | Yönetici olarak şube ve çalışma saatleri tanımlamak isterim | negatif/çakışan gün saatleri reddedilir |
| US-02 | Yönetici olarak bölümler ve süre/fiyatlı işlemler tanımlamak isterim | süre pozitif, ücret ≥0, para birimi tanımlı |
| US-03 | Yönetici olarak çalışan ve uzmanlık eklemek isterim | çalışan şube ile eşleşir, hizmet yetkinliği doğrulanır |
| US-04 | Personel olarak haftalık ve izin saatlerimi görmek isterim | izindeki slotlar müsait görünmez |
| US-05 | Hasta olarak uygun doktordan işlem için saat seçmek isterim | boş aralıklar gerçek süreyi sığdırır |
| US-06 | Hasta olarak randevu talep etmek isterim | işlem+personel+şube+başlangıç+bitış+ücret saklanır |
| US-07 | Hasta olarak aynı slotu eşzamanlı denesem de doğru yanıt isterim | sadece bir istek başarılıdır |
| US-08 | Personel olarak talebi onaylamak/reddetmek isterim | yalnız atanmış personel veya yetkili yönetici yapar |
| US-09 | Hasta olarak kendi randevumu iptal etmek isterim | geçmişi koruyup slotu serbest bırakır |
| US-10 | Yönetici olarak iki farklı şubenin verilerini ayırmak isterim | çapraz şube erişimi 403/404 üretir |

## Uçtan uca mutlu yol
1. Kurum yöneticisi kurum ve iki şube tanımlar.
2. Şube A Kardiyoloji ve Muayene hizmeti ekler (30dk / 800.00 TRY).
3. Doktor atanır, hizmet verebilir, Pazartesi 09:00–12:00 uygundur.
4. Hasta 09:00–09:30 seçer: randevu **PENDING**, 800.00 TRY snapshot.
5. Aynı doktora 09:15–09:45 ikinci randevu **409 CONFLICT**.
6. Doktor 09:00 randevusunu **CONFIRMED** yapar.
7. Şubedeki hizmet ücreti 900.00 TRY olsa da randevunun 800.00 TRY kaydı korunur.
8. Yetkisiz başka hasta ve başka şube çalışanı randevuyu göremez.

## Negatif yol
- Şube kapalı, doktor izinli, hizmet yetkisi yok, talep geçmişte, 0dk süre, fiyat negatif, kimlik doğrulanmamış, farklı org, aynı slot eşzamanlı talep, ikinci onay, iptalden sonra onay: her biri belirli hata kodu veya 403/404 döndürür.
