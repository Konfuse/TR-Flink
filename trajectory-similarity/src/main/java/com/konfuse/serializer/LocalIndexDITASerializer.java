package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.dita.DITALocalIndex;
import com.konfuse.dita.TrieNode;

/**
 * @author todd
 * @date 2020/5/20 21:24
 * @description: TODO
 */
public class LocalIndexDITASerializer extends Serializer<DITALocalIndex> {
    @Override
    public void write(Kryo kryo, Output output, DITALocalIndex DITALocalIndex) {
        output.writeInt(DITALocalIndex.getLocalIndexedPivotSize());
        output.writeInt(DITALocalIndex.getLocalMinNodeSize());
        output.writeInt(DITALocalIndex.getPartitionID());
        kryo.writeObject(output, DITALocalIndex.getRoot());
    }

    @Override
    public DITALocalIndex read(Kryo kryo, Input input, Class<DITALocalIndex> aClass) {
        int localIndexedPivotSize = input.readInt();
        int localMinNodeSize = input.readInt();
        int partitionID = input.readInt();
        TrieNode root = kryo.readObject(input, TrieNode.class);
        DITALocalIndex index = new DITALocalIndex(localIndexedPivotSize, localMinNodeSize, partitionID, root);
        return index;
    }
}
