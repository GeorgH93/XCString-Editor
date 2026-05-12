package com.xcstring.editor.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class XCStringsService {

    private final Gson gson;

    public XCStringsService() {
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    public Map<String, Object> parse(String content) {
        try {
            Map<String, Object> data = gson.fromJson(content, new TypeToken<Map<String, Object>>(){}.getType());
            return fixDataForJavaScript(data);
        } catch (Exception e) {
            throw new RuntimeException("Invalid xcstring format: " + e.getMessage(), e);
        }
    }

    public String generate(Object data) {
        try {
            JsonElement rootElement = JsonParser.parseString(gson.toJson(data));
            rootElement = ensureObjectsStayObjects(rootElement);
            rootElement = fixDoubleEscapedNewlines(rootElement);

            Gson prettyGson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
            String json = prettyGson.toJson(rootElement);

            json = convertIndentation(json);
            json = fixColonSpacing(json);

            return json;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate XCString: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fixDataForJavaScript(Map<String, Object> data) {
        if (!data.containsKey("strings")) {
            return data;
        }

        Object stringsObj = data.get("strings");
        if (!(stringsObj instanceof Map)) {
            return data;
        }

        Map<String, Object> strings = (Map<String, Object>) stringsObj;
        for (Map.Entry<String, Object> entry : strings.entrySet()) {
            Object stringData = entry.getValue();
            if (!(stringData instanceof Map)) {
                continue;
            }

            Map<String, Object> stringMap = (Map<String, Object>) stringData;
            if (stringMap.isEmpty()) {
                Map<String, Object> newStringData = new LinkedHashMap<>();
                newStringData.put("localizations", new LinkedHashMap<>());
                entry.setValue(newStringData);
            } else {
                Object localizations = stringMap.get("localizations");
                if (localizations == null) {
                    stringMap.put("localizations", new LinkedHashMap<>());
                } else if (localizations instanceof Iterable && !((Iterable<?>) localizations).iterator().hasNext()) {
                    stringMap.put("localizations", new LinkedHashMap<>());
                }
            }
        }
        return data;
    }

    private JsonElement ensureObjectsStayObjects(JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            JsonArray newArray = new JsonArray();
            for (JsonElement child : array) {
                newArray.add(ensureObjectsStayObjects(child));
            }
            return newArray;
        } else if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            JsonObject newObj = new JsonObject();
            for (String key : obj.keySet()) {
                JsonElement value = obj.get(key);
                if (value.isJsonArray() && value.getAsJsonArray().isEmpty()) {
                    newObj.add(key, new JsonObject());
                } else {
                    newObj.add(key, ensureObjectsStayObjects(value));
                }
            }
            return newObj;
        }
        return element;
    }

    private JsonElement fixDoubleEscapedNewlines(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            JsonObject newObj = new JsonObject();
            for (String key : obj.keySet()) {
                JsonElement value = obj.get(key);
                if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                    String text = value.getAsString();
                    if (text.contains("\\n")) {
                        newObj.addProperty(key, text.replace("\\n", "\n"));
                    } else {
                        newObj.add(key, value);
                    }
                } else {
                    newObj.add(key, fixDoubleEscapedNewlines(value));
                }
            }
            return newObj;
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            JsonArray newArray = new JsonArray();
            for (JsonElement child : array) {
                if (child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    String text = child.getAsString();
                    if (text.contains("\\n")) {
                        newArray.add(new JsonPrimitive(text.replace("\\n", "\n")));
                    } else {
                        newArray.add(child);
                    }
                } else {
                    newArray.add(fixDoubleEscapedNewlines(child));
                }
            }
            return newArray;
        }
        return element;
    }

    private String convertIndentation(String json) {
        Pattern pattern = Pattern.compile("^( {4})+", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(json);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            int indentLevel = matcher.group().length() / 4;
            matcher.appendReplacement(sb, "  ".repeat(indentLevel));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String fixColonSpacing(String json) {
        return json.replaceAll("\"\\s*:\\s*", "\" : ");
    }
}
