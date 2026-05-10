package com.airconsole.player.application.dto;

import java.util.UUID;

public record TokenResponse(UUID playerId, String token, String role) {}
