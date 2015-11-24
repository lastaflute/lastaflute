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
package org.lastaflute.unit.mock.web;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.message.MessageManager;
import org.lastaflute.core.time.TimeManager;
import org.lastaflute.web.api.ApiManager;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.servlet.cookie.CookieManager;
import org.lastaflute.web.servlet.request.RequestManager;
import org.lastaflute.web.servlet.request.ResponseManager;
import org.lastaflute.web.servlet.request.scoped.ScopedMessageHandler;
import org.lastaflute.web.servlet.session.SessionManager;

/**
 * @author jflute
 */
public class MockRequestManager implements RequestManager {

    @Override
    public <ATTRIBUTE> OptionalThing<ATTRIBUTE> getAttribute(String key, Class<ATTRIBUTE> genericType) {
        return null;
    }

    @Override
    public List<String> getAttributeNameList() {
        return Collections.emptyList();
    }

    @Override
    public void setAttribute(String key, Object value) {
    }

    @Override
    public void removeAttribute(String key) {
    }

    @Override
    public HttpServletRequest getRequest() {
        return null;
    }

    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public OptionalThing<String> getCharacterEncoding() {
        return null;
    }

    @Override
    public OptionalThing<String> getContentType() {
        return null;
    }

    @Override
    public boolean isPost() {
        return false;
    }

    @Override
    public OptionalThing<String> getParameter(String key) {
        return null;
    }

    @Override
    public String getRequestBody() {
        return null;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public String getRequestPath() {
        return null;
    }

    @Override
    public String getRequestPathAndQuery() {
        return null;
    }

    @Override
    public OptionalThing<String> getQueryString() {
        return null;
    }

    @Override
    public String toContextAbsolutePath(String contextRelativePath) {
        return null;
    }

    @Override
    public OptionalThing<String> getHeader(String headerKey) {
        return null;
    }

    @Override
    public OptionalThing<String> getHeaderHost() {
        return null;
    }

    @Override
    public OptionalThing<String> getHeaderReferer() {
        return null;
    }

    @Override
    public OptionalThing<String> getHeaderUserAgent() {
        return null;
    }

    @Override
    public OptionalThing<String> getHeaderXForwardedFor() {
        return null;
    }

    @Override
    public OptionalThing<String> getRemoteAddr() {
        return null;
    }

    @Override
    public OptionalThing<String> getRemoteHost() {
        return null;
    }

    @Override
    public OptionalThing<String> getRemoteIp() {
        return null;
    }

    @Override
    public OptionalThing<Integer> getRemotePort() {
        return null;
    }

    @Override
    public OptionalThing<String> getRemoteUser() {
        return null;
    }

    @Override
    public Locale getUserLocale() {
        return null;
    }

    @Override
    public Locale resolveUserLocale(ActionRuntime runtime) {
        return null;
    }

    @Override
    public void saveUserLocaleToCookie(Locale locale) {
    }

    @Override
    public void saveUserLocaleToSession(Locale locale) {
    }

    @Override
    public TimeZone getUserTimeZone() {
        return null;
    }

    @Override
    public TimeZone resolveUserTimeZone(ActionRuntime runtime) {
        return null;
    }

    @Override
    public void saveUserTimeZoneToCookie(TimeZone timeZone) {
    }

    @Override
    public void saveUserTimeZoneToSession(TimeZone timeZone) {
    }

    @Override
    public ScopedMessageHandler errors() {
        return null;
    }

    @Override
    public ScopedMessageHandler info() {
        return null;
    }

    @Override
    public void saveErrorsToSession() {
    }

    @Override
    public ResponseManager getResponseManager() {
        return null;
    }

    @Override
    public SessionManager getSessionManager() {
        return null;
    }

    @Override
    public CookieManager getCookieManager() {
        return null;
    }

    @Override
    public TimeManager getTimeManager() {
        return null;
    }

    @Override
    public MessageManager getMessageManager() {
        return null;
    }

    @Override
    public JsonManager getJsonManager() {
        return null;
    }

    @Override
    public ApiManager getApiManager() {
        return null;
    }
}
