package io.sim.test;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import io.sim.utils.JsonUtil;

public class JsonUtilTest {
    /**
     * Test to see if the method of receiving connection data, using the JSON file, is adequate.
     */
    @Test
    public void testeRecebersendConnection() {
        String login = "Unit Testing";
        String password = "testing";

        JsonUtil jsonFile = new JsonUtil();
        jsonFile.sendConnection(login, password);

        String[] answers = jsonFile.receiveConnection();

        assertEquals(login, answers[0]);

        assertEquals(password, answers[1]);
    }
}