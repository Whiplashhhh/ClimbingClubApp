package app.belay.route.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateHoldsRequest(@NotNull @Size(max = 200) List<@Valid HoldDto> holds) {}
