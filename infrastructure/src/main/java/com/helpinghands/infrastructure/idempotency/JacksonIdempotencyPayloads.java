package com.helpinghands.infrastructure.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.helpinghands.core.port.out.IdempotencyPayloads;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

@Component
class JacksonIdempotencyPayloads implements IdempotencyPayloads {

    private final ObjectMapper objectMapper;

    JacksonIdempotencyPayloads(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String fingerprint(Object request) {
        String canonical = canonical(objectMapper.valueToTree(request)).toString();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String serialize(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot snapshot response", e);
        }
    }

    @Override
    public <T> T deserialize(String snapshot, Class<T> type) {
        try {
            return objectMapper.readValue(snapshot, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read stored response", e);
        }
    }

    // keys sorted and numbers normalised, so 10 and 10.00 or a different field order hash the same
    private JsonNode canonical(JsonNode node) {
        JsonNodeFactory factory = objectMapper.getNodeFactory();
        if (node.isObject()) {
            ObjectNode sorted = factory.objectNode();
            var fields = new TreeMap<String, JsonNode>();
            node.fields().forEachRemaining(entry -> fields.put(entry.getKey(), entry.getValue()));
            fields.forEach((name, child) -> sorted.set(name, canonical(child)));
            return sorted;
        }
        if (node.isArray()) {
            ArrayNode array = factory.arrayNode();
            node.forEach(child -> array.add(canonical(child)));
            return array;
        }
        if (node.isNumber()) {
            BigDecimal number = node.decimalValue();
            return factory.textNode(number.signum() == 0 ? "0" : number.stripTrailingZeros().toPlainString());
        }
        return node;
    }
}
