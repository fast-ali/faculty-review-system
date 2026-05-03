package pk.edu.nu.isb.bms.models;

import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class MyUserService implements UserDetailsService {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]");

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public MyUser saveUser(MyUser user) {
        return userRepository.save(user);
    }

    public void registerUser(RegistrationRequest request) {
        String username = safeTrim(request.getUsername());
        String email = safeTrim(request.getEmail()).toLowerCase();
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateUserFieldException("Username already exists.");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateUserFieldException("Email already exists.");
        }

        if (!password.equals(confirmPassword)) {
            throw new WeakPasswordException("Password and confirmation do not match.");
        }

        validatePasswordPolicy(password);

        MyUser user = new MyUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        // default role set in entity pre-persist
        saveUser(user);
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new WeakPasswordException("Password must be between 8 and 72 characters.");
        }
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            throw new WeakPasswordException("Password must contain at least one uppercase letter.");
        }
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            throw new WeakPasswordException("Password must contain at least one lowercase letter.");
        }
        if (!DIGIT_PATTERN.matcher(password).find()) {
            throw new WeakPasswordException("Password must contain at least one digit.");
        }
        if (!SPECIAL_PATTERN.matcher(password).find()) {
            throw new WeakPasswordException("Password must contain at least one special character.");
        }
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
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
                    .roles(role) // map DB role to Spring Security roles
                    .build();
        }
        throw new UsernameNotFoundException(username);
    }
}
