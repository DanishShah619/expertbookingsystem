# Graph Report - .  (2026-05-14)

## Corpus Check
- Corpus is ~5,717 words - fits in a single context window. You may not need a graph.

## Summary
- 151 nodes · 368 edges · 24 communities (16 shown, 8 thin omitted)
- Extraction: 98% EXTRACTED · 2% INFERRED · 0% AMBIGUOUS · INFERRED: 6 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Expert Pages|Expert Pages]]
- [[_COMMUNITY_Community 1|Community 1]]
- [[_COMMUNITY_WebSocket & Streams|WebSocket & Streams]]
- [[_COMMUNITY_UI Assets|UI Assets]]
- [[_COMMUNITY_Community 4|Community 4]]
- [[_COMMUNITY_Expert Pages|Expert Pages]]
- [[_COMMUNITY_Auth & Users|Auth & Users]]
- [[_COMMUNITY_Community 7|Community 7]]
- [[_COMMUNITY_Community 8|Community 8]]
- [[_COMMUNITY_Community 9|Community 9]]
- [[_COMMUNITY_Expert Pages|Expert Pages]]
- [[_COMMUNITY_Community 11|Community 11]]
- [[_COMMUNITY_Community 12|Community 12]]
- [[_COMMUNITY_Auth & Users|Auth & Users]]
- [[_COMMUNITY_Community 14|Community 14]]
- [[_COMMUNITY_API Types|API Types]]
- [[_COMMUNITY_Mock Data|Mock Data]]
- [[_COMMUNITY_Auth & Users|Auth & Users]]
- [[_COMMUNITY_Routes & Config|Routes & Config]]
- [[_COMMUNITY_Routes & Config|Routes & Config]]
- [[_COMMUNITY_Routes & Config|Routes & Config]]
- [[_COMMUNITY_Routes & Config|Routes & Config]]

## God Nodes (most connected - your core abstractions)
1. `apiFetch()` - 37 edges
2. `AppShell()` - 14 edges
3. `PageHeader()` - 14 edges
4. `StatusPill()` - 12 edges
5. `formatCurrency()` - 10 edges
6. `appRoutes` - 9 edges
7. `apiRoutes` - 8 edges
8. `ApiRequestOptions` - 8 edges
9. `formatDateTime()` - 8 edges
10. `experts` - 7 edges

## Surprising Connections (you probably didn't know these)
- `Globe Icon` --semantically_similar_to--> `Vercel Platform`  [INFERRED] [semantically similar]
  expertbooking_frontend/public/globe.svg → expertbooking_frontend/README.md
- `File Icon` --semantically_similar_to--> `app/page.tsx`  [INFERRED] [semantically similar]
  expertbooking_frontend/public/file.svg → expertbooking_frontend/README.md
- `Window Browser Icon` --semantically_similar_to--> `app/page.tsx`  [INFERRED] [semantically similar]
  expertbooking_frontend/public/window.svg → expertbooking_frontend/README.md
- `ExpertDetailPage()` --calls--> `formatCurrency()`  [EXTRACTED]
  app/experts/[id]/page.tsx → lib/mock/data.ts
- `CheckoutPage()` --calls--> `formatCurrency()`  [EXTRACTED]
  app/experts/[id]/checkout/page.tsx → lib/mock/data.ts

## Communities (24 total, 8 thin omitted)

### Community 0 - "Expert Pages"
Cohesion: 0.25
Nodes (5): AppShell(), primaryLinks, roleLinks, PageHeader(), expertBookings

### Community 1 - "Community 1"
Cohesion: 0.32
Nodes (11): createExpert(), createSlot(), createSpecialty(), deleteExpert(), deleteSlot(), deleteSpecialty(), getAdminBookings(), getAdminExperts() (+3 more)

### Community 2 - "WebSocket & Streams"
Cohesion: 0.24
Nodes (5): SlotGrid(), ExpertDetailPage(), appRoutes, wsRoutes, formatTime()

### Community 3 - "UI Assets"
Cohesion: 0.18
Nodes (12): Next.js, Globe Icon, Next.js Logo, Vercel Logo, Window Browser Icon, npm run dev, app/page.tsx, create-next-app (+4 more)

### Community 4 - "Community 4"
Cohesion: 0.2
Nodes (9): BookingStatus, CurrencyCode, ExpertCreateRequest, IsoDateTime, PaymentStatus, Role, SlotCreateRequest, SlotStatus (+1 more)

### Community 5 - "Expert Pages"
Cohesion: 0.42
Nodes (5): ExpertCard(), experts, formatCurrency(), specialties, ExpertDto

### Community 6 - "Auth & Users"
Cohesion: 0.25
Nodes (7): getCurrentAuthUser(), ApiRequestOptions, lockSlot(), releaseSlotLock(), apiRoutes, SlotLockResponse, UserDto

### Community 7 - "Community 7"
Cohesion: 0.25
Nodes (7): ExpertListParams, getExpert(), getExperts(), getExpertSlots(), getSpecialties(), SpecialtyDto, TimeSlotDto

### Community 8 - "Community 8"
Cohesion: 0.25
Nodes (6): ApiError, ApiQuery, buildApiUrl(), parseResponseBody(), QueryValue, ApiErrorBody

### Community 9 - "Community 9"
Cohesion: 0.29
Nodes (6): getExpertBookings(), getExpertProfile(), getExpertTodayBookings(), getExpertUpcomingBookings(), ExpertBookingDto, ExpertProfileDto

### Community 12 - "Community 12"
Cohesion: 0.47
Nodes (3): CheckoutPage(), formatDateTime(), slots

### Community 13 - "Auth & Users"
Cohesion: 0.33
Nodes (5): getUserBookings(), getUserPastBookings(), getUserProfile(), getUserUpcomingBookings(), UserProfileDto

### Community 15 - "API Types"
Cohesion: 0.5
Nodes (3): cancelBooking(), getMyBookingsLegacy(), BookingDto

### Community 18 - "Routes & Config"
Cohesion: 0.67
Nodes (3): Breaking Changes Notice, Next.js Documentation, AGENTS Configuration

## Knowledge Gaps
- **25 isolated node(s):** `eslintConfig`, `nextConfig`, `config`, `metadata`, `primaryLinks` (+20 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **8 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `apiFetch()` connect `Community 1` to `Auth & Users`, `Community 7`, `Community 8`, `Community 9`, `Auth & Users`, `API Types`?**
  _High betweenness centrality (0.072) - this node is a cross-community bridge._
- **Why does `BookingDto` connect `API Types` to `Community 1`, `Expert Pages`, `Community 4`, `Auth & Users`?**
  _High betweenness centrality (0.008) - this node is a cross-community bridge._
- **What connects `eslintConfig`, `nextConfig`, `config` to the rest of the system?**
  _25 weakly-connected nodes found - possible documentation gaps or missing edges._