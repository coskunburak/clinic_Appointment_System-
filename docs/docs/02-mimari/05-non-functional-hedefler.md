# Non-functional kalite hedefleri ve sınırlar

| Kategori | Hedef | Doğrulama |
|---|---|---|
| Doğruluk | aynı çalışana çakışan iki randevu yok | 2+ thread gerçek Postgres testi |
| Yetkilendirme | kullanıcı başka hasta/kurum verisini okuyamaz | MVC/service negatif test |
| Kalıcılık | uygulama yeniden açılınca kayıtlar korunur | DB restart + smoke |
| Bakım | controller iş kuralı içermez | package check + PR inceleme |
| Test edilebilirlik | domain testleri DB olmadan çalışır | unit tests |
| Yerelleştirme | saatleri şube zone'unda gösterir | zaman dönüşümü/DST sınır testi |
| Gözlemlenebilirlik | anlamlı hata kodları, talep korelasyon ID | log ve hata ekranı |
| Performans | 1000 demo randevusu ve 50 çalışanla yerel p95 <1sn hedefi | basit yük denemesi; donanıma bağlı, garanti değil |
| Erişilebilirlik | etiketli form, klavye ile gezinme, görünür hata | manuel WCAG temel kontrol |
| Teslim edilebilirlik | README adımları temiz makinede kurulabilir | fresh clone prosedürü |

## Güvenli işletim kuralları
- Parola yalnız hash; dev demo hesabı prod profilinde yok.
- Sır değerler commit edilmez: örnek `.env.example`, gerçek `.env` gitignore.
- Profile `dev/test/prod`: veritabanı kimlik bilgileri environment'tan.
- Loglarda parola, oturum cookie, kişisel iletişim detayları bulunmaz.
- `ddl-auto=create`, `update` prod'da kullanılmaz; Flyway tek şema kaynağı.
- `429`, `403`, `404`, `409`, `422` hata ayrımları tanımlıdır; iç SQL hata detayı UI'ya sızmaz.
