package com.graphhopper.reader.dem;

import com.graphhopper.storage.DataAccess;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.PointList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
// Importation librairies mockito
import static org.mockito.Mockito.*;
import org.mockito.Mockito;

public class EdgeSamplingTest {
    private final ElevationProvider elevation = new ElevationProvider() {
        @Override
        public double getEle(double lat, double lon) {
            return 10;
        }

        @Override
        public boolean canInterpolate() {
            return false;
        }

        @Override
        public void release() {
        }

    };

    private double round(double d) {
        return Math.round(d * 1000) / 1000.0;
    }

    private PointList round(PointList list) {
        for (int i = 0; i < list.size(); i++) {
            list.set(i, round(list.getLat(i)), round(list.getLon(i)), list.getEle(i));
        }
        return list;
    }

    @Test
    public void doesNotAddExtraPointBelowThreshold() {
        PointList in = new PointList(2, true);
        in.add(0, 0, 0);
        in.add(1.4, 0, 0);

        PointList out = EdgeSampling.sample(
                in,
                DistanceCalcEarth.METERS_PER_DEGREE,
                new DistanceCalcEarth(),
                elevation
        );

        assertEquals("(0.0,0.0,0.0), (1.4,0.0,0.0)", round(out).toString());
    }

    @Test
    public void addsExtraPointAboveThreshold() {
        PointList in = new PointList(2, true);
        in.add(0, 0, 0);
        in.add(0.8, 0, 0);

        PointList out = EdgeSampling.sample(
                in,
                DistanceCalcEarth.METERS_PER_DEGREE / 2,
                new DistanceCalcEarth(),
                elevation
        );

        assertEquals("(0.0,0.0,0.0), (0.4,0.0,10.0), (0.8,0.0,0.0)", round(out).toString());
    }

    @Test
    public void addsExtraPointBelowSecondThreshold() {
        PointList in = new PointList(2, true);
        in.add(0, 0, 0);
        in.add(0.8, 0, 0);

        PointList out = EdgeSampling.sample(
                in,
                DistanceCalcEarth.METERS_PER_DEGREE / 3,
                new DistanceCalcEarth(),
                elevation
        );

        assertEquals("(0.0,0.0,0.0), (0.4,0.0,10.0), (0.8,0.0,0.0)", round(out).toString());
    }

    @Test
    public void addsTwoPointsAboveThreshold() {
        PointList in = new PointList(2, true);
        in.add(0, 0, 0);
        in.add(0.75, 0, 0);

        PointList out = EdgeSampling.sample(
                in,
                DistanceCalcEarth.METERS_PER_DEGREE / 4,
                new DistanceCalcEarth(),
                elevation
        );

        assertEquals("(0.0,0.0,0.0), (0.25,0.0,10.0), (0.5,0.0,10.0), (0.75,0.0,0.0)", round(out).toString());
    }

    @Test
    public void doesntAddPointsCrossingInternationalDateLine() {
        PointList in = new PointList(2, true);
        in.add(0, -178.5, 0);
        in.add(0.0, 178.5, 0);

        PointList out = EdgeSampling.sample(
                in,
                DistanceCalcEarth.METERS_PER_DEGREE,
                new DistanceCalcEarth(),
                elevation
        );

        assertEquals("(0.0,-178.5,0.0), (0.0,-179.5,10.0), (0.0,179.5,10.0), (0.0,178.5,0.0)", round(out).toString());
    }

    @Test
    public void usesGreatCircleInterpolationOnLongPaths() {
        PointList in = new PointList(2, true);
        in.add(88.5, -90, 0);
        in.add(88.5, 90, 0);

        PointList out = EdgeSampling.sample(
                in,
                DistanceCalcEarth.METERS_PER_DEGREE,
                new DistanceCalcEarth(),
                elevation
        );

        assertEquals("(88.5,-90.0,0.0), (89.5,-90.0,10.0), (89.5,90.0,10.0), (88.5,90.0,0.0)", round(out).toString());
    }

    // ce test verifie que EdgeSampling.sample() gère correctement une liste vide.
    @Test
    public void returnsEmptyListWhenInputIsEmpty() {
        PointList in = new PointList(2, true);

        PointList out = EdgeSampling.sample(
                in,
                DistanceCalcEarth.METERS_PER_DEGREE,
                new DistanceCalcEarth(),
                elevation
        );

        assertEquals(0, out.size(), "Empty input list should produce an empty output list");
    }

    // ce test vérifie que EdgeSampling.sample() ne modifie pas la liste lorsqu’elle contient un seul point
    @Test
    public void returnsSameListWhenSinglePoint() {
        PointList in = new PointList(2, true);
        in.add(45.0, -73.0, 0);

        PointList out = EdgeSampling.sample(
                in,
                DistanceCalcEarth.METERS_PER_DEGREE,
                new DistanceCalcEarth(),
                elevation
        );

        assertEquals(1, out.size());
        assertEquals("(45.0,-73.0,0.0)", round(out).toString());
    }

    // Ce test vérifie qu’aucun point intermédiaire n’est ajouté lorsque deux points sont
    // proches l’un de l’autre (distance inférieure au seuil).
    @Test
    public void doesNotAddPointsForVeryCloseCoordinates() {
        PointList in = new PointList(2, true);
        in.add(10.0, 10.0, 0);
        in.add(10.00001, 10.00001, 0); // très proche

        PointList out = EdgeSampling.sample(
                in,
                DistanceCalcEarth.METERS_PER_DEGREE / 100,
                new DistanceCalcEarth(),
                elevation
        );

        assertEquals(2, out.size(), "No intermediate point should be added for near-identical coordinates");
    }

    // Ce test garantit que ces mocks sont bien appelés et que l’algorithme produit
    // une liste cohérente de points interpolés.
    @Test
    public void testEdgeSamplingWithMockedDistanceCalculator() {
        DistanceCalcEarth mockDistance = mock(DistanceCalcEarth.class);

        when(mockDistance.calcDist3D(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(1000.0);

        ElevationProvider mockElevation = mock(ElevationProvider.class);
        when(mockElevation.getEle(anyDouble(), anyDouble())).thenReturn(50.0);
        when(mockElevation.canInterpolate()).thenReturn(true);

        PointList in = new PointList(2, true);
        in.add(0.0, 0.0, 0);
        in.add(1.0, 1.0, 0);

        PointList out = EdgeSampling.sample(
                in,
                500,
                mockDistance,  
                mockElevation
        );

        verify(mockDistance, atLeastOnce()).calcDist3D(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble());

        verify(mockElevation, atLeastOnce()).getEle(anyDouble(), anyDouble());

        assertTrue(out.size() >= 2);
    }

}