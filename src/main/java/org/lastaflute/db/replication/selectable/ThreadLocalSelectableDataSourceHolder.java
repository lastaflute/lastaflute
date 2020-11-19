/*
 * Copyright 2015-2020 the original author or authors.
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
package org.lastaflute.db.replication.selectable;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.lastaflute.di.core.LaContainer;
import org.lastaflute.di.util.LdiStringUtil;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class ThreadLocalSelectableDataSourceHolder implements SelectableDataSourceHolder {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final ThreadLocal<String> selectableDataSourceKey = new ThreadLocal<String>();

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    @Resource
    protected LaContainer container;

    // ===================================================================================
    //                                                                           Operation
    //                                                                           =========
    public void switchSelectableDataSourceKey(String key) {
        selectableDataSourceKey.set(key);
    }

    public String getCurrentSelectableDataSourceKey() {
        return selectableDataSourceKey.get();
    }

    public DataSource getSelectedDataSource() {
        return container.getRoot().getComponent(getDataSourceComponentName());
    }

    protected String getDataSourceComponentName() {
        final String dsName = getCurrentSelectableDataSourceKey();
        if (LdiStringUtil.isEmpty(dsName)) {
            throw new IllegalStateException("Not found the current selectable data source key.");
        }
        return dsName + "DataSource";
    }
}
