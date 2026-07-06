package app.belay.auth.dto;

import app.belay.organization.ClimbingType;
import app.belay.organization.Organization;
import app.belay.user.AppUser;
import app.belay.user.Role;
import app.belay.user.UserStatus;
import java.util.UUID;

public record MeResponse(
        UUID id,
        String email,
        String displayName,
        String avatarUrl,
        Role role,
        UserStatus status,
        OrganizationSummary organization) {

    public record OrganizationSummary(UUID id, String name, String slug, ClimbingType climbingType) {

        public static OrganizationSummary from(Organization organization) {
            return new OrganizationSummary(
                    organization.getId(),
                    organization.getName(),
                    organization.getSlug(),
                    organization.getClimbingType());
        }
    }

    public static MeResponse from(AppUser user, String avatarUrl) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                avatarUrl,
                user.getRole(),
                user.getStatus(),
                OrganizationSummary.from(user.getOrganization()));
    }
}
