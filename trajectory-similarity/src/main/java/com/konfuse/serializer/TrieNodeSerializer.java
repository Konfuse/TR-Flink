package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.dita.*;
import com.konfuse.strtree.MBR;

import java.util.ArrayList;
import java.util.List;

import static com.konfuse.dita.TrieNodeType.TRIE_LEAF_NODE;

/**
 * @author todd
 * @date 2020/5/19 21:08
 * @description: TrieNode 序列化
 */
public class TrieNodeSerializer extends Serializer<TrieNode> {
    @Override
    public void write(Kryo kryo, Output output, TrieNode trieNode) {
        if(trieNode instanceof TrieInternalNode) {
            kryo.writeObject(output, trieNode.getType());
            output.writeInt(trieNode.getLevel());
            kryo.writeObject(output, trieNode.getMbr());
            output.writeInt(trieNode.getCurrentCapacity());
            List<TrieNode> childes = ((TrieInternalNode) trieNode).getChildes();
            for (int i = 0; i < trieNode.getCurrentCapacity(); i++) {
                kryo.writeObject(output, childes.get(i));
            }
        } else {
            kryo.writeObject(output, trieNode.getType());
            output.writeInt(trieNode.getLevel());
            kryo.writeObject(output, trieNode.getMbr());
            output.writeInt(trieNode.getCurrentCapacity());
            List<DITATrajectory> data = ((TrieLeafNode) trieNode).getData();
            for (int i = 0; i < trieNode.getCurrentCapacity(); i++) {
                kryo.writeObject(output, data.get(i));
            }
        }
    }

    @Override
    public TrieNode read(Kryo kryo, Input input, Class<TrieNode> aClass) {
        TrieNodeType type = kryo.readObject(input, TrieNodeType.class);
        int level = input.readInt();
        MBR mbr = kryo.readObject(input, MBR.class);
        int currentCapacity = input.readInt();
        if (type.equals(TRIE_LEAF_NODE)) {
            ArrayList<DITATrajectory> trajectories = new ArrayList<>(currentCapacity);
            for (int i = 0; i < currentCapacity; i++) {
                DITATrajectory data = kryo.readObject(input, DITATrajectory.class);
                trajectories.add(data);
            }
            TrieNode node = new TrieLeafNode(level, type, mbr, currentCapacity, trajectories);
            return node;
        } else {
            ArrayList<TrieNode> childes = new ArrayList<>(currentCapacity);
            for (int i = 0; i < currentCapacity; i++) {
                TrieNode node = kryo.readObject(input, TrieNode.class);
                childes.add(node);
            }
            TrieNode node = new TrieInternalNode(level, type, mbr, currentCapacity, childes);
            return node;
        }
    }
}
