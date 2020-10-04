/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.jenkinsfile.runner.util;

import hudson.FilePath;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Controls how <tt>JENKINS_HOME</tt> is initialized.
 *
 * @author Kohsuke Kawaguchi
 */
public interface JenkinsHomeLoader {
    /** 
     * Returns a directory to be used as <tt>JENKINS_HOME</tt>
     *
     * @throws Exception
     *      to cause a test to fail.
     */
    File allocate() throws Exception;

    /**
     * Allocates a new empty directory, meaning this will emulate the fresh Jenkins installation.
     */
    JenkinsHomeLoader NEW = new JenkinsHomeLoader() {
        public File allocate() throws IOException {
            return ExecutionEnvironment.get().temporaryDirectoryAllocator.allocate();
        }
    };

    /**
     * Allocates a new directory by copying from an existing directory, or unzipping from a zip file.
     */
    final class CopyExisting implements JenkinsHomeLoader {
        private final URL source;

        /**
         * Either a zip file or a directory that contains the home image.
         */
        public CopyExisting(File source) throws MalformedURLException {
            this(source.toURI().toURL());
        }

        /**
         * Extracts from a zip file in the resource.
         *
         * <p>
         * This is useful in case you want to have a test data in the resources.
         * Only file URL is supported. 
         */
        public CopyExisting(URL source) {
            this.source = source;
        }

        public File allocate() throws Exception {
            File target = NEW.allocate();
            if(source.getProtocol().equals("file")) {
                File src = new File(source.toURI());
                if(src.isDirectory())
                    new FilePath(src).copyRecursiveTo("**/*",new FilePath(target));
                else
                if(src.getName().endsWith(".zip"))
                    new FilePath(src).unzip(new FilePath(target));
            } else {
                File tmp = File.createTempFile("hudson","zip");
                try {
                    FileUtils.copyURLToFile(source,tmp);
                    new FilePath(tmp).unzip(new FilePath(target));
                } finally {
                    tmp.delete();
                }
            }
            return target;
        }
    }

    /**
     * Does not allocate a new directory but uses the specified one.
     * Since TemporaryDirectoryAllocator is not used this folder will not be affected by the dispose() at the end.
     */
    final class UseExisting implements JenkinsHomeLoader {
        private final File source;

        public UseExisting(File source) {
            this.source = source;
        }

        public File allocate() throws Exception {
            return source;
        }
    }

    /**
     * Allocates a new directory by copying from a test resource
     */
    final class Local implements JenkinsHomeLoader {
        private final Method testMethod;
        private final String alterName;

        public Local(Method testMethod, String alterName) {
            this.testMethod = testMethod;
            this.alterName = alterName;
        }

        public File allocate() throws Exception {
            URL res = findDataResource();
            if(!res.getProtocol().equals("file"))
                throw new AssertionError("Test data is not available in the file system: "+res);
            File home = new File(res.toURI());
            System.err.println("Loading $JENKINS_HOME from " + home);

            return new CopyExisting(home).allocate();
        }

        public static boolean isJavaIdentifier(@CheckForNull String name) {
            if (StringUtils.isEmpty(name)) {
                return false;
            }
            if (!Character.isJavaIdentifierStart(name.charAt(0))) {
                return false;
            }
            for (int pos = 1; pos < name.length(); ++pos) {
                if (!Character.isJavaIdentifierPart(name.charAt(pos))) {
                    return false;
                }
            }
            return true;
        }

        private URL findDataResource() {
            // first, check method specific resource
            Class<?> clazz = testMethod.getDeclaringClass();
            String methodName = testMethod.getName();
            
            if (isJavaIdentifier(alterName)) {
                methodName = alterName;
            }
            
            for( String middle : new String[]{ '/'+methodName, "" }) {
                for( String suffix : SUFFIXES ) {
                    URL res = clazz.getResource(clazz.getSimpleName() + middle+suffix);
                    if(res!=null)   return res;
                }
            }

            throw new AssertionError("No test resource was found for "+testMethod);
        }

        private static final String[] SUFFIXES = {"/", ".zip"};
    }
}
