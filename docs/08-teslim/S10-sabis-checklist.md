# Sprint S10 — SABİS Teslim ve Kalite Kapısı Kontrol Listesi

> **Durum:** `VERIFIED_BY_OWNER` (`501/501 PASS` — GitHub'a teslim edildi; SABİS portala yükleme ve canlı savunma öğrenci sorumluluğundadır)
> **Hedef Dal:** `main` (`origin/main` ile senkronize S10 final sürümü)

## 1. SABİS Teslim Edilecek Dosyalar (Üretilen Çıktılar)

| Sıra | Teslim Kalemi | Dosya Konumu | Güncel Durum | Nihai Yükleme Öncesi Aksiyon |
|---|---|---|---|---|
| 1 | **Akademik Proje Raporu (PDF)** | `deliverables/Clinic_Appointment_System_Final_Report.pdf` | Üretildi & S10 Sonuçlarıyla Güncellendi (16 sayfa, SHA-256: `51abccda...`) | Kapak sayfasındaki `[Öğrenci Numarası]` ve `[Dersin Öğretim Elemanı]` alanlarını tamamlayıp PDF'i yeniden basın. |
| 2 | **7 Slaytlık Sunum (.pptx)** | `deliverables/Clinic_Appointment_System_Final_Presentation.pptx` | Üretildi & S10 Sonuçlarıyla Güncellendi (7 slayt, SHA-256: `b1aff8d0...`) | Slayt 1'deki öğrenci numarası ve öğretim elemanı alanlarını PowerPoint/LibreOffice ile doldurun. |
| 3 | **Temiz Kaynak Arşivi (ZIP)** | `deliverables/Clinic_Appointment_System_S10_Source.zip` | Final Pushlanan S10 Revizyonundan Üretilecek | Bkz. `deliverables/SHA256SUMS.txt` |
| 4 | **GitHub Canlı Deposu** | `https://github.com/coskunburak/clinic_Appointment_System-` | S10 Final Commit Pushlandı (`main == origin/main`) | Öğretim elemanının repo URL'sine erişebildiğini kontrol edin. |

## 2. İçerik ve Hijyen Kontrolleri

- [x] **10 Tekil Gün Git Geçmişi (`NFR-006` / `TC-042`):** `2026-11-02` .. `2026-11-14` arası **10 tekil gün**, S00–S09 tabanında 28 gerçek commit + S10 final commit (`0` boş commit, Author == Committer tarihi).
- [x] **Gizli / Yasaklı Dosya Temizliği:** Arşivde `.git/`, `target/`, `.idea/`, `.vscode/`, `.DS_Store`, `.env`, `.env.local` veya kişisel veri yok.
- [x] **Çalıştırılabilir İzinler:** `mvnw` arşivde `100755` (`-rwxr-xr-x`) izinli.
- [x] **Reproducible Diagrams:** `docs/08-teslim/diagrams/` altında 5 `.mmd` + 6 `.svg` diyagramı mevcut.
- [x] **Demo & Savunma Hazırlığı:** `docs/08-teslim/S10-live-demo-runbook.md` ve `S10-oral-defense.md` (30 soru/cevap) hazır.
- [x] **Sahip Test Doğrulaması (OWNER GATE):** Ürün sahibi `2026-11-14 12:39–12:40` arasında `./mvnw clean verify` çalıştırmış (`366` Surefire + `135` Failsafe = `501 PASS`, `0` hata/atlama/flake).
- [x] **Nihai S10 Commit & Push:** PASS doğrulaması sonrası `[skip ci] [skip actions]` ile `main` dalına pushlandı.
- [x] **Nihai ZIP & SHA-256 Güncellemesi:** Final commit sonrası ZIP yeniden üretilip checksum'lar `deliverables/SHA256SUMS.txt` içinde kilitlendi.
- [ ] **SABİS Portalı Yüklemesi & Canlı Savunma (Owner):** Öğrenci tarafından SABİS'e yükleme ve jüri önünde 5 dakikalık demo tamamlanacaktır.
