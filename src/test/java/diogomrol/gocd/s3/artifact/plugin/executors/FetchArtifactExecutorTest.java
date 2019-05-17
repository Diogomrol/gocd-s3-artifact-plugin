package diogomrol.gocd.s3.artifact.plugin.executors;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import diogomrol.gocd.s3.artifact.plugin.ConsoleLogger;
import diogomrol.gocd.s3.artifact.plugin.S3ClientFactory;
import diogomrol.gocd.s3.artifact.plugin.model.ArtifactStoreConfig;
import diogomrol.gocd.s3.artifact.plugin.model.FetchArtifactConfig;
import diogomrol.gocd.s3.artifact.plugin.model.FetchArtifactRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class FetchArtifactExecutorTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private ConsoleLogger consoleLogger;
    @Mock
    private S3ClientFactory s3ClientFactory;

    private File agentWorkingDir;
    @Mock
    private AmazonS3 s3Client;

    @Captor ArgumentCaptor<GetObjectRequest> getRequestCaptor;
    @Captor ArgumentCaptor<File> fileCaptor;
    private ArtifactStoreConfig storeConfig;
    private FetchArtifactConfig fetchArtifactConfig;

    @Before
    public void setUp() throws IOException, InterruptedException, SdkClientException {
        initMocks(this);
        agentWorkingDir = tmpFolder.newFolder("go-agent");
        when(s3ClientFactory.s3(any())).thenReturn(s3Client);
        storeConfig = new ArtifactStoreConfig("testBucket", "test", "test", "test");
        fetchArtifactConfig = new FetchArtifactConfig();
    }

    @Test
    public void shouldFetchSingleFileWhenUploadedAtRoot() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Source", "build.json");
        metadata.put("Destination", "");
        metadata.put("IsFile", true);
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, agentWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        final GoPluginApiResponse response = executor.execute();
        assertThat(response.responseCode()).isEqualTo(200);
        verify(s3Client, times(1)).getObject(getRequestCaptor.capture(), fileCaptor.capture());
        assertThat(getRequestCaptor.getValue().getBucketName()).isEqualTo("testBucket");
        assertThat(getRequestCaptor.getValue().getKey()).isEqualTo("build.json");
        assertThat(fileCaptor.getValue().getAbsoluteFile()).isEqualTo(Paths.get(agentWorkingDir.toString(), "build.json").toFile());
    }

    @Test
    public void shouldFetchSingleFileWhenUploadedAtDestinationFolder() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Source", "build.json");
        metadata.put("Destination", "x/y");
        metadata.put("IsFile", true);
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, agentWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        final GoPluginApiResponse response = executor.execute();
        assertThat(response.responseCode()).isEqualTo(200);
        verify(s3Client, times(1)).getObject(getRequestCaptor.capture(), fileCaptor.capture());
        assertThat(getRequestCaptor.getValue().getBucketName()).isEqualTo("testBucket");
        assertThat(getRequestCaptor.getValue().getKey()).isEqualTo("x/y/build.json");
        assertThat(fileCaptor.getValue().getAbsoluteFile()).isEqualTo(Paths.get(agentWorkingDir.toString(), "build.json").toFile());
    }

    @Test
    public void shouldFetchSingleFileWhenLocalDestination() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Source", "build.json");
        metadata.put("Destination", "x/y");
        metadata.put("IsFile", true);
        fetchArtifactConfig = new FetchArtifactConfig(null, "local", true);
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, agentWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        final GoPluginApiResponse response = executor.execute();
        assertThat(response.responseCode()).isEqualTo(200);
        verify(s3Client, times(1)).getObject(getRequestCaptor.capture(), fileCaptor.capture());
        assertThat(getRequestCaptor.getValue().getBucketName()).isEqualTo("testBucket");
        assertThat(getRequestCaptor.getValue().getKey()).isEqualTo("x/y/build.json");
        assertThat(fileCaptor.getValue().getAbsoluteFile()).isEqualTo(Paths.get(agentWorkingDir.toString(), "local/build.json").toFile());
    }

    @Test
    public void shouldFetchSingleFileWhenMultipleWerePublished() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Source", "*.json");
        metadata.put("Destination", "");
        metadata.put("IsFile", false);
        fetchArtifactConfig = new FetchArtifactConfig("build.json", "local", true);
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, agentWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        final GoPluginApiResponse response = executor.execute();
        assertThat(response.responseCode()).isEqualTo(200);
        verify(s3Client, times(1)).getObject(getRequestCaptor.capture(), fileCaptor.capture());
        assertThat(getRequestCaptor.getValue().getBucketName()).isEqualTo("testBucket");
        assertThat(getRequestCaptor.getValue().getKey()).isEqualTo("build.json");
        assertThat(fileCaptor.getValue().getAbsoluteFile()).isEqualTo(Paths.get(agentWorkingDir.toString(), "local/build.json").toFile());
    }

    @Test
    public void shouldFailFetchSingleFileWhenMultipleWerePublishedButNoSubpath() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Source", "*.json");
        metadata.put("Destination", "");
        metadata.put("IsFile", false);
        fetchArtifactConfig = new FetchArtifactConfig(null, "local", true);
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, agentWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        final GoPluginApiResponse response = executor.execute();
        assertThat(response.responseCode()).isEqualTo(412);
        verify(s3Client, times(0)).getObject(any(), any(String.class));
    }

    @Test
    public void shouldFailFetchDirectoryWhenNoObjectsMatchPrefix() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Source", "*.json");
        metadata.put("Destination", "");
        metadata.put("IsFile", false);
        fetchArtifactConfig = new FetchArtifactConfig("bla/h", "local", false);
        ObjectListing objectLists = new ObjectListing();
        objectLists.setBucketName("testBucket");
        when(s3Client.listObjects(any(String.class), eq("bla/h"))).thenReturn(objectLists);
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, agentWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        final GoPluginApiResponse response = executor.execute();
        assertThat(response.responseCode()).isEqualTo(400);
        verify(s3Client, times(0)).getObject(any(), any(String.class));
    }

    @Test
    public void shouldFetchSingleFileWhenMultipleWerePublishedAtCustomDestination() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Source", "*.json");
        metadata.put("Destination", "x/y");
        metadata.put("IsFile", false);
        fetchArtifactConfig = new FetchArtifactConfig("build.json", "local", true);
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, agentWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        final GoPluginApiResponse response = executor.execute();
        assertThat(response.responseCode()).isEqualTo(200);
        verify(s3Client, times(1)).getObject(getRequestCaptor.capture(), fileCaptor.capture());
        assertThat(getRequestCaptor.getValue().getBucketName()).isEqualTo("testBucket");
        assertThat(getRequestCaptor.getValue().getKey()).isEqualTo("x/y/build.json");
        assertThat(fileCaptor.getValue().getAbsoluteFile()).isEqualTo(Paths.get(agentWorkingDir.toString(), "local/build.json").toFile());
    }

    @Test
    public void shouldFetchDirectoryWhenMultipleWerePublished() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Source", "*.json");
        metadata.put("Destination", "");
        metadata.put("IsFile", false);
        fetchArtifactConfig = new FetchArtifactConfig(null, "local", false);
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, agentWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        ObjectListing objectLists = new ObjectListing();
        objectLists.setBucketName("testBucket");
        addObject(objectLists, "build.json");
        addObject(objectLists, "test.json");
        when(s3Client.listObjects(any(String.class))).thenReturn(objectLists);
        final GoPluginApiResponse response = executor.execute();
        assertThat(response.responseCode()).isEqualTo(200);
        verify(s3Client, times(1)).listObjects("testBucket");
        verify(s3Client, times(2)).getObject(getRequestCaptor.capture(), fileCaptor.capture());
        List<GetObjectRequest> allRequestsMade = getRequestCaptor.getAllValues();
        assertThat(allRequestsMade)
                .hasSize(2)
                .extracting(GetObjectRequest::getBucketName)
                .containsExactly("testBucket", "testBucket");
        assertThat(allRequestsMade)
                .hasSize(2)
                .extracting(GetObjectRequest::getKey)
                .containsExactly("build.json", "test.json");
        List<File> allFilesDownloaded = fileCaptor.getAllValues();
        assertThat(allFilesDownloaded)
                .hasSize(2)
                .extracting(File::getAbsolutePath)
                .contains(Paths.get(agentWorkingDir.toString(), "local/build.json").toString(), Paths.get(agentWorkingDir.toString(), "local/test.json").toString());
    }

    @Test
    public void shouldFetchDirectoryWhenMultipleWerePublishedAtCustomDirectory() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Source", "*.json");
        metadata.put("Destination", "x/y");
        metadata.put("IsFile", false);
        fetchArtifactConfig = new FetchArtifactConfig(null, "local", false);
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, agentWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        ObjectListing objectLists = new ObjectListing();
        objectLists.setBucketName("testBucket");
        addObject(objectLists, "x/y/build.json");
        addObject(objectLists, "x/y/test.json");
        when(s3Client.listObjects(any(String.class), eq("x/y"))).thenReturn(objectLists);
        final GoPluginApiResponse response = executor.execute();
        assertThat(response.responseCode()).isEqualTo(200);
        verify(s3Client, times(1)).listObjects("testBucket", "x/y");
        verify(s3Client, times(2)).getObject(getRequestCaptor.capture(), fileCaptor.capture());
        List<GetObjectRequest> allRequestsMade = getRequestCaptor.getAllValues();
        assertThat(allRequestsMade)
                .hasSize(2)
                .extracting(GetObjectRequest::getBucketName)
                .containsExactly("testBucket", "testBucket");
        assertThat(allRequestsMade)
                .hasSize(2)
                .extracting(GetObjectRequest::getKey)
                .containsExactly("x/y/build.json", "x/y/test.json");
        List<File> allFilesDownloaded = fileCaptor.getAllValues();
        assertThat(allFilesDownloaded)
                .hasSize(2)
                .extracting(File::getAbsolutePath)
                .contains(Paths.get(agentWorkingDir.toString(), "local/build.json").toString(), Paths.get(agentWorkingDir.toString(), "local/test.json").toString());
    }

    @Test
    public void shouldFetchRootDirectoryWhenSubdirectoryWasPublishedAtCustomDirectory() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Source", "**/*.json");
        metadata.put("Destination", "x/y");
        metadata.put("IsFile", false);
        fetchArtifactConfig = new FetchArtifactConfig(null, "local", false);
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, agentWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        ObjectListing objectLists = new ObjectListing();
        objectLists.setBucketName("testBucket");
        addObject(objectLists, "x/y/bin/build.json");
        addObject(objectLists, "x/y/bin/test.json");
        when(s3Client.listObjects(any(String.class), eq("x/y"))).thenReturn(objectLists);
        final GoPluginApiResponse response = executor.execute();
        assertThat(response.responseCode()).isEqualTo(200);
        verify(s3Client, times(1)).listObjects("testBucket", "x/y");
        verify(s3Client, times(2)).getObject(getRequestCaptor.capture(), fileCaptor.capture());
        List<GetObjectRequest> allRequestsMade = getRequestCaptor.getAllValues();
        assertThat(allRequestsMade)
                .hasSize(2)
                .extracting(GetObjectRequest::getBucketName)
                .containsExactly("testBucket", "testBucket");
        assertThat(allRequestsMade)
                .hasSize(2)
                .extracting(GetObjectRequest::getKey)
                .containsExactly("x/y/bin/build.json", "x/y/bin/test.json");
        List<File> allFilesDownloaded = fileCaptor.getAllValues();
        assertThat(allFilesDownloaded)
                .hasSize(2)
                .extracting(File::getAbsolutePath)
                .contains(Paths.get(agentWorkingDir.toString(), "local/bin/build.json").toString(), Paths.get(agentWorkingDir.toString(), "local/bin/test.json").toString());
    }

    @Test
    public void shouldFetchSubDirectoryWhenSubdirectoryWasPublishedAtCustomDirectory() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("Source", "**/*.json");
        metadata.put("Destination", "x/y");
        metadata.put("IsFile", false);
        fetchArtifactConfig = new FetchArtifactConfig("bin", "local", false);
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, agentWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        ObjectListing objectLists = new ObjectListing();
        objectLists.setBucketName("testBucket");
        addObject(objectLists, "x/y/bin/build.json");
        addObject(objectLists, "x/y/bin/test.json");
        when(s3Client.listObjects(any(String.class), eq("x/y/bin"))).thenReturn(objectLists);
        final GoPluginApiResponse response = executor.execute();
        assertThat(response.responseCode()).isEqualTo(200);
        verify(s3Client, times(1)).listObjects("testBucket", "x/y/bin");
        verify(s3Client, times(2)).getObject(getRequestCaptor.capture(), fileCaptor.capture());
        List<GetObjectRequest> allRequestsMade = getRequestCaptor.getAllValues();
        assertThat(allRequestsMade)
                .hasSize(2)
                .extracting(GetObjectRequest::getBucketName)
                .containsExactly("testBucket", "testBucket");
        assertThat(allRequestsMade)
                .hasSize(2)
                .extracting(GetObjectRequest::getKey)
                .containsExactly("x/y/bin/build.json", "x/y/bin/test.json");
        List<File> allFilesDownloaded = fileCaptor.getAllValues();
        assertThat(allFilesDownloaded)
                .hasSize(2)
                .extracting(File::getAbsolutePath)
                .contains(Paths.get(agentWorkingDir.toString(), "local/build.json").toString(), Paths.get(agentWorkingDir.toString(), "local/test.json").toString());
    }

    private void addObject(ObjectListing objectLists, String key) {
        S3ObjectSummary buildJsonObj = new S3ObjectSummary();
        buildJsonObj.setKey(key);
        objectLists.getObjectSummaries().add(buildJsonObj);
    }
}
