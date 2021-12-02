package com.airing;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NettySocketHolder {

	private static final Logger log = LoggerFactory.getLogger(NettySocketHolder.class);

	private static Map<String, Channel> MAP = new ConcurrentHashMap<>();

	public static void put(String id, Channel socketChannel) {
		MAP.put(id, socketChannel);
		log.debug("put id: {}, channel: {}", id, socketChannel.toString());
		printMap();
	}

	public static boolean syncPut(String id, Channel socketChannel) {
		if (!NettySocketHolder.containsKey(id)) {
			synchronized (NettySocketHolder.class) {
				if (!NettySocketHolder.containsKey(id)) {
					NettySocketHolder.put(id, socketChannel);
					return true;
				} else {
					return false;
				}
			}
		} else {
			return false;
		}
	}

	public static Channel get(String id) {
		return MAP.get(id);
	}

	public static Map<String, Channel> getMAP() {
		return MAP;
	}

	public static String remove(Channel socketChannel) {
		final String[] key = new String[1];
		MAP.entrySet().stream()
			.filter(entry -> entry.getValue() == socketChannel)
			.forEach((entry) -> {
				MAP.remove(entry.getKey());
				key[0] = entry.getKey();
			});
		printMap();
		return key[0];
	}

	public static boolean containsKey(String id) {
		return MAP.containsKey(id);
	}

	private static void printMap() {
		log.debug("socket map: {}", MAP);
	}
}
