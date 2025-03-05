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

package land.oras.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import land.oras.exception.OrasException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
public class SupportedAlgorithmTest {

    @Test
    void shouldThrowIfInvalidDigest() {
        assertThrows(OrasException.class, () -> SupportedAlgorithm.fromDigest("invalid"));
    }

    @Test
    void shouldPreventDuplicatePrefix() {
        assertThrows(
                OrasException.class,
                () -> SupportedAlgorithm.fromDigest(
                        "sha256:sha256:245d81d351d8d3b00ae1880ac480c593abd357d5bae561052ae23cbffbecbfe8"));
    }

    @Test
    void shouldWorkWithRegisteredDigest() {
        assertEquals(
                SupportedAlgorithm.SHA256,
                SupportedAlgorithm.fromDigest(
                        "sha256:245d81d351d8d3b00ae1880ac480c593abd357d5bae561052ae23cbffbecbfe8"));
        assertEquals(
                SupportedAlgorithm.SHA512,
                SupportedAlgorithm.fromDigest(
                        "sha512:cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"));
    }
}
