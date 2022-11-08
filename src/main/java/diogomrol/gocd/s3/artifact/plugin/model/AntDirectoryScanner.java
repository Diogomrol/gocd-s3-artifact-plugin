package diogomrol.gocd.s3.artifact.plugin.model;

import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AntDirectoryScanner {

    public ScanResult getFilesMatchingPattern(File baseDir, String pattern) {
        String[] inputPatterns = pattern.trim().split(" *, *");
        String[] parsedPatterns = getParsedPatterns(baseDir, inputPatterns);

        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(baseDir);
        scanner.setIncludes(parsedPatterns);
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

        for (String allPath : allPaths) {
            File file = new File(allPath);
            if ((!allFiles.contains(file)) && (!file.isDirectory())) {
                allFiles.add(new File(allPath));
            }
        }
        return new ScanResult(directories, allFiles);
    }

    private String[] getParsedPatterns(File baseDir, String[] inputPatterns) {
        String[] parsedPatterns = new String[inputPatterns.length];
        for (int i = 0; i < inputPatterns.length; i++) {
            String inputPattern = inputPatterns[i];
            File inputFile = new File(baseDir, inputPattern);
            if (inputFile.isDirectory()) {
                inputPattern += inputPattern.charAt(inputPattern.length() - 1) != File.separatorChar ? File.separator : "";
            }
            if (inputPattern.matches("\\w+\\/\\*$")) {
                inputPattern = inputPattern.substring(0, inputPattern.length() - 1);
            }
            parsedPatterns[i] = inputPattern;
        }
        return parsedPatterns;
    }
}
