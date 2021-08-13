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
import spacesheepgames.gocd.gcs.artifact.plugin.model.FetchArtifactConfig;
import spacesheepgames.gocd.gcs.artifact.plugin.model.FetchArtifactRequest;
import spacesheepgames.gocd.gcs.artifact.plugin.utils.Util;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import static spacesheepgames.gocd.gcs.artifact.plugin.GCSArtifactPlugin.LOG;
import static spacesheepgames.gocd.gcs.artifact.plugin.utils.Util.normalizePath;
import static java.lang.String.format;

public class FetchArtifactExecutor implements RequestExecutor {
    private final FetchArtifactRequest fetchArtifactRequest;
    private final ConsoleLogger consoleLogger;
    private final GCSClientFactory clientFactory;

    public FetchArtifactExecutor(GoPluginApiRequest request, ConsoleLogger consoleLogger) {
        this(request, consoleLogger, GCSClientFactory.instance());
    }

    public FetchArtifactExecutor(GoPluginApiRequest request, ConsoleLogger consoleLogger, GCSClientFactory clientFactory)
    {
        this(FetchArtifactRequest.fromJSON(request.requestBody()), consoleLogger, clientFactory);
    }

    public FetchArtifactExecutor(FetchArtifactRequest fetchArtifactRequest, ConsoleLogger consoleLogger, GCSClientFactory clientFactory) {
        this.fetchArtifactRequest = fetchArtifactRequest;
        this.consoleLogger = consoleLogger;
        this.clientFactory = clientFactory;
    }

    @Override
    public GoPluginApiResponse execute() {
        try {
            final Map<String, Object> artifactMetadata = fetchArtifactRequest.getMetadata();
            validateMetadata(artifactMetadata);

            FetchArtifactConfig fetchConfig = fetchArtifactRequest.getFetchArtifactConfig();
            String fetchSubPath = fetchConfig.getSubPath();
            boolean fetchIsFile = fetchConfig.getIsFile();

            final String workingDir = fetchArtifactRequest.getAgentWorkingDir();
            final String gocdSourcePatternOrFilePath = (String)artifactMetadata.get("Source");
            String gcsDestinationPath = (String) artifactMetadata.get("Destination");
            if(Util.isBlank(gcsDestinationPath))
                gcsDestinationPath = "";
            boolean sourceIsFile = (boolean)artifactMetadata.get("IsFile");

            Storage storage = clientFactory.storage(fetchArtifactRequest.getArtifactStoreConfig());
            
            String bucketName = fetchArtifactRequest.getArtifactStoreConfig().getGcsBucket();
            String gcsInBucketName;

            String targetFile;
            if(sourceIsFile) {
                targetFile = Paths.get(gocdSourcePatternOrFilePath).getFileName().toString();
                gcsInBucketName = normalizePath(Paths.get(gcsDestinationPath, gocdSourcePatternOrFilePath));
            }
            else {
                if(fetchIsFile) {
                    if(Util.isBlank(fetchSubPath)) {
                        String errMsg = "Invalid Fetch Configuration: Fetching a single file requires to specify a subpath when multiple artifacts were published";
                        consoleLogger.error(errMsg);
                        LOG.error(errMsg);
                        return DefaultGoPluginApiResponse.incompleteRequest(errMsg);
                    }
                    targetFile = Paths.get(fetchSubPath).getFileName().toString();
                    gcsInBucketName = normalizePath(Paths.get(gcsDestinationPath, fetchSubPath));
                }
                else {
                    String prefix;
                    if(Util.isBlank(fetchSubPath) && Util.isBlank(gcsDestinationPath) )
                        prefix = "";
                    else if(!Util.isBlank(fetchSubPath) && Util.isBlank(gcsDestinationPath) )
                        prefix = fetchSubPath;
                    else if(Util.isBlank(fetchSubPath) && !Util.isBlank(gcsDestinationPath) )
                        prefix = gcsDestinationPath;
                    else
                        prefix = normalizePath(Paths.get(gcsDestinationPath, fetchSubPath));

                    Page<Blob> blobs;
                    if (Util.isBlank(prefix)) {
                        blobs = storage.list(bucketName);
                    } else {
                        blobs = storage.list(bucketName, Storage.BlobListOption.prefix(prefix));
                    }
                    consoleLogger.info(String.format("Retrieving multiple files from GCS bucket `%s` using prefix `%s`", bucketName, prefix));

                    int count = 0;
                    for(Blob blob : blobs.iterateAll()) {
                        targetFile = blob.getName().replaceFirst(prefix, "");
                        Path outFile = getTargetPath(fetchConfig, workingDir, targetFile);
                        gcsInBucketName = blob.getName();
                        LOG.info(String.format("Retrieving file `%s` from GCS bucket `%s`.", gcsInBucketName, bucketName));
                        blob.downloadTo(outFile);
                        count++;
                    }

                    if(count > 0) {
                        consoleLogger.info(String.format("Successfully downloaded `%s` files from GCS bucket `%s` using prefix `%s`", count, bucketName, prefix));
                        return DefaultGoPluginApiResponse.success("");
                    }
                    else {
                        String message = String.format("No objects matching prefix `%s` are in GCS bucket `%s`", prefix, bucketName);
                        consoleLogger.error(message);
                        LOG.error(message);
                        return DefaultGoPluginApiResponse.badRequest(message);
                    }
                }
            }
            Path outFilePath = getTargetPath(fetchConfig, workingDir, targetFile);
            String logMessage = String.format("Retrieving file `%s` from GCS bucket `%s`...", gcsInBucketName, bucketName);
            consoleLogger.info(logMessage);
            LOG.info(logMessage);
            Blob blob = storage.get(BlobId.of(bucketName, gcsInBucketName));
            blob.downloadTo(outFilePath);
            consoleLogger.info(String.format("Source `%s` successfully pulled from GCS bucket `%s` to `%s`.", gcsInBucketName, bucketName, outFilePath));
            return DefaultGoPluginApiResponse.success("");
        } catch (Exception e) {
            final String message = format("Failed pull source file: %s", e);
            consoleLogger.error(message);
            LOG.error(message);
            return DefaultGoPluginApiResponse.error(message);
        }
    }

    private Path getTargetPath(FetchArtifactConfig fetchConfig, String workingDir, String targetFile) {
        Path outFilePath;
        if(Util.isBlank(fetchConfig.getDestination())) {
            outFilePath = Paths.get(workingDir, targetFile);
        }
        else {
            outFilePath = Paths.get(workingDir, fetchConfig.getDestination(), targetFile);
        }
        return outFilePath;
    }

    public void validateMetadata(Map<String, Object> artifactMap) {
        if (artifactMap == null) {
            throw new RuntimeException("Cannot fetch the source file from GCS: Invalid metadata received from the GoCD server. The artifact metadata is null.");
        }

        for(String requiredKey : Arrays.asList("Source", "Destination", "IsFile")) {
            if (!artifactMap.containsKey(requiredKey)) {
                throw new RuntimeException(String.format("Cannot fetch the source file from GCS: Invalid metadata received from the GoCD server. The artifact metadata must contain the key `%s`.", requiredKey));
            }
        }
    }
}
