package io.sim.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import io.sim.company.Route;

public class RouteTest {
    
    /**
     * Test to see if the method of returning the route ID is correct.
     */
    @Test
    public void testeIDRoute() {
        String ID = "Unit Test";

        Route route = new Route(ID, null);

        String result = route.getIdRoute();

        assertEquals(ID, result);
    }
}