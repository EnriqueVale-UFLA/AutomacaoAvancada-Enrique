package io.sim.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import io.sim.shared.Encryption;

public class EncryptionTest {
    /**
     *Test to see if the encryption and decryption methods work properly. 
     */
    @Test
    public void testeencryptgrafadecryptgrafa() {
        String message = "Cryptography Unit Test";
        

        Encryption encryption = new Encryption();

        String encrypt = encryption.encrypts("Encryption Unit Test");
        String decrypt = encryption.decrypts(encrypt);

        assertEquals(message, decrypt);

    }
}