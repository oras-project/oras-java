package land.oras.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

@NullMarked
public class RegistryContainer extends GenericContainer<RegistryContainer> {

    private Logger LOG = org.slf4j.LoggerFactory.getLogger(RegistryContainer.class);

    // myuser:mypass
    public static final String AUTH_STRING = "myuser:$2y$05$M1VYs6EzFkXBmuS.BrIreObAnJcWCgzSPeT9/Rh3aVEqTqtSL8XN.";

    /**
     * Create a new registry container
     */
    public RegistryContainer() {
        super("ghcr.io/oras-project/registry:latest");
        addExposedPort(5000);
        addEnv("REGISTRY_STORAGE_DELETE_ENABLED", "true");
        addEnv("REGISTRY_AUTH", "{htpasswd: {realm: localhost, path: /etc/docker/registry/auth.htpasswd}}");
        setWaitStrategy(Wait.forLogMessage(".*listening on.*", 1));

        try {
            // Create a temporary file with the auth string
            Path tempFile = Files.createTempFile("auth", ".htpasswd");
            Files.writeString(tempFile, AUTH_STRING);

            // Copy it into the container
            withCopyFileToContainer(
                    MountableFile.forHostPath(tempFile.toAbsolutePath().toString()),
                    "/etc/docker/registry/auth.htpasswd");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create auth.htpasswd", e);
        }
    }

    /**
     * Get the registry URL
     * @return The registry URL
     */
    public String getRegistry() {
        return getHost() + ":" + getMappedPort(5000);
    }

    public RegistryContainer withFollowOutput() {
        followOutput(new Slf4jLogConsumer(LOG));
        return this;
    }
}
