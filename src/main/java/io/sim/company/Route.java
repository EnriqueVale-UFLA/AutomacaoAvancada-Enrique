package io.sim.company;

import de.tudresden.sumo.objects.SumoStringList;
import de.tudresden.sumo.util.SumoCommand;

/**
 * Class to store data related to one of the routes required for the environment.
 * Includes ID and edges ("points").
 */
public class Route {
    //ID of the Route
    private final String id_route;

    //Set of edges of a route in a single String.
    private final String edges;

    //Number of edges present on the route.
    private int edges_number;

    //-------------------------------------------------------------------------------------------------------------------
    
    /**
     * Constructor of the class in question, considering the route ID, as well as its Edges (the only String containing the edges).
     * @param id_route {@link String} containing the route ID.
     * @param edges {@link String}, being a single String containing all the Edges.
     */
    public Route(String id_route, String edges) {
        this.id_route = id_route;
        this.edges = edges;
    }

    /**
     * Separates the Edges into an object of type {@link SumoStringList}.
     * @return {@link SumoStringList} containing all the "separated" edges.
     */
    private SumoStringList separateEdges() {
        SumoStringList edges = new SumoStringList();
        edges.clear();

        String[] aux = this.edges.split(" ");

        this.edges_number = aux.length;

        for (int i = 0; i < aux.length; i++) {
            edges.add(aux[i]);
        }

        return edges;
    }

    /**
     * Adds the route to Sumo based on its ID and its Edges.
     * @return {@link SumoCommand} containing the "necessary information" for the actual addition of the Route.
     */
    public SumoCommand addRouteSumo() {
        return new SumoCommand(198, 128, this.id_route, separateEdges());
    }

    /**
     * GET method for the {@link Route#id_route} attribute.
     * @return {@link String} containing the route ID.
     */
    public String getIdRoute() {
        return this.id_route;
    }

    /**
     * GET method for the {@link Route#edges} attribute.
     * @return {@link String} a single String containing all edges.
     */
    public String getEdges() {
        return this.edges;
    }

    /**
     * GET method for the {@link Route#edges_number} attribute.
     * @return {@link Integer} containing the number of Edges in a route.
     */
    public int getNumberEdges() {
        return this.edges_number;
    }
}
