package pk.edu.nu.isb.bms.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MyUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MyUserService myUserService;

    private RegistrationRequest mockRequest;
    private MyUser mockUser;

    @BeforeEach
    void setUp() {
        // We use Mockito to create a fake request to pass into our methods
        mockRequest = mock(RegistrationRequest.class);

        mockUser = new MyUser();
        mockUser.setUsername("testuser");
        mockUser.setPassword("encodedPassword123");
        mockUser.setEnabled(true);
        mockUser.setAccountLocked(false);
        mockUser.setRole("STUDENT");
    }

    @Test
    void saveUser() {
        // Arrange
        when(userRepository.save(mockUser)).thenReturn(mockUser);

        // Act
        MyUser savedUser = myUserService.saveUser(mockUser);

        // Assert
        assertNotNull(savedUser);
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void registerUser_Success() {
        // Branch: Everything is valid, user is successfully registered

        // Arrange
        when(mockRequest.getUsername()).thenReturn("newuser");
        when(mockRequest.getEmail()).thenReturn("newuser@example.com");
        when(mockRequest.getPassword()).thenReturn("ValidPass123!");
        when(mockRequest.getConfirmPassword()).thenReturn("ValidPass123!");

        when(userRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("ValidPass123!")).thenReturn("encodedHash");

        // Act
        myUserService.registerUser(mockRequest);

        // Assert
        // Capture the MyUser object that was passed to the repository to verify its contents
        ArgumentCaptor<MyUser> userCaptor = ArgumentCaptor.forClass(MyUser.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        MyUser capturedUser = userCaptor.getValue();
        assertEquals("newuser", capturedUser.getUsername());
        assertEquals("newuser@example.com", capturedUser.getEmail());
        assertEquals("encodedHash", capturedUser.getPassword());
    }

    @Test
    void registerUser_DuplicateUsername() {
        // Branch: Username already exists
        when(mockRequest.getUsername()).thenReturn("existinguser");
        when(userRepository.existsByUsernameIgnoreCase("existinguser")).thenReturn(true);

        // Assert that the specific exception is thrown
        assertThrows(DuplicateUserFieldException.class, () -> {
            myUserService.registerUser(mockRequest);
        });
    }

    @Test
    void registerUser_DuplicateEmail() {
        // Branch: Email already exists
        when(mockRequest.getUsername()).thenReturn("newuser");
        when(mockRequest.getEmail()).thenReturn("existing@example.com");
        when(userRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        assertThrows(DuplicateUserFieldException.class, () -> {
            myUserService.registerUser(mockRequest);
        });
    }

    @Test
    void registerUser_PasswordMismatch() {
        // Branch: Passwords do not match
        when(mockRequest.getUsername()).thenReturn("newuser");
        when(mockRequest.getEmail()).thenReturn("new@example.com");
        when(mockRequest.getPassword()).thenReturn("Password123!");
        when(mockRequest.getConfirmPassword()).thenReturn("DifferentPass!");

        assertThrows(WeakPasswordException.class, () -> {
            myUserService.registerUser(mockRequest);
        });
    }

    // --- PASSWORD POLICY BRANCH TESTS ---

    @Test
    void registerUser_WeakPassword_TooShort() {
        setupMockRequestForPasswordPolicy("Short1!");
        assertThrows(WeakPasswordException.class, () -> myUserService.registerUser(mockRequest));
    }

    @Test
    void registerUser_WeakPassword_NoUppercase() {
        setupMockRequestForPasswordPolicy("nouppercase123!");
        assertThrows(WeakPasswordException.class, () -> myUserService.registerUser(mockRequest));
    }

    @Test
    void registerUser_WeakPassword_NoLowercase() {
        setupMockRequestForPasswordPolicy("NOLOWERCASE123!");
        assertThrows(WeakPasswordException.class, () -> myUserService.registerUser(mockRequest));
    }

    @Test
    void registerUser_WeakPassword_NoDigit() {
        setupMockRequestForPasswordPolicy("NoDigitsHere!");
        assertThrows(WeakPasswordException.class, () -> myUserService.registerUser(mockRequest));
    }

    @Test
    void registerUser_WeakPassword_NoSpecialChar() {
        setupMockRequestForPasswordPolicy("NoSpecialChar123");
        assertThrows(WeakPasswordException.class, () -> myUserService.registerUser(mockRequest));
    }

    // Helper method to keep policy tests clean
    private void setupMockRequestForPasswordPolicy(String passwordToTest) {
        when(mockRequest.getUsername()).thenReturn("newuser");
        when(mockRequest.getEmail()).thenReturn("new@example.com");
        when(mockRequest.getPassword()).thenReturn(passwordToTest);
        when(mockRequest.getConfirmPassword()).thenReturn(passwordToTest);
        when(userRepository.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
    }

    // --- LOAD USER BY USERNAME TESTS ---

    @Test
    void loadUserByUsername_Success() {
        // Branch: User is found in the database
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));

        UserDetails userDetails = myUserService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedPassword123", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT")));
    }

    @Test
    void loadUserByUsername_NotFound() {
        // Branch: User is NOT found
        when(userRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            myUserService.loadUserByUsername("unknownuser");
        });
    }
}