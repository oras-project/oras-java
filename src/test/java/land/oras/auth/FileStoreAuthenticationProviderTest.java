package land.oras.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import land.oras.ContainerRef;
import land.oras.credentials.FileStore;
import land.oras.credentials.FileStore.Credential;
import land.oras.exception.OrasException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileStoreAuthenticationProviderTest {

    @Mock
    private FileStore mockFileStore;

    private FileStoreAuthenticationProvider authProvider;

    public static final String REGISTRY = "localhost:5000";

    @Test
    void testConstructor_validCredentials() throws Exception {
        // Mock valid credentials for the server address
        Credential credential = new Credential("testUser", "testPassword");
        doReturn(credential).when(mockFileStore).get(any(ContainerRef.class));

        // Create the authentication provider
        authProvider = new FileStoreAuthenticationProvider(mockFileStore, REGISTRY);

        // Assert that the authentication provider is created successfully
        assertNotNull(authProvider);
    }

    @Test
    void testGetAuthHeader_validCredentials() throws Exception {
        // Mock valid credentials for the server address
        Credential credential = new Credential("testUser", "testPassword");
        doReturn(credential).when(mockFileStore).get(any(ContainerRef.class));

        // Create the authentication provider
        authProvider = new FileStoreAuthenticationProvider(mockFileStore, REGISTRY);

        // Verify that the getAuthHeader method returns the expected Basic Auth header
        String authHeader = authProvider.getAuthHeader();
        String expectedAuthString = "testUser:testPassword";
        String expectedEncodedAuth =
                "Basic " + Base64.getEncoder().encodeToString(expectedAuthString.getBytes(StandardCharsets.UTF_8));

        assertEquals(expectedEncodedAuth, authHeader);
    }

    @Test
    void testGetAuthHeader_missingCredentials() throws Exception {

        doThrow(new OrasException("No credentials found for containerRef"))
                .when(mockFileStore)
                .get(any(ContainerRef.class));

        // Create the authentication provider, expecting it to throw ConfigLoadingException
        OrasException exception = assertThrows(OrasException.class, () -> {
            new FileStoreAuthenticationProvider(mockFileStore, REGISTRY);
        });

        // Verify the exception message
        assertTrue(exception.getMessage().contains("No credentials found for containerRef"));
    }
}
