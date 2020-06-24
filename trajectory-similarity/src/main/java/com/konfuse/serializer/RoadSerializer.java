package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.road.Road;

/**
 * @author todd
 * @date 2020/6/6 22:50
 * @description: TODO
 */
public class RoadSerializer extends Serializer<Road> {
    @Override
    public void write(Kryo kryo, Output output, Road road) {
        kryo.writeObject(output, road);
    }

    @Override
    public Road read(Kryo kryo, Input input, Class<Road> aClass) {
        Road p = kryo.readObject(input, Road.class);
        return p;
    }
}
