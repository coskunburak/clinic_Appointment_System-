# Sprint çalıştırma şablonu (token-ekonomik)

```text
Continue KlinikRandevu from the currently checked-out repository state.
Implement ONLY sprint SXX as specified in docs/07-sprintler/SXX.md.
Read docs/README.md, the relevant FR/TC rows, and only the domain/architecture files named by SXX. Discover existing relevant classes using targeted filenames before opening source. Do not re-read the entire docs folder or unrelated modules.
Inspect git status/branch and protect dirty changes. Implement necessary Java entities, application services, repositories, controllers/templates, migrations and tests with minimal diffs.
Enforce OOP course requirements: inheritance, encapsulation, polymorphism, abstraction where meaningful. Never add irrelevant packages or frameworks.
I run tests MANUALLY. Do not run Maven tests or app startup, do not commit or push. Give exactly one code block containing all root-relative test commands, no comments; report tests NOT_RUN until I provide output.
Report baseline, FR/TC coverage, changed files, implementation summary, risks and the single command block. No fabricated pass claims.
```

## Sprint özelleştirme
`SXX` yerine ilgili sprint (`S00..S10`). Sprint S06'da `docs/03-domain/02-randevu-algoritmasi.md` mutlaka okunur; S04'te `docs/03-domain/04-rol-ve-yetki-matrisi.md`, S08'de `docs/05-arayuz-api/...` önemlidir.

## Hata düzeltme devam promptu
```text
The owner ran your previous one-block commands and pasted the failure output below. Do not inspect unrelated files. Diagnose the first root-cause failure, make minimal safe fixes with regression coverage, keep dirty files intact, do not run tests, and respond with exactly one new comment-free bash test block and status AWAITING_OWNER_TEST.
[PASTE LOG HERE]
```
