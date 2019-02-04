/*
 * Copyright 2015-2019 the original author or authors.
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
package org.lastaflute.web.ruts;

import java.util.Arrays;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.core.smartdeploy.ManagedHotdeploy;
import org.lastaflute.web.exception.ActionHookReturnNullException;
import org.lastaflute.web.exception.ActionResponseAfterTxCommitHookNotSpecifiedException;
import org.lastaflute.web.exception.ExecuteMethodLonelyValidatorAnnotationException;
import org.lastaflute.web.exception.ExecuteMethodReturnNullException;
import org.lastaflute.web.exception.ExecuteMethodReturnTypeNotResponseException;
import org.lastaflute.web.exception.ExecuteMethodReturnUndefinedResponseException;
import org.lastaflute.web.exception.RebootAfterFreeGenError;
import org.lastaflute.web.exception.RebootAfterGenerateError;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.ResponseHook;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.config.ActionFormMeta;
import org.lastaflute.web.ruts.process.ActionRuntime;
import org.lastaflute.web.validation.ActionValidator;
import org.lastaflute.web.validation.LaValidatable;

/**
 * @author jflute
 */
public class RedCardableAssist {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final ActionExecute execute; // fixed info
    protected final ActionRuntime runtime; // has state
    protected final Object action; // basically for debug message

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public RedCardableAssist(ActionExecute execute, ActionRuntime runtime, Object action) {
        this.execute = execute;
        this.runtime = runtime;
        this.action = action;
    }

    // ===================================================================================
    //                                                                     Response Return
    //                                                                     ===============
    public void assertExecuteMethodResponseDefined(ActionResponse response) {
        if (response.isUndefined()) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Cannot return undefined resopnse from the execute method.");
            br.addItem("Advice");
            br.addElement("Not allowed to return undefined() in execute method.");
            br.addElement("If you want to return response as empty body,");
            br.addElement("use asEmptyBody() like this:");
            br.addElement("  @Execute");
            br.addElement("  public HtmlResponse index() {");
            br.addElement("      return HtmlResponse.asEmptyBody();");
            br.addElement("  }");
            br.addItem("Action Execute");
            br.addElement(execute);
            final String msg = br.buildExceptionMessage();
            throw new ExecuteMethodReturnUndefinedResponseException(msg);
        }
    }

    public void assertHookReturnNotNull(ActionResponse response) {
        if (response == null) {
            throwActionHookReturnNullException();
        }
    }

    protected void throwActionHookReturnNullException() {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not allowed to return null from ActionCallback methods.");
        br.addItem("Advice");
        br.addElement("ActionHook methods should return response instance.");
        br.addElement("For example, if callbackBefore():");
        br.addElement("  (x):");
        br.addElement("    public ActionResponse callbackBefore(...) {");
        br.addElement("        return null; // *Bad");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public ActionResponse callbackBefore(...) {");
        br.addElement("        return ActionResponse.empty(); // Good");
        br.addElement("    }");
        br.addElement("    public ActionResponse callbackBefore(...) {");
        br.addElement("        return asHtml(...); // Good");
        br.addElement("    }");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Action Object");
        br.addElement(action);
        final String msg = br.buildExceptionMessage();
        throw new ActionHookReturnNullException(msg);
    }

    protected void assertExecuteReturnNotNull(Object[] requestArgs, Object result) {
        if (result == null) {
            throwExecuteMethodReturnNullException(requestArgs);
        }
    }

    protected void throwExecuteMethodReturnNullException(Object[] requestArgs) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not allowed to return null from the execute method.");
        br.addItem("Advice");
        br.addElement("Execute method should return response instance.");
        br.addElement("For example:");
        br.addElement("  (x):");
        br.addElement("    public HtmlResponse index(...) {");
        br.addElement("        return null; // *Bad");
        br.addElement("    }");
        br.addElement("  (o):");
        br.addElement("    public HtmlResponse index(...) {");
        br.addElement("        return asHtml(...); // Good");
        br.addElement("    }");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Action Object");
        br.addElement(action);
        br.addItem("Request Arguments");
        br.addElement(Arrays.asList(requestArgs));
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodReturnNullException(msg);
    }

    public void assertExecuteMethodReturnTypeActionResponse(Object[] requestArgs, Object result) {
        if (!(result instanceof ActionResponse)) {
            throwExecuteMethodReturnTypeNotResponseException(requestArgs, result);
        }
    }

    protected void throwExecuteMethodReturnTypeNotResponseException(Object[] requestArgs, Object result) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Not action response type was returned from your action.");
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("Action Object");
        br.addElement(action);
        br.addItem("Request Arguments");
        br.addElement(Arrays.asList(requestArgs));
        br.addItem("Unknoww Return");
        br.addElement(result != null ? result.getClass() : null);
        br.addElement(result);
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodReturnTypeNotResponseException(msg);
    }

    // ===================================================================================
    //                                                                       AfterTxCommit
    //                                                                       =============
    public void assertAfterTxCommitHookNotSpecified(String actionHookTitle, ActionResponse response) {
        response.getAfterTxCommitHook().ifPresent(hook -> {
            throwActionResponseAfterTxCommitHookNotSpecifiedException(actionHookTitle, response, hook);
        });
    }

    protected void throwActionResponseAfterTxCommitHookNotSpecifiedException(String actionHookTitle, ActionResponse response,
            ResponseHook hook) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("The afterTxCommit() cannot be used in action hook.");
        br.addItem("Advice");
        br.addElement("The method only can be in action execute.");
        br.addElement("Make sure your action hook response.");
        br.addItem("Specified ActionResponse");
        br.addElement(response);
        br.addItem("Specified ResponseHook");
        br.addElement(hook);
        br.addItem("Action Execute");
        br.addElement(execute);
        br.addItem("ActionHook Type");
        br.addElement(actionHookTitle);
        final String msg = br.buildExceptionMessage();
        throw new ActionResponseAfterTxCommitHookNotSpecifiedException(msg);
    }

    // ===================================================================================
    //                                                                    Validator Called
    //                                                                    ================
    public void checkValidatorCalled() {
        if (!execute.isSuppressValidatorCallCheck() && certainlyNotBeValidatorCalled()) {
            execute.getFormMeta().filter(meta -> isValidatorAnnotated(meta)).ifPresent(meta -> {
                throwLonelyValidatorAnnotationException(meta); // #hope see fields in nested element
            });
        }
    }

    protected boolean certainlyNotBeValidatorCalled() {
        return ActionValidator.certainlyValidatorNotCalled();
    }

    protected boolean isValidatorAnnotated(ActionFormMeta meta) {
        return meta.isValidatorAnnotated();
    }

    protected void throwLonelyValidatorAnnotationException(ActionFormMeta meta) {
        final boolean apiExecute = execute.isApiExecute();
        final boolean hybrid = LaValidatable.class.isAssignableFrom(execute.getActionType());
        final String expectedMethod;
        if (apiExecute) {
            expectedMethod = hybrid ? "validateApi" : "validate";
        } else {
            expectedMethod = "validate";
        }
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Lonely validator annotations, so call " + expectedMethod + "().");
        br.addItem("Advice");
        br.addElement("The " + expectedMethod + "() method should be called in execute method of action");
        br.addElement("because the validator annotations are specified in the form (or body).");
        br.addElement("For example:");
        if (apiExecute) {
            br.addElement("  (x):");
            br.addElement("    @Execute");
            br.addElement("    public JsonResponse index(SeaForm form) { // *Bad");
            br.addElement("        return asJson(...);");
            br.addElement("    }");
            br.addElement("  (o):");
            br.addElement("    @Execute");
            br.addElement("    public JsonResponse index(SeaForm form) {");
            br.addElement("        " + expectedMethod + "(form, message -> {}); // Good");
            br.addElement("        return asJson(...);");
            br.addElement("    }");
        } else {
            br.addElement("  (x):");
            br.addElement("    @Execute");
            br.addElement("    public HtmlResponse index(SeaForm form) { // *Bad");
            br.addElement("        return asHtml(...);");
            br.addElement("    }");
            br.addElement("  (o):");
            br.addElement("    @Execute");
            br.addElement("    public HtmlResponse index(SeaForm form) {");
            br.addElement("        " + expectedMethod + "(form, message -> {}, () -> { // Good");
            br.addElement("            return asHtml(path_LandHtml);");
            br.addElement("        });");
            br.addElement("        return asHtml(...);");
            br.addElement("    }");
        }
        br.addElement("");
        br.addElement("Or remove validator annotations from the form (or body)");
        br.addElement("if the annotations are unneeded.");
        br.addItem("Action Execute");
        br.addElement(execute.toSimpleMethodExp());
        br.addItem("Action Form (or Body)");
        br.addElement(meta.getSymbolFormType());
        final String msg = br.buildExceptionMessage();
        throw new ExecuteMethodLonelyValidatorAnnotationException(msg);
    }

    // ===================================================================================
    //                                                                     HotDeploy Error
    //                                                                     ===============
    public void translateToHotdeployErrorIfPossible(Error e) { // to notice reboot timing
        if (!ManagedHotdeploy.isHotdeploy()) {
            return;
        }
        final String msg = e.getMessage();
        if (msg == null) {
            return;
        }
        // only frequent patterns
        if (e instanceof NoSuchFieldError) {
            if (isNoSuchFieldOfHtmlPath(msg)) {
                throwRebootAfterFreeGenError(e);
            }
        }
        if (e instanceof NoSuchMethodError) {
            if (isNoSuchMethodOfMessages(msg) || isNoSuchMethodOfConfig(msg)) {
                throwRebootAfterFreeGenError(e);
            }
            if (isNoSuchMethodOfDBFlute(msg)) {
                throwRebootAfterGenerateError(e);
            }
        }
    }

    protected boolean isNoSuchFieldOfHtmlPath(String msg) {
        return msg.contains("path_");
    }

    protected boolean isNoSuchMethodOfMessages(String msg) {
        return msg.contains("Messages.add") || msg.contains("Lables.add");
    }

    protected boolean isNoSuchMethodOfConfig(String msg) {
        return msg.contains("Config.get") || msg.contains("Env.get");
    }

    protected void throwRebootAfterFreeGenError(Error e) throws RebootAfterFreeGenError {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Reboot after FreeGen.");
        br.addItem("Advice");
        br.addElement("Sorry...");
        br.addElement("Auto-generated classes (e.g. HtmlPath, Messages, Config)");
        br.addElement(" are not HotDeploy target.");
        br.addElement("so please reboot your application.");
        br.addElement("");
        br.addElement("Execute your main() of the boot class like this:");
        br.addElement("  public static void main(String[] args) {");
        br.addElement("      new JettyBoot(8090, \"/harbor\").asDevelopment(isNoneEnv()).bootAwait();");
        br.addElement("  }");
        br.addItem("Occurred Error");
        br.addElement(e.getClass());
        br.addElement(e.getMessage());
        final String msg = br.buildExceptionMessage();
        throw new RebootAfterFreeGenError(msg, e);
    }

    protected boolean isNoSuchMethodOfDBFlute(String msg) {
        return msg.contains(".allcommon.") // e.g. CDef
                || msg.contains(".bsbhv.") || msg.contains(".exbhv.") // Behavior
                || msg.contains(".cbean.") // ConditionBean, ConditionQuery
                || msg.contains(".bsentity.") || msg.contains(".exentity."); // Entity, DBMeta
    }

    protected void throwRebootAfterGenerateError(Error e) throws RebootAfterFreeGenError {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Reboot after DBFlute Generate.");
        br.addItem("Advice");
        br.addElement("Sorry...");
        br.addElement("Auto-generated classes (e.g. Behavior, ConditionBean)");
        br.addElement(" are not HotDeploy target.");
        br.addElement("so please reboot your application.");
        br.addElement("");
        br.addElement("Execute your main() of the boot class like this:");
        br.addElement("  public static void main(String[] args) {");
        br.addElement("      new JettyBoot(8090, \"/harbor\").asDevelopment(isNoneEnv()).bootAwait();");
        br.addElement("  }");
        br.addItem("Occurred Error");
        br.addElement(e.getClass());
        br.addElement(e.getMessage());
        final String msg = br.buildExceptionMessage();
        throw new RebootAfterGenerateError(msg, e);
    }
}
