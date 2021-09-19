package pl.poznachowski.feign.slf4j;

import static feign.Util.UTF_8;
import static feign.Util.decodeOrDefault;
import static feign.Util.valuesOrEmpty;
import static java.util.Map.entry;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.INFO;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.TRACE;
import static org.slf4j.event.Level.WARN;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import feign.Request;
import feign.Response;
import feign.Util;

/**
 * FeignLogger based on Slf4j logger level:
 * INFO -> Log only the request method and URL and the response status code and execution time.
 * DEBUG -> Additionally, log body of both request and response.
 * TRACE -> Additionally, log headers and metadata of both request and response.
 *
 * Feign Logger level has to be higher than NONE.
 * Otherwise things won't be logged.
 */
public class Slf4jLoggerLevelBasedFeignLogger extends feign.Logger {

    private final Logger logger;

    public Slf4jLoggerLevelBasedFeignLogger(Class<?> clazz) {
        this(LoggerFactory.getLogger(clazz));
    }

    Slf4jLoggerLevelBasedFeignLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected void logRequest(String configKey, Level logLevel, Request request) {
        log(INFO, configKey, "---> %s %s HTTP/1.1", request.httpMethod().name(), request.url());

        if (logger.isTraceEnabled()) {
            for (String field : request.headers().keySet()) {
                for (String value : valuesOrEmpty(request.headers(), field)) {
                    log(TRACE, configKey, "%s: %s", field, value);
                }
            }
        }

        int bodyLength = 0;
        if (request.body() != null) {
            bodyLength = request.length();
            if (logger.isDebugEnabled()) {
                String bodyText =
                        request.charset() != null
                                ? new String(request.body(), request.charset())
                                : null;
                log(DEBUG, configKey, ""); // CRLF
                log(DEBUG, configKey, "%s", bodyText != null ? bodyText : "Binary data");
            }
        }

        log(INFO, configKey, "---> END HTTP (%s-byte body)", bodyLength);
    }

    @Override
    protected void logRetry(String configKey, Level logLevel) {
        log(INFO, configKey, "---> RETRYING");
    }

    @Override
    protected Response logAndRebufferResponse(
            String configKey,
            Level logLevel,
            Response response,
            long elapsedTime
    )
            throws IOException {
        String reason =
                response.reason() != null && logLevel.compareTo(Level.NONE) > 0 ? " " + response.reason()
                        : "";
        int status = response.status();
        log(INFO, configKey, "<--- HTTP/1.1 %s%s (%sms)", status, reason, elapsedTime);

        if (logger.isTraceEnabled()) {
            for (String field : response.headers().keySet()) {
                for (String value : valuesOrEmpty(response.headers(), field)) {
                    log(TRACE, configKey, "%s: %s", field, value);
                }
            }
        }

        int bodyLength = 0;
        if (response.body() != null && !(status == 204 || status == 205)) {
            // HTTP 204 No Content "...response MUST NOT include a message-body"
            // HTTP 205 Reset Content "...response MUST NOT include an entity"
            if (logger.isDebugEnabled()) {
                log(DEBUG, configKey, ""); // CRLF
            }
            byte[] bodyData = Util.toByteArray(response.body().asInputStream());
            bodyLength = bodyData.length;
            if (logger.isDebugEnabled() && bodyLength > 0) {
                log(DEBUG, configKey, "%s", decodeOrDefault(bodyData, UTF_8, "Binary data"));
            }
            log(INFO, configKey, "<--- END HTTP (%s-byte body)", bodyLength);
            return response.toBuilder().body(bodyData).build();
        } else {
            log(INFO, configKey, "<--- END HTTP (%s-byte body)", bodyLength);
        }
        return response;
    }

    @Override
    protected IOException logIOException(
            String configKey,
            Level logLevel,
            IOException ioe,
            long elapsedTime
    ) {
        log(ERROR, configKey, "<--- ERROR %s: %s (%sms)", ioe.getClass().getSimpleName(), ioe.getMessage(),
                elapsedTime
        );
        if (logger.isDebugEnabled()) {
            StringWriter sw = new StringWriter();
            ioe.printStackTrace(new PrintWriter(sw));
            log(DEBUG, configKey, "%s", sw.toString());
            log(DEBUG, configKey, "<--- END ERROR");
        }
        return ioe;
    }

    /**
     * Shouldn't be used at all. Prepended with warning message, to be able to spot it.
     */
    @Override
    protected void log(String configKey, String format, Object... args) {
        log(WARN, configKey, String.format("[Should not log this!]" + methodTag(configKey) + format, args));
    }

    private void log(org.slf4j.event.Level level, String configKey, String format, Object... args) {
        // Not using SLF4J's support for parameterized messages (even though it would be more efficient)
        // because it would
        // require the incoming message formats to be SLF4J-specific.
        LOGGERS.get(level).accept(logger, String.format(methodTag(configKey) + format, args));
    }

    private static final Map<org.slf4j.event.Level, BiConsumer<Logger, String>> LOGGERS = Map.ofEntries(
            entry(ERROR, Logger::error),
            entry(WARN, Logger::warn),
            entry(INFO, Logger::info),
            entry(DEBUG, Logger::debug),
            entry(TRACE, Logger::trace)
    );
}
