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
package org.lastaflute.web.login;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.TimeZone;

import org.dbflute.helper.HandyDate;
import org.dbflute.optional.OptionalThing;

/**
 * @param <ID> The type of user ID.
 * @author jflute
 */
public abstract class TypicalUserBean<ID> implements UserBean<ID>, SyncCheckable, I18nable, Serializable {

    private static final long serialVersionUID = 1L;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** the latest date of synchronized check. (NullAllowed: no check yet) */
    protected LocalDateTime lastestSyncCheckDateTime;

    /** The locale for the user. (NullAllowed) */
    protected Locale userLocale;

    /** The time-zone for the user. (NullAllowed) */
    protected TimeZone userTimeZone;

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{userId=").append(getUserId());
        setupToStringAdditionalUserInfo(sb);
        getLastestSyncCheckDateTime().map(checkDateTime -> {
            return new HandyDate(checkDateTime).toDisp("yyyy/MM/dd HH:mm:ss");
        }).ifPresent(checkDisp -> {
            sb.append(", sync=").append(checkDisp);
        });
        getUserLocale().ifPresent(locale -> {
            sb.append(", locale=").append(locale);
        });
        getUserTimeZone().ifPresent(zone -> {
            sb.append(", timeZone=").append(zone);
        });
        sb.append("}@").append(Integer.toHexString(hashCode()));
        return sb.toString();
    }

    protected void setupToStringAdditionalUserInfo(StringBuilder sb) {
    }

    // ===================================================================================
    //                                                                           SyncCheck
    //                                                                           =========
    @Override
    public OptionalThing<LocalDateTime> getLastestSyncCheckDateTime() {
        return OptionalThing.ofNullable(lastestSyncCheckDateTime, () -> {
            String msg = "Not found the lastest synchronized check date-time in the user bean: " + getUserId();
            throw new IllegalStateException(msg);
        });
    }

    @Override
    public void setLastestSyncCheckDateTime(LocalDateTime checkDt) {
        lastestSyncCheckDateTime = checkDt;
    }

    // ===================================================================================
    //                                                                       i18n Handling
    //                                                                       =============
    @Override
    public OptionalThing<Locale> getUserLocale() {
        return OptionalThing.ofNullable(userLocale, () -> {
            String msg = "Not found the user locale in the user bean: " + getUserId();
            throw new IllegalStateException(msg);
        });
    }

    @Override
    public void setUserLocale(Locale userLocale) {
        this.userLocale = userLocale;
    }

    @Override
    public OptionalThing<TimeZone> getUserTimeZone() {
        return OptionalThing.ofNullable(userTimeZone, () -> {
            String msg = "Not found the user time-zone in the user bean: " + getUserId();
            throw new IllegalStateException(msg);
        });
    }

    @Override
    public void setUserTimeZone(TimeZone userTimeZone) {
        this.userTimeZone = userTimeZone;
    }
}
