package com.trt.contentengagement.media.application;

public interface MediaBinaryStorage {
    void store(String storageKey, byte[] content);
    byte[] read(String storageKey);
}
