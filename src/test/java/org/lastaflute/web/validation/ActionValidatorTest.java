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
package org.lastaflute.web.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.constraints.NotNull;

import org.lastaflute.core.message.UserMessage;
import org.lastaflute.core.message.UserMessages;
import org.lastaflute.unit.UnitLastaFluteTestCase;
import org.lastaflute.unit.mock.web.MockRequestManager;
import org.lastaflute.unit.mock.web.validation.MockConstraintViolation;
import org.lastaflute.web.validation.theme.typed.TypeBigDecimal;
import org.lastaflute.web.validation.theme.typed.TypeDouble;
import org.lastaflute.web.validation.theme.typed.TypeFloat;
import org.lastaflute.web.validation.theme.typed.TypeInteger;
import org.lastaflute.web.validation.theme.typed.TypeLong;

/**
 * @author jflute
 */
public class ActionValidatorTest extends UnitLastaFluteTestCase {

    public void test_toUserMessages() throws Exception {
        // ## Arrange ##
        ActionValidator<UserMessages> validator = createValidator();
        MockForm form = new MockForm();
        Set<ConstraintViolation<Object>> vioSet = new HashSet<ConstraintViolation<Object>>();
        vioSet.add(createViolation("sea"));
        vioSet.add(createViolation("land.oneman"));
        vioSet.add(createViolation("land.minio"));
        vioSet.add(createViolation("iks"));
        vioSet.add(createViolation("iks[0].dstore"));
        vioSet.add(createViolation("iks[2].dstore"));
        vioSet.add(createViolation("iks[2].square"));
        vioSet.add(createViolation("iks[3].square"));
        vioSet.add(createViolation("iks[5].dstore"));

        // ## Act ##
        UserMessages messages = validator.toUserMessages(form, vioSet);

        // ## Assert ##
        Set<String> propertySet = messages.toPropertySet();
        assertHasAnyElement(propertySet);
        log(propertySet);
        assertEquals(vioSet.size(), propertySet.size());
        List<UserMessage> messageList = new ArrayList<UserMessage>();
        for (String property : propertySet) {
            for (Iterator<UserMessage> iterator = messages.accessByIteratorOf(property); iterator.hasNext();) {
                messageList.add(iterator.next());
            }
        }
        for (UserMessage message : messageList) {
            log(message);
        }
    }

    protected ActionValidator<UserMessages> createValidator() {
        MockRequestManager requestManager = new MockRequestManager();
        return new ActionValidator<UserMessages>(requestManager, () -> new UserMessages());
    }

    protected MockConstraintViolation createViolation(String propertyPath) {
        return new MockConstraintViolation(propertyPath);
    }

    // ===================================================================================
    //                                                            Annotation Determination
    //                                                            ========================
    public void test_isLastaPresentsAnnotation() throws Exception {
        assertFalse(ActionValidator.isLastaPresentsAnnotation(NotNull.class));
        assertTrue(ActionValidator.isLastaPresentsAnnotation(Required.class));
        assertTrue(ActionValidator.isLastaPresentsAnnotation(TypeInteger.class));
        assertTrue(ActionValidator.isLastaPresentsAnnotation(TypeLong.class));
        assertTrue(ActionValidator.isLastaPresentsAnnotation(TypeBigDecimal.class));
        assertTrue(ActionValidator.isLastaPresentsAnnotation(TypeFloat.class));
        assertTrue(ActionValidator.isLastaPresentsAnnotation(TypeDouble.class));
    }

    // ===================================================================================
    //                                                                         Test Helper
    //                                                                         ===========
    protected static class MockForm {

        public String sea;
        public MockBean land;
        public List<MockElement> iks;
    }

    protected static class MockBean {

        public String oneman;
        public String minio;
    }

    protected static class MockElement {

        public String dstore;
        public String square;
    }
}
