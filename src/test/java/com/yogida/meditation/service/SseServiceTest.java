package com.yogida.meditation.service;

import com.yogida.meditation.dto.EntitlementEventMessage;
import com.yogida.meditation.service.sse.PendingEventQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitterFrames;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs without a servlet container. Spring's {@code ResponseBodyEmitter} buffers sends made before
 * it is attached to a response, so a subscribed emitter accepts frames here just as it would on a
 * live connection.
 */
class SseServiceTest {

    private static final String USER = "kc-user-1";
    private static final String CLIENT = "client-1";

    private final JsonMapper mapper = JsonMapper.builder().build();

    private PendingEventQueue pendingEvents;
    private SseService service;

    @BeforeEach
    void setUp() {
        pendingEvents = new PendingEventQueue();
        service = new SseService(pendingEvents, mapper);
    }

    @Test
    void publishWithNoConnection_queuesTheFrameAsJson() {
        EntitlementEventMessage message = message();

        service.publishToUser(USER, message);

        PendingEventQueue.Entry queued = pendingEvents.poll(USER);
        assertThat(queued).isNotNull();
        assertThat(queued.eventId()).isEqualTo("evt-1");
        assertThat(queued.type()).isEqualTo("CANCELLATION");
        assertThat(mapper.readValue(queued.json(), EntitlementEventMessage.class)).isEqualTo(message);
    }

    @Test
    void subscribe_drainsWhatWasQueued() {
        service.publishToUser(USER, message());
        assertThat(pendingEvents.size(USER)).isEqualTo(1);

        service.subscribe(USER, CLIENT);

        assertThat(pendingEvents.size(USER)).isZero();
    }

    @Test
    void publishAfterSubscribe_queuesNothing() {
        service.subscribe(USER, CLIENT);

        service.publishToUser(USER, message());

        assertThat(pendingEvents.size(USER)).isZero();
    }

    /**
     * What the app parses: the named event with the JSON as plain text. Handing the emitter the
     * message object instead would reach the client as a quoted string, which the app reads as a
     * legacy frame and never shows a banner for.
     */
    @Test
    void publish_writesTheEntitlementUpdateFrame() throws Exception {
        SseEmitter emitter = service.subscribe(USER, CLIENT);
        List<Object> written = SseEmitterFrames.record(emitter);
        written.clear(); // the `connected` handshake

        service.publishToUser(USER, message());

        assertThat(written).allSatisfy(item -> assertThat(item).isInstanceOf(String.class));
        String frame = String.join("", written.stream().map(String.class::cast).toList());
        assertThat(frame).isEqualTo("event:entitlement-update\ndata:" + mapper.writeValueAsString(message()) + "\n\n");
    }

    @Test
    void subscribe_sendsConnectedThenFlushesQueuedFrames() throws Exception {
        service.publishToUser(USER, message());

        SseEmitter emitter = service.subscribe(USER, CLIENT);
        String frames = String.join("", SseEmitterFrames.record(emitter).stream().map(String::valueOf).toList());

        assertThat(frames).startsWith("event:connected\ndata:\n\n");
        assertThat(frames).endsWith("event:entitlement-update\ndata:" + mapper.writeValueAsString(message()) + "\n\n");
    }

    /** A connection whose every write fails keeps the frame for the next subscribe. */
    @Test
    void publishToDeadConnection_queuesTheFrame() throws Exception {
        SseEmitter emitter = service.subscribe(USER, CLIENT);
        SseEmitterFrames.failEverySend(emitter);

        service.publishToUser(USER, message());

        PendingEventQueue.Entry queued = pendingEvents.poll(USER);
        assertThat(queued).isNotNull();
        assertThat(queued.eventId()).isEqualTo("evt-1");
    }

    /** The frame stays one line even when the shared mapper is configured to indent. */
    @Test
    void publish_ignoresIndentedOutputOnTheSharedMapper() {
        JsonMapper indenting = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
        SseService indentingService = new SseService(pendingEvents, indenting);

        indentingService.publishToUser(USER, message());

        PendingEventQueue.Entry queued = pendingEvents.poll(USER);
        assertThat(queued).isNotNull();
        assertThat(queued.json()).doesNotContain("\n").startsWith("{");
    }

    private static EntitlementEventMessage message() {
        return new EntitlementEventMessage(
                EntitlementEventMessage.VERSION, "evt-1", "CANCELLATION", "prod_month", null,
                List.of("premium"), "NORMAL", 1_767_225_000_000L, 1_767_829_800_000L, null,
                1_767_225_600_000L, "UNSUBSCRIBE", null, "APP_STORE", "PRODUCTION", false, 2,
                null, true);
    }
}
