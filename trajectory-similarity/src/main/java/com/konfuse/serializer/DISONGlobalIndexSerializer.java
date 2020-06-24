package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.dison.DISONGlobalIndex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author todd
 * @date 2020/5/31 22:09
 * @description: TODO
 */
public class DISONGlobalIndexSerializer extends Serializer<DISONGlobalIndex> {
    @Override
    public void write(Kryo kryo, Output output, DISONGlobalIndex disonGlobalIndex) {
        output.writeInt(disonGlobalIndex.getGlobalPartitionNum());
        output.writeInt(disonGlobalIndex.getPartitionSize());
        HashMap<Long, Integer> firstLevelMap = disonGlobalIndex.getFirstLevelMap();
        output.writeInt(firstLevelMap.entrySet().size());
        for (Map.Entry<Long, Integer> longIntegerEntry : firstLevelMap.entrySet()) {
            output.writeLong(longIntegerEntry.getKey());
            output.writeInt(longIntegerEntry.getValue());
        }

        HashMap<Integer, HashMap<Long, Integer>> secondLevelMap = disonGlobalIndex.getSecondLevelMap();
        output.writeInt(secondLevelMap.entrySet().size());
        for (Map.Entry<Integer, HashMap<Long, Integer>> integerHashMapEntry : secondLevelMap.entrySet()) {
            output.writeInt(integerHashMapEntry.getKey());
            HashMap<Long, Integer> map = integerHashMapEntry.getValue();
            output.writeInt(map.size());
            for (Map.Entry<Long, Integer> longIntegerEntry : map.entrySet()) {
                output.writeLong(longIntegerEntry.getKey());
                output.writeInt(longIntegerEntry.getValue());
            }
        }

        HashMap<Integer , Set<Long>> partitionedVertices = disonGlobalIndex.getPartitionedVertices();
        output.writeInt(partitionedVertices.entrySet().size());
        for (Map.Entry<Integer, Set<Long>> integerSetEntry : partitionedVertices.entrySet()) {
            output.writeInt(integerSetEntry.getKey());
            Set<Long> idSet = integerSetEntry.getValue();
            output.writeInt(idSet.size());
            for (Long aLong : idSet) {
                output.writeLong(aLong);
            }
        }

        HashMap<Integer, Double> longestLengthOfPartition = disonGlobalIndex.getLongestLengthOfPartition();
        output.writeInt(longestLengthOfPartition.size());
        for (Map.Entry<Integer, Double> integerDoubleEntry : longestLengthOfPartition.entrySet()) {
            output.writeInt(integerDoubleEntry.getKey());
            output.writeDouble(integerDoubleEntry.getValue());
        }
    }

    @Override
    public DISONGlobalIndex read(Kryo kryo, Input input, Class<DISONGlobalIndex> aClass) {
        int globalPartitionNum = input.readInt();
        int partitionSize = input.readInt();

        HashMap<Long, Integer> firstLevelMap = new HashMap<>();
        int firstLevelMapSize = input.readInt();
        for (int i = 0; i < firstLevelMapSize; i++) {
            long id = input.readInt();
            int partitionId = input.readInt();
            firstLevelMap.put(id, partitionId);
        }

        HashMap<Integer, HashMap<Long, Integer>> secondLevelMap = new HashMap<>();
        int secondLevelMapSize = input.readInt();
        for (int i = 0; i < secondLevelMapSize; i++) {
            int key = input.readInt();
            HashMap<Long, Integer> levelMap = new HashMap<>();
            int levelMapSize = input.readInt();
            for (int j = 0; j < levelMapSize; j++) {
                long id = input.readLong();
                int partitionId = input.readInt();
                levelMap.put(id, partitionId);
            }
            secondLevelMap.put(key, levelMap);
        }

        HashMap<Integer , Set<Long>> partitionedVertices = new HashMap<>();
        int partitionedVerticesSize = input.readInt();
        for (int i = 0; i < partitionedVerticesSize; i++) {
            int key = input.readInt();
            HashSet<Long> idSet = new HashSet<>();
            int partitionIdSize = input.readInt();
            for (int j = 0; j < partitionIdSize; j++) {
                long id = input.readLong();
                idSet.add(id);
            }
            partitionedVertices.put(key, idSet);
        }
        HashMap<Integer, Double> longestLengthOfPartition = new HashMap<>();
        int longestLengthOfPartitionSize = input.readInt();
        for (int i = 0; i < longestLengthOfPartitionSize; i++) {
            int partitionId = input.readInt();
            double dist = input.readDouble();
            longestLengthOfPartition.put(partitionId, dist);
        }
        return new DISONGlobalIndex(globalPartitionNum, firstLevelMap, secondLevelMap, partitionedVertices, longestLengthOfPartition);
    }
}
