# Proje künyesi, amaç ve başarı ölçütleri

**Örnek ürün adı:** KlinikRandevu. Amaç: randevu alma, onaylama, personel ve kurum bilgisi düzenleme işlevlerini nesneye dayalı tasarımla güvenli ve gösterilebilir biçimde sunmak.

## Kullanıcılar
- **Hasta:** hizmet/doktor/şube/saat seçer, başvurur, randevularını izler, iptal eder.
- **Doktor/sağlık personeli:** kendi programını görür, yetkili olduğu randevuları onaylar veya reddeder, işleneni tamamlar.
- **Şube yöneticisi:** kurum, şube, bölüm, saat, hizmet, personel, hizmet yetkinliği ve randevuları yönetir. Kurum yöneticisi şube sınırları içinde veya organizasyon genelinde yetkilendirilebilir.

## Ölçülebilir çıkış hedefleri
1. FR matrisindeki **MUST** kriterlerinin tamamı test veya demo ile doğrulanır.
2. Aynı çalışana çakışan iki aktif randevu eklenemez; iki eşzamanlı istekte en çok biri başarıyla sonuçlanır.
3. Üç rolün izinleri ve 403/redirect davranışı uçtan uca gözlenebilir.
4. Yeni kurum/şube/saat/hizmet/çalışan ve randevu akışları sıfırdan demolanabilir.
5. 10 **ayrı gerçek takvim günü** üzerinde en az 10 anlamlı GitHub commit'i mevcut olur.
6. `README` kurulum ve demo akışını açıklarken GitHub URL ve SABİS teslimi tamamlanır; sunuma katılım gerekir.

## Teslim kanıtları
GitHub commit geçmişi, ekran görüntüleri, test çıktısı, Flyway migration'ları, kaynak kodu, örnek demo hesapları (yalnızca yerelde) ve kısa slaytlar. Demo sırasında internet bağımlılığı olmamalı; veritabanı Docker'da önceden hazır edilir.

## Tek/iki kişilik sorumluluk
**Tek kişi:** S00–S10 sırayla. **İki kişi:** A domain/backend/test; B arayüz/security/dokümantasyon; ortak API sözleşmesi ve günlük review. Aynı modülde eşzamanlı kontrolsüz değişiklik yapılmaz.
