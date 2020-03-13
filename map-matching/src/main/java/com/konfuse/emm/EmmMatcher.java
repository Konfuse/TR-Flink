package com.konfuse.emm;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.OperatorImportFromWkb;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WkbImportFlags;
import com.konfuse.RTree;
import com.konfuse.geometry.DataObject;
import com.konfuse.geometry.Point;
import com.konfuse.road.*;
import com.konfuse.spatial.Geography;
import com.konfuse.topology.Dijkstra;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * @Auther todd
 * @Date 2020/1/9
 */
public class EmmMatcher {

    public final Geography spatial = new Geography();
    public final Dijkstra<Road, RoadPoint> dijkstra = new Dijkstra<>();
    public final DistanceCost cost = new DistanceCost();

    public List<RoadPoint> match(List<GPSPoint> gpsPoints, RoadMap map, HashMap<Long, Vertex> vertices, RTree rtree){
        ArrayList<HashSet<Long>> S = getCandidateSetS(gpsPoints, vertices, rtree);

        ArrayList<HashSet<Long>> C = getCandidateSetC(S, map);

        ArrayList<HashSet<Long>> refinedC = getRefinedSetC(gpsPoints, C, S, map) ;

        ArrayList<TimeStep> timeSteps = getTimeSteps(gpsPoints, refinedC, map);

        return getMatchedPoints(timeSteps);
    }


    public double getScore(double cos, double distance) {
        return 10 * Math.pow(cos, 4) - 0.17 * Math.pow(distance, 1.4);
    }

    public double getCos(Point a1, Point b1, Point a2, Point b2) {
        double vector = (b1.getX() - a1.getX()) * (b2.getX() - a2.getX()) + (b1.getY() - a1.getY()) * (b2.getY() - a2.getY());
        double sqrt = Math.sqrt(
                (Math.abs((a1.getX() - b1.getX()) * (a1.getX() - b1.getX())) + Math.abs((a1.getY() - b1.getY()) * (a1.getY() - b1.getY())))
                        * (Math.abs((a2.getX() - b2.getX()) * (a2.getX() - b2.getX())) + Math.abs((a2.getY() - b2.getY()) * (a2.getY() - b2.getY()))));
        if (sqrt == 0)
            return 0;
        return vector / sqrt;
    }

    public double getDegree(Point a1, Point b1, Point a2, Point b2) {
        double vector = (b1.getX() - a1.getX()) * (b2.getX() - a2.getX()) + (b1.getY() - a1.getY()) * (b2.getY() - a2.getY());
        double sqrt = Math.sqrt(
                (Math.abs((a1.getX() - b1.getX()) * (a1.getX() - b1.getX())) + Math.abs((a1.getY() - b1.getY()) * (a1.getY() - b1.getY())))
                        * (Math.abs((a2.getX() - b2.getX()) * (a2.getX() - b2.getX())) + Math.abs((a2.getY() - b2.getY()) * (a2.getY() - b2.getY()))));

       if (sqrt == 0)
           return 0;
        return Math.toDegrees(Math.acos(vector / sqrt));
    }

    public double shortestDistance(RoadPoint source, RoadPoint target, DistanceCost cost) {
        double distanceToEndVertexOfSource = cost.cost(source.edge(), 1 -  source.fraction());
        double distanceFromStartVertexOfDestinationToTarget = cost.cost(target.edge(), target.fraction());

        if(source.edge().id() == target.edge().id()) {
            if(source.fraction() < target.fraction()) {
                return 2 * source.edge().length() - distanceToEndVertexOfSource +  distanceFromStartVertexOfDestinationToTarget;
            }
            else {
                return distanceFromStartVertexOfDestinationToTarget - distanceToEndVertexOfSource;
            }
        }

        List<Road> shortestPath = dijkstra.route(source, target, cost);

        if(shortestPath == null) {
            return Double.MAX_VALUE;
        }

        double pathDistance = 0.0;
        for(int i = 1; i < shortestPath.size() - 1; i++) {
            pathDistance += shortestPath.get(i).length();
        }
        return distanceToEndVertexOfSource + pathDistance + distanceFromStartVertexOfDestinationToTarget;
    }

    public ArrayList<HashSet<Long>> getCandidateSetS(List<GPSPoint> gpsPoints, HashMap<Long, Vertex> vertices, RTree rtree) {
        ArrayList<HashSet<Long>> S = new ArrayList<>();

        for(GPSPoint point : gpsPoints) {
            ArrayList<DataObject> miniVertices = rtree.knnQuery(new Point(point.getPosition().getX(), point.getPosition().getY()), 50);

            HashSet<Long> edgeId = new HashSet<>();
            for (DataObject miniVertex : miniVertices) {
                Long id = miniVertex.getId();
                edgeId.addAll(vertices.get(id).getRelateEdges());
            }
            S.add(edgeId);
        }
        return S;
    }

    public ArrayList<HashSet<Long>> getCandidateSetC(ArrayList<HashSet<Long>> S, RoadMap map) {
        ArrayList<HashSet<Long>> C = new ArrayList<>();
        C.add(S.get(0));

        int N = S.size();
        for(int i = 1; i < N; i++) {
            HashSet<Long> preSet = C.get(C.size() - 1);
            HashSet<Long> curSet = S.get(i);

            HashSet<Long> result = new HashSet<>(curSet);
            result.retainAll(preSet);

            for (Long edgeCurrent : curSet) {
                Long sourceId = map.getEdges().get(edgeCurrent).source();
                for(Long edgeLast : preSet) {
                    Long targetId = map.getEdges().get(edgeLast).target();
                    if(sourceId.equals(targetId)) {
                        result.add(edgeCurrent);
                    }
                }
            }
            C.add(result);
        }
        return C;
    }

    public ArrayList<HashSet<Long>> getRefinedSetC(List<GPSPoint> gpsPoints, ArrayList<HashSet<Long>> C,  ArrayList<HashSet<Long>> S, RoadMap map) {
        ArrayList<HashSet<Long>> refinedSetC = new ArrayList<>();

        int N = gpsPoints.size();
        for(int i = 0; i < N; i++) {
            HashSet<Long> firstFilterSet = new HashSet<>();
            GPSPoint gpsPointCurrent = gpsPoints.get(i);

            for (Long edgeId : C.get(i)) {
                Road road = map.getEdges().get(edgeId);
                Point q = new Point(gpsPointCurrent.getPosition().getX(), gpsPointCurrent.getPosition().getY());
                Polyline geometry = (Polyline) OperatorImportFromWkb.local().execute(
                        WkbImportFlags.wkbImportDefaults, Geometry.Type.Polyline, ByteBuffer.wrap(road.base().wkb()), null);
                double distance = spatial.distanceBetweenPolylineAndPoint(geometry, q);
                if(distance <= 18.0){
                    firstFilterSet.add(edgeId);
                }
            }

            HashSet<Long> secondFilterSet = new HashSet<>();

            if(i > 0) {
                GPSPoint preGpsPoint = gpsPoints.get(i - 1);
                for (Long edgeId : firstFilterSet) {
                    List<Point> points = map.getEdges().get(edgeId).getPoints();
                    Point source = points.get(0);
                    Point target = points.get(points.size() - 1);

                    double degree = getDegree(new Point(source.getX(), source.getY()), new Point(target.getX(), target.getY()),
                            new Point(preGpsPoint.getPosition().getX(), preGpsPoint.getPosition().getY()),
                            new Point(gpsPointCurrent.getPosition().getX(), gpsPointCurrent.getPosition().getY()));

                    if(degree <= 90) {
                        secondFilterSet.add(edgeId);
                    }
                }
            }

            HashSet<Long> result = secondFilterSet;

//            System.out.print("secondFilterSet's result size: " + result.size());

            if (result.isEmpty()) {
                if(i == 0) {
                    result = S.get(0);
                } else {
                    GPSPoint preGpsPoint = gpsPoints.get(i - 1);
                    ArrayList<Score> scores = new ArrayList<>();
                    for (Long edgeId : S.get(i)) {
                        List<Point> points = map.getEdges().get(edgeId).getPoints();
                        Point source = points.get(0);
                        Point target = points.get(points.size() - 1);

                        double cos = getCos(new Point(source.getX(), source.getY()), new Point(target.getX(), target.getY()),
                                new Point(preGpsPoint.getPosition().getX(), preGpsPoint.getPosition().getY()),
                                new Point(gpsPointCurrent.getPosition().getX(), gpsPointCurrent.getPosition().getY()));

                        Point q = new Point(gpsPointCurrent.getPosition().getX(), gpsPointCurrent.getPosition().getY());
                        Polyline geometry = (Polyline) OperatorImportFromWkb.local().execute(
                                WkbImportFlags.wkbImportDefaults, Geometry.Type.Polyline, ByteBuffer.wrap(map.getEdges().get(edgeId).base().wkb()), null);

                        double distance = spatial.distanceBetweenPolylineAndPoint(geometry, q);
                        double score = getScore(cos, distance);

                        scores.add(new Score(edgeId, score));
                    }

                    double maxScore = Integer.MIN_VALUE;
                    System.out.print("scores is: ");
                    for (Score score : scores) {
                        System.out.print(score.score + ";");
                        if(maxScore < score.score){
                            maxScore = score.score;
                        }
                    }

                    System.out.println("scores's size is: " + scores.size());

                    double bound = maxScore > 0 ? maxScore * 0.8 : maxScore /0.8;
                    for (Score score : scores) {
                        if(score.score > bound){
                            result.add(score.edgeId);
                        }
                    }
                }
            }

//            System.out.println("; final result size: " + result.size());

            refinedSetC.add(result);
        }
        return refinedSetC;
    }

    public ArrayList<TimeStep> getTimeSteps(List<GPSPoint> gpsPoints, ArrayList<HashSet<Long>> refinedC, RoadMap map) {
        ArrayList<TimeStep> timeSteps = new ArrayList<>();

        int N = refinedC.size();
        for(int i = 0; i < N; i++) {
            ArrayList<Long> edgeIds = new ArrayList<>(refinedC.get(i));
            ArrayList<RoadPoint> current = new ArrayList<>();
            ArrayList<Integer> pres = new ArrayList<>();
            ArrayList<Double> weights = new ArrayList<>();

            if(i == 0) {
                for (Long edgeId : edgeIds) {
                    Road road = map.getEdges().get(edgeId);

                    Point gpsPosition = new Point(gpsPoints.get(i).getPosition().getX(), gpsPoints.get(i).getPosition().getY());
                    Polyline geometry = (Polyline) OperatorImportFromWkb.local().execute(
                            WkbImportFlags.wkbImportDefaults, Geometry.Type.Polyline, ByteBuffer.wrap(road.base().wkb()), null);
                    double fraction = spatial.intercept(geometry, gpsPosition);
                    RoadPoint p = new RoadPoint(road, fraction);

                    current.add(p);
                    pres.add(-1);
                    weights.add(0.0);
                }
            } else {
                for (Long edgeId : edgeIds) {
                    Road road = map.getEdges().get(edgeId);

                    Point gpsPosition = new Point(gpsPoints.get(i).getPosition().getX(), gpsPoints.get(i).getPosition().getY());
                    Polyline geometry = (Polyline) OperatorImportFromWkb.local().execute(
                            WkbImportFlags.wkbImportDefaults, Geometry.Type.Polyline, ByteBuffer.wrap(road.base().wkb()), null);
                    double fraction = spatial.intercept(geometry, gpsPosition);
                    RoadPoint roadPoint = new RoadPoint(road, fraction);

                    TimeStep preTimeStep = timeSteps.get(timeSteps.size() - 1);
                    int roadPointsNum = preTimeStep.roadPoints.size();

                    int index = 0;
                    double minDist = Double.MAX_VALUE;
                    for(int preIndex = 0; preIndex < roadPointsNum; preIndex++) {
                        double spDist = shortestDistance(preTimeStep.roadPoints.get(preIndex), roadPoint, cost);

                        if(spDist + preTimeStep.getWeights().get(preIndex) < minDist ){
                            index = preIndex;
                            minDist = spDist + preTimeStep.getWeights().get(preIndex);
                        }
                    }
                    current.add(roadPoint);
                    pres.add(index);
                    weights.add(minDist);
                }
            }

            timeSteps.add(new TimeStep(gpsPoints.get(i)));
            timeSteps.get(timeSteps.size() - 1).setRoadPoints(current);
            timeSteps.get(timeSteps.size() - 1).setPres(pres);
            timeSteps.get(timeSteps.size() - 1).setWeights(weights);
        }
        return timeSteps;
    }

    public ArrayList<RoadPoint> getMatchedPoints(ArrayList<TimeStep> timeSteps) {
        ArrayList<RoadPoint> matchedPoint = new ArrayList<>();

        int index = 0;
        Double minWeight = Double.MAX_VALUE;
        int lastCandidateSize = timeSteps.get(timeSteps.size() - 1).getWeights().size();

        for(int i = 0; i < lastCandidateSize; i++) {
            if(minWeight > timeSteps.get(timeSteps.size() - 1).getWeights().get(i)) {
                minWeight = timeSteps.get(timeSteps.size() - 1).getWeights().get(i);
                index = i;
            }
        }

        int N = timeSteps.size();
        for(int i = N - 1; i >=  0; i--) {
//            System.out.println("timeSteps is: " + timeSteps.get(i).getPres() + " index is: " + index);
            matchedPoint.add(timeSteps.get(i).getRoadPoints().get(index));
            index = timeSteps.get(i).getPres().get(index);
        }

        Collections.reverse(matchedPoint);
        return matchedPoint;
    }

    private static class Score implements Comparable<Score>{
        public double score;
        public long edgeId;
        public Score(long edgeId, double score){
            this.edgeId = edgeId;
            this.score = score;
        }

        @Override
        public int compareTo(Score other) {
            return Double.compare(this.score, other.score);
        }
    }

    private static class TimeStep {
        public final GPSPoint gpsPoint;
        public ArrayList<RoadPoint> roadPoints;
        public ArrayList<Integer> pres;
        public ArrayList<Double> weights;

        public TimeStep(GPSPoint gpsPoint) {
            this.gpsPoint = gpsPoint;
        }

        public GPSPoint getGpsPoint() {
            return gpsPoint;
        }

        public ArrayList<RoadPoint> getRoadPoints() {
            return roadPoints;
        }

        public ArrayList<Integer> getPres() {
            return pres;
        }

        public ArrayList<Double> getWeights() {
            return weights;
        }

        public void setRoadPoints(ArrayList<RoadPoint> edgeIds) {
            this.roadPoints = edgeIds;
        }

        public void setPres(ArrayList<Integer> pres) {
            this.pres = pres;
        }

        public void setWeights(ArrayList<Double> weights) {
            this.weights = weights;
        }
    }
}
