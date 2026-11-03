# Mercor Assessment & Recording Playbook

## Task Information
- **Topology:** `IntelliJ IDEA (LOOKUP)` ➔ `Postman (LOOKUP)` ➔ `IntelliJ IDEA (MODIFY)` ➔ `Postman (LOOKUP)` ➔ `IntelliJ IDEA (LOOKUP)`
- **Core Objective:** Debug and resolve request validation failures and missing error responses in an existing Spring Boot REST service. Fix DTO constraints and controller advice so improper payloads return appropriate HTTP 4xx statuses with clear error messages, and add integration tests to confirm endpoints handle both valid and invalid input correctly.

---

## Pre-Recording Checklist (Do before starting screen recording)
1. **Open IntelliJ IDEA:**
   - Open this directory: `/Users/ayushkumardas/Downloads/FirstTask`.
   - Confirm project SDK is set to Java 27 (or Java 17+).
   - Ensure you are on git branch `main` (`git status` is clean).
2. **Open Postman:**
   - Click **Import** in Postman and select:
     `Mercor_Request_Validation.postman_collection.json` (located in the project root).
   - You will see two folders:
     - `Step 2 - Probe Endpoints (Before Fix)`
     - `Step 4 - Verify Responses (After Fix)`
3. **Screen Setup:**
   - Arrange your screen so switching between IntelliJ IDEA and Postman is smooth and fast (`Cmd + Tab`).

---

## Step-by-Step Recording Walkthrough

### Step 1: IntelliJ IDEA (LOOKUP)
> **Goal:** Run clean Maven build, inspect DTOs and Controller, identify where validation is missing.

1. **Terminal / Build Verification:**
   - Open IntelliJ's built-in terminal or the Maven tool window.
   - Run:
     ```bash
     ./mvnw clean compile
     ```
   - Point out: `BUILD SUCCESS` — project compiles cleanly.
2. **Code Inspection (Think Aloud):**
   - Open `CreateUserRequest.java`:
     - *"Notice that `CreateUserRequest` contains fields like `name`, `email`, `age`, `role`, and nested `address`, but has zero Bean Validation annotations (`@NotBlank`, `@Email`, `@Min`, `@Valid`)."*
   - Open `AddressDto.java`:
     - *"Likewise, the nested `AddressDto` has no constraints on `street`, `city`, or `zipCode`."*
   - Open `UserController.java`:
     - *"In `UserController.createUser(@RequestBody CreateUserRequest request)`, the `@Valid` annotation is completely missing on the `@RequestBody`. Spring Boot therefore bypasses validation entirely."*
   - Open `UserService.java`:
     - *"Looking into `UserService`, the business logic assumes pre-validated input. When `age < 18` or `name`/`email`/`address` is null/empty, it throws unhandled `IllegalArgumentException` or `NullPointerException`. Because there is no `@RestControllerAdvice`, Spring will propagate these as 500 Internal Server Errors."*

---

### Step 2: Postman (LOOKUP)
> **Goal:** Spin up local server, hit endpoints with malformed and boundary payloads, catch 500 errors.

1. **Spin up Local Server:**
   - In IntelliJ, click the **Green Run** button next to `FirstTaskApplication.java` (or run `./mvnw spring-boot:run` in terminal).
   - Wait 2 seconds until Tomcat starts on port `8080`.
2. **Switch to Postman:**
   - Under `Step 2 - Probe Endpoints (Before Fix)`:
   - **Request 1: Baseline Valid Payload**
     - Hit `Send`.
     - Output: `HTTP 201 Created` with the generated user JSON.
     - Say: *"The happy path succeeds with 201 Created."*
   - **Request 2: Boundary Case (Age = 16, under 18)**
     - Hit `Send`.
     - Output: `HTTP 500 Internal Server Error`!
     - Say: *"Notice that a boundary validation failure (age 16 < 18) triggers an unhandled 500 Internal Server Error rather than a 400 Bad Request."*
   - **Request 3: Missing Required Fields (`{ "role": "USER" }`)**
     - Hit `Send`.
     - Output: `HTTP 500 Internal Server Error`!
     - Say: *"Missing required fields also bubble up to an unhandled 500 error."*
   - **Request 4: Malformed JSON (`"age": "not_a_number"`)**
     - Hit `Send`.
     - Output: Returns Spring's generic/raw error without user-friendly structured details.
     - Say: *"Malformed payloads fail without standardized structured error responses. Now let's switch back to IntelliJ to implement the fix."*

---

### Step 3: IntelliJ IDEA (MODIFY)
> **Goal:** Patch DTO constraints, wire up `@RestControllerAdvice`, and add integration tests.

> [!TIP]
> **Recording Pro-Tip:**
> You can either type/paste the code or apply the pre-tested patch instantly by running in terminal:
> ```bash
> git apply solution.patch
> ```
> Below are the exact changes being introduced:

1. **Patch `CreateUserRequest.java`:**
   Add validation annotations to fields:
   ```java
   @NotBlank(message = "Name is required")
   @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
   private String name;

   @NotBlank(message = "Email is required")
   @Email(message = "Email format is invalid")
   private String email;

   @NotNull(message = "Age is required")
   @Min(value = 18, message = "Age must be at least 18")
   @Max(value = 120, message = "Age must not exceed 120")
   private Integer age;

   @NotBlank(message = "Role is required")
   @Pattern(regexp = "USER|ADMIN|MANAGER", message = "Role must be USER, ADMIN, or MANAGER")
   private String role;

   @NotNull(message = "Address is required")
   @Valid
   private AddressDto address;
   ```

2. **Patch `AddressDto.java`:**
   ```java
   @NotBlank(message = "Street is required")
   private String street;

   @NotBlank(message = "City is required")
   private String city;

   @NotBlank(message = "Zip code is required")
   @Pattern(regexp = "^\\d{5}$", message = "Zip code must be a 5-digit number")
   private String zipCode;
   ```

3. **Patch `UserController.java`:**
   Add `@Valid` to the controller method parameter:
   ```java
   @PostMapping
   public ResponseEntity<UserResponseDto> createUser(@Valid @RequestBody CreateUserRequest request) { ... }
   ```

4. **Create `ErrorResponse.java` (`com.Odin.FirstTask.dto`):**
   Standardized JSON response for all error scenarios:
   ```java
   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public class ErrorResponse {
       private LocalDateTime timestamp;
       private int status;
       private String error;
       private String message;
       private String path;
       private Map<String, String> errors;
   }
   ```

5. **Create `GlobalExceptionHandler.java` (`com.Odin.FirstTask.exception`):**
   Centralized `@RestControllerAdvice` that catches:
   - `MethodArgumentNotValidException` ➔ `400 Bad Request` with field error map (`field -> message`).
   - `HttpMessageNotReadableException` ➔ `400 Bad Request` with malformed JSON message.
   - `IllegalArgumentException` ➔ `400 Bad Request` with exception message.
   - `UserNotFoundException` ➔ `404 Not Found`.

6. **Add Integration Tests in `UserControllerIntegrationTest.java`:**
   Covers:
   - Valid payload ➔ `201 Created`
   - Missing required fields ➔ `400 Bad Request`
   - Boundary age < 18 ➔ `400 Bad Request`
   - Invalid email format ➔ `400 Bad Request`
   - Nested zipCode validation ➔ `400 Bad Request`
   - Malformed JSON ➔ `400 Bad Request`
   - Nonexistent user ID ➔ `404 Not Found`

---

### Step 4: Postman (LOOKUP)
> **Goal:** Send fresh requests to verify updated 400 Bad Request responses and JSON error payloads.

1. **Restart Application in IntelliJ** (click Re-run / Restart icon).
2. **Switch to Postman:**
   - Open folder `Step 4 - Verify Responses (After Fix)`:
   - **Request 5: Valid Payload**
     - Hit `Send` ➔ `201 Created` with created User object.
   - **Request 6: Boundary Age < 18 (age = 16)**
     - Hit `Send` ➔ **`HTTP 400 Bad Request`**
     - Verify Response Body:
       ```json
       {
         "timestamp": "...",
         "status": 400,
         "error": "Bad Request",
         "message": "Validation failed for one or more fields",
         "path": "/api/v1/users",
         "errors": {
           "age": "Age must be at least 18"
         }
       }
       ```
     - Say: *"The boundary case now cleanly returns 400 Bad Request with a descriptive field error."*
   - **Request 7: Missing Required Fields**
     - Hit `Send` ➔ **`HTTP 400 Bad Request`**
     - Verify Response Body has clear validation errors for all missing fields (`name`, `email`, `age`, `role`, `address`).
   - **Request 8: Malformed JSON**
     - Hit `Send` ➔ **`HTTP 400 Bad Request`**
     - Body: `"message": "Malformed JSON request payload or invalid parameter format"`
   - **Request 9: Nonexistent User (`/api/v1/users/999`)**
     - Hit `Send` ➔ **`HTTP 404 Not Found`**
     - Body: `"message": "User not found with id: 999"`

---

### Step 5: IntelliJ IDEA (LOOKUP)
> **Goal:** Run `mvn test`, confirm all assertions pass, complete final git diff review.

1. **Execute Tests:**
   - In IntelliJ terminal, run:
     ```bash
     ./mvnw test
     ```
   - Show output:
     ```text
     [INFO] Running com.Odin.FirstTask.FirstTaskApplicationTests
     [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
     [INFO] Running com.Odin.FirstTask.UserControllerIntegrationTest
     [INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
     [INFO] ------------------------------------------------------------------------
     [INFO] BUILD SUCCESS
     [INFO] ------------------------------------------------------------------------
     ```
   - Say: *"All 8 unit and integration test assertions pass green."*
2. **Final Diff Review:**
   - Run in terminal:
     ```bash
     git diff
     ```
     *(Or open IntelliJ's Git / Commit tool window to show file diffs).*
   - Walk through the diff:
     - `CreateUserRequest.java` & `AddressDto.java`: Added Jakarta validation constraints.
     - `UserController.java`: Added `@Valid` to trigger request validation.
     - `GlobalExceptionHandler.java`: Centralized exception advice with standardized responses.
     - `UserControllerIntegrationTest.java`: Comprehensive regression test coverage.
3. **Wrap Up:**
   - Conclude: *"All validation failures and unhandled 500 errors have been resolved, proper 4xx responses are established, integration tests confirm end-to-end correctness, and the git diff is reviewed."*
