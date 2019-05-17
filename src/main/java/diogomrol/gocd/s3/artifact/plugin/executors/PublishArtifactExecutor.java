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

import diogomrol.gocd.s3.artifact.plugin.ConsoleLogger;
import diogomrol.gocd.s3.artifact.plugin.S3ClientFactory;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import diogomrol.gocd.s3.artifact.plugin.model.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static diogomrol.gocd.s3.artifact.plugin.S3ArtifactPlugin.LOG;
import static diogomrol.gocd.s3.artifact.plugin.utils.Util.normalizePath;

public class PublishArtifactExecutor implements RequestExecutor {
    private final PublishArtifactRequest publishArtifactRequest;
    private final PublishArtifactResponse publishArtifactResponse;
    private final ConsoleLogger consoleLogger;
    private final S3ClientFactory clientFactory;
    private AntDirectoryScanner scanner;

    public PublishArtifactExecutor(GoPluginApiRequest request, ConsoleLogger consoleLogger) {
        this(request, consoleLogger, S3ClientFactory.instance());
    }

    PublishArtifactExecutor(GoPluginApiRequest request, ConsoleLogger consoleLogger, S3ClientFactory clientFactory) {
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
            final AmazonS3 s3 = clientFactory.s3(artifactStoreConfig);
            final String sourcePattern = artifactPlan.getArtifactPlanConfig().getSource();
            String destinationFolder = artifactPlan.getArtifactPlanConfig().getDestination();
            EnvironmentVariableResolver envResolver = new EnvironmentVariableResolver(destinationFolder, "Destination");
            destinationFolder = envResolver.resolve(publishArtifactRequest.getEnvironmentVariables());
            final String s3bucket = artifactStoreConfig.getS3bucket();
            final String workingDir = publishArtifactRequest.getAgentWorkingDir();
            String s3InbucketPath;
            if(!destinationFolder.isEmpty()) {
                s3InbucketPath = normalizePath(Paths.get(destinationFolder));
            }
            else {
                s3InbucketPath = "";
            }

            List<File> matchingFiles = scanner.getFilesMatchingPattern(new File(workingDir), sourcePattern);
            if(matchingFiles.size() == 0) {
                String noFilesMsg = String.format("No files are matching pattern: %s", sourcePattern);
                consoleLogger.error(noFilesMsg);
                LOG.warn(noFilesMsg);
                //TODO: tomzo consider handling no artifacts failure in GoCD core
                return DefaultGoPluginApiResponse.badRequest(noFilesMsg);
            }
            else if(matchingFiles.size() == 1) {
                File sourceFile = matchingFiles.get(0);
                String s3Key = normalizePath(Paths.get(s3InbucketPath, sourceFile.toPath().getFileName().toString()));
                PutObjectRequest request = new PutObjectRequest(s3bucket, s3Key, new File(Paths.get(workingDir, sourceFile.toString()).toString()));
                ObjectMetadata metadata = new ObjectMetadata();
                request.setMetadata(metadata);
                s3.putObject(request);
                publishArtifactResponse.addMetadata("Source", sourceFile.toString());
                publishArtifactResponse.addMetadata("IsFile", true);
                consoleLogger.info(String.format("Source file `%s` successfully pushed to S3 bucket `%s`.", sourceFile, artifactStoreConfig.getS3bucket()));
            }
            else {
                // upload many files
                for(File sourceFile : matchingFiles) {
                    String s3Key = normalizePath(Paths.get(s3InbucketPath, sourceFile.getPath()));
                    PutObjectRequest request = new PutObjectRequest(s3bucket, s3Key, new File(Paths.get(workingDir, sourceFile.toString()).toString()));
                    ObjectMetadata metadata = new ObjectMetadata();
                    request.setMetadata(metadata);
                    s3.putObject(request);
                    consoleLogger.info(String.format("Source file `%s` successfully pushed to S3 bucket `%s`.", sourceFile, artifactStoreConfig.getS3bucket()));
                }
                publishArtifactResponse.addMetadata("Source", sourcePattern);
                publishArtifactResponse.addMetadata("IsFile", false);
            }
            publishArtifactResponse.addMetadata("Destination", s3InbucketPath);

            return DefaultGoPluginApiResponse.success(publishArtifactResponse.toJSON());
        } catch (Exception e) {
            consoleLogger.error(String.format("Failed to publish %s: %s", artifactPlan, e));
            LOG.error(String.format("Failed to publish %s: %s", artifactPlan, e.getMessage()), e);
            return DefaultGoPluginApiResponse.error(String.format("Failed to publish %s: %s", artifactPlan, e.getMessage()));
        }
    }
}
