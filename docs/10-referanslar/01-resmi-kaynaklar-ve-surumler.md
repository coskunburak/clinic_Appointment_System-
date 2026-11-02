# Resmî teknik kaynaklar ve sürüm notu

**Araştırma tarihi:** 2026-11-02. Başlangıçta sürümler güncel görünse de `pom.xml` yazılmadan önce Spring Initializr/resmî docs ile kesin bağımlılık isimleri teyit edilmelidir.

| Teknoloji / konu | Önerilen sürüm veya doküman | URL |
|---|---|---|
| JDK | Java 21 LTS (Temurin veya uygun OpenJDK dağıtımı) | https://www.oracle.com/java/technologies/downloads/ |
| Spring Boot | 3.4.2 resmî referans / Java 21 uyumluluğu | https://docs.spring.io/spring-boot/system-requirements.html |
| Boot starter isimleri | Boot 3 `spring-boot-starter-web`, validation, Thymeleaf, security, data-jpa | https://docs.spring.io/spring-boot/reference/using/build-systems.html |
| Boot test | Spring Boot Test / MVC slice tests | https://docs.spring.io/spring-boot/reference/testing/ |
| PostgreSQL | 17 dokümantasyonu, row locks | https://www.postgresql.org/docs/17/explicit-locking.html |
| Java JDK API | `java.time`, `java.math.BigDecimal` | https://docs.oracle.com/en/java/javase/21/docs/api/ |
| Spring Security | authentication, authorization, method/URL security | https://docs.spring.io/spring-security/reference/ |
| Flyway | SQL migration referansı | https://documentation.red-gate.com/fd |
| Thymeleaf | Spring+Thymeleaf reference | https://www.thymeleaf.org/documentation.html |
| Testcontainers | PostgreSQL module / Java testing | https://java.testcontainers.org/modules/databases/postgres/ |
| Maven | official build user guide | https://maven.apache.org/guides/ |
| OWASP ASVS | 5.0.0 stable güvenlik yaklaşımı | https://asvs.owasp.org/ |

## Lisans / dağıtım notu
Java 21'in önceki LTS olması ve Oracle JDK dağıtım/lisans koşullarının sürüm bazında değişmesi nedeniyle, ders projesinde uygun açık OpenJDK dağıtımı (ör. Eclipse Temurin) seçmek pratik olabilir; sürümü ve lisansı yeniden kontrol et. Oracle'ın resmî açıklaması: https://blogs.oracle.com/java/jdk-21-approaches-end-of-permissive-license .

## Tasarım notu
Gereksinimde dil/teknoloji serbest olduğu için Java 21 ve Spring MVC bilinçli tercihtir; öğretim elemanı belirli JDK versiyonu zorunlu kılarsa değiştirilebilir.
