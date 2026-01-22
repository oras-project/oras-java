/*
 * Copyright The ORAS Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package land.oras;

/**
 * Options for copy operations.
 *
 * @param recursive true to recursively copy artifacts
 */
public record CopyOptions(boolean recursive) {

    /**
     * Creates a new builder for CopyOptions.
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for CopyOptions.
     */
    public static class Builder {
        private boolean recursive;

        /**
         * Sets whether to recursively copy artifacts.
         * @param recursive true to recursively copy
         * @return this builder
         */
        public Builder recursive(boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        /**
         * Builds the CopyOptions.
         * @return the CopyOptions
         */
        public CopyOptions build() {
            return new CopyOptions(recursive);
        }
    }
}