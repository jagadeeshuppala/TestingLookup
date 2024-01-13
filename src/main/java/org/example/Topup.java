package org.example;

import model.LookupResult;
import model.LookupResultOptions;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
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
public class Topup {
    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {


        long startTime = System.currentTimeMillis();
        int bnsResultsColNumber = 15;
        int sigmaResultsColNumber = 16;
        int tridentResultsColNumber = 17;
        int aahResultsColNumber = 14;


        String originalFileName = "\\\\11701279QSVR\\PSSharedarea\\Bridgwater\\Miscellaneous\\OrderList.xlsx";
        String date = LocalDateTime.now().getDayOfMonth() + "_" + LocalDateTime.now().getMonthValue() + "_" + LocalDateTime.now().getYear();
        String copiedFileName = "C:\\Users\\msola\\OneDrive\\Desktop\\OrderListCopy-DONT DELETE\\OrderList-Copy_"+ date +".xlsx";
        //String copiedFileName = "\\\\11701279QSVR\\PSSharedarea\\Bridgwater\\Miscellaneous\\OrderList - Copy.xlsx";

        File original = new File(originalFileName);
        File copied = new File(copiedFileName);
        FileUtils.copyFile(original, copied);


        ExecutorService executor = Executors.newFixedThreadPool(4);
        Future<Map<Integer, LookupResultOptions>> bnsFuture = executor.submit(new BnS(originalFileName, false));
        Future<Map<Integer, LookupResultOptions>> sigmaFuture = executor.submit(new Sig(originalFileName, false));
        Future<Map<Integer, LookupResultOptions>> tridentFuture = executor.submit(new Trident(originalFileName, false));
        Future<Map<Integer, LookupResultOptions>> aahFuture = executor.submit(new Aah(originalFileName, false));


        executor.shutdown();
        while (!executor.isTerminated()) {
        }
        System.out.println("Finished fetching rates from all the websites");


        Map<Integer, LookupResultOptions> bnsResults = bnsFuture.get();
        Map<Integer, LookupResultOptions> sigmaResults = sigmaFuture.get();
        Map<Integer, LookupResultOptions> tridentResults = tridentFuture.get();
        Map<Integer, LookupResultOptions> aahResults = aahFuture.get();


        File processCheck = new File( originalFileName );
        Boolean canWrite = processCheck.canWrite();
        while (!canWrite){
            Scanner input = new Scanner(System.in);
            System.out.print("Please close the OrderList file and press enter to continue");
            String nextLine = input.nextLine();
            System.out.println("You entered: " + nextLine + " Now it will ");
        }

        FileInputStream file = new FileInputStream(originalFileName);
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


            BigDecimal bnsPrice = new BigDecimal(bnsLookupResult.getCheapestAvailableOption()!=null ? bnsLookupResult.getCheapestAvailableOption().getPriceString() : "-1");
            BigDecimal sigmaPrice = new BigDecimal(sigmaLookupResult.getCheapestAvailableOption()!=null ? sigmaLookupResult.getCheapestAvailableOption().getPriceString() : "-1");
            BigDecimal tridentPrice = new BigDecimal(tridentLookupResult.getCheapestAvailableOption()!=null? tridentLookupResult.getCheapestAvailableOption().getPriceString() : "-1");
            BigDecimal aahPrice = new BigDecimal(aahLookupResult.getCheapestAvailableOption()!=null ? aahLookupResult.getCheapestAvailableOption().getPriceString(): "-1");



            List<BigDecimal> pricesList = Arrays.asList(bnsPrice, sigmaPrice, tridentPrice, aahPrice)
                    .stream()
                    .filter(v -> !v.equals(new BigDecimal("-1")))
                    .collect(Collectors.toList());
            BigDecimal cheapestOfAll = new BigDecimal("-1");
            if(!pricesList.isEmpty()){
                cheapestOfAll = Collections.min(pricesList, Comparator.comparing(v -> v));
            }




            Row row = sheet.getRow(rowNumber);
            populatePriceAndDesc(bnsLookupResult, bnsResultsColNumber,  redFontWithBold, greenFontWithBold, orangeFontWithBold, row, workbook, sheet, cheapestOfAll.toPlainString());
            populatePriceAndDesc(sigmaLookupResult, sigmaResultsColNumber,  redFontWithBold, greenFontWithBold, orangeFontWithBold, row, workbook, sheet, cheapestOfAll.toPlainString());
            populatePriceAndDesc(tridentLookupResult, tridentResultsColNumber,  redFontWithBold, greenFontWithBold, orangeFontWithBold, row, workbook, sheet, cheapestOfAll.toPlainString());
            populatePriceAndDesc(aahLookupResult, aahResultsColNumber,  redFontWithBold, greenFontWithBold, orangeFontWithBold, row, workbook, sheet, cheapestOfAll.toPlainString());

        }

        FileOutputStream outputStream = new FileOutputStream(originalFileName);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();

        long endTime = System.currentTimeMillis();
        System.out.println("Total time taken " + ((endTime - startTime) / 1000)/60 + " minutes");


    }


    private static void populatePriceAndDesc(LookupResultOptions lookupResultOptions, int resultsColumnNumber, Font redFontWithBold, Font greenFontWithBold, Font orangeFontWithBold,
                                             Row row, Workbook workbook, Sheet sheet, String cheapestPrice) {

        Cell priceCell = row.createCell(resultsColumnNumber);
        if (lookupResultOptions != null) {
            LookupResult cheapestAvailableOption = lookupResultOptions.getCheapestAvailableOption();
            LookupResult cheapestOption = lookupResultOptions.getCheapestOption();

            String priceString  = null;
            String priceDescription = null;
            String comparingPriceString = null;

            // set up background color
            CellStyle lightYellowCellStyle = workbook.createCellStyle();
            lightYellowCellStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            lightYellowCellStyle.setFillPattern(FillPatternType.ALT_BARS);




            if (cheapestOption.getPriceString().equals("-1")) {
                priceString = "NS";
                priceDescription = "NA";
                comparingPriceString = "-1";
                XSSFRichTextString priceStringRichText = new XSSFRichTextString(priceString);
                priceStringRichText.applyFont(0, priceString.length(), orangeFontWithBold);
                priceCell.setCellValue(priceStringRichText);
                addComment(workbook, sheet, row.getRowNum(), priceDescription, priceCell);
            } else if (cheapestAvailableOption.getPriceString().equals(cheapestOption.getPriceString())) {
                // Cheapest option is available
                priceString = cheapestAvailableOption.getPriceString();
                priceDescription = cheapestAvailableOption.getDescription();
                comparingPriceString = cheapestAvailableOption.getPriceString();



                XSSFRichTextString priceStringRichText = new XSSFRichTextString(priceString);
                priceStringRichText.applyFont(0, cheapestAvailableOption.getPriceString().length(), greenFontWithBold);
                priceCell.setCellValue(priceStringRichText);
                if(comparingPriceString.equals(cheapestPrice)){
                    priceCell.setCellStyle(lightYellowCellStyle);
                }
                addComment(workbook, sheet, row.getRowNum(), priceDescription, priceCell);
            } else if (cheapestAvailableOption.getPriceString().equals("-1")) {
                // there is no cheapest available
                priceString = cheapestOption.getPriceString();
                priceDescription = cheapestOption.getDescription();
                comparingPriceString = cheapestOption.getPriceString();

                XSSFRichTextString priceStringRichText = new XSSFRichTextString(priceString);
                priceStringRichText.applyFont(0, cheapestOption.getPriceString().length(), redFontWithBold);
                priceCell.setCellValue(priceStringRichText);
                if(comparingPriceString.equals(cheapestPrice)){
                    priceCell.setCellStyle(lightYellowCellStyle);
                }
                addComment(workbook, sheet, row.getRowNum(), priceDescription, priceCell);
            } else {
                // there is a mixture of available and cheapest options
                priceString = cheapestAvailableOption.getPriceString() + " " + cheapestOption.getPriceString();
                priceDescription = cheapestAvailableOption.getDescription() + "\r\n" + cheapestOption.getDescription();
                comparingPriceString = cheapestAvailableOption.getPriceString();


                XSSFRichTextString priceStringRichText = new XSSFRichTextString(priceString);
                priceStringRichText.applyFont(0,cheapestAvailableOption.getPriceString().length(), greenFontWithBold);
                priceStringRichText.applyFont(cheapestAvailableOption.getPriceString().length(), (cheapestAvailableOption.getPriceString() + " " + cheapestOption.getPriceString()).length(), redFontWithBold);

                /*priceStringRichText.applyFont(0, cheapestOption.getPriceString().length(), redFontWithBold);
                priceStringRichText.applyFont(cheapestOption.getPriceString().length(), (cheapestOption.getPriceString() + " " + cheapestAvailableOption.getPriceString()).length(), greenFontWithBold);*/
                priceCell.setCellValue(priceStringRichText);
                if(comparingPriceString.equals(cheapestPrice)){
                    priceCell.setCellStyle(lightYellowCellStyle);
                }
                addComment(workbook, sheet, row.getRowNum(), priceDescription, priceCell);
            }
        }

    }

    public static void addComment(Workbook workbook, Sheet sheet, int rowIdx, String commentText, Cell cell) {
        cell.setCellComment(null);
        cell.removeCellComment();

        CreationHelper factory = workbook.getCreationHelper();

        ClientAnchor anchor = factory.createClientAnchor();
        //i found it useful to show the comment box at the bottom right corner
        anchor.setCol1(cell.getColumnIndex() + 1); //the box of the comment starts at this given column...
        anchor.setCol2(cell.getColumnIndex() + 3); //...and ends at that given column
        anchor.setRow1(rowIdx + 1); //one row below the cell...
        anchor.setRow2(rowIdx + 5); //...and 4 rows high


        try{
            /*Drawing drawing = sheet.createDrawingPatriarch();
            Comment comment = drawing.createCellComment(anchor);*/

            Comment comment =  sheet.createDrawingPatriarch().createCellComment(
                    //new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 5, 6));
                    new XSSFClientAnchor(0, 0, 0, 0, cell.getColumnIndex(), cell.getRowIndex(), cell.getColumnIndex()+5, cell.getRowIndex()+6));


            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime dateTime = LocalDateTime.now();
            String formattedDateTime = dateTime.format(formatter);
            comment.setString(factory.createRichTextString(commentText+"\r\n"+formattedDateTime));

            cell.setCellComment(comment);
        }catch (Exception e){
            e.printStackTrace();

        }

    }
}
