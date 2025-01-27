package land.oras.credentials;

import land.oras.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import wiremock.com.fasterxml.jackson.databind.ObjectMapper;


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
         // Create a temporary JSON file for testing
        Map<String, FileStore.Credential> credentials = new HashMap<>();
        credentials.put("server1.example.com", new FileStore.Credential("admin", "password123"));
        credentials.put("server2.example.com", new FileStore.Credential("user", "userpass"));


        String jsonContent = JsonUtils.toJson(credentials);

        // Create a temporary file and write the JSON content to it
        Path tempFile = Files.createTempFile("config", ".json");
        Files.write(tempFile, jsonContent.getBytes());

        // Load the configuration from the temporary file
        FileStore.Config config = FileStore.Config.load(tempFile.toString());

        // Verify that the config was loaded successfully and contains the correct data
        assertNotNull(config);
        assertNotNull(config.getCredential("server1.example.com"));
        assertNotNull(config.getCredential("server2.example.com"));
        assertEquals("admin", config.getCredential("server1.example.com").getUsername());
        assertEquals("password123", config.getCredential("server1.example.com").getPassword());
        assertEquals("user", config.getCredential("server2.example.com").getUsername());
        assertEquals("userpass", config.getCredential("server2.example.com").getPassword());

        // Clean up by deleting the temporary file
        Files.delete(tempFile);
    }
}
