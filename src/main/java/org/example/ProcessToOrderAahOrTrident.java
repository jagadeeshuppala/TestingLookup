package org.example;

import model.LookupResult;
import model.LookupResultOptions;
import model.Product;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import service.Aah;
import service.BnS;
import service.Sig;
import service.Trident;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Hello world!
 *
 */
public class ProcessToOrderAahOrTrident {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {


        long startTime = System.currentTimeMillis();


        String date = LocalDateTime.now().getDayOfMonth() + "_" + LocalDateTime.now().getMonthValue() + "_" + LocalDateTime.now().getYear();
        String copiedFileName = "C:\\PharmacyProjectWorkspace\\TestingLookup\\src\\main\\resources\\newSpreadSheet_copy_" + date + ".xlsx";



        FileInputStream file = new FileInputStream(copiedFileName);
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);
        int aahResultsColNumber = 14;
        int tridentResultsColNumber = 17;


        for (int i = 1; i <= sheet.getLastRowNum() && sheet.getRow(i) != null && sheet.getRow(i).getCell(tridentResultsColNumber) != null; i++) {
            if (sheet.getRow(i).getCell(tridentResultsColNumber).getCellType() != CellType.BLANK
                    && !sheet.getRow(i).getCell(tridentResultsColNumber).toString().trim().equals("")) {

                HSSFCellStyle style = (HSSFCellStyle) sheet.getRow(i).getCell(tridentResultsColNumber).getCellStyle();
                HSSFFont font = style.getFont(workbook);
                if(font.getItalic()){
                    System.out.println(sheet.getRow(i).getCell(tridentResultsColNumber).getCellComment() + "is italic");
                }


            }
        }


        FileOutputStream outputStream = new FileOutputStream(copiedFileName);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();

        long endTime = System.currentTimeMillis();
        System.out.println("Total time taken " + ((endTime - startTime) / 1000)/60 + " minutes");


    }





}
