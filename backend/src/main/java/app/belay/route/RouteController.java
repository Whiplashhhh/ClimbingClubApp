package app.belay.route;

import app.belay.auth.UserPrincipal;
import app.belay.route.dto.CreateRouteRequest;
import app.belay.route.dto.CreateSectorRequest;
import app.belay.route.dto.RouteResponse;
import app.belay.route.dto.SectorResponse;
import app.belay.route.dto.UpdateHoldsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Tag(name = "routes", description = "Wall map: sectors and their routes with hold annotations (tenant-scoped)")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/sectors")
    @Operation(summary = "List the club's sectors with their routes, photos served through signed URLs")
    public List<SectorResponse> listSectors(@AuthenticationPrincipal UserPrincipal principal) {
        return routeService.listSectors(principal);
    }

    @PostMapping(value = "/sectors", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a sector with an optional wall photo (admins)")
    public SectorResponse createSector(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestPart("meta") CreateSectorRequest meta,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return routeService.createSector(principal, meta, photo);
    }

    @DeleteMapping("/sectors/{sectorId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an empty sector (admins)")
    @ApiResponse(responseCode = "404", description = "Sector not found in the caller's organization")
    @ApiResponse(responseCode = "409", description = "The sector still has routes")
    public void deleteSector(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID sectorId) {
        routeService.deleteSector(principal, sectorId);
    }

    @PostMapping(value = "/routes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'COACH')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a route in a sector, with an optional photo (coaches and admins)")
    @ApiResponse(responseCode = "404", description = "Sector not found in the caller's organization")
    public RouteResponse createRoute(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestPart("meta") CreateRouteRequest meta,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return routeService.createRoute(principal, meta, photo);
    }

    @DeleteMapping("/routes/{routeId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'COACH')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a route (admins or its creator)")
    @ApiResponse(responseCode = "404", description = "Route not found in the caller's organization")
    public void deleteRoute(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID routeId) {
        routeService.deleteRoute(principal, routeId);
    }

    @PutMapping("/routes/{routeId}/holds")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'COACH')")
    @Operation(summary = "Replace the route's hold annotations (admins or its creator)")
    @ApiResponse(responseCode = "404", description = "Route not found in the caller's organization")
    public RouteResponse updateHolds(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID routeId,
            @Valid @RequestBody UpdateHoldsRequest body) {
        return routeService.updateHolds(principal, routeId, body);
    }
}
