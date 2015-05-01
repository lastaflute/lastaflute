/*
 * Copyright 2014-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.dbflute.lastaflute.core.manage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import org.dbflute.utflute.core.PlainTestCase;

/**
 * @author jflute
 */
public class ManageLicenseTest extends PlainTestCase {

    private static final String COPYRIGHT = "Copyright 2014-2015 the original author or authors.";

    // ===================================================================================
    //                                                                      License Header
    //                                                                      ==============
    public void test_license_header_main() {
        // ## Arrange ##
        String srcPathMark = "src/main/java";
        File buildDir = getBuildDir(getClass());
        String buildPath = getCanonicalPath(buildDir);
        File srcDir = new File(buildPath + "/../../" + srcPathMark);
        assertTrue(srcDir.exists());
        List<File> unlicensedList = new ArrayList<File>();

        // ## Act ##
        checkUnlicensed(srcDir, unlicensedList);

        // ## Assert ##
        StringBuilder sb = new StringBuilder();
        for (File unlicensedFile : unlicensedList) {
            String path = unlicensedFile.getPath().replace("\\", "/");
            final String rear;
            final String splitToken = srcPathMark + "/";
            if (path.contains(splitToken)) {
                rear = path.substring(path.indexOf(splitToken) + splitToken.length());
            } else {
                rear = path;
            }
            sb.append(ln()).append(rear);
        }
        sb.append(ln()).append(" count: ").append(unlicensedList.size());
        log(sb.toString());
        assertTrue(unlicensedList.isEmpty());
    }

    // ===================================================================================
    //                                                                             Checker
    //                                                                             =======
    protected void checkUnlicensed(File currentFile, List<File> unlicensedList) {
        if (isPackageDir(currentFile)) {
            File[] subFiles = currentFile.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return isPackageDir(file) || isSourceFile(file);
                }
            });
            if (subFiles == null || subFiles.length == 0) {
                return;
            }
            for (File subFile : subFiles) {
                checkUnlicensed(subFile, unlicensedList);
            }
        } else if (isSourceFile(currentFile)) {
            doCheckUnlicensed(currentFile, unlicensedList);
        } else { // no way
            throw new IllegalStateException("Unknown file: " + currentFile);
        }
    }

    protected boolean isPackageDir(File file) {
        return file.isDirectory() && !file.getName().startsWith(".");
    }

    protected boolean isSourceFile(File file) {
        return file.getName().endsWith(".java");
    }

    protected void doCheckUnlicensed(File srcFile, List<File> unlicensedList) {
        if (srcFile == null) {
            String msg = "The argument 'targetFile' should not be null.";
            throw new IllegalArgumentException(msg);
        }
        if (!srcFile.isFile()) {
            String msg = "The argument 'targetFile' should be file: " + srcFile;
            throw new IllegalArgumentException(msg);
        }
        BufferedReader br = null;
        boolean contains = false;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(srcFile), "UTF-8"));
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                if (line.contains(COPYRIGHT)) {
                    contains = true;
                    break;
                }
            }
        } catch (IOException e) {
            String msg = "Failed to read the file: " + srcFile;
            throw new IllegalStateException(msg, e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {}
            }
        }
        if (!contains) {
            unlicensedList.add(srcFile);
        }
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected String getResourcePath(String path, String extension) { // from DBFlute DfResourceUtil
        if (extension == null) {
            return path;
        }
        extension = "." + extension;
        if (path.endsWith(extension)) {
            return path;
        }
        return path.replace('.', '/') + extension;
    }

    protected String getResourcePath(Class<?> clazz) {
        return clazz.getName().replace('.', '/') + ".class";
    }

    protected File getBuildDir(Class<?> clazz) {
        return getBuildDir(getResourcePath(clazz));
    }

    protected File getBuildDir(String path) {
        File dir = null;
        URL url = getResourceUrl(path);
        if ("file".equals(url.getProtocol())) {
            int num = path.split("/").length;
            dir = new File(getFileName(url));
            for (int i = 0; i < num;) {
                i++;
                dir = dir.getParentFile();
            }
        } else {
            dir = new File(toJarFilePath(url));
        }
        return dir;
    }

    protected URL getResourceUrl(String path) {
        return getResourceUrl(path, null);
    }

    protected URL getResourceUrl(String path, String extension) {
        return getResourceUrl(path, extension, Thread.currentThread().getContextClassLoader());
    }

    protected URL getResourceUrl(String path, String extension, ClassLoader loader) {
        if (path == null || loader == null) {
            return null;
        }
        path = getResourcePath(path, extension);
        return loader.getResource(path);
    }

    protected String getFileName(URL url) {
        String s = url.getFile();
        return decodeURL(s, "UTF8");
    }

    protected String decodeURL(String s, String enc) {
        try {
            return URLDecoder.decode(s, enc);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    protected String toJarFilePath(URL jarUrl) {
        URL nestedUrl = createURL(jarUrl.getPath());
        String nestedUrlPath = nestedUrl.getPath();
        int pos = nestedUrlPath.lastIndexOf('!');
        String jarFilePath = nestedUrlPath.substring(0, pos);
        File jarFile = new File(decodeURL(jarFilePath, "UTF8"));
        return getCanonicalPath(jarFile);
    }

    protected URL createURL(String spec) {
        try {
            return new URL(spec);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
