# Önerilen proje dizin hiyerarşisi

```text
clinic-appointment/
├── README.md
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/wrapper/
├── .github/workflows/ci.yml
├── compose.yaml
├── docs/                       # bu klasör
├── src/main/java/com/example/clinic/
│   ├── ClinicApplication.java
│   ├── common/{clock,exception,money,security}/
│   ├── identity/{domain,application,infrastructure,web}/
│   ├── organization/{domain,application,infrastructure,web}/
│   ├── catalog/{domain,application,infrastructure,web}/
│   ├── staff/{domain,application,infrastructure,web}/
│   ├── scheduling/{domain,application,infrastructure,web}/
│   └── booking/{domain,application,infrastructure,web}/
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-test.yml
│   ├── db/migration/V1__baseline.sql
│   ├── templates/{auth,patient,staff,admin,fragments}/
│   └── static/{css,js,img}/
└── src/test/java/com/example/clinic/
    ├── domain/
    ├── application/
    ├── integration/
    └── web/
```

## Sorumluluklar ve örnek sınıflar
| Modül | Domain | Application | Web |
|---|---|---|---|
| identity | `UserAccount`, `Role`, `Membership` | `AuthenticationQuery`, `AccessPolicy` | `LoginController` |
| organization | `Organization`, `Branch`, `Department`, `BranchOpeningHours` | `OrganizationAdminService` | `AdminBranchController` |
| catalog | `ServiceDefinition`, `BranchService`, `Money` | `CatalogService` | `AdminServiceController` |
| staff | `StaffProfile`, `StaffAssignment`, `StaffServiceEligibility` | `StaffManagementService` | `StaffAdminController` |
| scheduling | `WeeklyAvailability`, `AvailabilityException`, `SchedulingConstraint` | `AvailabilityQueryService` | `AvailabilityController` |
| booking | `Appointment`, `AppointmentStatus`, `AppointmentStatusHistory` | `BookingService`, `ApprovalService` | `BookingController` |

## Sınır ilkeleri
- `booking` modülü saat seçimini `scheduling` servisinden talep eder; şube ve personeli doğrular, tek transaction'da kaydeder.
- `organization` ve `catalog` değişikliği mevcut randevunun snapshot'ını güncellemez.
- `identity` güvenlik kimliğini taşır; personel profilini iş kimliği olarak `staff` tutar.
- Tablolara uygulama servisleri üzerinden erişilir, direkt controller → repository kısa yolu kabul edilmez.
- Not: paket ağacı hedef mimaridir; implementasyonda erişim sorunları çıkarsa ADR ile güncellenir.
