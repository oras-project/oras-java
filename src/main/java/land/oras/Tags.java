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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The tags response object
 */
@NullMarked
@OrasModel
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Tags {

    private final String name;
    private final List<String> tags;
    private final @Nullable String last;
    private final @Nullable Integer n;

    /**
     * Constructor with all fields
     * @param name The name
     * @param tags The tags
     * @param last The last tag index, to iterate
     * @param n The n parameter, to iterate
     */
    @JsonCreator
    public Tags(
            @JsonProperty("name") String name,
            @JsonProperty("tags") List<String> tags,
            @JsonProperty("last") @Nullable String last,
            @JsonProperty("n") @Nullable Integer n) {
        this.name = name;
        this.tags = tags;
        this.last = last;
        this.n = n;
    }

    /**
     * Constructor without last
     * @param name The name
     * @param tags The tags
     */
    public Tags(String name, List<String> tags) {
        this(name, tags, null, null);
    }

    /**
     * Get the name
     * @return The name
     */
    @JsonProperty("name")
    public String name() {
        return name;
    }

    /**
     * Get the tags
     * @return The tags
     */
    @JsonProperty("tags")
    public List<String> tags() {
        return tags;
    }

    /**
     * Get the last tag index
     * @return The last tag index
     */
    @JsonProperty("last")
    public @Nullable String last() {
        return last;
    }

    /**
     * Get the n parameter
     * @return The n parameter
     */
    @JsonProperty("n")
    public @Nullable Integer n() {
        return n;
    }

    /**
     * With last
     * @param last The last tag index, to iterate
     * @return A new Tags object with the last index
     */
    public Tags withLast(@Nullable String last) {
        return new Tags(this.name, this.tags, last, n);
    }

    /**
     * With n param
     * @param n The n param
     * @return A new Tags object with the n param
     */
    public Tags withN(@Nullable Integer n) {
        return new Tags(this.name, this.tags, last, n);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tags)) return false;
        Tags that = (Tags) o;
        return Objects.equals(name, that.name)
                && Objects.equals(tags, that.tags)
                && Objects.equals(last, that.last)
                && Objects.equals(n, that.n);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tags, last, n);
    }

    @Override
    public String toString() {
        return "Tags[name=" + name + ", tags=" + tags + ", last=" + last + ", n=" + n + "]";
    }
}
