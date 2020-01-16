package com.konfuse.eamm;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.OperatorImportFromWkb;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WkbImportFlags;
import com.konfuse.RTree;
import com.konfuse.geometry.DataObject;
import com.konfuse.geometry.Point;
import com.konfuse.hmm.HmmProbabilities;
import com.konfuse.road.*;
import com.konfuse.spatial.Geography;
import com.konfuse.topology.Dijkstra;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * @Auther todd
 * @Date 2020/1/9
 */
public class EammMatcher {

    public final Geography spatial = new Geography();
    public final HmmProbabilities hmmProbabilities = new HmmProbabilities();
    public final Dijkstra<Road, RoadPoint> dijkstra = new Dijkstra<>();
    public final DistanceCost cost = new DistanceCost();

    public List<RoadPoint> match(List<GPSPoint> gpsPoints, RoadMap map, HashMap<Long, Vertex> vertices, RTree rtree){

        class Score implements Comparable<Score>{
            public double score;
            public long edgeId;
            public Score(long edgeId, double score){
                this.edgeId = edgeId;
                this.score = score;
            }

            @Override
            public int compareTo(Score other) {
                return (this.score < other.score) ? -1 : (this.score > other.score) ? 1 : 0;
            }
        }

        class TimeStep{
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

        Geography spatial = new Geography();
        ArrayList<RoadPoint> matchedPoint = new ArrayList<>();
        ArrayList<TreeSet<Long>> S = new ArrayList<>();
        ArrayList<TreeSet<Long>> C = new ArrayList<>();
        ArrayList<TreeSet<Long>> refinedC = new ArrayList<>();
        ArrayList<TimeStep> timesteps = new ArrayList<>();
        int N = gpsPoints.size();
        for(GPSPoint point : gpsPoints){
            ArrayList<DataObject>  candidate = rtree.knnQuery(new Point(point.getPosition().getX(), point.getPosition().getY()), 30);
            TreeSet<Long> edgeId = new TreeSet<>();
            for (DataObject dataObject : candidate) {
                Long id = dataObject.getId();
                edgeId.addAll(vertices.get(id).getRelateEdges());
            }
            S.add(edgeId);
        }

        C.add(S.get(0));
        for(int i = 0; i < N; i++){
            TreeSet<Long> set1 = C.get(C.size() - 1);
            TreeSet<Long> set2 = S.get(i);
            TreeSet<Long> result = new TreeSet<>();
            result.addAll(set2);
            result.retainAll(set1);

            for (Long edgeCurrent : set2) {
                Long sourceId = map.getEdges().get(edgeCurrent).source();
                for(Long edgeLast : set1){
                    Long targetId = map.getEdges().get(edgeLast).target();
                    if(sourceId.equals(targetId)){
                        result.add(edgeCurrent);
                    }
                }
            }
            C.add(result);
        }

        for(int i = 0; i < N; i++){
            TreeSet<Long> set3 = new TreeSet<>();
            GPSPoint gpsPointCurrent = gpsPoints.get(i);

            for (Long edgeId : C.get(i)) {
                Road road = map.getEdges().get(edgeId);
                Point q = new Point(gpsPointCurrent.getPosition().getX(), gpsPointCurrent.getPosition().getY());
                Polyline geometry = (Polyline) OperatorImportFromWkb.local().execute(
                        WkbImportFlags.wkbImportDefaults, Geometry.Type.Polyline, ByteBuffer.wrap(road.base().wkb()), null);
                double fraction = spatial.intercept(geometry, q);
                Point e = spatial.interpolate(geometry, spatial.length(geometry), fraction);
                double d = spatial.distance(e, q);

                if(d > 18.0){
                    set3.add(edgeId);
                }
            }

            TreeSet<Long> result = new TreeSet<>();
            result.addAll(C.get(i));
            result.removeAll(set3);

            TreeSet<Long> set4 = new TreeSet<>();
            if(i > 0){
                GPSPoint gpsPointLast = gpsPoints.get(i - 1);
                for (Long edgeId : result) {
                    Road road = map.getEdges().get(edgeId);
                    List<Point> points = road.getPoints();
                    Point source = points.get(0);
                    Point target = points.get(points.size() - 1);

                    double degree = getDegree(new Point(source.getX(), source.getY()), new Point(target.getX(), target.getY()),
                            new Point(gpsPointLast.getPosition().getX(), gpsPointLast.getPosition().getY()),
                            new Point(gpsPointCurrent.getPosition().getX(), gpsPointCurrent.getPosition().getY()));

                    if(degree > 90){
                        set4.add(edgeId);
                    }
                }
            }
            result.removeAll(set4);

            if(result.isEmpty()){
                if(i == 0){
                    result = C.get(0);
                }else{
                    GPSPoint gpsPointLast = gpsPoints.get(i - 1);
                    ArrayList<Score> scores = new ArrayList<>();
                    for (Long edgeId : C.get(i)) {

                        Road road = map.getEdges().get(edgeId);
                        List<Point> points = road.getPoints();
                        Point source = points.get(0);
                        Point target = points.get(points.size() - 1);

                        double degree = getDegree(new Point(source.getX(), source.getY()), new Point(target.getX(), target.getY()),
                                new Point(gpsPointLast.getPosition().getX(), gpsPointLast.getPosition().getY()),
                                new Point(gpsPointCurrent.getPosition().getX(), gpsPointCurrent.getPosition().getY()));

                        Point q = new Point(gpsPointCurrent.getPosition().getX(), gpsPointCurrent.getPosition().getY());
                        Polyline geometry = (Polyline) OperatorImportFromWkb.local().execute(
                                WkbImportFlags.wkbImportDefaults, Geometry.Type.Polyline, ByteBuffer.wrap(road.base().wkb()), null);
                        double fraction = spatial.intercept(geometry, q);
                        Point e = spatial.interpolate(geometry, spatial.length(geometry), fraction);
                        double distance = spatial.distance(e, q);

                        double score = getScore(degree, distance);

                        scores.add(new Score(edgeId, score));
                    }

                    double maxScore = -1000000.0;
                    for (Score score : scores) {
                        if(maxScore < score.score){
                            maxScore = score.score;
                        }
                    }

                    for (Score score : scores) {
                        if(score.score > 0.8 * maxScore){
                            result.add(score.edgeId);
                        }
                    }
                }
            }

            refinedC.add(result);

        }

        for(int i = 0; i < N; i++){
            ArrayList<Long> edgeIds = new ArrayList<>(refinedC.get(i));
            ArrayList<RoadPoint> current = new ArrayList<>();
            ArrayList<Integer> pres = new ArrayList<>();
            ArrayList<Double> weights = new ArrayList<>();
            if(i == 0){
                for (Long edgeId : edgeIds) {
                    Road road = map.getEdges().get(edgeId);
                    Point q = new Point(gpsPoints.get(i).getPosition().getX(), gpsPoints.get(i).getPosition().getY());
                    Polyline geometry = (Polyline) OperatorImportFromWkb.local().execute(
                            WkbImportFlags.wkbImportDefaults, Geometry.Type.Polyline, ByteBuffer.wrap(road.base().wkb()), null);
                    double fraction = spatial.intercept(geometry, q);
                    RoadPoint p = new RoadPoint(road, fraction);
                    current.add(p);
                    pres.add(-1);
                    weights.add(0.0);
                }

            }else{
                for (Long edgeId : edgeIds) {
                    Road road = map.getEdges().get(edgeId);
                    Point q = new Point(gpsPoints.get(i).getPosition().getX(), gpsPoints.get(i).getPosition().getY());
                    Polyline geometry = (Polyline) OperatorImportFromWkb.local().execute(
                            WkbImportFlags.wkbImportDefaults, Geometry.Type.Polyline, ByteBuffer.wrap(road.base().wkb()), null);
                    double fraction = spatial.intercept(geometry, q);
                    RoadPoint p = new RoadPoint(road, fraction);
                    TimeStep lastTimeStep = timesteps.get(timesteps.size() - 1);
                    int roadPointsNum = lastTimeStep.roadPoints.size();
                    int index = 0;
                    double min_dist = Double.MAX_VALUE;
                    for(int j = 0; j < roadPointsNum; j++){
                        double dist = shortestDistance(lastTimeStep.roadPoints.get(j), p, cost);
                        if(dist + lastTimeStep.getWeights().get(j) < min_dist ){
                            index = j;
                            min_dist = dist + lastTimeStep.getWeights().get(j);
                        }
                    }
                    current.add(p);
                    pres.add(index);
                    weights.add(min_dist);
                }
            }

            timesteps.add(new TimeStep(gpsPoints.get(i)));
            timesteps.get(timesteps.size() - 1).setRoadPoints(current);
            timesteps.get(timesteps.size() - 1).setPres(pres);
            timesteps.get(timesteps.size() - 1).setWeights(weights);
        }

        int index = 0;
        Double minWeight = Double.MAX_VALUE;
        int candidateSize = timesteps.get(timesteps.size() - 1).getWeights().size();
        for(int i = 0; i < candidateSize; i++){
            if(minWeight > timesteps.get(timesteps.size() - 1).getWeights().get(i)){
                minWeight = timesteps.get(timesteps.size() - 1).getWeights().get(i);
                index = i;
            }
        }

        for(int i = N - 1; i>= 0; i--){
            matchedPoint.add(timesteps.get(i).getRoadPoints().get(index));
            index = timesteps.get(i).getPres().get(index);
        }

        Collections.reverse(matchedPoint);
        return matchedPoint;
    }

    public double getScore(double degree, double distance){
        return 10 * Math.pow(degree, 4) - 0.17 * Math.pow(distance, 1.4);
    }

    public double getCos(Point a1, Point b1, Point a2, Point b2){
        double vector = (a1.getX() - b1.getX()) * (a2.getX() - b2.getX()) + (a1.getY() - b1.getY()) * (a2.getY() - b2.getY());
        double sqrt = Math.sqrt(
                (Math.abs((a1.getX() - b1.getX()) * (a1.getX() - b1.getX())) + Math.abs((a1.getY() - b1.getY()) * (a1.getY() - b1.getY())))
                        * (Math.abs((a2.getX() - b2.getX()) * (a2.getX() - b2.getX())) + Math.abs((a2.getY() - b2.getY()) * (a2.getY() - b2.getY()))));
        return vector / sqrt;
    }

    public double getDegree(Point a1, Point b1, Point a2, Point b2){
        double vector = (a1.getX() - b1.getX()) * (a2.getX() - b2.getX()) + (a1.getY() - b1.getY()) * (a2.getY() - b2.getY());
        double sqrt = Math.sqrt(
                (Math.abs((a1.getX() - b1.getX()) * (a1.getX() - b1.getX())) + Math.abs((a1.getY() - b1.getY()) * (a1.getY() - b1.getY())))
                        * (Math.abs((a2.getX() - b2.getX()) * (a2.getX() - b2.getX())) + Math.abs((a2.getY() - b2.getY()) * (a2.getY() - b2.getY()))));
        return Math.acos(vector / sqrt);
    }

    public double shortestDistance(RoadPoint source, RoadPoint target, DistanceCost cost) {
        double distanceToEndVertexOfSource = cost.cost(source.edge(), 1 -  source.fraction());
        double distanceFromStartVertexOfDestinationToTarget = cost.cost(target.edge(), target.fraction());
        if(source.edge().id() == target.edge().id()){
            if(source.fraction() < target.fraction()){
                return 2 * source.edge().length() - distanceToEndVertexOfSource +  distanceFromStartVertexOfDestinationToTarget;
            }
            else{
                return distanceFromStartVertexOfDestinationToTarget - distanceToEndVertexOfSource;
            }
        }

        List<Road> shortestPath = dijkstra.route(source, target, cost);

        if(shortestPath == null){
            return Double.MAX_VALUE;
        }

        double pathDistance = 0.0;
        for(int i = 1; i < shortestPath.size() - 1; i++){
            pathDistance += shortestPath.get(i).length();
        }
        return distanceToEndVertexOfSource + pathDistance + distanceFromStartVertexOfDestinationToTarget;
    }

}
