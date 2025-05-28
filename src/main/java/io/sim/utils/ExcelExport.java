package io.sim.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JOptionPane;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import io.sim.company.Constants;

/**
 * Class for writing data to an Excel file (.xlsx).
 * 
 * For this I used as a base <a href="https://acervolima.com/como-escrever-data-em-planilhas-do-excel-usando-java/"> 
 */
public class ExcelExport extends Thread{

    //Writing data to the .xlsx file.
    private static XSSFWorkbook workbook;
    private static XSSFSheet data_folder;
    private static XSSFSheet extracts_folder;
    private static XSSFSheet reconciliation_folder;                                   //Part2

    //Storage of data from the Company and Account classes (using Excel)
    private static ArrayList<ArrayList<String>> data_set;
    private static ArrayList<ArrayList<String>> extracts_set;
    private static ArrayList<ArrayList<String>> reconciliation_set;             //Part2

    //Operation flag.
    private static boolean flag = true;

    //---------------------------------------------------------------------------------------------------------------

    /**
     * Class Constructor (default).
     */
    public ExcelExport() {

    }

    @Override
    public void run() {
        while(flag) {
            if (!data_set.isEmpty()) {
                writeCellsReport();
            }

            if (!extracts_set.isEmpty()) {
                writeCellsExtract();
            }

            if (!reconciliation_set.isEmpty()) {       //Part2
                writeCellsReconciliation();              //Part2
            }                                       //Part2

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("ExportExcel Thread Sleep failure.\nException: " + e);
            }
        }
    }

    /**
     * Writes the report cells using the data obtained in a given time interval 
     * (defined by the Thread.sleep of the run method).
     */
    private void writeCellsReport() {
        ArrayList<ArrayList<String>> aux = (ArrayList<ArrayList<String>>) data_set.clone();
        data_set = new ArrayList<>();

        for (int i = 0; i < aux.size(); i++) {
            ArrayList<String> data = aux.get(i);

            XSSFRow row = data_folder.createRow(data_folder.getLastRowNum() + 1);
            int cell_id = 0;
            for (int j = 0; j < data.size(); j++) {
                Cell cell = row.createCell(cell_id++);
                cell.setCellValue(data.get(j));
            }
        }
    }

    /**
     * Writes the cells of the extract using the data obtained in a given time interval 
     * (defined by the Thread.sleep of the run method).
     */
    private void writeCellsExtract() {
        ArrayList<ArrayList<String>> aux = (ArrayList<ArrayList<String>>) extracts_set.clone();
        extracts_set = new ArrayList<>();

        for (int i = 0; i < aux.size(); i++) {
            ArrayList<String> data = aux.get(i);

            XSSFRow row = extracts_folder.createRow(extracts_folder.getLastRowNum() + 1);
            int cell_id = 0;
            for (int j = 0; j < data.size(); j++) {
                Cell cell = row.createCell(cell_id++);
                cell.setCellValue(data.get(j));
            }
        }
    }

    /**
     * Part2!
     * Writes the cells of the extract using the data obtained in a given time interval 
     * (defined by the Thread.sleep of the run method).
     */
    private void writeCellsReconciliation() {
        ArrayList<ArrayList<String>> aux = (ArrayList<ArrayList<String>>) reconciliation_set.clone();
        reconciliation_set = new ArrayList<>();

        for (int i = 0; i < aux.size(); i++) {
            ArrayList<String> data = aux.get(i);

            XSSFRow row = reconciliation_folder.createRow(reconciliation_folder.getLastRowNum() + 1);
            int cell_id = 0;
            for (int j = 0; j < data.size(); j++) {
                Cell cell = row.createCell(cell_id++);
                cell.setCellValue(data.get(j));
            }
        }
    }

    /**
     * Part2!
     * Writes the data related to the report in the {@link ExcelExport#data_set} attribute.
     * @param arrayList {@link ArrayList} containing the data to be inserted.
     */
    public void writeReconciliation(double[] times) {
        double aux1[] = times.clone();
        for (int i = 0; i < times.length; i++) {
            aux1[i] = 1000/times[i];
        }

        double[] aux2 = new double[aux1.length-1];
        for (int i = 0; i < aux1.length-1; i++) {
            aux2[i] = aux1[i+1];
        }

        ArrayList<String> data = new ArrayList<>();

        int begin = 15 - aux2.length;
        int index = 0;

        data.add(Double.toString(times[0]));
        
        for (int i = 0; i < 15; i++) {
            if (i >= begin) {
                data.add(Double.toString(aux2[index]));
                index++;
            } else {
                data.add("");
            }
        }

        reconciliation_set.add(data);
    }

    /**
     * Writes the data related to the report in the {@link ExcelExport#data_set} attribute.
     * @param arrayList {@link ArrayList} containing the data to be inserted.
     */
    public void writeReport(ArrayList<Object> arrayList) {
        ArrayList<String> data = new ArrayList<>();

        for (int i = 0; i < arrayList.size(); i++) {
            data.add(arrayList.get(i).toString());
        }

        data_set.add(data);
    }

    /**
     * Writes the data related to the report in the {@link ExcelExport#extracts_set} attribute.
     * @param arrayList {@link ArrayList} containing the data to be inserted.
     */
    public void writeExtract(ArrayList<Object> arrayList) {
        ArrayList<String> data = new ArrayList<>();

        for (int i = 0; i < arrayList.size(); i++) {
            data.add(arrayList.get(i).toString());
        }

        extracts_set.add(data);
    }

    /**
     * Creates the Report header, containing the requested items:
     * TimeStamp // Car ID // Route ID // Speed ​​// Distance // Fuel Consumption //
     * Fuel Type // CO2 Emission // Longitude (Coord X) // Latitude (Coord Y)
     */
    private static void createHeaderReport() {
        String[] header = new String[] {"TimeStamp", "Car ID", "Route ID", "Speed", "Distance", "FuelConsumption", 
                                           "FuelType", "CO2 Emisison", "X Coordinate", "Y Coordinate"};

        XSSFRow row = data_folder.createRow(1);

        int cell_id = 0;
        for (String data : header) {
            Cell cell = row.createCell(cell_id++);
            cell.setCellValue(data);
        }
    }

    /**
     * Creates the Statement header, containing the items:
     * TimeStamp // Account Owner // Target Account // Previous Balance // Current Balance
     */
    private static void criarheaderExtrato() {
        String[] header = new String[] {"TimeStamp", "Account Owner", "Target Account", "Previous Balance", "Current Balance"};

        XSSFRow row = extracts_folder.createRow(1);

        int cell_id = 0;
        for (String data : header) {
            Cell cell = row.createCell(cell_id++);
            cell.setCellValue(data);
        }
    }

    /**
     * Part2!
     * Creates the Date Reconciliation header, containing the items:
     * Time Remaining // Section 02 // Section 03 // Section 04 // Section 05 // Section 06 // Section 07 // Section 08 // (...)
     */
    private static void createHeaderReconciliation() {
        String[] header = new String[] {"Time Remaining", "Section 02", "Section 03", "Section 04", "Section 05", "Section 06", 
                                           "Section 07", "Section 08", "Section 09", "Section 10", "Section 11",
                                           "Section 12", "Section 13", "Section 14", "Section 15", "Section 16 (Fim)"};

        XSSFRow row = reconciliation_folder.createRow(1);

        int cell_id = 0;
        for (String data : header) {
            Cell cell = row.createCell(cell_id++);
            cell.setCellValue(data);
        }
    }

    /**
     *SET method for the {@link ExcelExport#flag} attribute.
     * @param flag_new {@link Boolean} containing the Part2 value to be inserted into the attribute.
     */
    public static void setFlag(boolean flag_new) {
        flag = flag_new;
    }

    public static void main(String[] args) {
        data_set = new ArrayList<>();
        extracts_set = new ArrayList<>();
        reconciliation_set = new ArrayList<>();     //Part2

        workbook = new XSSFWorkbook();

        data_folder = workbook.createSheet("Report");
        extracts_folder = workbook.createSheet("Extracts");
        reconciliation_folder = workbook.createSheet("Reconciliation");        //Part2

        createHeaderReport();
        criarheaderExtrato();
        createHeaderReconciliation();          //Part2

        Thread thread = new Thread(() -> {
            boolean flag1 = true;
            while (flag1) {
                String message = "Do you want to download the report/extract?";
                int result = JOptionPane.showConfirmDialog(null, message, "Confirmation",JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    try {
                        try (FileOutputStream out = new FileOutputStream(new File(Constants.path_file))) {
                            workbook.write(out);
                        }
                        
                        FileInputStream inp = new FileInputStream(new File(Constants.path_file));
                        
                        workbook = new XSSFWorkbook(inp);
                        
                        data_folder = workbook.getSheet("Report");
                        extracts_folder = workbook.getSheet("Extracts");
                        reconciliation_folder = workbook.getSheet("Reconciliation");       //Part2
                        
                    } catch (IOException e) {
                        System.out.println("Error writing file.\nException: " + e);
                    }
                    JOptionPane.showMessageDialog(null, "Downloaded successfully");
                } else {
                    flag1 = false;
                }
            }
        });

        thread.start();
    }

}
