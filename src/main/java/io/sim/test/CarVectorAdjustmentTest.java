package io.sim.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import io.sim.car.Car;

public class CarVectorAdjustmentTest {
    /**
     * Test to verify the functioning of the adjustVector method, 
     * when the input is an array of double and a time elapsed.
     */
    @Test
    public void TimeVectorAdjustmentTest() {
        long time = 50;
        double[] vector = new double[] {760, 51.05, 24.15, 31.98, 32.54, 31.84, 194.38, 32.32, 27.40,
            153.63, 33.0, 43.83, 26.14, 38.51, 44.76, 64.32, 53.90};
        double[] res = new double[] {710, 25.2, 31.98, 32.54, 31.84, 194.38, 32.32, 27.40,
            153.63, 33.0, 43.83, 26.14, 38.51, 44.76, 64.32, 53.90};

        Car car = new Car();

        double[] result = car.adjustVector(vector, time);

        for (int i = 0; i < res.length; i++) {
            assertEquals(res[i], result[i], 0.01);
        }
    }
}
