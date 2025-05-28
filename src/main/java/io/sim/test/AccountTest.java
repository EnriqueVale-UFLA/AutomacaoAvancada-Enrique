package io.sim.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import io.sim.bank.Account;

public class AccountTest {
    
    /**
     * TTest to check whether the deposit was made correctly, i.e. adding the amount to the current balance.
     */
    @Test
    public void testDeposit() {
        double inicial_balance = 120;
        double deposit_value = 30;
        String expected_result = (inicial_balance + deposit_value) + "";

        Account account = new Account("Teste Unitario", "testing", 120);
        account.deposit(30);
        String balance = account.getCurrentBalance() + "";

        assertEquals(expected_result, balance);
    }
}