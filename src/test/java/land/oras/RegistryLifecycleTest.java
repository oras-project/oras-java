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

package land.oras;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

@ResourceLock(providers = EnvironmentLock.class)
class RegistryLifecycleTest {

    @Test
    void shouldShutDownDefaultExecutorOnClose() throws Exception {
        ExecutorService executor;
        try (Registry registry = Registry.builder().defaults().build()) {
            executor = registry.getExecutorService();
            assertFalse(executor.isShutdown());
            assertTrue(executor.submit(() -> Thread.currentThread().isDaemon()).get(5, TimeUnit.SECONDS));
        }
        assertTrue(executor.isShutdown());
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void shouldAllowRepeatedClose() {
        Registry registry = Registry.builder().build();
        registry.close();
        registry.close();
        assertTrue(registry.getExecutorService().isShutdown());
    }

    @Test
    void shouldNotShutDownCallerSuppliedExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            try (Registry registry =
                    Registry.builder().withExecutorService(executor).build()) {
                assertSame(executor, registry.getExecutorService());
            }
            assertFalse(executor.isShutdown());
            assertEquals(42, executor.submit(() -> 42).get(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldKeepSharedExecutorOwnedByOriginalRegistry() throws Exception {
        ExecutorService executor;
        try (Registry original = Registry.builder().build()) {
            executor = original.getExecutorService();
            for (Registry derived : List.of(
                    original.copy("registry.example"),
                    original.withAuthToken("test-token"),
                    original.asInsecure(),
                    original.asSecure(),
                    Registry.builder().from(original).build())) {
                try (derived) {
                    assertSame(executor, derived.getExecutorService());
                }
                assertFalse(executor.isShutdown());
            }
            assertEquals(42, executor.submit(() -> 42).get(5, TimeUnit.SECONDS));
        }
        assertTrue(executor.isShutdown());
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
}
