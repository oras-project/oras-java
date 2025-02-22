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

import land.oras.ContainerRef;
import land.oras.credentials.FileStore;
import land.oras.credentials.FileStore.Credential;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * FileStoreAuthenticationProvider is an implementation of the AuthProvider interface.
 * It retrieves credentials from a FileStore and generates a Basic Authentication header.
 */
@NullMarked
public final class FileStoreAuthenticationProvider implements AuthProvider {

    private final FileStore fileStore;

    /**
     * Default constructor
     */
    public FileStoreAuthenticationProvider() {
        this(FileStore.newFileStore());
    }

    /**
     * Constructor for FileStoreAuthenticationProvider.
     *
     * @param fileStore The FileStore instance to retrieve credentials from.
     */
    public FileStoreAuthenticationProvider(FileStore fileStore) {
        this.fileStore = fileStore;
    }

    @Override
    @Nullable
    public String getAuthHeader(ContainerRef registry) {
        Credential credential = fileStore.get(registry);
        if (credential == null) {
            return null;
        }
        return new UsernamePasswordProvider(credential.username(), credential.password()).getAuthHeader(registry);
    }
}
