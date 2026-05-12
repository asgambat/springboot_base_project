package com.example.msbaseprj.api.configuration.client.interceptor;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class TraceIdPropagationInterceptor implements ClientHttpRequestInterceptor {
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {
        var traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isEmpty()) 
            request.getHeaders().add(TRACE_ID_HEADER, traceId);
            
        return execution.execute(request, body);
    }

}
