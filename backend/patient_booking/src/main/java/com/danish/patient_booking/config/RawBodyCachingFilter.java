package com.danish.patient_booking.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * Stripe signature verification requires the EXACT raw bytes of the request body.
 * Spring's default HttpServletRequest input stream can only be read ONCE —
 * after @RequestBody reads it, it's gone. This filter caches the bytes so
 * Webhook.constructEvent() can re-read them for signature verification.
 */
@Component
public class RawBodyCachingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest &&
                httpRequest.getRequestURI().contains("/api/webhooks/stripe")) {

            // Cache the raw body bytes before Spring reads them
            byte[] rawBody = httpRequest.getInputStream().readAllBytes();
            chain.doFilter(new CachedBodyHttpServletRequest(httpRequest, rawBody), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Wraps the request so getInputStream() can be called multiple times
     * returning the same cached bytes each time.
     */
    public static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

        private final byte[] cachedBody;

        CachedBodyHttpServletRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.cachedBody = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override public int read() { return byteStream.read(); }
                @Override public boolean isFinished() { return byteStream.available() == 0; }
                @Override public boolean isReady() { return true; }
                @Override public void setReadListener(ReadListener l) {}
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }

        // Expose raw bytes for direct access in controller
        public byte[] getRawBody() { return cachedBody; }
    }
}