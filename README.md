# User Registration API

A Spring Boot REST API that lets you register users and view a registered user's details.
**Only adults (18 or older) who live in France can register.**

## Tech stack

- Java 17, Spring Boot 4.1 (Web MVC, Data JPA, Validation, AspectJ)
- Embedded **H2** in-memory database: nothing to install
- JUnit 5, Mockito, AssertJ, MockMvc

## Build and run

Prerequisite: **JDK 17+**. Maven is optional because the Maven wrapper is included.

```bash
# Build and run all tests
./mvnw clean verify          # Windows: mvnw.cmd clean verify

# Start the application on http://localhost:8080
./mvnw spring-boot:run

# Or run the packaged jar
./mvnw clean package
java -jar target/atosTest-0.0.1-SNAPSHOT.jar
```

The H2 database is in memory and starts empty on every launch.
You can browse it at <http://localhost:8080/h2-console>
(JDBC URL `jdbc:h2:mem:usersdb`, user `sa`, empty password).

## API
a postman collection is included in the project in the file "Atos test.postman_collection.json"
Base path: `/api/users`

### Register a user: `POST /api/users/add`

| Field                | Required | Rules                                                                     |
|----------------------|----------|---------------------------------------------------------------------------|
| `username`           | yes      | Any non-blank text. Unique (case-insensitive)                             |
| `birthdate`          | yes      | ISO date `yyyy-MM-dd`, in the past. The user must be **18 or older**      |
| `countryOfResidence` | yes      | ISO 3166-1 alpha-2 code (`FR`) or English name (`France`), any case. Must be **France** |
| `phoneNumber`        | no       | 6–15 digits, optional leading `+` (e.g. `+33612345678`)                   |
| `gender`             | no       | `MALE`, `FEMALE` or `OTHER`                                               |

```bash
curl -i -X POST http://localhost:8080/api/users/add \
  -H "Content-Type: application/json" \
  -d '{
    "username": "jdupont",
    "birthdate": "1990-03-21",
    "countryOfResidence": "France",
    "phoneNumber": "+33612345678",
    "gender": "MALE"
  }'
```

Response `201 Created`, with a `Location: http://localhost:8080/api/users/1` header:

```json response
{
  "id": 1,
  "username": "jdupont",
  "birthdate": "1990-03-21",
  "countryOfResidence": "FR",
  "phoneNumber": "+33612345678",
  "gender": "MALE",
  "createdAt": "2026-11-12T12:28:42.991709Z"
}
```

The country is stored and returned as its ISO code.

### Get a user's details: `GET /api/users/{id}`

```url
curl http://localhost:8080/api/users/1
```

Response `200 OK` with the same body as above.

### Errors

 Validation errors add an `errors` object that maps each invalid field to its message:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid",
  "instance": "/api/users/add",
  "errors": {
    "birthdate": "birthdate is required",
    "username": "username is required"
  }
}
```

| Status | When                                                                                           |
|--------|------------------------------------------------------------------------------------------------|
| 400    | Missing or invalid field, unknown country, malformed JSON or date, unknown gender, non-numeric or non-positive id |
| 404    | No user exists with the requested id                                                           |
| 409    | The username is already taken                                                                  |
| 415    | The request body is not JSON                                                                   |
| 422    | The request is well-formed but not eligible: the user is under 18 or does not live in France   |
| 500    | Unexpected error (no internal details are exposed)                                             |

## Configuration
 rules are set in [application.properties](src/main/resources/application.properties):

```properties
registration.allowed-country=FR
registration.minimum-age=18
```

## Call logging (AOP)

[CallLoggingAspect](src/main/java/com/example/atosTest/logging/CallLoggingAspect.java) logs the input, output
(or thrown exception) and processing time of every REST controller call:

```
--> UserController.register input=[UserRegistrationRequest[username=jdupont, birthdate=1990-03-21, ...]]
<-- UserController.register output=<201 CREATED Created,UserResponse[id=1, ...]> duration=35ms
--> UserController.getUser input=[42]
<-- UserController.getUser threw UserNotFoundException: No user found with id 42 duration=3ms
```

## Project structure

```
src/main/java/com/example/atosTest
├── config/        Clock bean, registration rule properties
├── logging/       AOP call-logging aspect
└── user/
    ├── domain/    User entity, Gender, UserRepository
    ├── dto/       Request and response records (with validation constraints)
    ├── exception/ Business exceptions
    ├── service/   UserService (business rules), CountryResolver
    └── web/       UserController, GlobalExceptionHandler
```

## Tests

`./mvnw test` runs:

- **Unit tests**: `UserServiceTest` (eligibility rules, with a fixed `Clock` for exact age boundaries) and `CountryResolverTest`
- **Web layer tests**: `UserControllerTest` (`@WebMvcTest`), which covers every validation rule and HTTP status
- **Persistence tests**: `UserRepositoryTest` (`@DataJpaTest`), which covers the case-insensitive lookup and the unique constraint
