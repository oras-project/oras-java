package land.oras.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
@Execution(ExecutionMode.CONCURRENT)
public class EnvironmentPasswordProviderTest {

    @SystemStub
    private final EnvironmentVariables envVars =
            new EnvironmentVariables().set("OCI_USERNAME", "the-username").set("OCI_PASSWORD", "fake-password");

    @Test
    public void testEnvironmentPasswordProvider() {
        EnvironmentPasswordProvider provider = new EnvironmentPasswordProvider();
        assertEquals("the-username", provider.getUsername());
        assertEquals("fake-password", provider.getPassword());
    }
}
