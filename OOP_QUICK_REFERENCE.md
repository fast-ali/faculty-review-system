# OOP Principles Quick Reference - Your Code

## 🔒 ENCAPSULATION - Hiding & Controlling Access

```
Review.java (EXAMPLE)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private Long id;                          ❌ DIRECT ACCESS BLOCKED
private int rating;
private short subjectMatterKnowledge;
private FacultyEntity faculty;            🔐 Hidden relationships
private Course course;

    ↓  ONLY ACCESS THROUGH ↓

public Long getId()                       ✅ CONTROLLED ACCESS
public void setId(Long id)
public int getRating()
public void setRating(int rating)
public FacultyEntity getFaculty()
public void setFaculty(FacultyEntity f)
```

**Real-world example from YOUR code:**
```java
// AdminService.java - Prevents invalid state
if (target.getId().equals(actor.getId())) {
    throw new IllegalArgumentException("You cannot delete your own account.");
}
// ✅ Protection: You CAN'T accidentally delete yourself
```

---

## 🧬 INHERITANCE - Code Reuse Through Extension

```
                    RuntimeException (Java Framework)
                            ↑
                            │  INHERITED
                            │
           ┌────────────────┴────────────────┐
           │                                 │
    WeakPasswordException              DuplicateUserFieldException
    (YOUR CODE)                        (YOUR CODE)

    public class WeakPasswordException extends RuntimeException {
        public WeakPasswordException(String message) {
            super(message);  // ← USE parent's constructor
        }
    }
```

**Inheritance chain in your code:**
```
Class Hierarchy:
    RuntimeException
        ├── WeakPasswordException (you created)
        ├── DuplicateUserFieldException (you created)
        └── All other Exceptions

Usage in MyUserService.java:
    if (!password.equals(confirmPassword)) {
        throw new WeakPasswordException("Don't match");
        // ↑ Inherits getMessage(), printStackTrace(), toString() etc.
    }
```

**Constructor Chaining Example:**
```
Faculty.java (Lines 13-23)
━━━━━━━━━━━━━━━━━━━━━━━

Constructor 1 (4 params)
    ↓ calls ↓
Constructor 2 (5 params - full)
    • Reduces code duplication
    • Easier to maintain
```

---

## 🎭 POLYMORPHISM - Same Interface, Different Behavior

```
METHOD OVERRIDING
━━━━━━━━━━━━━━━━━

Interface: UserDetailsService (from Spring)
    │
    ├── Your Implementation: MyUserService
    │       @Override
    │       public UserDetails loadUserByUsername(String username)
    │       // YOUR SPECIFIC LOGIC HERE
    │
    └── Spring calls your version (not its own)

Result: POLYMORPHIC BEHAVIOR
```

**Exception Polymorphism in your code:**
```
MyUserService.java (Lines 37-46)

SAME METHOD → DIFFERENT EXCEPTIONS
    ↓
if (userRepository.existsByUsernameIgnoreCase(username)) {
    throw new DuplicateUserFieldException(...);  ← Exception Type 1
}
if (userRepository.existsByEmailIgnoreCase(email)) {
    throw new DuplicateUserFieldException(...);  ← Exception Type 1
}
if (!password.equals(confirmPassword)) {
    throw new WeakPasswordException(...);        ← Exception Type 2
}

POLYMORPHIC: Same registerUser() method throws different exceptions
             depending on conditions
```

**Stream Polymorphism:**
```
AdminService.java (Lines 104, 112)

POLYMORPHIC FILTERING
listReportedReviews() → reviews filtered by: Review::isReported
listAllReviewRows()  → reviews transformed to: ReviewRow objects

SAME DATA → DIFFERENT POLYMORPHIC BEHAVIORS
```

---

## 📦 ABSTRACTION - Hiding Complex Details

```
Controller LAYER (What user sees)
    │
    ├── adminService.deleteUser(id, adminId)
    │           ↓
    │   (ABSTRACTION: Don't know HOW)
    │
SERVICE LAYER (Complex logic hidden)
    ├── Check if user exists
    ├── Check if actor is admin
    ├── Prevent self-deletion
    ├── Log the action
    ├── Delete user
    │           ↓
    │   (ABSTRACTION: Don't see HOW)
    │
DATA LAYER
    └── Database operations
```

**Real complexity abstracted in AdminService:**
```java
// ONE LINE CALL FROM CONTROLLER
adminService.deleteUser(id, actorId(authentication));

// HIDDEN COMPLEXITY INSIDE SERVICE:
public void deleteUser(Long userId, Long adminUserId) {
    // 1. Fetch actor and target users
    var actor = userRepository.findById(adminUserId).orElseThrow();
    var target = userRepository.findById(userId).orElseThrow();
    
    // 2. Validate: Cannot delete yourself
    if (target.getId().equals(actor.getId())) {
        logAction(adminUserId, "DELETE_USER_BLOCKED", ...);
        throw new IllegalArgumentException(...);
    }
    
    // 3. Validate: Cannot delete admin
    if ("ROLE_ADMIN".equalsIgnoreCase(target.getRole())) {
        logAction(adminUserId, "DELETE_USER_BLOCKED", ...);
        throw new IllegalArgumentException(...);
    }
    
    // 4. Delete
    userRepository.deleteById(userId);
    
    // 5. Log action
    logAction(adminUserId, "DELETE_USER", ...);
}
```

---

## ❤️ SINGLE RESPONSIBILITY PRINCIPLE - One Job Per Class

```
Your Classes:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Review.java
    └─ Responsibility: STORE REVIEW DATA
    └─ Reason to change: If review structure changes

Course.java
    └─ Responsibility: STORE COURSE DATA
    └─ Reason to change: If course structure changes

AdminService.java
    └─ Responsibility: HANDLE ADMIN OPERATIONS
    └─ Reason to change: If admin business logic changes

MyUserService.java
    └─ Responsibility: HANDLE USER REGISTRATION & AUTH
    └─ Reason to change: If user auth rules change

WeakPasswordException.java
    └─ Responsibility: REPRESENT PASSWORD ERRORS
    └─ Reason to change: If error format changes

❌ NOT MIXED: Review doesn't also do authentication
❌ NOT MIXED: Service doesn't also connect to database
❌ NOT MIXED: Exception doesn't also validate passwords
```

---

## 💉 DEPENDENCY INJECTION - Loose Coupling

```
BEFORE (Tight Coupling - BAD):
━━━━━━━━━━━━━━━━━━━━━━━━
public class AdminService {
    private UserRepository userRepository = new UserRepository();
                                            ↑
                                    HARDCODED CREATION
    // If UserRepository changes, AdminService must change 🔴
}

AFTER (Your Code - Dependency Injection - GOOD):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
public class AdminService {
    private final UserRepository userRepository;
    
    // Constructor receives dependency
    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
        // ↑ Can be any implementation of UserRepository
    }
    // If UserRepository changes, AdminService doesn't care 🟢
}

BENEFITS:
✅ Easy to test (pass mock repository)
✅ Easy to change (use different implementation)
✅ Loose coupling (depends on interface, not concrete class)
```

---

## 🔗 RELATIONSHIP MODELING - Objects Working Together

```
One-to-Many Relationships:
━━━━━━━━━━━━━━━━━━━━━━

Review.java (Lines 13-19):

    @ManyToOne               ← Many reviews
    @JoinColumn(name = "faculty_id")
    private FacultyEntity faculty;   ← Point to ONE faculty
    
    @ManyToOne               ← Many reviews
    @JoinColumn(name = "course_id")
    private Course course;           ← Point to ONE course

DATABASE:
    reviews table
    ├─ review_1 → faculty_1, course_1
    ├─ review_2 → faculty_1, course_2
    ├─ review_3 → faculty_2, course_1
    └─ review_4 → faculty_2, course_2

    One Faculty can have MANY reviews ✅
    One Course can have MANY reviews ✅
```

---

## 🛡️ IMMUTABILITY & STATE PROTECTION

```
Final Fields (AdminService.java Lines 21-28):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
private final UserRepository userRepository;        // Can't reassign
private final FacultyRepository facultyRepository;  // Can't reassign
private final ReviewRepository reviewRepository;    // Can't reassign

✅ Protection: Can't accidentally change what repositories to use
✅ Thread-safe: Final ensures visibility in multithreading

Controlled State Changes (AdminService.java):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

private void setUserEnabled(Long userId, boolean enabled, ...) {
    // Validation checklist:
    ✓ Is actor trying to modify themselves?   NO → can proceed
    ✓ Is actor trying to modify an admin?     NO → can proceed
    
    // ONLY THEN modify state
    target.setEnabled(enabled);      ← State change
    userRepository.save(target);     ← Persisted
    logAction(...);                  ← Logged
}

❌ Can't change state without validation
✅ Every state change is tracked in audit log
```

---

## 📊 OOP Score Card - YOUR PROJECT

| Principle | Score | Evidence |
|-----------|-------|----------|
| **Encapsulation** | ⭐⭐⭐⭐⭐ | All fields private, controlled getters/setters |
| **Inheritance** | ⭐⭐⭐ | Custom exceptions, lifecycle methods |
| **Polymorphism** | ⭐⭐⭐ | Method overriding, exception handling |
| **Abstraction** | ⭐⭐⭐⭐ | Service layer hides complexity |
| **SRP** | ⭐⭐⭐⭐⭐ | Each class has single responsibility |
| **DI** | ⭐⭐⭐⭐ | Constructor injection throughout |
| **Relationships** | ⭐⭐⭐⭐ | Proper entity modeling |
| **Immutability** | ⭐⭐⭐⭐ | Final fields, controlled changes |

---

## Key Takeaways: Your Code Quality

✅ **Professional-grade OOP implementation**
✅ **Strong separation of concerns**
✅ **Easy to test** (through DI)
✅ **Easy to maintain** (single responsibilities)
✅ **Easy to extend** (polymorphic design)
✅ **Data protected** (encapsulation + validation)
✅ **State protected** (immutable dependencies)

🎯 **Overall: EXCELLENT OOP practices!**

