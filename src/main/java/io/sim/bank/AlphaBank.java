package io.sim.bank;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import io.sim.company.Constants;
import io.sim.fuel_station.FuelStation; 
import io.sim.shared.Encryption;
import io.sim.utils.JsonUtil; 

/**
 * Class that aims to simulate the operation of a bank (AlphaBank), 
 * which must control the balance of Current Accounts, as well as make payments.
 */
public class AlphaBank extends Thread{

    //ArrayList containing all the bank's customer Accounts.
    private static ArrayList<Account> accounts;

    //ArrayList containing the bank's customers (AlphaBank).
    private static ArrayList<BufferedWriter> clients = new ArrayList<>();

    //Client-Server Communication
    private static ServerSocket server;
    private Socket server_socket;
    private InputStream is;
    private InputStreamReader isr;
    private BufferedReader bfr;

    //----------------------------------------------------------------------------------------------------
    
    /**
     * Server Constructor
     * @param socket {@link Socket} containing the client/server communication instance.
     */
    public AlphaBank(Socket socket) {
        this.server_socket = socket;
        try {
            is = socket.getInputStream();
            isr = new InputStreamReader(is);
            bfr = new BufferedReader(isr);
        } catch (IOException e) {
            System.out.println("Constructor error \nException: " + e);
        }
    }

    @Override
    public void run() {
        try{
            String msg;
            OutputStream ou =  this.server_socket.getOutputStream();
            Writer ouw = new OutputStreamWriter(ou);
            BufferedWriter bfw = new BufferedWriter(ouw);
            clients.add(bfw);

            msg = "begin";

            while (msg != null) {
                msg = bfr.readLine();

                Encryption encryption = new Encryption();
                
                String decrypts = encryption.decrypts(msg);
                
                JsonUtil jsonFile = new JsonUtil(decrypts);

                String commands = jsonFile.getCommand();

                if (commands.equals(Constants.consultation_command) || commands.equals(Constants.payment_command)) {
                    acessAccount(commands, jsonFile, bfw);
                } else if (commands.equals(Constants.connection_command)) {
                    String data[] = jsonFile.receiveConnection();
                    addAccount(data[0], data[1]);
                    //System.out.println(msg);
                    Thread.sleep(200);
                }
            }   

        }catch (IOException | InterruptedException e) {
            //e.printStackTrace();
        }
    }

    /**
     * Method for accessing Accounts, with the bank only performing one access at a time.
     * @param commands {@link String} containing the commands (action to be performed)
     * @param jsonFile {@link JsonUtil} containing the object for using the methods.
     * @param bfw {@link BufferedWriter} containing the reciever for return.
     */
    private synchronized static void acessAccount(String commands, JsonUtil jsonFile, BufferedWriter bfw) {
        if (commands.equals(Constants.consultation_command)) {
            checkBalance(jsonFile, bfw);
        } else if (commands.equals(Constants.payment_command)) {
            makePayment(jsonFile, bfw);      
        }
    }

    /**
     * Check the balance of the requested account
     * @param jsonFile {@link JsonUtil} containing the object for using the methods.
     * @param bfw {@link BufferedWriter} containing the reciever for return.
     */
    private synchronized static void checkBalance(JsonUtil jsonFile, BufferedWriter bfw) {
        ArrayList<String> data = jsonFile.receiveConsultationBalance();

        String login = data.get(0);
        String password = data.get(1);

        Account account = searchAccount(login, password, false);

        //if (account != null) {

        jsonFile.writeBalance(account.getCurrentBalance());

        account.startThread();

        String json = jsonFile.getJSONAsString();

        Encryption encryption = new Encryption();

        while (!account.getCompletedTransaction()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
        }

        try {
            bfw.write(encryption.encrypts(json) +"\r\n");
            bfw.flush();
        } catch (IOException e) {
            System.out.println("Error writing balance consultation.\nException: " + e);
        }

        //}
    }

    /**
     * Makes the payment to a specific account (this could be a {@link Driver} or {@link FuelStation}).
     * @param jsonFile {@link JsonUtil} containing the object for using the methods.
     * @param bfw {@link BufferedWriter} containing the reciever for return.
     */
    private synchronized static void makePayment(JsonUtil jsonFile, BufferedWriter bfw) {
        ArrayList<Object> dataJSON = jsonFile.receiveDataPayment();

        String login = (String) dataJSON.get(0);
        String password = (String) dataJSON.get(1);
        String destiny = (String) dataJSON.get(2);
        double value = ((BigDecimal) dataJSON.get(3)).doubleValue();

        //System.out.println(destiny);

        Account account_owner = searchAccount(login, password, false);
        Account account_destiny = searchAccount(destiny, null, true);

        account_owner.withdraw(value, destiny);
        account_owner.startThread();

        account_destiny.deposit(value);
        account_destiny.startThread();

        while (!account_owner.getCompletedTransaction() && !account_destiny.getCompletedTransaction()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {

            }
        }

        //System.out.println(account_destiny.getBalance());
        
        // is_consulting = false;
        //notify();
    }
    
    /**
     * Add a customer account to the ArrayList {@link Alpha Bank#accounts}.
     * @param login {@link String} containing the user's login.
     * @param password {@link String} containing the login.
     */
    private void addAccount(String login, String password) {
        double inicial_balance;
        
        if (login.equalsIgnoreCase("Company")) {
            inicial_balance = 10000000;
        } else if (login.equalsIgnoreCase("FuelStation")) {
            inicial_balance = 0;
        } else {
            inicial_balance = 100;
        }
        
        Account account = new Account(login, password, inicial_balance);
        accounts.add(account);
    }

    /**
     * Method to search for an Account in the system.
     * @param login {@link String} containing the user's login.
     * @param password {@link String} cotaining the user's password.
     * @param is_paying {@link Boolean} containing an identification to know if it is a payment or not.
     * @return {@link Account} containing the Account being sought // null if no Account is found.
     */
    private static Account searchAccount(String login, String password, boolean is_paying) {
        for (int i = 0; i< accounts.size(); i++) {
            Account account = accounts.get(i);
            if (account.getLogin().equals(login)) {
                if (is_paying) {
                    return account;
                } else if (account.getPassword().equals(password)) {
                    return account;
                }
            }
        }

        System.out.println("No Account with that login/password was found.");
        return null;
    }

    /**
     * Main class execution method. Starts the AlphaBank server.
     * @param args
     */
    public static void main (String[] args) {
        //Initialization of the ArrayList of Accounts
        accounts = new ArrayList<>();

        Thread thread_alphaBank = new Thread(() -> {
            try {
                //Creation of the ArrayList of server clients, 
                //that is, where all those who have a bank account will connect.
                clients = new ArrayList<BufferedWriter>();
                
                //Creation of the server, associated with a port, defined above.
                server = new ServerSocket(Constants.port_AlphaBank);
                
                //Execution to wait for the server's clients to connect to it.
                while (true) {
                    //System.out.println("Waiting for connection...");
                    Socket socket = server.accept();
                    //System.out.println("AlphaBank connected successfully!");
                    
                    Thread thread = new AlphaBank(socket);
                    thread.start();
                }
                
            } catch (IOException e) {
                System.out.println("Error in AlphaBank main execution.java\nException: " + e);
            }
        });
        thread_alphaBank.start();
    }
}
