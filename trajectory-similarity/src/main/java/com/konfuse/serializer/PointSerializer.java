package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.geometry.Point;

/**
 * @author todd
 * @date 2020/5/19 16:16
 * @description: Point 序列化
 */
public class PointSerializer extends Serializer<Point> {
    @Override
    public void write(Kryo kryo, Output output, Point object) {
        kryo.writeObject(output, object);
    }

    @Override
    public Point read(Kryo kryo, Input input, Class<Point> type) {
        Point p = kryo.readObject(input, Point.class);
        return p;
    }
}
