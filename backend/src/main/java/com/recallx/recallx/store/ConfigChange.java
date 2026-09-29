package com.recallx.recallx.store;

public record ConfigChange(String keyName, String oldValue, String newValue) {
}
