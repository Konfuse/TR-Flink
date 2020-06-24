package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.dison.DISONEdge;
import com.konfuse.dison.DISONTrajectory;
import com.konfuse.geometry.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * @author todd
 * @date 2020/5/31 22:10
 * @description: TODO
 */
public class DISONTrajectorySerializer extends Serializer<DISONTrajectory> {
    @Override
    public void write(Kryo kryo, Output output, DISONTrajectory trajectory) {
        output.writeInt(trajectory.getId());
        output.writeDouble(trajectory.getLength());
        List<Point> globalIndexedPivot = trajectory.getGlobalIndexedPivot();
        output.writeInt(globalIndexedPivot.size());
        for (int i = 0; i < globalIndexedPivot.size(); i++) {
            kryo.writeObject(output, globalIndexedPivot.get(i));
        }
        List<DISONEdge> trajectoryData = trajectory.getTrajectoryData();
        output.writeInt(trajectoryData.size());
        for (DISONEdge trajectoryDatum : trajectoryData) {
            kryo.writeObject(output, trajectoryDatum);
        }
    }

    @Override
    public DISONTrajectory read(Kryo kryo, Input input, Class<DISONTrajectory> aClass) {
        int id = input.readInt();
        double length = input.readDouble();
        int globalIndexedPivotSize = input.readInt();
        ArrayList<Point> globalIndexedPivots = new ArrayList<>(globalIndexedPivotSize);
        for (int i = 0; i < globalIndexedPivotSize; i++) {
            Point point = kryo.readObject(input, Point.class);
            globalIndexedPivots.add(point);
        }
        int trajectoryDataSize = input.readInt();
        ArrayList<DISONEdge> trajectories = new ArrayList<>(trajectoryDataSize);
        for (int i = 0; i < trajectoryDataSize; i++) {
            DISONEdge edge = kryo.readObject(input, DISONEdge.class);
            trajectories.add(edge);
        }
        return new DISONTrajectory(id, length, trajectories, globalIndexedPivots);
    }
}
