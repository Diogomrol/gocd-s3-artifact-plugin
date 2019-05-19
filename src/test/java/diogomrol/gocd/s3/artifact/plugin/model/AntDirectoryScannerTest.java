package diogomrol.gocd.s3.artifact.plugin.model;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import diogomrol.gocd.s3.artifact.plugin.ConsoleLogger;
import diogomrol.gocd.s3.artifact.plugin.S3ClientFactory;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class AntDirectoryScannerTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    private File workingDir;
    private AntDirectoryScanner scanner;

    @Before
    public void setUp() throws IOException, SdkClientException {
        initMocks(this);
        workingDir = tmpFolder.newFolder("test-tmp");
        scanner = new AntDirectoryScanner();
    }

    @Test
    public void shouldMatchFileByName() throws IOException {
        File test = createFile("test.bin");
        List<File> files = scanner.getFilesMatchingPattern(workingDir, "test.bin");
        assertThat(files)
                .hasSize(1)
                .contains(test);
    }
    @Test
    public void shouldMatchFileInSubdirectory() throws IOException {
        File test = createFile("out/test.bin");
        List<File> files = scanner.getFilesMatchingPattern(workingDir, "out");
        assertThat(files)
                .hasSize(1)
                .contains(test);
    }
    @Test
    public void shouldNotIncludeSameFileTwiceWhenMatches2Rules() throws IOException {
        File test = createFile("out/test.bin");
        List<File> files = scanner.getFilesMatchingPattern(workingDir, "out,out/*.bin");
        assertThat(files)
                .hasSize(1)
                .contains(test);
    }


    private File createFile(String path) throws IOException {
        Path filepath = Paths.get(workingDir.toPath().toAbsolutePath().toString(), path);
        filepath.getParent().toFile().mkdir();
        Files.write(filepath, "".getBytes());
        return new File(path);
    }

}
