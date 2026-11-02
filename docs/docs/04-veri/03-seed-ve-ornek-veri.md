# Demo seed, test fixture ve veri güvenliği

## Kurgusal örnek veri
**Kuruluş:** Örnek Sağlık Grubu; **şubeler:** Merkez (Europe/Istanbul) ve Sahil (Europe/Istanbul).

| Şube | Bölüm | Hizmet | Süre | Ücret | Atanan çalışan |
|---|---|---|---|---|---|
| Merkez | Dahiliye | Genel Muayene | 30dk | 800.00 TRY | Dr. Ada Örnek |
| Merkez | Radyoloji | Ultrason | 45dk | 1200.00 TRY | Tek. Deniz Test |
| Sahil | Dahiliye | Genel Muayene | 20dk | 650.00 TRY | Dr. Bora Demo |
| Sahil | Laboratuvar | Kan Alma | 15dk | 250.00 TRY | Hem. Elif Senaryo |

Uygulama demo hesabı/e-postası yerel `.env` veya seed ile oluşturulabilir; sabit gerçek şifre Git'e girilmez. README'de kullanıcıya seed'de hangi **kurmaca** hesapların bulunduğu ve ilk kurulum parolasının nasıl belirtildiği açıklanır.

## Fixture takvimi
Merkez Pazartesi–Cuma 09:00–17:00; Sahil 10:00–18:00; Dr. Ada Pazartesi 09:00–12:00 ve 13:00–16:00; örnek izin gününde 10:00–11:00 kapalı. Demo tarihleri çalıştırıldığı güne göre *gelecek en yakın çalışma günü* dinamik hesaplanır; geçmişte kalan sabit randevu demo kabulünü bozmaz.

## Reset stratejisi
Prod veritabanına dokunmadan `compose down -v` sadece **demo için ayrılmış** yerel volume üzerinde ve kullanıcının açık onayıyla çalıştırılır. Test verisi her testten önce rollback / fixture ile kurulur; demo ortamı temizlenebilir ve tekrar üretilebilir.
