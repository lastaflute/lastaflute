/*
 * Copyright 2015-2017 the original author or authors.
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

import javax.sql.DataSource;

import org.lastaflute.db.replication.selectable.SelectableDataSourceProxy;

/**
 * @author jflute
 */
public class MasterBasisSelectableDataSource extends SelectableDataSourceProxy {

    /**
     * Get the real data-source selected. <br>
     * This overrides to return data-source for MasterDB as default. 
     * @return The instance of real data-source. (NotNull)
     */
    @Override
    public DataSource getSelectedDataSource() {
        final String dataSourceName = selectableDataSourceHolder.getCurrentSelectableDataSourceKey();
        if (dataSourceName != null) {
            return selectableDataSourceHolder.getSelectedDataSource();
        } else { // means no name set on thread local
            try {
                selectableDataSourceHolder.switchSelectableDataSourceKey(SlaveDBAccessor.MASTER_DB); // as default
                return selectableDataSourceHolder.getSelectedDataSource();
            } finally {
                selectableDataSourceHolder.switchSelectableDataSourceKey(null); // restore
            }
        }
    }
}
