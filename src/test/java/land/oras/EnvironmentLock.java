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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLocksProvider;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Serializes access to the process environment (and the system properties modified alongside it) between
 * concurrently running tests.
 *
 * <ul>
 *   <li>Test methods that modify the environment (e.g. {@link TestUtils#withHome}) must be annotated with
 *   {@code @ResourceLock(EnvironmentLock.SYSTEM_ENVIRONMENT)} to take the exclusive lock.</li>
 *   <li>Test classes that read it (e.g. {@code Registry.builder()} reads {@code HOME} to locate
 *   {@code registries.conf}) must be annotated with {@code @ResourceLock(providers = EnvironmentLock.class)}
 *   which takes a shared READ lock on every other test method.</li>
 * </ul>
 *
 * <p>A class-level READ lock is not used on purpose: combined with a method-level READ_WRITE lock, JUnit would
 * escalate the whole class to READ_WRITE and run all its tests sequentially.
 */
public final class EnvironmentLock implements ResourceLocksProvider {

    /**
     * Resource lock key for the process environment. JUnit does not provide one in {@code Resources}.
     */
    public static final String SYSTEM_ENVIRONMENT = "java.lang.System.getenv";

    @Override
    public Set<Lock> provideForMethod(List<Class<?>> enclosingInstanceTypes, Class<?> testClass, Method testMethod) {
        boolean writer = AnnotationSupport.findRepeatableAnnotations(testMethod, ResourceLock.class).stream()
                .anyMatch(lock -> lock.value().equals(SYSTEM_ENVIRONMENT));
        if (writer) {
            return Set.of();
        }
        return Set.of(new Lock(SYSTEM_ENVIRONMENT, ResourceAccessMode.READ));
    }
}
