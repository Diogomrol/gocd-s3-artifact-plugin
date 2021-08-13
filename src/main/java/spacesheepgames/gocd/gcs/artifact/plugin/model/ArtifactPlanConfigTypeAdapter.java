package spacesheepgames.gocd.gcs.artifact.plugin.model;

import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.Optional;

public class ArtifactPlanConfigTypeAdapter implements JsonDeserializer<ArtifactPlanConfig>, JsonSerializer<ArtifactPlanConfig> {

    @Override
    public ArtifactPlanConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        if (isBuildFileConfig(jsonObject)) {
            return new GCSFileArtifactPlanConfig(jsonObject.get("Source").getAsString(), parseDestination(jsonObject));
        } else {
            throw new JsonParseException("Ambiguous or unknown json. `Source` property must be specified.");
        }
    }

    private Optional<String> parseDestination(JsonObject jsonObject) {
        JsonElement destination = jsonObject.get("Destination");
        if (destination != null && StringUtils.isNotBlank(destination.getAsString())) {
            return Optional.of(destination.getAsString());
        }
        return Optional.empty();
    }

    @Override
    public JsonElement serialize(ArtifactPlanConfig src, Type typeOfSrc, JsonSerializationContext context) {
        if (src instanceof GCSFileArtifactPlanConfig) {
            return context.serialize(src, GCSFileArtifactPlanConfig.class);
        }
        throw new JsonIOException("Unknown type of ArtifactPlanConfig");
    }

    private boolean isBuildFileConfig(JsonObject jsonObject) {
        return containsSourceFileProperty(jsonObject);
    }

    private boolean containsSourceFileProperty(JsonObject jsonObject) {
        return jsonObject.has("Source") && isPropertyNotBlank(jsonObject);
    }

    private boolean isPropertyNotBlank(JsonObject jsonObject) {
        try {
            JsonElement jsonElement = jsonObject.get("Source");
            return StringUtils.isNotBlank(jsonElement.getAsString());
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }
}
