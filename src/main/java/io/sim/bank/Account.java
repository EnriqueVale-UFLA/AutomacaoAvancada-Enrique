package io.sim.bank;

import java.util.ArrayList;

import io.sim.utils.ExcelExport;

/**
 * Class that aims to simulate the operation of a Current Account, 
 * with all customers of the Bank ({@link AlphaBank}) having a registered account. 
 * It should be noted that the account has a method with login and password.
 */
public class Account implements Runnable { 

    //Account access parameters
    private final String login;
    private final String password;

    //Account balance references - Before and after transactions.
    private double current_balance;
    private double previous_balance;

    //Destination account of the operation performed (only in case of Withdrawal, indicating transfer).
    private String destiny;

    //Thread object for execution.
    private Thread thread;

    //Flag to mark the end of the statement.
    private boolean completed_transaction = false;

    //------------------------------------------------------------------------------------------------
    /**
     * Constructor class.
     * @param login {@link String} containing the user login to access the Account.
     * @param password {@link String} containing the user's password for accessing the Account.
     * @param balance {@link Double} containing the initial balance of the Account.
     */
    public Account(String login, String password, double balance) {
        this.login = login;
        this.password = password;
        this.current_balance = balance;
    }

    
    // Starts the execution of the thread.
    public void startThread() {
        completed_transaction = false;
        thread = new Thread(this);
        thread.start();
    }

    
    // Stop thread execution
    public void stopThread() {
        thread.interrupt();
    }

    // Thread execution method
    @Override
    public void run() {

        // Creating objects
        ExcelExport excel = new ExcelExport();
        ArrayList<Object> arrayList = new ArrayList<>();

        // Adding account data to the array
        arrayList.add(System.nanoTime());
        arrayList.add(this.login);
        arrayList.add(this.destiny);
        arrayList.add(this.previous_balance);
        arrayList.add(this.current_balance);

        // Calls the statement writing method and finalizes the transaction
        extract(arrayList, excel);
        completed_transaction = true;
    }

    /**
     * Method for writing the extract to the report file. 
     * Designed to not allow simultaneous access by Threads.
     * @param arrayList {@link ArrayList} containing the data to be inserted.
     * @param excel {@link ExcelExport} containing the object for manipulating the file.
     */
    private static synchronized void extract(ArrayList<Object> arrayList, ExcelExport excel) {
        excel.writeExtract(arrayList);
    }

    /**
     * Represents the withdrawal of money from an Account.
     * @param value {@link Double} containing the amount to be withdrawn.
     */
    public void withdraw(double value, String withdraw_destination) {
        this.destiny = withdraw_destination;
        this.previous_balance = this.current_balance;
        this.current_balance -= value;
    }

    /**
     * Represents the deposit of money into an Account.
     * @param value {@link Double} containing the amount to be deposited.
     */
    public void deposit(double value) {
        this.destiny = "-";
        this.previous_balance = this.current_balance;
        this.current_balance += value;
    }

    /**
     * GET method for account balance.
     * @return {@link Double} containing the Account balance value.
     */
    public double getCurrentBalance() {
        this.destiny = "-";
        this.previous_balance = this.current_balance;
        return this.current_balance;
    }
    
    /**
     * GET method for account login.
     * @return {@link String} containing the Account user login.
     */
    public String getLogin() {
        return this.login;
    }

    /**
     * GET method for account password.
     * @return {@link String} containing the Account user password.
     */
    public String getPassword() {
        return this.password;
    }


    /**
     * GET method for the {@link Account completed_transaction} attribute.
     * @return {@link Boolean} ccontaining the value contained in the attribute.
     */
    public boolean getCompletedTransaction() {
        return this.completed_transaction;
    }
}
