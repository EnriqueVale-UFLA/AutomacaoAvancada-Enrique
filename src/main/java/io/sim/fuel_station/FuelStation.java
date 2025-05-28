package io.sim.fuel_station;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;

import io.sim.car.Car;
import io.sim.company.Constants;
import io.sim.shared.Encryption;
import io.sim.utils.JsonUtil;

/**
 * Class that aims to simulate the operation of a Gas Station, being responsible for
 * supplying vehicles with less than 3L of fuel. However, there are only two
 * pumps available, and if the pumps are occupied the car must wait.
 */
public class FuelStation implements Runnable{

    //Client-Server Communication
    private static Socket socket;
    private static OutputStream ou ;
    private static Writer ouw;
    private static BufferedWriter bfw;

    //Regarding Account access.
    private static final String login = "FuelStation";
    private static final String password = "fuel_station";

    //Flag for bomb 1
    private static boolean bomb1_occupied = false;

    //Flag for bomb 2
    private static boolean bomb2_occupied = false;

    //Contains the pump used by the car trying to be fueled
    private int used_bomb;

    //Object of the car whose fueling is being carried out.
    Car filling_car;

    //Quantity of liters to be filled.
    private final double filling_liters;

    //Flag to know if the refueling process is finished!
    private boolean filling;

    //-----------------------------------------------------------------------------------------------------------
    /**
     * FuelStation builder, for using your Thread.
     * @param filling_car {@link Car} being the car whose fueling will be carried out.
     * @param filling_liters {@link Double} containing the amount, in liters, to be filled in the car.
     */
    public FuelStation(Car filling_car, double filling_liters, boolean filling) {
        this.filling_car = filling_car;
        this.filling_liters = filling_liters;
        this.filling = filling;
    }

    @Override
    public void run() {
        try {
            // System.out.println("Thread started FILLING " + this.filling_car.getID() + " : " + System.nanoTime());     //Novo
            System.out.println(this.filling_car.getID() + " filling...");

            //Duration of supply.
            Thread.sleep(120000);

            filling_car.fillingFuelTank(this.filling_liters);

            this.filling = false;

            System.out.println("Leaving station...");
            
            if (this.used_bomb == 1) {
                bomb1_occupied = false;
            } else {
                bomb2_occupied = false;
            }

            // System.out.println("Ended Thread FILLING " + this.filling_car.getID() + " : " + System.nanoTime());    //Novo
        } catch (InterruptedException e) {
            System.out.println("Vehicle fueling failure");
        }
    }

    /**
     * Checks whether FILLING is possible, i.e., whether either of the two pumps is free.
     * If so, returns the number of the pump that can be used (1 or 2). If both pumps are busy, returns 0.
     * @return {@link Integer} containing the number of the pump to be used // 0 if the pumps are busy.
     */
    public synchronized static int tryFilling() {
        if (!bomb1_occupied) {
            bomb1_occupied = true;
            return 1;
        } else if (!bomb2_occupied) {
            bomb2_occupied = true;
            return 2;
        } else {
            return 0;
        }
    }
   

    /**
     * Starts the execution of the Thread to execute the filling function.
     * @param num {@link Integer} containing the number of the pump to be used.
     */
    public void startThread(int num) {
        Thread thread = new Thread(this);
        this.used_bomb = num;
        thread.start();
    }

    /**
     * GET method for the {@link FuelStation#filling} attribute.
     * @return {@link Boolean} indicating the value contained in the attribute.
     */
    public boolean getFilling() {
        return this.filling;
    }


    public static void main(String[] args) {
        connect();
    }

    /**
     * Method to connect to the Alpha Bank Server, since the gas station (FuelStation) has an account there.
     */
    public static void connect() {
        try {
            String IP = Constants.IP_FUEL_STATION; 

            socket = new Socket(IP, Constants.port_AlphaBank);
            ou = socket.getOutputStream();
            ouw = new OutputStreamWriter(ou);
            bfw = new BufferedWriter(ouw);

            JsonUtil jsonFile = new JsonUtil();
            jsonFile.sendConnection(login, password);
            String encrypts = new Encryption().encrypts(jsonFile.getJSONAsString());

            bfw.write(encrypts + "\r\n");
            bfw.flush();

        } catch (IOException e) {
            System.out.println("Error connecting to AlphaBank Server. Connecting again... \nException: " + e);
            connect();
        }
        
    }
}
