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
package org.lastaflute.core.time;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.dbflute.helper.HandyDate;
import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfReflectionUtil.ReflectionFailureException;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;
import org.dbflute.util.Srl.ScopeInfo;

/**
 * @author jflute
 */
public class RelativeDateScript {

    protected static final String RESOLVED_PATTERN = "yyyy/MM/dd HH:mm:ss.SSS";
    protected static final Date LIMIT_DATE = new HandyDate("8999/12/31").getDate();
    protected static final String HARD_CODING_BEGIN_MARK = "$(";
    protected static final String HARD_CODING_END_MARK = ")";

    public Date resolveHardCodingDate(String relativeDate) {
        // $(2014/07/10)
        // $(2014/07/10).addDay(1)
        // $(2014/07/10 12:34:56)
        // $(2014/07/10 12:34:56).addDay(1).moveToDayJust()
        // $(2014/07/10).addMonth(3).moveToMonthTerminal()
        final ScopeInfo first = Srl.extractScopeFirst(relativeDate, HARD_CODING_BEGIN_MARK, HARD_CODING_END_MARK);
        if (first == null) {
            throwRelativeDateHandyDateNotFoundException(relativeDate);
        }
        final String baseDateExp = first.getContent();
        final Date date = new HandyDate(Srl.unquoteDouble(baseDateExp)).getDate();
        final String nextExp = first.substringInterspaceToNext(); // e.g. .addDay(3)
        return resolveRelativeDate(nextExp, date);
    }

    public Date resolveRelativeDate(String relativeDate, Date date) {
        // addDay(1)
        // addDay(1).moveToDayJust()
        // addMonth(3).moveToMonthTerminal()
        final String filtered = Srl.ltrim(relativeDate, "."); // e.g. .addDay(3) -> addDay(3)
        final String resolvedExp = doResolveRelativeDate(filtered, date);
        return new HandyDate(resolvedExp).getDate();
    }

    protected String doResolveRelativeDate(String relativeDate, Date date) {
        final String calcPart = relativeDate.trim();
        if (calcPart.trim().length() == 0 || date.after(LIMIT_DATE)) {
            return DfTypeUtil.toString(date, RESOLVED_PATTERN);
        }
        final List<String> methodList = Srl.splitListTrimmed(Srl.trim(calcPart, "."), ".");
        HandyDate handyDate = new HandyDate(date);
        for (String methodCall : methodList) {
            handyDate = invokeMethod(relativeDate, handyDate, methodCall);
        }
        return DfTypeUtil.toString(handyDate.getDate(), RESOLVED_PATTERN);
    }

    protected HandyDate invokeMethod(String relativeDate, HandyDate handyDate, String methodCall) {
        if (!methodCall.contains("(") || !methodCall.endsWith(")")) {
            throwRelativeDateMethodArgPartNotFoundException(relativeDate);
        }
        final String methodName = Srl.substringFirstFront(methodCall, "(");
        final String methodArgsPart = Srl.substringFirstFront(Srl.substringFirstRear(methodCall, "("), ")");
        final List<String> argElementList;
        if (Srl.is_NotNull_and_NotTrimmedEmpty(methodArgsPart)) {
            argElementList = Srl.splitListTrimmed(methodArgsPart, ",");
        } else {
            argElementList = DfCollectionUtil.emptyList();
        }
        final List<Object> argValueList = DfCollectionUtil.newArrayList();
        for (String arg : argElementList) {
            if (isNumber(arg)) {
                argValueList.add(DfTypeUtil.toInteger(arg)); // int only supported (cannot use long)
            } else {
                argValueList.add(arg);
            }
        }
        final List<Class<?>> argTypeList = DfCollectionUtil.newArrayList();
        for (Object argValue : argValueList) {
            final Class<? extends Object> argType = argValue.getClass();
            if (Integer.class.equals(argType)) {
                // even if the argument value is int type, getClass() returns Integer type
                argTypeList.add(int.class);
            } else {
                argTypeList.add(argType);
            }
        }
        final Class<?>[] argTypes = argTypeList.toArray(new Class<?>[argTypeList.size()]);
        final Class<HandyDate> handyDateType = HandyDate.class;
        final Method method = DfReflectionUtil.getPublicMethod(handyDateType, methodName, argTypes);
        if (method == null) {
            throwRelativeDateMethodNotFoundException(relativeDate, handyDateType, methodName, argTypes);
        }
        try {
            handyDate = (HandyDate) DfReflectionUtil.invoke(method, handyDate, argValueList.toArray());
        } catch (ReflectionFailureException e) {
            throwRelativeDateInvokeFailureException(relativeDate, handyDateType, methodName, e);
        }
        return handyDate;
    }

    protected boolean isNumber(String str) { // except decimal
        final String minusRemovedStr = str.startsWith("-") ? Srl.substringFirstRear(str, "-") : str;
        return Srl.isNumberHarfAll(minusRemovedStr);
    }

    protected void throwRelativeDateHandyDateNotFoundException(String relativeDate) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the handy date expression.");
        br.addItem("Advice");
        br.addElement("You should specify like this:");
        br.addElement("For example:");
        br.addElement("  (o): $2014/07/10)");
        br.addElement("  (o): $2014/07/10 12:34:56)");
        br.addElement("  (o): $2014/07/10).addDay(3)");
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }

    protected void throwRelativeDateMethodArgPartNotFoundException(String relativeDate) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the method argument part for RelativeDate.");
        br.addItem("Advice");
        br.addElement("You should add '()' at method rear.");
        br.addElement("For example:");
        br.addElement("  (x): addDay");
        br.addElement("  (x): addDay.moveTo...");
        br.addElement("  (o): addDay(7)");
        br.addElement("  (o): addDay(7).moveTo...");
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }

    protected void throwRelativeDateMethodNotFoundException(String relativeDate, Class<HandyDate> handyDateType, String methodName,
            Class<?>[] argTypes) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not found the method to invoke for RelativeDate.");
        br.addItem("Relative Date");
        br.addElement(relativeDate);
        br.addItem("HandyDate Type");
        br.addElement(handyDateType);
        br.addItem("NotFound Method");
        br.addElement(methodName);
        br.addItem("Argument Type");
        br.addElement(Arrays.asList(argTypes));
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }

    protected void throwRelativeDateInvokeFailureException(String relativeDate, Class<HandyDate> targetType, String methodName,
            ReflectionFailureException e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to invoke the method for RelativeDate.");
        br.addItem("Relative Date");
        br.addElement(relativeDate);
        br.addItem("HandyDate Type");
        br.addElement(targetType);
        br.addItem("Failed Method");
        br.addElement(methodName);
        br.addItem("Reflection Exception");
        br.addElement(e.getClass());
        br.addElement(e.getMessage());
        final String msg = br.buildExceptionMessage();
        throw new IllegalStateException(msg);
    }
}
