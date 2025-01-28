package land.oras.auth;


import land.oras.credentials.FileStore;
import land.oras.credentials.FileStore.Credential;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileStoreAuthenticationProviderTest {

    private FileStore mockFileStore;
    private FileStoreAuthenticationProvider authProvider;
    private final String serverAddress = "example.com";

    @BeforeEach
    void setUp() {
        // Create a mock FileStore
        mockFileStore = mock(FileStore.class);

        // Initialize FileStoreAuthenticationProvider with the mock
        authProvider = new FileStoreAuthenticationProvider(mockFileStore, serverAddress);
    }

    @Test
    void testGetAuthHeader_validCredentials() throws Exception {
        // Mock valid credentials for the server address
        Credential credential = new Credential("testUser", "testPassword");
        when(mockFileStore.get(serverAddress)).thenReturn(credential);

        // Generate the authentication header
        String authHeader = authProvider.getAuthHeader();

        // Verify the expected Basic Auth header
        String expectedAuthString = "testUser:testPassword";
        String expectedEncodedAuth = "Basic " + Base64.getEncoder().encodeToString(expectedAuthString.getBytes());
        assertEquals(expectedEncodedAuth, authHeader);

        // Verify that username and password fields are set correctly
        assertEquals("testUser", authProvider.getUsername());
        assertEquals("testPassword", authProvider.getPassword());
    }

    @Test
    void testGetAuthHeader_retrievalError() throws Exception {
        // Mock an exception during credential retrieval
        when(mockFileStore.get(serverAddress)).thenThrow(new Exception("FileStore error"));

        // Verify that a RuntimeException is thrown
        RuntimeException exception = assertThrows(RuntimeException.class, authProvider::getAuthHeader);
        assertTrue(exception.getMessage().contains("Failed to generate authentication header"));

        // Ensure username and password fields are not set
        assertNull(authProvider.getUsername());
        assertNull(authProvider.getPassword());
    }

    @Test
    void testUsernameAndPasswordNotSetBeforeCall() {
        // Ensure username and password are null before calling getAuthHeader
        assertNull(authProvider.getUsername());
        assertNull(authProvider.getPassword());
    }


}
