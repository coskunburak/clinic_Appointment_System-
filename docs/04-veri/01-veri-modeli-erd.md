# İlişkisel veri modeli (ERD)

```mermaid
erDiagram
  ORGANIZATION ||--o{ BRANCH : contains
  ORGANIZATION ||--o{ SERVICE_DEFINITION : owns
  BRANCH ||--o{ DEPARTMENT : contains
  BRANCH ||--o{ BRANCH_OPENING_HOURS : defines
  BRANCH ||--o{ BRANCH_SERVICE : offers
  SERVICE_DEFINITION ||--o{ BRANCH_SERVICE : priced_as
  USER_ACCOUNT ||--o{ MEMBERSHIP : has
  ORGANIZATION ||--o{ MEMBERSHIP : scopes
  USER_ACCOUNT ||--o| PATIENT_PROFILE : patient
  USER_ACCOUNT ||--o| STAFF_PROFILE : employee
  STAFF_PROFILE ||--o{ STAFF_ASSIGNMENT : assigned
  BRANCH ||--o{ STAFF_ASSIGNMENT : hosts
  DEPARTMENT ||--o{ STAFF_ASSIGNMENT : department
  STAFF_ASSIGNMENT ||--o{ STAFF_SERVICE_ELIGIBILITY : performs
  BRANCH_SERVICE ||--o{ STAFF_SERVICE_ELIGIBILITY : eligible
  STAFF_ASSIGNMENT ||--o{ WEEKLY_AVAILABILITY : weekly
  STAFF_ASSIGNMENT ||--o{ AVAILABILITY_EXCEPTION : exception
  PATIENT_PROFILE ||--o{ APPOINTMENT : books
  STAFF_PROFILE ||--o{ APPOINTMENT : serves
  BRANCH_SERVICE ||--o{ APPOINTMENT : service
  BRANCH ||--o{ APPOINTMENT : location
  APPOINTMENT ||--o{ APPOINTMENT_STATUS_HISTORY : history
```

## İlişkilerde tutarlılık
- `branch_service.branch_id` ile `staff_assignment.branch_id` eşleşmeden `staff_service_eligibility` oluşamaz (domain validation; gerekirse kompozit FK).
- `appointment.branch_id`, `branch_service.branch_id` ve o tarihteki `staff_assignment.branch_id` eşleşmelidir.
- `appointment.staff_id` farklı şubelerde çalışabilir; **overlap sorgusu personel ID üzerinde globaldir**.
- Tarihsel appointment, çalışan veya katalog daha sonra pasifleştirilse bile FK ile okunabilir.

## İndeks stratejisi
`appointment(staff_id, start_at_utc, end_at_utc, status)`, `appointment(patient_id, start_at_utc desc)`, `staff_assignment(branch_id, staff_id)`, `weekly_availability(staff_assignment_id, day_of_week)`, `membership(user_id, organization_id, branch_id)`. Ölçmeden gereksiz indeks eklenmez.
