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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;

/**
 * The repositories response object
 */
@NullMarked
@OrasModel
public final class Repositories {

    private final List<String> repositories;

    /**
     * Create a new repositories instance
     * @param repositories The repositories
     */
    @JsonCreator
    public Repositories(@JsonProperty("repositories") List<String> repositories) {
        this.repositories = repositories;
    }

    /**
     * Get the repositories
     * @return The repositories
     */
    @JsonProperty("repositories")
    public List<String> repositories() {
        return repositories;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Repositories)) return false;
        Repositories that = (Repositories) o;
        return Objects.equals(repositories, that.repositories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repositories);
    }

    @Override
    public String toString() {
        return "Repositories[repositories=" + repositories + "]";
    }
}
