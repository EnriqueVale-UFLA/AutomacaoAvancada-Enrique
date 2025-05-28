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

import io.sim.bank.Account;
import io.sim.company.Constants;
import io.sim.company.Route;
import io.sim.fuel_station.FuelStation;
import io.sim.shared.Encryption;
import io.sim.utils.JsonUtil;

 /**
  * Class that simulates a Driver, whose responsibility is to execute routes using his {@link Car},
  * in addition to refueling it as necessary, using his {@link Account}.
  */
public class Driver extends Thread{

    //Attributes related to the car.
    private final Car car;
    private ArrayList<Route> routes_ready;
    private Route executing_routes;
    private ArrayList<Route> executed_routes;
    private final String ID;
    
    //Flags
    private boolean filling = false;

    //Attributes related to the Account.
    private final String password = "driver";

    //Client-Server Communication
    private final String IP;
    private Socket socket_client;
    private OutputStream os;
    private Writer writer;
    private BufferedWriter bfw;

    //----------------------------------------------------------------------------------------------------------------------
    /**
     * Class for making payments to drivers in the amount of R$3.25 per km driven.
     */
    private class BotPayment extends Thread{
        private final String fuelStation = "FuelStation";
        private final String login;
        private final String password;
        private final double payment_value;

        /**
         * BotPayment constructor.
         * @param login {@link String} containing the login of the account from which the payment was made.
         * @param password {@link String} containing the password for the account from which the payment was made.
         * @param liters {@link Double} containing the amount, in liters, to be paid.
         */
        public BotPayment(String login, String password, double liters) {
            this.login = login;
            this.password = password;
            this.payment_value = liters*5.87;
        }

        @Override
        public void run() {
            System.out.println("Thread started PAYMENT_STATION_" + this.login + " : " + System.nanoTime());       //Novo
            JsonUtil jsonFile = new JsonUtil();
            Encryption encryption = new Encryption();

            jsonFile.writePaymentData(this.login, this.password, this.fuelStation, this.payment_value);

            String json = jsonFile.getJSONAsString();

            try {
                bfw.write(encryption.encrypts(json) +"\r\n");
                bfw.flush();
            } catch (IOException e) {
                System.out.println("Error writing JSON with payment to AlphaBank.\nException: " + e);
            }

            System.out.println("Thread ended PAYMENT_STATION_" + this.login + " : " + System.nanoTime());       //Novo
        }
    }

    /**
     * Constructor for the Driver class.
     * @param ID {@link String} containing the ID of the car belonging to the driver (assigned based on the order of creation).
     * @param IP {@link String} containing the IP address associated with the Driver (DRIVERS contains the "default" address 127.0.1.x).
     * @param car {@link Car} containing the object belonging to the class.
     */
    public Driver(String ID, String IP, Car car) {
        this.IP = IP;
        this.ID = ID;
        this.car = car;

        this.executed_routes = new ArrayList<>();

        connect();
    }

    @Override
    public void run() {
        // System.out.println("Thread started " + this.ID + " : " + System.nanoTime());      //Part 2
        this.routes_ready = car.retrieveRoutes();

        try {
            for (int i = 0; i < this.routes_ready.size(); i++) {
                car.getSumo().do_job_set(this.routes_ready.get(i).addRouteSumo());
            }
        } catch (Exception e) {
            System.out.println("Error inserting Routes in Sumo.\nException: " + e);
        }

        //For testing with data reconciliation and Escalation:
        while (this.executed_routes.size() < 1) {                                          //Part 2
        // while (this.executed_routes.size() < 9) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }

            car.triggerRoute(this.routes_ready.get(0).getIdRoute());

            this.executing_routes = this.routes_ready.get(0);
            this.routes_ready.remove(0);

            //Initializes sending of car reports.
            this.car.setRouteEnded(false);
            // System.out.println("Called Thread CAR" + this.ID + " : " + System.nanoTime());        //Part2
            this.car.startThread();

            while (!this.car.getRouteEnded()) {
                if (this.car.getFlag()) {
                
                    this.car.stopThread();
                    this.car.startThread();
                }
                checkFuel();
            }

            this.car.stopThread();

            // System.out.println("Ended 1!");
            this.executed_routes.add(this.executing_routes);
        }
    }

    /**
     * Driver checks whether his car ({@link Driver#car}) needs to be refueled and/or if it is not filling.
     * If both options are true, the driver triggers the method to refuel the car.
     */
    private void checkFuel() {
        try{
            System.out.print("");
            if (car.getFuelTank() <= 3 && !filling) {

                this.filling = true;

                double balance = consultBalance();
                double liters;

                if (balance > 58.7) {
                    liters = 10;
                } else {
                    liters = balance/5.87;
                }

                BotPayment botPayment = new BotPayment(this.ID, this.password, liters);
                // System.out.println("Called Thread PAYMENT_STATION_" + this.ID + " : " + System.nanoTime());   //Part2
                botPayment.start();

                FuelStation fuelStation = new FuelStation(car, liters, true);

                int allowed_bomb = FuelStation.tryFilling();
                
                // System.out.println("Chamou a Thread ABASTECER" + this.car.getID() + " : " + System.nanoTime());    //Novo
                
                while (allowed_bomb == 0) {
                    Thread.sleep(200);
                    allowed_bomb = FuelStation.tryFilling();
                }

                fuelStation.startThread(allowed_bomb);

                while (fuelStation.getFilling()) {
                    Thread.sleep(200);
                }

                this.filling = false;
            }
        } catch (InterruptedException e) {
            System.out.println("Filling failed.\nException: " + e);
        }
    }

    /**
     * Server Connection (AlphaBank)
     */
    public void connect() {
        try {
            int port_AlphaBank = Constants.port_AlphaBank;

            socket_client = new Socket(this.IP, port_AlphaBank);

            os = socket_client.getOutputStream();
            writer = new OutputStreamWriter(os);
            bfw = new BufferedWriter(writer);

            JsonUtil jsonFile = new JsonUtil();
            jsonFile.sendConnection(this.ID, this.password);
            String encrypts = new Encryption().encrypts(jsonFile.getJSONAsString());

            bfw.write(encrypts +"\r\n");
            bfw.flush();
        } catch (IOException e) {
            System.out.println("Error connecting Alpha Bank Server.\nException: " + e);
            connect();
        }
        
    }

    /**
     * Perform a balance check to analyze how much gasoline you can fill up with.
     */
    private double consultBalance() {
        JsonUtil jsonFile = new JsonUtil();

        jsonFile.writeConsultBalance(this.ID, this.password);

        String msgJson = jsonFile.getJSONAsString();

        Encryption encryption = new Encryption();

        String msg_encrypted = encryption.encrypts(msgJson);

        try {
            bfw.write(msg_encrypted + "\r\n");
            bfw.flush();
        } catch (IOException e) {
            System.out.println("Failed to send message for Balance Query.\n Exception: " + e);
        }

        return receiveBalanceServer();
    }

    /**
     * Method to receive the balance value contained in your Account.
     */
    private double receiveBalanceServer() {
        try {
            Encryption encryption = new Encryption();

            InputStream in = this.socket_client.getInputStream();
            InputStreamReader inr = new InputStreamReader(in);
            BufferedReader bfr = new BufferedReader(inr);
            String msg = "";
            msg = bfr.readLine();

            String desencrypts = encryption.decrypts(msg);

            JsonUtil jsonFile = new JsonUtil(desencrypts);

            return jsonFile.receiveBalance();
        } catch (IOException e) {
            System.out.println("Error receiving message from Server.\nException: " + e);
        }

        return 0;
    }

    /**
     * GET method for Ready routes related to this Driver/Car.
     * @return {@link ArrayList} containg Routes
     */
    public ArrayList<Route> getRoutesReady() {
        return routes_ready;
    }

    /**
     * GET method for the Executed routes related to this Driver/Car.
     * @return {@link ArrayList} containing Routes
     */
    public ArrayList<Route> getExecuteRoutes() {
        return executed_routes;
    }

    /**
     * GET method for the running route relative to this Driver/Car.
     * @return {@link Route2} containing current Route
     */
    public Route getExecutingRoutes() {
        return executing_routes;
    }

    public void setRoutes_ready(ArrayList<Route> routes_ready) {
        this.routes_ready = routes_ready;
    }
    

    public void setExecutingRoutes(Route executing_routes) {
        this.executing_routes = executing_routes;
    }

    public void setExecutedRoutes(ArrayList<Route> executed_routes) {
        this.executed_routes = executed_routes;
    }
}
