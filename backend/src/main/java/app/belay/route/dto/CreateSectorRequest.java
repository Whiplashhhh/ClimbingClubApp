package app.belay.route.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSectorRequest(@NotBlank @Size(max = 120) String name) {}
