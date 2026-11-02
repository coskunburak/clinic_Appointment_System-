# Akademik sunum ve canlı demo planı

**Ödev koşulu:** sunuma katılmayan öğrenci puan alamaz; son 4 haftada sunumlar ve tarihler duyurulur. Bu plan **örnek 5–7 dakika** olarak düzenlenmiştir, öğretim elemanı daha kısa süre verirse ölçeklenir.

## 7 slayt önerisi
1. **Problem ve hedef:** doktorların saatleri ve hizmetlere göre çakışmasız randevu.
2. **Ödev gereksinimi ↔ ürün:** şube, hizmet/süre/ücret, personel, availability, onay, 3 rol.
3. **Teknoloji/mimari:** Java 21, Spring Boot, MVC, PostgreSQL; modüler monolit.
4. **OOP kanıtı:** `PersonProfile` inheritance, `Appointment` encapsulation, `SchedulingConstraint` polymorphism.
5. **ER ve algoritma:** staff lock, `[start,end)` conflict, snapshot, `PENDING → CONFIRMED`.
6. **Test ve GitHub:** JUnit/real PG concurrency, 10 farklı gün commit grafiği.
7. **Canlı demo, sınırlılıklar, sonraki işler:** gerçek hastane entegrasyonu yok.

## 5 dakikalık örnek demo senaryosu
1. Admin hesabıyla Merkez ve Sahil şubelerini; şube bazlı farklı ücretleri göster.
2. Admin doktoru ve haftalık uygunluğunu, ilgili hizmete yetkisini göster.
3. Hasta olarak Dr. Ada'dan 09:00–09:30 PENDING randevu al; ücreti/süreyi kontrol et.
4. İkinci kullanıcıyla aynı doktora 09:15 talep edip çakışmanın engellenmesini göster.
5. Personel hesabıyla ilk randevuyu CONFIRMED yap.
6. Hasta hesabında onaylı durumun değiştiğini göster.
7. İki somut Java sınıfı üzerinden kalıtım/çok biçimlilik örneği göster; test raporunu aç.

## Hızlı prova / başarısızlık planı
- [ ] Docker PostgreSQL ve uygulama önceden açıldı; lokal bağlantı var.
- [ ] Demo data kurmaca, gelecekte geçerli randevu günleri seçildi.
- [ ] Üç demo rolünün hesabı/oturumu önceden test edildi.
- [ ] Bilerek oluşturulacak çakışma senaryosu deterministic.
- [ ] İnternet olmadığında Bootstrap/CDN olmadan ekran açılıyor.
- [ ] Yazılım açılmazsa ekran görüntüsü + test logu + GitHub kaynak kodu yedeği var.
- [ ] Sunumu 1/2 kişi bölüşüyorsa kimin hangi slaytı anlatacağı belirlendi.

## Öğretim elemanının muhtemel soruları
**Neden Spring Boot?** Java OOP'yi web mimarisinde görünür kullanıp rol/DB akışını hızlı kurar.
**Polymorphism nerede?** `SchedulingConstraint` interface'inin farklı uygulamaları aynı `validate` sözleşmesiyle çağrılır.
**İki hasta aynı anda alırsa?** transaction içindeki staff row lock, overlap check ve gerçek PostgreSQL concurrency test.
**Fiyat değişirse?** Appointment snapshot tarihsel ücreti saklar.
**Hasta başka randevuyu görebilir mi?** Resource ownership ve service-level access policy engeller.
**Çok şubeyi nasıl destekler?** Branch, BranchService ve StaffAssignment ilişki modeli; veri şube bazında, çalışan çakışma kontrolü kişi bazında.
