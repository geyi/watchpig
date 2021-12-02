package com.airing.msg.service;

import com.airing.entity.BaseMsg;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CallbackHandler {

    private static final ConcurrentHashMap<Long, CompletableFuture<String>> mapping = new ConcurrentHashMap<>();

    public static void add(Long requestId, CompletableFuture<String> cf) {
        mapping.putIfAbsent(requestId, cf);
    }

    public static void remove(Long requestId) {
        mapping.remove(requestId);
    }

    public static void run(BaseMsg pkg) {
        mapping.get(pkg.getRequestId()).complete(pkg.getContent());
        remove(pkg.getRequestId());
    }

}
