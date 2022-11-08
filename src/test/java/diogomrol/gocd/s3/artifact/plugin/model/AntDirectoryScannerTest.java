package diogomrol.gocd.s3.artifact.plugin.model;

import com.amazonaws.SdkClientException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
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
        ScanResult scanResult = scanner.getFilesMatchingPattern(workingDir, "test.bin");
        assertThat(scanResult.getMatchingFiles())
                .hasSize(1)
                .contains(test);
        assertThat(scanResult.getDirectories())
                .isEmpty();
    }
    @Test
    public void shouldReturnEmptyIfFileNotPresent() {
        ScanResult scanResult = scanner.getFilesMatchingPattern(workingDir, "test.bin");
        assertThat(scanResult.getMatchingFiles())
                .isEmpty();
        assertThat(scanResult.getDirectories())
                .isEmpty();
    }
    @Test
    public void shouldReturnSubDirectoriesEvenIfTheyAreEmpty() {
        createDir("out");
        createDir("out/sub-dir-A");
        createDir("out/sub-dir-B");
        ScanResult scanResult = scanner.getFilesMatchingPattern(workingDir, "out");
        assertThat(scanResult.getMatchingFiles()).
                isEmpty();
        String[] scannedDirs = scanResult.getDirectories();
        assertThat(scannedDirs)
                .hasSize(3);
        assertThat(scannedDirs[0]).isEqualTo("out");
        assertThat(scannedDirs[1]).isEqualTo("out/sub-dir-A");
        assertThat(scannedDirs[2]).isEqualTo("out/sub-dir-B");

    }
    @Test
    public void shouldMatchFileInSubdirectory() throws IOException {
        File test = createFile("out/test.bin");
        ScanResult scanResult = scanner.getFilesMatchingPattern(workingDir, "out");
        assertThat(scanResult.getMatchingFiles())
                .hasSize(1)
                .contains(test);
        String[] scannedDirs = scanResult.getDirectories();
        assertThat(scannedDirs)
                .hasSize(1);
        assertThat(scannedDirs[0]).isEqualTo("out");
    }
    @Test
    public void shouldNotIncludeSameFileTwiceWhenMatches2Rules() throws IOException {
        File test = createFile("out/test.bin");
        ScanResult scanResult = scanner.getFilesMatchingPattern(workingDir, "out,out/*.bin");
        assertThat(scanResult.getMatchingFiles())
                .hasSize(1)
                .contains(test);
        String[] scannedDirs = scanResult.getDirectories();
        assertThat(scannedDirs)
                .hasSize(1);
        assertThat(scannedDirs[0]).isEqualTo("out");
    }
    @Test
    public void shouldListFilesRecursivelyInSubFoldersForPatternWithoutTrailingSlash() throws IOException {
        File test = createFile("out/test.bin");
        File testA = createFile("out/dir-a/test-a.bin");
        File testB1 = createFile("out/dir-a/dir-b/test-b1.bin");
        File testB2 = createFile("out/dir-a/dir-b/test-b2.bin");
        File testC = createFile("out/dir-a/dir-b/dir-c/test-c.bin");
        ScanResult scanResult = scanner.getFilesMatchingPattern(workingDir, "out");
        assertThat(scanResult.getMatchingFiles())
                .hasSize(5)
                .contains(test)
                .contains(testA)
                .contains(testB1)
                .contains(testB2)
                .contains(testC);
        String[] scannedDirs = scanResult.getDirectories();
        assertThat(scannedDirs)
                .hasSize(4);
        assertThat(scannedDirs[0]).isEqualTo("out");
        assertThat(scannedDirs[1]).isEqualTo("out/dir-a");
        assertThat(scannedDirs[2]).isEqualTo("out/dir-a/dir-b");
        assertThat(scannedDirs[3]).isEqualTo("out/dir-a/dir-b/dir-c");
    }
    @Test
    public void shouldListFilesRecursivelyInSubFolders() throws IOException {
        File test = createFile("out/test.bin");
        File testA = createFile("out/dir-a/test-a.bin");
        File testB1 = createFile("out/dir-a/dir-b/test-b1.bin");
        File testB2 = createFile("out/dir-a/dir-b/test-b2.bin");
        File testC = createFile("out/dir-a/dir-b/dir-c/test-c.bin");
        ScanResult scanResult = scanner.getFilesMatchingPattern(workingDir, "out/");
        assertThat(scanResult.getMatchingFiles())
                .hasSize(5)
                .contains(test)
                .contains(testA)
                .contains(testB1)
                .contains(testB2)
                .contains(testC);
        String[] scannedDirs = scanResult.getDirectories();
        assertThat(scannedDirs)
                .hasSize(4);
        assertThat(scannedDirs[0]).isEqualTo("out");
        assertThat(scannedDirs[1]).isEqualTo("out/dir-a");
        assertThat(scannedDirs[2]).isEqualTo("out/dir-a/dir-b");
        assertThat(scannedDirs[3]).isEqualTo("out/dir-a/dir-b/dir-c");
    }
    @Test
    public void shouldListFilesRecursivelyInSubFoldersForGlobPattern() throws IOException {
        File nonMatchingFile = createFile("outlet/test.bin");
        File test = createFile("out/test.bin");
        File testA = createFile("out/dir-a/test-a.bin");
        File testB1 = createFile("out/dir-a/dir-b/test-b1.bin");
        File testB2 = createFile("out/dir-a/dir-b/test-b2.bin");
        File testC = createFile("out/dir-a/dir-b/dir-c/test-c.bin");
        ScanResult scanResult = scanner.getFilesMatchingPattern(workingDir, "out/dir-a/**/*");
        assertThat(scanResult.getMatchingFiles())
                .hasSize(4)
                .contains(testA)
                .contains(testB1)
                .contains(testB2)
                .contains(testC);
        String[] scannedDirs = scanResult.getDirectories();
        assertThat(scannedDirs)
                .hasSize(2);
        assertThat(scannedDirs[0]).isEqualTo("out/dir-a/dir-b");
        assertThat(scannedDirs[1]).isEqualTo("out/dir-a/dir-b/dir-c");
    }
    @Test
    public void shouldListFilesRecursivelyInSubFoldersForWildcardByScanningTheInputAfterStrippingTheWildcard() throws IOException {
        File nonMatchingFile = createFile("outlet/test.bin");
        File test = createFile("out/test.bin");
        File testA = createFile("out/dir-a/test-a.bin");
        File testB1 = createFile("out/dir-a/dir-b/test-b1.bin");
        File testB2 = createFile("out/dir-a/dir-b/test-b2.bin");
        File testC = createFile("out/dir-a/dir-b/dir-c/test-c.bin");
        ScanResult scanResult = scanner.getFilesMatchingPattern(workingDir, "out/*");
        assertThat(scanResult.getMatchingFiles())
                .hasSize(5)
                .contains(test)
                .contains(testA)
                .contains(testB1)
                .contains(testB2)
                .contains(testC);
        String[] scannedDirs = scanResult.getDirectories();
        assertThat(scannedDirs)
                .hasSize(4);
        assertThat(scannedDirs[0]).isEqualTo("out");
        assertThat(scannedDirs[1]).isEqualTo("out/dir-a");
        assertThat(scannedDirs[2]).isEqualTo("out/dir-a/dir-b");
        assertThat(scannedDirs[3]).isEqualTo("out/dir-a/dir-b/dir-c");
    }
    @Test
    public void shouldListFilesWithPatternRecursivelyInSubFoldersForGlobPattern() throws IOException {
        File test = createFile("out/test.bin");
        File testA = createFile("out/dir-a/test-a.bin");
        File testB1 = createFile("out/dir-a/dir-b/test-b1.bin");
        File testB2 = createFile("out/dir-a/dir-b/test-b2.bin");
        File testC = createFile("out/dir-a/dir-b/dir-c/test-c.bin");
        File testD = createFile("out/dir-d/test-d.txt");
        ScanResult scanResult = scanner.getFilesMatchingPattern(workingDir, "out/**/*.bin");
        assertThat(scanResult.getMatchingFiles())
                .hasSize(5)
                .contains(test)
                .contains(testA)
                .contains(testB1)
                .contains(testB2)
                .contains(testC);
        String[] scannedDirs = scanResult.getDirectories();
        assertThat(scannedDirs)
                .isEmpty();
    }

    private File createFile(String path) throws IOException {
        Path filepath = Paths.get(workingDir.toPath().toAbsolutePath().toString(), path);
        filepath.getParent().toFile().mkdir();
        Files.write(filepath, "".getBytes());
        return new File(path);
    }

    private boolean createDir(String path) {
        Path filepath = Paths.get(workingDir.toPath().toAbsolutePath().toString(), path);
        return filepath.toFile().mkdir();
    }

}
