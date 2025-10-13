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

## Authentication

Using default existing login existing credentials (e.g. `~/.docker/config.json`)

```java
Registry registry = Registry.builder().defaults().build();
```

Using username and password

```java
Registry registry = Registry.builder().defaults("username", "password").build();
```

### Push an Artifact

```java
LocalPath artifact = LocalPath.of("my-file.txt");
Registry registry = Registry.builder().insecure().build();
Manifest manifest = registry.pushArtifact(ContainerRef.parse("localhost:5000/hello:v1"), artifact);
```

### Pull an Artifact

```java
Registry registry = Registry.builder().insecure().build();
registry.pullArtifact(ContainerRef.parse("localhost:5000/hello:v1"), Path.of("folder"), false);
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
