package io.sim.car;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.objects.SumoColor;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoStringList;
import io.sim.company.Company;
import io.sim.company.Constants;
import io.sim.company.Route;
import io.sim.shared.Encryption;
import io.sim.utils.ExcelExport;
import io.sim.utils.JsonUtil;
import io.sim.utils.Reconciliation;
import it.polito.appeal.traci.SumoTraciConnection;


/**
* Class whose objective is to simulate a Car, and which is responsible for actually executing routes,
* which are owned by {@link Company}, in addition to sending reports to it with execution details.
 */
public class Car extends Vehicle implements Runnable{

    //Sumo Car Parameters
    private SumoTraciConnection sumo;
    private final int fuelType = 2;                 // 1-diesel, 2-gasoline, 3-ethanol, 4-hybrid
    private final int personCapacity = 1;
    private final int personNumber = 1;
	private final long acquisitionRate = 500;
    private String fuel_type;
    private SumoColor color;

    //Car parameters as object.
    private String ID;
    private double fuel_tank = 3.01;
    // private double fuel_tank = 3.01;                
    private double distance_traveled;
    private boolean route_ended = false;

    //Parameters for Data reconciliationtion.
    private double last_distance;               
    private long last_time = 0;                
    private double[] y = new double[] {760, 51.05, 24.15, 31.98, 32.54, 31.84, 194.38, 32.32, 27.40,
                                            153.63, 33.0, 43.83, 26.14, 38.51, 44.76, 64.32, 53.90};                //Part2
    private double[] v = new double[] {0.5, 0.5719, 3.0751, 6.5083, 6.8557, 3.0186, 2545.3587, 11.7552, 17.1524,
                                          23.1312, 1.0435, 9.5197, 1.2204, 4.5040, 2.9270, 25.4682, 34.9596};       //Part2
    private double[][] A = new double[][] {{1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}};    //Part2
    double speed = 0;                           //Part2

    //Object of the Thread to be executed (due to the implements used)
    private Thread thread = new Thread(this);

    //Client-Server Communication
    private Socket client_socket;
    private OutputStream os;
    private Writer writer;
    private BufferedWriter bfw;
    private String IP;

    private boolean flag_try_catch = false;

    //-------------------------------------------------------------------------------------------------------

    /**
     * Class Constructor.
     * @param ID {@link String} containing card Id.
     * @param IP {@link String} containing the IP address for connecting to the server ({@link Company}).
     * @param sumo {@link SumoTraciConnection} used to perform operations with Sumo.
     */
    public Car(String ID, String IP, SumoTraciConnection sumo) {
        this.ID = ID;
        this.IP = IP;
        this.sumo = sumo;
        this.color = genColor();
        this.fuel_type = fuel();

        this.distance_traveled = 0;                  
        this.last_distance = 0;                        

        connect();
    }

    /**
     * Default constructor for testing purposes.
     */
    public Car() {

    }

    /**
     * Connection to server (Company).
     */
    public void connect() {
        try {
            this.client_socket = new Socket(this.IP, Constants.port_Company);

            this.os = client_socket.getOutputStream();
            this.writer = new OutputStreamWriter(os);
            this.bfw = new BufferedWriter(writer);

            JsonUtil jsonFile = new JsonUtil();
            jsonFile.sendConnection("Connected: ", this.ID);
            String encrypts = new Encryption().encrypts(jsonFile.getJSONAsString());

            this.bfw.write(encrypts + "\r\n");
            this.bfw.flush();

        } catch (IOException e) {
            System.out.println(this.ID + " Error connecting to Company Server.\nException: " + e);
            connect();
        }
        
    }

    /**
     * Method to "get" the routes to be executed by the Driver (owner) of the car in question.
     * @return {@link ArrayList} 
     */
    public ArrayList<Route> retrieveRoutes() {
        sendCatchRoutesServer();

        return receiveCatchRoutesServer();
    }

    /**
     * Sends a message to the server to request the routes to be executed by this car.
     */
    public void sendCatchRoutesServer() {
        try {
            JsonUtil jsonFile = new JsonUtil();

            jsonFile.writeRoutesRequest(this.ID);

            String msgJson = jsonFile.getJSONAsString();

            Encryption encryption = new Encryption();

            String msg_encrypt = encryption.encrypts(msgJson);

            this.bfw.write(msg_encrypt + "\r\n");
            this.bfw.flush();

        } catch (IOException e) {
            System.out.println("Error when sending Message to the Server.\nException: " + e);
        }
        
    }

    /**
     * Receives the message from the server containing the data necessary to build a Route ({@link Route}).
     * @return {@link ArrayList}<Route> containing the "rescued" routes from Company.
     */
    private ArrayList<Route> receiveCatchRoutesServer() {
        ArrayList<Route> routes = new ArrayList<>();

        try {
            Encryption encryption = new Encryption();

            InputStream in = this.client_socket.getInputStream();
            InputStreamReader inr = new InputStreamReader(in);
            BufferedReader bfr = new BufferedReader(inr);
            String msg = bfr.readLine();

            String decrypt = encryption.decrypts(msg);

            JsonUtil jsonFile = new JsonUtil(decrypt);

            routes = jsonFile.receiveRoutes();

        } catch (IOException e) {
            System.out.println("Error when receiving Message from Server.\nException: " + e);
        }

        return routes;
        
    }

    /**
     * Starts the execution of the thread.
     */
    public void startThread() {
        thread = new Thread(this);
        thread.start();
    }

    /**
     * Stops the execution of the thread.
     */
    public void stopThread() {
        thread.interrupt();
    }

    /**
     * Execution of the thread, containing the capture of vehicle data (and sending to the Server (Company)), 
     * in addition to fuel verification
     */
    @Override
    public void run() {
        // System.out.println("Execution started" + this.ID + " : " + System.nanoTime());        
        try {
            boolean is_stopped = false;
            boolean flag = true, cars_loaded = false;

            SumoStringList arrayList = (SumoStringList) sumo.do_job_get(Vehicle.getIDList());

            while (!cars_loaded) {
                if (!arrayList.isEmpty() && arrayList.contains(this.ID)) {
                    cars_loaded = true;
                }

                arrayList = (SumoStringList) sumo.do_job_get(Vehicle.getIDList());
            }

            while (flag) {
                //if (arrayList.size() != 0) {
                    arrayList = (SumoStringList) sumo.do_job_get(Vehicle.getIDList());

                    if (!arrayList.isEmpty() && arrayList.contains(this.ID)) {
                        
                        if (last_time == 0) {
                            last_time = System.nanoTime();
                        }

                        double consumed = convertConsumed((double) sumo.do_job_get(Vehicle.getFuelConsumption(ID)), is_stopped);

                        sendReport(consumed, is_stopped);

                        this.fuel_tank -= consumed;

                        //System.out.println("Gasolina: " + this.fuel_tank);

                        if (fuel_tank <= 3) {
                            sumo.do_job_set(Vehicle.setSpeed(ID, 0));
                            if ((double) sumo.do_job_get(Vehicle.getSpeed(ID)) == 0) {
                                is_stopped = true;
                            }
                        } else {
                            if (is_stopped) {
                                is_stopped = false;
                                sumo.do_job_set(Vehicle.setSpeed(ID, 20.60));
                            }
                        }

                        //Update Edge currently.
                        //current_edge = (Integer) sumo.do_job_get(super.getRouteIndex(ID));
                        Thread.sleep(acquisitionRate);
                    } else {
                        flag = false;
                    }
                //}            
            }

            // System.out.println(distance_traveled);
        } catch (Exception e) {
            flag_try_catch = true;
        }

        route_ended = true;
        // System.out.println("End of Thread " + this.ID + " : " + System.nanoTime()); 
    }


    /**
     * Method for sending the report, containing data relating to the car's performance, including:
     * <ol>
     *      <li> Timestamp in nanoseconds </li>
     *      <li> car ID </li>
     *      <li> ID of the route being executed </li>
     *      <li> Distance traveled </li>
     *      <li> Fuel consumed, in Liters </li>
     *      <li> Type of fuel used (Gasoline) </li>
     *      <li> CO2 Emission </li>
     *      <li> Latitude (x-coordinate) </li>
     *      <li> Longitude (y-coordinate) </li>
     * </ol>
     * @param consumed {@link Double} containing the value of fuel consumed, in Liters.
     * @param is_stopped {@link Boolean} to know if the vehicle is stopped (refueling) or not.
     */
    private void sendReport(double consumed, boolean is_stopped) {
        JsonUtil jsonFile = new JsonUtil();

        double latitude = 0, longitude = 0;
        try {
            SumoPosition2D sumoPosition2D = (SumoPosition2D) sumo.do_job_get(Vehicle.getPosition(ID));
            latitude = sumoPosition2D.x;
            longitude = sumoPosition2D.y;

            double dist = (double) sumo.do_job_get(Vehicle.getDistance(ID));
            if (dist >= 0) {
                distance_traveled = distanceCalculator(dist);
            }

            long time = System.nanoTime();                                 //Part2

            if (distance_traveled - last_distance >= 1) {                //Part2
                last_distance = distance_traveled;                       //Part2
                sumo.do_job_set(Vehicle.setSpeed(ID, reconciliation(time)));     //Part2
            }                                                               //Part2

            if (speed != 0) {                                               //Part2
                sumo.do_job_set(Vehicle.setSpeed(ID, speed));                 //Part2
            }                                                               //Part2

            jsonFile.writeReport(this.ID, 
                                        (String) sumo.do_job_get(Vehicle.getRouteID(ID)),
                                        (double) sumo.do_job_get(Vehicle.getSpeed(ID)), 
                                        distance_traveled, 
                                        consumed,
                                        fuel_type,
                                        (double) sumo.do_job_get(Vehicle.getCO2Emission(ID)),
                                        latitude, longitude,
                                        time);

            String json = jsonFile.getJSONAsString();
            Encryption encryption = new Encryption();

            try {
                bfw.write(encryption.encrypts(json) +"\r\n");
                bfw.flush();
            } catch (IOException e) {
                System.out.println("Error when writing report.\nException: " + e);
            }

        } catch (Exception e) {
            System.out.println(ID + " Error capturing vehicle data.\nException: " + e);
        }
    }

    /**
     * Calculates the distance traveled, in km, given that the command used returns the distance traveled
     * during the timestep, so to speak.
     * @param dist {@link Double} containing the distance traveled during the last timestep in dm (decameter)
     * @return {@link Double} containing the total distance traveled by the vehicle.
     */
    public double distanceCalculator(double dist) {
        double dist_meters = dist/100;

        return dist_meters;
    }

    /**
     * Converts fuel consumption, with the measurement being given in mg/s. 
     * The acquisition rate is taken into account ({@link Car#acquisitionRate}).
     * @param consumed {@link Double} containing the fuel consumed in mg/s
     * @return {@link Double} containing the amount consumed in Liters.
     */
    private double convertConsumed(double consumed, boolean is_stopped) {
        int density = 770000;            // gasoline density: 770 g/L (or 770000 mg/L)

        if (consumed >= 0 && !is_stopped) {
            return (consumed*acquisitionRate)/(density*1000);
        } else {
            return 0;
        } 
    }

    /**
     * Car fuel type, considering the number assigned to the {@link Car#fuelType} attribute.
     * @return {@link String} containing the fuel type
     */
    private String fuel() {
        if (this.fuelType == 1) {
            return "Diesel";
        } else if (this.fuelType == 2) {
            return "Gasoline";
        } else if (this.fuelType == 3) {
            return "Ethanol";
        } else {
            return "Hybrid";
        }
    }

    /**
     * Method to trigger the current Route of the car in Sumo.
     * @param idRoute {@link String} containing the ID of the route to be executed.
     */
    public void triggerRoute(String idRoute) {
        try {
            sumo.do_job_set(Vehicle.addFull(this.ID,
                                            idRoute,
                                            "DEFAULT_VEHTYPE",
                                            "now", 								//depart  
                                            "0", 								//departLane 
                                            "0", 								//departPos 
                                            "0",								//departSpeed
                                            "current",							//arrivalLane 
                                            "max",								//arrivalPos 
                                            "current",							//arrivalSpeed 
                                            "",									//fromTaz 
                                            "",									//toTaz 
                                            "", 								//line 
                                            this.personCapacity,
                                            this.personNumber
                                            ));

            sumo.do_job_set(Vehicle.setColor(this.ID, this.color));

        } catch (Exception e) {
            System.out.println("Error startig Route to Car " + this.ID + ".\nException: " + e);
        }
    }

    /**
     * Generates a "random" color for the Car to be inserted.
     * @return {@link SumoColor} containing the color of the car to be "used.
     */
    private SumoColor genColor() {
        Random random = new Random();

        int color1 = random.nextInt(255);
        int color2 = random.nextInt(255);
        int color3 = random.nextInt(255);

        return new SumoColor(color1, color2, color3, 126);
    }

    /**
     * GET method for the {@link Car#sumo} attribute.
     * @return {@link SumoTraciConnection} contendo o objeto para manipulação.
     */
    public SumoTraciConnection getSumo() {
        return this.sumo;
    }
    
    /**
     * GET method for the {@link Car#fuel_tank} attribute.
     * @return {@link Double} containing the value contained in the attribute.
     */
    public double getFuelTank() {
        return fuel_tank;
    }

    /**
     * GET method for the {@link Car#route_ended} attribute.
     * @return {@link Boolean} containing the logical value of the attribute.
    */
    public boolean getRouteEnded() {
        return this.route_ended;
    }

    public boolean getFlag() {
        return this.flag_try_catch;
    }

    public String getID() {
        return this.ID;
    }

    /**
     * SET method for the {@link Car#route_ended} attribute.
     * @param route_ended {@link Boolean} containing the logical value to be inserted into the attribute.
     */
    public void setRouteEnded(boolean route_ended) {
        this.route_ended = route_ended;
    }

    /**
     * SET method for the {@link Car#fuel_tank} attribute.
     * @param fuel_tank {@link Double} containing the value to be inserted into the attribute.
     */
    public void fillingFuelTank(double fuel_tank) {
        this.fuel_tank += fuel_tank;
    }

    //----------------------------------- Part2 ----------------------------------------

    /**
     * Performs data reconciliation based on the 1km stretch traveled.
     * @param time {@link Long} containing the time between the previous/initial and current measurement.
     * @return {@link Double} containing the speed to be used by the car.
     */
    private double reconciliation(long time) {
        long auxT = (time - last_time)/1000000000;

        y = adjustVector(y, auxT);
        v = adjustVector(v);
        A = adjustVector(A);

        Reconciliation rec = new Reconciliation(y, v, A);
        double[] res = rec.getReconciledFlow();

        // System.out.println("current time: " + time/1000000000.00);
        // System.out.println("previous time: " + last_time/1000000000.00);
        // System.out.println("resulting time: " + res[0] + " " + res[1]);
        System.out.println("Suggested speed: " + 1000/res[1]);

        y = res;

        last_time = time;

        ExcelExport excel = new ExcelExport();
        excel.writeReconciliation(res);

        return 1000/res[1];
    }

    /**
     * Adjust the y vector, considering the time traveled and, consequently, the remainder.
     * @param vector {@link Double[]} containing the vector to be adjusted.
     * @param time {@link Long} containing the past time
     * @return {@link Double[]} containing the adjusted vector (with one less position).
     */
    public double[] adjustVector(double[] vector, long time) {
        int size = vector.length - 1;
        double[] aux = new double[size];

        aux[0] = vector[0] - time;

        for (int i = 1; i < size; i++) {
            aux[i] = vector[i+1];
        }

        if (size >= 2) {
            aux[1] += (vector[1] - time);
        }

        vector = null;
        
        return aux;
    }

    /**
     * Adjusts the vector, basically excluding position 1.
     * @param vector {@link Double[]} containing the vector to be adjusted.
     * @return {@link Double[]} containing the adjusted vector (with one less position).
     */
    public double[] adjustVector(double[] vector) {
        int size = vector.length - 1;
        double[] aux = new double[size];

        aux[0] = vector[0];

        for (int i = 1; i < size; i++) {
            aux[i] = vector[i+1];
        }

        vector = null;
        
        return aux;
    }

    /**
     * Adjusts the matrix, basically deleting the [0][1] position.
     * @param vector {@link Double[][]} containing the matrix to be adjusted.
     * @return {@link Double[][]} containing the adjusted matrix (with one less position).
     */
    public double[][] adjustVector(double[][] vector) {
        int size = vector[0].length - 1;
        double[][] aux = new double[1][size];

        aux[0][0] = vector[0][0];

        for (int i = 1; i < size; i++) {
            aux[0][i] = vector[0][i+1];
        }

        vector = null;
        
        return aux;
    }
}
