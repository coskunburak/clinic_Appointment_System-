# GitHub — 10 farklı gün commit zorunluluğu

**Ödev metni:** GitHub'da minimum **10 commit, farklı günlerde yapılmış** bulunmalı. 10 commit'i aynı gün atmak bu koşulu sağlamaz. Commit tarihlerini değiştirme/backdate kullanma, yapay tarih üretme; gerçek geliştirme günlerini belgelemek hedef.

## Gerçek gün bazlı örnek takvim (teslim tarihi bilinmediğinden tarih yerine Gün-1 … Gün-12)
| Gerçek gün | Beklenen anlamlı değişiklik | Örnek commit mesajı | Kanıt |
|---|---|---|---|
| Gün-1 | Repo/README/requirements | `docs: define project requirements` | SHA / GitHub link |
| Gün-2 | Build ve CI | `build: bootstrap spring application` | SHA / link |
| Gün-3 | DB Flyway V1 | `feat(db): add identity and organization schema` | SHA / link |
| Gün-4 | OOP domain modelleri | `feat(domain): add appointment value objects` | SHA / link |
| Gün-5 | Şube ve hizmet | `feat(catalog): manage branch services` | SHA / link |
| Gün-6 | Security rol/scope | `feat(auth): implement scoped role access` | SHA / link |
| Gün-7 | Personel programı | `feat(schedule): add staff availability` | SHA / link |
| Gün-8 | Booking/lock | `feat(booking): prevent overlapping reservations` | SHA / link |
| Gün-9 | Approval/history | `feat(booking): add approval lifecycle` | SHA / link |
| Gün-10 | MVC ekranları | `feat(web): add patient appointment flow` | SHA / link |
| Gün-11 | Test regression/CI | `test: cover concurrent booking and role access` | SHA / link |
| Gün-12 | Rapor/sunum/fix | `docs: finalize demo and submission` | SHA / link |

**Öneri:** minimum 10 yerine 12 veya daha fazla **farklı gerçek gün** hedefle; ancak boş veya yapay commit eklemek yerine anlamlı ilerlemeyi kaydet. Günler peş peşe olmak zorunda değil; kesin tarihler ders takvimine göre doldurulacak.

## Takip defteri (gerçek tarihler kullanıcı tarafından doldurulur)
| Gerçek tarih (YYYY-MM-DD) | Sprint | Commit SHA | GitHub bağlantısı | Test durumu |
|---|---|---|---|---|
| | | | | |
| | | | | |
| | | | | |
| | | | | |

## Git kontrol komutları
```bash
git log --format='%h %as %s' --reverse
git log --format=%as | sort -u | wc -l
git status --short
```

**Dikkat:** `--format=%as` author date'i verir. GitHub'da tarihlerin nasıl görüntülendiğini de kontrol et; sadece yerel sayım tek başına uzak repo kanıtı değildir. Push gecikmesi, squash merge veya rebase tarihi yorumunu değiştirebilir. Gerçek commitleri düzenli olarak uzak repoya gönder.

## GitHub → SABİS
Gönderim kanıtı: GitHub repo URL, gerekli repo erişim türü (private ise öğretim elemanına yetki gerekip gerekmediği), dosya/ZIP, SABİS yükleme ekran görüntüsü ve sunum duyuru takibi. **Son tarih açıklanmadı**: asla varsayılmamalı.
