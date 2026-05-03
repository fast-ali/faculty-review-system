package pk.edu.nu.isb.bms.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pk.edu.nu.isb.bms.models.DuplicateUserFieldException;
import pk.edu.nu.isb.bms.models.RegistrationRequest;
import pk.edu.nu.isb.bms.models.WeakPasswordException;
import jakarta.validation.ConstraintViolationException;

import java.util.NoSuchElementException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateUserFieldException.class)
    public String handleDuplicateUserField(DuplicateUserFieldException ex, Model model) {
        model.addAttribute("signupError", ex.getMessage());
        if (!model.containsAttribute("registrationRequest")) {
            model.addAttribute("registrationRequest", new RegistrationRequest());
        }
        return "signup";
    }

    @ExceptionHandler(WeakPasswordException.class)
    public String handleWeakPassword(WeakPasswordException ex, Model model) {
        model.addAttribute("signupError", ex.getMessage());
        if (!model.containsAttribute("registrationRequest")) {
            model.addAttribute("registrationRequest", new RegistrationRequest());
        }
        return "signup";
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ConstraintViolationException.class})
    public String handleDatabaseConstraint(Exception ex, Model model) {
        model.addAttribute("signupError", "Unable to process request due to data constraints. Please verify your input.");
        if (!model.containsAttribute("registrationRequest")) {
            model.addAttribute("registrationRequest", new RegistrationRequest());
        }
        return "signup";
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, NoSuchElementException.class})
    public String handleAdminAndValidationErrors(RuntimeException ex, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        redirectAttributes.addFlashAttribute("adminError", ex.getMessage());

        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/admin")) {
            return "redirect:/admin";
        }
        return "redirect:/";
    }
}
