package diogomrol.gocd.s3.artifact.plugin.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import diogomrol.gocd.s3.artifact.plugin.annotation.FieldMetadata;
import diogomrol.gocd.s3.artifact.plugin.annotation.Validatable;
import diogomrol.gocd.s3.artifact.plugin.annotation.ValidationResult;
import diogomrol.gocd.s3.artifact.plugin.utils.Util;
import org.apache.commons.lang.StringUtils;

public class FetchArtifactConfig implements Validatable {
    @Expose
    @SerializedName("SubDirectory")
    @FieldMetadata(key = "SubDirectory", required = false)
    private String subDirectory;

    public FetchArtifactConfig() {
    }

    public FetchArtifactConfig(String subDir) {
        this.subDirectory = subDir;
    }

    public static FetchArtifactConfig fromJSON(String json) {
        return Util.GSON.fromJson(json, FetchArtifactConfig.class);
    }

    public String getSubDirectory() {
        return subDirectory;
    }

    @Override
    public ValidationResult validate() {
        ValidationResult validationResult = new ValidationResult();
        //TODO: tomzo check if subdirectory is a valid path for S3 path
        return validationResult;
    }
}