package com.learning.systemdesign.consistenthashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ConsistentHashing {
    private final TreeMap<Long, String> ring;
    private final int ringSize;
    private final int numberOfReplicas;
    private final MessageDigest md;

    public ConsistentHashing(int numberOfReplicas) throws NoSuchAlgorithmException {
        this.ring = new TreeMap<>();
        this.numberOfReplicas = numberOfReplicas;
        this.md = MessageDigest.getInstance("MD5");
        this.ringSize = 256;
    }

    public void addServer(String server) {
        for (int i = 0; i < numberOfReplicas; i++) {
            long hash = generateHash(server + i);
            ring.put(hash, server);
        }
    }

    public void removeServer(String server) {
        for (int i = 0; i < numberOfReplicas; i++) {
            long hash = generateHash(server + i);
            ring.remove(hash);
        }
    }

    public String getServer(String key) {
        if (ring.isEmpty()) {
            return null;
        }
        long hash = generateHash(key);
        if (!ring.containsKey(hash)) {
            SortedMap<Long, String> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        return ring.get(hash);
    }

    public TreeMap<Long, String> getMap(){
        return this.ring;
    }

    private long generateHash(String key) {
        md.reset();
        md.update(key.getBytes());
        byte[] digest = md.digest();
        long hash = ((long) (digest[3] & 0xFF) << 24) |
                ((long) (digest[2] & 0xFF) << 16) |
                ((long) (digest[1] & 0xFF) << 8) |
                ((long) (digest[0] & 0xFF));
        return hash % ringSize;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        ConsistentHashing ch = new ConsistentHashing(3);
        ch.addServer("server1");
        ch.addServer("server2");
        ch.addServer("server3");
        ch.addServer("server4");

        System.out.println("key1: is present on server: " + ch.getServer("key1"));
        System.out.println("testkey1: is present on server: " + ch.getServer("testkey1"));
        System.out.println("key100: is present on server: " + ch.getServer("key100"));
        System.out.println("key5000: is present on server: " + ch.getServer("key5000"));
        System.out.println("key67890: is present on server: " + ch.getServer("key67890"));

        ch.removeServer("server3");
        System.out.println("After removing server3");
        System.out.println("key1: is present on server: " + ch.getServer("key1"));
        System.out.println("testkey1: is present on server: " + ch.getServer("testkey1"));
        System.out.println("key100: is present on server: " + ch.getServer("key100"));
        System.out.println("key5000: is present on server: " + ch.getServer("key5000"));
        System.out.println("key67890: is present on server: " + ch.getServer("key67890"));
    }
}