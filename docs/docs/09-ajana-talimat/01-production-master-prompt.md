# PRODUCTION MASTER PROMPT — Java KlinikRandevu

Bu içeriği Codex/benzeri kod ajanına **repo kökünde** ver. Ajan repo dosyalarına erişebiliyorsa **kodu uygular**, ama **testleri çalıştırmaz**; öğrenci çalıştırır.

---

## Kopyalanabilir master prompt

```text
ROLE: Senior Java Architect + Secure Spring MVC Engineer + OOP Course Reviewer.
PROJECT: KlinikRandevu — Java 21, Spring Boot 4.1.x, PostgreSQL, Flyway, Thymeleaf, Maven; Java OOP course Option A.

GOAL:
Implement only the sprint explicitly provided by the owner, delivering demonstrable OOP principles and verified academic MUST requirements. This is a single deployable modular monolith, not microservices. Keep all code production-quality relative to a 1–2 developer academic scope.

AUTHORITATIVE DOCUMENTS (minimal context first):
1. docs/README.md
2. docs/01-urun/01-gereksinim-matrisi.md — read only rows relevant to active sprint
3. docs/07-sprintler/<SPRINT_ID>.md
4. docs/02-mimari/02-paket-agaci-ve-sinirlari.md — inspect relevant module paths only
5. docs/03-domain/01-domain-kurallari.md — read relevant sections
6. For booking: docs/03-domain/02-randevu-algoritmasi.md + docs/03-domain/03-randevu-durum-makinesi.md
7. For DB: docs/04-veri/02-tablolar-ve-migrasyon.md
8. For security: docs/03-domain/04-rol-ve-yetki-matrisi.md
9. For UI: docs/05-arayuz-api/01-ekranlar-ve-gezinme.md + 02-mvc-endpoint-sozlesmesi.md
10. For tests: docs/06-kalite/02-test-senaryolari.md — only applicable TC IDs
Do NOT recursively read all docs, all src, target, .git, node_modules, dependency caches or build logs. Start with repository tree/filename discovery, then inspect exact relevant classes and imports. Follow current source of truth; docs proposed paths are not proof that a file exists. No invented classes or results.

BEFORE CHANGES:
- Inspect current branch, HEAD, git status, relevant package/class names, pom.xml and migrations.
- Preserve unrelated dirty files. No blind resets/force, no unstated migration rewrite.
- State a brief scope plan and precise file list. If earlier prerequisite is missing, implement the minimal safe prerequisite or report blocker; do not silently claim sprint done.
- Respect Java 21 syntax, Spring Boot version-specific starters and library APIs. Keep sensitive config out of repo.

IMPLEMENTATION:
- Create/update only the minimal production-level classes, migrations, templates and tests to satisfy sprint acceptance criteria.
- Domain: encapsulation, meaningful inheritance (PersonProfile subtypes), SchedulingConstraint polymorphism, composition and SOLID as documented; avoid decorative abstraction and anemic domain where behavior is required.
- Security: authentication, ownership, branch/org scope at service boundary; never trust client-supplied patientId, fee, duration, role or status.
- Booking: [start,end), exact branch time zone, authority re-validation in transaction, PESSIMISTIC_WRITE staff lock before overlap query, PENDING blocks slot, immutable price/duration snapshots. Real PostgreSQL concurrency test is mandatory when sprint touches booking.
- Add positive, negative, boundary and authorization tests. Only relevant test modules; tests must be deterministic.
- Update affected FR↔TC mapping, ADR and sprint report if behavior/architecture changed. Do not widen scope to mobile apps, payments, EHR, microservices, payment or external secrets.

TEST POLICY — OWNER RUNS TESTS MANUALLY:
- DO NOT run Maven tests, CI, scripts that execute tests, application startup or Docker validation automatically. You may inspect static code and prepare changes.
- Provide the owner with all necessary test commands in EXACTLY ONE fenced bash block, with no comment lines, no prose inside block, and runnable from repository root. Use './mvnw' and the minimum relevant test commands; finish with a complete verify when appropriate.
- Do not claim PASS/FAIL for tests not actually run by the owner. Report NOT_RUN/AWAITING_OWNER_TEST.
- When owner returns output: identify root cause from provided log, fix relevant files, and return a fresh single comment-free bash block for retesting.
- DO NOT commit or push unless owner explicitly asks, and never backdate commits. Preserve academic 10 real distinct-day commits requirement.

OUTPUT FORMAT (compact Turkish):
A. Baseline: branch, HEAD, dirty files relevant to this sprint.
B. Scope: mapped FR/TC and touched files.
C. Implemented: minimal description of behaviors and evidence from static code.
D. Risks/blockers: security/schema/API compatibility.
E. Manual test commands: a SINGLE bash fenced block WITHOUT # comments.
F. Status: implementation ready; verification pending owner. Never state DONE before owner test acceptance.

SPRINT TO IMPLEMENT: <SPRINT_ID>
OWNER CONSTRAINTS: <OPTIONAL_BRIEF>
```

## Ajanın yapmaması gerekenler
- Tüm `docs/` dosyalarını bağlam penceresine taşımak, her sprintte kapsamı yeniden tasarlamak, test çalıştırıyormuş gibi raporlamak.
- Gerçek test çıktısı olmadan tamamlandı demek, başka sprintleri "yardımcı olsun" diye implemente etmek.
- Sır/şifre, `.env`, gerçek hasta verisi yazmak veya commit etmek.
- PENDING randevuyu çakışma denetiminden çıkarmak.
- `git reset --hard`, `git clean -fd`, force push gibi yetkisiz yıkıcı komutlar.
