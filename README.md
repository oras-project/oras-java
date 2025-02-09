# ORAS Java

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
