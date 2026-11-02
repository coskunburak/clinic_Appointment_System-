# GitHub — 10 Farklı Gün Commit Zorunluluğu ve Takip Takvimi

**Ödev Kuralı:** GitHub üzerinde minimum **10 commit, birbirinden farklı takvim günlerinde** yapılmış bulunmalıdır. Aynı gün içinde atılan birden fazla commit tek bir geliştirme günü olarak sayılır. Commit tarihlerini değiştirme/backdate kullanma, yapay tarih üretme ve boş commit kesinlikle yasaktır; hedef gerçek geliştirme günlerini belgelemektir.

---

## 1. Depo Mevcut Durum Denetimi (Baseline Audit — 2026-11-02)

| Metrik | Değer | Açıklama |
|---|---|---|
| **Toplam Kayıtlı Commit Sayısı** | 4 | Başlangıç, dokümanlar ve S00 yerel commit'i |
| **Tespit Edilen Commit Tarihleri** | 2026-11-02 (Tüm 4 commit) | `cdd87b6`, `215a542`, `3b2dd1c`, `23b7d28` |
| **Benzersiz Takvim Günü Sayısı** | **1** | Kural gereği aynı gün (2026-11-02) atılan 4 commit 1 gün sayılır |
| **Hedefe Ulaşma Durumu** | **1 / 10** | Tamamlanması gereken en az 9 benzersiz gün bulunmaktadır |
| **Kural Uyum Riski** | Yüksek (Zaman planlaması yapılmazsa) | S01 commit'i **ertesi gün veya sonraki takvim gününde** atılmalıdır |

---

## 2. Gerçek Gün Bazlı Geliştirme ve Commit Takvimi

Ders kapsamında 10 farklı gün kuralını karşılamak için önerilen sprint-gün dağılımı:

| Gün No | Sprint | Beklenen Anlamlı Değişiklik | Önerilen Commit Mesajı | Durum |
|---|---|---|---|---|
| **Gün-1** | S00 | Repo iskeleti, dokümantasyon normalizasyonu, README ve kapsam | `docs(s00): establish repository foundation, academic scope and fix build dependencies` | Tamamlandı (`23b7d28` — 2026-11-02) |
| **Gün-2** | S01 | PostgreSQL 17 Docker, Flyway V1 şeması, JPA ve CI | `feat(s01): establish PostgreSQL, Flyway, JPA and CI foundation` | **Uygulandı — Yeni Günde Commit Bekliyor** |
| **Gün-3** | S02 | OOP domain çekirdeği (BaseEntity, Value Objects, hiyerarşi) | `feat(s02): implement core entity models, value objects and domain guards` | Planlandı |
| **Gün-4** | S03 | Kurum, şube, bölüm ve hizmet kataloğu yönetimi | `feat(s03): implement branch, department and service catalog management` | Planlandı |
| **Gün-5** | S04 | Kimlik doğrulama, Spring Security ve 3 rol yetkilendirmesi | `feat(s04): configure spring security and multi-tenant access control` | Planlandı |
| **Gün-6** | S05 | Doktor ve personel mesai saatleri, haftalık uygunluk slotları | `feat(s05): implement staff availability schedules and working hours` | Planlandı |
| **Gün-7** | S06 | Randevu slot hesaplama, pesimist satır kilidi ve çakışma önleme | `feat(s06): enforce pessimistic locking and conflict-free booking` | Planlandı |
| **Gün-8** | S07 | Randevu onay yaşam döngüsü, durum makinesi ve snapshot | `feat(s07): implement appointment approval state machine and audit log` | Planlandı |
| **Gün-9** | S08 | Spring MVC ve Thymeleaf kullanıcı arayüz ekranları | `feat(s08): deliver thymeleaf views for patient, doctor and admin` | Planlandı |
| **Gün-10** | S09 | Entegrasyon testleri, eşzamanlılık doğrulaması ve demo seed verisi | `test(s09): verify concurrent reservation and seed demo dataset` | Planlandı |
| **Gün-11** | S10 | Son kalite kapısı, sunum dokümanları ve akademik nihai rapor | `docs(s10): finalize presentation materials and academic report` | Planlandı |

> **Önemli Akademik Tavsiye:** Hedef 10 yerine 11 farklı gerçek takvim günü olarak planlanmıştır. Günlerin art arda olması zorunlu değildir; önemli olan commit'lerin farklı takvim günlerinde ve anlamlı kod/doküman ilerlemesiyle atılmasıdır.

---

## 3. Gerçekleşen Commit Takip Defteri

| No | Gerçek Tarih (YYYY-MM-DD) | Sprint | Commit SHA | Kısa Mesaj | Doğrulama Durumu |
|---|---|---|---|---|---|
| 1 | 2026-11-02 | S00 | `cdd87b6` | first commit | Tarihçe denetlendi |
| 2 | 2026-11-02 | S00 | `215a542` | first comit | Tarihçe denetlendi |
| 3 | 2026-11-02 | S00 | `3b2dd1c` | docs | Tarihçe denetlendi |
| 4 | 2026-11-02 | S00 | `23b7d28` | docs(s00): establish repository foundation... | Yerelde commit edildi (ahead 1) |
| 5 | *Yeni Gün (Ör: 2026-11-03)* | S01 | *Oluşacak* | `feat(s01): establish PostgreSQL, Flyway, JPA and CI foundation` | Manuel test sonrası atılacak |

---

## 4. Git Kontrol Komutları

Benzersiz geliştirme gün sayısını yerel terminalinizde anlık olarak kontrol etmek için:

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
- **Canlı Sunum:** 7. haftadan itibaren sunumlar başlayacaktır. Sunuma katılmayan öğrenci puan alamaz.
