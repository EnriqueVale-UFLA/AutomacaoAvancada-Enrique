package io.sim.app;

import java.util.ArrayList;

import io.sim.bank.AlphaBank;
import io.sim.car.Car;
import io.sim.car.Driver;
import io.sim.company.Company;
import io.sim.company.Constants;
import io.sim.fuel_station.FuelStation;
import io.sim.utils.ExcelExport;
import it.polito.appeal.traci.SumoTraciConnection;

/**
 * Main class for initializing services.
 */
public class App {

    private static final int num_drivers = 100;  //Number of drivers/cars
    private static boolean sumo_control = true; //Boolean that checks the execution of the SUMO simulator

    /**
     * Main method that initializes the entire system taken as a base from the 
     * EnvSimulator.java file present in this repository
     */
    public static void main( String[] args ) {

        SumoTraciConnection sumo;
        String sumo_bin = "sumo-gui";		
		String config_file = "map/map.sumo.cfg";
		
		// Sumo connection
		sumo = new SumoTraciConnection(sumo_bin, config_file);
		sumo.addOption("start", "1"); // auto-run on GUI show
		sumo.addOption("quit-on-end", "1"); // auto-close on end

        try {
            sumo.runServer(12345);
        } catch (Exception e) {
            System.out.println("Erro ao iniciar o Sumo.\nException: " + e);
        }
        
        /**
         * End of commands used from EnvSimulator.java
         */

        // Initialization of necessary classes
        AlphaBank.main(args);
        FuelStation.main(args);
        ExcelExport.main(args);
        ExcelExport excel = new ExcelExport();
        excel.start();
        Company.main(args);


        //Initializing all drivers and their cars, saving them to an Array List
        ArrayList<Driver> drivers = new ArrayList<>();
        ArrayList<Car> cars = new ArrayList<>();

        // Individual creation of each car and driver where part of the 
        // IP is saved inside the Constants class
        for (int i = 1; i <= num_drivers; i++) {
            String id_car = "Car_" + i;
            String IP_car = Constants.IP_CAR + i;
            Car car = new Car(id_car, IP_car, sumo);
            cars.add(car);
            
            String id_driver = "Driver_" + i;
            String IP_driver = Constants.IP_DRIVER + i;
            Driver driver = new Driver(id_driver, IP_driver, car);
            drivers.add(driver);
        }

        // for (int i = 0; i < num_drivers; i++) {
        //     drivers.get(i).start();
        // }

        // For data reconciliation.
        drivers.get(2).start();                                             

        // System.out.println("Got in line DRIVER 1: " + System.nanoTime());     
        // drivers.get(0).start();                                             
        // System.out.println("Got in line DRIVER 4: " + System.nanoTime());      
        // drivers.get(3).start();                                            

        //Keeping the simulator running inside a Thread
        Thread thread = new Thread(() -> {
            while (sumo_control) {
                try {
                    sumo.do_timestep();
                    Thread.sleep(500);
                    
                    if (sumo.isClosed()) {
                        sumo_control = false;
                        System.out.println("SUMO is closed...");
                    }
                } catch (Exception e) {
                    sumo_control = false;
                    System.out.println("Error in do_timestep.\nException: " + e);
                }
            }
            
            ExcelExport.setFlag(false);
        });

        thread.start();

        try {           
            drivers.get(2).join();                                                                         
            // drivers.get(0).join();                                                        
            // drivers.get(3).join();                                                        

            // System.out.println("Thread Ended - Thread DRIVER_1" + " : " + System.nanoTime());       
            // System.out.println("Thread Ended - Thread DRIVER_4" + " : " + System.nanoTime());       

        } catch (InterruptedException e) {                                                                 
            System.out.println("Error when trying Join");                                         
        }                                                                                       

        // try {
        //     for (int i = 0; i < num_drivers; i++) {
        //         drivers.get(i).join();
        //     }

        //     thread.join();
        // } catch (Exception e) {
        //     System.out.println("Error starting Driver Threads.\nException: " + e);
        // }

        
    }

    public static int getNum_drivers() {
        return num_drivers;
    }
}