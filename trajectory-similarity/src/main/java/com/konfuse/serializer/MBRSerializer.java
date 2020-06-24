package com.konfuse.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.konfuse.strtree.MBR;
import com.konfuse.strtree.PartitionedMBR;

/**
 * @author todd
 * @date 2020/5/19 20:57
 * @description: MBR 序列化
 */
public class MBRSerializer extends Serializer<MBR> {
    @Override
    public void write(Kryo kryo, Output output, MBR object) {
        output.writeDouble(object.getX1());
        output.writeDouble(object.getX2());
        output.writeDouble(object.getY1());
        output.writeDouble(object.getY2());
        if (object instanceof PartitionedMBR) {
            output.writeBoolean(true);
            PartitionedMBR mbr = (PartitionedMBR) object;
            output.writeInt(mbr.getSize());
            output.writeInt(mbr.getPartitionNumber());
        } else {
            output.writeBoolean(false);
        }
    }

    @Override
    public MBR read(Kryo kryo, Input input, Class<MBR> type) {
        double x1 = input.readDouble();
        double x2 = input.readDouble();
        double y1 = input.readDouble();
        double y2 = input.readDouble();
        boolean isPartitionedMBR = input.readBoolean();
        if (isPartitionedMBR) {
            int size = input.readInt();
            int partitionNum = input.readInt();
            PartitionedMBR mbr = new PartitionedMBR(x1, y1, x2, y2, size, partitionNum);
            return mbr;
        } else {
            MBR mbr = new MBR(x1, y1, x2, y2);
            return mbr;
        }
    }
}
