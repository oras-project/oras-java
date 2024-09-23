# ORAS Java

> [!WARNING]
> The Oras Java SDK is currently in **alpha** state.
>
> It's configuration and APIs might change in future releases

![ORAS Logo](https://raw.githubusercontent.com/oras-project/oras-www/main/static/img/oras.png)

OCI Registry as Storage enables libraries to push OCI Artifacts to [OCI Conformant](https://github.com/opencontainers/oci-conformance) registries. This is a Java SDK for Java developers to empower them to do this in their applications.

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

## Code of Conduct

Please note that this project has adopted the [CNCF Code of Conduct](https://github.com/cncf/foundation/blob/master/code-of-conduct.md).
Please follow it in all your interactions with the project members and users.

## License

This code is licensed under the Apache 2.0 [LICENSE](LICENSE).
