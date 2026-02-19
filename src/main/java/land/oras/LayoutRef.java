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

import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;
import land.oras.exception.OrasException;
import land.oras.utils.SupportedAlgorithm;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A referer of a container on a {@link OCILayout}.
 */
@NullMarked
@OrasModel
public final class LayoutRef extends Ref<LayoutRef> {

    private final Path folder;

    private static final Pattern NAME_REGEX = Pattern.compile(
            "^(.+?)(?::([^:@]+))?(?:@(.+))?$" // folder[:tag][@digest]
            );

    /**
     * Private constructor
     * @param tag The tag.
     */
    private LayoutRef(Path folder, @Nullable String tag) {
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

    /**
     * Return a new layout reference for a path and digest or tag
     * @param layout The OCI layout
     * @param digest The digest
     * @return The layout ref
     */
    public static LayoutRef of(OCILayout layout, String digest) {
        return new LayoutRef(layout.getPath(), digest);
    }

    /**
     * Return a new layout reference for a path and digest or tag
     * @param layout The OCI layout
     * @return The layout ref
     */
    public static LayoutRef of(OCILayout layout) {
        return new LayoutRef(layout.getPath(), null);
    }

    /**
     * Return a new layout reference for a path
     * @param path The path
     * @return The layout ref
     */
    public LayoutRef forPath(Path path) {
        return new LayoutRef(path, tag);
    }

    /**
     * Return a new layout reference for a path
     * @param ociLayout The OCI layout
     * @return The layout ref
     */
    public LayoutRef forLayout(OCILayout ociLayout) {
        return forPath(ociLayout.getPath());
    }

    @Override
    public SupportedAlgorithm getAlgorithm() {
        // Default if not set
        if (tag == null) {
            return SupportedAlgorithm.getDefault();
        }
        // See https://github.com/opencontainers/image-spec/blob/main/descriptor.md#digests
        else if (SupportedAlgorithm.isSupported(tag)) {
            return SupportedAlgorithm.fromDigest(tag);
        }

        return SupportedAlgorithm.getDefault();
    }

    /**
     * Return if the current layout tag is a valid digest.
     * @return True if the tag is a valid digest.
     */
    public boolean isValidDigest() {
        if (tag == null) {
            return false;
        }
        return SupportedAlgorithm.isSupported(tag);
    }

    @Override
    public String getRepository() {
        return getFolder().toString();
    }

    @Override
    public LayoutRef forTargetRegistry(String target) {
        return new LayoutRef(Path.of(target), tag);
    }

    @Override
    public LayoutRef forTargetRepository(String targetRegistry) {
        return new LayoutRef(folder, tag);
    }

    @Override
    public String getTargetRegistry(OCI<LayoutRef> target) {
        return folder.toString();
    }

    @Override
    public String getTargetRepository(OCI<LayoutRef> target) {
        return getRepository();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LayoutRef layoutRef = (LayoutRef) o;
        return Objects.equals(getFolder(), layoutRef.getFolder()) && Objects.equals(tag, layoutRef.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFolder(), tag);
    }

    @Override
    public String toString() {
        if (tag != null) {
            if (isValidDigest()) {
                return "%s@%s".formatted(getFolder().toString(), tag);
            }
            return "%s:%s".formatted(getFolder().toString(), tag);
        } else {
            return getFolder().toString();
        }
    }
}
