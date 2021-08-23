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

package spacesheepgames.gocd.gcs.artifact.plugin.executors;

import spacesheepgames.gocd.gcs.artifact.plugin.ConsoleLogger;
import spacesheepgames.gocd.gcs.artifact.plugin.GCSClientFactory;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import spacesheepgames.gocd.gcs.artifact.plugin.model.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static spacesheepgames.gocd.gcs.artifact.plugin.GCSArtifactPlugin.LOG;
import static spacesheepgames.gocd.gcs.artifact.plugin.utils.Util.normalizePath;

public class PublishArtifactExecutor implements RequestExecutor {
    private final PublishArtifactRequest publishArtifactRequest;
    private final PublishArtifactResponse publishArtifactResponse;
    private final ConsoleLogger consoleLogger;
    private final GCSClientFactory clientFactory;
    private AntDirectoryScanner scanner;

    public PublishArtifactExecutor(GoPluginApiRequest request, ConsoleLogger consoleLogger) {
        this(request, consoleLogger, GCSClientFactory.instance());
    }

    PublishArtifactExecutor(GoPluginApiRequest request, ConsoleLogger consoleLogger, GCSClientFactory clientFactory) {
        this.publishArtifactRequest = PublishArtifactRequest.fromJSON(request.requestBody());
        this.consoleLogger = consoleLogger;
        this.clientFactory = clientFactory;
        scanner = new AntDirectoryScanner();
        publishArtifactResponse = new PublishArtifactResponse();
    }

    @Override
    public GoPluginApiResponse execute() {
        ArtifactPlan artifactPlan = publishArtifactRequest.getArtifactPlan();
        final ArtifactStoreConfig artifactStoreConfig = publishArtifactRequest.getArtifactStore().getArtifactStoreConfig();
        try {
            final int bufferSize = 1024 * 1024 * 128; // 128Mi
            final Storage storage = clientFactory.storage(artifactStoreConfig);
            final String sourcePattern = artifactPlan.getArtifactPlanConfig().getSource();
            String destinationFolder = artifactPlan.getArtifactPlanConfig().getDestination();
            EnvironmentVariableResolver envResolver = new EnvironmentVariableResolver(destinationFolder, "Destination");
            destinationFolder = envResolver.resolve(publishArtifactRequest.getEnvironmentVariables());
            final String gcsBucket = artifactStoreConfig.getGcsBucket();
            final String workingDir = publishArtifactRequest.getAgentWorkingDir();
            String gcsInBucketPath;
            if(!destinationFolder.isEmpty()) {
                gcsInBucketPath = normalizePath(Paths.get(destinationFolder));
            }
            else {
                gcsInBucketPath = "";
            }

            List<File> matchingFiles = scanner.getFilesMatchingPattern(new File(workingDir), sourcePattern);
            if(matchingFiles.size() == 0) {
                String noFilesMsg = String.format("No files are matching pattern: %s", sourcePattern);
                consoleLogger.error(noFilesMsg);
                LOG.warn(noFilesMsg);
                //TODO: consider handling no artifacts failure in GoCD core
                return DefaultGoPluginApiResponse.badRequest(noFilesMsg);
            }
            else if(matchingFiles.size() == 1) {
                File sourceFile = matchingFiles.get(0);
                String blobName = normalizePath(Paths.get(gcsInBucketPath, sourceFile.getPath()));
                BlobId blobId = BlobId.of(gcsBucket, blobName);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
                Path sourcePath = Paths.get(workingDir, sourceFile.getPath());
                storage.createFrom(blobInfo, sourcePath, bufferSize);
                publishArtifactResponse.addMetadata("Source", sourceFile.toString());
                publishArtifactResponse.addMetadata("IsFile", true);
                consoleLogger.info(String.format("Source file `%s` successfully pushed to GCS bucket `%s`.", sourceFile, artifactStoreConfig.getGcsBucket()));
            }
            else {
                // upload many files
                for(File sourceFile : matchingFiles) {
                    Path sourceFilePath = Paths.get(workingDir, sourceFile.getPath());
                    String blobName = normalizePath(Paths.get(gcsInBucketPath, sourceFile.getPath()));
                    BlobId blobId = BlobId.of(gcsBucket, blobName);
                    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
                    storage.createFrom(blobInfo, sourceFilePath, bufferSize);
                    consoleLogger.info(String.format("Source file `%s` successfully pushed to GCS bucket `%s`.", sourceFile, artifactStoreConfig.getGcsBucket()));
                }
                publishArtifactResponse.addMetadata("Source", sourcePattern);
                publishArtifactResponse.addMetadata("IsFile", false);
            }
            publishArtifactResponse.addMetadata("Destination", gcsInBucketPath);

            return DefaultGoPluginApiResponse.success(publishArtifactResponse.toJSON());
        } catch (Exception e) {
            consoleLogger.error(String.format("Failed to publish %s: %s", artifactPlan, e));
            LOG.error(String.format("Failed to publish %s: %s", artifactPlan, e.getMessage()), e);
            return DefaultGoPluginApiResponse.error(String.format("Failed to publish %s: %s", artifactPlan, e.getMessage()));
        }
    }
}
