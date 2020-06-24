package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.dison.DISONEdge;
import com.konfuse.geometry.Point;

/**
 * @author todd
 * @date 2020/5/31 22:10
 * @description: TODO
 */
public class DISONEdgeSerializer extends Serializer<DISONEdge> {
    @Override
    public void write(Kryo kryo, Output output, DISONEdge disonEdge) {
        output.writeLong(disonEdge.getEdgeId());
        output.writeDouble(disonEdge.getLength());
        kryo.writeObject(output, disonEdge.getStart());
        kryo.writeObject(output, disonEdge.getEnd());
    }

    @Override
    public DISONEdge read(Kryo kryo, Input input, Class<DISONEdge> aClass) {
        Long id = input.readLong();
        Double length = input.readDouble();
        Point start = kryo.readObject(input, Point.class);
        Point end = kryo.readObject(input, Point.class);
        return new DISONEdge(id, start, end,length);
    }
}
