# Ganesh's ClaimFlow

Ganesh's ClaimFlow (originally named ClaimsFlow) is my take-home project for the Chubb APAC Fullstack Developer assessment. The idea is to put claim submission and claim handling in one place instead of relying on email and spreadsheets.

There are two views in the same Angular app:

- **Claimant:** submit a motor or property claim, check its status, see the history and reply when more information is needed.
- **Claims officer:** pick up a claim, review it, request information, set a reserve, approve or reject it, and mark an approved claim as settled.

I also included a small dashboard showing open claims, officer workload and estimated outstanding exposure.

This is a working **prototype**, not a production claims system. I used AI to help create the initial codebase and have been running, checking and learning the implementation locally. The details of that process are in [AI_JOURNAL.md](AI_JOURNAL.md).

## Tech stack

| Area | Technology |
| --- | --- |
| Frontend | Angular 21, TypeScript, CSS |
| Backend | Java 21, Spring Boot 3.5, REST API |
| Persistence | Spring Data JPA, file-based H2 |
| API documentation | Swagger/OpenAPI |
| Tests | JUnit 5, Spring Boot integration tests with MockMvc |

I kept the backend as **one application** with separate claim and reporting packages. I preferred this over microservices because the main goal was to get the claims workflow right and keep the project easy to run and test. See [architecture.md](docs/architecture.md) for more detail.

## Running it locally

You will need Java 21, Maven, Node.js compatible with Angular 21, and npm. For this project, Node.js 22.12+ from the Node 22 line is a suitable choice.

Check your installed versions:

```bash
java -version
mvn -version
node -v
npm -v
```

Start the backend in one terminal:

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

Then start Angular in a second terminal from the project root:

```bash
cd frontend
npm install
npm start
```

Open **http://localhost:4200**. Angular runs on port 4200 and proxies `/api` requests to Spring Boot on port 8080.

Useful links while the backend is running:

- Claims API: http://localhost:8080/api/claims
- Dashboard API: http://localhost:8080/api/dashboard
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- H2 console (development only): http://localhost:8080/h2-console

The H2 database is stored under `backend/data/` when Spring Boot is started from the `backend` directory. It retains local claims between restarts. The app adds sample claims only if the database is empty. No real personal or claims data should be used in this demo.

For more setup details, see [SETUP_GUIDE.md](SETUP_GUIDE.md).

## How the workflow works

```text
SUBMITTED -> IN_REVIEW -> APPROVED -> SETTLED
                 |
                 +----> REJECTED
                 |
                 +----> NEEDS_INFORMATION -> IN_REVIEW
```

For example, I can create a motor claim from the claimant page and then find it in the officer queue. An officer assigns it, requests more information if needed and makes a decision. Every action is recorded in the claim history.

The backend checks which status changes are allowed. This is important because hiding a button in Angular isn't enough to protect a business rule; someone could still call the API directly.

To try the workflow:

1. Create a MOTOR claim for `demo-customer-001` from **New claim**.
2. Find it under **My claims**. Its first status should be `SUBMITTED`.
3. In **Claims queue**, assign it to `officer-001`.
4. Request more information and send a reply through **My claims**.
5. Approve the claim with a valid amount and reason, then mark it settled.
6. Check the activity history and refresh the dashboard.

For a negative case, try to settle the same claim again using Swagger. The API should reject the invalid transition with HTTP `409 Conflict`.

## A quick tour of the pages

I kept the pages focused on the two user journeys instead of adding a lot of unrelated screens.

| Page | What it does | Main code |
| --- | --- | --- |
| **New claim** | Collects incident details, posts them to the API, and shows the claim number | `new-claim.component.ts` |
| **My claims** | Finds claims by demo customer ID, filters by status, pages through results, shows the timeline, and handles information replies | `my-claims.component.ts` |
| **Claims queue** | Loads paginated/filterable work items and runs officer actions through the API service | `claims-queue.component.ts` |
| **Dashboard** | Requests workload and exposure data from the backend | `dashboard.component.ts` |

A request normally follows this path: **Angular component → `ClaimApiService` → Spring controller → `ClaimService` → JPA/H2 → JSON response → updated UI**. The controller handles HTTP; the service is where I expect the rules to be checked.

## Main APIs

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/claims` | Create a claim |
| GET | `/api/claims` | Legacy claim list: optional `claimantId` and `status` filters; returns an array |
| GET | `/api/claims?page=0&size=10` | Paginated list; can also include `claimantId` and `status` |
| GET | `/api/claims/{id}` | Get claim details |
| GET | `/api/claims/{id}/history` | Get the activity history |
| POST | `/api/claims/{id}/assign` | Assign to an officer |
| POST | `/api/claims/{id}/reserve` | Set estimated reserve |
| POST | `/api/claims/{id}/request-info` | Ask for more information |
| POST | `/api/claims/{id}/information` | Submit the claimant's reply |
| POST | `/api/claims/{id}/decision` | Approve or reject |
| POST | `/api/claims/{id}/settle` | Mark as settled |
| GET | `/api/dashboard` | Get basic workload and exposure totals |

Example `POST /api/claims` request:

```json
{
  "claimantId": "demo-customer-001",
  "claimantName": "Alex Tan",
  "claimType": "MOTOR",
  "market": "MY",
  "incidentDate": "2026-10-18",
  "description": "Rear bumper damaged in a parking incident",
  "claimedAmount": 3500.00
}
```

The six markets in the code (MY, SG, HK, TH, VN and ID) are **demo assumptions**. The assessment mentions six markets but doesn't specify which ones. Currency is derived from the market.

## Code structure

```text
backend/src/main/java/com/chubb/claims/
  claim/api/          Controllers and request/response DTOs
  claim/domain/       Claim, ClaimActivity, statuses, markets
  claim/service/      Claim workflow and business rules
  claim/repository/   JPA repositories
  reporting/          Dashboard calculations
  common/             Exception handling

frontend/src/app/
  core/               API service
  features/claimant/  Submit and track claims
  features/officer/   Queue and dashboard
  app.routes.ts       Page routing
```

## What I have checked so far

- `mvn clean test` initially failed due to incorrect JSON escaping in the generated Java test file. I replaced the affected test file and reran the command. **The second run passed.**
- I ran the frontend locally and checked that the UI was working with the backend. The main claim flow was working in my manual check.
- After the test expansion, `mvn clean test` initially hit a Java runtime mismatch (`release version 21 not supported`). After switching Maven back to JDK 21, the updated tests passed.
- The pagination changes and UI redesign still need their final `mvn clean test`, `npm run build`, and browser checks recorded after applying the ZIPs. Do not treat packaging a change as proof it passed the local build.
- A separate successful `mvn clean package` is not yet recorded.

## Known gaps

I intentionally kept the scope small. These are the main things I would improve next:

- **Security:** there is no real login or role-based access control. Officer and claimant IDs currently come from the UI, so this must not handle real customer information.
- **Kafka:** not implemented. REST handles the current operations; I would use asynchronous events for notifications and external integrations later.
- **Database:** H2 and automatic schema updates are fine for a local demo, but I would use a production database and versioned migrations in a real system.
- **Performance:** claim-list filtering now uses Spring Data JPA database queries and pageable results. The legacy unpaged list still returns all matching claims; dashboard aggregation is still in memory.
- **Insurance rules:** policy checks, attachments, fraud checks and payment processing are outside this prototype. Marking a claim `SETTLED` does not transfer money.
- **Testing:** I expanded backend integration coverage and added separate pagination integration tests. Automated frontend tests and exact financial-reporting assertions can still be improved.

Outstanding exposure is only a **demo estimate**: approved amount if approved, otherwise reserve if entered, otherwise claimed amount. Totals are kept separate by market and currency rather than adding unlike currencies together.

The initial code was generated with AI assistance; subsequent changes include expanded tests, database filtering/pagination and a simpler Angular visual design. The [AI journal](AI_JOURNAL.md) records the debugging and verification that happened, as well as checks still pending. This remains an assessment prototype, not a production claims platform.

## Recent improvements

### More backend tests

I expanded the JUnit 5 + MockMvc integration tests beyond the happy path. They exercise workflow checks such as incorrect officer IDs, invalid decisions, repeated actions and input validation. The tests run against the Spring application context rather than using Mockito-mocked service dependencies.

### Database filtering and pagination

The original list method loaded all claims and filtered them in Java. The updated `ClaimRepository` now applies optional `claimantId` and `status` filters in JPQL, with a Spring Data `Pageable` for paged queries. Pagination is zero-based, defaults to 10 results, and limits the page size to 100.

A paginated result is returned only when `page` is supplied; otherwise `/api/claims` continues to return the old JSON array. This was intentional to avoid breaking consumers of the original endpoint. The page response contains `content`, `totalElements`, `totalPages`, `page`, `size`, `first` and `last`.

The My Claims and Claims Queue screens use the paginated API and show simple Previous/Next navigation and status filtering.

### Simpler interface

The Angular UI changes introduce **Ganesh's ClaimFlow** branding, a simplified top navigation, cleaner typography, consistent cards and filter controls, and better spacing for the claimant and officer views. The CSS is local to the app; I did not add a separate component framework. The brand title is also reflected in the browser tab and favicon.

The UI ZIP was prepared separately from backend changes, so it should be applied, built and reviewed in the browser before its commit is considered finished.
