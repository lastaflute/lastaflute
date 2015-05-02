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
package org.lastaflute.web.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.lastaflute.web.LastaWebKey;
import org.lastaflute.web.ruts.message.ActionMessages;

/**
 * @author modified by jflute (originated in Seasar)
 */
public final class LaActionMessagesUtil {

    private LaActionMessagesUtil() {
    }

    public static void saveErrors(HttpServletRequest request, ActionMessages errors) {
        if ((errors == null) || errors.isEmpty()) {
            request.removeAttribute(LastaWebKey.ACTION_ERRORS_KEY);
            return;
        }
        request.setAttribute(LastaWebKey.ACTION_ERRORS_KEY, errors);
    }

    public static void saveErrors(HttpSession session, ActionMessages errors) {
        if ((errors == null) || errors.isEmpty()) {
            session.removeAttribute(LastaWebKey.ACTION_ERRORS_KEY);
            return;
        }
        session.setAttribute(LastaWebKey.ACTION_ERRORS_KEY, errors);
    }

    public static void saveMessages(HttpServletRequest request, ActionMessages messages) {
        if ((messages == null) || messages.isEmpty()) {
            request.removeAttribute(LastaWebKey.ACTION_INFO_KEY);
            return;
        }
        request.setAttribute(LastaWebKey.ACTION_INFO_KEY, messages);
    }

    public static void saveMessages(HttpSession session, ActionMessages messages) {
        if ((messages == null) || messages.isEmpty()) {
            session.removeAttribute(LastaWebKey.ACTION_INFO_KEY);
            return;
        }
        session.setAttribute(LastaWebKey.ACTION_INFO_KEY, messages);
    }

    public static void addErrors(HttpServletRequest request, ActionMessages errors) {
        if (errors == null) {
            return;
        }
        ActionMessages requestErrors = (ActionMessages) request.getAttribute(LastaWebKey.ACTION_ERRORS_KEY);
        if (requestErrors == null) {
            requestErrors = new ActionMessages();
        }
        requestErrors.add(errors);
        saveErrors(request, requestErrors);
    }

    public static void addErrors(HttpSession session, ActionMessages errors) {
        if (errors == null) {
            return;
        }
        ActionMessages sessionErrors = (ActionMessages) session.getAttribute(LastaWebKey.ACTION_ERRORS_KEY);
        if (sessionErrors == null) {
            sessionErrors = new ActionMessages();
        }
        sessionErrors.add(errors);
        saveErrors(session, sessionErrors);
    }

    public static boolean hasErrors(HttpServletRequest request) {
        ActionMessages errors = (ActionMessages) request.getAttribute(LastaWebKey.ACTION_ERRORS_KEY);
        if (errors != null && !errors.isEmpty()) {
            return true;
        }
        return false;
    }

    public static void addMessages(HttpServletRequest request, ActionMessages messages) {
        if (messages == null) {
            return;
        }
        ActionMessages requestMessages = (ActionMessages) request.getAttribute(LastaWebKey.ACTION_INFO_KEY);
        if (requestMessages == null) {
            requestMessages = new ActionMessages();
        }
        requestMessages.add(messages);
        saveMessages(request, requestMessages);
    }

    public static void addMessages(HttpSession session, ActionMessages messages) {
        if (messages == null) {
            return;
        }
        ActionMessages sessionMessages = (ActionMessages) session.getAttribute(LastaWebKey.ACTION_INFO_KEY);
        if (sessionMessages == null) {
            sessionMessages = new ActionMessages();
        }
        sessionMessages.add(messages);
        saveMessages(session, sessionMessages);
    }
}
