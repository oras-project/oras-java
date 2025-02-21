# ORAS Java

[![GitHub Workflow Status](https://github.com/oras-project/oras-java/actions/workflows/build.yml/badge.svg)](https://github.com/oras-project/oras-java/actions/workflows/build.yml)
[![codecov](https://codecov.io/gh/jonesbusy/oras-java/graph/badge.svg?token=NUOO2COAXT)](https://codecov.io/gh/jonesbusy/oras-java)
[![GitHub release](https://img.shields.io/github/v/release/oras-project/oras-java)](https://github.com/oras-project/oras-java/releases)
[![GitHub license](https://img.shields.io/github/license/oras-project/oras-java)](https://github.com/oras-project/oras-java/blob/main/LICENSE)
[![Javadoc](https://img.shields.io/badge/javadoc-latest-blue)](https://oras-project.github.io/oras-java/)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/oras-project/oras-java/badge)](https://scorecard.dev/viewer/?uri=github.com/oras-project/oras-java)
[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/10047/badge)](https://www.bestpractices.dev/projects/10047)

> [!WARNING]
> The Oras Java SDK is currently in **alpha** state.
>
> It's configuration and APIs might change in future releases

![ORAS Logo](https://raw.githubusercontent.com/oras-project/oras-www/main/static/img/oras.png)

OCI Registry as Storage enables libraries to push OCI Artifacts to [OCI Conformant](https://github.com/opencontainers/oci-conformance) registries. This is a Java SDK for Java developers to empower them to do this in their applications.

## Consuming SDK

SNAPSHOT version are published on GitHub Maven package.

Javadoc is published from main branch into: https://oras-project.github.io/oras-java/

GitHub requires authentication to download packages. You can use a personal access token to authenticate with GitHub Packages. To authenticate with GitHub Packages, you need to update your `~/.m2/settings.xml` file to include your personal access token.

```xml
<server>
    <id>oras-java</id>
    <username>YOUR_USERNAME</username>
    <password>YOUR_ACCESS_TOKEN_WITH_PACKAGE_READ_SCOPE</password>
</server>
```

Then on your `pom.xml`

```xml
<repositories>
    <repository>
        <id>oras-java</id>
        <url>https://maven.pkg.github.com/oras-project/oras-java</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

## Examples

### Push an Artifact

```java
Path artifact = Path.of("pom.xml");
Registry registry = Registry.Builder.builder()
        .withInsecure(true)
        .build();
Manifest manifest = registry.pushArtifact(ContainerRef.parse("localhost:5000/hello:v1"), artifact);
```

### Pull an Artifact

```java
registry.pullArtifact(ContainerRef.parse("localhost:5000/hello:v1"), Path.of("folder"));
```

## Authentication

Using docker login existing credentials

```java
Registry registry = Registry.Builder.builder()
        .withAuthProvider(new FileStoreAuthenticationProvider("docker.io"))
        .build();
```

Using username and password

```java
ContainerRef containerRef = ContainerRef.forRegistry("docker.io");
Registry registry = Registry.Builder.builder()
        .withAuthProvider(new UsernamePasswordProvider("username", "password"))
        .build();
```

### Deploy to GitHub Packages

This is temporary until published to Maven Central with a proper workflow.

The maven resolver must be switched to `wagon` to deploy to GitHub Packages.

```shell
mvn -Dmaven.resolver.transport=wagon -DskipTests -Poras-java clean deploy
```

### Perform release

- Ensure the draft release version correspond to the version on the `pom.xml`
- Run the release workflow

## Code of Conduct

Please note that this project has adopted the [CNCF Code of Conduct](https://github.com/cncf/foundation/blob/master/code-of-conduct.md).
Please follow it in all your interactions with the project members and users.

## License

This code is licensed under the Apache 2.0 [LICENSE](LICENSE).
