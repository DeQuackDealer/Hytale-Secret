package com.hypixel.hytale.codec.builder;

public interface BuilderCodec<T> {
    T decode(String data);
    String encode(T value);
}
