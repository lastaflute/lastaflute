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
package org.lastaflute.db.replication.selectable;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class SelectableDataSourceProxy implements DataSource {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected SelectableDataSourceHolder selectableDataSourceHolder;

    // ===================================================================================
    //                                                                 Selected DataSource
    //                                                                 ===================
    public DataSource getDataSource() {
        return selectableDataSourceHolder.getSelectedDataSource();
    }

    // ===================================================================================
    //                                                                      Implementation
    //                                                                      ==============
    public Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public Connection getConnection(final String username, final String password) throws SQLException {
        return getDataSource().getConnection(username, password);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return getDataSource().getLogWriter();
    }

    public void setLogWriter(final PrintWriter out) throws SQLException {
        getDataSource().setLogWriter(out);
    }

    public int getLoginTimeout() throws SQLException {
        return getDataSource().getLoginTimeout();
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        getDataSource().setLoginTimeout(seconds);
    }

    // #java8comp DataSource: getParentLogger(), unwrap(), isWrapperFor()
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return getDataSource().getParentLogger();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return getDataSource().unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return getDataSource().isWrapperFor(iface);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public void setSelectableDataSourceHolder(SelectableDataSourceHolder selectableDataSourceHolder) {
        this.selectableDataSourceHolder = selectableDataSourceHolder;
    }
}
