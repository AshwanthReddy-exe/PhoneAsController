package com.airconsole.room.unit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.airconsole.common.events.RoomCreatedEvent;
import com.airconsole.room.application.RoomEventPublisher;
import com.airconsole.room.domain.Room;
import com.airconsole.room.domain.RoomCode;
import com.airconsole.room.domain.RoomRepository;
import com.airconsole.room.domain.RoomService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RoomServiceTest {

    RoomRepository repository;
    RoomEventPublisher eventPublisher;
    RoomService roomService;

    @BeforeEach
    void setUp() {
        repository = mock(RoomRepository.class);
        eventPublisher = mock(RoomEventPublisher.class);
        roomService = new RoomService(repository, eventPublisher);
    }

    @Test
    void createRoomGeneratesUniqueCode() {
        when(repository.existsByCode(any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID hostId = UUID.randomUUID();
        Room room = roomService.createRoom(hostId, "SNAKE", 4);

        assertThat(room.roomCode().value()).hasSize(5);
        assertThat(room.hostId()).isEqualTo(hostId);
        verify(eventPublisher).publish(any(RoomCreatedEvent.class));
    }

    @Test
    void createRoomRetriesOnCollision() {
        when(repository.existsByCode(any()))
            .thenReturn(true)
            .thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        roomService.createRoom(UUID.randomUUID(), "SNAKE", 4);

        verify(repository, times(2)).existsByCode(any());
    }

    @Test
    void findByCodeReturnsRoom() {
        Room room = Room.createNew(RoomCode.generate(), UUID.randomUUID(), "SNAKE", 4);
        when(repository.findByCode(room.roomCode().value())).thenReturn(Optional.of(room));

        Optional<Room> found = roomService.findByCode(room.roomCode().value());

        assertThat(found).isPresent();
    }

    @Test
    void joinRoomSuccess() {
        Room room = Room.createNew(RoomCode.generate(), UUID.randomUUID(), "SNAKE", 4);
        when(repository.findByCode(room.roomCode().value())).thenReturn(Optional.of(room));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<Room> joined = roomService.joinRoom(room.roomCode().value());

        assertThat(joined).isPresent();
        assertThat(joined.get().playerCount()).isOne();
    }

    @Test
    void joinRoomFailsWhenFull() {
        Room room = Room.createNew(RoomCode.generate(), UUID.randomUUID(), "SNAKE", 1)
            .incrementPlayerCount();
        when(repository.findByCode(room.roomCode().value())).thenReturn(Optional.of(room));

        Optional<Room> joined = roomService.joinRoom(room.roomCode().value());

        assertThat(joined).isEmpty();
    }

    @Test
    void joinRoomFailsWhenNotFound() {
        when(repository.findByCode("XXXXX")).thenReturn(Optional.empty());

        Optional<Room> joined = roomService.joinRoom("XXXXX");

        assertThat(joined).isEmpty();
    }

    @Test
    void deleteRoomByHost() {
        UUID hostId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        Room room = new Room.Builder()
            .roomId(roomId)
            .roomCode(RoomCode.generate())
            .hostId(hostId)
            .status(com.airconsole.common.enums.RoomStatus.WAITING)
            .gameType("SNAKE")
            .maxPlayers(4)
            .playerCount(0)
            .createdAt(java.time.Instant.now())
            .expiresAt(java.time.Instant.now().plusSeconds(3600))
            .build();

        when(repository.findById(roomId)).thenReturn(Optional.of(room));

        roomService.deleteRoom(roomId, hostId);

        verify(repository).delete(roomId);
    }

    @Test
    void deleteRoomForbiddenForNonHost() {
        UUID hostId = UUID.randomUUID();
        UUID otherPlayer = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        Room room = Room.createNew(RoomCode.generate(), hostId, "SNAKE", 4);

        when(repository.findById(roomId)).thenReturn(Optional.of(room));

        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalStateException.class,
            () -> roomService.deleteRoom(roomId, otherPlayer)
        );
    }
}
