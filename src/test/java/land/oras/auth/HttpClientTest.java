/*-
 * =LICENSE=
 * ORAS Java SDK
 * ===
 * Copyright (C) 2024 - 2025 ORAS
 * ===
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =LICENSEEND=
 */

package land.oras.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class HttpClientTest {

    @Test
    void testIsSameOriginSame() {
        URI uri1 = URI.create("https://example.com:443/path");
        URI uri2 = URI.create("https://example.com/path2");
        assertTrue(HttpClient.isSameOrigin(uri1, uri2));
    }

    @Test
    void testIsSameOriginDifferentPort() {
        URI uri1 = URI.create("http://example.com:8080");
        URI uri2 = URI.create("http://example.com");
        assertFalse(HttpClient.isSameOrigin(uri1, uri2));
    }

    @Test
    void testIsSameOriginDifferentScheme() {
        URI uri1 = URI.create("http://example.com");
        URI uri2 = URI.create("https://example.com");
        assertFalse(HttpClient.isSameOrigin(uri1, uri2));
    }

    @Test
    void testIsSameOriginDifferentHost() {
        URI uri1 = URI.create("http://example.com");
        URI uri2 = URI.create("http://example.org");
        assertFalse(HttpClient.isSameOrigin(uri1, uri2));
    }

    @Test
    void testGetPortExplicit() {
        URI uri = URI.create("http://example.com:8080");
        assertEquals(8080, HttpClient.getPort(uri));
    }

    @Test
    void testGetPortDefaultHttp() {
        URI uri = URI.create("http://example.com");
        assertEquals(80, HttpClient.getPort(uri));
    }

    @Test
    void testGetPortDefaultHttps() {
        URI uri = URI.create("https://example.com");
        assertEquals(443, HttpClient.getPort(uri));
    }

    @Test
    void testShouldRedirectTrue301() {
        HttpResponse<?> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(HttpURLConnection.HTTP_MOVED_PERM);
        assertTrue(HttpClient.shouldRedirect(response));
    }

    @Test
    void testShouldRedirectTrue302() {
        HttpResponse<?> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(HttpURLConnection.HTTP_MOVED_TEMP);
        assertTrue(HttpClient.shouldRedirect(response));
    }

    @Test
    void testShouldRedirectTrue307() {
        HttpResponse<?> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(307);
        assertTrue(HttpClient.shouldRedirect(response));
    }

    @Test
    void testShouldRedirectFalse() {
        HttpResponse<?> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        assertFalse(HttpClient.shouldRedirect(response));
    }
}
