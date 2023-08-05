package com.learning.systemdesign.consistenthashing;

import org.javatuples.Pair;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ConsistentHashing {
    private final TreeMap<Long, Pair<String, List<String>>> ring;
    private final int ringSize;
    private final int noOfVirtualNodes;
    private final int noOfReplicas;

    private final MessageDigest md;

    public ConsistentHashing(int noOfVirtualNodes, int noOfReplicas) throws NoSuchAlgorithmException {
        this.ring = new TreeMap<>();
        this.noOfVirtualNodes = noOfVirtualNodes;
        this.noOfReplicas = noOfReplicas;
        this.md = MessageDigest.getInstance("MD5");
        this.ringSize = 1024;
    }

    public void addServer(String server) {
        for (int i = 0; i < noOfVirtualNodes; i++) {
            for(int j = 0; j < noOfReplicas; j++){
                // All virtual nodes with replica 0 is master virtual node
                // Generates different hash values for each virtual node and replica
                long hash = generateHash(server + i, j);
                Pair<String, List<String>> dataMap = new Pair<>(server, new ArrayList<>());
                ring.put(hash, dataMap);
            }
        }
    }

    public void removeServer(String server) {
        for (int i = 0; i < noOfVirtualNodes; i++) {
            // Server with 0 replica id is master virtual node
            for(int j = 0; j < noOfReplicas; j++) {
                long hash = generateHash(server + i, j);
                ring.remove(hash);
            }
        }
    }

    public String getServer(String key) {
        if (ring.isEmpty()) {
            return null;
        }
        long hash = generateHash(key, 0);
        if (!ring.containsKey(hash)) {
            // SortedMap<Long, String> tailMap = ring.tailMap(hash);
            final SortedMap<Long, Pair<String, List<String>>> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        return ring.get(hash).getValue0();
    }

    public String getReplica(String key, int replicaId) {
        if (ring.isEmpty()) {
            return null;
        }
        long hash = generateHash(key, replicaId);
        if (!ring.containsKey(hash)) {
            final SortedMap<Long, Pair<String, List<String>>> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        return ring.get(hash).getValue0();
    }

    private long generateHash(String key, int replica) {
        md.reset();
        String digestKey = key + String.valueOf(replica);
        md.update(digestKey.getBytes());
        byte[] digest = md.digest();
        long hash = ((long) (digest[3] & 0xFF) << 24) |
                ((long) (digest[2] & 0xFF) << 16) |
                ((long) (digest[1] & 0xFF) << 8) |
                ((long) (digest[0] & 0xFF));
        return hash % ringSize;
    }

    private void addData(String data, int replica){
        if (ring.isEmpty()) {
            return;
        }
        long hash = generateHash(data, replica);
        if (!ring.containsKey(hash)) {
            final SortedMap<Long, Pair<String, List<String>>> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        List<String> list = (List<String>) ring.get(hash).getValue(1);
        list.add(data);
    }

    private boolean contains(String data, int replica){
        if (ring.isEmpty()) {
            return false;
        }
        long hash = generateHash(data, replica);
        if (!ring.containsKey(hash)) {
            final SortedMap<Long, Pair<String, List<String>>> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        List<String> list = (List<String>) ring.get(hash).getValue(1);
        return list.contains(data);
    }

    private List<String> getData(String data, int replica){
        if (ring.isEmpty()) {
            return null;
        }
        long hash = generateHash(data, replica);
        if (!ring.containsKey(hash)) {
            final SortedMap<Long, Pair<String, List<String>>> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        return (List<String>) ring.get(hash).getValue(1);
    }



    public static void main(String[] args) throws NoSuchAlgorithmException {
        ConsistentHashing ch = new ConsistentHashing(5, 3);
        ch.addServer("server1");
        ch.addServer("server2");
        ch.addServer("server3");
        ch.addServer("server4");
        ch.addServer("server5");

        System.out.println("key101: is present on server: " + ch.getServer("key101"));
        System.out.println("key101: replica 1 will be added on to server: " + ch.getReplica("key101", 1));
        System.out.println("key101: replica 2 will be added on to server: " + ch.getReplica("key101", 2));
        System.out.println("key101: replica 3 will be added on to server: " + ch.getReplica("key101", 3));
        System.out.println("key101 added to master");
        ch.addData("key101", 0);
        System.out.println("key101 added to replica 1");
        ch.addData("key101", 1);
        System.out.println("key101 is not added to replica 2 intentionally");
        System.out.println("key101 added to replica 3");
        ch.addData("key101", 3);
        List<String> replica1Data = ch.getData("key101", 1);
        List<String> replica2Data = ch.getData("key101", 2);
        List<String> replica3Data = ch.getData("key101", 3);
        System.out.println("Key101 data is fetched from all replicas");
        System.out.println(" Replica 1 data " + replica1Data);
        System.out.println(" Replica 2 data " + replica2Data);
        System.out.println(" Replica 3 data " + replica3Data);

        System.out.println("key5000: is present on server: " + ch.getServer("key5000"));
        System.out.println("key5000: replica 1 is present on server: " + ch.getReplica("key5000", 1));
        System.out.println("key5000: replica 2 is present on server: " + ch.getReplica("key5000", 2));
        System.out.println("key5000: replica 3 is present on server: " + ch.getReplica("key5000", 3));

        ch.removeServer("server3");
        System.out.println("After removing server3");
        System.out.println("key101: will be rebalanced to server: " + ch.getServer("key101"));
        System.out.println("Key101 data is fetched from all replicas");
        replica1Data = ch.getData("key101", 1);
        replica2Data = ch.getData("key101", 2);
        replica3Data = ch.getData("key101", 3);
        System.out.println("Replica 1 data " + replica1Data);
        System.out.println("Replica 2 data " + replica2Data);
        System.out.println("Replica 3 data " + replica3Data);

        System.out.println("key5000: is present on server: " + ch.getServer("key5000"));
    }
}