# Risk kaydı ve azaltma planı

| Risk | Olasılık/etki | Tetikleyici | Mitigasyon | Sahip |
|---|---|---|---|---|
| 10 farklı gün commit unutulması | orta/yüksek | 7. güne kadar <7 gün commit | günlük gerçek, anlamlı commit tablosu | öğrenci |
| Son teslim tarihinin bilinmemesi | yüksek/yüksek | plan durumu sürüncemede | SABİS/duyuru tarihini öğrenci doğrular | öğrenci |
| Çift rezervasyon | orta/çok yüksek | iki eşzamanlı POST | personel satır lock + DB concurrency test | booking geliştirici |
| IDOR / başka hasta verisi | orta/çok yüksek | URL UUID değiştirme | service-level access policy + security test | security geliştirici |
| Boot 4 dependency uyumsuzluğu | orta/orta | init/test compiler hata | Initializr resmi modüllerle smoke test | backend |
| PostgreSQL Docker çalışmaması | orta/yüksek | fresh clone kurulum | README prerequisites + alternatif lokal PG | devops |
| UI kapsam şişmesi | yüksek/orta | yeni grafik, React ekleme | Thymeleaf minimum ekranlar, freeze | tüm ekip |
| Sağlık verisi mahremiyeti | düşük/yüksek | gerçek kişisel veri seed | yalnız sahte demo profili | tüm ekip |
| Sunumda internet/DB yok | orta/yüksek | offline sınıf ortamı | offline prova, local assets, screenshots | sunan |
| İki kişinin çakışan commit'i | orta/orta | aynı controller dosyasına parallel PR | modül sahipliği, küçük PR, review | iki kişi |
