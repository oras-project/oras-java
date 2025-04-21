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

import java.util.LinkedList;
import java.util.List;
import land.oras.ContainerRef;
import land.oras.Registry;
import org.jspecify.annotations.NullMarked;

/**
 * Store scopes
 */
@NullMarked
public final class Scopes {

    /**
     * Constructor
     */
    private final List<String> scopes;

    private final Registry registry;
    private final ContainerRef containerRef;

    /**
     * Private constructor
     * @param registry The registry
     * @param containerRef The container reference
     * @param scopes The scopes
     */
    private Scopes(Registry registry, ContainerRef containerRef, Scope... scopes) {
        this(registry, containerRef, ScopeUtils.appendRepositoryScope(List.of(), containerRef, scopes));
    }

    /**
     * Private constructor
     * @param registry The registry
     * @param containerRef The container reference
     * @param scopes The scopes
     */
    private Scopes(Registry registry, ContainerRef containerRef, List<String> scopes) {
        this.registry = registry;
        this.containerRef = containerRef;
        this.scopes = scopes;
    }

    /**
     * Create a new Scopes object
     * @param registry The registry
     * @param containerRef The container reference
     * @param scopes The scopes
     * @return A new Scopes object
     */
    public static Scopes of(Registry registry, ContainerRef containerRef, Scope... scopes) {
        return new Scopes(registry, containerRef, scopes);
    }

    /**
     * Create a new Scopes object with no scopes
     * @param registry The registry
     * @param containerRef The container reference
     * @return A new Scopes object with no scopes
     */
    public static Scopes empty(Registry registry, ContainerRef containerRef) {
        return new Scopes(registry, containerRef, List.of());
    }

    /**
     * Return a new copy of the Scopes object with the given scopes
     * @param scopes The scopes to set
     * @return A new Scopes object with the given scopes
     */
    public Scopes withRegistryScopes(Scope... scopes) {
        return new Scopes(registry, containerRef, scopes);
    }

    /**
     * Return a new copy of the Scopes object with the given scopes
     * @param newScopes The scopes to add
     * @return A new Scopes object with the given scopes
     */
    public Scopes withNewRegistryScopes(Scope... newScopes) {
        return new Scopes(
                registry, containerRef, ScopeUtils.appendRepositoryScope(this.scopes, containerRef, newScopes));
    }

    /**
     * Return a new copy of the Scopes object with the given scope
     * @param scope The scope to add
     * @return A new Scopes object with the given scope
     */
    public Scopes withNewScope(String scope) {
        List<String> newScopes = new LinkedList<>(scopes);
        newScopes.add(scope);
        return new Scopes(registry, containerRef, ScopeUtils.cleanScopes(newScopes));
    }

    /**
     * Get the scopes
     * @return The scopes
     */
    public List<String> getScopes() {
        return scopes;
    }

    /**
     * Get the registry
     * @return The registry
     */
    public String getRegistry() {
        return containerRef.forRegistry(registry).getRegistry();
    }

    /**
     * Get the container reference
     * @return The container reference
     */
    public ContainerRef getContainerRef() {
        return containerRef;
    }
}
