package pl.poznachowski.feign.slf4j;

import java.io.IOException;

import org.slf4j.Logger;

import feign.Request;
import feign.Response;

class MethodsExposedFeignLogger extends Slf4jLoggerLevelBasedFeignLogger {

    public MethodsExposedFeignLogger(Class<?> clazz) {
        super(clazz);
    }

    public MethodsExposedFeignLogger(Logger logger) {
        super(logger);
    }

    @Override
    public void logRequest(String configKey, Level logLevel, Request request) {
        super.logRequest(configKey, logLevel, request);
    }

    @Override
    public void logRetry(String configKey, Level logLevel) {
        super.logRetry(configKey, logLevel);
    }

    @Override
    public Response logAndRebufferResponse(
            String configKey, Level logLevel, Response response, long elapsedTime
    ) throws IOException {
        return super.logAndRebufferResponse(configKey, logLevel, response, elapsedTime);
    }

    @Override
    public IOException logIOException(
            String configKey, Level logLevel, IOException ioe, long elapsedTime
    ) {
        return super.logIOException(configKey, logLevel, ioe, elapsedTime);
    }
}
