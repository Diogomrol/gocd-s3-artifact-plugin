package diogomrol.gocd.s3.artifact.plugin.model;

import java.io.File;
import java.util.List;

public class ScanResult {
    private final String[] directories;
    private final List<File> matchingFiles;

    public ScanResult(String[] directories, List<File> matchingFiles) {
        this.directories = directories;
        this.matchingFiles = matchingFiles;
    }

    public String[] getDirectories() {
        return directories;
    }

    public List<File> getMatchingFiles() {
        return matchingFiles;
    }

    public boolean hasDirectories() {
        return directories.length > 0;
    }

    public boolean hasFiles() {
        return !matchingFiles.isEmpty();
    }

    public boolean isEmpty() {
        return !hasDirectories() && !hasFiles();
    }
}
