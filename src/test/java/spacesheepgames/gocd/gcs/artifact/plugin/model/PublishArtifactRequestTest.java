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

package spacesheepgames.gocd.gcs.artifact.plugin.model;

import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class PublishArtifactRequestTest {

    @Test
    public void shouldDeserializeRequestBody() {
        final String json = "{\n" +
                "  \"artifact_plan\": {\n" +
                "    \"configuration\": {\n" +
                "      \"Source\": \"alpine-build.json\"\n" +
                "    },\n" +
                "    \"id\": \"installers\",\n" +
                "    \"storeId\": \"gcs-store\"\n" +
                "  },\n" +
                "  \"artifact_store\": {\n" +
                "    \"configuration\": {\n" +
                "      \"GCSBucket\": \"gcs-url\",\n" +
                "      \"ProjectID\": \"TestProject\"\n" +
                "    },\n" +
                "    \"id\": \"gcs-store\"\n" +
                "  },\n" +
                "  \"agent_working_directory\": \"/temp\"\n" +
                "}";

        final PublishArtifactRequest publishArtifactRequest = PublishArtifactRequest.fromJSON(json);

        assertThat(publishArtifactRequest.getAgentWorkingDir()).isEqualTo("/temp");

        assertThat(publishArtifactRequest.getArtifactStore().getId()).isEqualTo("gcs-store");
        assertThat(publishArtifactRequest.getArtifactStore().getArtifactStoreConfig())
                .isEqualTo(new ArtifactStoreConfig("gcs-url", "TestProject"));

        assertThat(publishArtifactRequest.getArtifactPlan())
                .isEqualTo(new ArtifactPlan("gcs-url", "gcs-store", "alpine-build.json", Optional.empty()));
    }
}