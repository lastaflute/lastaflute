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
package org.lastaflute.core.smartdeploy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation for environment dispatch. <br>
 * You should use creator that can handle this, e.g. RomanticLogicCreator.
 * <pre>
 * e.g.
 *  &#64;EnvDispatch(development=FooLogicVirtual.class, production=FooLogicReal.class)
 *  public interface FooLogic {
 *  }
 *  
 *  // for development
 *  public class FooLogicVirtual implements FooLogic {
 *  }
 *  
 *  // for production
 *  public class FooLogicReal implements FooLogic {
 *  }
 * </pre>
 * @author jflute
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface EnvDispatch {

    /**
     * @return The type of implementation class for development environment. (NotNull)
     */
    Class<?> development();

    /**
     * @return The type of implementation class for production environment. (NotNull)
     */
    Class<?> production();

    /**
     * You can switch environment determiner.
     * <pre>
     * &#64;EnvDispatch(development=..., production=..., prop=DocksideMessages.MAIL_SEND_MOCK)
     * </pre>
     * @return The key of property as environment determiner. (NullAllowed: then use 'development.here')
     */
    String prop();
}
