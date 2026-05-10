package com.airconsole.room.api;

import com.airconsole.room.api.request.CreateRoomRequest;
import com.airconsole.room.api.request.JoinRoomRequest;
import com.airconsole.room.application.dto.RoomResponse;
import com.airconsole.room.domain.Room;
import com.airconsole.room.domain.RoomService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for room lifecycle.
 * Thin layer — all logic delegated to domain RoomService.
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private static final Logger LOG = LoggerFactory.getLogger(RoomController.class);

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        LOG.info("Creating room for host={}, game={}", request.hostId(), request.gameType());
        Room room = roomService.createRoom(request.hostId(), request.gameType(), request.maxPlayers());
        return ResponseEntity.created(URI.create("/api/rooms/" + room.roomCode().value()))
            .body(toResponse(room));
    }

    @PostMapping("/join")
    public ResponseEntity<RoomResponse> joinRoom(@Valid @RequestBody JoinRoomRequest request) {
        LOG.info("Joining room code={}", request.roomCode());
        Optional<Room> room = roomService.joinRoom(request.roomCode().toUpperCase());
        return room.map(r -> ResponseEntity.ok(toResponse(r)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{code}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String code) {
        Optional<Room> room = roomService.findByCode(code.toUpperCase());
        return room.map(r -> ResponseEntity.ok(toResponse(r)))
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable UUID roomId,
            @RequestHeader("X-Player-Id") UUID playerId) {
        roomService.deleteRoom(roomId, playerId);
        return ResponseEntity.noContent().build();
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
            room.roomId(),
            room.roomCode().value(),
            room.status(),
            room.hostId(),
            room.gameType(),
            room.maxPlayers(),
            room.playerCount(),
            room.createdAt(),
            room.expiresAt()
        );
    }
}
