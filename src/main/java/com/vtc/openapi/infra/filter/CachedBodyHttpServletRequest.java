package com.vtc.openapi.infra.filter;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 缓存请求体的 HttpServletRequestWrapper，支持 body 多次读取（幂等校验用）。
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = readAll(request);
    }

    private static byte[] readAll(HttpServletRequest request) throws IOException {
        int contentLength = request.getContentLength();
        int size = contentLength > 0 ? contentLength : 256;
        try (ServletInputStream is = request.getInputStream();
             java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(size)) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = is.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
            return baos.toByteArray();
        }
    }

    public byte[] getCachedBody() {
        return cachedBody;
    }

    public String getCachedBodyAsString() {
        return cachedBody != null ? new String(cachedBody, StandardCharsets.UTF_8) : "";
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    private static class CachedServletInputStream extends ServletInputStream {
        private final ByteArrayInputStream delegate;

        CachedServletInputStream(byte[] body) {
            this.delegate = new ByteArrayInputStream(body != null ? body : new byte[0]);
        }

        @Override
        public boolean isFinished() {
            return delegate.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() {
            return delegate.read();
        }
    }
}