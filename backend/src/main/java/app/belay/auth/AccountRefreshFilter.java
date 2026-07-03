package app.belay.auth;

import app.belay.user.UserRepository;
import app.belay.user.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Recharge rôle et statut depuis la base à chaque requête authentifiée : une désactivation ou un
 * changement de rôle prend effet immédiatement, même si une session Redis est encore vivante.
 * Coût : une requête indexée par appel — acceptable à l'échelle d'un club.
 */
@Component
public class AccountRefreshFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public AccountRefreshFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            var user = userRepository.findById(principal.id()).orElse(null);
            if (user == null || user.getStatus() == UserStatus.DISABLED) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            } else if (user.getRole() != principal.role() || user.getStatus() != principal.status()) {
                UserPrincipal refreshed = UserPrincipal.from(user);
                SecurityContextHolder.getContext()
                        .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                                refreshed, null, refreshed.getAuthorities()));
            }
        }
        chain.doFilter(request, response);
    }
}
