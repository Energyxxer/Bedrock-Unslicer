package com.energyxxer.tools.bedrock.unslicer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

public class JsonTraverser {
    static final JsonTraverser INSTANCE = new JsonTraverser(null);

    private JsonElement root;
    private JsonElement neck;
    private JsonElement head;
    private boolean createOnTraversal;

    private boolean missingHead = false;
    private Object lastTraversalKey = null;

    private JsonTraverser(JsonElement root) {
        this.root = root;
        this.head = root;
        createOnTraversal = false;
    }

    public JsonTraverser reset() {
        this.head = root;
        createOnTraversal = false;
        return this;
    }

    JsonTraverser reset(JsonElement newHead) {
        this.head = newHead;
        createOnTraversal = false;
        missingHead = false;
        return this;
    }

    JsonTraverser get(String key) {
        restoreHead(JsonObject::new);

        if(head != null && head.isJsonObject() && head.getAsJsonObject().has(key)) {
            neck = head;
            head = head.getAsJsonObject().get(key);
        } else {
            deadEnd();
        }
        lastTraversalKey = key;
        return this;
    }

    public JsonTraverser get(int index) {
        restoreHead(JsonArray::new);

        if(head != null && index >= 0 && head.isJsonArray() && head.getAsJsonArray().size() > index) {
            neck = head;
            head = head.getAsJsonArray().get(index);
        } else {
            deadEnd();
        }
        lastTraversalKey = index;
        return this;
    }

    public JsonObject asJsonObject() {
        restoreHead(JsonObject::new);
        if(head != null && head.isJsonObject()) return (JsonObject) head;
        return null;
    }

    JsonArray asJsonArray() {
        restoreHead(JsonArray::new);
        if(head != null && head.isJsonArray()) return (JsonArray) head;
        return null;
    }

    public Iterable<Map.Entry<String, JsonElement>> iterateAsObject() {
        restoreHead(JsonObject::new);
        if(head != null && head.isJsonObject()) return head.getAsJsonObject().entrySet();
        return Collections.emptyList();
    }

    Iterable<JsonElement> iterateAsArray() {
        restoreHead(JsonArray::new);
        if(head != null && head.isJsonArray()) return head.getAsJsonArray();
        return Collections.emptyList();
    }

    String asString() {
        return asString(null);
    }

    String asString(String defaultValue) {
        restoreHead(() -> defaultValue != null ? new JsonPrimitive(defaultValue) : null);
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isString()) return head.getAsString();
        return defaultValue;
    }

    public boolean asBoolean() {
        return asBoolean(false);
    }

    boolean asBoolean(boolean defaultValue) {
        restoreHead(() -> new JsonPrimitive(defaultValue));
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isBoolean()) return head.getAsBoolean();
        return defaultValue;
    }

    public byte asByte() {
        return asByte((byte) 0);
    }

    private byte asByte(byte defaultValue) {
        restoreHead(() -> new JsonPrimitive(defaultValue));
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsByte();
        return defaultValue;
    }

    public short asShort() {
        return asShort((short) 0);
    }

    private short asShort(short defaultValue) {
        restoreHead(() -> new JsonPrimitive(defaultValue));
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsShort();
        return defaultValue;
    }

    public int asInt() {
        return asInt(0);
    }

    private int asInt(int defaultValue) {
        restoreHead(() -> new JsonPrimitive(defaultValue));
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsInt();
        return defaultValue;
    }

    public long asLong() {
        return asLong(0);
    }

    private long asLong(long defaultValue) {
        restoreHead(() -> new JsonPrimitive(defaultValue));
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsLong();
        return defaultValue;
    }

    public float asFloat() {
        return asFloat(0);
    }

    private float asFloat(float defaultValue) {
        restoreHead(() -> new JsonPrimitive(defaultValue));
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsFloat();
        return defaultValue;
    }

    public double asDouble() {
        return asDouble(0);
    }

    private double asDouble(double defaultValue) {
        restoreHead(() -> new JsonPrimitive(defaultValue));
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsDouble();
        return defaultValue;
    }

    public BigInteger asBigInt() {
        return asBigInt(BigInteger.ZERO);
    }

    private BigInteger asBigInt(BigInteger defaultValue) {
        restoreHead(() -> new JsonPrimitive(defaultValue));
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsBigInteger();
        return defaultValue;
    }

    public BigDecimal asBigDecimal() {
        return asBigDecimal(BigDecimal.ZERO);
    }

    private BigDecimal asBigDecimal(BigDecimal defaultValue) {
        restoreHead(() -> new JsonPrimitive(defaultValue));
        if(head != null && head.isJsonPrimitive() && head.getAsJsonPrimitive().isNumber()) return head.getAsBigDecimal();
        return defaultValue;
    }

    private void deadEnd() {
        neck = head;
        head = null;
        missingHead = true;
    }

    public JsonElement getHead() {
        return head;
    }

    public JsonTraverser createOnTraversal() {
        createOnTraversal = true;
        return this;
    }

    private void restoreHead(Supplier<JsonElement> newHeadSupplier) {
        if(createOnTraversal && missingHead && neck != null) {
            JsonElement newHead = newHeadSupplier.get();
            if(newHead == null) return;
            head = newHead;
            missingHead = false;
            if (lastTraversalKey instanceof String) { //Last traversal should have been via a key
                neck.getAsJsonObject().add((String) lastTraversalKey, head);
            } else { //Last traversal should have been via an index
                JsonArray neckArr = neck.getAsJsonArray();
                while (neckArr.size() <= ((int) lastTraversalKey)) {
                    neckArr.add(0);
                }
                neckArr.set((int) lastTraversalKey, head);
            }
        }
    }
}