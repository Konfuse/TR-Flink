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
            System.out.println(size);
            this.tree = new IndexBuilder().createRTreeBySTR(rectangles.toArray(new Rectangle[size]));
        }

        public void clear() {
            tree.clear();
        }

        private Set<RoadPoint> split(Set<Tuple<Long, Double>> points) {
            Set<RoadPoint> neighbors = new HashSet<>();

            for (Tuple<Long, Double> point : points) {
                neighbors.add(new RoadPoint(getEdges().get(point.f0 * 2), point.f1));

                if (getEdges().containsKey(point.f0 * 2 + 1)) {
                    neighbors.add(new RoadPoint(getEdges().get(point.f0 * 2 + 1), 1.0 - point.f1));
                }
            }

            return neighbors;
        }

        public Set<RoadPoint> boxMatch(GPSPoint p, double r) {
            Geography spatial = new Geography();
            Set<Tuple<Long, Double>> nearests = new HashSet<>();

            do {
                MBR query = spatial.envelopeToMBR(p.getPosition().getX(), p.getPosition().getY(), r);
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

        if(base.oneway() == 0 || base.oneway() == 2){
            roads.add(new Road(base, Heading.forward));
            roads.add(new Road(base, Heading.backward));
        }else{
            roads.add(new Road(base, Heading.forward));
        }

        return roads;
    }

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

        index = new Index();
        ArrayList<Road> roadList = new ArrayList<>(getEdges().values());
        index.put(roadList);

        System.out.println("index and topology constructed");

        System.gc();
        memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) - memory;
        System.out.println(Math.max(0, Math.round(memory)) + " bits used for spatial index (estimate)" );

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
