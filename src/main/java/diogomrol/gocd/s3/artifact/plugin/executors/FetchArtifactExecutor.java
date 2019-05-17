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

package diogomrol.gocd.s3.artifact.plugin.executors;

import com.amazonaws.services.s3.model.*;
import diogomrol.gocd.s3.artifact.plugin.ConsoleLogger;
import diogomrol.gocd.s3.artifact.plugin.S3ClientFactory;
import diogomrol.gocd.s3.artifact.plugin.model.AntDirectoryScanner;
import diogomrol.gocd.s3.artifact.plugin.model.ArtifactStoreConfig;
import diogomrol.gocd.s3.artifact.plugin.model.FetchArtifactConfig;
import diogomrol.gocd.s3.artifact.plugin.model.FetchArtifactRequest;
import diogomrol.gocd.s3.artifact.plugin.utils.Util;
import com.amazonaws.services.s3.AmazonS3;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static diogomrol.gocd.s3.artifact.plugin.S3ArtifactPlugin.LOG;
import static diogomrol.gocd.s3.artifact.plugin.utils.Util.normalizePath;
import static java.lang.String.format;

public class FetchArtifactExecutor implements RequestExecutor {
    private FetchArtifactRequest fetchArtifactRequest;
    private final ConsoleLogger consoleLogger;
    private S3ClientFactory clientFactory;

    public FetchArtifactExecutor(GoPluginApiRequest request, ConsoleLogger consoleLogger) {
        this(request, consoleLogger, S3ClientFactory.instance());
    }

    public FetchArtifactExecutor(GoPluginApiRequest request, ConsoleLogger consoleLogger, S3ClientFactory clientFactory)
    {
        this(FetchArtifactRequest.fromJSON(request.requestBody()), consoleLogger, clientFactory);
    }

    public FetchArtifactExecutor(FetchArtifactRequest fetchArtifactRequest, ConsoleLogger consoleLogger, S3ClientFactory clientFactory) {
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
            String awsDestinationPath = (String) artifactMetadata.get("Destination");
            if(Util.isBlank(awsDestinationPath))
                awsDestinationPath = "";
            boolean sourceIsFile = (boolean)artifactMetadata.get("IsFile");

            AmazonS3 s3 = clientFactory.s3(fetchArtifactRequest.getArtifactStoreConfig());
            String bucketName = fetchArtifactRequest.getArtifactStoreConfig().getS3bucket();
            String s3InbucketPath;

            String targetFile;
            if(sourceIsFile) {
                targetFile = Paths.get(gocdSourcePatternOrFilePath).getFileName().toString();
                s3InbucketPath = normalizePath(Paths.get(awsDestinationPath, gocdSourcePatternOrFilePath));
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
                    s3InbucketPath = normalizePath(Paths.get(awsDestinationPath, fetchSubPath));
                }
                else {
                    String prefix;
                    if(Util.isBlank(fetchSubPath) && Util.isBlank(awsDestinationPath) )
                        prefix = "";
                    else if(!Util.isBlank(fetchSubPath) && Util.isBlank(awsDestinationPath) )
                        prefix = fetchSubPath;
                    else if(Util.isBlank(fetchSubPath) && !Util.isBlank(awsDestinationPath) )
                        prefix = awsDestinationPath;
                    else
                        prefix = normalizePath(Paths.get(awsDestinationPath, fetchSubPath));

                    ObjectListing listing = Util.isBlank(prefix) ? s3.listObjects(bucketName) : s3.listObjects(bucketName, prefix);
                    consoleLogger.info(String.format("Retrieving multiple files from S3 bucket `%s` using prefix `%s`", bucketName, prefix));
                    int count = 0;
                    while(true) {
                        for(S3ObjectSummary obj : listing.getObjectSummaries()) {
                            targetFile = obj.getKey().replaceFirst(prefix, "");
                            File outFile = getTargetFile(fetchConfig, workingDir, targetFile);
                            s3InbucketPath = obj.getKey();
                            LOG.info(String.format("Retrieving file `%s` from S3 bucket `%s`.", s3InbucketPath, bucketName));
                            GetObjectRequest getRequest = new GetObjectRequest(bucketName, s3InbucketPath);
                            s3.getObject(getRequest, outFile);
                            count++;
                        }
                        if(listing.isTruncated())
                            listing = s3.listNextBatchOfObjects (listing);
                        else
                            break;
                    }
                    if(count > 0) {
                        consoleLogger.info(String.format("Successfully downloaded `%s` files from S3 bucket `%s` using prefix `%s`", count, bucketName, prefix));
                        return DefaultGoPluginApiResponse.success("");
                    }
                    else {
                        String message = String.format("No objects are matching prefix `%s` in S3 bucket `%s`", prefix, bucketName);
                        consoleLogger.error(message);
                        LOG.error(message);
                        return DefaultGoPluginApiResponse.badRequest(message);
                    }
                }
            }
            File outFile = getTargetFile(fetchConfig, workingDir, targetFile);
            consoleLogger.info(String.format("Retrieving file `%s` from S3 bucket `%s`.", s3InbucketPath, bucketName));
            LOG.info(String.format("Retrieving file `%s` from S3 bucket `%s`.", s3InbucketPath, bucketName));
            GetObjectRequest getRequest = new GetObjectRequest(bucketName, s3InbucketPath);
            s3.getObject(getRequest, outFile);

            consoleLogger.info(String.format("Source `%s` successfully pulled from S3 bucket `%s` to `%s`.", s3InbucketPath, bucketName, outFile));

            return DefaultGoPluginApiResponse.success("");
        } catch (Exception e) {
            final String message = format("Failed pull source file: %s", e);
            consoleLogger.error(message);
            LOG.error(message);
            return DefaultGoPluginApiResponse.error(message);
        }
    }

    private File getTargetFile(FetchArtifactConfig fetchConfig, String workingDir, String targetFile) {
        File outFile;
        if(Util.isBlank(fetchConfig.getDestination())) {
            outFile = new File(Paths.get(workingDir, targetFile).toString());
        }
        else {
            outFile = new File(Paths.get(workingDir, fetchConfig.getDestination(), targetFile).toString());
        }
        return outFile;
    }

    public void validateMetadata(Map<String, Object> artifactMap) {
        if (artifactMap == null) {
            throw new RuntimeException(String.format("Cannot fetch the source file from S3: Invalid metadata received from the GoCD server. The artifact metadata is null."));
        }

        for(String requiredKey : Arrays.asList("Source", "Destination", "IsFile")) {
            if (!artifactMap.containsKey(requiredKey)) {
                throw new RuntimeException(String.format("Cannot fetch the source file from S3: Invalid metadata received from the GoCD server. The artifact metadata must contain the key `%s`.", requiredKey));
            }
        }
    }
}
