package io.sim.utils;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import io.sim.company.Constants;
import io.sim.company.Route;

/**
 * Class whose responsibility is to handle operations related to a JSON file, exchanged between servers.
 */
public class JsonUtil {

    //Object that contains all the information. Basically it is the JSON file.
    private JSONObject object;

    //Key for COMMAND
    private final String command_key = "command";

    //Keys for REPORT
    private final String routes_id_key = "Routes_id";
    private final String routes_edge_key = "Routes_edge";
    private final String car_id_key = "car_id";
    private final String speed_key = "speed";
    private final String distance_key = "distance";
    private final String fuel_consumption_key = "fuel_consumption";
    private final String fuel_type_key = "fuel_type";
    private final String CO2_key = "CO2";
    private final String long_key = "longitude";
    private final String lat_key = "latitude";
    private final String timestamp_key = "nanoStamp";

    //Keys for PAYMENT and BALANCE INQUIRY
    private final String login_key = "login";
    private final String password_key = "password";
    private final String target_account_key = "target_account";
    private final String value_key = "value";
    private final String balance_key = "balance";

    //----------------------------------------------------------------------------------------------------------------
    /**
     * Constructor of the class in question, initializing the {@link JsonUtil#object} attribute.
     */
    public JsonUtil() {
        object = new JSONObject();
    }

    /**
     * Class constructor, initializing the attribute from a {@link String} containing data from a {@link JSONObject}
     * @param JSONObject_as_String
     */
    public JsonUtil(String JSONObject_as_String) {
        object = new JSONObject(JSONObject_as_String);
    }

    /**
     * GET method for the message contained in the command to be performed.
     * @return {@link String} containing the command to be performed (Ex: pay, consult, etc.).
     */
    public String getCommand() {
        return object.getString(command_key);
    }

    /**
     * GET method for the object ({@link JSONObject}) related to the JSON file as a String.
     * @return Object of type {@link String} containing the object converted to the mentioned type.
     */
    public String getJSONAsString() {
        return this.object.toString();
    }

    /**
     * Write all necessary parameters in the requested management report.
     * @param car_id {@link String}containing the car ID.
     * @param IDroute {@link String} containing the ID of the route being executed.
     * @param speed {@link Double} containing the current speed of the car.
     * @param distance {@link Double} contendo a distância percorrida pelo carro.
     * @param fuel_consumption {@link Double} containing fuel consumption.
     * @param fuel_type {@link String} containing the type of fuel used.
     * @param CO2_emission {@link Double} containing CO2 emissions.
     * @param latitude {@link Double} containing the latitude coordinate.
     * @param longitude {@link Double} containing the longitude coordinate.
     * @param timeStamp {@link Long} containing the time, in nanoseconds, that the data was obtained.
     */
    public void writeReport(String car_id, String IDroute, double speed, double distance,
                                  double fuel_consumption, String fuel_type, double CO2_emission, 
                                  double latitude, double longitude, long timeStamp) {

        object.put("command", "report");
        object.put(car_id_key, car_id);
        object.put(routes_id_key, IDroute);
        object.put(speed_key, speed);
        object.put(distance_key, distance);
        object.put(fuel_consumption_key, fuel_consumption);
        object.put(fuel_type_key, fuel_type);
        object.put(CO2_key, CO2_emission);
        object.put(long_key, longitude);
        object.put(lat_key, latitude);
        object.put(timestamp_key, timeStamp);
    }

    /**
     * "Catch" all the data present in the report JSON file and stores it in a {@link ArrayList}
     * @return {@link ArrayList} containing all the data for the report.
     */
    public ArrayList<Object> catchReport() {
        ArrayList<Object> arrayList = new ArrayList<>();
        
        arrayList.add(object.get(timestamp_key));
        arrayList.add(object.get(car_id_key));
        arrayList.add(object.get(routes_id_key));
        arrayList.add(object.get(speed_key));
        arrayList.add(object.get(distance_key));
        arrayList.add(object.get(fuel_consumption_key));
        arrayList.add(object.get(fuel_type_key));
        arrayList.add(object.get(CO2_key));
        arrayList.add(object.get(long_key));
        arrayList.add(object.get(lat_key));

        return arrayList;
    }

    /**
     * Writes the data required to make a payment to a JSON file.
     * @param login {@link String} containing the login of the account from which the money will be withdrawn.
     * @param password {@link String} containing the password for the account from which the money will be withdrawn.
     * @param target {@link String} containing the account where the money will be deposited.
     * @param value {@link Double} containing the amount to be transacted.
     */
    public void writePaymentData(String login, String password, String target, double value) {
        object.put(command_key, "pay");
        object.put(login_key, login);
        object.put(password_key, password);
        object.put(target_account_key, target);
        object.put(value_key, value);
    }

    /**
     * Returns payment data to AlphaBank.
     * @return {@link ArrayList} containing all payment data.
     */
    public ArrayList<Object> receiveDataPayment() {
        ArrayList<Object> arrayList = new ArrayList<>();

        arrayList.add(object.get(login_key));
        arrayList.add(object.get(password_key));
        arrayList.add(object.get(target_account_key));
        arrayList.add(object.get(value_key));

        return arrayList;
    }
    
    /**
     * Writes the route request message to be sent.
     * @param car_id {@link String} containing the car ID.
     */
    public void writeRoutesRequest(String car_id) {
        object.put(command_key, "sendRoutes");
        object.put(car_id_key, car_id);
    }
    
    /**
     * Receives the route request, performing the operations involved with the file.
     * @return {@link String} containing the ID of the car that sent the request.
     */
    public String receiveRequestRoutes() {
        return object.getString(car_id_key);
    }
    
    /**
     * Write the message to check your balance.
     * @param login {@link String} containing the login of the person trying to perform the query.
     * @param password {@link String} containing the password of the person trying to perform the query.
     */
    public void writeConsultBalance(String login, String password) {
        object.put(command_key, Constants.consultation_command);
        object.put(login_key, login);
        object.put(password_key, password);
    }
    
    /**
     * Receive the balance inquiry message with the appropriate parameters.
     * @return {@link ArrayList} containing the login and password data to access the balance.
     */
    public ArrayList<String> receiveConsultationBalance() {
        ArrayList<String> arrayList = new ArrayList<>();

        arrayList.add(object.getString(login_key));
        arrayList.add(object.getString(password_key));

        return arrayList;
    }

    /**
     * Writes a message to a customer containing the balance.
     * @param balance{@link Double} containng the balance
     */
    public void writeBalance(double balance) {
        object.put(command_key, Constants.balance_command);
        object.put(balance_key, balance);
    }

    /**
     * Receive the requested balance.
     * @return {@link Double} containing the balance.
     */
    public double receiveBalance() {
        return object.getDouble(balance_key);
    }
    
    /**
     * Writes an object of type {@link ArrayList}<Route> inside the JSON file.
     * @param routes {@link ArrayList} containing the routes to be inserted.
     */
    public void writeRoutes(ArrayList<Route> routes) {
        JSONArray array_id = new JSONArray();
        JSONArray aarray_edges = new JSONArray();

        for (int i = 0; i < routes.size(); i++) {
            array_id.put(routes.get(i).getIdRoute());
            aarray_edges.put(routes.get(i).getEdges());
        }

        object.put(routes_id_key, array_id);
        object.put(routes_edge_key, aarray_edges);
    }
    
    /**
     * GET method for routes present in a {@link JSONArray} inside a JSONObject
     * @return {@link ArrayList}<Route> containing the necessary information.
     */
    public ArrayList<Route> receiveRoutes() {
        ArrayList<Route> routes = new ArrayList<>();
        
        JSONArray array_id = (JSONArray) this.object.get(routes_id_key);
        JSONArray array_edge = (JSONArray) this.object.get(routes_edge_key);
        
        for (int i = 0; i < array_id.length(); i++) {
            Route route = new Route(array_id.getString(i), array_edge.getString(i));
            routes.add(route);
        }

        return routes;
    }

    /**
     * Method for sending a message indicating the client's connection to the server.
     * @param login {@link String} containing user login (Alpha Bank) // "Random" message (Company)
     * @param password {@link String} contendo o password do usuário (AlphaBank) // Mensagem "aleatória" (Company)
     */
    public void sendConnection(String login, String password) {
        object.put(command_key, Constants.connection_command);
        object.put(login_key, login);
        object.put(password_key, password);
    }

    /**
     * Receiving data indicating that a connection was made.
     * @return {@link String}[] containing login/password data (AlphaBank) // "Random" messages (Company)
     */
    public String[] receiveConnection() {
        String[] data = new String[2];

        data[0] = (object.getString(login_key));
        data[1] = (object.getString(password_key));

        return data;
    }
}
