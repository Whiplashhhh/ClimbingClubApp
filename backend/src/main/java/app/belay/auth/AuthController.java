package app.belay.auth;

import app.belay.auth.dto.LoginRequest;
import app.belay.auth.dto.MeResponse;
import app.belay.auth.dto.RegisterRequest;
import app.belay.common.NotFoundException;
import app.belay.user.AppUser;
import app.belay.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "auth", description = "Registration, login and session management")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserRepository userRepository;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            UserRepository userRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new account, creating or joining an organization")
    @ApiResponse(responseCode = "201", description = "Account created and session established")
    @ApiResponse(responseCode = "404", description = "joinSlug does not match any organization")
    @ApiResponse(responseCode = "409", description = "Email already registered")
    public MeResponse register(
            @Valid @RequestBody RegisterRequest body, HttpServletRequest request, HttpServletResponse response) {
        AppUser user = authService.register(body);
        establishSession(body.email(), body.password(), request, response);
        return MeResponse.from(user);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password (server session cookie)")
    @ApiResponse(responseCode = "200", description = "Session established")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @Transactional(readOnly = true)
    public MeResponse login(
            @Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        UserPrincipal principal = establishSession(body.email(), body.password(), request, response);
        return MeResponse.from(loadUser(principal));
    }

    @GetMapping("/csrf")
    @Operation(summary = "Prime the CSRF cookie and return the current token")
    public java.util.Map<String, String> csrf(org.springframework.security.web.csrf.CsrfToken token) {
        return java.util.Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping("/me")
    @Operation(summary = "Current authenticated user profile")
    @Transactional(readOnly = true)
    public MeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return MeResponse.from(loadUser(principal));
    }

    private AppUser loadUser(UserPrincipal principal) {
        return userRepository.findById(principal.id()).orElseThrow(() -> new NotFoundException("User not found"));
    }

    /** Authentifie et attache le contexte à la session serveur (cookie HttpOnly). */
    private UserPrincipal establishSession(
            String email, String password, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        // Protection contre la fixation de session : nouvel identifiant après login
        request.getSession(true);
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return (UserPrincipal) authentication.getPrincipal();
    }
}
