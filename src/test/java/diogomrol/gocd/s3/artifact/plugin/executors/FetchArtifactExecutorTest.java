package diogomrol.gocd.s3.artifact.plugin.executors;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    private GoPluginApiRequest request;
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

    @Before
    public void setUp() throws IOException, InterruptedException, SdkClientException {
        initMocks(this);
        agentWorkingDir = tmpFolder.newFolder("go-agent");
        when(s3ClientFactory.s3(any())).thenReturn(s3Client);
        storeConfig = new ArtifactStoreConfig("testBucket", "test", "test", "test");
    }

    @Test
    public void shouldFetchSingleFileWhenUploadedAtRoot() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("Source", "build.json");
        FetchArtifactConfig fetchArtifactConfig = new FetchArtifactConfig();
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, agentWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        final GoPluginApiResponse response = executor.execute();
        assertThat(response.responseCode()).isEqualTo(200);
        verify(s3Client, times(1)).getObject(getRequestCaptor.capture(), fileCaptor.capture());
        assertThat(getRequestCaptor.getValue().getBucketName()).isEqualTo("testBucket");
        assertThat(getRequestCaptor.getValue().getKey()).isEqualTo("build.json");
    }
}
