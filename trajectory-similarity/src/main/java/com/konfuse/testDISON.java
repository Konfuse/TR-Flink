package com.konfuse;

import com.konfuse.road.BaseRoad;

import java.util.LinkedList;
import java.util.List;

/**
 * @Auther todd
 * @Date 2020/4/21
 */
public class testDISON {
    public static void main(String[] args) {

    }

    public List<BaseRoad> getAllRoad() {
        LinkedList<BaseRoad> roads = new LinkedList<>();
        roads.add(new BaseRoad(1, 1, 2, 1, 2, 1.0f, 60.0f, 60.0f, 3.5, (byte[]) null));
        roads.add(new BaseRoad(2, 2, 3, 2, 2, 1.0f, 60.0f, 60.0f, 2.5, (byte[]) null));
        roads.add(new BaseRoad(3, 3, 4, 3, 2, 1.0f, 60.0f, 60.0f, 1.5, (byte[]) null));
        roads.add(new BaseRoad(4, 1, 5, 4, 2, 1.0f, 60.0f, 60.0f, 2, (byte[]) null));
        roads.add(new BaseRoad(5, 4, 14, 5, 2, 1.0f, 60.0f, 60.0f, 1.5, (byte[]) null));
        roads.add(new BaseRoad(6, 4, 9, 6, 2, 1.0f, 60.0f, 60.0f, 2.5, (byte[]) null));
        roads.add(new BaseRoad(7, 2, 7, 7, 2, 1.0f, 60.0f, 60.0f, 1, (byte[]) null));
        roads.add(new BaseRoad(8, 6, 7, 8, 2, 1.0f, 60.0f, 60.0f, 3, (byte[]) null));
        roads.add(new BaseRoad(9, 5, 6, 9, 2, 1.0f, 60.0f, 60.0f, 1.5, (byte[]) null));
        roads.add(new BaseRoad(10, 5, 10, 10, 2, 1.0f, 60.0f, 60.0f, 3.5, (byte[]) null));
        roads.add(new BaseRoad(11, 10, 11, 11, 2, 1.0f, 60.0f, 60.0f, 2, (byte[]) null));
        roads.add(new BaseRoad(12, 6, 12, 12, 2, 1.0f, 60.0f, 60.0f, 5.5, (byte[]) null));
        roads.add(new BaseRoad(13, 11, 12, 13, 2, 1.0f, 60.0f, 60.0f, 3, (byte[]) null));
        roads.add(new BaseRoad(14, 12, 17, 14, 2, 1.0f, 60.0f, 60.0f, 4, (byte[]) null));
        roads.add(new BaseRoad(15, 11, 16, 15, 2, 1.0f, 60.0f, 60.0f, 3.5, (byte[]) null));
        roads.add(new BaseRoad(16, 16, 17, 16, 2, 1.0f, 60.0f, 60.0f, 4, (byte[]) null));
        roads.add(new BaseRoad(17, 8, 9, 17, 2, 1.0f, 60.0f, 60.0f, 2, (byte[]) null));
        roads.add(new BaseRoad(18, 3, 9, 18, 2, 1.0f, 60.0f, 60.0f, 3, (byte[]) null));
        roads.add(new BaseRoad(19, 14, 15, 19, 2, 1.0f, 60.0f, 60.0f, 2.5, (byte[]) null));
        roads.add(new BaseRoad(20, 13, 14, 20, 2, 1.0f, 60.0f, 60.0f, 4, (byte[]) null));
        roads.add(new BaseRoad(21, 13, 18, 21, 2, 1.0f, 60.0f, 60.0f, 3, (byte[]) null));
        roads.add(new BaseRoad(22, 15, 20, 22, 2, 1.0f, 60.0f, 60.0f, 3, (byte[]) null));
        roads.add(new BaseRoad(23, 19, 20, 23, 2, 1.0f, 60.0f, 60.0f, 2.5, (byte[]) null));
        return roads;
    }
}
