# Çok katmanlı test ve kalite stratejisi

## Test piramidi
| Seviye | Araç | Ne doğrular? | Uygun sprint |
|---|---|---|---|
| Saf unit | JUnit Jupiter + AssertJ, Mockito gerektiğinde | interval, Money, domain state machine, constraint policy | S02/S05/S06/S07 |
| Repository integration | Spring Boot Test + Testcontainers PostgreSQL | JPA mapping, FK, migration, gerçek lock | S01/S06/S09 |
| MVC slice / security | `@WebMvcTest`, Spring Security Test | form validation, role auth, CSRF, redirect | S04/S08 |
| Full integration | `@SpringBootTest` + gerçek PostgreSQL | booking → DB → history, transaction | S06–S09 |
| E2E / UAT | manuel tarayıcı kabul listesi | üç rol, şube, onay, negatif senaryo | S08–S10 |

## Test fixture ve bağımsızlık
- Unit testler DB/Spring context açmaz; test saatini enjekte edilmiş `Clock.fixed` ile sabitler.
- Gerçek eşzamanlılık testleri **H2 ile değil PostgreSQL ile** yapılır; `CountDownLatch`/`CyclicBarrier` ile yarış koşulları tetiklenir.
- Durum geçişleri ve validation parametrik teste dönüştürülür; deterministic seed gerekir.
- Data testleri transaction rollback veya izole şema ile çalışır; test sırasına bağımlı değildir.
- UI testleri rol ve kaynak scope'unu ayrı ayrı kontrol eder.

## Kalite kapıları (Done)
1. Kod biçimi ve derleme geçerli.
2. İlgili unit testler yeşil; işlev kapsamına uygun en az 1 negatif test.
3. Veri migration varsa sıfırdan DB kurulumu testi.
4. Yetki etkisi varsa başka kullanıcı/branch negatif testi.
5. Çakışma etkisi varsa gerçek DB paralellik testi.
6. Dokümantasyon (`FR↔TC↔Sprint`) ve ADR gerekirse güncel.
7. Öğrenci **test komutlarını manuel** çalıştırıp çıktıyı kaydeder; ajan 'PASS' uydurmaz.

## Hedef ölçümler (not verme garantisi değil)
- Core domain ile scheduling/booking package statement coverage hedefi >=80% (tek başına kalite kanıtı değildir).
- Kritik iş kuralları için mutation/branch coverage veya sınır değer analizi.
- `./mvnw verify` geçer, CI başarılı. Unit hızlı; Testcontainers local Docker gerektirir.
- Ödevin son 4 hafta sunumunda internet yoksa bile demo yerelde çalışır.

## Çalıştırma sözleşmesi
Her sprint ajanı yalnızca ilgili testleri de ekler, **çalıştırmaz**. Yanıtta tek, yorumsuz `bash` blok verilir; öğrenci çalıştırır. Öğrenci başarısız çıktı verir, ajan yalnız ilgili kaynaklara döner ve tekrar aynı biçimde komut verir.
