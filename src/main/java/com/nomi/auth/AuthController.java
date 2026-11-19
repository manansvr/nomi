package com.nomi.auth;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @GetMapping("/auth/sign-in")
    public String signInPage(@RequestParam(defaultValue = "") String next, Model model) {
        model.addAttribute("next", safeNext(next));
        return "sign-in";
    }

    @GetMapping("/auth/sign-up")
    public String signUpPage(@RequestParam(defaultValue = "") String next, Model model) {
        model.addAttribute("next", safeNext(next));
        return "sign-up";
    }

    @PostMapping("/auth/sign-up")
    public String signUp(@RequestParam String name, @RequestParam String email, @RequestParam String password,
                         @RequestParam(defaultValue = "") String next, HttpSession session,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        AuthResult result = authService.signUp(name, email, password);
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("error", result.error());
            redirectAttributes.addFlashAttribute("name", name);
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/auth/sign-up?next=" + encode(safeNext(next));
        }
        request.changeSessionId();
        session.setAttribute("currentUser", result.user());
        return "redirect:" + safeNext(next);
    }

    @PostMapping("/auth/sign-in")
    public String signIn(@RequestParam String email, @RequestParam String password,
                         @RequestParam(defaultValue = "") String next, HttpSession session,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        AuthResult result = authService.signIn(email, password);
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("error", result.error());
            redirectAttributes.addFlashAttribute("email", email);
            return "redirect:/auth/sign-in?next=" + encode(safeNext(next));
        }
        request.changeSessionId();
        session.setAttribute("currentUser", result.user());
        return "redirect:" + safeNext(next);
    }

    @PostMapping("/auth/sign-out")
    public String signOut(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/sign-in";
    }

    private String safeNext(String next) {
        return next != null && next.startsWith("/") && !next.startsWith("//") && !next.contains("\\")
                && next.chars().noneMatch(Character::isISOControl) ? next : "/";
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
