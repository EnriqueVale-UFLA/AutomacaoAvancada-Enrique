package io.sim.company;

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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import io.sim.car.Car;
import io.sim.shared.Encryption;
import io.sim.utils.ExcelExport;
import io.sim.utils.JsonUtil;


/**
 * Class that simulates the operation of a Company, being responsible for managing a set of routes, 
 * as well as making payments to drivers per km driven.
 */
public class Company extends Thread {
    //Set of routes: Ready to be executed // Running // Already executed
    private static ArrayList<Route> routes_ready;
    private static ArrayList<Route> executing_routes;
    private static ArrayList<Route> executed_routes;
    private String current_route_id = "";

    //Account access parameters.
    private static final String login = "Company";
    private static final String password = "company";

    //File containing the routes.
    private static final String file = "data/dados2.xml";

    //Client-Server Communication
    private static ServerSocket server;
    private Socket socket;
    private InputStream in;
    private InputStreamReader inr;
    private BufferedReader bfr;
    private static Socket socket_client;
    private static OutputStream ou ;
    private static Writer ouw;
    private static BufferedWriter bfw;

    private double paid_distance = 0;

    //------------------------------------------------------------------------------------------------------
    
    /**
     * Class for making payments to drivers in the amount of R$3.25 per km driven.
     */
    private class BotPayment extends Thread{
        private final double payment_value = 3.25;
        private final String driver_id;
        private final String login;
        private final String password;

        public BotPayment(String driver_id, String login, String password) {
            this.driver_id = driver_id;
            this.login = login;
            this.password = password;
        }

        @Override
        public void run() {
            // System.out.println("Thread started PAYMENT" + this.login + " : " + System.nanoTime());      //Novo
            JsonUtil jsonUtil = new JsonUtil();
            Encryption encryption = new Encryption();

            jsonUtil.writePaymentData(this.login, this.password, this.driver_id, this.payment_value);

            String json = jsonUtil.getJSONAsString();

            try {
                bfw.write(encryption.encrypts(json) +"\r\n");
                bfw.flush();
            } catch (IOException e) {
                System.out.println("Error writing JSON with payment to AlphaBank.\nException: " + e);
            }

            // System.out.println("Ended Thread PAYMENT + this.login + " : " + System.nanoTime());     //Novo
        }
    }

    /**
     * Constructor for the Company class, to be used as a Thread and Server.
     * 
     * @param socket {@link Socket} containing the client/server communication path
     * (Definition: <a href="https://www.devmedia.com.br/como-criar-um-chat-multithread-com-socket-em-java/33639">
     */
    public Company(Socket socket) {

        this.socket = socket;
        try {
            in  = socket.getInputStream();
            inr = new InputStreamReader(in);
            bfr = new BufferedReader(inr);
        } catch (IOException e) {
        }
    }

    /**
     * Method Run() - Thread.
     */
    @Override
    public void run() {

        try{
            String msg;
            OutputStream ou =  this.socket.getOutputStream();
            Writer ouw = new OutputStreamWriter(ou);
            BufferedWriter bfw = new BufferedWriter(ouw);
            //msg = bfr.readLine();
            msg = "begin";

            //System.out.println(msg);
            // Thread.sleep(300);

            while (msg != null) {
                msg = bfr.readLine();

                Encryption encryption = new Encryption();
                String decrypt = encryption.decrypts(msg);
                JsonUtil jsonUtil = new JsonUtil(decrypt);

                String command = jsonUtil.getCommand();

                switch (command) {
                    case Constants.routes_command -> sendRoutes(jsonUtil, bfw);
                    case Constants.report_command -> carReport(jsonUtil);
                    case Constants.connection_command -> Thread.sleep(300);
                    default -> {
                    }
                }
            }
            
        } catch (IOException | InterruptedException e) {
            //e.printStackTrace();
        }
    }

    /**
     * Receives a car report and performs the necessary operations based on the information sent by the vehicle.
     * @param jsonUtil {@link JsonUtil} containing the object to be manipulated to retrieve information.
     */
    private void carReport(JsonUtil jsonUtil) {
        ArrayList<Object> arrayList = jsonUtil.catchReport();

        String Car_id = (String) arrayList.get(1);
        String route_id = (String) arrayList.get(2);
        double distance = convertNumber(arrayList.get(4));

        if (!this.current_route_id.equals(route_id)) {
            if (!this.current_route_id.equals("")) {
                executed_routes.add(searchExecutingRoute(this.current_route_id));
            }
            executing_routes.add(searchReadyRoute(route_id));
            this.current_route_id = route_id;
        }

        //System.out.println(distance + "//" + paid_distance);

        if (distance < paid_distance) {
            paid_distance = 0;
            
            // //Generate Excel with the data!
            // ExportExcel excel = new ExportExcel();
            // excel.writeReport(arrayList);
        } else {
            if (distance - paid_distance >= 1) {
                paid_distance = distance;
                String driver_id = "Driver_" + Car_id.split("_")[1];
                BotPayment botPayment = new BotPayment(driver_id, login, password);
                // System.out.println("Called Thread PAYMENT" + driver_id + " : " + System.nanoTime());
                botPayment.start();

                // //Generate Excel with the data!
                // System.out.println("Completed 1km");
                // ExportExcel excel = new ExportExcel();
                // excel.writeReconciliation(arrayList);
            }
        }

        //Generate Excel with the data!
        ExcelExport excel = new ExcelExport();
        excel.writeReport(arrayList);
        excel.start();
    }

    /**
     * Number conversion method, considering that the JSON file call changes the type of the numbers
     * sent (double -> int -> BigDecimal);
     * @param number {@link Object} containing the type of number received
     * @return {@link Double} containing the number in the "proper" type.
     */
    private double convertNumber(Object number) {
        try {
            return (double) number;
        } catch (Exception e) {
            try {
                return (Integer) number;
            } catch (Exception e2) {
                return ((BigDecimal) number).doubleValue();
            }
        }
    }

    /**
     * Looks for ready-to-run routes in the {@link Company#routes_ready} attribute.
     * @param route_id {@link String} containing the ID of the route to be searched.
     * @return {@link Route} containing the found object // Null if none were found.
     */
    private Route searchReadyRoute(String route_id) {
        for (int i = 0; i < routes_ready.size(); i++) {
            if (routes_ready.get(i).getIdRoute().equals(route_id)) {
                return routes_ready.get(i);
            }
        }

        System.out.println("Route not found.");
        return null;
    }

    /**
     * Looks for ready-to-be-executed routes in the {@link Company#executing_routes} attribute.
     * @param route_id {@link String} containing the ID of the route to be searched.
     * @return {@link Route} containing the found object // Null if none were found.
     */
    private Route searchExecutingRoute(String route_id) {
        for (int i = 0; i < executing_routes.size(); i++) {
            if (executing_routes.get(i).getIdRoute().equals(route_id)) {
                return executing_routes.get(i);
            }
        }

        System.out.println("Route not found.");
        return null;
    }

    /**
     * Sends the requested routes for a specific {@link Car}.
     * @param jsonUtil {@link JsonUtil} containing the instance received through communication.
     * @param command {@link Strind} containing the command used ("command").
     * @param bfw {@link BufferedWriter} containing who sent the message (for return).
     */
    private void sendRoutes(JsonUtil jsonUtil, BufferedWriter bfw) {
        ArrayList<Route> routes = new ArrayList<>();

        String[] id = jsonUtil.receiveRequestRoutes().split("_");
        
        int num = Integer.parseInt(id[1]);

        for (int i = (num-1)*9; i < num*9; i++) {
            routes.add(routes_ready.get(i));
        }

        jsonUtil.writeRoutes(routes);

        String json = jsonUtil.getJSONAsString();
        
        Encryption encryption = new Encryption();

        try {
            bfw.write(encryption.encrypts(json) +"\r\n");
            bfw.flush();
        } catch (IOException e) {
            System.out.println("Error writing JSON back to Car.\nException: " + e);
        }
    }

    /**
     * Gets all routes from the XML file titled through the variable {@link Company#file};
     * @return {@link NodeList} containing all the routes present in the file.
     */
    private static ArrayList<String> routesList() {
        ArrayList<String> routes = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            NodeList nList = doc.getElementsByTagName("vehicle");
            
            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element elem = (Element) nNode;
					Node node = elem.getElementsByTagName("route").item(0);
					Element edges = (Element) node;
					routes.add(edges.getAttribute("edges"));
				}
            }

        } catch (IOException | ParserConfigurationException | SAXException e) {
            System.out.println("Error in the list of available routes (Company.java)\nException: " + e);
        }

        return routes;
    }

    /**
     * Fills the ArrayList with routes ready to be executed.
     * @param routes ArrayList containing the coordinates (?) (Edges) of the chosen routes (total of 900 routes).
     */
    private static void fillReadyRoutes(ArrayList<String> routes)  {
        for (int i = 0; i < routes.size(); i++) {
            String id = "Route_" + (i+1);
            Route route = new Route(id, routes.get(i));
            routes_ready.add(route);
        }
    }

    /**
     * Get() method for {@link Company#routes_ready} attribute
     * @return {@link ArrayList} containing the routes to be executed.
     */
    public ArrayList<Route> getReadyRoutes() {
        return routes_ready;
    }

    /**
     * Get() method for {@link Company#executing_routes} attribute
     * @return {@link ArrayList} containing the running routes.
     */
    public ArrayList<Route> getExecutingRoutes() {
        return executing_routes;
    }

    /**
     * Get() method for {@link Company#executed_routes} attribute
     * @return {@link ArrayList} containing the routes that have already been executed.
     */
    public ArrayList<Route> getExecutedRoutes() {
        return executed_routes;
    }

    /**
     * Method to connect to the Alpha Bank Server, since the company has an account there.
     */
    public static void connect() {
        try {
            String IP = Constants.IP_COMPANY + "1";//+ (clientes.size() + 1);

            socket_client = new Socket(IP, Constants.port_AlphaBank);
            ou = socket_client.getOutputStream();
            ouw = new OutputStreamWriter(ou);
            bfw = new BufferedWriter(ouw);

            JsonUtil jsonUtil = new JsonUtil();
            jsonUtil.sendConnection(login, password);
            String encrypts = new Encryption().encrypts(jsonUtil.getJSONAsString());

            bfw.write(encrypts + "\r\n");
            bfw.flush();

        } catch (IOException e) {
            System.out.println("Error connecting to Alpha Bank Server.\nException: " + e);
        }
        
    }

    /**
     * Sets a route that is being executed by a car as running.
     * @param idRoute {@link String} containing the ID of the running route.
     */
    public static void setExecutingRoutes(String idRoute) {
        for (int i = 0; i < routes_ready.size(); i++) {
            if (idRoute.equals(routes_ready.get(i).getIdRoute())) {
                Route route = routes_ready.get(i);

                executing_routes.add(route);
                routes_ready.remove(route);

                return;
            }
        }
    }

    /**
     * Sets a route that has already been executed by a car as executed.
     * @param idRoute {@link String} containing the ID of the running route.
     */
    public static void setExecutedRoute(String idRoute) {
        for (int i = 0; i < executing_routes.size(); i++) {
            if (idRoute.equals(executing_routes.get(i).getIdRoute())) {
                Route route = executing_routes.get(i);

                executed_routes.add(route);
                executing_routes.remove(route);

                return;
            }
        }
    }

    /**
     * Main class execution method. Starts the Company server.
     * @param args
     */
    public static void main (String[] args) {
        connect();
        
        routes_ready = new ArrayList<>();
        executing_routes = new ArrayList<>();
        executed_routes = new ArrayList<>();

        fillReadyRoutes(routesList());

        Thread thread_company = new Thread(() -> {
            try{
                // Creates the objects needed to instantiate the server
                server = new ServerSocket(Constants.port_Company);
                
                while(true){
                    //System.out.println("Waiting for connection...");
                    Socket con = server.accept();
                    //System.out.println("Company connected");
                    
                    Company company = new Company(con);
                    company.start();
                }
                
            }catch (IOException e) {
            }
        });
        
        thread_company.start();
    }
}
