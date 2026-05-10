package com.airconsole.room.infrastructure.config;

import com.airconsole.room.application.RoomEventPublisher;
import com.airconsole.room.domain.RoomRepository;
import com.airconsole.room.domain.RoomService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoomServiceConfig {

    @Bean
    public RoomService roomService(RoomRepository repository, RoomEventPublisher eventPublisher) {
        return new RoomService(repository, eventPublisher);
    }
}
