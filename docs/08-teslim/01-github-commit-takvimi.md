# GitHub — 10 Farklı Gün Commit Zorunluluğu ve Nihai Denetim Kaydı (`NFR-006 / TC-042`)

**Ödev Kuralı:** GitHub üzerinde minimum **10 commit, birbirinden farklı takvim günlerinde** yapılmış bulunmalıdır. Aynı gün içinde atılan birden fazla commit tek bir geliştirme günü olarak sayılır. Commit tarihlerini değiştirme/backdate kullanma, yapay tarih üretme ve boş commit kesinlikle yasaktır; hedef gerçek geliştirme günlerini belgelemektir.

---

## 1. Nihai Git Geçmişi Denetimi (`HEAD: 8b70de2`, `origin/main` ile Eşit)

| Metrik | Doğrulanan Değer | Açıklama |
|---|---|---|
| **Hedef Dal ve Uzak Eşitleme** | `main` &harr; `origin/main` (`0 / 0`) | `git rev-parse HEAD`: `8b70de2ef212fe5ffb5c0ae3176b99d661804565` |
| **Toplam Gerçek Commit Sayısı** | **28** | Tümü anlamlı kod veya dokümantasyon değişikliği içerir (**0 boş commit**) |
| **Tarih Bütünlüğü (Author == Committer)** | **28 / 28** | Hiçbir commit'te tarih tahrifi / backdating yoktur |
| **İlk Commit Tarihi** | `2026-11-02` (`cdd87b6`) | `first commit` |
| **Son Commit Tarihi** | `2026-11-14` (`8b70de2`) | `feat(s09): implement demo seed, concurrency stress, performance and regression verification [skip ci] [skip actions]` |
| **Benzersiz Takvim Günü Sayısı** | **10** | `git log --format=%as \| sort -u \| wc -l` = **10** |
| **Kural Uyum Sonucu (`NFR-006 / TC-042`)** | **`GIT_10_DAY_REQUIREMENT: PASS`** | En az 10 farklı gerçek takvim günü şartı eksiksiz sağlanmıştır |

---

## 2. 10 Tekil Takvim Gününe Yayılan Commit Dökümü

| Gün No | Tarih (`YYYY-MM-DD`) | Adet | SHA Aralığı / Kimliği | Sprint &amp; Geliştirme Kapsamı |
|---|---|---|---|---|
| **Gün 1** | `2026-11-02` | 4 | `cdd87b6`, `215a542`, `3b2dd1c`, `23b7d28` | S00: Depo kurulumu, dokümantasyon normalizasyonu, kapsam dondurma ve bağımlılık onarımı |
| **Gün 2** | `2026-11-03` | 1 | `f2004d4` | S01: PostgreSQL 17 Docker, Flyway V1, JPA ve GitHub Actions CI temeli |
| **Gün 3** | `2026-11-04` | 1 | `0c6e369` | S02: Saf OOP domain çekirdeği, Value Object'ler (`Money`, `TimeInterval`) ve strateji kuralları |
| **Gün 4** | `2026-11-05` | 1 | `3825db1` | S03: Kurum, şube, bölüm, hizmet kataloğu ve Flyway V2 |
| **Gün 5** | `2026-11-06` | 1 | `328b297` | S04: Kimlik doğrulama, Spring Security 6, BCrypt-12 ve `AccessPolicy` çok kiracılı güvenlik |
| **Gün 6** | `2026-11-08` | 1 | `072cc88` | S05: Personel yönetimi, hizmet yetkinliği ve müsaitlik motoru (Flyway V3, `[skip ci]`) |
| **Gün 7** | `2026-11-09` | 1 | `ebd0ed4` | S06: Randevu rezervasyonu, sıralı `PESSIMISTIC_WRITE` kilidi ve Flyway V4 (`[skip ci] [skip actions]`) |
| **Gün 8** | `2026-11-11` | 6 | `753826e`, `9dec76e`, `2bfdd58`, `f705835`, `853fe30`, `9c81d86` | S07: Randevu onay yaşam döngüsü, append-only tarihçe, erişim/IT düzeltmeleri (`[skip ci]`) |
| **Gün 9** | `2026-11-13` | 5 | `c84bfd5`, `3f36ec2`, `5923433`, `3bf3f38`, `1fe6b38` | S08: Rol tabanlı Spring MVC ve Thymeleaf arayüzleri ve ilk S08 test doğrulaması (`[skip ci] [skip actions]`) |
| **Gün 10** | `2026-11-14` | 7 | `4827bf6`, `7d48a92`, `9bff1f3`, `1d0f72b`, `9c35715`, `b21ee75`, `8b70de2` | S08 tamamlama (121 IT PASS) + S09 `DemoDataSeeder`, `ConcurrentBookingIT`, `PerformanceBenchmarkIT` (501 PASS) |

---

## 3. Tüm 28 Commit Listesi (`git log --format='%h %as %s' --reverse`)

1. `cdd87b6` `2026-11-02` `first commit`
2. `215a542` `2026-11-02` `first comit`
3. `3b2dd1c` `2026-11-02` `docs`
4. `23b7d28` `2026-11-02` `docs(s00): establish repository foundation, academic scope and fix build dependencies`
5. `f2004d4` `2026-11-03` `feat(s01): establish PostgreSQL, Flyway, JPA and CI foundation`
6. `0c6e369` `2026-11-04` `feat(s02): implement pure domain core and OOP scheduling policies`
7. `3825db1` `2026-11-05` `feat(s03): implement organization, branch, department and medical service catalog`
8. `328b297` `2026-11-06` `feat(s04): implement authentication, spring security, password hashing and multi-tenant authorization`
9. `072cc88` `2026-11-08` `feat(s05): implement staff management, service eligibility and availability engine [skip ci]`
10. `ebd0ed4` `2026-11-09` `feat(booking): implement S06 appointment reservation and concurrency lock [skip ci] [skip actions]`
11. `753826e` `2026-11-11` `feat(s07): implement appointment lifecycle and audit history [skip ci]`
12. `9dec76e` `2026-11-11` `fix(s07): resolve test compilation constructor and injection symbols [skip ci]`
13. `2bfdd58` `2026-11-11` `fix(identity): restore AccessPolicy findById branch lookup and add S07 access tests [skip ci]`
14. `f705835` `2026-11-11` `fix(test): use literal snapshot values in IT fixtures to avoid LazyInitializationException [skip ci]`
15. `853fe30` `2026-11-11` `fix(test): wrap custom clock service instances in transactional proxy for pessimistic locking [skip ci]`
16. `9c81d86` `2026-11-11` `docs(s07): record owner-verified test success and complete S07 milestone [skip ci] [skip actions]`
17. `c84bfd5` `2026-11-13` `feat(s08): implement role-based MVC and Thymeleaf interfaces [skip ci] [skip actions]`
18. `3f36ec2` `2026-11-13` `fix(s08): resolve compilation types in staff query, command and exception form [skip ci] [skip actions]`
19. `5923433` `2026-11-13` `fix(s08): correct TimeSlot constructor invocation in test [skip ci] [skip actions]`
20. `3bf3f38` `2026-11-13` `fix(s08): use valid phone format in PatientBookingQueryServiceTest [skip ci] [skip actions]`
21. `1fe6b38` `2026-11-13` `docs(s08): record owner-verified test success and complete S08 milestone [skip ci] [skip actions]`
22. `4827bf6` `2026-11-14` `feat(s08): implement role-based MVC and Thymeleaf interfaces [skip ci] [skip actions]`
23. `7d48a92` `2026-11-14` `fix(s08): resolve multi-catch subclassing and align test/JSON error contracts [skip ci] [skip actions]`
24. `9bff1f3` `2026-11-14` `fix(s08): fix remaining test compilation errors [skip ci] [skip actions]`
25. `1d0f72b` `2026-11-14` `fix(s08): align admin template layout fragments and timezone test expectation [skip ci] [skip actions]`
26. `9c35715` `2026-11-14` `fix(s08): assert canonical view model attribute in StaffScheduleControllerMvcTest [skip ci] [skip actions]`
27. `b21ee75` `2026-11-14` `docs(s08): record owner-verified test success and complete S08 milestone [skip ci] [skip actions]`
28. `8b70de2` `2026-11-14` `feat(s09): implement demo seed, concurrency stress, performance and regression verification [skip ci] [skip actions]`

---

## 4. Git Kontrol Komutları

```bash
git log --format='%h %as %s' --reverse
git log --format=%as | sort -u | wc -l
git status --short
```
