package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.strtree.*;

/**
 * @author todd
 * @date 2020/5/20 21:35
 * @description: TODO
 */
public class RTreeSerializer extends Serializer<RTree> {
    @Override
    public void write(Kryo kryo, Output output, RTree object) {
        output.writeInt(object.getMaxNodeNb());
        output.writeInt(object.getMinNodeNb());
        output.writeInt(object.getRoot().getTypeNum());
        kryo.writeObject(output, object.getRoot());
    }

    @Override
    public RTree read(Kryo kryo, Input input, Class<RTree> type) {
        int M = input.readInt();
        int m = input.readInt();
        int typeNum = input.readInt();
        RTreeNode root = null;
        if (typeNum == 1 || typeNum == 2) {
            root = kryo.readObject(input, RTreeInternalNode.class);
        } else if (typeNum == 3) {
            root = kryo.readObject(input, RTreePointLeafNode.class);
        } else if (typeNum == 4) {
            root = kryo.readObject(input, RTreeLineLeafNode.class);
        } else {
            root = kryo.readObject(input, RTreeRectLeafNode.class);
        }
        return new RTree(root, M, m);
    }
}
