package spacesheepgames.gocd.gcs.artifact.plugin.model;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

public class AntDirectoryScannerTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    private File workingDir;
    private AntDirectoryScanner scanner;

    @Before
    public void setUp() throws IOException {
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
