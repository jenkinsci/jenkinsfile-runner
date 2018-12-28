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


import org.junit.runner.Description;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * @author Kohsuke Kawaguchi
 */
public class TestEnvironment extends ExecutionEnvironment {

    /**
     * Current test case being run (null for a JUnit 3 test).
     */
    private final @CheckForNull Description description;

    public TestEnvironment(@Nonnull Description description) {
        this.description = description;
    }

    /**
     * Current test case being run (works for JUnit 3 or 4).
     * Warning: {@link Description#getTestClass} is currently broken in some environments (claimed fixed in JUnit 4.11). Use {@link Description#getClassName} instead.
     */
    public @Nonnull Description description() {
        return description;
    }

    @Override
    public String displayName() {
        return description.getDisplayName();
    }
}
