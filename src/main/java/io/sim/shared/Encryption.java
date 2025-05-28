package io.sim.shared;

/**
 * Class to perform the Encryption of the JSON file to be sent through Client/Server communication.
 * 
 * I used as a base:  <a href="https://www.devmedia.com.br/utilizando-criptografia-simetrica-em-java/31170">
 */
public class Encryption {
    public String encrypts(String message) {
        String key = genKey(message.length());
        if (message.length() != key.length()) {
            error("The size of the message and key must be the same.");
        }
        
        int[] im = charArrayToInt(message.toCharArray());
        int[] ik = charArrayToInt(key.toCharArray());
        int[] data = new int[message.length()];

        for (int i = 0; i < message.length(); i++) {
            data[i] = im[i] + ik[i];
        }

        return new String(intArrayToChar(data));
    }

    public String decrypts(String message) {
        String key = genKey(message.length());

        if (message.length() != key.length()) {
                error("The size of the message and key must be the same.");
        }

        int[] im = charArrayToInt(message.toCharArray());
        int[] ik = charArrayToInt(key.toCharArray());
        int[] data = new int[message.length()];

        for (int i=0;i<message.length();i++) {
            data[i] = im[i] - ik[i];
        }

        return new String(intArrayToChar(data));
    }

    public String genKey(int size) {
        String key = "";
        for (int i = 0; i < size; i++) {
            key += "a";
        }

        return key;
    }

    private int charToInt(char c) {
        return (int) c;
    }

    private char intToChar(int i) {
        return (char) i;
    }
    
    private int[] charArrayToInt(char[] cc) {
        int[] ii = new int[cc.length];
        for(int i = 0; i < cc.length; i++){
            ii[i] = charToInt(cc[i]);
        }
        return ii;
    }

    private char[] intArrayToChar(int[] ii) {
        char[] cc = new char[ii.length];
        for(int i = 0; i < ii.length; i++){
        cc[i] = intToChar(ii[i]);
        }
        return cc;
    }

    private void error(String msg) {
        System.out.println(msg);
        System.exit(-1);
    }
}
