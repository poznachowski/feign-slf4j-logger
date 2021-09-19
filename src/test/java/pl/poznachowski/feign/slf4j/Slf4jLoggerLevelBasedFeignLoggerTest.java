package pl.poznachowski.feign.slf4j;


import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import feign.Logger;
import feign.Request;
import feign.Request.HttpMethod;
import feign.RequestTemplate;
import feign.Response;
import feign.Util;
import uk.org.lidalia.slf4jext.Level;
import uk.org.lidalia.slf4jtest.LoggingEvent;
import uk.org.lidalia.slf4jtest.TestLogger;
import uk.org.lidalia.slf4jtest.TestLoggerFactory;

class Slf4jLoggerLevelBasedFeignLoggerTest {

    TestLogger testLogger = TestLoggerFactory.getTestLogger(Slf4jLoggerLevelBasedFeignLogger.class);

    private static final String CONFIG_KEY = "someMethod()";
    private static final Request REQUEST =
            new RequestTemplate()
                    .method(HttpMethod.GET)
                    .target("http://api.example.com")
                    .resolve(emptyMap())
                    .header("Content-Type", "application/json")
                    .request();
    private static final Response RESPONSE =
            Response.builder()
                    .status(200)
                    .reason("OK")
                    .request(Request.create(HttpMethod.GET, "/api", emptyMap(), null, Util.UTF_8))
                    .headers(Map.of("Content-Type", List.of( "application/json")))
                    .body("body", StandardCharsets.UTF_8)
                    .build();

    private final Slf4jLoggerLevelBasedFeignLogger logger = new Slf4jLoggerLevelBasedFeignLogger(
            Slf4jLoggerLevelBasedFeignLogger.class);

    @Test
    void should_have_proper_log_levels_on_logging_request() {
        logger.logRequest(CONFIG_KEY, Logger.Level.BASIC, REQUEST);
        assertThat(testLogger.getLoggingEvents())
                .extracting(LoggingEvent::getLevel, LoggingEvent::getMessage)
                .contains(
                        tuple(Level.INFO, "[someMethod] ---> GET http://api.example.com HTTP/1.1"),
                        tuple(Level.TRACE, "[someMethod] Content-Type: application/json"),
                        tuple(Level.INFO, "[someMethod] ---> END HTTP (0-byte body)")
                );
    }

    @Test
    void should_have_proper_log_levels_on_logging_retry() {
        logger.logRetry(CONFIG_KEY, Logger.Level.BASIC);
        assertThat(testLogger.getLoggingEvents())
                .extracting(LoggingEvent::getLevel, LoggingEvent::getMessage)
                .contains(tuple(Level.INFO, "[someMethod] ---> RETRYING"));
    }

    @Test
    void should_have_proper_log_levels_on_logging_exception() {
        logger.logIOException(CONFIG_KEY, Logger.Level.BASIC, new IOException("ex"), 1000L);
        assertThat(testLogger.getLoggingEvents())
                .extracting(LoggingEvent::getLevel, LoggingEvent::getMessage)
                .contains(tuple(Level.ERROR, "[someMethod] <--- ERROR IOException: ex (1000ms)"));
    }

    @Test
    void should_have_proper_log_levels_on_logging_response() throws IOException {
        logger.logAndRebufferResponse(CONFIG_KEY, Logger.Level.BASIC, RESPONSE, 1000L);
        assertThat(testLogger.getLoggingEvents())
                .extracting(LoggingEvent::getLevel, LoggingEvent::getMessage)
                .contains(
                        tuple(Level.INFO, "[someMethod] <--- HTTP/1.1 200 OK (1000ms)"),
                        tuple(Level.TRACE, "[someMethod] content-type: application/json"),
                        tuple(Level.DEBUG, "[someMethod] body"),
                        tuple(Level.INFO, "[someMethod] <--- END HTTP (4-byte body)")
                );
    }

    @AfterEach
    public void clearLoggers() {
        TestLoggerFactory.clear();
    }
}
