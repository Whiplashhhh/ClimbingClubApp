package app.belay.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;
import java.util.Set;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private static final int MIN_LENGTH = 10;

    // Petite liste anti-évidences (comparée en minuscules) ; la vraie robustesse vient des règles.
    private static final Set<String> COMMON = Set.of(
            "password",
            "password1",
            "motdepasse",
            "motdepasse1",
            "12345678",
            "123456789",
            "1234567890",
            "azertyuiop",
            "qwertyuiop",
            "iloveyou");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // @NotBlank gère l'absence
        }
        if (value.length() < MIN_LENGTH) {
            return false;
        }
        if (COMMON.contains(value.toLowerCase(Locale.ROOT))) {
            return false;
        }
        int classes = 0;
        if (value.chars().anyMatch(Character::isUpperCase)) {
            classes++;
        }
        if (value.chars().anyMatch(Character::isLowerCase)) {
            classes++;
        }
        if (value.chars().anyMatch(Character::isDigit)) {
            classes++;
        }
        if (value.chars().anyMatch(c -> !Character.isLetterOrDigit(c))) {
            classes++;
        }
        return classes >= 3;
    }
}
