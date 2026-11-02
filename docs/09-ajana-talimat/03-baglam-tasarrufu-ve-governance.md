# Context budget, çalışma rejimi ve quality gates

## Dosya okuma protokolü
1. `git status --short`, `git branch --show-current`, son ilgili commit; build manifest `pom.xml`.
2. Yalnız aktif sprint dokümanı ve ona bağlı FR/TC satırları.
3. Dosya isimleri/`rg` sembolleriyle ilgili modülleri bul, yalnız hedef sınıfları incele.
4. Değişiklikten sonra sadece etkilenen testler ve dokümanların diff'ini incele.
5. Paket hiyerarşisini repo gerçekliğine göre değerlendir; önerilen doküman path'i otomatik oluşturulmuş dosya sayılmaz.

## Öncelik
Bug fix > MUST FR > authorization > concurrency > migration/schema > UX > SHOULD > COULD. Bir sprintte COULD işi MUST kalitesini geciktiremez.

## Geliştirici/ajan sınırları
- Ajan: kodu değiştirir, test yazar, dokumentasyonu günceller, öğrencinin çalıştıracağı komutları tek blok verir.
- Öğrenci: testleri çalıştırır, çıktı paylaşır, commit/push için açık karar verir, gerçek teslim ve sunuma katılır.
- Ajan: test sonucu uydurmaz, commit tarihini taklit etmez, dış bağlantı veya kişisel veriler için secrets istemez.

## Baseline kontrolü
Repo adı henüz bilinmiyorsa veya proje kurulmadıysa sıfırdan oluşturulduğu açıkça belirtilir. Mevcut dosyalar doğrudan silinmez; değişiklikten önce status kayda geçer.

## GitHub ders kanıtı
Bir günde 5 commit ≠ 5 farklı gün. Bu sınır yapay commit veya sahte git author date ile aşılmaya çalışılmaz. Öğrenci gerçek ilerleme günleri tutar.

## Definition of Ready / Done
**Ready:** FR, TC, önceki sprint bağımlılığı, dosya sınırı, risk tanımı belirli.
**Done:** kod + uygulanan tasarım + negatif test + manuel test PASS kanıtı + doküman + review; migration ve güvenlik testleri gerekiyorsa ayrıca geçmeli.
