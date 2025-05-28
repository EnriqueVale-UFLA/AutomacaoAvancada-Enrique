package io.sim.test;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import io.sim.car.Car;

public class CarTest {
    /**
     * Test to see if the method of converting distance from decameter to kilometer is adequate.
     */
    @Test
    public void testCalculatioDistance() {
        double distance_decameters = 1500;
        String distance_km = (distance_decameters/100) + "";

        Car car = new Car();

        String result = car.distanceCalculator(distance_decameters) + "";

        assertEquals(distance_km, result);
    }
}