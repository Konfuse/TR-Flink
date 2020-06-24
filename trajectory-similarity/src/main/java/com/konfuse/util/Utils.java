package com.konfuse.util;

import org.apache.flink.api.java.ExecutionEnvironment;

/**
 * @author todd
 * @date 2020/5/21 22:54
 * @description: TODO
 */
public class Utils {
    public static ExecutionEnvironment registerTypeWithKryoSerializer(ExecutionEnvironment env){
        Class[] kryoTypeClasses = new Class[]{
//                Point.class,
//                Line.class,
//                Rectangle.class,
//                MBR.class,
//                RTreePointLeafNode.class,
//                RTreeLineLeafNode.class,
//                RTreeRectLeafNode.class,
//                RTreeInternalNode.class,
//                RTreeNode.class,
//                RTree.class,
//                TrieLeafNode.class,
//                TrieInternalNode.class,
//                TrieNode.class,
//                DITAGlobalIndex.class,
//                DITALocalIndex.class,
//                DITATrajectory.class,
//                DISONEdge.class,
//                DISONTrajectory.class,
//                DISONGlobalIndex.class,
//                DISONLocalIndex.class,
//                Road.class,
                };
        for(int i = 0; i< kryoTypeClasses.length; i++){
            env.getConfig().registerKryoType(kryoTypeClasses[i]);
        }
        return env;
    }

    public static ExecutionEnvironment registerCustomSerializer(ExecutionEnvironment env){
//        env.registerTypeWithKryoSerializer(Point.class, PointSerializer.class);
//        env.registerTypeWithKryoSerializer(Line.class, MBRSerializer.class);
//        env.registerTypeWithKryoSerializer(Rectangle.class, RectangleSerializer.class);
//        env.registerTypeWithKryoSerializer(MBR.class, MBRSerializer.class);
//        env.registerTypeWithKryoSerializer(RTreePointLeafNode.class, RTreeNodeSerializer.class);
//        env.registerTypeWithKryoSerializer(RTreeLineLeafNode.class, RTreeNodeSerializer.class);
//        env.registerTypeWithKryoSerializer(RTreeRectLeafNode.class, RTreeNodeSerializer.class);
//        env.registerTypeWithKryoSerializer(RTreeInternalNode.class, RTreeNodeSerializer.class);
//        env.registerTypeWithKryoSerializer(RTreeNode.class, RTreeNodeSerializer.class);
//        env.registerTypeWithKryoSerializer(RTree.class, RTreeSerializer.class);
//        env.registerTypeWithKryoSerializer(TrieLeafNode.class, TrieNodeSerializer.class);
//        env.registerTypeWithKryoSerializer(TrieInternalNode.class, TrieNodeSerializer.class);
//        env.registerTypeWithKryoSerializer(TrieNode.class, TrieNodeSerializer.class);
//        env.registerTypeWithKryoSerializer(DITAGlobalIndex.class, GlobalIndexDITASerializer.class);
//        env.registerTypeWithKryoSerializer(DITALocalIndex.class, LocalIndexDITASerializer.class);
//        env.registerTypeWithKryoSerializer(DITATrajectory.class, DITATrajectorySerializer.class);
//        env.registerTypeWithKryoSerializer(DISONEdge.class, DISONEdgeSerializer.class);
//        env.registerTypeWithKryoSerializer(DISONTrajectory.class, DISONTrajectorySerializer.class);
//        env.registerTypeWithKryoSerializer(DISONGlobalIndex.class, DISONGlobalIndexSerializer.class);
//        env.registerTypeWithKryoSerializer(DISONLocalIndex.class, DISONLocalIndexSerializer.class);
//        env.registerTypeWithKryoSerializer(Road.class, RoadSerializer.class);
        return env;
    }
}
