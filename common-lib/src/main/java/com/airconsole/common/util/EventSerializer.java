package com.airconsole.common.util;

import com.airconsole.common.messaging.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

/**
 * Fast JSON serializer for events.
 * ObjectMapper is thread-safe and reused (latency optimization).
 */
public final class EventSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .registerModule(new ParameterNamesModule());

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    private EventSerializer() {
        // utility class
    }

    public static String serialize(Object event) throws JsonProcessingException {
        return MAPPER.writeValueAsString(event);
    }

    public static <T> T deserialize(String json, Class<T> type) throws JsonProcessingException {
        return MAPPER.readValue(json, type);
    }

    public static <T> T deserialize(String json, TypeReference<T> typeRef) throws JsonProcessingException {
        return MAPPER.readValue(json, typeRef);
    }

    public static String serializeEnvelope(EventEnvelope<?> envelope) throws JsonProcessingException {
        return MAPPER.writeValueAsString(envelope);
    }

    @SuppressWarnings("unchecked")
    public static <T> EventEnvelope<T> deserializeEnvelope(
            String json, Class<T> payloadType) throws JsonProcessingException {
        TypeReference<EventEnvelope<T>> typeRef = new TypeReference<>() {};
        return (EventEnvelope<T>) MAPPER.readValue(json, typeRef);
    }

    public static <T> EventEnvelope<T> deserializeEnvelopeSafe(
            String json, Class<T> payloadType) throws JsonProcessingException {
        // Deserialize envelope first, then assert payload type
        EventEnvelope<?> raw = MAPPER.readValue(json, EventEnvelope.class);
        T castPayload = MAPPER.convertValue(raw.getPayload(), payloadType);
        return new EventEnvelope<>(
            raw.getEventId(), raw.getEventType(), raw.getSource(),
            raw.getVersion(), raw.getTimestamp(), castPayload, raw.getRoomId()
        );
    }
}
