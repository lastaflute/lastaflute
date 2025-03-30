/*
 * Copyright 2015-2024 the original author or authors.
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
package org.lastaflute.core.json.mask;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lastaflute.core.json.JsonManager;

/**
 * @author jflute
 * @since 1.2.8 (2025/03/30 Sunday at nakameguro)
 */
public class JsonMaskingTape {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final JsonManager jsonManager; // not null
    protected final MaskingTapeResource resource; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public JsonMaskingTape(JsonManager jsonManager, MaskingTapeResource resource) {
        this.jsonManager = jsonManager;
        this.resource = resource;
    }

    // ===================================================================================
    //                                                                               Mask
    //                                                                              ======
    public String mask(String plainJson) {
        final Set<String> maskParamSet = resource.getMaskParamSet();
        if (maskParamSet.isEmpty()) {
            return plainJson; // unneeded
        }
        final String maskedJson;
        final String trimmedJson = plainJson.trim(); // to determine
        if (trimmedJson.startsWith("[")) { // as list
            final List<Object> rootList = fromJsonList(plainJson);
            tapeList(maskParamSet, rootList);
            maskedJson = jsonManager.toJson(rootList);
        } else if (trimmedJson.startsWith("{")) { // as map
            final Map<String, Object> rootMap = fromJsonMap(plainJson);
            tapeMap(maskParamSet, rootMap);
            maskedJson = jsonManager.toJson(rootMap);
        } else { // may be scalar value
            maskedJson = plainJson; // mask unneeded
        }
        return maskedJson;
    }

    // ===================================================================================
    //                                                                         JSON Parser
    //                                                                         ===========
    // #for_now jflute Gson returns mutable list and map so no care here (2025/03/29)
    // however, hopely it should gurantee mutable collections
    // (e.g. converting all list/map objects to mutable here)
    @SuppressWarnings("unchecked")
    protected List<Object> fromJsonList(String json) {
        return jsonManager.fromJson(json, List.class); // should be mutable
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> fromJsonMap(String json) {
        return jsonManager.fromJson(json, Map.class); // should be mutable
    }

    // ===================================================================================
    //                                                                        Masking Tape
    //                                                                        ============
    protected void tapeList(Set<String> maskParamSet, List<Object> list) {
        for (Object element : list) {
            tapeNest(maskParamSet, element);
        }
    }

    protected void tapeMap(Set<String> maskParamSet, Map<String, Object> map) {
        final String maskingString = resource.getMaskingString();
        map.keySet().forEach(key -> {
            final Object value = map.get(key);
            if (maskParamSet.contains(key)) { // e.g. password
                map.put(key, maskingString); // overwrite (making here)
            } else {
                tapeNest(maskParamSet, value);
            }
        });
    }

    protected void tapeNest(Set<String> maskParamSet, Object value) {
        if (value instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) value;
            tapeList(maskParamSet, list);
        } else if (value instanceof Map<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nestedMap = (Map<String, Object>) value;
            tapeMap(maskParamSet, nestedMap);
        }
    }
}
