package com.example.productai.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private JsonUtils() {}
    public static JsonNode readTree(String json) throws Exception { return MAPPER.readTree(json); }
}
