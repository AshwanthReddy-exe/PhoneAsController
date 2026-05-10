package com.airconsole.player.api.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateConnectionRequest(
    @NotNull UUID playerId,
    String connectionId   // null = disconnect
) {}
