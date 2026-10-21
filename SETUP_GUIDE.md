# Ganesh's ClaimFlow — setup and demo

These are the steps I use to run ClaimsFlow locally. I tested it on an Intel Mac using VS Code. All commands assume the terminal starts in the project root unless I say otherwise.

## What I need installed

- JDK 21
- Maven (3.8.3 worked on my machine)
- Node.js compatible with Angular 21 (Node 22.12+ in the Node 22 line)
- npm and Git

```bash
java -version
mvn -version
node -v
npm -v
git --version
```

Make sure Maven is using Java 21 too. On a Mac with JDK 21 installed, I can select it for the current terminal with:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

## 1. Backend

Open the first terminal:

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

When the server starts, check one of these:

- http://localhost:8080/api/claims
- http://localhost:8080/api/dashboard
- http://localhost:8080/swagger-ui/index.html

Spring Boot uses a local file-based H2 database, stored under `backend/data/` when launched from the backend directory. It keeps my demo data between restarts. The project adds sample claims only when no claims exist.

If I need to reset the data, I must stop Spring Boot first, then remove the local database files. Doing that deletes the demo claims, so I wouldn't do it without checking.

## 2. Frontend

Leave the backend running. Open a second terminal from the project root:

```bash
cd frontend
npm install
npm start
```

Open http://localhost:4200.

Angular uses a local proxy (`src/proxy.conf.json`) so requests to `/api` are forwarded to Spring Boot on port 8080. I don't need to hard-code the backend host in each Angular component.

## 3. Problems I hit during setup

**Maven test compilation:** The first generated version of `ClaimsIntegrationTest.java` contained JSON text with unescaped quotes. The build failed with 62 syntax errors. After replacing that test file, I reran `mvn clean test` and got `BUILD SUCCESS`.

**npm dependency installation:** My first `npm install` showed `Cannot read properties of null (reading 'edgesOut')`. An attempted global npm update gave me a permissions (`EACCES`) error. I used the AI troubleshooting guidance and got Angular running. I didn't keep the exact final fix command.

If npm installation has the same issue again, I can try this from inside `frontend/`:

```bash
rm -rf node_modules
npx --yes npm@11.19.1 install
```

I should avoid installing dependencies with `sudo` or changing system directory permissions just for this project.

## 4. Manual walkthrough

Once both servers are running:

1. Go to **New claim** and create a motor claim using `demo-customer-001`.
2. Open **My claims** and confirm the new reference appears with status `SUBMITTED`.
3. Open **Claims queue**, select it and assign it to `officer-001`.
4. Request some extra information and reply from the claimant view.
5. Approve it with a valid amount and reason.
6. Mark it settled, then inspect the history and dashboard.
7. If I try to settle it again through Swagger, the backend should reject that transition.

The role views are for demo purposes. There is no real login or user authorization, and `SETTLED` is only an application status, not a real payment.

## What I check on each page

This is a simple smoke-test order I use after changing the code:

| Page | Action | Expected result |
| --- | --- | --- |
| New claim | Submit a valid motor claim | A reference number is returned |
| My claims | Search using the same customer ID and filter by status | The matching claims show and the total count is correct |
| Claims queue | Assign it to a demo officer | It moves to `IN_REVIEW` |
| Claims queue | Request additional information | It moves to `NEEDS_INFORMATION` |
| My claims | Enter a response | It returns to `IN_REVIEW` |
| Claims queue | Approve with amount and reason | It moves to `APPROVED` |
| Claims queue | Mark it settled | It moves to `SETTLED` |
| Dashboard | Refresh after those actions | Counts and exposure reflect the latest state |

If a page fails, I start with Chrome DevTools → Network to see the HTTP status and request payload. Then I compare it to the backend endpoint and check the Spring Boot terminal. I don't start by changing CSS or unrelated components.

## 5. Final checks

Before sharing the repo, run:

```bash
# From the project root
cd backend
mvn clean test
mvn clean package
cd ../frontend
npm run build
cd ..
git status
```

Only say the commands passed if they actually did. The `.gitignore` should keep `backend/target`, `backend/data`, `frontend/node_modules`, `frontend/dist` and `frontend/.angular` out of Git. I also need to check that no credentials, local logs or confidential assessment files are staged.

## 6. Updating the existing GitHub repository

Git has already been initialized and the baseline commit is `aa9daa6` on `main`. There is no need to run `git init` again or create a second repository.

For documentation-only edits, from the project root:

```bash
git status
git diff -- README.md AI_JOURNAL.md SETUP_GUIDE.md docs/architecture.md
git add README.md AI_JOURNAL.md SETUP_GUIDE.md docs/architecture.md
git diff --cached --stat
git commit -m "docs: update testing pagination and UI notes"
git push origin main
```

`git commit` saves the change locally; `git push` uploads it to the configured GitHub remote. The intended repository is `https://github.com/Ganesh010598/ganesh-claimflow`. Check `git remote -v` before pushing.

Only include commands and test results in the journal that actually happened. Any UI, pagination or build check that is still pending should stay marked pending until verified.

## 7. Pagination and UI checks

Once both servers are running, open the new **Ganesh's ClaimFlow** header and verify the navigation works. On **My claims**, confirm that the Customer ID, Status and Search claims controls line up at desktop width and rearrange sensibly on a smaller screen. Repeat that check on **Claims queue**.

Try the paginated API directly while the backend is running:

```bash
curl 'http://localhost:8080/api/claims?page=0&size=10'
curl 'http://localhost:8080/api/claims?page=0&size=5&status=SUBMITTED'
curl 'http://localhost:8080/api/claims'
```

The first two should return objects with `content` and pagination metadata; the final one should still return an array. Create enough demo claims to check that Next and Previous work, then confirm filtering resets the page to the first page.

For the updated code, rerun both builds (the Java 21 selection matters whenever a new terminal opens):

```bash
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean test
cd ../frontend
npm run build
```

If the browser is blank, start **both** servers in separate terminal windows (`mvn spring-boot:run` and `npm start`) and check the Angular terminal and browser developer console before changing source code.
