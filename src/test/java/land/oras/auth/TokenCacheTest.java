/*-
 * =LICENSE=
 * ORAS Java SDK
 * ===
 * Copyright (C) 2024 - 2026 ORAS
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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.TimeUnit;
import land.oras.ContainerRef;
import land.oras.TestUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class TokenCacheTest {

    @BeforeAll
    static void beforeAll() {
        TokenCache.get(Scopes.empty(
                ContainerRef.parse("foo/bar:tag"),
                "fake-service-init")); // Initialize the cache to ensure it's ready for testing
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldLookupWithGlobalScope() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        TokenCache.setMeterRegistry(meterRegistry);
        HttpClient.TokenResponse tokenResponse =
                new HttpClient.TokenResponse("other-token", null, "dockerhub", 1, null);
        ContainerRef containerRef = ContainerRef.parse("docker.io/library/alpine:latest");
        Scopes scopes = Scopes.of(containerRef, Scope.PULL).withAddedGlobalScopes("aws");
        TokenCache.put(scopes, tokenResponse);
        assertEquals(
                tokenResponse,
                TokenCache.get(Scopes.empty(containerRef, "dockerhub").withAddedGlobalScopes("aws")),
                "Should retrieve the token before expiration");
        TestUtils.dumpMetrics(meterRegistry);

        // At least one hit
        assertTrue(
                meterRegistry.find("cache.gets").tags("result", "hit").functionCounters().stream()
                                .mapToDouble(FunctionCounter::count)
                                .sum()
                        >= 1,
                "Should have at least one cache hit");
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    void shouldAddAndRetrieveTokenThenExpiredIt() throws InterruptedException {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        TokenCache.setMeterRegistry(meterRegistry);
        HttpClient.TokenResponse tokenResponse =
                new HttpClient.TokenResponse("other-token", null, "dockerhub", 1, null);
        ContainerRef containerRef = ContainerRef.parse("docker.io/library/alpine0:latest");
        Scopes scopes = Scopes.of(containerRef, Scope.PULL).withService("dockerhub"); // Pull only
        TokenCache.put(scopes, tokenResponse);
        assertEquals(tokenResponse, TokenCache.get(scopes), "Should retrieve the token before expiration");
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> TokenCache.get(scopes) == null
                        && meterRegistry.find("cache.evictions").functionCounters().stream()
                                        .mapToDouble(FunctionCounter::count)
                                        .sum()
                                >= 1);
        TestUtils.dumpMetrics(meterRegistry);
    }

    @Test
    void shouldRetrieveTokenPullTokenUsingPushPullScope() throws InterruptedException {
        HttpClient.TokenResponse tokenResponse =
                new HttpClient.TokenResponse("other-token", null, "dockerhub", 3600, null);
        ContainerRef containerRef = ContainerRef.parse("docker.io/library/alpine1:latest");
        Scopes scopes = Scopes.of(containerRef, Scope.PULL, Scope.PUSH); // Pull push
        TokenCache.put(scopes, tokenResponse);
        Scopes pullOnlyScopes = Scopes.of(containerRef, Scope.PULL); // Pull only
        assertEquals(tokenResponse, TokenCache.get(pullOnlyScopes), "Should retrieve the token using pull-only scopes");
    }

    @Test
    void shouldRetrieveTokenPullTokenUsingPushPullDeleteScope() throws InterruptedException {
        HttpClient.TokenResponse tokenResponse =
                new HttpClient.TokenResponse("other-token", null, "dockerhub", 3600, null);
        ContainerRef containerRef = ContainerRef.parse("docker.io/library/alpine2:latest");
        Scopes scopes = Scopes.of(containerRef, Scope.PULL, Scope.PUSH, Scope.DELETE); // Pull push, delete
        TokenCache.put(scopes, tokenResponse);
        Scopes pullOnlyScopes = Scopes.of(containerRef, Scope.PULL); // Pull only
        assertEquals(tokenResponse, TokenCache.get(pullOnlyScopes), "Should retrieve the token using pull-only scopes");
    }

    @Test
    void shouldRetrieveTokenPullTokenUsingAllScope() throws InterruptedException {
        HttpClient.TokenResponse tokenResponse =
                new HttpClient.TokenResponse("other-token", null, "dockerhub", 3600, null);
        ContainerRef containerRef = ContainerRef.parse("docker.io/library/alpine3:latest");
        Scopes scopes = Scopes.of(containerRef, Scope.ALL); // Pull push, delete
        TokenCache.put(scopes, tokenResponse);
        Scopes pullOnlyScopes = Scopes.of(containerRef, Scope.PULL); // Pull only
        assertEquals(tokenResponse, TokenCache.get(pullOnlyScopes), "Should retrieve the token using pull-only scopes");
    }
}
