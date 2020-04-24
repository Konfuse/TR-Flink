package com.konfuse.road;

import com.esri.core.geometry.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author: Konfuse
 * @Date: 2020/4/22 9:22
 */
public class DemoRoadReader implements RoadReader {
    private List<BaseRoad> roads;
    private int index;
    private int size;

    public void addMethod1(List<BaseRoad> roads) {
        Polyline polyline = new Polyline();
        polyline.startPath(0, 0);
        polyline.lineTo(1, 0);
        polyline.lineTo(1, 1);

        roads.add(new BaseRoad(0L, 1L, 2L, 0L, 2, 1, 60.0f, 60.0f, 12, polyline));
        roads.add(new BaseRoad(1L, 1L, 6L, 0L, 2, 1, 60.0f, 60.0f, 16, polyline));
        roads.add(new BaseRoad(2L, 1L, 7L, 0L, 2, 1, 60.0f, 60.0f, 14, polyline));
        roads.add(new BaseRoad(3L, 2L, 3L, 0L, 2, 1, 60.0f, 60.0f, 10, polyline));
        roads.add(new BaseRoad(4L, 2L, 6L, 0L, 2, 1, 60.0f, 60.0f, 7, polyline));
        roads.add(new BaseRoad(5L, 3L, 6L, 0L, 2, 1, 60.0f, 60.0f, 6, polyline));
        roads.add(new BaseRoad(6L, 3L, 5L, 0L, 2, 1, 60.0f, 60.0f, 5, polyline));
        roads.add(new BaseRoad(7L, 3L, 4L, 0L, 2, 1, 60.0f, 60.0f, 3, polyline));
        roads.add(new BaseRoad(8L, 4L, 5L, 0L, 2, 1, 60.0f, 60.0f, 4, polyline));
        roads.add(new BaseRoad(9L, 5L, 6L, 0L, 2, 1, 60.0f, 60.0f, 2, polyline));
        roads.add(new BaseRoad(10L, 5L, 7L, 0L, 2, 1, 60.0f, 60.0f, 8, polyline));
        roads.add(new BaseRoad(11L, 6L, 7L, 0L, 2, 1, 60.0f, 60.0f, 9, polyline));
    }

    public void addMethod2(List<BaseRoad> roads) {
        Polyline polyline = new Polyline();
        polyline.startPath(0, 0);
        polyline.lineTo(1, 0);
        polyline.lineTo(1, 1);

        roads.add(new BaseRoad(0L, 1L, 3L, 0L, 1, 1, 60.0f, 60.0f, 10, polyline));
        roads.add(new BaseRoad(1L, 1L, 5L, 0L, 1, 1, 60.0f, 60.0f, 30, polyline));
        roads.add(new BaseRoad(2L, 1L, 6L, 0L, 1, 1, 60.0f, 60.0f, 100, polyline));
        roads.add(new BaseRoad(3L, 2L, 3L, 0L, 1, 1, 60.0f, 60.0f, 5, polyline));
        roads.add(new BaseRoad(4L, 3L, 4L, 0L, 1, 1, 60.0f, 60.0f, 50, polyline));
        roads.add(new BaseRoad(5L, 4L, 6L, 0L, 1, 1, 60.0f, 60.0f, 10, polyline));
        roads.add(new BaseRoad(6L, 5L, 4L, 0L, 1, 1, 60.0f, 60.0f, 20, polyline));
        roads.add(new BaseRoad(7L, 5L, 6L, 0L, 1, 1, 60.0f, 60.0f, 60, polyline));
    }

    @Override
    public void open() throws Exception {
        roads = new ArrayList<>();
        addMethod1(roads);

        index = 0;
        size = roads.size();
    }

    @Override
    public boolean isOpen() {
        return roads != null;
    }

    @Override
    public void close() throws Exception {
        if (roads != null) {
            roads = null;
        }
        index = size = 0;
    }

    @Override
    public BaseRoad next() throws Exception {
        if (index == size) {
            return null;
        }

        return roads.get(index++);
    }
}
