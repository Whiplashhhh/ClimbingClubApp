package app.belay.slot.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddSlotMemberRequest(@NotNull UUID userId) {}
