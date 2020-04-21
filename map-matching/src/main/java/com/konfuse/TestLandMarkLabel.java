package com.konfuse;

import com.konfuse.lmm.LandMarkLabel;
import com.konfuse.road.PostgresRoadReader;
import com.konfuse.road.RoadMap;

/**
 * @Author: Konfuse
 * @Date: 2020/4/21 1:58
 */
public class TestLandMarkLabel {
    public static void main(String[] args) throws Exception {
        RoadMap map = RoadMap.Load(new PostgresRoadReader());
        map.construct();

        System.out.println("start constructing land mark label...");
        LandMarkLabel label = LandMarkLabel.constructLabel(map);
        System.out.println("label constructed.");
        label.store("label_in.txt", "label_out.txt");
    }
}
