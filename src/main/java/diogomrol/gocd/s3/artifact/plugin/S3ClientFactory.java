package diogomrol.gocd.s3.artifact.plugin;
import diogomrol.gocd.s3.artifact.plugin.model.ArtifactStoreConfig;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.apache.commons.lang3.StringUtils;

public class S3ClientFactory {
    private static final S3ClientFactory S3_CLIENT_FACTORY = new S3ClientFactory();

    public AmazonS3 s3(ArtifactStoreConfig artifactStoreConfig) throws SdkClientException {
        return createClient(artifactStoreConfig);
    }

    public static S3ClientFactory instance() {
        return S3_CLIENT_FACTORY;
    }

    private static AmazonS3 createClient(ArtifactStoreConfig artifactStoreConfig) throws SdkClientException {
        AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard();

        if (StringUtils.isNotBlank(artifactStoreConfig.getEndpointURL())) {
            s3ClientBuilder = s3ClientBuilder.withEndpointConfiguration(
                new EndpointConfiguration(artifactStoreConfig.getEndpointURL(), artifactStoreConfig.getRegion())
            );
        }
        else if (StringUtils.isNotBlank(artifactStoreConfig.getRegion())) {
            s3ClientBuilder = s3ClientBuilder.withRegion(Regions.fromName(artifactStoreConfig.getRegion()));
        }

        if (artifactStoreConfig.getPathStyleAccess()) {
            s3ClientBuilder.withPathStyleAccessEnabled(true);
        }

        if (StringUtils.isNotBlank(artifactStoreConfig.getAwsaccesskey()) && StringUtils.isNotBlank(artifactStoreConfig.getAwssecretaccesskey())) {
            BasicAWSCredentials awsCredentials = new BasicAWSCredentials(artifactStoreConfig.getAwsaccesskey(), artifactStoreConfig.getAwssecretaccesskey());
            s3ClientBuilder = s3ClientBuilder.withCredentials(new AWSStaticCredentialsProvider(awsCredentials));
        }

        return s3ClientBuilder.build();
    }
}
