package diogomrol.gocd.s3.artifact.plugin.model;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
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
    public void shouldReturnTrueForEmptyDirectoriesCheckIfScanResultHasDirectoriesAndNoFiles() {
        String[] directories = new String[2];
        directories[0] = "dir";
        directories[1] = "dir/sub-dir-a";
        ScanResult scanResult = new ScanResult(directories, Collections.emptyList());

        assertThat(scanResult.hasEmptyDirectories()).isTrue();
    }

    @Test
    public void shouldReturnFalseForEmptyDirectoriesCheckIfScanResultHasNoDirectories() {
        String[] directories = new String[0];
        ScanResult scanResult = new ScanResult(directories, Collections.emptyList());

        assertThat(scanResult.hasEmptyDirectories()).isFalse();
    }

    @Test
    public void shouldReturnFalseForEmptyDirectoriesCheckIfScanResultHasNoDirectoriesAndNoFiles() {
        ScanResult scanResult = new ScanResult(new String[0], Collections.emptyList());

        assertThat(scanResult.hasEmptyDirectories()).isFalse();
    }

    @Test
    public void shouldReturnFalseForEmptyDirectoriesCheckIfScanResultHasNoDirectoriesButHasFiles() {
        String[] directories = new String[0];
        List<File> files = new ArrayList<>();
        files.add(new File("tmpFile"));
        ScanResult scanResult = new ScanResult(directories, files);

        assertThat(scanResult.hasEmptyDirectories()).isFalse();
    }
}