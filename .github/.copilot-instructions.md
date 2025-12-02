# GitHub Copilot Instructions for StatusBoard

## Project Overview
StatusBoard is a real-time monitoring platform for tracking service health status.  It features:
- HTTP and AWS SDK-based health checks
- Status change history tracking
- Email and MS Teams notifications
- REST API for programmatic access
- Angular-based UI for visualization

## Technology Stack

### Backend (Scala)
- **Framework**: ZIO (functional effects library)
- **Build Tool**: SBT (Scala Build Tool)
- **API**: Tapir for type-safe HTTP endpoints with Swagger documentation
- **HTTP Server**: Blaze Server (http4s)
- **Database**: AWS DynamoDB for persistence
- **Testing**: ZIO Test framework
- **Code Coverage**: Jacoco

### Frontend (TypeScript)
- **Framework**: Angular 19
- **UI Library**: PrimeNG, CPS UI Kit
- **Testing**: Jest (unit tests) with `jest.config.ts`, Cypress (e2e tests) with `cypress.json`
- **Build Tool**: Angular CLI
- **Styling**: SCSS

### Infrastructure
- **Runtime**: JRE 21 (Corretto)
- **Containerization**: Docker with docker-compose
- **Cloud**: AWS (DynamoDB, SDK integrations)

## Code Style and Conventions

### Scala Code
- Follow functional programming principles with ZIO effects
- Use `scalafmt` for code formatting (`.scalafmt.conf` is configured)
- Enable compiler flags: `-unchecked`, `-deprecation`, `-feature`, `-Xfatal-warnings`, `-Ymacro-annotations`
- Organize code into layers:
  - **Controllers**: Business logic and API endpoint handlers
  - **Repository**: Database access layer
  - **Services**: Core business services (monitoring, notifications)
  - **Checkers**: Health check implementations
  - **Models**: Domain models and DTOs
  - **Config**: Configuration management with ZIO Config

### TypeScript/Angular Code
- Use ESLint (via `eslint.config.js`) and Prettier (via `.prettierrc`) for linting and formatting
- Follow Angular style guide
- Component-based architecture with services for business logic
- Use RxJS for reactive programming
- Test with Jest and Cypress

### Error Handling
- Use ZIO's `IO[ErrorResponse, A]` pattern for typed errors
- Map database errors to API response errors consistently
- Common error types: `RecordNotFoundErrorResponse`, `DataConflictErrorResponse`, `InternalServerErrorResponse`, `UnauthorizedErrorResponse`

### Authentication
- API key-based authentication via `Authorization` header
- Use `AuthController` ([api/controllers/AuthController.scala](../src/main/scala/za/co/absa/statusboard/api/controllers/AuthController.scala)) to protect write endpoints
- Read endpoints are generally public

## Project Structure

### Backend (`src/main/scala`)
```
za. co.absa.statusboard/
├── api/
│   ├── controllers/          # API endpoint handlers
│   │   ├── AuthController
│   │   ├── ServiceConfigurationController
│   │   ├── StatusController
│   │   └── MonitoringController
│   └── http/                 # HTTP layer (routes, endpoints)
├── checker/                  # Health check implementations
├── config/                   # Configuration models
├── model/                    # Domain models
├── monitoring/               # Monitoring service and workers
├── notification/             # Email and Teams notifications
├── providers/                # External service providers
└── repository/               # Database access layer
```

### Frontend (`ui/src/app`)
```
├── components/               # Angular components
│   ├── status-list/
│   ├── status-history/
│   ├── card/
│   └── dependency-graph/
├── services/                 # Angular services
│   ├── backend/             # API client
│   ├── repository/          # Data management
│   └── refresh/             # Auto-refresh logic
└── modules/                  # Shared modules
```

## Key Patterns

### ZIO Layers
- Use ZIO layers for dependency injection
- Define layer in companion object: `object XImpl { val layer: TaskLayer[X] = ...  }`
- Access services via ZIO environment: `ZIO.serviceWithZIO[Service](_. method())`

### API Endpoints (Tapir)
 Define endpoints in `Endpoints. scala` with full type safety
- Use `@accessible` macro for controller traits
- Bind public endpoints: `bindEndpoint(endpoint, handler)`
- Bind authenticated endpoints: `bindEndpoint(authController)(endpoint, handler)`
- Add `. noCache` to prevent caching on real-time data
- Read endpoints (GET) are typically public
- Write endpoints (POST, PUT, DELETE) typically require authentication via API key

### Controllers Pattern
```scala
@accessible
trait XController {
  def method(params): IO[ErrorResponse, Result]
}

class XControllerImpl(dependencies) extends XController {
  def method(params): IO[ErrorResponse, Result] = 
    // implementation with error handling
}

object XControllerImpl {
  val layer: TaskLayer[XController] = ZLayer { /* ... */ }
}
```

### Checker Architecture
The checker system uses a polymorphic design:
- **Base trait**: `Checker` defines the core interface:
  ```scala
  trait Checker {
    def checkRawStatus(action: StatusCheckAction): UIO[RawStatus]
  }
  ```
- **Specialized traits**: Each checker type has its own trait (e.g., `HttpGetRequestWithMessageChecker`, `AwsRdsChecker`, `AwsEmrChecker`)
- **Implementations**: Concrete implementations for each specialized trait (e.g., `HttpGetRequestWithMessageCheckerImpl`)
- **Polymorphic dispatcher**: `PolymorphicChecker` implements the base `Checker` trait and delegates to specialized checkers based on the `StatusCheckAction` subtype
- **Supported checkers**:
  - HTTP-based: `HttpGetRequestStatusCodeOnly`, `HttpGetRequestWithMessage`, `HttpGetRequestWithJsonStatus`
  - AWS-based: `AwsRds`, `AwsRdsCluster`, `AwsEmr`, `AwsEc2AmiCompliance`
  - Special: `FixedStatus`, `TemporaryFixedStatus`, `Composition`

### Database Operations
- Repository layer returns `IO[DatabaseError, Result]`
- Controllers map `DatabaseError` to `ErrorResponse`
- Use DynamoDB as primary persistence

## Testing Guidelines

### Scala Tests
- Unit tests: `sbt test` - should require no external dependencies
- Integration tests: Custom SBT commands (`testDynamo`, `testSMTP`, `testMSTeams`)
- Use ZIO Test framework with `suite` and `test` functions
- Mock external dependencies for unit tests
- Use `ConfigProviderSpec` for test configurations
- Test aspects: Use `@@ TestAspect. sequential` for tests that must run sequentially

### TypeScript Tests
- Unit tests: `npm run test` (Jest via `jest.config.ts`)
- E2E tests: `npm run e2e` (Cypress via `cypress.json`)
- Test coverage: `npm run test:coverage`

## Configuration
- Main config: `config. conf` (HOCON format)
- Environment-specific configurations for different deployments
- AWS credentials required (can be mocked for non-AWS environments)
- DynamoDB endpoint configurable for local development
- Configuration uses ZIO Config with magnolia derivation: `deriveConfig[T]. nested("path", "to", "config")`

## Build and Assembly
- Backend: `sbt assembly` creates fat JAR in `target/scala-2.13/`
- Frontend: `npm run build` in `ui/` directory
- Version source of truth: `VERSION` file at root
- Version sync: Frontend runs `npm run sync-version` (via `syncVersion.js`) to sync with VERSION file

## Dependency Management

### Scala Dependencies
- Define dependencies in `project/Dependencies.scala`
- Version constants in `Dependencies. Versions` object
- Common dependencies in `Dependencies.commonDependencies` sequence
- Referenced in `build.sbt`

### Frontend Dependencies
- Add npm dependencies in `ui/package.json`
- Major dependencies: Angular 19, PrimeNG, CPS UI Kit, RxJS

## API Documentation
- Swagger UI available at `GET /docs`
- Endpoints follow pattern: `/api/v1/{resource}`
- Use typed requests/responses with Circe JSON codec

## Notifications
- Email templates support variable substitution: `{{ SERVICE_NAME }}`, `{{ STATUS }}`, `{{ STATUS_COLOR }}`, etc.
- MS Teams webhook integration
- Notification logic in `notification/` package (templates in `src/main/resources/notifications/`)
- Polymorphic design: `PolymorphicNotificationActioner` delegates to specialized actioners
- Notification deciders: Duration-based logic to prevent notification spam

## Docker
- Backend Dockerfile for service
- UI.Dockerfile for frontend
- docker-compose.yml for local DynamoDB setup
- Follow comments in Dockerfiles for build/run instructions

## Release Management
- Semver versioning in VERSION file (keep the file in sync with Git tags/CI release pipelines to avoid mismatches)
- Snapshot builds: `X.Y.Z-SNAPSHOT`
- Release builds: `X.Y.Z` (no suffix)
- DEV deployment: automatic on merge to master
- PROD deployment: manual trigger after release

## When Writing Code

### For Backend Changes
1. Start with the model/domain types
2. Add repository methods if database access needed
3. Implement controller logic with proper error handling
4. Define Tapir endpoints with all error cases
5. Wire up in `RoutesImpl`
6. Add tests covering happy path and error cases
7. Update API documentation if endpoints change

### For Frontend Changes
1. Create/modify Angular components
2. Update services for API communication
3. Use CPS UI Kit and PrimeNG components
4. Add unit tests (Jest) and/or E2E tests (Cypress)
5. Ensure responsive design
6. Follow Angular reactive patterns with RxJS

### For Health Checkers
1. Define a specialized trait in `checker/` package extending the checker pattern:
   ```scala
   @accessible
   trait XChecker {
     def checkRawStatus(action: StatusCheckAction. X): UIO[RawStatus]
   }
   ```
2. Implement the trait in `XCheckerImpl` class
3. Define a ZIO layer in the companion object
4. Add the checker to `PolymorphicChecker` to handle the new action type
5. Support configuration from HOCON if needed
6. Return typed status results: `RawStatus. Green`, `RawStatus.Amber`, or `RawStatus.Red`
7. Use `intermittent` flag for transient failures
8. Add integration tests
9. Document in wiki under "Supported checkers"

## Common Commands
```bash
# Backend
sbt clean test               # Clean + unit tests
sbt compile                  # Compile
sbt assembly                 # Build fat JAR
sbt jacoco                   # Code coverage

# Frontend
npm run start                # Dev server
npm run build                # Production build
npm run test                 # Unit tests (Jest)
npm run e2e                  # E2E tests (Cypress)
npm run lint                 # Lint & format check
npm run lint:fix             # Auto-fix linting issues
npm run sync-version         # Sync VERSION file to package.json
```

## AWS Integration
- AWS SDK used for: DynamoDB, RDS, RDS Clusters, EMR, EC2
- ZIO-AWS library provides typed AWS integrations
- Region configuration per service check
- Endpoint override available for local testing

## Additional Resources
- [Wiki - REST API](https://github.com/AbsaOSS/StatusBoard/wiki/REST-API)
- [Wiki - Architecture](https://github.com/AbsaOSS/StatusBoard/wiki/Architecture)
- [Wiki - Supported Checkers](https://github.com/AbsaOSS/StatusBoard/wiki/Supported-checkers)