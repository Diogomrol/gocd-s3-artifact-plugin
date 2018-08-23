package diogomrol.gocd.s3.artifact.plugin.model;

import diogomrol.gocd.s3.artifact.plugin.annotation.FieldMetadata;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class SourceFileArtifactPlanConfig extends ArtifactPlanConfig {
    @Expose
    @SerializedName("Source")
    @FieldMetadata(key = "Source")
    private String source;

    public SourceFileArtifactPlanConfig(String source) {
        this.source = source;
    }

    @Override
    public String getSource() {
        return source;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SourceFileArtifactPlanConfig that = (SourceFileArtifactPlanConfig) o;
        return Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source);
    }
}
