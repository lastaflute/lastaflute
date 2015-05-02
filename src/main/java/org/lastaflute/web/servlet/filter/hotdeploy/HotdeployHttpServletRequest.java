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
package org.lastaflute.web.servlet.filter.hotdeploy;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;

/**
 * @author modified by jflute (originated in Seasar)
 */
public class HotdeployHttpServletRequest extends HttpServletRequestWrapper {

    protected final HttpServletRequest wrapped;
    protected HttpSession session;

    public HotdeployHttpServletRequest(HttpServletRequest originalRequest) {
        super(originalRequest);
        this.wrapped = originalRequest;
    }

    @Override
    public HttpSession getSession() {
        return getSession(true);
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (session != null) {
            return session;
        }
        final HttpSession originalSession = wrapped.getSession(create);
        if (originalSession == null) {
            return originalSession;
        }
        session = new HotdeployHttpSession(this, originalSession);
        return session;
    }

    public void invalidateSession() {
        session = null;
    }

    @Override
    public String toString() {
        final String plain = wrapped.toString();
        final String firstLine = plain.contains("\n") ? Srl.substringFirstFront(plain, "\n") + "..." : plain; // might contain line so...
        return DfTypeUtil.toClassTitle(this) + "{" + firstLine + "}@" + Integer.toHexString(hashCode());
    }
}
