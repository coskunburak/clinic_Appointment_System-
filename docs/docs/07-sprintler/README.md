# Master sprint yol haritası (S00–S10)

## Planlama varsayımı
Bu **11 sprint** süreleri 1/2 kişilik öğrenci işi için tahmini etkin çalışma günleridir; gerçek ders teslim tarihleri henüz bilinmiyor. Sprintler **küçük iterasyonlar** olarak tasarlanmıştır; 10 farklı gün commit şartı ile birebir gün eşleştirilmek zorunda değildir. Önce akademik MUST, sonra SHOULD, en son COULD.

| Sprint | Hedef | Önkoşul | Öngörülen etkin gün | Teslim çıktısı |
|---|---|---|---|---|
| [S00](./S00.md) | Kapsam dondurma ve GitHub hazırlığı | Öğretim elemanının Seçenek A metni; Java seçimi. | 1–2 gün | GitHub repo URL |
| [S01](./S01.md) | Java Bootstrapping ve veritabanı temeli | S00 scope onayı. | 2–3 gün | V1 migration |
| [S02](./S02.md) | Saf domain modeli ve OOP kanıtları | S01 build ve migration. | 2–3 gün | Java sınıfları + tests |
| [S03](./S03.md) | Kurum, şube, bölüm ve hizmet kataloğu | S02 Money, temel domain. | 3–4 gün | Organization/Catalog tests |
| [S04](./S04.md) | Kullanıcı, üç rol, güvenlik ve kaynak bazlı yetki | S03 kurum/şube kimlikleri ve S01 user_account migration. | 3–4 gün | Role matrix geçiş testleri |
| [S05](./S05.md) | Personel yönetimi, yetkinlik ve müsaitlik | S03 katalog; S04 identity ve yetki. | 3–4 gün | Staff/availability testleri |
| [S06](./S06.md) | Randevu transaction, snapshot ve eşzamanlılık | S03 katalog + S04 auth + S05 availability; V3 migration. | 4–5 gün | BookingServiceTest |
| [S07](./S07.md) | Onay/ret/iptal/tamamla ve audit history | S06 booking ve S04 AccessPolicy. | 2–3 gün | StateTest |
| [S08](./S08.md) | Üç rol için tamamlanmış web arayüzü | S03–S07 tamamlanmış business flow. | 4–5 gün | 3 rol ekran görüntüleri |
| [S09](./S09.md) | Regression, hardening ve tekrarlanabilir demo | S08 kullanıcı akışı tamamlanmış. | 3–4 gün | UAT checklist |
| [S10](./S10.md) | Rapor, GitHub/SABİS ve sınıf içi sunum | S09 UAT temiz; 10 farklı gerçek gün ilerlemesi tamamlanmış olmalı. | 2–3 gün (+ son 4 haftadaki sunum tarihi) | git log date check |

## Critical path
`S00 → S01 → S02 → S03 + S04 → S05 → S06 → S07 → S08 → S09 → S10`. S03 ve S04 bazı işleri paralel yürütebilir; S06 öncesi katalog + kullanıcı + personel uygunluğu tamamlanmalıdır.

## MVP kapısı
S08 sonunda hasta isteği + personel onayı + admin kurum yönetimi gösterilebilir olmalı. S09 kalite/regression, S10 teslim. **S06 bitmeden “randevu çakışması çözüldü” denemez**; concurrency integration test kanıtı gerekir.

## Her sprint sonu protokol
1. İlgili FR ve TC listesini kontrol et.
2. Kod + gerekli migration + test + doküman güncellemelerini hazırla.
3. Komutları öğrenci manuel çalıştırır; hata çıktısı geri gelir.
4. Ancak test kanıtı sonrası `Done` işaretlenir, gerçek gün commit/push yapılır.
5. Sonraki sprint bağımlılığı tamamlanmadan geçiş yok.

## Sprint review özeti şablonu
```text
Sprint: S__
Branch / HEAD:
Changes (classes, paths):
Covered FR/TC:
Manual commands supplied:
Owner-reported test results (PASS/FAIL/NOT_RUN):
Migrations / security effect:
Known risks / TODO:
Commit date and GitHub link:
Next sprint gate:
```
