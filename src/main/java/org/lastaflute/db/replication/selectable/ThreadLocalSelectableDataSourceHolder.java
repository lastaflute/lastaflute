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
    // no static because it may be created per schema (for e.g. multiple schema (DB))
    //
    // and you cannot nest use of SlaveDBAccessor between several schemas
    // because of only-one thread-local instance (if you use selectable_datasource.xml)
    // if you want wide-scope selectable, you should define instances per schema
    //
    protected final ThreadLocal<String> selectableDataSourceKey = new ThreadLocal<String>();

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
        return getDataSourceComponent(getDataSourceComponentName());
    }

    protected DataSource getDataSourceComponent(String dataSourceComponentName) {
        // uses the root container so the component name must be unique in your project
        return container.getRoot().getComponent(dataSourceComponentName);
    }

    // ===================================================================================
    //                                                                      Component Name
    //                                                                      ==============
    protected String getDataSourceComponentName() {
        final String dataSourceKey = getCurrentSelectableDataSourceKey();
        if (LdiStringUtil.isEmpty(dataSourceKey)) {
            throw new IllegalStateException("Not found the current selectable data source key.");
        }
        return buildDataSourceComponentName(dataSourceKey);
    }

    protected String buildDataSourceComponentName(String dataSourceKey) {
        return dataSourceKey + getDataSourceSuffix();
    }

    protected String getDataSourceSuffix() {
        return "DataSource";
    }
}
