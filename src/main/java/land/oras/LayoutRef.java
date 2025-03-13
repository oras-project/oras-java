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

package land.oras;

import java.nio.file.Path;
import java.util.regex.Pattern;
import land.oras.exception.OrasException;
import land.oras.utils.SupportedAlgorithm;
import org.jspecify.annotations.NullMarked;

/**
 * A referer of a container on a {@link OCILayout}.
 */
@NullMarked
public final class LayoutRef extends Ref {

    private final Path folder;

    private static final Pattern NAME_REGEX = Pattern.compile(
            "^(.+?)(?::([^:@]+))?(?:@(.+))?$" // folder[:tag][@digest]
            );

    /**
     * Private constructor
     * @param tag The tag.
     */
    private LayoutRef(Path folder, String tag) {
        super(tag);
        this.folder = folder;
    }

    /**
     * Get the folder
     * @return The folder
     */
    public Path getFolder() {
        return folder;
    }

    /**
     * Return a new layout ref with the tag.
     * @param tag The tag.
     * @return The new layout ref.
     */
    public LayoutRef withTag(String tag) {
        return new LayoutRef(folder, tag);
    }

    @Override
    public LayoutRef withDigest(String digest) {
        return withTag(digest);
    }

    /**
     * Parse the layout ref with folder and tag.
     * @param name The layout ref.
     * @return The container object with the registry, repository and tag.
     */
    public static LayoutRef parse(String name) {
        var matcher = NAME_REGEX.matcher(name);
        if (!matcher.matches()) {
            throw new OrasException("Invalid layout ref: " + name);
        }
        Path path = Path.of(matcher.group(1)); // Folder path
        String tag = matcher.group(2) != null ? matcher.group(2) : matcher.group(3); // Tag or digest
        return new LayoutRef(path, tag);
    }

    @Override
    public SupportedAlgorithm getAlgorithm() {
        // Default if not set
        if (tag == null) {
            return SupportedAlgorithm.getDefault();
        }
        // See https://github.com/opencontainers/image-spec/blob/main/descriptor.md#digests
        else if (SupportedAlgorithm.matchPattern(tag)) {
            return SupportedAlgorithm.fromDigest(tag);
        }

        return SupportedAlgorithm.getDefault();
    }

    @Override
    public String getRepository() {
        return getFolder().toString();
    }
}
