package app.belay.auth;

import app.belay.common.TooManyRequestsException;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Limiteur anti brute-force adossé à Redis (fenêtre fixe). Les échecs de connexion sont comptés
 * par email (dimension pertinente contre le bourrage d'identifiants) ; les inscriptions par IP
 * cliente (contre la création massive de comptes) — l'IP de bouclage est ignorée (santé/local).
 */
@Component
public class AuthRateLimiter {

    private final StringRedisTemplate redis;
    private final int loginMax;
    private final Duration loginWindow;
    private final int registerMax;
    private final Duration registerWindow;

    public AuthRateLimiter(
            StringRedisTemplate redis,
            @Value("${belay.ratelimit.login-max}") int loginMax,
            @Value("${belay.ratelimit.login-window-seconds}") long loginWindowSeconds,
            @Value("${belay.ratelimit.register-max}") int registerMax,
            @Value("${belay.ratelimit.register-window-seconds}") long registerWindowSeconds) {
        this.redis = redis;
        this.loginMax = loginMax;
        this.loginWindow = Duration.ofSeconds(loginWindowSeconds);
        this.registerMax = registerMax;
        this.registerWindow = Duration.ofSeconds(registerWindowSeconds);
    }

    /** Refuse la tentative de connexion si trop d'échecs récents pour cet email. */
    public void assertLoginAllowed(String email) {
        String key = loginKey(email);
        String current = redis.opsForValue().get(key);
        if (current != null && Integer.parseInt(current) >= loginMax) {
            throw new TooManyRequestsException("Too many failed login attempts; try again later");
        }
    }

    public void recordLoginFailure(String email) {
        increment(loginKey(email), loginWindow);
    }

    public void resetLogin(String email) {
        redis.delete(loginKey(email));
    }

    /** Compte et vérifie une inscription depuis cette IP (ignorée en bouclage) ; 429 au-delà. */
    public void recordAndCheckRegister(String ip) {
        if (isLoopback(ip)) {
            return;
        }
        long count = increment(registerKey(ip), registerWindow);
        if (count > registerMax) {
            throw new TooManyRequestsException("Too many sign-ups from this network; try again later");
        }
    }

    private long increment(String key, Duration ttl) {
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, ttl);
        }
        return count == null ? 0L : count;
    }

    private String loginKey(String email) {
        return "belay:rl:login:" + email.trim().toLowerCase(Locale.ROOT);
    }

    private String registerKey(String ip) {
        return "belay:rl:register:" + ip;
    }

    private boolean isLoopback(String ip) {
        return ip == null || ip.startsWith("127.") || ip.equals("::1") || ip.equals("0:0:0:0:0:0:0:1");
    }
}
