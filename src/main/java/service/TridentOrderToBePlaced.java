package service;

import io.github.bonigarcia.wdm.WebDriverManager;
import model.LookupResult;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.FileInputStream;
import java.util.List;

public class TridentOrderToBePlaced {

    String fileName;
    int tridentResultsColNumber;
    int quantityColNumber;


    public TridentOrderToBePlaced(String fileName, int tridentResultsColNumber, int quantityColNumber){
        this.fileName = fileName;
        this.tridentResultsColNumber =tridentResultsColNumber;
        this.quantityColNumber = quantityColNumber;
    }





    public void placeOrder() throws Exception {


        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.get("https://www.aah.co.uk/s/signin?startURL=https%3A%2F%2Fwww.tridentonline.co.uk%2Ftrident%2Fsearchresults%3Foperation%3DquickSearch");
        //driver.get("https://www.tridentonline.co.uk/trident/searchresults?operation=quickSearch");

        Thread.sleep(5000);
        driver.findElement(By.id("onetrust-reject-all-handler")).click();


        driver.findElement(By.xpath("/html[1]/body[1]/div[3]/div[1]/div[1]/div[1]/div[2]/div[1]/div[1]/div[1]/article[1]/div[2]/div[2]/div[1]/lightning-input[1]/lightning-primitive-input-simple[1]/div[1]/div[1]/input[1]")).sendKeys("bridgwaterpharmacy");
        driver.findElement(By.xpath("/html[1]/body[1]/div[3]/div[1]/div[1]/div[1]/div[2]/div[1]/div[1]/div[1]/article[1]/div[2]/div[2]/div[1]/div[1]/lightning-input[1]/lightning-primitive-input-simple[1]/div[1]/div[1]/input[1]")).sendKeys("Brid@8486");
        Thread.sleep(5000);


        driver.findElement(By.xpath("/html[1]/body[1]/div[3]/div[1]/div[1]/div[1]/div[2]/div[1]/div[1]/div[1]/article[1]/div[2]/div[2]/div[2]/button[1]"))
                .sendKeys(Keys.RETURN);
        Thread.sleep(20000);




        FileInputStream file = new FileInputStream(fileName);
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);

        for(int i=0;i<=sheet.getLastRowNum() ;i++){
            Cell tridentCell = sheet.getRow(i).getCell(tridentResultsColNumber);
            Cell quantityCell = sheet.getRow(i).getCell(quantityColNumber);
            if(tridentCell != null && tridentCell.getCellType() != CellType.BLANK && quantityCell!=null && quantityCell.getCellType() != CellType.BLANK){


              /*  CellStyle lightYellowCellStyle = workbook.createCellStyle();
                lightYellowCellStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
                lightYellowCellStyle.setFillPattern(FillPatternType.ALT_BARS);*/

                CellStyle tridentStyle = tridentCell.getCellStyle();

                if(tridentStyle.getFillForegroundColor() == IndexedColors.LIGHT_YELLOW.getIndex()){
                    try{
                        String s = tridentCell.getCellComment().getString().getString();
                        String productToBeOrdered = s.split("\r\n")[0];
                        String quantityToBeOrdered = new DataFormatter().formatCellValue(quantityCell);
                        boolean addedToBasket = addToBasket(driver, productToBeOrdered, quantityToBeOrdered);
                        if(addedToBasket){
                            System.out.println(productToBeOrdered + " was added to basket");
                        }else{
                            System.out.println(productToBeOrdered + " was not added to basket");
                        }
                    }catch (Exception e){
                        System.out.println("row number "+i);
                        e.printStackTrace();
                    }

                }


                /*XSSFCellStyle tridentStyle = (XSSFCellStyle) tridentCell.getCellStyle();
                XSSFFont font = tridentStyle.getFont();
                XSSFColor foregroundColorColor = tridentStyle.getFillForegroundColorColor();
                if(foregroundColorColor.equals(XSSFColor.toXSSFColor(C))){
                    String s = tridentCell.getCellComment().getString().getString();
                    String productToBeOrdered = s.split("\n")[0];
                    String quantityToBeOrdered = new DataFormatter().formatCellValue(quantityCell);
                    boolean addedToBasket = addToBasket(driver, productToBeOrdered, quantityToBeOrdered);
                    if(addedToBasket){
                        System.out.println(productToBeOrdered + " was added to basket");
                    }else{
                        System.out.println(productToBeOrdered + " was not added to basket");
                    }

                }*/
            }

        }
        placedOrdersList(driver);

        driver.close();
        driver.quit();


    }

    private boolean addToBasket(WebDriver driver, String productDesc, String quantity) throws InterruptedException {
        try{
            driver.findElement(By.xpath("/html[1]/body[1]/div[1]/header[1]/div[1]/div[1]/div[1]/div[4]/div[1]/div[1]/div[1]/div[1]/span[1]/lightning-input[1]/lightning-primitive-input-simple[1]/div[1]/div[1]/input[1]")).clear();
            driver.findElement(By.xpath("/html[1]/body[1]/div[1]/header[1]/div[1]/div[1]/div[1]/div[4]/div[1]/div[1]/div[1]/div[1]/span[1]/lightning-input[1]/lightning-primitive-input-simple[1]/div[1]/div[1]/input[1]")).sendKeys(productDesc );


            driver.findElement(By.xpath("/html[1]/body[1]/div[1]/header[1]/div[1]/div[1]/div[1]/div[4]/div[1]/div[1]/div[1]/div[1]/span[1]/lightning-input[1]/lightning-primitive-input-simple[1]/div[1]/div[1]/input[1]"))
                    .sendKeys( Keys.RETURN);
            Thread.sleep(3000);


            driver.findElement(By.xpath("/html[1]/body[1]/div[1]/div[2]/span[1]/div[1]/div[1]/div[2]/div[4]/div[1]/div[2]/div[2]/span[1]/div[1]/div[1]/div[1]/div[2]/div[1]/div[4]/span[1]/div[1]/div[1]/div[1]/div[1]/div[1]/input[1]")).sendKeys(quantity );


            driver.findElement(By.xpath("/html[1]/body[1]/div[1]/div[2]/span[1]/div[1]/div[1]/div[2]/div[4]/div[1]/div[2]/div[2]/span[1]/div[1]/div[1]/div[1]/div[2]/div[1]/div[4]/span[1]/div[1]/div[1]/div[2]/div[1]/button[1]"))
                    .sendKeys(Keys.RETURN);

            return true;
        }catch (Exception e){
            System.out.println("Trident ordering exception during the search field ::::::"+ productDesc+ ":" +e.getMessage());
            e.printStackTrace();
        }

        return false;

    }

    public void placedOrdersList(WebDriver driver) throws InterruptedException {

        driver.get("https://www.tridentonline.co.uk/trident/basket");
        Thread.sleep(10000);

        List<WebElement> numberOfLis = driver.findElements(By.xpath("/html[1]/body[1]/div[1]/div[2]/div[1]/div[4]/div[1]/div[1]/div[2]/div[1]/div[1]/div[2]/div[1]/div"));

        for(int i=1; i<=numberOfLis.size();i++){
            String description = driver.findElement(By.xpath("/html[1]/body[1]/div[1]/div[2]/div[1]/div[4]/div[1]/div[1]/div[2]/div[1]/div[1]/div[2]/div[1]/div["+i+"]/div[1]/div[1]/p[1]/a[1]")).getText();
            //String quantity = driver.findElement(By.xpath("/html[1]/body[1]/div[1]/div[2]/div[1]/div[4]/div[1]/div[1]/div[2]/div[1]/div[1]/div[2]/div[1]/div[\"+i+\"]/div[1]/div[2]/div[1]/div[1]/div[1]/div[1]/div[1]/input[1]/@value")).g();
            System.out.println("ordered "+ description );
        }



    }



}