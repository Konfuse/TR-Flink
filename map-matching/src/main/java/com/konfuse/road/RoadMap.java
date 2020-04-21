package com.konfuse.road;

import com.esri.core.geometry.*;
import com.konfuse.IndexBuilder;
import com.konfuse.RTree;
import com.konfuse.geometry.DataObject;
import com.konfuse.geometry.Point;
import com.konfuse.geometry.Rectangle;
import com.konfuse.internal.MBR;
import com.konfuse.spatial.Geography;
import com.konfuse.topology.Graph;
import com.konfuse.util.Quadruple;
import com.konfuse.util.Tuple;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * @Author: todd
 * @Date: 2020/1/1
 */
public class RoadMap extends Graph<Road> {
    private transient Index index = null;
    private static HashMap<Long, Long> nodeRef = new HashMap<>();
    private static long vertexId = 0, roadId = 0;
    public static long convertTime = 0L;

    public class Index implements Serializable {
        private RTree tree;

        public void put(ArrayList<Road> roadList) {
            TreeSet<Long> ids = new TreeSet<>();
            ArrayList<Rectangle> rectangles = new ArrayList<>();
            for(Road road : roadList){
                Long id = road.base().id();
                if (ids.contains(id)) {
                    continue;
                }

                Polyline geometry = road.base().geometry();
                Envelope2D env = new Envelope2D();
                geometry.queryEnvelope2D(env);
                Rectangle rectangle = new Rectangle(id, env.getLowerLeft().getX(), env.getLowerLeft().getY(),
                        env.getUpperRight().getX(), env.getUpperRight().getY());

                rectangles.add(rectangle);
                ids.add(id);
            }
            int size = rectangles.size();
            System.out.println(size + " roads segments added into index");
            this.tree = new IndexBuilder().createRTreeBySTR(rectangles.toArray(new Rectangle[size]));
        }

        public void clear() {
            tree.clear();
        }

        private Set<RoadPoint> split(Set<Tuple<Long, Double>> points) {
            long start = System.currentTimeMillis();
            Set<RoadPoint> neighbors = new HashSet<>();

            for (Tuple<Long, Double> point : points) {
                neighbors.add(new RoadPoint(getEdges().get(point.f0 * 2), point.f1));

                if (getEdges().containsKey(point.f0 * 2 + 1)) {
                    neighbors.add(new RoadPoint(getEdges().get(point.f0 * 2 + 1), 1.0 - point.f1));
                }
            }
            long end = System.currentTimeMillis();
            convertTime += end - start;

            return neighbors;
        }

        private Set<RoadPoint> knnSplit(Set<Quadruple<Long, Double, Double, Double>> points) {
            long start = System.currentTimeMillis();
            Set<RoadPoint> neighbors = new HashSet<>();

            for (Quadruple<Long, Double, Double, Double> point : points) {
                neighbors.add(new RoadPoint(getEdges().get(point.f0 * 2), point.f1, point.f2, point.f3));

                if (getEdges().containsKey(point.f0 * 2 + 1)) {
                    neighbors.add(new RoadPoint(getEdges().get(point.f0 * 2), point.f1, point.f2, point.f3));
                }
            }
            long end = System.currentTimeMillis();
            convertTime += end - start;

            return neighbors;
        }

        public Set<RoadPoint> boxMatch(GPSPoint p, double r) {
            Geography spatial = new Geography();
            Set<Tuple<Long, Double>> nearests = new HashSet<>();

            do {
                MBR query = spatial.envelopeToMBR(p.getPosition().getX(), p.getPosition().getY(), r);
//                MBR query = new MBR(p.getPosition().getX() - r, p.getPosition().getY() - r, p.getPosition().getX() + r, p.getPosition().getY() + r);
                ArrayList<DataObject> candidateObject = tree.boxRangeQuery(query);
                Point q = new Point(p.getPosition().getX(), p.getPosition().getY());
                for (DataObject candidate : candidateObject){
                    long id = candidate.getId();
                    Polyline geometry = (Polyline) OperatorImportFromWkb.local().execute(
                            WkbImportFlags.wkbImportDefaults, Geometry.Type.Polyline, ByteBuffer.wrap(getEdges().get(2 * id).base().wkb()), null);
                    double fraction = spatial.intercept(geometry, q);
                    Point e = spatial.interpolate(geometry, spatial.length(geometry), fraction);
                    double d = spatial.distance(e, q);

                    if (d < r) {
//                    candidateRoads.add(new RoadPoint(getRoads().get(id), fraction));
                        nearests.add(new Tuple<>(id, fraction));
                    }
                }
                r *= 2;
            } while (nearests.isEmpty());

            return split(nearests);
        }

        public Set<RoadPoint> knnMatch(GPSPoint p, int k, double r) {
            Geography spatial = new Geography();
//            Set<Tuple<Long, Double>> nearests = new HashSet<>();
            Set<Quadruple<Long, Double, Double, Double>> nearests = new HashSet<>();

//            MBR query = spatial.envelopeToMBR(p.getPosition().getX(), p.getPosition().getY(), r);
            MBR query = new MBR(p.getPosition().getX() - r, p.getPosition().getY() - r, p.getPosition().getX() + r, p.getPosition().getY() + r);
            ArrayList<DataObject> candidateObject = tree.boxRangeQuery(query);

            if (candidateObject.size() > k) {
                Point queryPoint = new Point(p.getPosition().getX(), p.getPosition().getY());
                candidateObject.sort((o1, o2) -> {
                    Double d1 = o1.calDistance(queryPoint);
                    Double d2 = o2.calDistance(queryPoint);
                    return d1.compareTo(d2);
                });
            }

            int count = 1;
            Point q = new Point(p.getPosition().getX(), p.getPosition().getY());

            for (DataObject candidate : candidateObject){
                if (count > k) break;
                long id = candidate.getId();
                Polyline geometry = (Polyline) OperatorImportFromWkb.local().execute(
                        WkbImportFlags.wkbImportDefaults, Geometry.Type.Polyline, ByteBuffer.wrap(getEdges().get(2 * id).base().wkb()), null);
//                double fraction = spatial.intercept(geometry, q);
//                Point e = spatial.interpolate(geometry, spatial.length(geometry), fraction);
////                double d = spatial.distance(e, q);
//                double d = e.calDistance(q);

                double[] message = spatial.getDistanceAndInterceptWithLngLon(geometry, q);
                double fraction = message[0];
                double d = message[1];
                double x = message[2];
                double y = message[3];

                if (d < r) {
//                    candidateRoads.add(new RoadPoint(getRoads().get(id), fraction));
//                    nearests.add(new Tuple<>(id, fraction));
                    nearests.add(new Quadruple<>(id, fraction, x, y));
                    ++count;
                }
            }

            return knnSplit(nearests);
        }

        public Set<RoadPoint> radiusMatch(GPSPoint p, double r) {
            Geography spatial = new Geography();
            Set<Tuple<Long, Double>> nearests = new HashSet<>();

            do {
                double radius = spatial.convertRadius(p.getPosition().getX(), p.getPosition().getY(), r);
                ArrayList<DataObject> candidateObject = tree.circleRangeQuery(p.getPosition(), radius);
                Point q = new Point(p.getPosition().getX(), p.getPosition().getY());
                for (DataObject candidate : candidateObject){
                    long id = candidate.getId();
                    Polyline geometry = (Polyline) OperatorImportFromWkb.local().execute(
                            WkbImportFlags.wkbImportDefaults, Geometry.Type.Polyline, ByteBuffer.wrap(getEdges().get(2 * id).base().wkb()), null);
                    double fraction = spatial.intercept(geometry, q);
                    Point e = spatial.interpolate(geometry, spatial.length(geometry), fraction);
                    double d = spatial.distance(e, q);

                    if (d < r) {
//                    candidateRoads.add(new RoadPoint(getRoads().get(id), fraction));
                        nearests.add(new Tuple<>(id, fraction));
                    }
                }
                r *= 2;
            } while (nearests.isEmpty());

            return split(nearests);
        }
    }

    private static Collection<Road> split(BaseRoad base) {
        ArrayList<Road> roads = new ArrayList<>();

        if(base.oneway() == 0 || base.oneway() == 2) {
            roads.add(new Road(base, Heading.forward));
            roads.add(new Road(base, Heading.backward));
        } else {
            roads.add(new Road(base, Heading.forward));
        }

        return roads;
    }

//    private static Collection<Road> split(BaseRoad baseRoad) {
//        ArrayList<Road> roads = new ArrayList<>();
//
//        Polyline geometry = (Polyline) OperatorImportFromWkb.local().execute(
//                WkbImportFlags.wkbImportDefaults, Geometry.Type.Polyline, ByteBuffer.wrap(baseRoad.wkb()), null);
//
//        int size = geometry.getPointCount();
//        for (int i = 1; i < size; i++) {
//            Polyline polyline = new Polyline();
//            polyline.startPath(geometry.getPoint(i - 1));
//            polyline.lineTo(geometry.getPoint(i));
//            byte[] wkb = OperatorExportToWkb.local()
//                    .execute(WkbExportFlags.wkbExportLineString, geometry, null).array();
//
//            long source, target;
//            if (i == 1) {
//                if (!nodeRef.containsKey(baseRoad.source())) {
//                    nodeRef.put(baseRoad.source(), vertexId);
//                    source = vertexId;
//                    target = vertexId + 1;
//                    vertexId += 2;
//                } else {
//                    source = nodeRef.get(baseRoad.source());
//                    target = vertexId;
//                    vertexId += 1;
//                }
//            } else if (i == size - 1) {
//                if (!nodeRef.containsKey(baseRoad.target())) {
//                    nodeRef.put(baseRoad.source(), vertexId);
//                    source = vertexId;
//                    target = vertexId + 1;
//                    vertexId += 2;
//                } else {
//                    source = vertexId;
//                    target = nodeRef.get(baseRoad.target());
//                    vertexId += 1;
//                }
//            } else {
//                source = vertexId;
//                target = vertexId + 1;
//                vertexId += 2;
//            }
//
//            BaseRoad base = new BaseRoad(roadId, source, target, baseRoad.refid(), baseRoad.oneway(),
//                    baseRoad.priority(), baseRoad.maxspeedforward(), baseRoad.maxspeedbackward(), baseRoad.length(), wkb);
//            roadId += 1;
//
//            if(base.oneway() == 0 || base.oneway() == 2) {
//                roads.add(new Road(base, Heading.forward));
//                roads.add(new Road(base, Heading.backward));
//            } else {
//                roads.add(new Road(base, Heading.forward));
//            }
//        }
//
//        return roads;
//    }

    public static RoadMap Load(RoadReader reader) throws Exception {
        long memory = 0;

        System.gc();
        memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        if (!reader.isOpen()) {
            reader.open();
        }

        RoadMap roadmap = new RoadMap();

        BaseRoad road;
        while ((road = reader.next()) != null) {
            for (Road uni : split(road)) {
                roadmap.add(uni);
            }
        }
//        nodeRef.clear();
        reader.close();

        System.gc();
        memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) - memory;
        System.out.println(Math.max(0, Math.round(memory / 1E6)) + " megabytes used for edge data (estimate)");

        return roadmap;
    }

    /**
     * Constructs edge network topology and spatial index.
     */
    @Override
    public RoadMap construct() {
        long memory = 0;

        System.gc();
        memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("index and topology constructing ...");

        super.construct();

        System.out.println("road map contains nodes out " + getNodesOut().size() + " vertices; " + "nodes in " + getNodesIn().size() + " vertices.");
        System.gc();
        memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) - memory;
        System.out.println(Math.max(0, Math.round(memory / 1E6)) + " megabytes used for spatial topology object (estimate)" );

        memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        index = new Index();
        ArrayList<Road> roadList = new ArrayList<>(getEdges().values());
        index.put(roadList);

        System.out.println("index and topology constructed.");
        System.gc();
        memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) - memory;
        System.out.println(Math.max(0, Math.round(memory / 1E6)) + " megabytes used for spatial index (estimate)" );

        return this;
    }

    /**
     * Destroys edge network topology and spatial index. (Necessary if roads have been added and
     * edge network topology and spatial index must be reconstructed.)
     */
    @Override
    public void deconstruct() {
        super.deconstruct();
        index.clear();
        index = null;
    }

    public Index spatial() {
        if (index == null){ throw new RuntimeException("index not constructed");}
        return index;
    }
}
