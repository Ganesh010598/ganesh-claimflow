# AI Working Journal

**Project:** ClaimsFlow — Chubb APAC take-home assessment  
**Date:** 21 October 2026

I'm keeping these notes in the order I worked through the project. The assessment specifically asked for an AI working journal, so I've included what I asked for, where AI helped, what I actually tested, and things I still want to improve. This isn't meant to be a polished report.

## 1. Working out the scope

I started by asking ChatGPT to break the assessment down into something achievable within the time limit. There were a lot of possible features, but the two main journeys were clear: a customer needs to report and follow up on a claim, and an officer needs to manage and decide claims.

I wanted the backend to carry the business rules, not just accept any status sent by the frontend. AI suggested a modular monolith with Spring Boot and an Angular application. That felt appropriate for a small exercise because running several services would add a lot of setup work. I kept H2 for a local database and REST for actions needing an immediate response. Kafka came up as an option for future notifications but isn't implemented.

I asked AI for a complete starting codebase and then worked on getting it running and understanding the actual code. The first implementation was AI-generated; my subsequent work has been environment setup, debugging, manual testing and reviewing the design before making my own changes.

## 2. Local environment and the first backend run

I checked the versions installed on my Mac. I initially had Java 17 and Node 12, while the project was configured for Java 21 and a much newer Node version. Maven was already available. I used the installation guidance to get the environment into a usable state.

My first `mvn clean test` was not successful. It compiled the main backend classes, but failed when compiling `ClaimsIntegrationTest.java` with **62 errors**. Most of them were pointing to the same few lines.

After sharing the terminal output with AI, the issue turned out to be JSON strings written in Java without escaped quotation marks. One example was:

```java
// Wrong
.content("{"officerId":"officer-001"}")

// Correct
.content("{\"officerId\":\"officer-001\"}")
```

I replaced the generated test file with the corrected version and reran `mvn clean test`. The next run returned **BUILD SUCCESS**.

**What I took from this:** when a compiler reports dozens of errors, it is worth looking for a common root cause instead of trying to fix every line individually. I also learned not to assume generated tests are valid just because the application code looks reasonable.

## 3. Getting Angular installed

I hadn't used Angular before, so even getting the tooling running was a learning step. My first `npm install` failed with `Cannot read properties of null (reading 'edgesOut')`. I also tried upgrading npm globally, but macOS rejected it with an `EACCES` permission error.

I asked for help interpreting those errors rather than changing the application code. I followed the suggested local-installation troubleshooting and eventually got the frontend running. I didn't note which exact npm command resolved it, so I'm not guessing here.

The biggest Angular concepts I needed to understand were `@Component`, templates, `ngModel`, `inject()`, `HttpClient`, routing and `subscribe()`.

## 4. How I worked through the pages

I tested the main browser flow and then asked AI to explain how each screen connects to the backend. These are my notes from that walkthrough, not a claim that I wrote each page manually from scratch.

### New claim

This is the simplest place to start because it shows the full request path. The form holds a `CreateClaimRequest` object. Angular's `[(ngModel)]` keeps the fields and that object in sync. When Submit is clicked, the component calls `ClaimApiService.create()`, which makes a POST request to `/api/claims`.

Spring Boot validates the request, creates the claim and history record, then sends back a claim number. The component uses the response to show the success state. I want to revisit the validation messages so the user gets clearer feedback for invalid dates and amounts.

### My claims

This page retrieves claims using a demo customer ID. A selected claim shows the details and activity timeline. When an officer asks for more information, this page displays the request and allows a customer response.

One thing I checked conceptually is that the customer reply goes through an API action; the frontend is not supposed to change the claim status itself. Real claim ownership verification is still missing, so this remains a demo-only screen.

### Claims queue

This is where most of the workflow actions are. The page loads claims, allows selection, and calls separate APIs for assignment, reserves, information requests, decisions and settlement.

The Angular component has one `execute()` helper for handling action responses, loading state and errors. I can see why this avoids repeating similar subscription code. I also want to refactor this component because its inline template is long and will get harder to maintain as more actions are added.

### Dashboard

The frontend asks `/api/dashboard` for its data. The actual calculations are in `DashboardService` on the backend. The dashboard distinguishes open claims, work assigned to officers and estimated exposure by market/currency.

I especially wanted to understand why exposure is not simply the sum of all claimed amounts. The demo first uses the approved amount for an approved claim, otherwise a reserve if available, and otherwise the claimed amount as a fallback. That is useful for the prototype, but not a proper insurance reserving calculation.

## 5. What I tested

I reported that the frontend and main user flow were working. The workflow I checked included creating a claim, finding it in the claims list, moving it through officer actions, and looking at the resulting status/history. The backend tests also passed after correcting the Java strings.

| Check | What I know so far |
| --- | --- |
| First `mvn clean test` | Failed due to invalid Java test strings |
| `mvn clean test` after correction | Passed (`BUILD SUCCESS`) |
| Angular application | Started locally and working in the browser |
| End-to-end claim flow | Manually checked; looked good |
| `npm run build` | Not separately recorded yet |
| `mvn clean package` | Not separately recorded yet |
| Authentication / Kafka / payments | Not implemented |

I don't want to mark a test as complete just because a screen renders. Before submitting, I'll run the standalone build commands and record the actual results.

## 6. Problems I want to explore next

These are **realistic engineering scenarios I want to reproduce or test**, not errors I have already encountered. I'll write a separate result after actually trying each one.

**A. Form binding and validation.** If a field doesn't update the request object, I would check its `ngModel` binding, input `name`, and `FormsModule` import. My planned improvement is to make required-field and invalid-amount feedback more obvious.

**B. A status that looks stale after an officer action.** Even if the backend saves a decision, the page must update its selected claim and history. I want to test the response handling in `execute()`, including a second click while the request is in progress.

**C. Invalid status changes through the API.** A hidden or disabled button is not a security control. I want explicit tests for attempts to decide a claim before assignment or settle a rejected claim, and for incorrect officer IDs.

**D. A claims queue with more records.** Right now the backend loads records and filters in memory. I want to move filtering to repository queries and add pagination, then check that the Angular page still shows the right claims.

**E. Exposure across currencies.** The code already groups market/currency amounts, but the current dashboard test mostly checks response shape. I want a test that creates known MYR and SGD claims and verifies their totals exactly.

**F. Two officers updating the same claim.** The entity has a `@Version` field for optimistic locking. I want to test a stale update and see how the API returns a useful conflict rather than silently overwriting data.

## 7. What I am keeping, and what I would change later

I'm keeping the modular backend layout, request/response DTOs, transactions for claim/activity changes and server-side workflow rules. Those choices make the code easier to reason about.

The obvious limits are real login and authorization, database migrations, genuine insurer policy rules, secure document handling, a reliable notification system and payment integration. Kafka and the transactional outbox are design ideas, not working features. Also, the current role switcher is only a demo navigation aid and shouldn't be described as access control.

## 8. Notes for the next commits

I plan to start with one baseline commit and then make small, testable changes instead of creating an artificial commit history. For each change I'll add a short note like this:

```text
Change / date:
Why I changed it:
What I asked AI:
What suggestion I accepted or rejected:
Files I edited:
How I verified it:
Result:
Commit:
```

The assessment has a five-hour hard cap. I haven't kept a reliable time breakdown, so I'm not recording a made-up duration here.



## Appendix A — Module-by-module implementation walkthrough

I wanted to understand the system in smaller pieces rather than treating it as one large codebase. The following is the order I used to review and verify the modules after receiving the AI-assisted starter. It also describes the sequence I would use to build a similar application myself.

### Module A — Claim model and persistence

I began with the Claim entity and repository because the rest of the application depends on what a claim contains. I checked the status, amount, market, currency and assigned-officer fields, then followed how `JpaRepository` stores and retrieves the entity. One design choice I wanted to understand was why amounts use `BigDecimal` rather than floating-point types. The answer is that money calculations should not rely on binary floating-point precision.

### Module B — Claim submission API

Next I traced the submission path from `CreateClaimRequest` to `ClaimController`, `ClaimService` and the database. I checked why request validation is separate from workflow logic, and why the service saves an activity record together with the claim. The practical verification was submitting a claim through the frontend and seeing its generated reference.

### Module C — Claim tracking and information requests

I followed the customer-facing claim list and the activity timeline. I then checked the request-information and customer-response actions. The interesting part here is that the backend controls the transition between `IN_REVIEW` and `NEEDS_INFORMATION`; the customer page only sends a response. This is also where I identified a future need for actual claimant authentication and ownership checks.

### Module D — Officer review and decisions

I reviewed assignment, approval, rejection and settlement separately. I focused on what would happen if an officer tried to approve a claim in the wrong state or settle a claim twice. The backend rejects invalid transitions rather than trusting which buttons happen to be visible in Angular. I also noted the limitation of passing a demo officer ID from the UI instead of deriving it from an authenticated session.

### Module E — Dashboard and financial reporting

I checked how the dashboard counts open claims and estimates outstanding exposure. One important design point is keeping currencies separate. A MYR amount and an SGD amount cannot be combined into a meaningful total without conversion. The current implementation is sufficient for a small demo, but I would move aggregation into database queries for larger data volumes.

### Module F — Angular pages and shared API service

Since Angular was new to me, I studied one screen at a time: New Claim, My Claims, Claims Queue and Dashboard. I traced `ngModel` form binding, button event handlers, service injection and `HttpClient` subscriptions. I also checked how routing changes pages without reloading the whole application. The most useful realization was that the Angular service centralizes HTTP calls while components manage what the user sees.

### Module G — Testing and end-to-end verification

Finally, I ran the backend tests after correcting the Java test strings and verified the application manually in the browser. I submitted a claim and worked through the main customer/officer actions. I still want to add targeted tests for exact dashboard totals, invalid transitions and concurrent updates.

## Appendix B — Engineering issues to investigate in later commits

These are scenarios I identified during review, not bugs I am claiming to have encountered already. I'll only turn them into debugging entries if I can reproduce and fix them.

- **Angular form validation:** Check required fields, invalid amounts and whether the user receives a useful message.
- **Stale status display:** Check whether the officer screen refreshes selected claim details after an action.
- **Invalid state changes:** Add tests for repeated settlement and approval from an invalid status.
- **Large claims queues:** Replace in-memory filtering with repository queries and consider pagination.
- **Dashboard accuracy:** Test amounts by currency, reserve and decision state.
- **Concurrent assignment:** Verify that optimistic locking prevents one officer from silently overwriting another's update.

For each change I'll record the failing behavior or test, the code change, and the result before making a separate Git commit.
