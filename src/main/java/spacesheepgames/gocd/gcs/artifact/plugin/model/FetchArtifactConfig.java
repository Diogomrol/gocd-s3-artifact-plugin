package spacesheepgames.gocd.gcs.artifact.plugin.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import spacesheepgames.gocd.gcs.artifact.plugin.annotation.FieldMetadata;
import spacesheepgames.gocd.gcs.artifact.plugin.annotation.Validatable;
import spacesheepgames.gocd.gcs.artifact.plugin.annotation.ValidationResult;
import spacesheepgames.gocd.gcs.artifact.plugin.utils.Util;

public class FetchArtifactConfig implements Validatable {
    @Expose
    @SerializedName("SubPath")
    @FieldMetadata(key = "SubPath", required = false)
    private String subPath;

    @Expose
    @SerializedName("IsFile")
    @FieldMetadata(key = "IsFile", required = false)
    private boolean isFile;

    @Expose
    @SerializedName("Destination")
    @FieldMetadata(key = "Destination", required = false)
    private String destination;

    public FetchArtifactConfig() {
    }

    public FetchArtifactConfig(String subPath, String destination, boolean isFile) {
        this.isFile = isFile;
        this.destination = destination;
        this.subPath = subPath;
    }

    public static FetchArtifactConfig fromJSON(String json) {
        return Util.GSON.fromJson(json, FetchArtifactConfig.class);
    }

    public String getSubPath() {
        return subPath;
    }

    @Override
    public ValidationResult validate() {
        ValidationResult validationResult = new ValidationResult();
        //TODO: check if subdirectory is a valid path for GCS path
        return validationResult;
    }

    public boolean getIsFile() {
        return isFile;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}