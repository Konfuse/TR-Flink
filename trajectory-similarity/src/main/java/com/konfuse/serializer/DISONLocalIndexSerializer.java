package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.dison.DISONLocalIndex;
import com.konfuse.dison.DISONTrajectory;
import org.apache.flink.api.java.tuple.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author todd
 * @date 2020/5/31 22:09
 * @description: TODO
 */
public class DISONLocalIndexSerializer extends Serializer<DISONLocalIndex> {
    @Override
    public void write(Kryo kryo, Output output, DISONLocalIndex disonLocalIndex) {
        output.writeInt(disonLocalIndex.getPartitionID());
        HashMap<Long, List<Tuple2<DISONTrajectory, Double>>> InvertedIndex = disonLocalIndex.getInvertedIndex();
        output.writeInt(InvertedIndex.entrySet().size());
        for (Map.Entry<Long, List<Tuple2<DISONTrajectory, Double>>> longListEntry : InvertedIndex.entrySet()) {
            output.writeLong(longListEntry.getKey());
            List<Tuple2<DISONTrajectory, Double>> list = longListEntry.getValue();
            output.writeInt(list.size());
            for (Tuple2<DISONTrajectory, Double> tuple : list) {
                kryo.writeObject(output, tuple.f0);
                output.writeDouble(tuple.f1);
            }
        }
    }

    @Override
    public DISONLocalIndex read(Kryo kryo, Input input, Class<DISONLocalIndex> aClass) {
        int partitionID = input.readInt();
        HashMap<Long, List<Tuple2<DISONTrajectory, Double>>> InvertedIndex = new HashMap<>();
        int InvertedIndexSize = input.readInt();
        for (int i = 0; i < InvertedIndexSize; i++) {
            long key = input.readLong();
            int listSize = input.readInt();
            List<Tuple2<DISONTrajectory, Double>> list = new ArrayList<>(listSize);
            for (int j = 0; j < listSize; j++) {
                DISONTrajectory data = kryo.readObject(input, DISONTrajectory.class);
                double dist = input.readDouble();
                list.add(new Tuple2<>(data, dist));
            }
            InvertedIndex.put(key, list);
        }
        return new DISONLocalIndex(partitionID, InvertedIndex);
    }
}
