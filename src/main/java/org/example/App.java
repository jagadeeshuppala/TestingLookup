package org.example;

import model.LookupResult;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import service.Aah;
import service.BnS;
import service.Sig;
import service.Trident;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws ExecutionException, InterruptedException, IOException {



        long startTime = System.currentTimeMillis();
        int bnsResultsColNumber = 30;
        int bnsProductNameColNumber = 34;
        int sigmaResultsColNumber = 31;
        int sigmaProductNameColNumber = 35;
        int tridentResultsColNumber = 32;
        int tridentProductNameColNumber = 36;
        int aahResultsColNumber = 33;
        int aahProductNameColNumber = 37;


        //String originalFileName = "/Users/juppala/Downloads/TestingLookup/src/main/resources/myownspreadsheet.xlsx";
        String originalFileName = "C:\\PharmacyProjectWorkspace\\TestingLookup\\src\\main\\resources\\myownspreadsheet.xlsx";
        //String copiedFileName = "/Users/juppala/Downloads/TestingLookup/src/main/resources/myownspreadsheetCopy.xlsx";
        String date = LocalDateTime.now().getDayOfMonth()+"_"+LocalDateTime.now().getMonthValue()+"_"+LocalDateTime.now().getYear();
        String copiedFileName = "C:\\PharmacyProjectWorkspace\\TestingLookup\\src\\main\\resources\\myownspreadsheet_copy_"+ date +".xlsx";

        File original = new File(originalFileName);
        File copied = new File(copiedFileName);
        FileUtils.copyFile(original, copied);

        /*BnS bns = new BnS(copiedFileName);
        Sig sigma = new Sig(copiedFileName);
        Trident trident = new Trident(copiedFileName);
        Aah aah = new Aah(copiedFileName);

        Thread bnsThread = new Thread(bns);
        Thread sigmaThread = new Thread(sigma);
        Thread tridentThread = new Thread(trident);
        Thread aahThread = new Thread(aah);

        tridentThread.start();
        aahThread.start();
        bnsThread.start();
        sigmaThread.start();


        tridentThread.join();
        aahThread.join();
        bnsThread.join();
        sigmaThread.join();
*/
        ExecutorService executor = Executors.newFixedThreadPool(4);
        Future<Map<Integer, LookupResult>> bnsFuture = executor.submit(new BnS(copiedFileName));
        Future<Map<Integer, LookupResult>> sigmaFuture = executor.submit(new Sig(copiedFileName));
        Future<Map<Integer, LookupResult>> tridentFuture = executor.submit(new Trident(copiedFileName));
        Future<Map<Integer, LookupResult>> aahFuture = executor.submit(new Aah(copiedFileName));



        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        System.out.println("Finished all threads");


        Map<Integer, LookupResult> bnsResults = bnsFuture.get();
        Map<Integer, LookupResult> sigmaResults = sigmaFuture.get();
        Map<Integer, LookupResult> tridentResults = tridentFuture.get();
        Map<Integer, LookupResult> aahResults = aahFuture.get();



        FileInputStream file = new FileInputStream(copiedFileName);
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);
        CellStyle redCellStyle = workbook.createCellStyle();
        CellStyle greenCellStyle = workbook.createCellStyle();
        CellStyle orangeCellStyle = workbook.createCellStyle();

        Font redFontWithBold = workbook.createFont();
        redFontWithBold.setColor(HSSFColor.HSSFColorPredefined.RED.getIndex());
        redFontWithBold.setBold(true);
        redCellStyle.setFont(redFontWithBold);

        Font greenFontWithBold = workbook.createFont();
        greenFontWithBold.setColor(HSSFColor.HSSFColorPredefined.GREEN.getIndex());
        greenFontWithBold.setBold(true);
        greenCellStyle.setFont(greenFontWithBold);

        Font orangeFontWithBold = workbook.createFont();
        orangeFontWithBold.setColor(HSSFColor.HSSFColorPredefined.ORANGE.getIndex());
        orangeFontWithBold.setBold(true);
        orangeCellStyle.setFont(orangeFontWithBold);

        for (Integer rowNumber : aahResults.keySet()) {
            LookupResult bnsLookupResult = bnsResults.get(rowNumber);
            LookupResult sigmaLookupResult = sigmaResults.get(rowNumber);
            LookupResult tridentLookupResult = tridentResults.get(rowNumber);
            LookupResult aahLookupResult = aahResults.get(rowNumber);

            Row row = sheet.getRow(rowNumber);
            populatePriceAndDesc(bnsLookupResult, bnsResultsColNumber, bnsProductNameColNumber, redFontWithBold, greenFontWithBold, orangeFontWithBold, row);
            populatePriceAndDesc(sigmaLookupResult, sigmaResultsColNumber, sigmaProductNameColNumber, redFontWithBold, greenFontWithBold, orangeFontWithBold, row);
            populatePriceAndDesc(tridentLookupResult, tridentResultsColNumber, tridentProductNameColNumber, redFontWithBold, greenFontWithBold, orangeFontWithBold, row);
            populatePriceAndDesc(aahLookupResult, aahResultsColNumber, aahProductNameColNumber, redFontWithBold, greenFontWithBold, orangeFontWithBold, row);


            /*Row row = sheet.getRow(rowNumber);
            Cell bnsPriceCell = row.createCell(bnsResultsColNumber);
            Cell bnsProductNameCell = row.createCell(bnsProductNameColNumber);
            if(bnsLookupResult.getAvailable().equals("NA") ){
                bnsPriceCell.setCellStyle(orangeCellStyle);
                bnsPriceCell.setCellValue("NS");
            }else if(bnsLookupResult.getAvailable().equals("available") ){
                bnsPriceCell.setCellStyle(greenCellStyle);
                bnsPriceCell.setCellValue(String.valueOf(bnsPrice));
                bnsProductNameCell.setCellValue(bnsLookupResult.getDescription());
            }else{
                bnsPriceCell.setCellStyle(redCellStyle);
                bnsPriceCell.setCellValue(String.valueOf(bnsPrice));
                bnsProductNameCell.setCellValue(bnsLookupResult.getDescription());
            }


            Cell sigmaPriceCell = row.createCell(sigmaResultsColNumber);
            Cell sigmaProductNameCell = row.createCell(sigmaProductNameColNumber);
            if(sigmaLookupResult.getAvailable().equals("NA")){
                sigmaPriceCell.setCellStyle(orangeCellStyle);
                sigmaPriceCell.setCellValue("NS");
            } else if(sigmaLookupResult.getAvailable().equals("available") ){
                sigmaPriceCell.setCellStyle(greenCellStyle);
                sigmaPriceCell.setCellValue(String.valueOf(sigmaPrice));
                sigmaProductNameCell.setCellValue(sigmaLookupResult.getDescription());
            }else{
                sigmaPriceCell.setCellStyle(redCellStyle);
                sigmaPriceCell.setCellValue(String.valueOf(sigmaPrice));
                sigmaProductNameCell.setCellValue(sigmaLookupResult.getDescription());
            }


            Cell tridentPriceCell = row.createCell(tridentResultsColNumber);
            Cell tridentProductNameCell = row.createCell(tridentProductNameColNumber);
            if(tridentLookupResult.getAvailable().equals("NA")){
                tridentPriceCell.setCellStyle(orangeCellStyle);
                tridentPriceCell.setCellValue("NS");
            } else if(tridentLookupResult.getAvailable().equals("In stock") ){
                tridentPriceCell.setCellStyle(greenCellStyle);
                tridentPriceCell.setCellValue(String.valueOf(tridentPrice));
                tridentProductNameCell.setCellValue(tridentLookupResult.getDescription());
            }else{
                tridentPriceCell.setCellStyle(redCellStyle);
                tridentPriceCell.setCellValue(String.valueOf(tridentPrice));
                tridentProductNameCell.setCellValue(tridentLookupResult.getDescription());
            }


            Cell aahPriceCell = row.createCell(aahResultsColNumber);
            Cell aahProductNameCell = row.createCell(aahProductNameColNumber);
            if(aahLookupResult.getAvailable().equals("NA")){
                aahPriceCell.setCellStyle(orangeCellStyle);
                aahPriceCell.setCellValue("NS");
            } else if(aahLookupResult.getAvailable().equals("In stock") ){
                aahPriceCell.setCellStyle(greenCellStyle);
                aahPriceCell.setCellValue(String.valueOf(aahPrice));
                aahProductNameCell.setCellValue(aahLookupResult.getDescription());
            }else{
                aahPriceCell.setCellStyle(redCellStyle);
                aahPriceCell.setCellValue(String.valueOf(aahPrice));
                aahProductNameCell.setCellValue(aahLookupResult.getDescription());
            }*/



        }

        FileOutputStream outputStream = new FileOutputStream(copiedFileName);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();

        long endTime = System.currentTimeMillis();
        System.out.println("Total time taken "+ (endTime-startTime)/1000 + " seconds");






    }



    private static void populatePriceAndDesc(LookupResult lookupResult, int resultsColumnNumber, int productNameColumnNumber, Font redFontWithBold, Font greenFontWithBold, Font orangeFontWithBold, Row row) {
        Cell priceCell = row.createCell(resultsColumnNumber);
        Cell productNameCell = row.createCell(productNameColumnNumber);
        if(lookupResult!=null){
            if(lookupResult.getPriceString().equals("-1")){
                XSSFRichTextString priceString = new XSSFRichTextString("NS");
                priceString.applyFont(0, "NS".length(), orangeFontWithBold);
                priceCell.setCellValue(priceString );
                productNameCell.setCellValue(lookupResult.getDescription());
            }else if(lookupResult.getAvailable().equals("available") || lookupResult.getAvailable().equals("low stock")  || lookupResult.getAvailable().equals("In stock")){
                XSSFRichTextString priceString = new XSSFRichTextString(lookupResult.getPriceString());
                priceString.applyFont(0, lookupResult.getPriceString().length(), greenFontWithBold);
                priceCell.setCellValue(priceString );
                productNameCell.setCellValue(lookupResult.getDescription());
            }else{
                // its not available
                XSSFRichTextString priceString = new XSSFRichTextString(lookupResult.getPriceString());
                priceString.applyFont(0, lookupResult.getPriceString().length(), redFontWithBold);
                priceCell.setCellValue(priceString );
                productNameCell.setCellValue(lookupResult.getDescription());
            }
        }

    }
}
