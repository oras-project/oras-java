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

import java.util.List;
import land.oras.ContainerRef;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class ScopeUtilsTest {

    @Test
    void shouldAppendRepositoryScope() {
        List<String> existingScopes = List.of("repository:test:pull");
        ContainerRef ref = ContainerRef.parse("foo/bar:tag");

        List<String> scopes = ScopeUtils.appendRepositoryScope(existingScopes, ref, Scope.PULL);
        scopes = ScopeUtils.appendRepositoryScope(scopes, ref, Scope.PULL);
        assertEquals(List.of("repository:foo/bar:pull", "repository:test:pull"), scopes);

        // Null scopes
        scopes = ScopeUtils.appendRepositoryScope(null, ref, Scope.PULL);
        assertEquals(List.of("repository:foo/bar:pull"), scopes);

        // Empty scopes
        scopes = ScopeUtils.appendRepositoryScope(List.of(), ref);
        assertEquals(List.of(), scopes);
    }

    @Test
    void shouldScopeRepository() {

        // Pull
        ContainerRef ref = ContainerRef.parse("foo/bar:tag");
        String scope = ScopeUtils.scopeRepository(ref, Scope.PULL);
        assertEquals("repository:foo/bar:pull", scope);

        // Push
        scope = ScopeUtils.scopeRepository(ref, Scope.PUSH);
        assertEquals("repository:foo/bar:push", scope);

        // Delete
        scope = ScopeUtils.scopeRepository(ref, Scope.DELETE);
        assertEquals("repository:foo/bar:delete", scope);

        // Multiple scopes
        scope = ScopeUtils.scopeRepository(ref, Scope.PULL, Scope.PUSH);
        assertEquals("repository:foo/bar:pull,push", scope);

        // Not scopes
        scope = ScopeUtils.scopeRepository(ref);
        assertEquals("", scope);
    }

    @Test
    void shouldCleanActions() {

        // Just sorting
        List<String> actions = ScopeUtils.cleanActions(List.of("push", "pull"));
        assertEquals(List.of("pull", "push"), actions);

        // Removing duplicates
        actions = ScopeUtils.cleanActions(List.of("push", "push", "pull"));
        assertEquals(List.of("pull", "push"), actions);

        // Removing duplicates and sorting
        actions = ScopeUtils.cleanActions(List.of("push", "pull", "push"));
        assertEquals(List.of("pull", "push"), actions);

        // Using wildcard
        actions = ScopeUtils.cleanActions(List.of("push", "*", "pull"));
        assertEquals(List.of("*"), actions);

        // Using wildcard and removing duplicates
        actions = ScopeUtils.cleanActions(List.of("push", "*", "push"));
        assertEquals(List.of("*"), actions);

        // Null values
        actions = ScopeUtils.cleanActions(null);
        assertEquals(List.of(), actions);

        // Empty list
        actions = ScopeUtils.cleanActions(List.of());
        assertEquals(List.of(), actions);

        // Empty string
        actions = ScopeUtils.cleanActions(List.of(""));
        assertEquals(List.of(), actions);

        // Empty string and other values
        actions = ScopeUtils.cleanActions(List.of("", "push", "pull"));
        assertEquals(List.of("pull", "push"), actions);

        // Empty string and wildcard
        actions = ScopeUtils.cleanActions(List.of("", "*", "push"));
        assertEquals(List.of("*"), actions);
    }

    @Test
    void shouldCleanScopes() {
        // Sort repository scopes
        List<String> scopes = ScopeUtils.cleanScopes(List.of("repository:foo:push", "repository:bar:pull"));
        assertEquals(List.of("repository:bar:pull", "repository:foo:push"), scopes);

        // Sort action
        scopes = ScopeUtils.cleanScopes(List.of("repository:foo:push,pull", "repository:bar:pull"));
        assertEquals(List.of("repository:bar:pull", "repository:foo:pull,push"), scopes);

        // Single scope
        scopes = ScopeUtils.cleanScopes(List.of("repository:foo:push"));
        assertEquals(List.of("repository:foo:push"), scopes);

        // Single scope, sort action
        scopes = ScopeUtils.cleanScopes(List.of("repository:foo:push,pull"));
        assertEquals(List.of("repository:foo:pull,push"), scopes);

        // Single scope wildcard
        scopes = ScopeUtils.cleanScopes(List.of("repository:foo:*,push"));
        assertEquals(List.of("repository:foo:*"), scopes);

        // All repository scopes
        scopes = ScopeUtils.cleanScopes(List.of("repository:*:*", "repository:*:push"));
        assertEquals(List.of("repository:*:*"), scopes);

        // All scopes
        scopes = ScopeUtils.cleanScopes(List.of("repository:test:*", "repository:test:push"));
        assertEquals(List.of("repository:test:*"), scopes);

        // Null scope
        scopes = ScopeUtils.cleanScopes(null);
        assertEquals(List.of(), scopes);

        // Empty scope
        scopes = ScopeUtils.cleanScopes(List.of());
        assertEquals(List.of(), scopes);

        // Registry
        scopes = ScopeUtils.cleanScopes(List.of("repository:foo:push,pull", "registry:bar:pull"));
        assertEquals(List.of("registry:bar:pull", "repository:foo:pull,push"), scopes);
    }
}
