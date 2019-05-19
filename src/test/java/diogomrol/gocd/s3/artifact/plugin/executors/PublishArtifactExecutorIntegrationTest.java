package diogomrol.gocd.s3.artifact.plugin.executors;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import diogomrol.gocd.s3.artifact.plugin.ConsoleLogger;
import diogomrol.gocd.s3.artifact.plugin.IntegrationTests;
import diogomrol.gocd.s3.artifact.plugin.S3ClientFactory;
import diogomrol.gocd.s3.artifact.plugin.model.ArtifactPlan;
import diogomrol.gocd.s3.artifact.plugin.model.ArtifactStore;
import diogomrol.gocd.s3.artifact.plugin.model.ArtifactStoreConfig;
import diogomrol.gocd.s3.artifact.plugin.model.PublishArtifactRequest;
import diogomrol.gocd.s3.artifact.plugin.utils.Util;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@Category(IntegrationTests.class)
public class PublishArtifactExecutorIntegrationTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Mock
    private GoPluginApiRequest request;
    @Mock
    private ConsoleLogger consoleLogger;
    private S3ClientFactory s3ClientFactory;

    private File agentWorkingDir;
    private AmazonS3 s3Client;

    ArtifactStoreConfig storeConfig;
    private String bucketName;

    @Before
    public void setUp() throws IOException,  SdkClientException {
        initMocks(this);
        s3ClientFactory = new S3ClientFactory();
        agentWorkingDir = tmpFolder.newFolder("go-agent");
        bucketName = System.getenv("AWS_BUCKET");
        if(Util.isBlank(bucketName))
            throw new RuntimeException("Must set AWS_BUCKET env var");
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
    public void shouldPublishArtifactFileWhenDestinationFolder() throws IOException {
        final ArtifactPlan artifactPlan = new ArtifactPlan("id", "storeId", "build.json", Optional.of("DestinationFolder"));
        final ArtifactStore artifactStore = new ArtifactStore(artifactPlan.getId(), storeConfig);
        final PublishArtifactRequest publishArtifactRequest = new PublishArtifactRequest(artifactStore, artifactPlan, agentWorkingDir.getAbsolutePath());

        Path path = Paths.get(agentWorkingDir.getAbsolutePath(), "build.json");
        Files.write(path, "{\"content\":\"example artifact file\"}".getBytes());

        when(request.requestBody()).thenReturn(publishArtifactRequest.toJSON());

        final GoPluginApiResponse response = new PublishArtifactExecutor(request, consoleLogger, s3ClientFactory).execute();
        assertThat(response.responseCode()).isEqualTo(200);

        ObjectListing listing = s3Client.listObjects( bucketName, "DestinationFolder" );
        List<S3ObjectSummary> summaries = listObjects(listing);
        assertThat(summaries).hasOnlyOneElementSatisfying(s -> assertThat(s.getKey()).isEqualTo("DestinationFolder/build.json"));
    }

    @Test
    public void shouldPublishArtifactFilesMatchingPatternWhenNoDestinationFolder() throws IOException {
        final ArtifactPlan artifactPlan = new ArtifactPlan("id", "storeId", "*.json", Optional.empty());
        final ArtifactStore artifactStore = new ArtifactStore(artifactPlan.getId(), storeConfig);
        final PublishArtifactRequest publishArtifactRequest = new PublishArtifactRequest(artifactStore, artifactPlan, agentWorkingDir.getAbsolutePath());

        Path buildJsonPath = Paths.get(agentWorkingDir.getAbsolutePath(), "build.json");
        Files.write(buildJsonPath, "{\"content\":\"example build artifact file\"}".getBytes());
        Path testJsonPath = Paths.get(agentWorkingDir.getAbsolutePath(), "test.json");
        Files.write(testJsonPath, "{\"content\":\"example build artifact file\"}".getBytes());
        Path binPath = Paths.get(agentWorkingDir.getAbsolutePath(), "test.bin");
        Files.write(binPath, "example binary artifact".getBytes());

        when(request.requestBody()).thenReturn(publishArtifactRequest.toJSON());

        final GoPluginApiResponse response = new PublishArtifactExecutor(request, consoleLogger, s3ClientFactory).execute();
        assertThat(response.responseCode()).isEqualTo(200);

        ObjectListing listing = s3Client.listObjects( bucketName);
        List<S3ObjectSummary> summaries = listObjects(listing);
        assertThat(summaries)
                .extracting(S3ObjectSummary::getKey)
                .containsExactly("build.json", "test.json");
    }


    @Test
    public void shouldPublishArtifactFilesMatchingPatternWhenDestinationFolder() throws IOException {
        final ArtifactPlan artifactPlan = new ArtifactPlan("id", "storeId", "**/*.json", Optional.of("DestinationFolder"));
        final ArtifactStore artifactStore = new ArtifactStore(artifactPlan.getId(), storeConfig);
        final PublishArtifactRequest publishArtifactRequest = new PublishArtifactRequest(artifactStore, artifactPlan, agentWorkingDir.getAbsolutePath());

        Path subDir = Paths.get(agentWorkingDir.getAbsolutePath(), "bin");
        subDir.toFile().mkdir();

        Path buildJsonPath = Paths.get(subDir.toAbsolutePath().toString(), "build.json");
        Files.write(buildJsonPath, "{\"content\":\"example build artifact file\"}".getBytes());
        Path testJsonPath = Paths.get(subDir.toAbsolutePath().toString(), "test.json");
        Files.write(testJsonPath, "{\"content\":\"example build artifact file\"}".getBytes());
        Path binPath = Paths.get(subDir.toAbsolutePath().toString(), "test.bin");
        Files.write(binPath, "example binary artifact".getBytes());

        when(request.requestBody()).thenReturn(publishArtifactRequest.toJSON());

        final GoPluginApiResponse response = new PublishArtifactExecutor(request, consoleLogger, s3ClientFactory).execute();
        assertThat(response.responseCode()).isEqualTo(200);

        ObjectListing listing = s3Client.listObjects( bucketName);
        List<S3ObjectSummary> summaries = listObjects(listing);
        assertThat(summaries)
                .extracting(S3ObjectSummary::getKey)
                .containsExactly("DestinationFolder/bin/build.json", "DestinationFolder/bin/test.json");
    }
}
