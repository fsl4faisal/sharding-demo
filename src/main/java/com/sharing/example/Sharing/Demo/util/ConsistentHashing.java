package com.sharing.example.Sharing.Demo.util;

import lombok.Builder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;

@Builder
public class ConsistentHashing {
    private static final Log LOG = LogFactory.getLog(ConsistentHashing.class);
    public static final String SERVER_NAME = "Shard";
    private final TreeMap<Long, String> ring;
    private final int numberOfReplicas;
    private static final MessageDigest messageDigest = getInstance();


    private static MessageDigest getInstance() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public ConsistentHashing() {
        this(new TreeMap<>(), 3);
    }

    public ConsistentHashing(int numberOfReplicas) {
        this(new TreeMap<>(), numberOfReplicas);
    }

    public ConsistentHashing(TreeMap<Long, String> ring, int numberOfReplicas) {
        this.ring = ring;
        this.numberOfReplicas = numberOfReplicas;

        for (int i = 0; i < numberOfReplicas; i++) {
            addServer(i);
        }
    }

    public void addServer(int i) {
        var serverName = SERVER_NAME + i;
        var hash = generateHash(SERVER_NAME + i);
        ring.put(hash, serverName);
        LOG.info(String.format("Server %d Added successfully", i));
    }

    public boolean removeServer(int serverId) {
        var serverName = SERVER_NAME + serverId;
        var hash = generateHash(serverName);
        return ring.remove(hash) == null ?
                false :
                true;
    }


    public String getServer(String key) {
        if (ring.isEmpty()) {
            return null;
        } else {
            var hash = generateHash(key);
            if (!ring.containsKey(hash)) {
                var sortedMap = ring.tailMap(hash);
                hash = sortedMap.isEmpty() ?
                        ring.firstKey() :
                        sortedMap.firstKey();
            }
            return ring.get(hash);
        }
    }


    private long generateHash(String str) {
        messageDigest.reset();
        messageDigest.update(str.getBytes(StandardCharsets.UTF_8));
        byte[] digest = messageDigest.digest();
        long hash = ((long) (digest[3] & 0xFF) << 24) |
                ((long) (digest[2] & 0xFF) << 16) |
                ((long) (digest[1] & 0xFF) << 8) |
                ((long) (digest[0] & 0xFF));
        return hash;
    }
}
