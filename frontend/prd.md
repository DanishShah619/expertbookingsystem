# PRD v3 — Real-Time Expert Session Booking System
### With Seat Locking + Stripe Payment Integration

**Stack:** Spring Boot 3 · MySQL · WebSockets (STOMP) · Next.js 14 · OAuth2 (Google) · Stripe  
**Auth:** Google OAuth2 (ID token as Bearer JWT) — handled by Next.js frontend  
**Real-time:** STOMP over WebSocket (SockJS fallback)  
**Roles:** ADMIN · EXPERT · USER  
**Seat Lock TTL:** 5 minutes  
**Payment:** Stripe one-time charge per session (price set per expert)  
**Currency:** INR (Indian Rupee — zero-decimal currency in Stripe)

---

## 1. Product Overview

Users browse experts by specialty (Cardiology, Orthopedics, ENT etc.) or search by name, click a slot to lock it for 5 minutes (preventing others from booking it), complete Stripe payment within that window, and the booking is confirmed. If payment is not completed in 5 minutes the lock expires, the slot is released, and all connected clients see it go green again via WebSocket. Each expert has their own session price set by admin. Experts can log in to view their own schedule and patient bookings. Users can view their current and past bookings on their dashboard.

---

## 2. Actors & Roles

| Role | Capabilities |
|-------|-------------|
| ADMIN | Create/edit/delete experts (with price + specialty); create/delete slots; create/delete specialties; view all bookings; view payments |
| USER | Browse experts; filter by specialty; search by name; lock a slot; pay via Stripe; view/cancel own bookings; view booking history |
| EXPERT | View own profile + dashboard stats; view all patient bookings; view upcoming sessions; view today's schedule |

---

## 3. Booking & Payment Flow (Critical Path)

```
User clicks slot
      │
      ▼
POST /api/slots/{id}/lock
  → Slot status: AVAILABLE → LOCKED
  → SeatLock row created (userId, slotId, expiresAt = now+5min)
  → WebSocket broadcast: slot LOCKED
  → Return: { lockToken, expiresAt, stripeClientSecret }
      │
      ▼
Frontend renders Stripe Payment Element
(5-min countdown timer shown to user)
      │
      ▼
User completes Stripe payment in browser
  → Stripe calls POST /api/webhooks/stripe
      │
      ├── payment_intent.succeeded
      │     → Idempotency check (booking already exists? skip)
      │     → Verify lock not expired (refund immediately if expired)
      │     → Slot status: LOCKED → BOOKED
      │     → Booking row created (CONFIRMED)
      │     → Payment audit row created (SUCCEEDED)
      │     → SeatLock row deleted
      │     → WebSocket broadcast: slot BOOKED
      │
      └── payment_intent.payment_failed
            → Slot status: LOCKED → AVAILABLE
            → SeatLock row deleted
            → WebSocket broadcast: slot AVAILABLE
      │
      ▼
  Lock expiry (if user abandons — handled by scheduler)
  → @Scheduled every 60s scans expired SeatLock rows
  → Guard: skip if slot already BOOKED (webhook arrived first)
  → Slot status: LOCKED → AVAILABLE
  → WebSocket broadcast: slot AVAILABLE
  → Stripe PaymentIntent cancelled (via Stripe API)
```

---

## 4. Data Model (MySQL)

```sql
users
  id              BIGINT PK AUTO_INCREMENT
  google_id       VARCHAR(255) UNIQUE NOT NULL
  email           VARCHAR(255) UNIQUE NOT NULL
  name            VARCHAR(255)
  picture         TEXT
  role            ENUM('ADMIN','USER','EXPERT') DEFAULT 'USER'
  created_at      TIMESTAMP

specialties
  id              BIGINT PK AUTO_INCREMENT
  name            VARCHAR(255) UNIQUE NOT NULL   -- e.g. "Cardiology", "ENT"
  slug            VARCHAR(255) UNIQUE NOT NULL   -- e.g. "cardiology", "ent"

experts
  id              BIGINT PK AUTO_INCREMENT
  user_id         BIGINT FK → users.id UNIQUE    -- linked login account
  name            VARCHAR(255) NOT NULL
  title           VARCHAR(255)
  bio             TEXT
  photo_url       TEXT
  specialty_id    BIGINT FK → specialties.id     -- medical field
  tags            VARCHAR(500)                   -- sub-specialization e.g. "Pediatric,Interventional"
  session_price   DECIMAL(10,2) NOT NULL         -- in INR
  currency        VARCHAR(3) DEFAULT 'INR'
  created_at      TIMESTAMP

time_slots
  id              BIGINT PK AUTO_INCREMENT
  expert_id       BIGINT FK → experts.id
  start_time      DATETIME NOT NULL
  end_time        DATETIME NOT NULL
  status          ENUM('AVAILABLE','LOCKED','BOOKED') DEFAULT 'AVAILABLE'
  version         BIGINT DEFAULT 0               -- @Version optimistic lock column
  created_at      TIMESTAMP

seat_locks
  id              BIGINT PK AUTO_INCREMENT
  slot_id         BIGINT FK → time_slots.id UNIQUE
  user_id         BIGINT FK → users.id
  lock_token      VARCHAR(255) UNIQUE NOT NULL   -- UUID, verified in webhook
  payment_intent_id VARCHAR(255)                 -- Stripe PaymentIntent ID
  expires_at      DATETIME NOT NULL              -- now + 5 minutes
  created_at      TIMESTAMP

bookings
  id              BIGINT PK AUTO_INCREMENT
  user_id         BIGINT FK → users.id
  slot_id         BIGINT FK → time_slots.id UNIQUE
  payment_intent_id VARCHAR(255) NOT NULL        -- Stripe PaymentIntent ID
  amount_paid     DECIMAL(10,2) NOT NULL
  currency        VARCHAR(3) NOT NULL
  booked_at       TIMESTAMP
  status          ENUM('CONFIRMED','CANCELLED') DEFAULT 'CONFIRMED'

payments
  id              BIGINT PK AUTO_INCREMENT
  booking_id      BIGINT FK → bookings.id
  stripe_payment_intent_id  VARCHAR(255) UNIQUE NOT NULL
  stripe_charge_id          VARCHAR(255)
  amount          DECIMAL(10,2) NOT NULL
  currency        VARCHAR(3) NOT NULL
  status          ENUM('PENDING','SUCCEEDED','FAILED','REFUNDED')
  created_at      TIMESTAMP
  updated_at      TIMESTAMP
```

---

## 5. REST API Contract

### User
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/user/me | USER | Profile + booking stats |
| GET | /api/user/bookings | USER | All bookings (current + past + cancelled) |
| GET | /api/user/bookings/upcoming | USER | Upcoming confirmed bookings only |
| GET | /api/user/bookings/past | USER | Past bookings only |

### Auth
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/auth/me | Bearer | Current user profile |

### Experts (Public)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/experts | Bearer | All experts (no filter) |
| GET | /api/experts?specialty={slug} | Bearer | Filter by specialty slug |
| GET | /api/experts?search={query} | Bearer | Search by name or specialty |
| GET | /api/experts/{id} | Bearer | Expert detail + price |
| GET | /api/experts/{id}/slots | Bearer | Slots with status + lockExpiresAt |
| GET | /api/experts/specialties | Bearer | All specialties for filter dropdown |

### Expert Account (Expert only)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/expert/me | EXPERT | Own profile + dashboard stats |
| GET | /api/expert/bookings | EXPERT | All patient bookings |
| GET | /api/expert/bookings/upcoming | EXPERT | Upcoming sessions only |
| GET | /api/expert/bookings/today | EXPERT | Today's schedule |

### Admin — Experts
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/admin/experts | ADMIN | All experts |
| POST | /api/admin/experts | ADMIN | Create expert (links userId, specialtyId, price) |
| PUT | /api/admin/experts/{id} | ADMIN | Update expert / price / specialty |
| DELETE | /api/admin/experts/{id} | ADMIN | Delete expert |

### Admin — Slots
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/admin/slots | ADMIN | Create slot for expert |
| DELETE | /api/admin/slots/{id} | ADMIN | Delete slot (AVAILABLE only) |

### Admin — Specialties
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/admin/specialties | ADMIN | All specialties |
| POST | /api/admin/specialties | ADMIN | Create specialty |
| DELETE | /api/admin/specialties/{id} | ADMIN | Delete specialty |

### Admin — Bookings
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | /api/admin/bookings | ADMIN | All bookings |

### Seat Locking + Booking
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/slots/{id}/lock | USER | Lock slot → returns lockToken + Stripe clientSecret |
| DELETE | /api/slots/{id}/lock | USER | Release lock manually |
| DELETE | /api/bookings/{id} | USER | Cancel booking (triggers Stripe refund) |

### Stripe Webhook
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | /api/webhooks/stripe | Stripe-Signature | Handles payment success/failure |

### WebSocket
| Endpoint | Direction | Payload |
|----------|-----------|---------|
| ws://host/ws (SockJS) | Connect | STOMP handshake |
| /topic/experts/{expertId}/slots | Server → Client | SlotUpdateEvent |

---

## 6. SlotUpdateEvent (WebSocket Payload)

```json
{
  "slotId": 42,
  "expertId": 7,
  "status": "LOCKED",
  "startTime": "2025-09-01T10:00:00",
  "endTime": "2025-09-01T11:00:00",
  "lockExpiresAt": "2025-09-01T08:35:00"
}
```

`lockExpiresAt` is only populated when `status = LOCKED`. Null for `AVAILABLE` and `BOOKED`.

---

## 7. Spring Boot Project Structure

```
expert-booking-backend/
├── pom.xml
└── src/
    └── main/
        ├── java/com/danish/patient_booking/
        │   ├── PatientBookingApplication.java      # @EnableScheduling
        │   │
        │   ├── config/
        │   │   ├── SecurityConfig.java             # JWT + role-based rules
        │   │   ├── WebSocketConfig.java            # STOMP broker + SockJS
        │   │   ├── CorsConfig.java                 # allows Next.js origin
        │   │   └── RawBodyCachingFilter.java       # preserves raw bytes for Stripe sig verification
        │   │
        │   ├── controller/
        │   │   ├── AuthController.java             # GET /api/auth/me
        │   │   ├── UserController.java             # GET /api/user/me + bookings
        │   │   ├── ExpertController.java           # public expert + specialty endpoints
        │   │   ├── ExpertAccountController.java    # EXPERT role — own schedule + bookings
        │   │   ├── AdminExpertController.java      # ADMIN — expert CRUD
        │   │   ├── AdminSlotController.java        # ADMIN — slot CRUD
        │   │   ├── AdminBookingController.java     # ADMIN — view all bookings
        │   │   ├── AdminSpecialtyController.java   # ADMIN — specialty CRUD
        │   │   ├── SlotLockController.java         # POST/DELETE /api/slots/{id}/lock
        │   │   ├── BookingController.java          # cancel booking
        │   │   └── StripeWebhookController.java    # POST /api/webhooks/stripe
        │   │
        │   ├── dto/
        │   │   ├── UserDto.java
        │   │   ├── UserProfileDto.java             # profile + booking stats
        │   │   ├── UserBookingDto.java             # booking with expert info + isCancellable
        │   │   ├── ExpertDto.java                  # includes nested SpecialtyDto
        │   │   ├── ExpertProfileDto.java           # expert dashboard profile + stats
        │   │   ├── ExpertBookingDto.java           # booking with patient info + isUpcoming
        │   │   ├── ExpertCreateRequest.java        # includes userId, specialtyId, sessionPrice
        │   │   ├── SpecialtyDto.java               # id + name + slug
        │   │   ├── TimeSlotDto.java                # includes lockExpiresAt
        │   │   ├── SlotCreateRequest.java
        │   │   ├── SlotLockResponse.java           # lockToken + expiresAt + clientSecret
        │   │   ├── BookingDto.java
        │   │   └── SlotUpdateEvent.java            # WebSocket broadcast payload
        │   │
        │   ├── entity/
        │   │   ├── User.java
        │   │   ├── Expert.java                    # has user_id, specialty_id, sessionPrice
        │   │   ├── Specialty.java                 # name + slug
        │   │   ├── TimeSlot.java                  # status + @Version
        │   │   ├── SeatLock.java                  # lockToken + paymentIntentId + expiresAt
        │   │   ├── Booking.java                   # paymentIntentId + amountPaid
        │   │   └── Payment.java                   # Stripe audit record
        │   │
        │   ├── enums/
        │   │   ├── Role.java                      # ADMIN, USER, EXPERT
        │   │   ├── SlotStatus.java                # AVAILABLE, LOCKED, BOOKED
        │   │   ├── BookingStatus.java             # CONFIRMED, CANCELLED
        │   │   └── PaymentStatus.java             # PENDING, SUCCEEDED, FAILED, REFUNDED
        │   │
        │   ├── exception/
        │   │   ├── GlobalExceptionHandler.java    # maps exceptions to HTTP responses
        │   │   ├── ResourceNotFoundException.java # 404
        │   │   ├── SlotNotAvailableException.java # 409
        │   │   ├── LockExpiredException.java      # 410
        │   │   └── InvalidLockTokenException.java # 400
        │   │
        │   ├── repository/
        │   │   ├── UserRepository.java
        │   │   ├── ExpertRepository.java          # findByUserId, findBySpecialtySlug, searchExperts
        │   │   ├── SpecialtyRepository.java       # findBySlug
        │   │   ├── TimeSlotRepository.java        # findByIdWithLock (PESSIMISTIC_WRITE)
        │   │   ├── SeatLockRepository.java        # findAllByExpiresAtBefore, findAllBySlotIdIn
        │   │   ├── BookingRepository.java         # findUpcomingByUserId, findPastByUserId
        │   │   └── PaymentRepository.java         # findByStripePaymentIntentId
        │   │
        │   ├── service/
        │   │   ├── UserService.java               # findOrCreate + profile + booking views
        │   │   ├── ExpertService.java             # public + admin + filter + search
        │   │   ├── ExpertAccountService.java      # expert own schedule + stats
        │   │   ├── SpecialtyService.java          # CRUD + slug generation
        │   │   ├── SlotService.java
        │   │   ├── SeatLockService.java           # lockSlot + releaseLock + race condition guards
        │   │   ├── BookingService.java            # confirm + failure + cancel + refund
        │   │   ├── StripeService.java             # createPaymentIntent (INR zero-decimal) + cancel + refund
        │   │   ├── LockExpiryScheduler.java       # @Scheduled — sweeps expired locks every 60s
        │   │   └── WebSocketNotificationService.java
        │   │
        │   └── security/
        │       ├── JwtAuthConverter.java          # maps Google JWT → DB user → Spring authorities
        │       └── OAuth2UserService.java
        │
        └── resources/
            ├── application.yml
            └── db/migration/
                ├── V1__create_users.sql
                ├── V2__create_specialties.sql
                ├── V3__create_experts.sql
                ├── V4__create_time_slots.sql
                ├── V5__create_seat_locks.sql
                ├── V6__create_bookings.sql
                └── V7__create_payments.sql
```

---

## 8. pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
             https://maven.apache.org/xsd/maven-4.0.0.xsd">

  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.5</version>
    <relativePath/>
  </parent>

  <groupId>com.danish</groupId>
  <artifactId>patient_booking</artifactId>
  <version>0.0.1-SNAPSHOT</version>

  <properties>
    <java.version>21</java.version>
    <stripe.version>25.1.0</stripe.version>
  </properties>

  <dependencies>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
      <groupId>com.mysql</groupId>
      <artifactId>mysql-connector-j</artifactId>
      <scope>runtime</scope>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <dependency>
      <groupId>com.stripe</groupId>
      <artifactId>stripe-java</artifactId>
      <version>${stripe.version}</version>
    </dependency>

    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>

    <dependency>
      <groupId>org.springframework.security</groupId>
      <artifactId>spring-security-test</artifactId>
      <scope>test</scope>
    </dependency>

  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <excludes>
            <exclude>
              <groupId>org.projectlombok</groupId>
              <artifactId>lombok</artifactId>
            </exclude>
          </excludes>
        </configuration>
      </plugin>
    </plugins>
  </build>

</project>
```

---

## 9. application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/expertbooking?useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: yourpassword
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
    open-in-view: false

  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: https://www.googleapis.com/oauth2/v3/certs
          issuer-uri: https://accounts.google.com

stripe:
  secret-key: sk_test_YOUR_STRIPE_SECRET_KEY
  webhook-secret: whsec_YOUR_STRIPE_WEBHOOK_SECRET

app:
  cors:
    allowed-origins: http://localhost:3000
  seat-lock:
    ttl-minutes: 5
  google:
    client-id: YOUR_GOOGLE_CLIENT_ID

logging:
  level:
    com.danish.patient_booking: DEBUG
    org.springframework.security: INFO
    org.hibernate.SQL: DEBUG
```

---

## 10. Key Implementation Details

### 10.1 Google OAuth2 Flow

```
Frontend (Next.js)
      │
      │  1. User clicks "Sign in with Google"
      │  2. @react-oauth/google returns Google ID token
      │  3. All API requests: Authorization: Bearer <google_id_token>
      ▼
Spring Boot (Resource Server)
      │
      │  4. JwtAuthConverter.convert(jwt)
      │     → findOrCreateUser(googleId, email, name, picture)
      │     → first login: INSERT user row, role = USER
      │     → returning: UPDATE name/picture
      │     → returns ROLE_USER / ROLE_EXPERT / ROLE_ADMIN
      ▼
SecurityConfig rules applied per endpoint
```

### 10.2 Specialty Filter Flow

```
GET /api/experts/specialties
→ returns all specialties for dropdown:
  [{ id:1, name:"Cardiology", slug:"cardiology" }, ...]

User selects "Cardiology"
→ GET /api/experts?specialty=cardiology
→ ExpertRepository.findBySpecialtySlug("cardiology")
→ returns only cardiologists

User types "Kumar" in search bar
→ GET /api/experts?search=kumar
→ ExpertRepository.searchExperts("kumar")
→ matches name OR specialty name OR tags
```

### 10.3 INR Stripe Amount Handling

```java
// INR is zero-decimal in Stripe
// ₹500 → send 500, NOT 50000
private long toStripeAmount(BigDecimal amount, String currency) {
    Set<String> zeroDecimal = Set.of("INR", "JPY", "KRW", ...);
    if (zeroDecimal.contains(currency.toUpperCase())) {
        return amount.longValue();   // ₹500 → 500
    }
    return amount.multiply(BigDecimal.valueOf(100)).longValue();
}
```

### 10.4 SeatLockService — Race Condition Defence

```java
@Transactional
public SlotLockResponse lockSlot(Long slotId, String googleId) {
    // PESSIMISTIC_WRITE — DB-level serialisation
    TimeSlot slot = slotRepo.findByIdWithLock(slotId)...
    // Status check — rejects if not AVAILABLE
    // Duplicate lock check — prevents same user locking twice
    // Stripe PaymentIntent created
    // SeatLock persisted
    // WebSocket broadcast → slot turns AMBER
}
```

### 10.5 Stripe Webhook Raw Body

```java
// RawBodyCachingFilter preserves exact bytes
// Stripe signature verification requires unmodified raw body
byte[] rawBody = ((CachedBodyHttpServletRequest) request).getRawBody();
Event event = Webhook.constructEvent(new String(rawBody), sigHeader, webhookSecret);
```

### 10.6 BookingService — Idempotency + Race Condition

```java
@Transactional
public void confirmBooking(String paymentIntentId) {
    // 1. Idempotency — Stripe retries for 3 days, ignore duplicates
    if (bookingRepo.existsByPaymentIntentId(paymentIntentId)) return;
    // 2. Lock expired before webhook → refund immediately
    if (lock.getExpiresAt().isBefore(LocalDateTime.now())) {
        stripeService.refundPaymentIntent(paymentIntentId);
        return;
    }
    // 3. slot → BOOKED, Booking created, Payment created, SeatLock deleted
    // 4. WebSocket broadcast → slot turns GREY
}
```

### 10.7 LockExpiryScheduler

```java
@Scheduled(fixedDelay = 60_000)
@Transactional
public void expireStaleLocks() {
    // Per-lock try/catch — one failure does not block others
    // Guard: skip if slot already BOOKED (webhook arrived first)
    // slot → AVAILABLE, Stripe cancel, SeatLock deleted, WebSocket broadcast
}
```

### 10.8 Expert Account Linking

```java
// Admin creates expert → links to existing user account
// User must have signed in with Google at least once
// Their role is automatically upgraded: USER → EXPERT
user.setRole(Role.EXPERT);
userRepo.save(user);
// JwtAuthConverter picks up ROLE_EXPERT on next request
```

### 10.9 SecurityConfig Rules

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/ws/**").permitAll()
    .requestMatchers("/api/webhooks/stripe").permitAll()
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/expert/**").hasRole("EXPERT")
    .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
    .anyRequest().authenticated()
)
```

---

## 11. Next.js Frontend Structure

```
expert-booking-frontend/
├── app/
│   ├── layout.tsx
│   ├── page.tsx
│   ├── login/page.tsx
│   ├── experts/
│   │   ├── page.tsx                        # Expert listing + specialty filter + search bar
│   │   └── [id]/
│   │       ├── page.tsx                    # Expert detail + slot grid
│   │       └── checkout/
│   │           └── page.tsx               # Stripe Payment Element + countdown
│   ├── my-bookings/page.tsx               # All / Upcoming / Past tabs
│   ├── dashboard/page.tsx                 # User dashboard + stats
│   └── admin/
│       ├── experts/page.tsx
│       ├── slots/page.tsx
│       └── specialties/page.tsx
│   └── expert/
│       ├── dashboard/page.tsx             # Expert dashboard + stats
│       ├── bookings/page.tsx              # All patient bookings
│       └── schedule/page.tsx             # Today + upcoming sessions
│
├── components/
│   ├── ExpertCard.tsx
│   ├── SpecialtyFilter.tsx               # Dropdown — calls /api/experts/specialties
│   ├── ExpertSearchBar.tsx               # Search — calls /api/experts?search=
│   ├── SlotGrid.tsx                      # green=AVAILABLE, amber=LOCKED, grey=BOOKED
│   ├── SlotCountdownBadge.tsx            # "held for X:XX" on LOCKED slots
│   ├── CheckoutForm.tsx                  # Stripe Payment Element wrapper
│   ├── BookingCard.tsx                   # shows isCancellable for cancel button
│   ├── UserStatsCard.tsx                 # total/upcoming/completed/cancelled
│   ├── ExpertStatsCard.tsx               # total/upcoming/completed
│   └── Navbar.tsx
│
├── hooks/
│   ├── useSlotWebSocket.ts              # STOMP subscription → updates slot state
│   ├── useAuth.ts
│   └── useLockCountdown.ts             # counts down 5 min, fires expiry callback
│
├── lib/
│   ├── api.ts                           # Axios + Bearer token interceptor
│   ├── wsClient.ts                      # SockJS + STOMP client
│   └── stripe.ts                        # loadStripe(publishableKey)
│
└── types/
    ├── expert.ts                        # includes specialty: SpecialtyDto
    ├── specialty.ts                     # id, name, slug
    ├── slot.ts                          # status + lockExpiresAt
    ├── booking.ts                       # UserBookingDto shape
    ├── expertBooking.ts                 # ExpertBookingDto shape
    └── slotUpdateEvent.ts
```

---

## 12. Step-by-Step Build Order

### Phase 1 — Foundation
1. Create MySQL DB: `CREATE DATABASE expertbooking;`
2. Use corrected `pom.xml` above (Spring Boot 3.4.5)
3. Configure `application.yml`
4. Create all enums first (Role, SlotStatus, BookingStatus, PaymentStatus)
5. Create all entity classes in order: User → Specialty → Expert → TimeSlot → SeatLock → Booking → Payment
6. Run once — Hibernate DDL generates all tables
7. Create all repository interfaces

### Phase 2 — Security
8. Implement `JwtAuthConverter` — maps Google JWT → DB user → Spring authorities
9. Implement `SecurityConfig`
10. Implement `UserService.findOrCreateUser()` — upserts user on every first login
11. Test `GET /api/auth/me` with real Google ID token in Postman

### Phase 3 — Specialties + Experts
12. Implement `SpecialtyService` + `AdminSpecialtyController`
13. Seed specialties via SQL
14. Implement `ExpertService` + `ExpertController` + `AdminExpertController`
15. Test specialty filter: `GET /api/experts?specialty=cardiology`
16. Test search: `GET /api/experts?search=kumar`

### Phase 4 — User + Expert Account Controllers
17. Implement `UserController` + booking methods in `UserService`
18. Implement `ExpertAccountService` + `ExpertAccountController`
19. Test expert account linking — admin creates expert, user logs in with EXPERT role

### Phase 5 — Slots
20. Implement `AdminSlotController`
21. Test slot creation and deletion guard (AVAILABLE only)

### Phase 6 — WebSockets
22. Implement `WebSocketConfig`
23. Implement `WebSocketNotificationService`
24. Test subscription with STOMP browser client

### Phase 7 — Seat Locking
25. Implement `SeatLockService.lockSlot()` with `PESSIMISTIC_WRITE`
26. Implement `SlotLockController`
27. Implement `LockExpiryScheduler` with `@Scheduled`
28. Add `@EnableScheduling` to main class
29. Test: lock a slot, wait 5+ min, confirm release + WebSocket fires

### Phase 8 — Stripe
30. Create Stripe account → get test keys
31. Implement `StripeService` (INR zero-decimal amount handling)
32. Wire into `SeatLockService.lockSlot()`
33. Implement `RawBodyCachingFilter`
34. Implement `StripeWebhookController` with signature verification
35. Install Stripe CLI: `stripe listen --forward-to localhost:8080/api/webhooks/stripe`
36. Implement `BookingService` (idempotency + expiry guard + confirm + failure + cancel)
37. Test full flow: lock → Stripe test card → webhook → BOOKED + WebSocket

### Phase 9 — Frontend
38. Bootstrap Next.js 14 app
39. Install: `@react-oauth/google axios @stomp/stompjs sockjs-client @stripe/stripe-js @stripe/react-stripe-js`
40. Implement `useAuth` + Login page
41. Implement `api.ts` with Bearer interceptor
42. Implement `wsClient.ts` + `useSlotWebSocket`
43. Implement `SpecialtyFilter` + `ExpertSearchBar` components
44. Implement Expert Listing page with filter + search
45. Implement Expert Detail + slot grid with WebSocket updates
46. Implement Checkout page with Stripe Payment Element + countdown
47. Implement User Dashboard + My Bookings (All / Upcoming / Past tabs)
48. Implement Expert Dashboard + Schedule pages
49. Implement Admin pages (specialties, experts with userId linking, slots)

### Phase 10 — Hardening
50. Add idempotency guard in `confirmBooking()`
51. Add DB unique constraint on `seat_locks.slot_id`
52. Write integration test for concurrent lock scenario
53. Write integration test for lock expiry + re-availability
54. Set up Stripe webhook in dashboard for production

---

## 13. Stripe Setup Checklist

1. Create account at https://stripe.com → Developers → API Keys
2. Copy **Publishable key** → `NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY` in Next.js `.env.local`
3. Copy **Secret key** → `stripe.secret-key` in `application.yml`
4. Install Stripe CLI and run: `stripe listen --forward-to localhost:8080/api/webhooks/stripe`
5. Copy webhook signing secret → `stripe.webhook-secret` in `application.yml`
6. Use test card: `4242 4242 4242 4242`, any future date, any CVV
7. For production: add endpoint in Stripe Dashboard → `payment_intent.succeeded` + `payment_intent.payment_failed`

---

## 14. Slot Status Colour Guide (Frontend)

| Status | Colour | User Action | What Others See |
|--------|--------|-------------|-----------------|
| AVAILABLE | Green | Click to lock | Available |
| LOCKED | Amber | — | "Held — available in X:XX" (countdown from `lockExpiresAt`) |
| BOOKED | Grey | — | Booked |

---

## 15. Race Condition Prevention — Layers of Defence

| Layer | Mechanism | Protects Against |
|-------|-----------|-----------------|
| DB level | `UNIQUE` on `seat_locks.slot_id` | Two concurrent inserts for same slot |
| JPA level | `@Lock(PESSIMISTIC_WRITE)` on slot fetch | Concurrent lock requests in same JVM |
| Entity level | `@Version` on `TimeSlot` | Stale updates from different threads |
| Business level | Status check (`!= AVAILABLE` → reject) | Application-level guard |
| Duplicate lock check | `findBySlotIdAndUserId` before insert | Same user locking twice (two tabs) |
| Webhook level | Idempotency check before confirming | Stripe duplicate webhook delivery |
| Expiry guard | `lock.getExpiresAt().isBefore(now)` | Payment arriving after lock expired |
| Scheduler guard | Skip if slot already `BOOKED` | Scheduler + webhook firing simultaneously |

---

## 16. Seed Data

```sql
INSERT INTO specialties (name, slug) VALUES
('Cardiology',       'cardiology'),
('Orthopedics',      'orthopedics'),
('ENT',              'ent'),
('Dermatology',      'dermatology'),
('Neurology',        'neurology'),
('Pediatrics',       'pediatrics'),
('General Medicine', 'general-medicine'),
('Psychiatry',       'psychiatry'),
('Ophthalmology',    'ophthalmology'),
('Gynecology',       'gynecology');
```

---

## 17. Environment Variables

```bash
# Backend (application.yml)
SPRING_DATASOURCE_URL=jdbc:mysql://prod-db:3306/expertbooking
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Frontend (.env.local)
NEXT_PUBLIC_API_URL=https://api.yourdomain.com
NEXT_PUBLIC_WS_URL=wss://api.yourdomain.com/ws
NEXT_PUBLIC_GOOGLE_CLIENT_ID=...
NEXT_PUBLIC_STRIPE_PUBLISHABLE_KEY=pk_live_...
```      please go through my codebase and vet verify my current codebase against the prd, note down errors or deviations and let me know