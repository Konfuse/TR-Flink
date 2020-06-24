package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.geometry.Line;

/**
 * @author todd
 * @date 2020/5/21 10:48
 * @description: TODO
 */
public class LineSerializer extends Serializer<Line> {
    @Override
    public void write(Kryo kryo, Output output, Line object) {
        kryo.writeObject(output, object);
    }

    @Override
    public Line read(Kryo kryo, Input input, Class<Line> type) {
        Line p = kryo.readObject(input, Line.class);
        return p;
    }
}
