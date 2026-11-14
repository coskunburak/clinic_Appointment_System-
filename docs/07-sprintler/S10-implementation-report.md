# Sprint S10 — Nihai Kalite Kapısı, SABİS Teslimi ve Savunma Hazırlığı Uygulama Raporu

## 1. Yönetici Özeti ve Mevcut Durum

Sprint S10 **Aşama B (Sahip Doğrulamalı Nihai Yayın — `VERIFIED_BY_OWNER`)** başarıyla tamamlanmıştır. Kapsam dondurma politikası gereği `src/main/**`, `src/test/**`, `pom.xml`, `compose.yaml` ve Flyway SQL betiklerinde (`V1`–`V4`) hiçbir uygulama veya şema değişikliği yapılmamıştır. Ürün sahibi `2026-11-14 12:39:21–12:40:07+03:00` arasında `./mvnw clean verify` çalıştırarak **366/366 Surefire + 135/135 Failsafe PostgreSQL 17 Testcontainers = 501/501 PASS** sonucunu diskteki XML raporlarıyla kanıtlamıştır. En az 10 farklı gerçek takvim gününde commit şartı (`NFR-006` / `TC-042`) **10 tekil gün** ile doğrulanmış (`GIT_10_DAY_REQUIREMENT: PASS`); akademik PDF raporu (16 sayfa), 7 slaytlık düzenlenebilir `.pptx` sunumu, 6 üretilebilir diyagram, 5 dakikalık canlı demo runbook'u, 30 soruluk sözlü savunma rehberi, SABİS kontrol listesi ve temiz kaynak ZIP arşivi üretilerek GitHub `main` dalı `[skip ci] [skip actions]` ile senkronize edilmiştir.

## 2. Sprint Kimliği, Kapsam ve Kapsam Dondurma Beyanı

- **Sprint:** `S10` — Nihai Kalite Kapısı, SABİS Teslimi ve Canlı Savunma (`docs/07-sprintler/S10.md`)
- **Hedef Dal ve Taban Revizyon:** `main` (S09 tabanı `8b70de2` üzerine S10 nihai yayın commit'i ve `origin/main` ile `0 / 0` eşitleme)
- **İlişkili Gereksinimler:** `NFR-006 / TC-042` (`PASSED`), `NFR-007 / TC-043` (`READY_FOR_SABIS_SUBMISSION`)
- **Kapsam Dondurma Uyumu:** Yeni özellik, spekülatif refactoring, domain yeniden tasarımı, bağımlılık yükseltme ve şema değişikliği **yapılmamıştır**. Sadece `.gitignore` (`deliverables/`) ve dokümantasyon/teslim varlıkları eklenip güncellenmiştir. `FR-031` (`DEFERRED`) ve `COULD` öncelikli `FR-035`, `FR-036` kapsam dışında kalmaya devam etmektedir.

## 3. Teslim Edilen / Üretilen Dosyaların Envanteri

### A. İkili SABİS Teslimatları (`deliverables/`, `.gitignore` ile yalıtılmış)
1. `deliverables/Clinic_Appointment_System_Final_Report.pdf` (`1.089.105` bayt, SHA-256: `51abccda7efc3cfa1f51fa036524b543459dbbd646cb120e3f3728ddc1010ef6`) — 16 sayfa akademik PDF raporu (S10 sahip doğrulamasıyla güncellendi).
2. `deliverables/Clinic_Appointment_System_Final_Presentation.pptx` (`425.956` bayt, SHA-256: `b1aff8d054d127dc72ce9b8d2d4cce93f246c2889ac76668262ab8a5e5aecdf6`) — 7 slaytlık düzenlenebilir PowerPoint sunumu + Türkçe sunucu notları.
3. `deliverables/Clinic_Appointment_System_S10_Source.zip` — Teslim edilen final S10 commit'i üzerinden `git archive` ile üretilen temiz kaynak ZIP (güncel SHA-256 değeri `deliverables/SHA256SUMS.txt` dosyasındadır).

### B. Yeni Eklenen Dokümantasyon ve Diyagram Dosyaları
- `docs/08-teslim/diagrams/01-modular-monolith-architecture.{mmd,svg}`
- `docs/08-teslim/diagrams/02-domain-class-diagram.{mmd,svg}`
- `docs/08-teslim/diagrams/03-booking-sequence-diagram.{mmd,svg}`
- `docs/08-teslim/diagrams/04-appointment-state-machine.{mmd,svg}`
- `docs/08-teslim/diagrams/05-database-erd.{mmd,svg}`
- `docs/08-teslim/diagrams/06-git-10-day-commits.svg`
- `docs/08-teslim/report/Clinic_Appointment_System_Final_Report.html`
- `docs/08-teslim/S10-live-demo-runbook.md`
- `docs/08-teslim/S10-oral-defense.md`
- `docs/08-teslim/S10-sabis-checklist.md`
- `docs/08-teslim/S10-artifact-manifest.md`
- `docs/07-sprintler/S10-implementation-report.md` (bu rapor)

### C. Güncellenen Mevcut Dosyalar
- `.gitignore` (`deliverables/` eklendi)
- `README.md`, `docs/README.md`, `docs/07-sprintler/README.md`, `docs/07-sprintler/S10.md`
- `docs/01-urun/01-gereksinim-matrisi.md`, `docs/01-urun/traceability.csv`
- `docs/08-teslim/01-github-commit-takvimi.md`, `docs/08-teslim/02-sunum-ve-demo.md`, `docs/08-teslim/03-proje-raporu-iskeleti.md`

## 4. Gerçekleştirilen OOP ve SOLID Kanıt Özeti (Kaynak Kodu Denetimi)

Uygulama kaynak kodunun statik incelenmesinde (`144` ana Java dosyası, `68` test Java dosyası) rapora yansıtılan tüm OOP kanıtları birebir doğrulanmıştır:
- **Kapsülleme:** `booking.domain.Appointment` üzerinde public `setStatus` yoktur (`approve`, `reject`, `cancel`, `complete`); `UserAccount.toString()` parola özeti içermez, `DemoDataProperties.toString()` parolaları `[REDACTED]` olarak maskeler.
- **Kalıtım ve Bileşim:** `BaseEntity` (`@MappedSuperclass`) &rarr; `PersonProfile` (`validateFullName`, `validatePhone`) &rarr; `PatientProfile` ve `StaffProfile`; şube görev ve hizmet yetkileri `StaffAssignment` / `StaffServiceEligibility` üzerinden bileşimle yönetilir.
- **Çok Biçimlilik:** `SchedulingValidator`, `List<SchedulingConstraint>` üzerinden `instanceof` olmadan `OpeningHoursConstraint`, `StaffAvailabilityConstraint`, `StaffServiceEligibilityConstraint` ve `NoOverlapConstraint` stratejilerini işletir.
- **Soyutlama ve Değer Nesneleri:** `AppointmentSchedulePort` &larr; `DatabaseAppointmentSchedulePort`, `Clock` soyutlaması, değişmez `Money`, `TimeInterval` ve `ServiceSnapshot`.

## 5. Mimari Katman ve Çok Kiracılı Bütünlük Kanıtları

- **Modüler Monolit Sınırları:** `DomainArchitectureTest`, saf domain çekirdeğinin Spring/JPA/Hibernate bağımlılığı almadığını; `WebArchitectureTest`, 9 MVC denetleyicisinin doğrudan `*JpaRepository` veya `DemoData*` enjekte etmediğini; `AppointmentArchitectureTest`, append-only tarihçeyi ve `Appointment` status setter yasağını denetler.
- **Veritabanı Şeması (`Flyway V1`–`V4`):** `16 tablo`, `ddl-auto: validate`, `open-in-view: false`, `membership(branch_id, organization_id)` ve `appointment(branch_service_id, branch_id)` kompozit yabancı anahtarları.

## 6. Gerçek Git Geçmişi Denetimi (`WP-S10-01` · `NFR-006 / TC-042`)

- **Komutlar:** `git log --format='%h %as %s' --reverse`, `git log --format=%as | sort -u | wc -l`, `git log --format='%h | A:%ai | C:%ci | %s' --reverse`
- **Sonuçlar:** Toplam **28** commit (`0` boş commit), tüm commit'lerde Author Date == Committer Date, ilk commit `2026-11-02`, son commit `2026-11-14`, benzersiz takvim günü = **10** (`2026-11-02`, `11-03`, `11-04`, `11-05`, `11-06`, `11-08`, `11-09`, `11-11`, `11-13`, `11-14`).
- **Karar:** `GIT_10_DAY_REQUIREMENT: PASS`. Nihai S10 commit'i pushlandığında gün sayısı korunacak veya yeni takvim günündeyse artacaktır.

## 7. GitHub Actions CI ve Uzak Depo Senkronizasyon Analizi (`K-S10-02`, `K-S10-03`)

- **Uzak Hizalama (`K-S10-02`):** Aşama A başında `main` ve `origin/main` birebir aynı SHA üzerindedir (`8b70de2`, `0 / 0`).
- **CI Durumu (`K-S10-03`):** `.github/workflows/ci.yml` dosyası `main` dalına `push`/`pull_request` ile `./mvnw -B verify` çalıştıracak şekilde tanımlıdır; ancak S05–S09 commit mesajları `[skip ci]` / `[skip actions]` içerdiği için bu commit'lerde GitHub Actions iş akışı **bilinçli olarak atlanmıştır** (`SKIPPED_BY_COMMIT_MARKER`). Dolayısıyla S05–S09 döneminin kalite güvencesi ürün sahibinin yerel `./mvnw clean verify` çalıştırmalarına dayanmaktadır ve S10 nihai push'unun da talep gereği `[skip ci] [skip actions]` ile yapılacağı bilinmektedir.

## 8. Tarihsel S09 ve Nihai S10 Sahip Doğrulama Kanıtları (`VERIFIED_BY_OWNER`)

- **Nihai S10 Sahip Doğrulaması (`2026-11-14 12:39:21–12:40:07+03:00`):** Ürün sahibi tarafından güncel S10 kaynak/doküman ağacı üzerinde çalıştırılan `./mvnw clean verify` sonrası diskteki XML raporlarından doğrulanan sonuçlar:
  - Surefire: **46 sınıf / 366 test PASS** (`0` hata, `0` başarısızlık, `0` atlama — `12:39:21`).
  - Failsafe PostgreSQL 17 Testcontainers: **20 sınıf / 135 test PASS** (`0` hata, `0` başarısızlık, `0` atlama, `0` flake — `failsafe-summary.xml`, `12:40:07`).
  - Toplam S10 Doğrulama: **501 PASS**.
  - `ConcurrentBookingIT`: 5-thread `CountDownLatch` bariyeriyle `2/2` PASS (`0.177 s`).
  - `PerformanceBenchmarkIT`: 50 personel / 20 hasta / 1.000 arka plan randevusu üzerinde `availability-slot-search` (40/40 başarılı, `p95=14.83 ms`), `booking-tx` (24/24 başarılı, `p95=14.40 ms`), `errorRate=0.0000`.
- **Tarihsel S09 Tabanı (`8b70de2`):** Önceki Sprint S09 kapanışında da 366 + 135 = 501 test yeşil geçmiştir (`availability-slot-search p95=21.86 ms`, `booking-tx p95=14.74 ms`). S10 çalışması kodu değiştirmeden teslim kalitesini aynı `501/501 PASS` seviyesinde yeniden kanıtlamıştır.

## 9. Akademik Proje Raporu Üretim Özeti (PDF + HTML)

- **Üretilen Dosyalar:** `deliverables/Clinic_Appointment_System_Final_Report.pdf` ve `docs/08-teslim/report/Clinic_Appointment_System_Final_Report.html`.
- **Yapı:** `docs/08-teslim/03-proje-raporu-iskeleti.md` ile uyumlu Kapak Sayfası (`Burak Coşkun`, öğrenci no / öğretim elemanı alanları görünür biçimde sahip tamamlamasına bırakılmış), ÖZET, İçindekiler, Bölüm 1–8, Kaynakça, 6 gömülü SVG diyagramı, 2 verbatim Java kod kesiti.
- **Görsel Denetim:** Chrome headless baskı sonrası `pdftoppm` ile üretilen 16 sayfanın kilit sayfaları (Kapak, Özet/İçindekiler, Bölüm 1–8, Diyagram ve Tablo sayfaları) görsel olarak incelenmiş, sayfa taşması, kelime bölünmesi ve boş trailing sayfalar giderilmiştir.

## 10. 7 Slaytlık Sunum Paketi Özeti (`.pptx`)

- **Üretilen Dosya:** `deliverables/Clinic_Appointment_System_Final_Presentation.pptx` (16:9 widescreen, tamamen düzenlenebilir OpenXML şekil/metin/resim öğeleri + her slaytta Türkçe sunucu notları).
- **Slayt Dağılımı:** `docs/08-teslim/02-sunum-ve-demo.md` doğrultusunda tam 7 slayt: Slayt 1 Kimlik, Slayt 2 Problem/FR/NFR/Kapsam, Slayt 3 Modüler Monolit & PostgreSQL ERD özeti (`01-modular-monolith-architecture.png`), Slayt 4 OOP & SOLID (`02-domain-class-diagram.png`), Slayt 5 Eşzamanlılık/Durum Makinesi/Güvenlik (`04-appointment-state-machine.png`), Slayt 6 Kalite KPI'ları & 10-Gün Git Grafiği (`06-git-10-day-commits.png`), Slayt 7 5 Dakikalık Demo & Kısıtlar.
- **Görsel Denetim:** `soffice` ile PDF'e ve `pdftoppm` ile 7 PNG'ye dönüştürülüp 7/7 slayt tek tek okunarak sıfır taşma ve tam hizalama doğrulandı.

## 11. Üretilebilir Diyagram Varlıkları

Mermaid `.mmd` kaynakları ve üretime hazır `.svg` vektör dosyaları `docs/08-teslim/diagrams/` altında saklanmaktadır (`01`..`06`). Tüm SVG'ler LibreOffice üzerinden PNG'ye dönüştürülüp etiket yerleşimleri kontrol edilmiş ve kenar taşmaları giderilmiştir.

## 12. 5 Dakikalık Canlı Demo ve Çevrimdışı Hazırlık Özeti

- `docs/08-teslim/S10-live-demo-runbook.md` dosyası `00:00`–`05:00` arası 6 adımlı zaman çizelgesini, 5 `dev` hesabını, deterministik CLI sıfırlama komutunu ve sorun giderme tablosunu içerir.
- **Statik Çevrimdışı Teyit:** `src/main/resources/templates/` ve `clinic.css`/`clinic.js` üzerinde dış CDN/harici URL taramasında `0` dış ağ bağımlılığı doğrulandı (`Bootstrap 5.3.3` yerel `static/` altındadır). İlk kurulum için önceden dolu `.m2` ve Docker PostgreSQL 17 imajı gerekliliği açıkça belirtilmiştir.

## 13. Sözlü Savunma (Oral Defense) Hazırlığı

`docs/08-teslim/S10-oral-defense.md` belgesi, teknoloji seçimi, OOP 4 temel ilkesi, SOLID, immutable Value Object'ler, PostgreSQL 17/Flyway `V1`–`V4`, `[start, end)` matematiği, sıralı `PESSIMISTIC_WRITE` ve deadlock önleme, `@Transactional`/`open-in-view: false`, `ServiceSnapshot`, durum makinesi, append-only tarihçe, CSRF/IDOR/`AccessPolicy`, çok kiracılı kompozit FK, Testcontainers/`ConcurrentBookingIT`/`PerformanceBenchmarkIT`, `DemoEnvironmentGuard`, kısıtlar ve 10-gün Git denetimi dahil **30 soruyu** kaynak kod kanıtlarıyla yanıtlar.

## 14. SABİS Temiz Kaynak Arşivi Analizi (`WP-S10-02`)

- **Komut:** `git archive --format=zip --prefix=clinic-appointment/ -o deliverables/Clinic_Appointment_System_S10_Source.zip HEAD`
- **Sonuç:** Teslim edilen final S10 commit'i üzerinden üretilir; `unzip -tq` `No errors`, `mvnw` dosya izni `-rwxr-xr-x`, yasaklı dosya kontrolü `ZERO_FORBIDDEN_ENTRIES` (`.git/`, `target/`, `.idea/`, `.vscode/`, `.DS_Store`, `.env`, `deliverables/` hariç tutulur). Güncel SHA-256 ve bayt değerleri `deliverables/SHA256SUMS.txt` dosyasındadır.

## 15. Dokümantasyon Senkronizasyonu

S10 durumu ve üretilen belgeler `README.md`, `docs/README.md`, `docs/07-sprintler/README.md`, `docs/07-sprintler/S10.md`, `docs/01-urun/01-gereksinim-matrisi.md`, `docs/01-urun/traceability.csv`, `docs/08-teslim/01-github-commit-takvimi.md`, `docs/08-teslim/02-sunum-ve-demo.md` ve `docs/08-teslim/03-proje-raporu-iskeleti.md` dosyalarına tutarlı biçimde işlenmiştir.

## 16. Statik Hijyen ve Çıktı Bütünlüğü Denetimleri

- `git diff --check`: Temiz (whitespace/conflict hatası yok).
- `git status --short`: Yalnızca izinli `docs/**` ve `.gitignore` değişiklikleri mevcut; `deliverables/` `.gitignore` ile yalıtılmış.
- Tüm ikili ve metin teslimatları boyut ve SHA-256 checksum'larıyla `S10-artifact-manifest.md` içinde kayıt altına alınmıştır.

## 17. Sahip Tarafından Tamamlanması Gereken Eksik Alanlar (Dış Bilgi Gerektirenler)

Depoda doğrulanamayan iki akademik kimlik alanı kasıtlı olarak uydurulmamış, kapak sayfası (PDF), Slayt 1 (`.pptx`) ve SABİS kontrol listesinde görünür biçimde bırakılmıştır:
1. **Öğrenci Numarası** (`[Öğrenci Numarası — Teslim Öncesi Doldurulacak]`)
2. **Dersin Öğretim Elemanı** (`[Dersin Öğretim Elemanı — Teslim Öncesi Doldurulacak]`)

*(Not: Öğrenci Adı Soyadı olan **Burak Coşkun** depo dokümanlarından teyit edilerek işlenmiştir.)*

## 18. Aşama B Release Gate Tamamlanma Kaydı

1. Ürün sahibi `2026-11-14 12:39–12:40` arasında `./mvnw clean verify` çalıştırarak `366/366` Surefire + `135/135` Failsafe = `501 PASS` kanıtını üretmiştir.
2. Tüm S10 belgeleri, PDF raporu ve 7 slayt sunum güncel S10 doğrulama verileriyle senkronize edilmiştir.
3. Final `git diff --check` sonrası `[skip ci] [skip actions]` ile `main` dalına S10 commit/push yapılmıştır.
4. Teslim edilen final revizyondan `deliverables/Clinic_Appointment_System_S10_Source.zip` yeniden üretilmiş ve `deliverables/SHA256SUMS.txt` kilitlenmiştir.

## 19. SABİS Yükleme ve Canlı Sunum Sorumlulukları (Öğrenci)

- Kapak sayfası ve Slayt 1'deki öğrenci no + öğretim elemanı bilgilerini doldurun.
- SABİS portalına `Clinic_Appointment_System_Final_Report.pdf`, `Clinic_Appointment_System_S10_Source.zip` ve GitHub URL (`https://github.com/coskunburak/clinic_Appointment_System-`) yüklemesini gerçekleştirin.
- Canlı sunum gününde Docker PostgreSQL 17 ve `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` akışını kullanarak 5 dakikalık demoyu icra edin.

## 20. Sahip Doğrulama Komut Seti

Ürün sahibinin kendi terminalinde (proje kök dizininde) çalıştırması gereken doğrulama komutları:

```bash
git log --format='%h %as %s' --reverse
git log --format=%as | sort -u | wc -l
./mvnw clean verify
git diff --check
git status --short
```

## 21. Riskler ve Azaltma Önlemleri

| Risk | Etki | Alınan Önlem |
|---|---|---|
| Eksik öğrenci no / hoca adı ile SABİS yüklemesi | Akademik puan kesintisi | Kapak ve Slayt 1'de vurgulu yer tutucu ve `S10-sabis-checklist.md` hatırlatması eklendi. |
| Sunum salonunda internet olmaması | Bootstrap veya harici istek kırılması | Tüm CSS/JS yerel `static/` içinde tutulduğu static tarama ile doğrulandı; `.m2` ve Docker önceden ısındırma prosedürü `S10-live-demo-runbook.md`'de verildi. |
| S09 sonrası S10 dosya düzenlemelerinin ZIP'e girmemesi | Eski kodun paketlenmesi | Mevcut ZIP açıkça S09 `HEAD` adayı olarak işaretlendi; Aşama B'de final commit sonrası yeniden üretilecek. |

## 22. Sonuç ve S10 Kapanış Kararı

Sprint S10 kapsamında 10 tekil günde Git geçmişi (`GIT_10_DAY_REQUIREMENT: PASS`), `2026-11-14 12:39–12:40` tarihli ürün sahibi `./mvnw clean verify` (`501/501 PASS`), üretilebilir diyagramlar, akademik PDF raporu, 7 slayt `.pptx` sunumu, demo/savunma rehberleri ve temiz kaynak ZIP arşivi tamamlanarak GitHub `main` dalına senkronize edilmiştir (`VERIFIED_BY_OWNER`). Öğrencinin yalnızca kimlik alanlarını tamamlayarak SABİS yüklemesini ve canlı savunmayı gerçekleştirmesi kalmıştır.
