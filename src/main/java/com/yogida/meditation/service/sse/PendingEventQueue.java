package com.yogida.meditation.service.sse;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Events held for users who had no deliverable connection when they were published.
 *
 * <p>Separated from {@code SseService} because it is the one piece of that class with its own
 * storage and its own invariant. The service decides who to deliver to and how; this decides what
 * survives until they reconnect.
 *
 * <p>The per-user cap is the invariant worth isolating. Without it a user who never reconnects
 * accumulates events for the lifetime of the process, and entitlement events are published on
 * every RevenueCat webhook — so a single lapsed subscriber becomes an unbounded queue. Oldest are
 * dropped first: an entitlement update supersedes the one before it, so the newest is the one that
 * matters.
 */
@Log4j2
@Component
public class PendingEventQueue {

    private static final int MAX_PENDING_EVENTS_PER_USER = 20;

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<String>> pending =
        new ConcurrentHashMap<>();

    /** Holds an event for a user, dropping the oldest once the cap is reached. */
    public void enqueue(String keycloakUserId, String event) {
        ConcurrentLinkedDeque<String> queue =
            pending.computeIfAbsent(keycloakUserId, k -> new ConcurrentLinkedDeque<>());
        queue.offerLast(event);
        while (queue.size() > MAX_PENDING_EVENTS_PER_USER) {
            queue.pollFirst();
        }
        log.info("PendingEventQueue > No deliverable SSE connection for user {}; event queued (pending: {})",
            keycloakUserId, queue.size());
    }

    /** The next held event for a user, or null when there are none. */
    public String poll(String keycloakUserId) {
        ConcurrentLinkedDeque<String> queue = pending.get(keycloakUserId);
        return queue == null ? null : queue.pollFirst();
    }

    /**
     * Puts an event back at the front.
     *
     * <p>Used when delivery fails part-way through a flush: the event is not lost and, because it
     * goes back to the front rather than the end, the order the user eventually sees is the order
     * they were published in.
     */
    public void returnToFront(String keycloakUserId, String event) {
        pending.computeIfAbsent(keycloakUserId, k -> new ConcurrentLinkedDeque<>()).offerFirst(event);
    }

    /** How many events are still held for a user. */
    public int size(String keycloakUserId) {
        ConcurrentLinkedDeque<String> queue = pending.get(keycloakUserId);
        return queue == null ? 0 : queue.size();
    }
}
