# ORAS Java

[![GitHub Workflow Status](https://github.com/oras-project/oras-java/actions/workflows/build.yml/badge.svg)](https://github.com/oras-project/oras-java/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/oras-project/oras-java/branch/main/graph/badge.svg)](https://codecov.io/gh/oras-project/oras-java)
![GitHub Release](https://img.shields.io/github/v/release/oras-project/oras-java?logo=github&color=green)
[![GitHub license](https://img.shields.io/github/license/oras-project/oras-java)](https://github.com/oras-project/oras-java/blob/main/LICENSE)
[![Javadoc](https://img.shields.io/badge/javadoc-latest-blue)](https://oras-project.github.io/oras-java/)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/oras-project/oras-java/badge)](https://scorecard.dev/viewer/?uri=github.com/oras-project/oras-java)
[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/10047/badge)](https://www.bestpractices.dev/projects/10047)
[![Reproducible Central Artifact](https://img.shields.io/reproducible-central/artifact/land.oras/oras-java-sdk/0.2.15)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/land/oras/oras-java-sdk/README.md)


> [!WARNING]
> The Oras Java SDK is currently in **alpha** state.
>
> It's configuration and APIs might change in future releases

<p align="left">
<a href="https://oras.land/"><img src="https://oras.land/img/oras.svg" alt="banner" width="200px"></a>
</p>

OCI Registry as Storage enables libraries to push OCI Artifacts to [OCI Conformant](https://github.com/opencontainers/oci-conformance) registries. This is a Java SDK for Java developers to empower them to do this in their applications.

## Consuming SDK

SNAPSHOT for version 0.2.x are published on GitHub Maven packages.
SNAPSHOT for version 0.3.x and above are published on Maven Central at: https://central.sonatype.com/repository/maven-snapshots/

Releases are published on Maven Central since version 0.2.x.

Javadoc is published from main branch into: https://oras-project.github.io/oras-java/

```xml
<dependency>
    <groupId>land.oras</groupId>
    <artifactId>oras-java-sdk</artifactId>
    <version>VERSION_HERE</version>
</dependency>
```

### Quarkus

Quarkus users can use the extension `quarkus-oras` to use the SDK in their applications.

Follow the [Quarkus ORAS documentation](https://docs.quarkiverse.io/quarkus-oras/dev/index.html#) to get started with Quarkus.

### Only for SNAPSHOTS (only for testing)

Then on your `pom.xml`

```xml
<repositories>
    <repository>
        <id>central-snapshots</id>
        <name>ORAS Maven Central SNAPSHOTS</name>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
        <releases>
            <enabled>false</enabled>
        </releases>
    </repository>
</repositories>
```

## Examples

### Authentication

Using existing credentials from `~/.docker/config.json` or `$XDG_RUNTIME_DIR/containers/auth.json`:

```java
Registry registry = Registry.builder().defaults().build();
```

Using a username and password:

```java
Registry registry = Registry.builder().defaults("username", "password").build();
```

### Push a single file

```java
Registry registry = Registry.builder().insecure().build();
LocalPath artifact = LocalPath.of(Path.of("my-file.txt"));
Manifest manifest = registry.pushArtifact(ContainerRef.parse("localhost:5000/hello:v1"), artifact);
```

### Push multiple files with a custom artifact type

Push several files at once with a custom artifact type, per-file media types, and manifest-level annotations:

```java
Registry registry = Registry.builder().insecure().build();

Annotations annotations = Annotations.ofManifest(Map.of("build-tool", "maven"))
        .withFileAnnotations("pom.xml", Map.of("format", "xml"));

Manifest manifest = registry.pushArtifact(
        ContainerRef.parse("localhost:5000/my-app:v1"),
        ArtifactType.from("application/vnd.maven+type"),
        annotations,
        LocalPath.of(Path.of("pom.xml"), "application/xml"),
        LocalPath.of(Path.of("target/app.jar"), "application/java-archive"));
```

The resulting manifest will contain one layer per file, each annotated with its filename via `org.opencontainers.image.title`.

### Push a directory

Directories are automatically compressed as a `tar+gzip` archive and tagged with
`org.opencontainers.image.title` set to the directory name. The `io.deis.oras.content.unpack`
annotation is set to `true` so the SDK automatically extracts the archive on pull.

```java
Registry registry = Registry.builder().insecure().build();
Manifest manifest = registry.pushArtifact(
        ContainerRef.parse("localhost:5000/my-configs:v1"),
        LocalPath.of(Path.of("config-dir")));
```

To push a directory as a plain zip instead:

```java
Manifest manifest = registry.pushArtifact(
        ContainerRef.parse("localhost:5000/my-configs:v1"),
        LocalPath.of(Path.of("config-dir"), "application/zip"));
```

### Pull an artifact

Files are automatically written using the `org.opencontainers.image.title` layer annotation as the filename.
The third argument controls whether existing files are overwritten:

```java
Registry registry = Registry.builder().insecure().build();
registry.pullArtifact(ContainerRef.parse("localhost:5000/hello:v1"), Path.of("output-dir"), true);
```

### Attach an artifact (referrers)

Attach a signature or attestation to an already-pushed artifact. The attached manifest references the
original via its `subject` field and is discoverable through the [Referrers API](https://github.com/opencontainers/distribution-spec/blob/main/spec.md#listing-referrers):

```java
Registry registry = Registry.builder().insecure().build();
ContainerRef ref = ContainerRef.parse("localhost:5000/my-app:v1");

// Push the main artifact first
Manifest manifest = registry.pushArtifact(ref,
        ArtifactType.from("application/vnd.maven+type"),
        LocalPath.of(Path.of("pom.xml"), "application/xml"));

// Attach a signature as a referrer
Manifest signatureManifest = registry.attachArtifact(
        ref,
        ArtifactType.from("application/vnd.example.signature"),
        LocalPath.of(Path.of("pom.xml.asc")));

// List all referrers for the artifact
Referrers referrers = registry.getReferrers(
        ref.withDigest(manifest.getDescriptor().getDigest()), null);
```

### Assemble a manifest from individual blobs

For fine-grained control, push blobs and configs individually before assembling and pushing the manifest:

```java
Registry registry = Registry.builder().insecure().build();
ContainerRef ref = ContainerRef.parse("localhost:5000/my-app:v1");

// Push individual layers
Layer layer1 = registry.pushBlob(ref, Files.readAllBytes(Path.of("schema.json")))
        .withAnnotations(Map.of(Const.ANNOTATION_TITLE, "schema.json"));
Layer layer2 = registry.pushBlob(ref, Files.readAllBytes(Path.of("data.csv")))
        .withAnnotations(Map.of(Const.ANNOTATION_TITLE, "data.csv"));

// Push a custom config
Config config = registry.pushConfig(ref, Config.empty().withMediaType("application/vnd.example.config+json"));

// Assemble and push the manifest
Manifest manifest = Manifest.empty()
        .withConfig(config)
        .withLayers(List.of(layer1, layer2));
registry.pushManifest(ref, manifest);
```

### Copy between registries

Copy a tagged artifact — including all its blobs — from one registry to another:

```java
Registry source = Registry.builder().defaults("user", "pass").insecure().build();
Registry target = Registry.builder().defaults("user", "pass").build();

ContainerRef from = ContainerRef.parse("localhost:5000/my-app:v1");
ContainerRef to   = ContainerRef.parse("registry.example.com/my-app:v1");

CopyUtils.copy(source, from, target, to, CopyUtils.CopyOptions.shallow());
```

### OCI Layout

OCI Layout lets you work with artifacts stored on disk in the [OCI Image Layout](https://github.com/opencontainers/image-spec/blob/main/image-layout.md) format.

**Push to an OCI Layout directory:**

```java
LayoutRef ref = LayoutRef.parse("/tmp/my-layout:latest");
OCILayout ociLayout = OCILayout.Builder.builder().defaults(Path.of("/tmp/my-layout")).build();

Manifest manifest = ociLayout.pushArtifact(
        ref,
        ArtifactType.from("application/vnd.example.type"),
        Annotations.empty(),
        LocalPath.of(Path.of("my-file.txt"), "text/plain"));
```

**Pull from an OCI Layout directory:**

```java
LayoutRef ref = LayoutRef.parse("/tmp/my-layout:latest");
OCILayout ociLayout = OCILayout.Builder.builder().defaults(Path.of("/tmp/my-layout")).build();
ociLayout.pullArtifact(ref, Path.of("output-dir"), false);
```

**Tar-backed OCI Layout** (single-file, portable archive):

```java
LayoutRef ref = LayoutRef.parse("/tmp/my-layout.tar:latest");
OCILayout ociLayout = OCILayout.Builder.builder().defaults(Path.of("/tmp/my-layout.tar")).build();

ociLayout.pushArtifact(ref, ArtifactType.from("application/vnd.example.type"),
        Annotations.empty(), LocalPath.of(Path.of("my-file.txt"), "text/plain"));

// Pull from the same tar
ociLayout.pullArtifact(ref, Path.of("output-dir"), false);
```

**Copy from OCI Layout to a registry:**

```java
LayoutRef layoutRef = LayoutRef.parse("/tmp/my-layout:latest");
OCILayout ociLayout = OCILayout.Builder.builder().defaults(Path.of("/tmp/my-layout")).build();

Registry registry = Registry.builder().defaults("user", "pass").build();
ContainerRef target = ContainerRef.parse("registry.example.com/my-app:v1");

CopyUtils.copy(ociLayout, layoutRef, registry, target, CopyUtils.CopyOptions.shallow());
```

## Registries configuration

Since version `0.7.0` the ORAS Java SDK supports the `registries.conf` format
(see the [containers/image documentation](https://github.com/containers/image/blob/main/docs/containers-registries.conf.5.md)).

The SDK reads configuration from the following locations, in order (later entries override earlier ones):

1. `/etc/containers/registries.conf`
2. `/etc/containers/registries.conf.d/*.conf` (alphabetical)
3. `$HOME/.config/containers/registries.conf`
4. `$HOME/.config/containers/registries.conf.d/*.conf` (alphabetical)

Set the `CONTAINERS_REGISTRIES_CONF` environment variable to use a single file exclusively.

### Supported features

```toml
# Short-name resolution mode (enforcing is the default)
short-name-mode = "enforcing"
unqualified-search-registries = ["docker.io"]

# Rewrite a location via a prefix
[[registry]]
prefix = "docker.io/bitnami"
location = "docker.io/bitnamilegacy"

# Block a registry
[[registry]]
prefix = "gcr.io"
blocked = true

# Mark a registry as insecure
[[registry]]
location = "localhost:5000"
insecure = true

# Mirrors — tried in order before falling back to the upstream registry
[[registry]]
prefix = "registry.example.com"
location = "registry.example.com"
mirror-by-digest-only = false   # set to true to restrict all mirrors to digest-only pulls

  [[registry.mirror]]
  location = "mirror1.example.com"
  insecure = false
  pull-from-mirror = "all"       # "all" (default) | "tag-only" | "digest-only"

  [[registry.mirror]]
  location = "mirror2.example.com"
  insecure = true
  pull-from-mirror = "digest-only"
```

## Trust policy

The ORAS Java SDK can enforce a containers trust policy when pulling, using the
[`policy.json`](https://man.archlinux.org/man/containers-policy.json.5.en) format used by Podman,
Skopeo and Buildah.

The policy is loaded from the following locations, in order (the first that exists wins):

1. the path in the `CONTAINERS_POLICY` environment variable (if set)
2. `$HOME/.config/containers/policy.json`
3. `/etc/containers/policy.json`

If no policy file is found, an **accept-all** policy is used. You can also set it explicitly:

```java
// Load from a specific file
Registry registry = Registry.builder()
        .defaults()
        .withPolicy(Path.of("/etc/containers/policy.json"))
        .build();

// Or build one programmatically
Registry registry = Registry.builder()
        .defaults()
        .withPolicy(ContainersPolicy.rejectAll())
        .build();
```

When a policy is set, every manifest/index pull is evaluated against it and rejected
(`OrasException`) if it does not pass.

### Supported requirement types

| Type                     | Supported | Behaviour                                                          |
|--------------------------|-----------|--------------------------------------------------------------------|
| `insecureAcceptAnything` | ✅        | Accept the image without any verification (trust all).             |
| `reject`                 | ✅        | Reject the image unconditionally.                                  |
| `sigstoreSigned`         | ✅        | Accept only images with a valid keyed Sigstore (cosign) signature. |
| `signedBy` (GPG)         | ❌        | Not implemented. Legacy                                            |

### `sigstoreSigned`

Only **keyed** verification is supported for now. If `keyPath` or `keyData` is present it contains a single
Sigstore public key (the `cosign.pub` produced by `cosign generate-key-pair`), and only signatures
made by that key are accepted:

- `keyPath` — path to a PEM public key file.
- `keyData` — the same key, base64-encoded inline.

Multiple keys (`keyPaths`/`keyDatas`) and keyless (Fulcio/Rekor) verification are **not** supported.
Signatures are discovered through the OCI [referrers API](https://github.com/opencontainers/distribution-spec/blob/main/spec.md#listing-referrers)
(the Sigstore bundle, `application/vnd.dev.sigstore.bundle.v0.3+json`, attached to the image); no
local signature store is consulted. Verification binds the signature to the pulled image by its
digest. The `signedIdentity` field is **not supported** and is ignored if present, because the
cosign bundle payload carries only the image digest and no claimed Docker reference to match against.

```json
{
    "default": [{"type": "insecureAcceptAnything"}],
    "transports": {
        "docker": {
            "example.com/my-image": [
                {"type": "sigstoreSigned", "keyPath": "/home/me/my-key.pub"}
            ]
        }
    }
}
```

### Deploy SNAPSHOTS

SNAPSHOTS are automatically deployed when the `main` branch is updated. See the [GitHub Actions](.github/workflows/deploy-snapshots.yml) for more details.

### Perform release

- Ensure the draft release version correspond to the version on the `pom.xml`. Specially if changing the major or minor version. Patch releases are automatically updated.
- Run the release workflow

## Code of Conduct

Please note that this project has adopted the [CNCF Code of Conduct](https://github.com/cncf/foundation/blob/master/code-of-conduct.md).
Please follow it in all your interactions with the project members and users.

## License

This code is licensed under the Apache 2.0 [LICENSE](LICENSE).
