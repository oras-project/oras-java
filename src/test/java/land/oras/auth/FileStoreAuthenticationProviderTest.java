package land.oras.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import land.oras.ContainerRef;
import land.oras.credentials.FileStore;
import land.oras.credentials.FileStore.Credential;
import land.oras.exception.OrasException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileStoreAuthenticationProviderTest {

    private FileStore mockFileStore;
    private FileStoreAuthenticationProvider authProvider;
    private ContainerRef serverAddress;

    @BeforeEach
    void setUp() {
        // Mock the FileStore
        mockFileStore = mock(FileStore.class);
    }

    @Test
    void testConstructor_validCredentials() throws Exception {
        // Mock valid credentials for the server address
        Credential credential = new Credential("testUser", "testPassword");
        when(mockFileStore.get(serverAddress)).thenReturn(credential);

        // Create the authentication provider
        authProvider = new FileStoreAuthenticationProvider(mockFileStore, serverAddress);

        // Assert that the authentication provider is created successfully
        assertNotNull(authProvider);
    }

    @Test
    void testGetAuthHeader_validCredentials() throws Exception {
        // Mock valid credentials for the server address
        Credential credential = new Credential("testUser", "testPassword");
        when(mockFileStore.get(serverAddress)).thenReturn(credential);

        // Create the authentication provider
        authProvider = new FileStoreAuthenticationProvider(mockFileStore, serverAddress);

        // Verify that the getAuthHeader method returns the expected Basic Auth header
        String authHeader = authProvider.getAuthHeader();
        String expectedAuthString = "testUser:testPassword";
        String expectedEncodedAuth =
                "Basic " + Base64.getEncoder().encodeToString(expectedAuthString.getBytes(StandardCharsets.UTF_8));

        assertEquals(expectedEncodedAuth, authHeader);
    }

    @Test
    void testGetAuthHeader_missingCredentials() throws Exception {
        // Mock no credentials for the server address
        when(mockFileStore.get(serverAddress)).thenReturn(null);

        // Create the authentication provider, expecting it to throw ConfigLoadingException
        OrasException exception = assertThrows(OrasException.class, () -> {
            new FileStoreAuthenticationProvider(mockFileStore, serverAddress);
        });

        // Verify the exception message
        assertTrue(exception.getMessage().contains("No credentials found for containerRef"));
    }
}
