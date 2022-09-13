package diogomrol.gocd.s3.artifact.plugin.model;

import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AntDirectoryScanner {

    public List<File> getFilesMatchingPattern(File baseDir, String pattern) {
        File inputFile = new File(baseDir, pattern);
        if (inputFile.isDirectory()) {
            pattern += pattern.charAt(pattern.length() - 1) != File.separatorChar ? File.separator : "";
        }

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(baseDir);
        scanner.setIncludes(pattern.trim().split(" *, *"));
        scanner.scan();

        String[] allPaths = scanner.getIncludedFiles();
        List<File> allFiles = new ArrayList<>();
        String[] directories = scanner.getIncludedDirectories();
        for (String directory : directories) {
            File[] files = new File(baseDir, directory).listFiles();
            if(files != null) {
                for(File f : files) {
                    if (!f.isDirectory()) {
                        allFiles.add(new File(directory, f.getName()));
                    }
                }
            }
        }

        for (int i = 0; i < allPaths.length; i++) {
            File file = new File(allPaths[i]);
            if ((!allFiles.contains(file)) && (!file.isDirectory())) {
                allFiles.add(new File(allPaths[i]));
            }
        }
        return allFiles;
    }
}
