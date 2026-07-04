package app.belay.route;

import app.belay.auth.UserPrincipal;
import app.belay.common.ConflictException;
import app.belay.common.NotFoundException;
import app.belay.organization.OrganizationRepository;
import app.belay.route.dto.CreateRouteRequest;
import app.belay.route.dto.CreateSectorRequest;
import app.belay.route.dto.HoldDto;
import app.belay.route.dto.RouteResponse;
import app.belay.route.dto.SectorResponse;
import app.belay.route.dto.UpdateHoldsRequest;
import app.belay.storage.StorageService;
import app.belay.user.Role;
import app.belay.user.UserRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RouteService {

    private final SectorRepository sectorRepository;
    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final StorageService storageService;

    public RouteService(
            SectorRepository sectorRepository,
            RouteRepository routeRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            StorageService storageService) {
        this.sectorRepository = sectorRepository;
        this.routeRepository = routeRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.storageService = storageService;
    }

    /** Cartographie du club : secteurs triés par nom, avec leurs voies (photos servies via /api/media). */
    @Transactional(readOnly = true)
    public List<SectorResponse> listSectors(UserPrincipal principal) {
        Map<UUID, List<RouteResponse>> routesBySector =
                routeRepository.findAllByOrganizationId(principal.organizationId()).stream()
                        .collect(Collectors.groupingBy(
                                route -> route.getSector().getId(),
                                Collectors.mapping(this::toResponse, Collectors.toList())));
        return sectorRepository.findAllByOrganizationIdOrderByNameAsc(principal.organizationId()).stream()
                .map(sector -> SectorResponse.from(
                        sector,
                        publicUrlOrNull(sector.getPhotoObjectKey()),
                        routesBySector.getOrDefault(sector.getId(), List.of())))
                .toList();
    }

    @Transactional
    public SectorResponse createSector(UserPrincipal principal, CreateSectorRequest request, MultipartFile photo) {
        String objectKey = photo == null || photo.isEmpty()
                ? null
                : storageService.storeImage(bytesOf(photo), "sectors/" + principal.organizationId());
        Sector sector = sectorRepository.save(new Sector(
                organizationRepository.getReferenceById(principal.organizationId()), request.name(), objectKey));
        return SectorResponse.from(sector, publicUrlOrNull(objectKey), List.of());
    }

    @Transactional
    public void deleteSector(UserPrincipal principal, UUID sectorId) {
        Sector sector = sectorRepository
                .findByIdAndOrganizationId(sectorId, principal.organizationId())
                .orElseThrow(() -> new NotFoundException("Sector not found"));
        if (routeRepository.countBySectorId(sectorId) > 0) {
            throw new ConflictException("The sector still has routes");
        }
        String objectKey = sector.getPhotoObjectKey();
        sectorRepository.delete(sector);
        if (objectKey != null) {
            storageService.delete(objectKey);
        }
    }

    @Transactional
    public RouteResponse createRoute(UserPrincipal principal, CreateRouteRequest request, MultipartFile photo) {
        Sector sector = sectorRepository
                .findByIdAndOrganizationId(request.sectorId(), principal.organizationId())
                .orElseThrow(() -> new NotFoundException("Sector not found"));
        String objectKey = photo == null || photo.isEmpty()
                ? null
                : storageService.storeImage(bytesOf(photo), "routes/" + principal.organizationId());
        Route route = routeRepository.save(new Route(
                organizationRepository.getReferenceById(principal.organizationId()),
                sector,
                userRepository.getReferenceById(principal.id()),
                request.name(),
                request.grade(),
                request.climbType(),
                objectKey));
        return toResponse(route);
    }

    @Transactional
    public void deleteRoute(UserPrincipal principal, UUID routeId) {
        Route route = findManagedRoute(principal, routeId);
        String objectKey = route.getPhotoObjectKey();
        routeRepository.delete(route);
        if (objectKey != null) {
            storageService.delete(objectKey);
        }
    }

    /** Les annotations de prises sont posées par le créateur de la voie ou un admin. */
    @Transactional
    public RouteResponse updateHolds(UserPrincipal principal, UUID routeId, UpdateHoldsRequest request) {
        Route route = findManagedRoute(principal, routeId);
        route.setHolds(request.holds().stream().map(HoldDto::toHold).toList());
        return toResponse(route);
    }

    /** Scope tenancy (hors org → 404) puis autorisation : admins, ou le créateur de la voie. */
    private Route findManagedRoute(UserPrincipal principal, UUID routeId) {
        Route route = routeRepository
                .findByIdAndOrganizationId(routeId, principal.organizationId())
                .orElseThrow(() -> new NotFoundException("Route not found"));
        boolean isAdmin = principal.role() == Role.OWNER || principal.role() == Role.ADMIN;
        if (!isAdmin && !route.getCreatedBy().getId().equals(principal.id())) {
            throw new AccessDeniedException("Only admins or the route's creator can manage this route");
        }
        return route;
    }

    private RouteResponse toResponse(Route route) {
        return RouteResponse.from(route, publicUrlOrNull(route.getPhotoObjectKey()));
    }

    private String publicUrlOrNull(String objectKey) {
        return objectKey == null ? null : storageService.publicUrl(objectKey);
    }

    private byte[] bytesOf(MultipartFile photo) {
        try {
            return photo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file", e);
        }
    }
}
