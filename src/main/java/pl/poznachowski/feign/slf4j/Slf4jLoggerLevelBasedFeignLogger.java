package pl.poznachowski.feign.slf4j;
import static java.util.Map.entry;

import java.util.Map;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FeignLogger based on Slf4j logger level:
 * INFO -> Log only the request method and URL and the response status code and execution time.
 * DEBUG -> Additionally, log body of both request and response.
 * TRACE -> Additionally, log headers and metadata of both request and response.
 * <p>
 * Feign Logger level has to be set to FULL.
 * Otherwise, things won't get logged.
 */
public class Slf4jLoggerLevelBasedFeignLogger extends feign.Logger {

    private static final Map<String, BiConsumer<Logger, String>> LOGGER_BY_FORMAT = Map.ofEntries(
            entry("<--- ERROR %s: %s (%sms)", Logger::error),
            entry("<--- END ERROR", Logger::debug),
            entry("<--- HTTP/1.1 %s%s (%sms)", Logger::info),
            entry("---> RETRYING", Logger::info),
            entry("---> %s %s HTTP/1.1", Logger::info),
            entry("---> END HTTP (%s-byte body)", Logger::info),
            entry("<--- END HTTP (%s-byte body)", Logger::info),
            entry("", Logger::debug),
            entry("%s", Logger::debug),
            entry("%s: %s", Logger::trace)
    );

    private final Logger logger;

    public Slf4jLoggerLevelBasedFeignLogger(Class<?> clazz) {
        this(LoggerFactory.getLogger(clazz));
    }

    public Slf4jLoggerLevelBasedFeignLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected void log(String configKey, String format, Object... args) {
        LOGGER_BY_FORMAT.get(format).accept(logger, String.format(methodTag(configKey) + format, args));
    }

}
