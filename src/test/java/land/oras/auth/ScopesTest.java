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

import static org.junit.jupiter.api.Assertions.*;

import land.oras.ContainerRef;
import land.oras.Registry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Test for {@link Scopes}.
 */
@Execution(ExecutionMode.CONCURRENT)
class ScopesTest {

    @Test
    void shouldBuildScopes() {
        ContainerRef containerRef = ContainerRef.parse("localhost:5000/library/test:latest");
        Registry registry = Registry.builder().build();
        Scopes scopes = Scopes.of(registry, containerRef, Scope.PULL);
        assertEquals(1, scopes.getScopes().size());
        assertEquals("repository:library/test:pull", scopes.getScopes().get(0));
        assertSame(containerRef, scopes.getContainerRef());
        assertEquals("localhost:5000", scopes.getRegistry());

        Scopes newScopes = scopes.withRegistryScopes(Scope.PUSH);
        assertNotSame(scopes, newScopes, "Scopes should be immutable");
        assertEquals(1, newScopes.getScopes().size());
        assertEquals("repository:library/test:push", newScopes.getScopes().get(0));
        assertSame(containerRef, newScopes.getContainerRef());
        assertEquals("localhost:5000", newScopes.getRegistry());

        Scopes newScopes2 = newScopes.withNewRegistryScopes(Scope.PULL);
        assertNotSame(newScopes, newScopes2, "Scopes should be immutable");
        assertEquals(1, newScopes2.getScopes().size());
        assertEquals("repository:library/test:pull,push", newScopes2.getScopes().get(0));
    }
}
