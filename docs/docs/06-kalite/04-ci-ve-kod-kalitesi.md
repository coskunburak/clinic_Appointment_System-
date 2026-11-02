# GitHub Actions / kod disiplini

## CI önerisi
PR ve `main` push olaylarında: `checkout`, `setup-java` (Temurin 21), Maven Wrapper doğrulama, `./mvnw -B verify`; integration testler için Testcontainers'ın desteklendiği runner; raporlama. `main` her zaman derlenebilir tutulur.

## Yapılandırma örneği (taslak)
```yaml
name: Java CI
on: [push, pull_request]
jobs:
  verify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - run: chmod +x mvnw
      - run: ./mvnw -B verify
```
**Uyarı:** workflow action sürümleri yayın sırasında resmî dokümandan yeniden teyit edilmeli; README ve dependency sürümleri `pom.xml` ile uyumlu olmalıdır. Testcontainers CI için Docker erişimi koşuldur.

## Pull request / commit kuralı
- 1 issue → 1 küçük kapsam → kod + test + doküman, tek review.
- Kod isimleri İngilizce, kullanıcı metinleri Türkçe; Java standartları izlenir.
- Kapsam dışı paket ekleme/major dependency upgrade için ADR gerekir.
- `git diff --check`, Maven verify, `.gitignore` ve gizli bilgi kontrolü yapılır.
- Otomatik CI commit'leri 10 ayrı gerçek gün şartının yerine geçmez; gerçek geliştirme kaydı tutulur.

## Commit mesaj formatı
`docs(scope): add acceptance matrix`, `feat(booking): enforce staff slot lock`, `test(security): reject cross-branch approval` gibi açıklayıcı Conventional Commits önerilir.
