package org.example;

import model.LookupResult;
import model.LookupResultOptions;
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
        int bnsResultsColNumber = 4;
        int bnsProductNameColNumber = 5;
        int sigmaResultsColNumber = 6;
        int sigmaProductNameColNumber = 7;
        int tridentResultsColNumber = 8;
        int tridentProductNameColNumber = 9;
        int aahResultsColNumber = 10;
        int aahProductNameColNumber = 11;


        String originalFileName = "/Users/juppala/Downloads/TestingLookup/src/main/resources/myownspreadsheet.xlsx";
        String copiedFileName = "/Users/juppala/Downloads/TestingLookup/src/main/resources/myownspreadsheetCopy.xlsx";

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
        ExecutorService executor = Executors.newFixedThreadPool(5);
        Callable bnsWorker = new BnS(copiedFileName);
        Callable sigmaWorker = new Sig(copiedFileName);
        Callable tridentWorker = new Trident(copiedFileName);
        Callable aahWorker = new Aah(copiedFileName);
        Future<Map<Integer, LookupResultOptions>> bnsFuture = executor.submit(bnsWorker);
        Future<Map<Integer, LookupResultOptions>> sigmaFuture = executor.submit(sigmaWorker);
        Future<Map<Integer, LookupResultOptions>> tridentFuture = executor.submit(tridentWorker);
        Future<Map<Integer, LookupResultOptions>> aahFuture = executor.submit(aahWorker);



        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        System.out.println("Finished all threads");

        /*System.out.println("BNS Results"+bnsFuture.get());
        System.out.println("Sigma Results"+sigmaFuture.get())*/;

        /*Map<Integer, LookupResult> bnsResults = bns.getConcurrentHashMap();
        Map<Integer, LookupResult> sigmaResults = sigma.getConcurrentHashMap();
        Map<Integer, LookupResult> tridentResults = trident.getConcurrentHashMap();
        Map<Integer, LookupResult> aahResults = aah.getConcurrentHashMap();*/

        Map<Integer, LookupResultOptions> bnsResults = bnsFuture.get();
        Map<Integer, LookupResultOptions> sigmaResults = sigmaFuture.get();
        Map<Integer, LookupResultOptions> tridentResults = tridentFuture.get();
        Map<Integer, LookupResultOptions> aahResults = aahFuture.get();



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
            LookupResultOptions bnsLookupResult = bnsResults.get(rowNumber);
            LookupResultOptions sigmaLookupResult = sigmaResults.get(rowNumber);
            LookupResultOptions tridentLookupResult = tridentResults.get(rowNumber);
            LookupResultOptions aahLookupResult = aahResults.get(rowNumber);

            Row row = sheet.getRow(rowNumber);
            populatePriceAndDesc(bnsLookupResult, bnsResultsColNumber, bnsProductNameColNumber, redFontWithBold, greenFontWithBold, orangeFontWithBold, bnsLookupResult, row);
            populatePriceAndDesc(sigmaLookupResult, sigmaResultsColNumber, sigmaProductNameColNumber, redFontWithBold, greenFontWithBold, orangeFontWithBold, bnsLookupResult, row);
            populatePriceAndDesc(tridentLookupResult, tridentResultsColNumber, tridentProductNameColNumber, redFontWithBold, greenFontWithBold, orangeFontWithBold, bnsLookupResult, row);
            populatePriceAndDesc(aahLookupResult, aahResultsColNumber, aahProductNameColNumber, redFontWithBold, greenFontWithBold, orangeFontWithBold, bnsLookupResult, row);


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

    private static void populatePriceAndDesc(LookupResultOptions lookupResultOptions, int resultsColumnNumber, int productNameColumnNumber, Font redFontWithBold, Font greenFontWithBold, Font orangeFontWithBold, LookupResultOptions bnsLookupResult, Row row) {
        Cell priceCell = row.createCell(resultsColumnNumber);
        Cell productNameCell = row.createCell(productNameColumnNumber);
        LookupResult cheapestAvailableOption = lookupResultOptions.getChepestAvailableOption();
        LookupResult cheapestOption = lookupResultOptions.getChepestOption();
        if(cheapestOption.getPriceString().equals("-1")  ){
            XSSFRichTextString priceString = new XSSFRichTextString("NS");
            priceString.applyFont(0, "NS".length(), orangeFontWithBold);
            priceCell.setCellValue(priceString );
            productNameCell.setCellValue(cheapestOption.getDescription());
        }else if(cheapestAvailableOption.getPriceString().equals(cheapestOption.getPriceString() ) ){
            // Cheapest option is available
            XSSFRichTextString priceString = new XSSFRichTextString(cheapestAvailableOption.getPriceString());
            priceString.applyFont(0, cheapestAvailableOption.getPriceString().length(), greenFontWithBold);
            priceCell.setCellValue(priceString );
            productNameCell.setCellValue(cheapestAvailableOption.getDescription());
        }else if(cheapestAvailableOption.getPriceString().equals("-1")){
            // there is no cheapest available
            XSSFRichTextString priceString = new XSSFRichTextString(cheapestOption.getPriceString());
            priceString.applyFont(0, cheapestOption.getPriceString().length(), redFontWithBold);
            priceCell.setCellValue(priceString );
            productNameCell.setCellValue(cheapestOption.getDescription());
        }else{
            // there is a mixture of available and cheapest options
            XSSFRichTextString priceString  = new XSSFRichTextString(cheapestOption.getPriceString()+""+cheapestAvailableOption.getPriceString() );
            priceString.applyFont(0, cheapestOption.getPriceString().length(), redFontWithBold);
            priceString.applyFont(cheapestOption.getPriceString().length(), (cheapestOption.getPriceString()+""+cheapestAvailableOption.getPriceString()).length(), greenFontWithBold);
            priceCell.setCellValue(priceString);
            productNameCell.setCellValue(cheapestOption.getDescription()+"\r\n"+cheapestAvailableOption.getDescription());
        }
    }
}
