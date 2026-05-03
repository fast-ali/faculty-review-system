# OOP Principles Used in Your Code

## 1. ENCAPSULATION ✅ (Major Use)

**Definition**: Hiding internal data and controlling access through public methods.

### Examples:

#### A. Private Fields + Public Getters/Setters (Review.java)
```java
// Line 11: Private field
private Long id;
private int rating;
private short subjectMatterKnowledge;

// Lines 63-64: Controlled access through public methods
public Long getId() { return id; }
public void setId(Long id) { this.id = id; }
public int getRating() { return rating; }
public void setRating(int rating) { this.rating = rating; }
```
**Why it matters**: Only Review class can directly modify these fields. Callers must use getters/setters, 
allowing validation if needed.

#### B. Relationships Encapsulated (Review.java, Lines 13-19)
```java
@ManyToOne
@JoinColumn(name = "faculty_id", nullable = false)
private FacultyEntity faculty;  // Private relationship

@ManyToOne
@JoinColumn(name = "course_id")
private Course course;  // Private relationship

// Public access only through controlled methods
public FacultyEntity getFaculty() { return faculty; }
public void setFaculty(FacultyEntity faculty) { this.faculty = faculty; }
```

#### C. State Protection (AdminService.java, Lines 91-102)
```java
// deleteUser method PROTECTS state - prevents deleting yourself or admin accounts
if (target.getId().equals(actor.getId())) {
    throw new IllegalArgumentException("You cannot delete your own account.");
}
if ("ROLE_ADMIN".equalsIgnoreCase(target.getRole())) {
    throw new IllegalArgumentException("Admin accounts cannot be deleted from admin panel.");
}
```
**Encapsulation in action**: Prevents invalid state transitions through business logic validation.

#### D. Boolean State Encapsulation (Review.java, Line 50)
```java
private boolean reported;  // Encapsulated state
public boolean isReported() { return reported; }
public void setReported(boolean reported) { this.reported = reported; }
```

---

## 2. INHERITANCE ✅ (Moderate Use)

**Definition**: A class deriving properties and methods from another class.

### A. Custom Exception Inheritance (User-Created Classes)

#### WeakPasswordException.java (Lines 3-7)
```java
// YOUR CODE - inherits from RuntimeException
public class WeakPasswordException extends RuntimeException {
    public WeakPasswordException(String message) {
        super(message);  // Calls parent constructor
    }
}
```

#### DuplicateUserFieldException.java (Lines 3-6)
```java
// YOUR CODE - also inherits from RuntimeException
public class DuplicateUserFieldException extends RuntimeException {
    public DuplicateUserFieldException(String message) {
        super(message);
    }
}
```

**Usage in MyUserService.java (Lines 38, 42, 46)**:
```java
if (userRepository.existsByUsernameIgnoreCase(username)) {
    throw new DuplicateUserFieldException("Username already exists.");  // Inherits methods/behavior
}
if (!password.equals(confirmPassword)) {
    throw new WeakPasswordException("Password and confirmation do not match.");
}
```

**Why it matters**: Your exceptions inherit `getMessage()`, `printStackTrace()`, `toString()` from RuntimeException, 
enabling proper error handling throughout the app.

### B. Constructor Chaining (Faculty.java, Lines 13-23)
```java
// Constructor 1: Backward compatible (no bio)
public Faculty(Long id, String name, String department, String imageUrl) {
    this(id, name, department, imageUrl, null);  // Calls Constructor 2
}

// Constructor 2: Full constructor
public Faculty(Long id, String name, String department, String imageUrl, String bio) {
    this.id = id;
    this.name = name;
    this.department = department;
    this.imageUrl = imageUrl;
    this.bio = bio;
}
```
**Pattern**: Constructor delegates to another - reduces code duplication (inheritance of behavior).

### C. Object Lifecycle Methods (Review.java, Lines 58-61)
```java
@PrePersist
public void prePersist() {
    if (createdAt == null) createdAt = OffsetDateTime.now();
}
```
**Same pattern in AuditLog.java, Lines 24-27 and MyUser.java, Lines 43-49**

This demonstrates **inherited behavior** - all entities follow same lifecycle pattern.

---

## 3. POLYMORPHISM ✅ (Moderate Use)

**Definition**: Objects behaving differently based on their type or context.

### A. Method Overriding (MyUserService.java, Line 81)
```java
@Override  // YOUR CODE overrides Spring's interface method
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    Optional<MyUser> user = userRepository.findByUsername(username);
    if (user.isPresent()) {
        var userObj = user.get();
        String role = userObj.getRole() == null ? "STUDENT" : userObj.getRole();
        return User.builder()
                .username(userObj.getUsername())
                .password(userObj.getPassword())
                .disabled(!userObj.isEnabled())
                .accountLocked(userObj.isAccountLocked())
                .roles(role)
                .build();
    }
    throw new UsernameNotFoundException(username);
}
```
**Polymorphism**: Spring Security calls this method, but YOUR implementation determines what happens.

### B. Polymorphic Behavior in AdminService (Lines 53, 68, 88)
```java
// Different objects, same interface - polymorphic handling
var actor = userRepository.findById(adminUserId).orElseThrow();  // MyUser object
var target = userRepository.findById(userId).orElseThrow();      // MyUser object

// But they're treated AS DIFFERENT USERS based on business logic
if (target.getId().equals(actor.getId())) {
    // POLYMORPHIC BEHAVIOR: Same method, different responses based on object state
    throw new IllegalArgumentException("You cannot demote your own admin role.");
}
```

### C. Stream API Polymorphism (AdminService.java, Line 104)
```java
// Different filtering logic for different object types
public List<Review> listReportedReviews() {
    return reviewRepository.findAll().stream()
            .filter(Review::isReported)  // POLYMORPHIC: calls isReported() on each Review
            .toList();
}

public List<ReviewRow> listAllReviewRows() {
    return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(r -> new ReviewRow(...))  // POLYMORPHIC: transforms each Review differently
            .toList();
}
```

### D. Exception Polymorphism (MyUserService.java)
```java
// POLYMORPHIC: Two different exceptions thrown from same method
if (userRepository.existsByUsernameIgnoreCase(username)) {
    throw new DuplicateUserFieldException("Username already exists.");  // Exception Type 1
}
if (!password.equals(confirmPassword)) {
    throw new WeakPasswordException("Password and confirmation do not match.");  // Exception Type 2
}
// Same catch block can handle both (they share RuntimeException parent)
```

---

## 4. ABSTRACTION ✅ (High Use)

**Definition**: Hiding complex implementation details; showing only essential features.

### A. Service Layer Abstraction (AdminService.java)
Your service hides complex admin operations:

```java
// ABSTRACTION: Caller doesn't know HOW it's done, just WHAT it does
public void deleteUser(Long userId, Long adminUserId) {
    // Complex logic hidden inside
    var actor = userRepository.findById(adminUserId).orElseThrow();
    var target = userRepository.findById(userId).orElseThrow();
    
    // Validation logic
    if (target.getId().equals(actor.getId())) {
        logAction(adminUserId, "DELETE_USER_BLOCKED", ...);
        throw new IllegalArgumentException(...);
    }
    
    // More validation...
    if ("ROLE_ADMIN".equalsIgnoreCase(target.getRole())) {
        logAction(adminUserId, "DELETE_USER_BLOCKED", ...);
        throw new IllegalArgumentException(...);
    }
    
    // Finally, delete
    userRepository.deleteById(userId);
    logAction(adminUserId, "DELETE_USER", "Deleted user " + userId);
}
```

**Called from Controller (abstracted away):**
```java
// AdminController just calls this - doesn't know implementation details
adminService.deleteUser(id, actorId(authentication));
```

### B. Validation Logic Abstraction (MyUserService.java)
```java
// Complex password validation ABSTRACTED away
private void validatePasswordPolicy(String password) {
    if (password == null || password.length() < 8 || password.length() > 72) {
        throw new WeakPasswordException("Password must be between 8 and 72 characters.");
    }
    if (!UPPERCASE_PATTERN.matcher(password).find()) {
        throw new WeakPasswordException("Password must contain at least one uppercase letter.");
    }
    // More validation...
}

// Called simply as:
validatePasswordPolicy(password);  // Implementation details hidden
```

### C. Data Transformation Abstraction (AdminService.java, Lines 112-123)
```java
// ABSTRACTION: Complex data transformation hidden
public List<ReviewRow> listAllReviewRows() {
    return reviewRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(r -> new ReviewRow(
                    r.getId(),
                    "Anonymous",
                    r.getFaculty() != null ? r.getFaculty().getName() : "Unknown",
                    r.getCourse() != null ? (r.getCourse().getCode() + " - " + r.getCourse().getTitle()) : "N/A",
                    r.getCreatedAt(),
                    r.getRating()
            ))
            .toList();
}

// Controller just calls this:
model.addAttribute("reviewRows", adminService.listAllReviewRows());  // No idea HOW it's done
```

---

## 5. SINGLE RESPONSIBILITY PRINCIPLE (SRP) ✅

**Definition**: Each class should have ONE reason to change.

| Class | Single Responsibility |
|-------|----------------------|
| **Review.java** | Store review data |
| **Course.java** | Store course data |
| **AuditLog.java** | Store audit log entries |
| **AdminService.java** | Handle admin operations |
| **MyUserService.java** | Handle user registration & authentication |
| **WeakPasswordException.java** | Represent weak password errors |
| **DuplicateUserFieldException.java** | Represent duplicate field errors |

Each class has ONE reason to change - no mixing of concerns.

---

## 6. DEPENDENCY INJECTION (DI) ✅

**Definition**: Objects receive dependencies from outside, not creating them.

### A. Constructor Injection (AdminService.java, Lines 27-44)
```java
// YOUR CODE: Dependencies injected, not created
public AdminService(
    UserRepository userRepository,
    FacultyRepository facultyRepository,
    ReviewRepository reviewRepository,
    CourseRepository courseRepository,
    CourseRequestRepository courseRequestRepository,
    DepartmentRepository departmentRepository,
    AuditLogRepository auditLogRepository
) {
    this.userRepository = userRepository;
    this.facultyRepository = facultyRepository;
    // ... more assignments
}
```

**Usage in AdminController.java**:
```java
public AdminController(AdminService adminService, UserRepository userRepository) {
    this.adminService = adminService;      // Injected dependency
    this.userRepository = userRepository;   // Injected dependency
}
```

**Why it matters**: Easy to test, change implementations, maintain loose coupling.

---

## 7. RELATIONSHIP MODELING ✅

### A. One-to-Many Relationships (Review.java, Lines 13-19)
```java
@ManyToOne
@JoinColumn(name = "faculty_id", nullable = false)
private FacultyEntity faculty;  // Many reviews → One faculty

@ManyToOne
@JoinColumn(name = "course_id")
private Course course;  // Many reviews → One course
```

### B. Aggregation (AdminService.java, Lines 21-28)
```java
// AdminService AGGREGATES all repositories
private final UserRepository userRepository;
private final FacultyRepository facultyRepository;
private final ReviewRepository reviewRepository;
private final CourseRepository courseRepository;
// ... more repositories (whole-part relationship)
```

---

## 8. IMMUTABILITY & STATE PROTECTION ✅

### A. Final Fields (AdminService.java)
```java
// IMMUTABLE: Cannot be reassigned after construction
private final UserRepository userRepository;
private final FacultyRepository facultyRepository;
private final ReviewRepository reviewRepository;
// ... all final - protects object state
```

### B. Controlled State Changes (AdminService.java, Lines 65-82)
```java
// State can only change through validated methods
@Transactional
public void setUserEnabled(Long userId, boolean enabled, Long adminUserId) {
    // Validation BEFORE state change
    if (target.getId().equals(actor.getId())) {
        throw new IllegalArgumentException(...);
    }
    if ("ROLE_ADMIN".equalsIgnoreCase(target.getRole())) {
        throw new IllegalArgumentException(...);
    }
    
    // State change only after validation
    target.setEnabled(enabled);
    userRepository.save(target);
    logAction(adminUserId, ...);  // Log the change
}
```

---

## Summary Table: OOP Principles in YOUR Code

| Principle | Usage Level | Key Files |
|-----------|------------|-----------|
| **Encapsulation** | ⭐⭐⭐⭐⭐ (High) | Review.java, AdminService.java, All Models |
| **Inheritance** | ⭐⭐⭐ (Medium) | WeakPasswordException.java, DuplicateUserFieldException.java |
| **Polymorphism** | ⭐⭐⭐ (Medium) | MyUserService.java, AdminService.java |
| **Abstraction** | ⭐⭐⭐⭐ (High) | AdminService.java, MyUserService.java |
| **Single Responsibility** | ⭐⭐⭐⭐⭐ (High) | All classes follow SRP |
| **Dependency Injection** | ⭐⭐⭐⭐ (High) | AdminService.java, AdminController.java |
| **Relationship Modeling** | ⭐⭐⭐⭐ (High) | Review.java, Course.java, Department.java |

---

## Conclusion

Your code demonstrates **strong OOP principles**, particularly:
1. **Excellent layering** (Controllers → Services → Repositories → Models)
2. **Proper encapsulation** (all fields private with controlled access)
3. **Clean exception handling** (custom exceptions with inheritance)
4. **Service-oriented design** (business logic abstracted away)
5. **Dependency injection** (loose coupling, high testability)

This is **professional-quality code** following industry best practices!

