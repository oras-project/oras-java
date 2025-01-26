package land.oras.credentials;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;


class FileStoreTest {

    private FileStore fileStore;
    private FileStore.Config mockConfig;
    private FileStore.Credential mockCredential;
    private static final String SERVER_ADDRESS = "server.example.com";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";

    @BeforeEach
    void setUp() {
        // Mock Config and Credential
        mockConfig = Mockito.mock(FileStore.Config.class);
        mockCredential = new FileStore.Credential(USERNAME, PASSWORD);

        // Create FileStore instance
        fileStore = new FileStore(false, mockConfig);
    }

    @Test
    void testNewFileStore_success() throws Exception {
        // Simulate loading configuration
        String configPath = "config.json";
        FileStore.Config mockConfig = Mockito.mock(FileStore.Config.class);
        FileStore fileStoreInstance = new FileStore(false, mockConfig);

        assertNotNull(fileStoreInstance);
    }

    @Test
    void testGetCredential_success() throws Exception {
        // Mock the behavior of getting credentials
        Mockito.when(mockConfig.getCredential(SERVER_ADDRESS)).thenReturn(mockCredential);

        FileStore.Credential credential = fileStore.get(SERVER_ADDRESS);

        assertNotNull(credential);
        assertEquals(USERNAME, credential.getUsername());
        assertEquals(PASSWORD, credential.getPassword());
    }

    @Test
    void testPutCredential_success() throws Exception {
        // Mock the behavior of putting credentials
        Mockito.doNothing().when(mockConfig).putCredential(SERVER_ADDRESS, mockCredential);

        fileStore.put(SERVER_ADDRESS, mockCredential);

        Mockito.verify(mockConfig, Mockito.times(1)).putCredential(SERVER_ADDRESS, mockCredential);
    }

    @Test
    void testPutCredential_whenPutDisabled_throwsException() {
        fileStore = new FileStore(true, mockConfig);  // Set disablePut to true

        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, () -> {
            fileStore.put(SERVER_ADDRESS, mockCredential);
        });

        assertEquals(FileStore.ERR_PLAINTEXT_PUT_DISABLED, thrown.getMessage());
    }

    @Test
    void testPutCredential_invalidFormat_throwsException() {
        // Credential with a colon in the username should throw an exception
        FileStore.Credential invalidCredential = new FileStore.Credential("user:name", PASSWORD);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            fileStore.put(SERVER_ADDRESS, invalidCredential);
        });

        assertEquals(FileStore.ERR_BAD_CREDENTIAL_FORMAT + ": colons(:) are not allowed in username", thrown.getMessage());
    }

    @Test
    void testDeleteCredential_success() throws Exception {
        // Mock the behavior of deleting credentials
        Mockito.doNothing().when(mockConfig).deleteCredential(SERVER_ADDRESS);

        fileStore.delete(SERVER_ADDRESS);

        Mockito.verify(mockConfig, Mockito.times(1)).deleteCredential(SERVER_ADDRESS);
    }

    @Test
    void testValidateCredentialFormat_invalidUsernameFormat_throwsException() {
        // Test validation for credentials with colon in username
        FileStore.Credential invalidCredential = new FileStore.Credential("user:name", "password");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            fileStore.put(SERVER_ADDRESS, invalidCredential);
        });

        assertEquals(FileStore.ERR_BAD_CREDENTIAL_FORMAT + ": colons(:) are not allowed in username", thrown.getMessage());
    }

    @Test
    void testConfigLoad_success() throws Exception {
        // Simulate a successful config load
        FileStore.Config config = FileStore.Config.load("config.json");

        assertNotNull(config);
    }
}
