/*
 * Copyright 2015-2022 the original author or authors.
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
package org.lastaflute.db.replication.slavedb;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.lastaflute.db.replication.selectable.SelectableDataSourceHolder;
import org.lastaflute.db.replication.selectable.SelectableDataSourceProxy;

/**
 * The proxy for selectable data-source that is on master basis.
 * @author jflute
 */
public class MasterBasisSelectableDataSource extends SelectableDataSourceProxy {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    @Resource
    private SlaveDBAccessor slaveDBAccessor; // to use data source key
    @Resource
    private SelectableDataSourceHolder selectableDataSourceHolder; // needs selectable_datasource.xml

    // ===================================================================================
    //                                                                 Selected DataSource
    //                                                                 ===================
    /**
     * Get the real data-source selected. <br>
     * This overrides to return data-source for master DB as default. 
     * @return The instance of real data-source. (NotNull)
     */
    @Override
    public DataSource getSelectedDataSource() {
        final String current = getCurrentSelectableDataSourceKey();
        if (current != null) {
            return selectDataSrouce();
        } else { // means no name set on thread local
            try {
                switchSelectableDataSourceKey(prepareMasterDataSourceKey());
                return selectDataSrouce();
            } finally {
                switchSelectableDataSourceKey(null); // restore
            }
        }
    }

    // ===================================================================================
    //                                                                          Selectable
    //                                                                          ==========
    protected String getCurrentSelectableDataSourceKey() {
        return selectableDataSourceHolder.getCurrentSelectableDataSourceKey();
    }

    protected DataSource selectDataSrouce() {
        return selectableDataSourceHolder.getSelectedDataSource();
    }

    protected void switchSelectableDataSourceKey(String key) {
        selectableDataSourceHolder.switchSelectableDataSourceKey(key);
    }

    // ===================================================================================
    //                                                                      DataSource Key
    //                                                                      ==============
    protected String prepareMasterDataSourceKey() {
        return slaveDBAccessor.prepareMasterDataSourceKey();
    }
}
