/*
 * Copyright 2015-2019 the original author or authors.
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
package org.lastaflute.core.smartdeploy.coins;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.lastaflute.di.naming.NamingConvention;

/**
 * @author jflute
 * @since 1.1.8 (2020/07/02 Thursday at rjs)
 */
public class CreatorPackageProvider {

    public List<String> deriveWebPackageList(NamingConvention namingConvention) {
        final String[] packageNames = namingConvention.getRootPackageNames();
        final String webRoot = namingConvention.getWebRootPackageName();
        return buildPackageExp(packageNames, webRoot);
    }

    public List<String> deriveJobPackageList(NamingConvention namingConvention) {
        final String[] packageNames = namingConvention.getRootPackageNames();
        final String jobRoot = namingConvention.getJobRootPackageName();
        return buildPackageExp(packageNames, jobRoot);
    }

    protected List<String> buildPackageExp(String[] packageNames, String jobRoot) {
        return Stream.of(packageNames).map(name -> name + "." + jobRoot + ".").collect(Collectors.toList());
    }
}
