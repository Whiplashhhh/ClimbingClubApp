package app.belay.account;

import app.belay.account.dto.AccountExportResponse;
import app.belay.auth.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@Tag(name = "account", description = "Account-wide operations (GDPR export and deletion)")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/export")
    @Operation(summary = "Export all of the caller's personal data (GDPR)")
    public AccountExportResponse export(@AuthenticationPrincipal UserPrincipal principal) {
        return accountService.export(principal);
    }
}
