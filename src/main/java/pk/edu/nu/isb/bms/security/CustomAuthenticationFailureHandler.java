package pk.edu.nu.isb.bms.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        if (exception instanceof LockedException) {
            response.sendRedirect("/login?locked=true");
            return;
        }
        if (exception instanceof DisabledException) {
            response.sendRedirect("/login?disabled=true");
            return;
        }
        if (exception instanceof BadCredentialsException) {
            response.sendRedirect("/login?invalid=true");
            return;
        }
        response.sendRedirect("/login?error=true");
    }
}

