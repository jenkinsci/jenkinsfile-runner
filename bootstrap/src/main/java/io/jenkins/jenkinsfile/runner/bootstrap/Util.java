package io.jenkins.jenkinsfile.runner.bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jenkins.tools.pluginmanager.config.Config;
import io.jenkins.tools.pluginmanager.impl.PluginManager;


public class Util {

    public static File explodeWar(String jarPath) throws IOException {
        JarFile jarfile = new JarFile(new File(jarPath));
        Enumeration<JarEntry> enu = jarfile.entries();

        // Get current working directory path
        Path currentPath = FileSystems.getDefault().getPath("").toAbsolutePath();
        //Create Temporary directory
        Path path = Files.createTempDirectory(currentPath.toAbsolutePath(), "jenkinsfile-runner");
        File destDir = path.toFile();

        while(enu.hasMoreElements()) {
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

    public interface PluginManagerConfigurator {
        void configure(Config.Builder configBuilder);
    }

    @NonNull
    public static PluginManager initPluginManager(File pluginsDir, PluginManagerConfigurator configurator) {
        Config.Builder configBuilder = Config.builder()
                //.withJenkinsWar(jenkinsWar.getAbsolutePath())
                .withPluginDir(pluginsDir)
                .withShowAvailableUpdates(true)
                .withIsVerbose(true)
                .withDoDownload(false);
        configurator.configure(configBuilder);
        Config config = configBuilder.build();

        PluginManager pluginManager = new PluginManager(config);
        //pluginManager.setCm(new CacheManager(cacheDir.toPath(), true));
        //pluginManager.setJenkinsVersion(new VersionNumber("2.222.1"));
        //pluginManager.setLatestUcJson(latestUcJson);
        //pluginManager.setLatestUcPlugins(latestUcJson.getJSONObject("plugins"));
        //pluginManager.setPluginInfoJson(pluginManager.getJson(pluginVersionsFile.toURI().toURL(), "plugin-versions"));
        return pluginManager;
    }
}
