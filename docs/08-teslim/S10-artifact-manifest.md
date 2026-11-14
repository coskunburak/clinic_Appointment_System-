# Sprint S10 — Teslimat Manifestosu ve Doğrulama Özeti

> **Aşama:** Aşama B — Sahip Doğrulamalı Final Yayın (`VERIFIED_BY_OWNER`)
> **Hedef Dal:** `main` (`origin/main` ile senkronize S10 final teslimi)
> **Doğrulama Zamanı:** `2026-11-14 12:39–12:40+03:00` (`366` Surefire + `135` Failsafe PostgreSQL 17 Testcontainers = `501 PASS`)

---

## 1. İkili SABİS Teslimatları (`deliverables/` — Git Dışı Paketler)

| Dosya | Boyut (Bayt) | SHA-256 Özet Değeri | Yapı &amp; Görsel Doğrulama | Durum |
|---|---|---|---|---|
| `deliverables/Clinic_Appointment_System_Final_Report.pdf` | `1089105` | `51abccda7efc3cfa1f51fa036524b543459dbbd646cb120e3f3728ddc1010ef6` | **16 sayfa A4 PDF**, S10 doğrulama kanıtlarıyla güncellenmiş, gömülü 6 SVG diyagramı, sayfa kenarı başlık/sayfa numaraları. | Kapaktaki öğrenci no / öğretim elemanı alanları doldurulup SABİS'e yüklenecek |
| `deliverables/Clinic_Appointment_System_Final_Presentation.pptx` | `425956` | `b1aff8d054d127dc72ce9b8d2d4cce93f246c2889ac76668262ab8a5e5aecdf6` | **7 slayt 16:9 OpenXML (.pptx)**, S10 doğrulamalı Slayt 6 KPI'ları, 7 Türkçe sunucu notu (`notesSlides`), 4 gömülü diyagram PNG'si. | Slayt 1'deki öğrenci no / öğretim elemanı alanları doldurulacak |
| `deliverables/Clinic_Appointment_System_S10_Source.zip` | Final Commit Sonrası | Bkz. `deliverables/SHA256SUMS.txt` | `git archive --format=zip --prefix=clinic-appointment/ HEAD` ile teslim edilen final S10 commit'inden deterministik üretilir (`mvnw` `-rwxr-xr-x`, `0` yasaklı dosya). | Final S10 commit sonrasında üretilip SABİS'e yüklenecek |

---

## 2. Üretilebilir Diyagram Varlıkları (`docs/08-teslim/diagrams/`)

| Diyagram | Mermaid Kaynağı | Üretilen SVG | Sunum/Rapor Kullanımı |
|---|---|---|---|
| Modüler Monolit Bileşen Mimarisi | `01-modular-monolith-architecture.mmd` | `01-modular-monolith-architecture.svg` | Rapor Şekil 4.1 + Slayt 3 |
| OOP Kalıtım &amp; Strategy Sınıf Diyagramı | `02-domain-class-diagram.mmd` | `02-domain-class-diagram.svg` | Rapor Şekil 3.1 + Slayt 4 |
| Sıralı Pesimist Kilitleme Sıra Diyagramı | `03-booking-sequence-diagram.mmd` | `03-booking-sequence-diagram.svg` | Rapor Şekil 5.1 |
| Randevu Yaşam Döngüsü Durum Makinesi | `04-appointment-state-machine.mmd` | `04-appointment-state-machine.svg` | Rapor Şekil 5.2 + Slayt 5 |
| PostgreSQL 17 Flyway V1–V4 ERD (16 Tablo) | `05-database-erd.mmd` | `05-database-erd.svg` | Rapor Şekil 4.2 |
| Gerçek 10 Tekil Gün Git Commit Dağılımı | — | `06-git-10-day-commits.svg` | Rapor Şekil 7.1 + Slayt 6 |

---

## 3. S10 Dokümantasyon ve Hazırlık Çıktıları (`docs/`)

| Dosya | Amacı |
|---|---|
| `docs/08-teslim/report/Clinic_Appointment_System_Final_Report.html` | Nihai PDF raporun düzenlenebilir HTML/SVG kaynağı (öğrenci numarası / hoca adı kolayca güncellenebilir) |
| `docs/08-teslim/S10-live-demo-runbook.md` | 5 dakikalık zamanlı canlı gösterim senaryosu, kurmaca `dev` hesapları, reset komutu ve çevrimdışı hazırlık |
| `docs/08-teslim/S10-oral-defense.md` | 30 soruluk OOP, SOLID, eşzamanlılık, PostgreSQL, güvenlik ve kapsam sözlü savunma rehberi |
| `docs/08-teslim/S10-sabis-checklist.md` | SABİS yükleme, kapak tamamlama, sahip test kapısı ve savunma kontrol listesi |
| `docs/08-teslim/01-github-commit-takvimi.md` | 28 commit / 10 tekil takvim günü denetim defteri (`GIT_10_DAY_REQUIREMENT: PASS`) |
| `docs/07-sprintler/S10-implementation-report.md` | Sprint S10 uygulama ve kalite kapısı raporu (22 bölüm) |
| `docs/01-urun/01-gereksinim-matrisi.md` &amp; `traceability.csv` | `NFR-006 / TC-042` (`PASSED`) ve `NFR-007 / TC-043` (`PENDING_OWNER_ACTION`) izlenebilirlik güncellemeleri |
| `README.md`, `docs/README.md`, `docs/07-sprintler/README.md`, `docs/07-sprintler/S10.md`, `docs/08-teslim/{02,03}-*.md` | S00–S10 yol haritası, OOP özetleri ve teslimat bağlantıları senkronizasyonu |

---

## 4. Kalite ve Yayın Politikası Beyanı

- **Kod Donması (Scope Freeze):** `src/main/**`, `src/test/**`, `pom.xml`, `compose.yaml` ve `db/migration/**` dosyalarında hiçbir değişiklik yapılmamıştır.
- **Doğrulama Durumu (`VERIFIED_BY_OWNER`):** `2026-11-14 12:39:21–12:40:07+03:00` tarihli ürün sahibi `./mvnw clean verify` çalıştırmasıyla **46 sınıf / 366 Surefire** + **20 sınıf / 135 Failsafe PostgreSQL 17 Testcontainers** = **501 test 0 hata / 0 atlama** ile doğrulanmıştır. Nihai ikili checksum'lar `deliverables/SHA256SUMS.txt` dosyasında tutulur.
