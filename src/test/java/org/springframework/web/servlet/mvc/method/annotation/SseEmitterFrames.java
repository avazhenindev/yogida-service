package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Attaches a recording handler to an {@link SseEmitter}, the way Spring MVC does when the emitter is
 * returned from a controller, so a test can read the bytes that would have gone onto the wire.
 *
 * <p>It lives in Spring's package because {@code ResponseBodyEmitter.initialize} and its
 * {@code Handler} are package-private. Sends made before attaching are buffered by the emitter and
 * replayed into the handler, so frames from {@code subscribe()} are captured too.
 */
public final class SseEmitterFrames {

    private SseEmitterFrames() {
    }

    /**
     * Everything sent through the emitter, in order. Each item must be text: an object here would
     * go through a JSON converter at runtime and reach the client quoted.
     */
    public static List<Object> record(SseEmitter emitter) throws IOException {
        RecordingHandler handler = new RecordingHandler();
        emitter.initialize(handler);
        return handler.written;
    }

    /**
     * Attaches a handler whose every later write fails, as a dead client's would. The frames the
     * emitter buffered before attaching are accepted, so it attaches cleanly.
     */
    public static void failEverySend(SseEmitter emitter) throws IOException {
        RecordingHandler handler = new RecordingHandler();
        emitter.initialize(handler);
        handler.failing = true;
    }

    private static final class RecordingHandler implements ResponseBodyEmitter.Handler {

        private final List<Object> written = new ArrayList<>();
        private boolean failing;

        @Override
        public void send(Object data, MediaType mediaType) throws IOException {
            if (failing) throw new IOException("Broken pipe");
            written.add(data);
        }

        @Override
        public void send(Set<ResponseBodyEmitter.DataWithMediaType> items) throws IOException {
            if (failing) throw new IOException("Broken pipe");
            items.forEach(item -> written.add(item.getData()));
        }

        @Override
        public void complete() {
        }

        @Override
        public void completeWithError(Throwable failure) {
        }

        @Override
        public void onTimeout(Runnable callback) {
        }

        @Override
        public void onError(Consumer<Throwable> callback) {
        }

        @Override
        public void onCompletion(Runnable callback) {
        }
    }
}
