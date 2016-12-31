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
    protected LocalDateTime lastestSyncCheckTime;

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
        getLastestSyncCheckTime().map(checkDateTime -> {
            return new HandyDate(checkDateTime).toDisp("yyyy/MM/dd HH:mm:ss");
        }).ifPresent(checkDisp -> {
            sb.append(", sync=").append(checkDisp);
        });
        getUserLocale().ifPresent(locale -> {
            sb.append(", locale=").append(locale);
        });
        getUserTimeZone().ifPresent(zone -> {
            sb.append(", zone=").append(zone);
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
    public OptionalThing<LocalDateTime> getLastestSyncCheckTime() {
        return OptionalThing.ofNullable(lastestSyncCheckTime, () -> {
            String msg = "Not found the lastest synchronized check date-time in the user bean: " + getUserId();
            throw new IllegalStateException(msg);
        });
    }

    @Override
    public void manageLastestSyncCheckTime(LocalDateTime lastestSyncCheckTime) {
        if (lastestSyncCheckTime == null) {
            throw new IllegalArgumentException("The argument 'lastestSyncCheckTime' should not be null.");
        }
        this.lastestSyncCheckTime = lastestSyncCheckTime;
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
    public void manageUserLocale(Locale userLocale) {
        if (userLocale == null) {
            throw new IllegalArgumentException("The argument 'userLocale' should not be null.");
        }
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
    public void manageUserTimeZone(TimeZone userTimeZone) {
        if (userTimeZone == null) {
            throw new IllegalArgumentException("The argument 'userTimeZone' should not be null.");
        }
        this.userTimeZone = userTimeZone;
    }
}
