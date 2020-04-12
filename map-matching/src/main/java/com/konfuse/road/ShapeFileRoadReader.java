package com.konfuse.road;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.OperatorImportFromWkt;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.WkbImportFlags;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @Author: Konfuse
 * @Date: 2020/4/12 10:23
 */
public class ShapeFileRoadReader implements RoadReader {
    private String pathOfShapeFile = null;
    //读取shp
    private SimpleFeatureCollection collection;
    //拿到所有features
    private SimpleFeatureIterator iterator;

    public ShapeFileRoadReader(String pathOfShapeFile) {
        assert pathOfShapeFile != null;
        this.pathOfShapeFile = pathOfShapeFile;
    }


    @Override
    public void open() throws Exception {
        File file = new File(pathOfShapeFile);
        FileDataStore store;
        SimpleFeatureSource featureSource = null;
        try {
            store = FileDataStoreFinder.getDataStore(file);
            ((ShapefileDataStore) store).setCharset(StandardCharsets.UTF_8);
            featureSource = store.getFeatureSource();
        } catch (IOException e) {
            System.out.println("read shape file has exception , msg = " + e.getMessage());
            throw e;
        }

        assert featureSource != null;
        collection = featureSource.getFeatures();
    }

    @Override
    public boolean isOpen() {
        return collection != null;
    }

    @Override
    public void close() throws Exception {
        if (iterator != null) {
            iterator.close();
        }
    }

    @Override
    public BaseRoad next() throws Exception {
        if (iterator == null) {
            iterator = collection.features();
        }

        if (!iterator.hasNext()) {
            return null;
        }
        SimpleFeature feature = iterator.next();
        long id = (long) feature.getAttribute("ID");
        long refId = 0;
        long source = (long) feature.getAttribute("SOURCE");
        long target = (long) feature.getAttribute("TARGET");
        int oneway = 1;
        float priority = 1;
        float maxSpeedForward = 60;
        float maxSpeedBackward = 60;
        Polyline geometry = (Polyline) OperatorImportFromWkt.local().execute(
                WkbImportFlags.wkbImportDefaults, Geometry.Type.Polyline, feature.getAttribute("the_geom").toString(), null);
        double length = geometry.calculateLength2D();
        return new BaseRoad(id, source, target, refId, oneway, priority, maxSpeedForward, maxSpeedBackward, length, geometry);
    }
}
