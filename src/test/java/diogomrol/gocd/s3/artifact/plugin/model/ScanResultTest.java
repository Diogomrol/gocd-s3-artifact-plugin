package diogomrol.gocd.s3.artifact.plugin.model;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanResultTest {

    @Test
    public void shouldReturnTrueForEmptyCheckIfScanResultHasEmptyDirectoriesAndFiles() {
        ScanResult scanResult = new ScanResult(new String[0], Collections.emptyList());

        assertThat(scanResult.isEmpty()).isTrue();
    }

    @Test
    public void shouldReturnFalseForEmptyCheckIfScanResultHasDirectories() {
        String[] directories = new String[1];
        directories[0] = "dir";
        ScanResult scanResult = new ScanResult(directories, Collections.emptyList());

        assertThat(scanResult.isEmpty()).isFalse();
    }

    @Test
    public void shouldReturnFalseForEmptyCheckIfScanResultHasMatchingFiles() {
        List<File> files = new ArrayList<>();
        files.add(new File("tmpFile"));
        ScanResult scanResult = new ScanResult(new String[0], files);

        assertThat(scanResult.isEmpty()).isFalse();
    }

    @Test
    public void shouldReturnFalseForEmptyCheckIfScanResultHasDirectoriesAndMatchingFiles() {
        String[] directories = new String[1];
        directories[0] = "dir";
        List<File> files = new ArrayList<>();
        files.add(new File("tmpFile"));
        ScanResult scanResult = new ScanResult(directories, files);

        assertThat(scanResult.isEmpty()).isFalse();
    }

    @Test
    public void shouldReturnTrueIfThereAreDirectories() {
        String[] directories = new String[2];
        directories[0] = "dir";
        directories[1] = "dir/sub-dir-a";
        ScanResult scanResultWithDirsAndFiles = new ScanResult(directories, Collections.singletonList(new File("fileName")));
        ScanResult scanResultWithDirsAndWithoutFiles = new ScanResult(directories, Collections.emptyList());

        assertThat(scanResultWithDirsAndFiles.hasDirectories()).isTrue();
        assertThat(scanResultWithDirsAndWithoutFiles.hasDirectories()).isTrue();
    }

    @Test
    public void shouldReturnFalseIfThereIsNoDirectory() {
        String[] directories = new String[0];
        ScanResult scanResultWithoutDirsAndWithFiles = new ScanResult(directories, Collections.singletonList(new File("fileName")));
        ScanResult scanResultWithoutDirsAndFiles = new ScanResult(directories, Collections.emptyList());

        assertThat(scanResultWithoutDirsAndWithFiles.hasDirectories()).isFalse();
        assertThat(scanResultWithoutDirsAndFiles.hasDirectories()).isFalse();
    }

    @Test
    public void shouldReturnFalseIfThereAreNoFiles() {
        String[] directories = new String[2];
        directories[0] = "dir";
        ScanResult scanResultWithoutFilesAndWithDirs = new ScanResult(directories, Collections.emptyList());
        ScanResult scanResultWithoutFilesAndDirs = new ScanResult(new String[0], Collections.emptyList());

        assertThat(scanResultWithoutFilesAndWithDirs.hasFiles()).isFalse();
        assertThat(scanResultWithoutFilesAndDirs.hasFiles()).isFalse();
    }

    @Test
    public void shouldReturnTrueIfThereAreFiles() {
        String[] directories = new String[0];
        List<File> files = new ArrayList<>();
        files.add(new File("tmpFile"));
        ScanResult scanResultWithFilesAndDirs = new ScanResult(directories, files);
        ScanResult scanResultWithFilesAndWithoutDirs = new ScanResult(new String[0], files);

        assertThat(scanResultWithFilesAndDirs.hasFiles()).isTrue();
        assertThat(scanResultWithFilesAndWithoutDirs.hasFiles()).isTrue();
    }
}