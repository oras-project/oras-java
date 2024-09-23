package land.oras.utils;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

@NullMarked
public class RegistryContainer extends GenericContainer<RegistryContainer> {

    private Logger LOG = org.slf4j.LoggerFactory.getLogger(RegistryContainer.class);

    /**
     * Create a new registry container
     */
    public RegistryContainer() {
        super("ghcr.io/oras-project/registry:latest");
        addExposedPort(5000);
        addEnv("REGISTRY_STORAGE_DELETE_ENABLED", "true");
        setWaitStrategy(Wait.forLogMessage(".*listening on.*", 1));
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
