# Son sürüm manuel kabul / demonstrasyon kontrol listesi

> Her kutu geliştirici tarafından gerçek uygulamada doğrulandıktan sonra işaretlenir. Bu dokümanda kutular bilinçli olarak boş bırakılmıştır.

## Kurulum
- [ ] Temiz clone açılıyor, `./mvnw verify` çalışıyor.
- [ ] PostgreSQL ayağa kalkıyor, Flyway migrate başarıyla tamamlanıyor.
- [ ] Seed yalnız demo modunda kurmaca kullanıcıları oluşturuyor.
- [ ] README doğrulanmış komut ve ekran görüntüsü içeriyor.

## Yönetici
- [ ] Kurum ve iki şube tanımlanabiliyor.
- [ ] Şube çalışma saati ve bölüm oluşturulabiliyor.
- [ ] Hizmete süre/ücret tanımlanabiliyor.
- [ ] Doktor/sağlık personeli, uzmanlık ve şube ataması yapılabiliyor.
- [ ] Personelin servis yetkisi, haftalık saat ve izin aralıkları eklenebiliyor.

## Hasta
- [ ] Giriş → şube → hizmet → çalışan → tarih → slot tamamlanıyor.
- [ ] PENDING randevu işlem, dakika, ücret, personel, tarih ve saat ile listeleniyor.
- [ ] Çakışma 409/uygun UI mesajı ile engelleniyor.
- [ ] Aynı saatte paralel iki istek tek kayıtta sonuçlanıyor.
- [ ] Hasta kendi randevusunu iptal edebiliyor, başkasını göremiyor.

## Personel
- [ ] Yalnız atanmış randevular görünüyor.
- [ ] PENDING → CONFIRMED ve PENDING → REJECTED çalışıyor.
- [ ] CONFIRMED → COMPLETED yalnız bitiş sonrası çalışıyor.
- [ ] Başka şube/başka personel randevu onayı reddediliyor.

## Sunum / teslim
- [ ] OOP kalıtım, encapsulation, interface polymorphism canlı koddan gösterilebiliyor.
- [ ] Maven test çıktısı ve concurrency integration kanıtı saklandı.
- [ ] 10 farklı **gerçek** gün commit şartı karşılanıyor.
- [ ] GitHub linki, SABİS yüklemesi ve sunum katılımı tamamlandı.
