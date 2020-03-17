/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.gateway.impl.broker.cluster;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.agrona.collections.Int2IntHashMap;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.IntArrayList;

public final class BrokerClusterStateImpl implements BrokerClusterState {

  private final Int2IntHashMap partitionLeaders;
  private final Int2ObjectHashMap<Long> partitionLeaderTerms;
  private final Int2ObjectHashMap<List<Integer>> partitionFollowers;
  private final Int2ObjectHashMap<String> brokerAddresses;
  private final IntArrayList brokers;
  private final IntArrayList partitions;
  private final Random randomBroker;
  private int clusterSize;
  private int partitionsCount;
  private int replicationFactor;

  public BrokerClusterStateImpl(final BrokerClusterStateImpl topology) {
    this();
    if (topology != null) {
      partitionLeaders.putAll(topology.partitionLeaders);
      partitionLeaderTerms.putAll(topology.partitionLeaderTerms);
      partitionFollowers.putAll(topology.partitionFollowers);
      brokerAddresses.putAll(topology.brokerAddresses);

      brokers.addAll(topology.brokers);
      partitions.addAll(topology.partitions);

      clusterSize = topology.clusterSize;
      partitionsCount = topology.partitionsCount;
      replicationFactor = topology.replicationFactor;
    }
  }

  public BrokerClusterStateImpl() {
    partitionLeaders = new Int2IntHashMap(NODE_ID_NULL);
    partitionLeaderTerms = new Int2ObjectHashMap<>();
    partitionFollowers = new Int2ObjectHashMap<>();
    brokerAddresses = new Int2ObjectHashMap<>();
    brokers = new IntArrayList(5, NODE_ID_NULL);
    partitions = new IntArrayList(32, PARTITION_ID_NULL);
    randomBroker = new Random();
  }

  public void setPartitionLeader(final int partitionId, final int leaderId, final long term) {
    if (partitionLeaderTerms.getOrDefault(partitionId, -1L) < term) {
      partitionLeaders.put(partitionId, leaderId);
      partitionLeaderTerms.put(partitionId, Long.valueOf(term));
      final List<Integer> followers = partitionFollowers.get(partitionId);
      if (followers != null) {
        followers.removeIf(follower -> follower == leaderId);
      }
    }
  }

  public void addPartitionFollower(final int partitionId, final int followerId) {
    partitionFollowers.computeIfAbsent(partitionId, ArrayList::new).add(followerId);
  }

  public void addPartitionIfAbsent(final int partitionId) {
    if (partitions.indexOf(partitionId) == -1) {
      partitions.addInt(partitionId);
    }
  }

  public void addBrokerIfAbsent(final int nodeId) {
    if (brokerAddresses.get(nodeId) == null) {
      brokerAddresses.put(nodeId, "");
      brokers.addInt(nodeId);
    }
  }

  public void setBrokerAddressIfPresent(final int brokerId, final String address) {
    if (brokerAddresses.get(brokerId) != null) {
      brokerAddresses.put(brokerId, address);
    }
  }

  public void removeBroker(final int brokerId) {
    brokerAddresses.remove(brokerId);
    brokers.removeInt(brokerId);
    partitions.forEachOrderedInt(
        partitionId -> {
          if (partitionLeaders.get(partitionId) == brokerId) {
            partitionLeaders.remove(partitionId);
          }
          final List<Integer> followers = partitionFollowers.get(partitionId);
          if (followers != null) {
            followers.remove(new Integer(brokerId));
          }
        });
  }

  @Override
  public int getClusterSize() {
    return clusterSize;
  }

  public void setClusterSize(final int clusterSize) {
    this.clusterSize = clusterSize;
  }

  @Override
  public int getPartitionsCount() {
    return partitionsCount;
  }

  public void setPartitionsCount(final int partitionsCount) {
    this.partitionsCount = partitionsCount;
  }

  @Override
  public int getReplicationFactor() {
    return replicationFactor;
  }

  public void setReplicationFactor(final int replicationFactor) {
    this.replicationFactor = replicationFactor;
  }

  @Override
  public int getLeaderForPartition(final int partition) {
    return partitionLeaders.get(partition);
  }

  @Override
  public List<Integer> getFollowersForPartition(final int partition) {
    return partitionFollowers.get(partition);
  }

  @Override
  public int getRandomBroker() {
    if (brokers.isEmpty()) {
      return UNKNOWN_NODE_ID;
    } else {
      return brokers.get(randomBroker.nextInt(brokers.size()));
    }
  }

  @Override
  public List<Integer> getPartitions() {
    return partitions;
  }

  @Override
  public List<Integer> getBrokers() {
    return brokers;
  }

  @Override
  public String getBrokerAddress(final int brokerId) {
    return brokerAddresses.get(brokerId);
  }

  @Override
  public int getPartition(final int index) {
    if (!partitions.isEmpty()) {
      return partitions.getInt(index % partitions.size());
    } else {
      return PARTITION_ID_NULL;
    }
  }

  @Override
  public String toString() {
    return "BrokerClusterStateImpl{"
        + "partitionLeaders="
        + partitionLeaders
        + ", brokers="
        + brokers
        + ", partitions="
        + partitions
        + ", clusterSize="
        + clusterSize
        + ", partitionsCount="
        + partitionsCount
        + ", replicationFactor="
        + replicationFactor
        + '}';
  }
}
