package spacesheepgames.gocd.gcs.artifact.plugin;
import spacesheepgames.gocd.gcs.artifact.plugin.model.ArtifactStoreConfig;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;


public class GCSClientFactory {
    private static final GCSClientFactory GCS_CLIENT_FACTORY = new GCSClientFactory();

    public Storage storage(ArtifactStoreConfig artifactStoreConfig) {
        Storage storage = StorageOptions.newBuilder().setProjectId(artifactStoreConfig.getProjectId()).build().getService();
        return storage;
    }

    public static GCSClientFactory instance() {
        return GCS_CLIENT_FACTORY;
    }
}
