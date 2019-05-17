package diogomrol.gocd.s3.artifact.plugin.executors;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import diogomrol.gocd.s3.artifact.plugin.ConsoleLogger;
import diogomrol.gocd.s3.artifact.plugin.S3ClientFactory;
import diogomrol.gocd.s3.artifact.plugin.model.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PublishAndFetchIntegrationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private GoPluginApiRequest publishRequest;
    @Mock
    private ConsoleLogger consoleLogger;
    private S3ClientFactory s3ClientFactory;

    private File sourceWorkingDir;
    private AmazonS3 s3Client;

    ArtifactStoreConfig storeConfig;
    private String bucketName;
    private File destinationWorkingDir;

    @Before
    public void setUp() throws IOException, SdkClientException {
        initMocks(this);
        s3ClientFactory = new S3ClientFactory();
        sourceWorkingDir = temporaryFolder.newFolder("go-agent-source");
        destinationWorkingDir = temporaryFolder.newFolder("go-agent-dest");
        bucketName = System.getenv("AWS_BUCKET");
        storeConfig = new ArtifactStoreConfig(bucketName, "eu-west-1", System.getenv("AWS_ACCESS_KEY"), System.getenv("AWS_SECRET_ACCESS_KEY"));
        s3Client = s3ClientFactory.s3(storeConfig);
        ObjectListing listing = s3Client.listObjects( bucketName);
        List<S3ObjectSummary> summaries = listObjects(listing);
        for(S3ObjectSummary o : summaries) {
            s3Client.deleteObject(bucketName, o.getKey());
        }
    }

    private List<S3ObjectSummary> listObjects(ObjectListing listing) {
        List<S3ObjectSummary> summaries = listing.getObjectSummaries();
        while (listing.isTruncated()) {
            listing = s3Client.listNextBatchOfObjects (listing);
            summaries.addAll (listing.getObjectSummaries());
        }
        return summaries;
    }

    @Test
    public void shouldPublishAndFetchArtifactFileWhenDestinationFolder() throws IOException {
        final ArtifactPlan artifactPlan = new ArtifactPlan("id", "storeId", "build.json", Optional.of("DestinationFolder"));
        final ArtifactStore artifactStore = new ArtifactStore(artifactPlan.getId(), storeConfig);
        final PublishArtifactRequest publishArtifactRequest = new PublishArtifactRequest(artifactStore, artifactPlan, sourceWorkingDir.getAbsolutePath());

        Path path = Paths.get(sourceWorkingDir.getAbsolutePath(), "build.json");
        Files.write(path, "{\"content\":\"example artifact file\"}".getBytes());

        when(publishRequest.requestBody()).thenReturn(publishArtifactRequest.toJSON());

        final GoPluginApiResponse response = new PublishArtifactExecutor(publishRequest, consoleLogger, s3ClientFactory).execute();
        assertThat(response.responseCode()).isEqualTo(200);
        Map<String, Object> responseHash = new Gson().fromJson(response.responseBody(), new TypeToken<Map<String,Object>>(){}.getType());
        Map<String, Object> metadata = (Map<String, Object>)responseHash.get("metadata");
        FetchArtifactConfig fetchArtifactConfig = new FetchArtifactConfig();
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, destinationWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        GoPluginApiResponse fetchResponse = executor.execute();
        assertThat(fetchResponse.responseCode()).isEqualTo(200);
        Path destPath = Paths.get(destinationWorkingDir.getAbsolutePath(), "build.json");
        assertThat(destPath).isRegularFile();
    }

    @Test
    public void shouldPublishAndFetchArtifactFileWhenUploadedManyFiles() throws IOException {
        final ArtifactPlan artifactPlan = new ArtifactPlan("id", "storeId", "**/*.json", Optional.of("DestinationFolder"));
        final ArtifactStore artifactStore = new ArtifactStore(artifactPlan.getId(), storeConfig);
        final PublishArtifactRequest publishArtifactRequest = new PublishArtifactRequest(artifactStore, artifactPlan, sourceWorkingDir.getAbsolutePath());

        Path subDir = Paths.get(sourceWorkingDir.getAbsolutePath(), "bin");
        subDir.toFile().mkdir();

        Path buildJsonPath = Paths.get(subDir.toAbsolutePath().toString(), "build.json");
        Files.write(buildJsonPath, "{\"content\":\"example build artifact file\"}".getBytes());
        Path testJsonPath = Paths.get(subDir.toAbsolutePath().toString(), "test.json");
        Files.write(testJsonPath, "{\"content\":\"example build artifact file\"}".getBytes());
        Path binPath = Paths.get(subDir.toAbsolutePath().toString(), "test.bin");
        Files.write(binPath, "example binary artifact".getBytes());

        when(publishRequest.requestBody()).thenReturn(publishArtifactRequest.toJSON());

        final GoPluginApiResponse response = new PublishArtifactExecutor(publishRequest, consoleLogger, s3ClientFactory).execute();
        assertThat(response.responseCode()).isEqualTo(200);
        Map<String, Object> responseHash = new Gson().fromJson(response.responseBody(), new TypeToken<Map<String,Object>>(){}.getType());
        Map<String, Object> metadata = (Map<String, Object>)responseHash.get("metadata");
        FetchArtifactConfig fetchArtifactConfig = new FetchArtifactConfig("bin/build.json", "bin", true);
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, destinationWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        GoPluginApiResponse fetchResponse = executor.execute();
        assertThat(fetchResponse.responseCode()).isEqualTo(200);
        Path destPath = Paths.get(destinationWorkingDir.getAbsolutePath(), "bin/build.json");
        assertThat(destPath).isRegularFile();
    }

    @Test
    public void shouldPublishAndFetchArtifactDirectoryWhenUploadedManyFiles() throws IOException {
        final ArtifactPlan artifactPlan = new ArtifactPlan("id", "storeId", "**/*.json", Optional.of("DestinationFolder"));
        final ArtifactStore artifactStore = new ArtifactStore(artifactPlan.getId(), storeConfig);
        final PublishArtifactRequest publishArtifactRequest = new PublishArtifactRequest(artifactStore, artifactPlan, sourceWorkingDir.getAbsolutePath());

        Path subDir = Paths.get(sourceWorkingDir.getAbsolutePath(), "bin");
        subDir.toFile().mkdir();

        Path buildJsonPath = Paths.get(subDir.toAbsolutePath().toString(), "build.json");
        Files.write(buildJsonPath, "{\"content\":\"example build artifact file\"}".getBytes());
        Path testJsonPath = Paths.get(subDir.toAbsolutePath().toString(), "test.json");
        Files.write(testJsonPath, "{\"content\":\"example build artifact file\"}".getBytes());
        Path binPath = Paths.get(subDir.toAbsolutePath().toString(), "test.bin");
        Files.write(binPath, "example binary artifact".getBytes());

        when(publishRequest.requestBody()).thenReturn(publishArtifactRequest.toJSON());

        final GoPluginApiResponse response = new PublishArtifactExecutor(publishRequest, consoleLogger, s3ClientFactory).execute();
        assertThat(response.responseCode()).isEqualTo(200);
        Map<String, Object> responseHash = new Gson().fromJson(response.responseBody(), new TypeToken<Map<String,Object>>(){}.getType());
        Map<String, Object> metadata = (Map<String, Object>)responseHash.get("metadata");
        FetchArtifactConfig fetchArtifactConfig = new FetchArtifactConfig("", "", false);
        FetchArtifactRequest fetchArtifactRequest = new FetchArtifactRequest(storeConfig, metadata, fetchArtifactConfig, destinationWorkingDir.toString());
        FetchArtifactExecutor executor = new FetchArtifactExecutor(fetchArtifactRequest, consoleLogger, s3ClientFactory);
        GoPluginApiResponse fetchResponse = executor.execute();
        assertThat(fetchResponse.responseCode()).isEqualTo(200);
        assertThat(Paths.get(destinationWorkingDir.getAbsolutePath(), "bin/build.json")).isRegularFile();
        assertThat(Paths.get(destinationWorkingDir.getAbsolutePath(), "bin/test.json")).isRegularFile();
    }
}
