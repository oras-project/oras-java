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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import land.oras.auth.HttpClient;
import land.oras.exception.Error;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class ClassAnnotationsTest {

    @Test
    void shouldHaveAnnotationOnModel() throws Exception {
        Set<Class<?>> modelClasses = findAnnotatedClasses("land.oras");

        // Check number of classes
        assertEquals(43, modelClasses.size());

        // Check classes
        assertTrue(modelClasses.contains(Annotations.class));
        assertTrue(modelClasses.contains(ArtifactType.class));
        assertTrue(modelClasses.contains(Config.class));
        assertTrue(modelClasses.contains(CopyUtils.CopyOptions.class));
        assertTrue(modelClasses.contains(Descriptor.class));
        assertTrue(modelClasses.contains(Describable.class));
        assertTrue(modelClasses.contains(Error.class));
        assertTrue(modelClasses.contains(HttpClient.TokenResponse.class));
        assertTrue(modelClasses.contains(Index.class));
        assertTrue(modelClasses.contains(Layer.class));
        assertTrue(modelClasses.contains(Manifest.class));
        assertTrue(modelClasses.contains(ManifestDescriptor.class));
        assertTrue(modelClasses.contains(OCILayout.class));
        assertTrue(modelClasses.contains(OCI.PullOptions.class));
        assertTrue(modelClasses.contains(OCI.PushOptions.class));
        assertTrue(modelClasses.contains(Repositories.class));
        assertTrue(modelClasses.contains(Subject.class));
        assertTrue(modelClasses.contains(Tags.class));
    }

    @Test
    void shouldHaveAnnotationOnAuthPackage() throws Exception {
        Set<Class<?>> modelClasses = findAnnotatedClasses("land.oras.auth");

        // Check number of classes
        assertEquals(10, modelClasses.size());
    }

    @Test
    void shouldHaveAnnotationOnPolicyPackage() throws Exception {
        Set<Class<?>> modelClasses = findAnnotatedClasses("land.oras.policy");

        // Check number of classes
        assertEquals(11, modelClasses.size());
    }

    /**
     * Find all classes annotated with {@link OrasModel} in the given package and its sub-packages
     * @param packageName The package name
     * @return The annotated classes
     */
    private static Set<Class<?>> findAnnotatedClasses(String packageName) throws IOException, URISyntaxException {
        Path root = Path.of(OrasModel.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
        Path packageDir = root.resolve(packageName.replace('.', '/'));
        ClassLoader classLoader = ClassAnnotationsTest.class.getClassLoader();
        try (Stream<Path> paths = Files.walk(packageDir)) {
            return paths.filter(path -> path.toString().endsWith(".class"))
                    .map(path -> root.relativize(path).toString())
                    .map(name -> name.substring(0, name.length() - ".class".length())
                            .replace('/', '.')
                            .replace('\\', '.'))
                    .map(name -> loadClass(name, classLoader))
                    .filter(clazz -> clazz.isAnnotationPresent(OrasModel.class))
                    .collect(Collectors.toSet());
        }
    }

    private static Class<?> loadClass(String name, ClassLoader classLoader) {
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}
