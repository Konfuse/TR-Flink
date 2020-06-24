package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.geometry.Rectangle;

/**
 * @author todd
 * @date 2020/5/21 10:50
 * @description: TODO
 */
public class RectangleSerializer extends Serializer<Rectangle> {
    @Override
    public void write(Kryo kryo, Output output, Rectangle object) {
        kryo.writeObject(output, object);
    }

    @Override
    public Rectangle read(Kryo kryo, Input input, Class<Rectangle> type) {
        Rectangle p = kryo.readObject(input, Rectangle.class);
        return p;
    }
}
