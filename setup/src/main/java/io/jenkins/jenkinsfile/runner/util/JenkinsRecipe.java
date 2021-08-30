package io.jenkins.jenkinsfile.runner.util;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.lang.annotation.Annotation;
import java.io.File;

import io.jenkins.jenkinsfile.runner.JenkinsEmbedder;


/**
 * Meta-annotation for recipe annotations, which controls
 * the execution environment set up.
 *
 * @author Kohsuke Kawaguchi
 * @since 1.436
 */
@Retention(RUNTIME)
@Documented
@Target(ANNOTATION_TYPE)
public @interface JenkinsRecipe {
    /**
     * Specifies the class that sets up the test environment.
     *
     * <p>
     * When a recipe annotation is placed on a test method,
     */
    Class<? extends Runner> value();

    /**
     * The code that implements the recipe semantics.
     *
     * @param <T>
     *      The recipe annotation associated with this runner.
     */
    abstract class Runner<T extends Annotation> {
        /**
         * Called to prepare the execution environment.
         */
        public void setup(JenkinsEmbedder embedder, T recipe) throws Exception {}

        /**
         * Called right before {@code jenkins.model.Jenkins#Jennkins(java.io.File, javax.servlet.ServletContext)} is invoked
         * to decorate the hudson home directory.
         */
        public void decorateHome(JenkinsEmbedder embedder, File home) throws Exception {}

        /**
         * Called to shut down the execution environment.
         */
        public void tearDown(JenkinsEmbedder embedder, T recipe) throws Exception {}
    }
}
