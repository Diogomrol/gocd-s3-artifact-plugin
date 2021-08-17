package spacesheepgames.gocd.gcs.artifact.plugin.model;

import com.google.common.collect.ImmutableSet;
import spacesheepgames.gocd.gcs.artifact.plugin.annotation.FieldMetadata;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import spacesheepgames.gocd.gcs.artifact.plugin.annotation.ValidationResult;

import java.util.Objects;
import java.util.Optional;

public class GCSFileArtifactPlanConfig extends ArtifactPlanConfig {

    private static final ImmutableSet<String> OPTIONAL_PROPERTIES = ImmutableSet.of("Destination");

    @Expose
    @SerializedName("Source")
    @FieldMetadata(key = "Source")
    private String source;

    @Expose
    @SerializedName("Destination")
    @FieldMetadata(key = "Destination")
    private String destination;

    public GCSFileArtifactPlanConfig(String source, Optional<String> destination) {
        this.source = source;
        this.destination = destination.orElse("");
    }

    @Override
    public String getSource() { return source; }

    @Override
    public String getDestination() {
        return destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GCSFileArtifactPlanConfig that = (GCSFileArtifactPlanConfig) o;
        return Objects.equals(source, that.source) && Objects.equals(destination, that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination);
    }

    @Override
    public ValidationResult validate() {
        return new ValidationResult(validateAllFieldsAsRequired(OPTIONAL_PROPERTIES));
    }
}
