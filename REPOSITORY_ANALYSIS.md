# ORAS Java SDK - Comprehensive Repository Analysis

**Repository**: https://github.com/oras-project/oras-java  
**Version**: 0.5.1-SNAPSHOT  
**License**: Apache 2.0  
**Build Tool**: Maven 3.9.13  
**Java Version**: 17+  

---

## 1. Directory Structure Overview

```
/oras-java
├── src/
│   ├── main/java/land/oras/           [46 Java source files]
│   │   ├── Registry.java              [1400+ lines - Remote registry]
│   │   ├── OCILayout.java             [500+ lines - Local OCI Layout]
│   │   ├── OCI.java                   [501 lines - Abstract base]
│   │   ├── CopyUtils.java             [278 lines - Copy utilities]
│   │   ├── Layer.java                 [Blob layer representation]
│   │   ├── Manifest.java              [OCI image manifest]
│   │   ├── Index.java                 [OCI image index]
│   │   ├── Config.java                [Image config blob]
│   │   ├── ContainerRef.java          [Registry reference type]
│   │   ├── LayoutRef.java             [Layout reference type]
│   │   ├── Ref.java                   [Abstract reference base]
│   │   ├── auth/                      [Authentication providers]
│   │   ├── exception/                 [Exception classes]
│   │   └── utils/                     [Utilities & constants]
│   ├── test/java/land/oras/           [39 test files]
│   │   ├── RegistryTest.java          [56 test methods]
│   │   ├── OCILayoutTest.java         [Multiple tests]
│   │   └── (Integration & unit tests)
│   └── test/resources/                [Test data: OCI, archives, etc.]
├── pom.xml                            [Maven configuration]
└── README.md, LICENSE, etc.
```

---

## 2. Core Architecture

### Sealed Class Hierarchy

The SDK uses Java's sealed classes for type safety and polymorphism control:

```
OCI<T extends Ref> [abstract sealed]
├── Registry extends OCI<ContainerRef>
└── OCILayout extends OCI<LayoutRef>
```

```
Ref<T extends Ref> [abstract sealed]
├── ContainerRef (registry references)
└── LayoutRef (layout references)
```

### Key Design Patterns

- **Sealed Classes**: Restricts inheritance to known implementations (Registry, OCILayout)
- **Generics**: `OCI<T>` parameterized by reference type
- **Records**: `CopyOptions` and configuration objects
- **Parallel Execution**: `CompletableFuture` for concurrent blob operations
- **Builder Pattern**: Registry and OCILayout builders for configuration

---

## 3. Blob Upload/Push Methods

### Registry Class (Remote HTTP Registry)

#### Method 1: `pushBlob(ContainerRef ref, Path blob, Map<String, String> annotations)`

**Line**: 491  
**Purpose**: Push a blob from a file path  
**Workflow**:
1. Calculate SHA256/SHA512 digest from file
2. Check if blob already exists (HEAD request)
3. Attempt single POST: `POST /v2/{repo}/blobs/uploads/?digest={digest}`
4. If 202 Accepted (not immediate creation):
   - Extract Location header
   - Perform chunked upload: `PUT {location}?digest={digest}`
5. Return Layer object with file metadata and annotations

**Signature**:
```java
@Override
public Layer pushBlob(ContainerRef containerRef, Path blob, Map<String, String> annotations)
```

**Returns**: `Layer` with digest, size, and annotations

---

#### Method 2: `pushBlob(ContainerRef ref, long size, Supplier<InputStream> stream, Map<String, String> annotations)`

**Line**: 550  
**Purpose**: Push a blob from an InputStream with known digest and size  
**Requirements**: 
- Digest MUST be pre-set in the ref (e.g., `ref.withDigest("sha256:xxx")`)
- Size must be known beforehand

**Workflow**:
1. Validate digest is present (throws OrasException if not)
2. Check if blob exists (HEAD request)
3. Initiate upload: `POST /v2/{repo}/blobs/uploads/` (empty body, no digest)
   - Response: 202 Accepted with Location header
4. Parse location, append digest: `{location}?digest={digest}`
5. Complete upload: `PUT {location}?digest={digest}` with stream data
6. Validate response (201 Created = success)
7. Return Layer with digest and size

**Signature**:
```java
@Override
public Layer pushBlob(ContainerRef ref, long size, Supplier<InputStream> stream, 
                      Map<String, String> annotations)
```

**Returns**: `Layer` with digest, size, and annotations

---

#### Method 3: `pushBlob(ContainerRef ref, byte[] data)`

**Line**: 602  
**Purpose**: Push a blob from a byte array  
**Workflow**:
1. Calculate digest from data
2. Check if blob already exists (HEAD request)
3. Single POST: `POST /v2/{repo}/blobs/uploads/?digest={digest}` with full data
4. Return Layer with data metadata

**Signature**:
```java
@Override
public Layer pushBlob(ContainerRef containerRef, byte[] data)
```

**Returns**: `Layer` with digest and data

---

#### Method 4: `deleteBlob(ContainerRef ref)` [Public]

**Line**: 410  
**Purpose**: Delete a blob from the registry

**Workflow**:
- `DELETE /v2/{repo}/blobs/{digest}`

**Signature**:
```java
public void deleteBlob(ContainerRef containerRef)
```

---

#### Method 5: `hasBlob(ContainerRef ref)` [Private Helper]

**Line**: 664  
**Purpose**: Check if a blob exists in the registry

**Workflow**:
- `HEAD /v2/{repo}/blobs/{digest}` → returns `statusCode == 200`

**Signature**:
```java
private boolean hasBlob(ContainerRef containerRef)
```

**Returns**: `boolean` (true if 200 OK, false otherwise)

---

### OCILayout Class (Local Filesystem OCI Layout)

#### Method 1: `pushBlob(LayoutRef ref, Path blob, Map<String, String> annotations)`

**Line**: 275  
**Purpose**: Push a blob from a file to local OCI layout  
**Workflow**:
1. Ensure algorithm directory exists: `blobs/{algorithm}/`
2. Calculate digest from file
3. Copy file to: `blobs/{algorithm}/{digest_hex}`
4. Return Layer with file metadata

**Signature**:
```java
@Override
public Layer pushBlob(LayoutRef ref, Path blob, Map<String, String> annotations)
```

**Returns**: `Layer` with digest and size

---

#### Method 2: `pushBlob(LayoutRef ref, long size, Supplier<InputStream> stream, Map<String, String> annotations)`

**Line**: 294  
**Purpose**: Push a blob from stream to local OCI layout  
**Requirements**:
- Digest must be pre-set in ref (as tag/ref)
- Digest must be valid format (SHA256, SHA512, etc.)

**Workflow**:
1. Validate digest format using `SupportedAlgorithm.isSupported(digest)`
2. Ensure algorithm directory exists
3. `Files.copy(stream, blobs/{algorithm}/{digest})`
4. Validate copied file digest matches expected
5. Return Layer

**Signature**:
```java
@Override
public Layer pushBlob(LayoutRef ref, long size, Supplier<InputStream> stream, 
                      Map<String, String> annotations)
```

**Returns**: `Layer`

---

#### Method 3: `pushBlob(LayoutRef ref, byte[] data)`

**Line**: 321  
**Purpose**: Push a blob from byte array to local OCI layout  
**Workflow**:
1. Create temporary file
2. Write data to temp file
3. Validate digest
4. Delegate to `pushBlob(Path)` variant
5. Return Layer

**Signature**:
```java
@Override
public Layer pushBlob(LayoutRef ref, byte[] data)
```

**Returns**: `Layer`

---

### OCI Base Class - Helper Methods

```java
// Convenience overloads
public Layer pushBlob(T ref, Path blob)
public Layer pushBlob(T ref, InputStream input)

// Fetch methods (abstract)
public abstract void fetchBlob(T ref, Path path)
public abstract InputStream fetchBlob(T ref)
public abstract Descriptor fetchBlobDescriptor(T ref)
public abstract byte[] getBlob(T ref)
```

---

## 4. Copy Operation Implementation

### CopyUtils.copy() - Main Entry Point

**File**: CopyUtils.java  
**Lines**: 137-254

**Signature**:
```java
public static <SourceRefType extends Ref<@NonNull SourceRefType>, 
               TargetRefType extends Ref<@NonNull TargetRefType>>
  void copy(OCI<SourceRefType> source,
            SourceRefType sourceRef,
            OCI<TargetRefType> target,
            TargetRefType targetRef,
            CopyOptions options)
```

### CopyOptions Record

```java
public record CopyOptions(boolean includeReferrers) {
    public static CopyOptions shallow()  // includeReferrers = false
    public static CopyOptions deep()     // includeReferrers = true (recursive)
}
```

### Copy Workflow for Single Manifest

**Lines**: 165-201

1. **Collect & Copy Layers**:
   ```java
   copyLayers(source, effectiveSourceRef, target, effectiveTargetRef, contentType);
   ```
   - Fetches all layers for the manifest
   - Uses parallel `CompletableFuture.allOf()` execution
   - For each layer: `target.pushBlob(targetRef.withDigest(digest), size, () -> source.fetchBlob(...), annotations)`

2. **Copy Config**:
   ```java
   copyConfig(manifest, source, effectiveSourceRef, target, effectiveTargetRef);
   ```
   - Extracts config from manifest
   - Pulls config from source
   - Pushes config as blob to target

3. **Push Manifest**:
   ```java
   target.pushManifest(effectiveTargetRef.withDigest(tag), manifest);
   ```

4. **[Optional] Copy Referrers** (if `includeReferrers == true`):
   ```java
   Referrers referrers = source.getReferrers(effectiveSourceRef.withDigest(manifestDigest), null);
   for (ManifestDescriptor referer : referrers.getManifests()) {
       copy(source, effectiveSourceRef.withDigest(referer.getDigest()), 
            target, effectiveTargetRef, options);  // Recursive call
   }
   ```

### Copy Workflow for Index (Multi-platform)

**Lines**: 202-244

1. **Get Index**:
   ```java
   Index index = source.getIndex(effectiveSourceRef);
   ```

2. **For Each Manifest in Index**:
   - If manifest type: Copy layers, config, push manifest
   - If index type: Recursively call `copy()` for nested indices

3. **Copy All Layers** for each manifest:
   ```java
   copyLayers(source, effectiveSourceRef.withDigest(...), target, effectiveTargetRef, mediaType);
   ```

4. **Push Index**:
   ```java
   Index pushedIndex = target.pushIndex(effectiveTargetRef.withDigest(tag), index);
   ```

### copyLayers() Helper

**Lines**: 102-125

```java
private static void copyLayers(OCI<SourceRefType> source, SourceRefType sourceRef,
                                OCI<TargetRefType> target, TargetRefType targetRef,
                                String contentType)
```

- Collects all layers using `source.collectLayers(sourceRef, contentType, true)`
- Uses parallel execution: `CompletableFuture.allOf(...).stream().map(...).toArray()`
- For each layer, pushes blob:
  ```java
  target.pushBlob(
      targetRef.withDigest(layer.getDigest()),
      layer.getSize(),
      () -> source.fetchBlob(sourceRef.withDigest(layer.getDigest())),
      layer.getAnnotations()
  );
  ```

### copyConfig() Helper

**Lines**: 256-276

```java
private static void copyConfig(Manifest manifest,
                               OCI<SourceRefType> source, SourceRefType sourceRef,
                               OCI<TargetRefType> target, TargetRefType targetRef)
```

- Extracts config: `Config config = manifest.getConfig();`
- Validates digest and size are present
- Pushes config as blob:
  ```java
  target.pushBlob(
      targetRef.forTarget(target).withDigest(config.getDigest()),
      config.getSize(),
      () -> source.pullConfig(sourceRef, config),
      config.getAnnotations()
  );
  ```

---

## 5. Test Structure & Framework

### Build & Test Tools

| Component | Version |
|-----------|---------|
| Build Tool | Maven 3.9.13 |
| Test Framework | JUnit 5 (Jupiter) 6.0.3 |
| Container Support | TestContainers 2.0.3 + Docker |
| HTTP Mocking | WireMock 3.13.2 |
| Object Mocking | Mockito 5.22.0 |
| Java Target | 17 |

### Test Files Structure

```
src/test/java/land/oras/
├── RegistryTest.java                    [56 test methods]
├── OCILayoutTest.java                   [Multiple blob & manifest tests]
├── RegistryWireMockTest.java           [HTTP mocking tests]
├── GitHubContainerRegistryITCase.java  [Integration - GitHub]
├── OracleContainerRegistryITCase.java  [Integration - Oracle]
├── NexusITCase.java                    [Integration - Nexus]
├── PublicECRITCase.java                [Integration - AWS ECR]
├── FluxCDITCase.java                   [Integration - FluxCD]
└── auth/                                [Auth-related tests]
```

### Test Infrastructure

```java
@Testcontainers                           // JUnit 5 extension
@Execution(ExecutionMode.CONCURRENT)      // Parallel execution
@Container
private final ZotContainer registry = new ZotContainer().withStartupAttempts(3);

@TempDir                                  // Temporary directory injection
private Path blobDir;
```

### Test Patterns

#### Registry Blob Test Example (Line 254):

```java
@Test
void shouldPushAndGetBlobThenDeleteWithSha256() {
    Registry registry = Registry.Builder.builder()
        .defaults("myuser", "mypass")
        .withRegistry(this.registry.getRegistry())
        .withInsecure(true)
        .build();
    
    ContainerRef containerRef = ContainerRef.parse(
        "library/artifact-text@sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
    );
    
    // Test blob push
    Layer layer = registry.pushBlob(containerRef, "hello".getBytes());
    assertEquals("sha256:2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", 
                 layer.getDigest());
    
    // Test blob get
    byte[] blob = registry.getBlob(containerRef.withDigest("sha256:..."));
    assertEquals("hello", new String(blob));
    
    // Test idempotent push
    registry.pushBlob(containerRef, "hello".getBytes());
    
    // Test blob delete
    registry.deleteBlob(containerRef.withDigest("sha256:..."));
    
    // Verify deletion
    assertThrows(OrasException.class, () -> 
        registry.getBlob(containerRef.withDigest("sha256:..."))
    );
}
```

#### OCILayout Blob Test Example (Line 623):

```java
@Test
void shouldPushBlob() throws IOException {
    Path path = layoutPath.resolve("shouldPushBlob");
    byte[] content = "hi".getBytes(StandardCharsets.UTF_8);
    String digest = SupportedAlgorithm.SHA256.digest(content);
    
    OCILayout ociLayout = OCILayout.Builder.builder().defaults(path).build();
    LayoutRef layoutRef = LayoutRef.of(ociLayout, digest);
    
    // Push blob
    ociLayout.pushBlob(layoutRef, "hi".getBytes(StandardCharsets.UTF_8));
    
    // Assert file exists
    assertBlobExists(path, digest);
    assertBlobContent(path, digest, "hi");
    
    // Push again (idempotent)
    ociLayout.pushBlob(layoutRef, "hi".getBytes(StandardCharsets.UTF_8));
    
    assertBlobExists(path, digest);
    assertBlobContent(path, digest, "hi");
}
```

### Test Helpers

```java
assertBlobExists(ociLayoutPath, digest)              // Verify blob file exists
assertBlobAbsent(ociLayoutPath, digest)              // Verify blob doesn't exist
assertBlobContent(ociLayoutPath, digest, "content")  // Verify blob content matches
```

### Test Execution

```java
@Execution(ExecutionMode.CONCURRENT)     // Parallel test execution
@Execution(ExecutionMode.SAME_THREAD)    // Sequential (integration tests)
```

---

## 6. Interfaces & Abstract Classes

### OCI<T> Abstract Base Class

**File**: OCI.java (501 lines)

**Abstract Methods** (must be implemented by Registry and OCILayout):

**Artifact Operations**:
```java
abstract Manifest pushArtifact(T ref, ArtifactType, Annotations, Config, LocalPath...)
abstract void pullArtifact(T ref, Path path, boolean overwrite)
```

**Manifest Operations**:
```java
abstract Manifest pushManifest(T ref, Manifest manifest)
abstract Manifest getManifest(T ref)
```

**Index Operations**:
```java
abstract Index pushIndex(T ref, Index index)
abstract Index getIndex(T ref)
```

**Blob Operations**:
```java
abstract Layer pushBlob(T ref, Path blob, Map<String, String> annotations)
abstract Layer pushBlob(T ref, long size, Supplier<InputStream> stream, Map<String, String> annotations)
abstract Layer pushBlob(T ref, byte[] data)
abstract void fetchBlob(T ref, Path path)
abstract InputStream fetchBlob(T ref)
abstract Descriptor fetchBlobDescriptor(T ref)
abstract byte[] getBlob(T ref)
```

**Metadata Operations**:
```java
abstract Descriptor getDescriptor(T ref)
abstract Descriptor probeDescriptor(T ref)
abstract Tags getTags(T ref)
abstract Tags getTags(T ref, int n, @Nullable String last)
abstract Repositories getRepositories()
abstract Referrers getReferrers(T ref, @Nullable ArtifactType artifactType)
```

**Infrastructure**:
```java
abstract ExecutorService getExecutorService()
```

**Concrete Helper Methods**:
```java
public Manifest pushArtifact(T ref, LocalPath... paths)
public Manifest pushArtifact(T ref, ArtifactType, LocalPath... paths)
public Manifest pushArtifact(T ref, ArtifactType, Annotations, LocalPath... paths)
public Layer pushBlob(T ref, Path blob)                      // No annotations
public Layer pushBlob(T ref, InputStream input)              // Creates temp file
public final Config pushConfig(T ref, Config config)
public final InputStream pullConfig(T ref, Config config)
protected List<Layer> collectLayers(T ref, String contentType, boolean includeAll)
```

### Ref<T> Abstract Base Class

**File**: Ref.java

**Abstract Methods**:
```java
public abstract T withDigest(String digest)
public abstract SupportedAlgorithm getAlgorithm()
public abstract String getRepository()
public abstract T forTarget(String target)
```

### Registry Class

**Extends**: `OCI<ContainerRef>`

**Key Features**:
- Remote OCI registry HTTP client
- HTTPS/HTTP protocol support
- Authentication & token management
- Parallel blob operations (configurable concurrency)
- Digest calculation and validation
- Manifest/Index handling
- Blob lifecycle management (push, pull, delete, exists check)

### OCILayout Class

**Extends**: `OCI<LayoutRef>`

**Key Features**:
- Local filesystem OCI Layout
- Direct file I/O operations
- Multiple digest algorithms (SHA256, SHA512, etc.)
- OCI Layout v1.0.0 spec compliance
- Single-threaded executor

---

## 7. Blob-Related Tests

### RegistryTest.java Blob Tests (56 total methods)

| Test Name | Line | Purpose |
|-----------|------|---------|
| `shouldFailToPushBlobForInvalidDigest` | 211 | Error handling for invalid digests |
| `shouldFailToPushBlobWithMissingDigestViaStream` | 223 | Requires digest for stream upload |
| `shouldPushBlobWithDigestViaStream` | 239 | Stream-based upload with digest |
| `shouldPushAndGetBlobThenDeleteWithSha256` | 254 | Full lifecycle (SHA256) |
| `shouldPushAndGetBlobThenDeleteWithSha512` | 279 | Full lifecycle (SHA512) |
| `shouldFailToPushBlobWithEmptyDigest` | 314 | Edge case handling |

### OCILayoutTest.java Blob Tests

| Test Name | Line | Purpose |
|-----------|------|---------|
| `shouldPushBlob` | 623 | Basic blob push to layout |
| `shouldFailToPushBlobViaStreamWithoutDigest` | 648 | Digest requirement |
| `shouldFailToPushBlobViaStreamWithInvalidDigest` | 660 | Digest format validation |
| `cannotPushBlobWithoutTagOrDigest` | 672 | Tag/digest requirement |
| `cannotPushWithInvalidDigest` | 693 | Invalid digest error |
| `shouldPushAndRetrieveBlob` | 490 | Blob roundtrip |
| `testShouldCopyIntoOciLayoutWithBlobConfig` | 1016 | Copy with blob config |

---

## 8. OCI Distribution Spec Endpoints

### Implemented Registry Blob Endpoints

#### POST - Initiate Upload
```
POST /v2/{name}/blobs/uploads/
Response: 202 Accepted + Location header
Example: POST /v2/library/alpine/blobs/uploads/
         → Location: /v2/library/alpine/blobs/uploads/a1b2c3d4
```

#### POST - Single-Step Upload with Digest
```
POST /v2/{name}/blobs/uploads/?digest={digest}
Response: 201 Created (if upload succeeds immediately)
Example: POST /v2/library/alpine/blobs/uploads/?digest=sha256:abc123...
```

#### PUT - Complete Chunked Upload
```
PUT {location}?digest={digest}
Response: 201 Created
Example: PUT /v2/library/alpine/blobs/uploads/a1b2c3d4?digest=sha256:abc123...
```

#### HEAD - Check Blob Existence
```
HEAD /v2/{name}/blobs/{digest}
Response: 200 OK (exists) or 404 Not Found
```

#### GET - Download Blob
```
GET /v2/{name}/blobs/{digest}
Response: 200 OK + blob content
Headers: Docker-Content-Digest, Content-Length
```

#### DELETE - Delete Blob
```
DELETE /v2/{name}/blobs/{digest}
Response: 202 Accepted or 204 No Content
```

### Upload Strategies

**Single-Step Upload** (for small blobs):
```
1. POST /v2/{repo}/blobs/uploads/?digest={sha256:xxx}
   Response: 201 Created
```

**Chunked Upload** (for large blobs):
```
1. POST /v2/{repo}/blobs/uploads/
   Response: 202 Accepted + Location: /v2/{repo}/blobs/uploads/uuid

2. PUT {Location}?digest={sha256:xxx}
   Response: 201 Created
```

### NOT Implemented

- **❌ MOUNT Endpoint**: `POST /v2/{name}/blobs/uploads/?mount={digest}&from={source_repo}`
  - Would allow cross-repository blob linking without re-upload
  - Not implemented in current codebase

- **❌ PATCH Method**: Chunked streaming upload (monolithic upload used instead)

### Blob Path Construction

```java
// In ContainerRef class:
ref.getBlobsUploadDigestPath(registry)   // "/v2/{repo}/blobs/uploads/?digest={digest}"
ref.getBlobsUploadPath(registry)         // "/v2/{repo}/blobs/uploads/"
ref.getBlobsPath(registry)               // "/v2/{repo}/blobs/{digest}"
```

### Upload Location URI Construction

```java
// Registry.java line 820-840
private URI createLocationWithDigest(String location, String digest) throws URISyntaxException {
    URI uploadURI;
    try {
        uploadURI = new URI(location);
        if (uploadURI.getQuery() == null) {
            uploadURI = new URI(uploadURI + "?digest=%s".formatted(digest));
        } else {
            uploadURI = new URI(uploadURI + "&digest=%s".formatted(digest));
        }
    } catch (URISyntaxException e) {
        throw new OrasException("Failed to create upload URI", e);
    }
    return uploadURI;
}
```

---

## 9. Summary Statistics

| Metric | Value |
|--------|-------|
| Java Source Files (main) | 46 |
| Java Source Files (test) | 39 |
| Total Lines (main code) | ~15,000+ |
| Total Test Methods | 56+ (RegistryTest) |
| Blob Push Methods | 3 (file, stream, bytes) |
| Blob Fetch Methods | 4 (file, stream, bytes, descriptor) |
| Blob Utility Methods | 2 (delete, has) |
| Registry HTTP Methods | 6 (POST, PUT, HEAD, GET, DELETE, PATCH) |
| Blob Upload Strategies | 2 (single-POST, chunked POST+PUT) |
| Sealed Classes | 2 (OCI, Ref) |
| Reference Types | 2 (ContainerRef, LayoutRef) |
| Digest Algorithms | Multiple (SHA256, SHA512) |
| Test Framework | JUnit 5 (Jupiter) 6.0.3 |
| Container Support | TestContainers 2.0.3 + Docker |
| Mock Support | WireMock 3.13.2, Mockito 5.22.0 |
| Mount Support | ❌ Not implemented |

---

## 10. Key Features & Capabilities

### ✅ Blob Management
- Push from file, stream, or byte array
- Download to file or stream
- Parallel upload/download (configurable concurrency)
- Digest verification and validation
- Blob existence checking
- Blob deletion
- Single-step and chunked upload strategies

### ✅ Copy Operations
- Copy between remote registries
- Copy between local OCI layouts
- Mixed copy (registry ↔ layout)
- Shallow copy (no referrers)
- Deep/recursive copy (include referrers)
- Parallel layer copying
- Config and manifest copying

### ✅ OCI Specification Compliance
- OCI Distribution Spec v2 endpoints
- Proper HTTP status code handling (201, 202, 404)
- Location header processing for chunked uploads
- Digest calculation and validation
- Docker-Content-Digest header support

### ✅ Multi-Platform Support
- OCI Image Index (multi-platform) handling
- Nested manifests support
- Referrers (OCI Spec) support
- Platform-specific manifest selection

### ✅ Authentication & Security
- Bearer token authentication
- Username/password authentication
- Token caching and refresh
- TLS verification options
- Insecure HTTP mode option
- Scope-based authorization

### ❌ Not Implemented
- Mount endpoint (cross-repo blob linking)
- PATCH-based streaming upload
- Some advanced registry features
- Direct blob mount operations

---

## Conclusion

The ORAS Java SDK provides a comprehensive, well-architected implementation of OCI Registry operations with a focus on:

1. **Type Safety**: Sealed classes and generics for compile-time safety
2. **Modularity**: Separate abstractions for registry vs. local layout operations
3. **Performance**: Parallel blob operations and streaming uploads
4. **Correctness**: Comprehensive test coverage with integration tests
5. **Standards Compliance**: OCI Distribution Spec v2 adherence

The codebase is production-ready and suitable for applications needing to interact with OCI-compliant registries or local OCI layouts in Java.

