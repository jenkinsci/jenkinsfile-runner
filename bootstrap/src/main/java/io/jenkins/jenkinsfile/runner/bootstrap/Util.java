package io.jenkins.jenkinsfile.runner.bootstrap;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.util.VersionNumber;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class Util {

    @CheckForNull
    private static Properties JFR_PROPERTIES = null;

    public static class VersionProviderImpl implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() throws Exception {
            return new String[] { getJenkinsfileRunnerVersion() };
        }
    }

    public static String getJenkinsfileRunnerVersion() throws IOException {
        return readJenkinsPomProperty("jfr.version");
    }

    public static String getMininumJenkinsVersion() throws IOException {
        return readJenkinsPomProperty("minimum.jenkins.version");
    }

    public static boolean isJenkinsVersionSupported(String version) throws IOException {
        return new VersionNumber(version).isNewerThanOrEqualTo(new VersionNumber(getMininumJenkinsVersion()));
    }

    public static String readJenkinsPomProperty(String key) throws IOException {
        if (JFR_PROPERTIES != null) {
            return JFR_PROPERTIES.getProperty(key);
        }
        try (InputStream pomProperties = Bootstrap.class.getResourceAsStream("/jfr.properties")) {
            if (pomProperties == null) {
                throw new IOException("Cannot find the Jenkinsfile Runner version properties file: /jfr.properties");
            }
            Properties props = new Properties();
            props.load(pomProperties);
            JFR_PROPERTIES = props;
            return props.getProperty(key);
        }
    }

    public static File explodeWar(String warPath) throws IOException {
        return explodeWar(new File(warPath));
    }

    public static File explodeWar(File warFile) throws IOException {
        try (JarFile jarfile = new JarFile(warFile)) {
            Enumeration<JarEntry> enu = jarfile.entries();

            // Get current working directory path
            Path currentPath = FileSystems.getDefault().getPath("").toAbsolutePath();
            //Create Temporary directory
            Path destPath = Files.createTempDirectory(currentPath.toAbsolutePath(), "jenkinsfile-runner");
            File destDir = destPath.toFile();
            // Canonicalize once so every entry can be checked against the real destination root,
            // guarding against a maliciously crafted WAR escaping it via "../" or absolute paths
            // (a.k.a. "Zip Slip", CWE-22).
            Path destDirReal = destPath.toRealPath();

            while (enu.hasMoreElements()) {
                JarEntry je = enu.nextElement();

                Path resolved = destDirReal.resolve(je.getName()).normalize();
                if (!resolved.startsWith(destDirReal)) {
                    throw new IOException(
                            "Entry " + je.getName() + " in " + warFile
                                    + " would be extracted outside of the target directory. Aborting.");
                }

                if (je.isDirectory()) {
                    Files.createDirectories(resolved);
                    continue;
                }

                Files.createDirectories(resolved.getParent());
                try (InputStream is = jarfile.getInputStream(je)) {
                    Files.copy(is, resolved, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return destDir;
        }
    }
}