package com.kodedu.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

public interface ExecShortcuts {
    static final ObjectMapper objectMapper = new ObjectMapper();

    WebEngine webEngine();

    JSObject getWindow();

    default Object executeScript(String script) {
        return webEngine().executeScript(script);
    }

    default <T> T executeScript(String script, Class<T> javaType) {
        String object = (String) webEngine().executeScript(script);
        try {
            return objectMapper.readValue(object, javaType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    default Object call(String methodName, Object... args) {
        return getWindow().call(methodName, args);
    }

}
