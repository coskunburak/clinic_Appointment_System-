# GitHub — 10 Farklı Gün Commit Zorunluluğu ve Takip Takvimi

**Ödev Kuralı:** GitHub üzerinde minimum **10 commit, birbirinden farklı takvim günlerinde** yapılmış bulunmalıdır. Aynı gün içinde atılan birden fazla commit tek bir geliştirme günü olarak sayılır. Commit tarihlerini değiştirme/backdate kullanma, yapay tarih üretme ve boş commit kesinlikle yasaktır; hedef gerçek geliştirme günlerini belgelemektir.

---

## 1. Depo Mevcut Durum Denetimi (Baseline Audit — 2026-11-02)

| Metrik | Değer | Açıklama |
|---|---|---|
| **Toplam Kayıtlı Commit Sayısı** | 3 | Başlangıç ve dokümantasyon yüklemesi |
| **Tespit Edilen Commit Tarihleri** | 2026-11-02 (Tüm 3 commit) | `cdd87b6`, `215a542`, `3b2dd1c` |
| **Benzersiz Takvim Günü Sayısı** | **1** | Kural gereği aynı gün atılan 3 commit 1 gün sayılır |
| **Hedefe Ulaşma Durumu** | **1 / 10** | Tamamlanması gereken en az 9 benzersiz gün bulunmaktadır |
| **Kural Uyum Riski** | Yüksek (Zaman planlaması yapılmazsa) | Sprintlerin farklı günlere planlanması zorunludur |

---

## 2. Gerçek Gün Bazlı Geliştirme ve Commit Takvimi

Teslim son tarihi henüz açıklanmadığından (`PENDING_OWNER_CONFIRMATION`), geliştirme adımları sprint bazında günlere dağıtılmıştır:

| Gün No | Sprint | Beklenen Anlamlı Değişiklik | Önerilen Commit Mesajı | Durum |
|---|---|---|---|---|
| **Gün-1** | S00 | Repo iskeleti, dokümantasyon normalizasyonu, README ve kapsam | `docs(s00): establish repository foundation and academic scope` | Tamamlandı (2026-11-02) |
| **Gün-2** | S01 | PostgreSQL Docker, Flyway V1 şeması ve paket mimarisi | `feat(db): bootstrap flyway v1 schema and package structure` | Planlandı |
| **Gün-3** | S02 | OOP domain çekirdeği (BaseEntity, Value Objects, hiyerarşi) | `feat(domain): implement core entity models and value objects` | Planlandı |
| **Gün-4** | S03 | Kurum, şube, bölüm ve hizmet kataloğu yönetimi | `feat(catalog): implement branch and healthcare services` | Planlandı |
| **Gün-5** | S04 | Kimlik doğrulama, Spring Security ve rol yetkilendirmesi | `feat(security): configure role-based access control` | Planlandı |
| **Gün-6** | S05 | Doktor ve personel mesai saatleri, haftalık uygunluk | `feat(schedule): add staff availability and working hours` | Planlandı |
| **Gün-7** | S06 | Randevu slot hesaplama ve çakışma önleme (pessimistic lock) | `feat(booking): prevent overlapping reservations with locking` | Planlandı |
| **Gün-8** | S07 | Randevu onay yaşam döngüsü ve işlem geçmişi (state machine) | `feat(booking): add appointment approval lifecycle` | Planlandı |
| **Gün-9** | S08 | Spring MVC ve Thymeleaf arayüz ekranları | `feat(web): add thymeleaf appointment flow screens` | Planlandı |
| **Gün-10** | S09 | Entegrasyon testleri, eşzamanlılık doğrulaması ve demo seed verisi | `test: cover concurrent booking and provide demo seed` | Planlandı |
| **Gün-11** | S10 | Son kalite kapısı, sunum dokümanları ve akademik rapor | `docs(s10): finalize presentation materials and report` | Planlandı |

> **Önemli Tavsiye:** Hedef 10 yerine en az 11–12 farklı gerçek gün olarak planlanmıştır. Günler art arda olmak zorunda değildir; önemli olan commit'lerin gerçek takvim günlerinde ve anlamlı kod/doküman ilerlemesiyle atılmasıdır.

---

## 3. Gerçekleşen Commit Takip Defteri

| No | Gerçek Tarih (YYYY-MM-DD) | Sprint | Commit SHA | Kısa Mesaj | Doğrulama Durumu |
|---|---|---|---|---|---|
| 1 | 2026-11-02 | S00 | `cdd87b6` | first commit | Tarihçe denetlendi |
| 2 | 2026-11-02 | S00 | `215a542` | first comit | Tarihçe denetlendi |
| 3 | 2026-11-02 | S00 | `3b2dd1c` | docs | Tarihçe denetlendi |
| 4 | *Öğrenci belirleyecek* | S00 | *Oluşacak* | `docs(s00): establish repository foundation and academic scope` | Manuel test sonrası commit edilecek |
| 5 | *Öğrenci belirleyecek* | S01 | *Oluşacak* | `feat(db): ...` | Bekliyor |

---

## 4. Git Kontrol Komutları

Benzersiz geliştirme gün sayısını yerel terminalinizde kontrol etmek için:

```bash
git log --format='%h %as %s' --reverse
git log --format=%as | sort -u | wc -l
git status --short
```

> **Uyarı:** `--format=%as` yazar tarihini (author date) verir. GitHub web arayüzünde committer tarihi de önem taşıdığından, commit'lerinizi düzenli olarak `git push` ile uzak depoya gönderiniz. Rebase veya squash işlemleri commit tarihlerini birleştirebileceğinden ortak geçmişte rebase/squash yapmayınız.

---

## 5. GitHub ve SABİS Teslim Kontrolü

- **GitHub Teslimi:** Depo açık (public) veya öğretim elemanına yetki verilmiş olmalıdır.
- **SABİS Teslimi:** Proje zip arşivi ve GitHub repo URL bağlantısı süresi içinde yüklenmelidir.
- **Kesin Teslim Tarihi:** Duyurulduğunda buraya işlenecektir (`PENDING_OWNER_CONFIRMATION`).
