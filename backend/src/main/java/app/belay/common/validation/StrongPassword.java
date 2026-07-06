package app.belay.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mot de passe robuste : au moins 10 caractères, mélangeant au moins 3 des 4 classes (majuscule,
 * minuscule, chiffre, caractère spécial), hors mots de passe trop courants.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default
            "Password must be at least 10 characters and mix at least 3 of: uppercase, lowercase, digit, special character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
