/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package diogomrol.gocd.s3.artifact.plugin.executors;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import diogomrol.gocd.s3.artifact.plugin.ConsoleLogger;
import diogomrol.gocd.s3.artifact.plugin.S3ClientFactory;
import diogomrol.gocd.s3.artifact.plugin.model.ArtifactPlan;
import diogomrol.gocd.s3.artifact.plugin.model.ArtifactStore;
import diogomrol.gocd.s3.artifact.plugin.model.ArtifactStoreConfig;
import diogomrol.gocd.s3.artifact.plugin.model.PublishArtifactRequest;
import com.amazonaws.SdkClientException;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Map;
import java.util.Optional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PublishArtifactExecutorTest {
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

    @Captor ArgumentCaptor<PutObjectRequest> requestCaptor;

    ArtifactStoreConfig storeConfig;

    @Before
    public void setUp() throws IOException, InterruptedException, SdkClientException {
        initMocks(this);
        agentWorkingDir = tmpFolder.newFolder("go-agent");
        when(s3ClientFactory.s3(any())).thenReturn(s3Client);
        storeConfig = new ArtifactStoreConfig("test", "test", "test", "test");
    }

    @Test
    public void shouldPublishArtifactUsingSourceFile() throws IOException, InterruptedException, JSONException {
        final ArtifactPlan artifactPlan = new ArtifactPlan("id", "storeId", "build.json", Optional.of("DestinationFolder"));
        final ArtifactStore artifactStore = new ArtifactStore(artifactPlan.getId(), storeConfig);
        final PublishArtifactRequest publishArtifactRequest = new PublishArtifactRequest(artifactStore, artifactPlan, agentWorkingDir.getAbsolutePath());

        Path path = Paths.get(agentWorkingDir.getAbsolutePath(), "build.json");
        Files.write(path, "{\"content\":\"example artifact file\"}".getBytes());

        when(request.requestBody()).thenReturn(publishArtifactRequest.toJSON());

        final GoPluginApiResponse response = new PublishArtifactExecutor(request, consoleLogger, s3ClientFactory).execute();
        assertThat(response.responseCode()).isEqualTo(200);
        String expectedJSON = "{" +
                "\"metadata\": {" +
                    "\"Source\": \"build.json\"," +
                    "\"Destination\": \"DestinationFolder\"" +
                "}}";
        JSONAssert.assertEquals(expectedJSON, response.responseBody(), JSONCompareMode.STRICT);

        verify(s3Client, times(1)).putObject(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getFile()).isEqualTo(path.toFile());
        assertThat(requestCaptor.getValue().getBucketName()).isEqualTo("test/DestinationFolder");
    }
}
