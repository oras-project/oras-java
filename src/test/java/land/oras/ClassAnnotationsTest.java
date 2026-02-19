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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.util.HashSet;
import java.util.Set;
import land.oras.auth.HttpClient;
import land.oras.exception.Error;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class ClassAnnotationsTest {

    @Test
    void shouldHaveAnnotationOnModel() {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages("land.oras")
                .scan()) {
            Set<Class<?>> modelClasses = new HashSet<>(scanResult
                    .getClassesWithAnnotation(OrasModel.class.getName())
                    .loadClasses());

            // Check number of classes
            assertEquals(20, modelClasses.size());

            // Check classes
            assertTrue(modelClasses.contains(Annotations.class));
            assertTrue(modelClasses.contains(ArtifactType.class));
            assertTrue(modelClasses.contains(Config.class));
            assertTrue(modelClasses.contains(Descriptor.class));
            assertTrue(modelClasses.contains(Describable.class));
            assertTrue(modelClasses.contains(Error.class));
            assertTrue(modelClasses.contains(HttpClient.TokenResponse.class));
            assertTrue(modelClasses.contains(Index.class));
            assertTrue(modelClasses.contains(Layer.class));
            assertTrue(modelClasses.contains(Manifest.class));
            assertTrue(modelClasses.contains(ManifestDescriptor.class));
            assertTrue(modelClasses.contains(OCILayout.class));
            assertTrue(modelClasses.contains(Repositories.class));
            assertTrue(modelClasses.contains(Subject.class));
            assertTrue(modelClasses.contains(Tags.class));
        }
    }

    @Test
    void shouldHaveAnnotationOnAuthPackage() {
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .acceptPackages("land.oras.auth")
                .scan()) {
            Set<Class<?>> modelClasses = new HashSet<>(scanResult
                    .getClassesWithAnnotation(OrasModel.class.getName())
                    .loadClasses());

            // Check number of classes
            assertEquals(2, modelClasses.size());
        }
    }
}
