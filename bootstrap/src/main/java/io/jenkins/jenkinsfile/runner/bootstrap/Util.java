package io.jenkins.jenkinsfile.runner.bootstrap;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.util.VersionNumber;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public static File explodeWar(String jarPath) throws IOException {
        try (JarFile jarfile = new JarFile(new File(jarPath))) {
            Enumeration<JarEntry> enu = jarfile.entries();

            // Get current working directory path
            Path currentPath = FileSystems.getDefault().getPath("").toAbsolutePath();
            //Create Temporary directory
            Path path = Files.createTempDirectory(currentPath.toAbsolutePath(), "jenkinsfile-runner");
            File destDir = path.toFile();

            while (enu.hasMoreElements()) {
                JarEntry je = enu.nextElement();
                File file = new File(destDir, je.getName());
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file = new File(destDir, je.getName());
                }
                if (je.isDirectory()) {
                    continue;
                }
                InputStream is = jarfile.getInputStream(je);

                try (FileOutputStream fo = new FileOutputStream(file)) {
                    while (is.available() > 0) {
                        fo.write(is.read());
                    }
                    fo.close();
                    is.close();
                }
            }
            return destDir;
        }
    }
}
