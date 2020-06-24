package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.dita.DITATrajectory;
import com.konfuse.geometry.Point;
import com.konfuse.strtree.MBR;
import com.konfuse.util.Tuple;

import java.util.ArrayList;
import java.util.List;

/**
 * @author todd
 * @date 2020/5/19 15:11
 * @description: DITATrajectory 序列化
 */
public class DITATrajectorySerializer extends Serializer<DITATrajectory> {
    @Override
    public void write(Kryo kryo, Output output, DITATrajectory object) {
        output.writeInt(object.getId());
        output.writeInt(object.getNum());
        kryo.writeObject(output, object.getMbr());
        kryo.writeObject(output, object.getExtendedMBR());

        List<Tuple<MBR, Integer>> cells = object.getCells();
        output.writeInt(cells.size());
        for (int i = 0; i < cells.size(); i++) {
            kryo.writeObject(output, cells.get(i).f0);
            kryo.writeObject(output, cells.get(i).f1);
        }

        List<Point> trajectoryData = object.getTrajectoryData();
        output.writeInt(trajectoryData.size());
        for (int i = 0; i < trajectoryData.size(); i++) {
            kryo.writeObject(output, trajectoryData.get(i));
        }

        List<Point> localIndexedPivot = object.getLocalIndexedPivot();
        output.writeInt(localIndexedPivot.size());
        for (int i = 0; i < localIndexedPivot.size(); i++) {
            kryo.writeObject(output, localIndexedPivot.get(i));
        }

        List<Point> globalIndexedPivot = object.getGlobalIndexedPivot();
        output.writeInt(globalIndexedPivot.size());
        for (int i = 0; i < globalIndexedPivot.size(); i++) {
            kryo.writeObject(output, globalIndexedPivot.get(i));
        }

    }

    @Override
    public DITATrajectory read(Kryo kryo, Input input, Class<DITATrajectory> type) {

        int id = input.readInt();
        int num = input.readInt();
        MBR mbr = kryo.readObject(input, MBR.class);
        MBR extendedMBR = kryo.readObject(input, MBR.class);

        int numCells = input.readInt();
        List<Tuple<MBR, Integer>> cells = new ArrayList<>(numCells);
        for (int i = 0; i < numCells; i++) {
            MBR cellMBR = kryo.readObject(input, MBR.class);
            int pointNums = input.readInt();
            cells.add(new Tuple<>(cellMBR, pointNums));
        }

        int trajectorySize = input.readInt();
        List<Point> trajectoryData = new ArrayList<>();
        for (int i = 0; i < trajectorySize; i++) {
            Point point = kryo.readObject(input, Point.class);
            trajectoryData.add(point);
        }

        int localIndexedPivotSize = input.readInt();
        List<Point> localIndexedPivot = new ArrayList<>();
        for (int i = 0; i < localIndexedPivotSize; i++) {
            Point point = kryo.readObject(input, Point.class);
            localIndexedPivot.add(point);
        }

        int globalIndexedPivotSize = input.readInt();
        List<Point> globalIndexedPivot = new ArrayList<>();
        for (int i = 0; i < globalIndexedPivotSize; i++) {
            Point point = kryo.readObject(input, Point.class);
            globalIndexedPivot.add(point);
        }

        DITATrajectory ditaTrajectory = new DITATrajectory(id, num, mbr, extendedMBR, cells, trajectoryData, localIndexedPivot, globalIndexedPivot);
        return ditaTrajectory;
    }
}
