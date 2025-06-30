package io.sim.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import io.sim.car.Car;

public class CarVectorAdjustmentTest3 {
    /**
     * Test to verify the functioning of the adjustVector method, 
     * in the case where the parameter is a double matrix.
     */
    @Test
    public void MatrixAdjustmentTest() {
        double[][] matrix = new double[][] {{1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}};
        double[][] res = new double[][] {{1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}};

        Car car = new Car();

        double[][] result = car.adjustVector(matrix);

        for (int i = 0; i < res.length; i++) {
            assertEquals(res[0][i], result[0][i], 0.01);
        }
    }
}
