# Verdixa

Verdixa is a full-stack online coding platform for practising algorithms, submitting solutions, and managing a curated problem catalogue.

## Overview

The application pairs a React/Monaco solver with a Java 21 Spring Boot REST API, JWT authentication, MySQL persistence, and local Java, C++17, and Python execution. It has separate USER and ADMIN workspaces with server-enforced RBAC.

## Architecture

```text
React + Monaco
      ↓ REST / JWT
Spring Boot + JPA
      ↓
    MySQL
```

## Technology stack

- Frontend: React, React Router, Axios, Monaco, CSS, Lucide
- Backend: Java 21, Spring Boot, Spring Security, JWT, Spring Data JPA/Hibernate
- Database: MySQL 8
- Testing: JUnit/Spring Boot Test, Vitest, React Testing Library
- Delivery: Docker, Docker Compose, GitHub Actions

## Core features

USER features include registration/login, a searchable Problem Library with filters and status, bookmarks, Monaco editing, custom Run, official Submit, submission history/detail/replay, editorials, private notes, lists, curated collections, learning paths, daily challenges, heatmap/profile analytics, and a leaderboard.

ADMIN features include platform/user/problem analytics, problem and testcase management, FUNCTION metadata, the four-hidden-test publishing rule, editorials, collections, learning paths, daily challenges, and paginated management views.

## Online judge

Both STDIN and FUNCTION problems support Java, C++17, and Python. FUNCTION problems accept a solution method only; language-specific wrappers deserialize supported values, invoke the method, and normalize its result.

Supported FUNCTION values: `int`, `Integer`, `long`, `double`, `boolean`, `String`, `int[]`, `long[]`, `double[]`, and `String[]`.

**Run** executes only custom cases and never persists a submission or affects progress, streaks, challenges, or rankings. **Submit** evaluates official public and hidden cases and persists the result.

Published problems require exactly four hidden cases. User-facing results identify hidden cases only by ordinal and verdict; hidden inputs, arguments, expected values, actual values, and generated wrappers are not returned.

## Security

- JWTs use `ALGOSPHERE_JWT_SECRET` (minimum 32 characters); no signing key is committed.
- Stateless security disables form login and HTTP Basic authentication.
- RBAC protects administrator endpoints; ownership checks protect notes, lists, bookmarks, and submissions.
- API DTOs and safe result projections avoid returning credentials, private data, and hidden testcase payloads.

## Local setup (Windows CMD)

Create a MySQL database named `leetcode_platform`, then set environment variables and start the API:

```cmd
cd /d C:\Users\surig\leetcode-platform\backend
set ALGOSPHERE_DB_URL=jdbc:mysql://localhost:3306/leetcode_platform
set ALGOSPHERE_DB_USERNAME=root
set ALGOSPHERE_DB_PASSWORD=YOUR_MYSQL_PASSWORD
set ALGOSPHERE_JWT_SECRET=replace-with-a-random-32-plus-character-secret
mvn -s .m2\settings.xml spring-boot:run
```

Start the frontend in another terminal:

```cmd
cd /d C:\Users\surig\leetcode-platform\frontend
npm install
set VITE_API_BASE_URL=http://localhost:8080/api
npm run dev
```

Open `http://localhost:5173`; check the API with `curl http://localhost:8080/api/health`.

To create a local bootstrap admin explicitly, set both `ALGOSPHERE_BOOTSTRAP_ADMIN_USERNAME` and `ALGOSPHERE_BOOTSTRAP_ADMIN_PASSWORD` before starting. No default administrator is created.

## Testing

```cmd
cd /d C:\Users\surig\leetcode-platform\backend
mvn -s .m2\settings.xml clean test
mvn -s .m2\settings.xml package

cd /d C:\Users\surig\leetcode-platform\frontend
npm run lint
npm test -- --run
npm run build
```

The backend suite uses isolated H2 configuration; it does not destructively test the development MySQL database.

## Docker

Copy `.env.example` to `.env`, set a strong `ALGOSPHERE_JWT_SECRET`, then run:

```cmd
docker compose up --build
```

Compose starts MySQL, the API, and Nginx-hosted frontend. The API container includes a JDK, `g++`, and Python 3 for the existing executor. The production frontend proxies `/api` to the backend and supports client-side route refreshes.

## CI

GitHub Actions runs Java 21 backend tests/package and frontend lint, tests, and production build on pushes and pull requests.

## Important limitation

Solutions are executed with `ProcessBuilder`, timeouts, temporary directories, and bounded request payloads. This is suitable for a local demo/development environment, **not** a hardened hostile-code sandbox. Docker/Compose improves reproducibility but does not by itself isolate untrusted code safely; production use requires dedicated sandbox infrastructure, OS-level resource limits, and stronger isolation.
