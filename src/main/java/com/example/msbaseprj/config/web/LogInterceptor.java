package com.example.msbaseprj.config.web;

import org.slf4j.Logger;
import org.springframework.web.servlet.HandlerInterceptor;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LogInterceptor implements HandlerInterceptor {
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(LogInterceptor.class);
    private final Tracer tracer;

    public LogInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler) throws Exception {
        request.setAttribute("startTime", System.currentTimeMillis());
        setSpanHeader(response);

        logger.info("Incoming request: " + request.getMethod() + " " + request.getRequestURI());
        return true; // Continue with the next interceptor or the handler method
    }

    public void postHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler,
                            org.springframework.web.servlet.ModelAndView modelAndView) throws Exception {
    }

    private void setSpanHeader(HttpServletResponse response) {
        var httpResponse = (HttpServletResponse) response;
        var span = getSpan();
        if (span != null) {
            logger.trace("Setting trace headers: traceId={}, spanId={}", span.context().traceId(), span.context().spanId());
            httpResponse.setHeader("X-Trace-ID", span.context().traceId());
            httpResponse.setHeader("X-Span-ID",  span.context().spanId());
        } else {
            logger.warn("No active span found, skipping trace headers");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {
        long startTime = (Long)request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Completed: " + request.getMethod() + " " + request.getRequestURI()
                + " [" + response.getStatus() + "] in " + duration + "ms");
    }

    private Span getSpan() {
        if (tracer == null) {
            logger.warn("Tracer is not available, cannot set trace headers");
            return null;
        } 
        return tracer.currentSpan();
    }

}
