package com.airconsole.player.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RegisterPlayerRequest(
    @NotNull UUID roomId,
    @NotBlank String playerName
) {}
