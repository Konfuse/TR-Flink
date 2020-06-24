package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.dita.DITAGlobalIndex;
import com.konfuse.strtree.RTree;

/**
 * @author todd
 * @date 2020/5/20 21:24
 * @description: TODO
 */
public class GlobalIndexDITASerializer extends Serializer<DITAGlobalIndex> {
    @Override
    public void write(Kryo kryo, Output output, DITAGlobalIndex DITAGlobalIndex) {
        output.writeInt(DITAGlobalIndex.getGlobalPartitionNum());
        kryo.writeObject(output, DITAGlobalIndex.getSTRtreeFirst());
        kryo.writeObject(output, DITAGlobalIndex.getSTRtreeLast());
    }

    @Override
    public DITAGlobalIndex read(Kryo kryo, Input input, Class<DITAGlobalIndex> aClass) {
        int globalPartitionNum = input.readInt();
        RTree STRtreeFirst = kryo.readObject(input, RTree.class);
        RTree STRtreeLast = kryo.readObject(input, RTree.class);
        DITAGlobalIndex index = new DITAGlobalIndex(globalPartitionNum, STRtreeFirst, STRtreeLast);
        return index;
    }
}
