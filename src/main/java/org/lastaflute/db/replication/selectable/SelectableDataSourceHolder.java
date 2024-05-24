/*
 * Copyright 2015-2024 the original author or authors.
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

import javax.sql.DataSource;

import org.lastaflute.di.core.exception.ComponentNotFoundException;

/**
 * @author modified by jflute (originated in Seasar)
 */
public interface SelectableDataSourceHolder {

    /**
     * @param key The key of data source. (NullAllowed: if null, no selectable)
     */
    void switchSelectableDataSourceKey(String key);

    /**
     * @return The current key of data source. (NullAllowed: if no selectable)
     */
    String getCurrentSelectableDataSourceKey();

    /**
     * @return The selected data source. (NotNull)
     * @throws ComponentNotFoundException When the data source is not found by the key. 
     */
    DataSource getSelectedDataSource();
}