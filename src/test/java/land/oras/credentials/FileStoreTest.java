package land.oras.credentials;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import land.oras.ContainerRef;
import land.oras.exception.OrasException;
import land.oras.utils.Const;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class FileStoreTest {

    @TempDir
    private Path tempDir;

    private FileStore fileStore;
    private FileStore.Config mockConfig;
    private FileStore.Credential mockCredential;
    private ContainerRef SERVER_ADDRESS;
    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";

    @BeforeEach
    void setUp() {
        // Mock Config and Credential
        mockConfig = Mockito.mock(FileStore.Config.class);
        mockCredential = new FileStore.Credential(USERNAME, PASSWORD);

        // Create FileStore instance
        fileStore = new FileStore(mockConfig);
    }

    @Test
    void testNewFileStore_success() throws Exception {
        // Simulate loading configuration
        FileStore.Config mockConfig = Mockito.mock(FileStore.Config.class);
        FileStore fileStoreInstance = new FileStore(mockConfig);

        assertNotNull(fileStoreInstance);
    }

    @Test
    void testNewFileStore_defaultLocation_success() throws Exception {
        // Simulate loading configuration from default location
        FileStore fileStoreInstance = FileStore.newFileStore();
        assertNotNull(fileStoreInstance);
    }

    @Test
    void testGetCredential_success() throws Exception {
        // Mock the behavior of getting credentials
        Mockito.when(mockConfig.getCredential(SERVER_ADDRESS)).thenReturn(mockCredential);

        FileStore.Credential credential = fileStore.get(SERVER_ADDRESS);

        assertNotNull(credential);
        assertEquals(USERNAME, credential.username());
        assertEquals(PASSWORD, credential.password());
    }

    @Test
    void testPutCredential_success() throws Exception {
        // Mock the behavior of putting credentials
        Mockito.doNothing().when(mockConfig).putCredential(SERVER_ADDRESS, mockCredential);

        fileStore.put(SERVER_ADDRESS, mockCredential);

        Mockito.verify(mockConfig, Mockito.times(1)).putCredential(SERVER_ADDRESS, mockCredential);
    }

    @Test
    void testPutCredential_invalidFormat_throwsException() {
        // Credential with a colon in the username should throw an exception
        FileStore.Credential invalidCredential = new FileStore.Credential("user:name", PASSWORD);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            fileStore.put(SERVER_ADDRESS, invalidCredential);
        });

        assertEquals(
                FileStore.ERR_BAD_CREDENTIAL_FORMAT + ": colons(:) are not allowed in username", thrown.getMessage());
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

        assertEquals(
                FileStore.ERR_BAD_CREDENTIAL_FORMAT + ": colons(:) are not allowed in username", thrown.getMessage());
    }

    @Test
    void testParse_withAllComponents() {
        String containerName = "registry.example.com/namespace/repository:tag@sha256:123456";
        ContainerRef ref = ContainerRef.parse(containerName);

        assertEquals("registry.example.com", ref.getRegistry());
        assertEquals("namespace", ref.getNamespace());
        assertEquals("repository", ref.getRepository());
        assertEquals("tag", ref.getTag());
        assertEquals("sha256:123456", ref.getDigest());
    }

    @Test
    void testParse_noRegistry_defaultsToDefaultRegistry() {
        String containerName = "namespace/repository:tag";
        ContainerRef ref = ContainerRef.parse(containerName);

        assertEquals(Const.DEFAULT_REGISTRY, ref.getRegistry());
        assertEquals("namespace", ref.getNamespace());
        assertEquals("repository", ref.getRepository());
        assertEquals("tag", ref.getTag());
        assertNull(ref.getDigest());
    }

    @Test
    void testParse_noTag_defaultsToDefaultTag() {
        String containerName = "registry.example.com/namespace/repository";
        ContainerRef ref = ContainerRef.parse(containerName);

        assertEquals("latest", ref.getTag()); // Assuming Const.DEFAULT_TAG = "latest"
    }

    @Test
    void testParse_missingRepository_throwsException() {
        String containerName = "registry.example.com/";
        assertThrows(IllegalArgumentException.class, () -> ContainerRef.parse(containerName));
    }

    @Test
    void testGetTagsPath() {
        String containerName = "registry.example.com/namespace/repository:tag";
        ContainerRef ref = ContainerRef.parse(containerName);

        String expectedTagsPath = "registry.example.com/v2/namespace/repository/tags/list";
        assertEquals(expectedTagsPath, ref.getTagsPath());
    }

    @Test
    void testGetManifestsPath_withDigest() {
        String containerName = "registry.example.com/namespace/repository:tag@sha256:123456";
        ContainerRef ref = ContainerRef.parse(containerName);

        String expectedManifestsPath = "registry.example.com/v2/namespace/repository/manifests/sha256:123456";
        assertEquals(expectedManifestsPath, ref.getManifestsPath());
    }

    @Test
    void testGetManifestsPath_withoutDigest_usesTag() {
        String containerName = "registry.example.com/namespace/repository:tag";
        ContainerRef ref = ContainerRef.parse(containerName);

        String expectedManifestsPath = "registry.example.com/v2/namespace/repository/manifests/tag";
        assertEquals(expectedManifestsPath, ref.getManifestsPath());
    }

    @Test
    void testGetBlobsPath_withoutDigest_throwsException() {
        String containerName = "registry.example.com/namespace/repository:tag";
        ContainerRef ref = ContainerRef.parse(containerName);

        assertThrows(OrasException.class, ref::getBlobsPath);
    }

    @Test
    void testGetBlobsPath_withDigest() {
        String containerName = "registry.example.com/namespace/repository:tag@sha256:123456";
        ContainerRef ref = ContainerRef.parse(containerName);

        String expectedBlobsPath = "registry.example.com/v2/namespace/repository/blobs/sha256:123456";
        assertEquals(expectedBlobsPath, ref.getBlobsPath());
    }

    @Test
    void testConfigLoad_success() throws Exception {
        // Create a temporary JSON file for testing
        ContainerRef containerRef =
                ContainerRef.parse("docker.io/library/foo/hello-world:latest@sha256:1234567890abcdef");

        FileStore.ConfigFile configFile =
                FileStore.ConfigFile.fromCredential(new FileStore.Credential("admin", "password123"));

        // Load the configuration from the temporary file
        FileStore.Config.load(configFile);

        assertEquals("docker.io", containerRef.getRegistry());
        assertEquals("library/foo", containerRef.getNamespace());
        assertEquals("hello-world", containerRef.getRepository());
        assertEquals("latest", containerRef.getTag());
        assertEquals("sha256:1234567890abcdef", containerRef.getDigest());

        // Clean up by deleting the temporary file
        Files.delete(tempDir);
    }
}
