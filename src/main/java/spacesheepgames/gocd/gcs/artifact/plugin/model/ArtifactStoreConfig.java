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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import spacesheepgames.gocd.gcs.artifact.plugin.annotation.FieldMetadata;
import spacesheepgames.gocd.gcs.artifact.plugin.annotation.Validatable;
import spacesheepgames.gocd.gcs.artifact.plugin.annotation.ValidationError;
import spacesheepgames.gocd.gcs.artifact.plugin.annotation.ValidationResult;
import spacesheepgames.gocd.gcs.artifact.plugin.utils.Util;

import java.util.List;
import java.util.Objects;

public class ArtifactStoreConfig implements Validatable {

    private static final ImmutableSet<String> OPTIONAL_PROPERTIES = ImmutableSet.of();

    @Expose
    @SerializedName("GCSBucket")
    @FieldMetadata(key = "GCSBucket", required = true)
    private String gcsBucket;

    @Expose
    @SerializedName("ProjectID")
    @FieldMetadata(key = "ProjectID", required = true)
    private String projectId;

    public ArtifactStoreConfig() {
    }

    public ArtifactStoreConfig(String gcsBucket, String projectId) {
        this.gcsBucket = gcsBucket;
        this.projectId = projectId;
    }

    public String getGcsBucket() {
        return gcsBucket;
    }

    public String getProjectId() {
        return projectId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtifactStoreConfig)) return false;

        ArtifactStoreConfig that = (ArtifactStoreConfig) o;

        return Objects.equals(gcsBucket, that.gcsBucket);
    }

    public static ArtifactStoreConfig fromJSON(String json) {
        return Util.GSON.fromJson(json, ArtifactStoreConfig.class);
    }
    @Override
    public int hashCode() {
        int result = gcsBucket != null ? gcsBucket.hashCode() : 0;
        return result;
    }

    @Override
    public ValidationResult validate() {
        List<ValidationError> validationErrors = Lists.newArrayList();
        validationErrors.addAll(validateAllFieldsAsRequired(OPTIONAL_PROPERTIES));

        return new ValidationResult(validationErrors);
    }
}
