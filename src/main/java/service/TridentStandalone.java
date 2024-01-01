package service;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.SneakyThrows;
import model.LookupResult;
import model.Product;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TridentStandalone {




    public static void main(String[] args) throws IOException, InterruptedException {

        TridentStandalone newLogic = new TridentStandalone();

        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.get("https://www.aah.co.uk/s/signin?startURL=https%3A%2F%2Fwww.tridentonline.co.uk%2Ftrident%2Fsearchresults%3Foperation%3DquickSearch");

        Thread.sleep(5000);

        driver.findElement(By.id("onetrust-reject-all-handler")).click();


        driver.findElement(By.xpath("/html[1]/body[1]/div[3]/div[1]/div[1]/div[1]/div[2]/div[1]/div[1]/div[1]/article[1]/div[2]/div[2]/div[1]/lightning-input[1]/lightning-primitive-input-simple[1]/div[1]/div[1]/input[1]")).sendKeys("bridgwaterpharmacy");
        driver.findElement(By.xpath("/html[1]/body[1]/div[3]/div[1]/div[1]/div[1]/div[2]/div[1]/div[1]/div[1]/article[1]/div[2]/div[2]/div[1]/div[1]/lightning-input[1]/lightning-primitive-input-simple[1]/div[1]/div[1]/input[1]")).sendKeys("Brid@8486");
        Thread.sleep(5000);


        driver.findElement(By.xpath("/html[1]/body[1]/div[3]/div[1]/div[1]/div[1]/div[2]/div[1]/div[1]/div[1]/article[1]/div[2]/div[2]/div[2]/button[1]"))
                .sendKeys(Keys.RETURN);
        Thread.sleep(20000);



        //String fileName = "C:\\JavaWorkSpace\\ProductLookup\\ProductLookup\\src\\main\\resources\\JagOrderList.xlsx";
        //String fileName = "/Users/juppala/MyNewWorkspace/prodfinder/src/main/resources/JagOrderList.xlsx";
       // FileInputStream file = new FileInputStream(fileName);
        //Workbook workbook = new XSSFWorkbook(file);
       // Sheet sheet = workbook.getSheetAt(0);
        int productNameColumnNumber = 0;
        int strengthColumnNumber = 1;
        int packSizeColumnNumber = 2;
        int quantityColumnNumber = 3;
        int notesColumnNumber = 5;

        /*List<Product> productNames = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum() && sheet.getRow(i) != null && sheet.getRow(i).getCell(productNameColumnNumber) != null; i++) {
            if (sheet.getRow(i).getCell(quantityColumnNumber).getCellType() != CellType.BLANK
                    && !sheet.getRow(i).getCell(quantityColumnNumber).toString().trim().equals("")
                    &&
                    sheet.getRow(i).getCell(notesColumnNumber).toString().equals("FM")
                    //true
                ) {


                String productName = sheet.getRow(i).getCell(productNameColumnNumber) != null ? new DataFormatter().formatCellValue(sheet.getRow(i).getCell(productNameColumnNumber)).toLowerCase() : null;
                String strenth = sheet.getRow(i).getCell(productNameColumnNumber) != null ? new DataFormatter().formatCellValue(sheet.getRow(i).getCell(strengthColumnNumber)).toLowerCase() : null;
                String packsize = sheet.getRow(i).getCell(productNameColumnNumber) != null ? new DataFormatter().formatCellValue(sheet.getRow(i).getCell(packSizeColumnNumber)).toLowerCase() : null;
                productNames.add(Product.builder().productName(productName).strength(strenth).packsize(packsize).productNameUnmodified(productName).build());
                if(  productName.startsWith("o") || productName.startsWith("p") || productName.startsWith("q") || productName.startsWith("r")
                        || productName.startsWith("s") || productName.startsWith("t") || productName.startsWith("u") || productName.startsWith("v")
                        || productName.startsWith("w") || productName.startsWith("x") || productName.startsWith("y") || productName.startsWith("z")){
                    productNames.add(Product.builder().productName(productName).strength(strenth).packsize(packsize).productNameUnmodified(productName).build());
                }

            }
        }*/
        List<Product> productNames = Arrays.asList(
                Product.builder().productName("memantine soln sf").strength("10mg/ml").packsize("50").productNameUnmodified("memantine soln sf").build()
        );





        for(Product product : productNames){
            System.out.println("Product:"+product.getProductName()+" Strength:"+product.getStrength() + " PackSize:"+ product.getPacksize());
            overrideProductBeforeEvenSearch(product);

            try{

                List<LookupResult> lookupResultList = newLogic.lookupResults(driver, product.getProductName(), product.getStrength());
                System.out.println("Result list from website");
                newLogic.print(lookupResultList);

                List<LookupResult> matchedWithProdNameAndStrengthAndPackSize =  lookupResultList.stream()
                        .filter(websiteDesc -> Util.websiteDescContainsProductName(websiteDesc.getDescription().toLowerCase(),
                                product.getProductName().toLowerCase().replaceAll("\\+","+ ") ))
                        .filter(websiteDesc -> Util.websiteDescContainsStrength(websiteDesc.getDescription().toLowerCase(), product.getStrength().toLowerCase()))
                        .filter(websiteDesc -> Util.websiteDescContainsPacksize(websiteDesc.getDescription().toLowerCase(), product.getPacksize().toLowerCase()))
                        .filter(websiteDesc -> specialConsiderationOfProductResultsFromWebsite(websiteDesc.getDescription().toLowerCase(),
                                product))
                                //product.getProductName().toLowerCase(), product.getStrength().toLowerCase(), product.getProductNameUnmodified()))
                        .collect(Collectors.toList());

                List<LookupResult> matchedWithProdNameAndStrength = new ArrayList<>();
                if(matchedWithProdNameAndStrengthAndPackSize.isEmpty()){
                    matchedWithProdNameAndStrength =  lookupResultList.stream()
                            .filter(websiteDesc -> Util.websiteDescContainsProductName(websiteDesc.getDescription().toLowerCase(),
                                    product.getProductName().toLowerCase().replaceAll("\\+"," ") ))
                            .filter(websiteDesc -> Util.websiteDescContainsStrength(websiteDesc.getDescription().toLowerCase(), product.getStrength().toLowerCase()))
                            .filter(websiteDesc -> specialConsiderationOfProductResultsFromWebsite(websiteDesc.getDescription().toLowerCase(),
                                    product))
                                    //product.getProductName().toLowerCase(), product.getStrength().toLowerCase(), product.getProductNameUnmodified()))
                            .collect(Collectors.toList());
                }

               /* List<LookupResult> matchedWithAllWords = lookupResultList.stream()
                        .filter( websiteDesc -> websiteDescContainsProductNameStrengthAndPackSize(websiteDesc.getDescription(),
                                (productName.getProductName().toLowerCase().replaceAll("\\+"," ") + " "+ productName.getStrength().toLowerCase() + " " + productName.getPacksize().toLowerCase()).split(" ")))
                        .collect(Collectors.toList());*/
                System.out.println("matched result with desc, strength, packsize");
                newLogic.print(matchedWithProdNameAndStrengthAndPackSize);
                if(!matchedWithProdNameAndStrength.isEmpty()){
                    System.out.println("tried matched result with desc, strength and without packsize");
                    newLogic.print(matchedWithProdNameAndStrength);
                }


                System.out.println("--------------------------------------------------------");
            }catch (Exception e){
                System.out.println("exception:::"+e.getMessage());
               e.printStackTrace();
               Thread.sleep(1000);
            }

        }

        driver.close();
        driver.quit();



    }



    private static void overrideProductBeforeEvenSearch(Product product){

        // remove p and pom from product
        List<String> listWithPorPom  = Arrays.stream(product.getProductName().toLowerCase().split("\\(|\\)|\\s|\\[|]|'"))
                .filter(v -> !v.isEmpty())
                //.collect(Collectors.toList());
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));


        if(listWithPorPom.contains("p") || listWithPorPom.contains("pom")){
            List<String> listWithOutPorPom = listWithPorPom.stream()
                    .filter(v -> !v.equals("p"))
                    .filter(v -> !v.equals("pom"))
                            //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
            product.setProductName(String.join(" ", listWithOutPorPom));
        }



        if(product.getStrength().contains(".0%")){
            product.setStrength(product.getStrength().replaceAll("\\.0%","%"));
        }

        if(product.getProductName().contains("levothyroxine tabs") && product.getStrength().equals("12.5mg")){
            product.setProductName("levothyroxine tabs");
            product.setStrength("12.5mcg");
        }else if(product.getProductName().contains("metformin sr generic")){
            product.setProductName("metformin sr tab");
        }else if(product.getProductName().contains("perindopril")){
            product.setProductName("perindopril");
        }else if(product.getProductName().contains("salbutamol inhaler")){
            product.setProductName("salbutamol inhaler");
        }else if(product.getProductName().contains("vipidia tabs alogliptin")){
            product.setProductName("vipidia tabs");
        }else if(product.getProductName().contains("aciclovir cream (gsl)")){
            product.setProductName("aciclovir cream");
        }else if(product.getProductName().contains("aciclovir cream")){
            product.setProductName("aciclovir cream");
        }else if(product.getProductName().contains("aciclovir tabs disp act,acc only")){
            product.setProductName("aciclovir tabs disp");
        }else if(product.getProductName().contains("alendronic")){
            product.setProductName("alendronic tab");
        }else if(product.getProductName().contains("allevyn adhesive") && product.getStrength().equals("7.5x7.5")){
            product.setProductName("allevyn adhesive");
            product.setStrength("7.5x7.5cm");
        }else if(product.getProductName().contains("allevyn adhesive")){
            product.setProductName("allevyn adhesive");
        }else if(product.getProductName().contains("allevyn ag adh")){
            product.setProductName("allevyn ag adh");
        }else if(product.getProductName().contains("allevyn ag gentle border") && product.getStrength().equals("7.5x7.5")){
            product.setProductName("allevyn ag gentle border");
            product.setStrength("7.5x7.5cm");
        }else if(product.getProductName().contains("allevyn ag gentle border") && product.getStrength().equals("12.5cm2")){
            product.setProductName("allevyn ag gentle border");
            product.setStrength("12.5cmx12.5cm");
        }else if(product.getProductName().contains("allevyn ag gentle border")){
            product.setProductName("allevyn ag gentle border");
        }else if(product.getProductName().contains("allevyn gentle border") && product.getStrength().equals("10x10")){
            product.setProductName("allevyn gentle border");
            product.setStrength("10x10cm");
        }else if(product.getProductName().contains("allevyn non-adh")){
            product.setProductName("allevyn non-adh");
        }else if(product.getProductName().contains("amisulpiride")){
            product.setProductName("amisulpride");
        }else if(product.getProductName().contains("amorolfine nail lacq")){
            product.setProductName("amorolfine nail lacq");
        }else if(product.getProductName().contains("amoxicillin s/f sac")){
            product.setProductName("amoxicillin s/f sac");
        }else if(product.getProductName().contains("aquacel ag dressing")){
            product.setProductName("aquacel ag dressing");
        }else if(product.getProductName().contains("aquacel ag extra dressing")){
            product.setProductName("aquacel ag extra");
        }else if(product.getProductName().contains("aquacel ag foam non-adhesive")){
            product.setProductName("aquacel ag foam non adhesive");
        }else if(product.getProductName().contains("aquacel ag ribbon")){
            product.setProductName("aquacel ag ribbon");
        }else if(product.getProductName().contains("aquacel ag+extra")){
            product.setProductName("aquacel ag+ extra");
        }else if(product.getProductName().contains("aquacel ag+ribbon")){
            product.setProductName("aquacel ag+ ribbon");
        }else if(product.getProductName().contains("aquacel extra dressing")){
            product.setProductName("aquacel extra");
        }else if(product.getProductName().contains("aquacel foam adhesive")){
            product.setProductName("aquacel foam adhesive");
        }else if(product.getProductName().contains("aquacel foam non adh")){
            product.setProductName("aquacel foam non adh");
        }else if(product.getProductName().contains("aquacel ribbon")){
            product.setProductName("aquacel ribbon");
        }else if(product.getProductName().contains("aquacel")){
            product.setProductName("aquacell");
        }else if(product.getProductName().contains("atovaquone/proguanil (malarone)") && product.getStrength().equals("250/100")){
            product.setProductName("atovaquone proguanil");
            product.setStrength("250mg/100mg");
        }else if(product.getProductName().contains("atrauman dressing")){
            product.setProductName("atrauman dressing");
        }else if(product.getProductName().contains("azarga eye drops") && product.getStrength().equals("10+5mg")){
            product.setProductName("azarga eye drops");
            product.setStrength("10mg/ml+5mg/ml");
        }else if(product.getProductName().contains("azopt eye drops") && product.getStrength().equals("5ml")){
            product.setProductName("azopt eye drops");
            product.setStrength("10mg/ml");
        }else if(product.getProductName().contains("adapalene (a) or differin (d) gel") && product.getStrength().equals("0.10%")){
            product.setProductName("differin gel");
            product.setStrength("0.10%");
        }else if(product.getProductName().contains("adapalene cream") && product.getStrength().equals("0.10%")){
            product.setProductName("differin cream");
            product.setStrength("0.1%");
        }else if(product.getProductName().contains("alzest rivastigmine 4.6 patch") && product.getStrength().equals("4.6mg")){
            product.setProductName("rivastigmine patch");
            product.setStrength("4.6mg");
        }else if(product.getProductName().contains("aveeno cream") && product.getPacksize().equals("100g")){
            product.setProductName("aveeno cream");
            product.setStrength("100ml");
            product.setPacksize("");
        }else if(product.getProductName().contains("aveeno cream") && product.getPacksize().equals("300g")){
            product.setProductName("aveeno cream");
            product.setStrength("300ml");
            product.setPacksize("");
        }else if(product.getProductName().contains("aveeno cream") && product.getPacksize().equals("500g")){
            product.setProductName("aveeno cream");
            product.setStrength("500ml");
            product.setPacksize("");
        }else if(product.getProductName().contains("bard flip-flo catheter valve bff5")){
            product.setProductName("bard flip flo catheter valve bff5");
        }else if(product.getProductName().contains("b-d pen needles") && product.getStrength().equals("31g/5mm")){
            product.setProductName(product.getProductName().replaceAll("b-d","bd      "));
            product.setStrength("5mm");
        }else if(product.getProductName().contains("b-d pen needles") && product.getStrength().equals("31g/8mm")){
            product.setProductName(product.getProductName().replaceAll("b-d","bd      "));
            product.setStrength("8mm");
        }else if(product.getProductName().contains("b-d pen needles") && product.getStrength().equals("32g/4mm")){
            product.setProductName(product.getProductName().replaceAll("b-d","bd      "));
            product.setStrength("4mm");
        }else if(product.getProductName().contains("b-d")){
            product.setProductName(product.getProductName().replaceAll("b-d","bd      "));
        }else if(product.getProductName().contains("betmiga mr tabs")){
            product.setProductName("betmiga mr tabs");
        }else if(product.getProductName().contains("biatain non adh dressing")){
            product.setProductName("biatain non adh");
        }else if(product.getProductName().contains("biatain silicone dressing")){
            product.setProductName("biatain silicone");
        }else if(product.getProductName().contains("brimonidine+timolol combigan ed")){
            product.setProductName("brimonidine timolol ed");
        }else if(product.getProductName().contains("brintelix tab vortioxetine")){
            product.setProductName("brintellix tab");
        }else if(product.getProductName().contains("brinzolamide+timolol gen azarga")){
            product.setProductName("brinzolamide timolol ed");
        }else if(product.getProductName().contains("briviact tabs (brivaracetam)")){
            product.setProductName("briviact tabs");
        }else if(product.getProductName().contains("budesonide ns rhinocort 3.49+")){
            product.setProductName("budesonide ns");
        }else if(product.getProductName().contains("benzydamine throat spray") && product.getStrength().equals("0.15%")){
            product.setProductName("benzydamine spray");
            product.setStrength("0.15%");
        }else if(product.getProductName().contains("candesartan tabs")){
            product.setProductName("candesartan tabs");
        }else if(product.getProductName().contains("canesten clotrimazole cream thrush")){
            product.setProductName("canesten cream thrush");
        }else if(product.getProductName().contains("carbimazole-longlif")){
            product.setProductName("carbimazole tab");
        }else if(product.getProductName().contains("carmellose eye drops 0.4ml") || product.getProductName().contains("carmellose eye drops pf 0.4ml") || product.getProductName().contains("carmellosepf cellus,evolv,ocu-lub,pfdr,vizc")){
            product.setProductName("carmellose eye drops 0.4ml");
        }else if(product.getProductName().contains("cavilon barrier cream")){
            product.setProductName("cavilon barrier cream");
        }
        else if(product.getProductName().contains("cetraben emollient cr agcy yes")){
            product.setProductName("cetraben emollient");
        }else if(product.getProductName().contains("cetraban")){
            product.setProductName(product.getProductName().replaceAll("cetraban","cetraben"));
        }else if(product.getProductName().contains("chlorpheniramine")){
            product.setProductName(product.getProductName().replaceAll("chlorpheniramine", "chlorphenamine"));
        }else if(product.getProductName().contains("ciloxan (ciprofloxacin) eye drops")){
            product.setProductName("ciloxan eye drops");
        }else if(product.getProductName().contains("circadin tab is rx generic? (cheaper)")){
            product.setProductName("circadin tab");
        }else if(product.getProductName().contains("co-amilofruse ls tabs")){
            product.setProductName("co-amilofruse tabs");
        }else if(product.getProductName().contains("co-careldopa") && product.getStrength().equals("10/100")){
            product.setProductName("co-careldopa tab");
            product.setStrength("10mg/100mg");
        }else if(product.getProductName().contains("co-careldopa") && product.getStrength().equals("12.5/50")){
            product.setProductName("co-careldopa tab");
            product.setStrength("12.5mg/50mg");
        }else if(product.getProductName().contains("colecalciferol tabs")){
            product.setProductName("colecalciferol tabs");
        }else if(product.getProductName().contains("olestyramine 4g sf sachet")){
            product.setProductName("olestyramine 4g sf sachet");
        }else if(product.getProductName().contains("cosmopor e dressing")){
            product.setProductName("cosmopor e");
        }else if(product.getProductName().contains("coversyl arginine tab")){
            product.setProductName("coversyl arginine");
        }else if(product.getProductName().contains("cetirizine solution") && product.getStrength().equals("1mg/1ml")){
            product.setProductName("cetirizine solution");
            product.setStrength("");
        }else if(product.getProductName().contains("clotrimazole vag tabs pessary")){
            product.setProductName("clotrimazole vaginal pessary");
        }else if(product.getProductName().contains("co-amoxiclav s/f susp") && product.getStrength().equals("250/62")){
            product.setProductName("co-amoxiclav s/f susp");
            product.setStrength("250mg/62mg");
        }else if(product.getProductName().contains("co-amoxiclav s/f susp") && product.getStrength().equals("400/57")){
            product.setProductName("co-amoxiclav s/f susp");
            product.setStrength("400mg/57mg");
        }else if(product.getProductName().contains("dalacin vaginal cream")){
            product.setProductName("dalacin cream");
        }else if(product.getProductName().contains("debrisoft 10cmx10cm")){
            product.setProductName("debrisoft dressing");
        }else if(product.getProductName().contains("debrisoft lolly")){
            product.setProductName("debrisoft lolly");
        }else if(product.getProductName().contains("depo-medrone+lidocaine")){
            product.setProductName("depo-medrone lido");
        }else if(product.getProductName().contains("diltiazem mr tildiemoct23 stock80")){
            product.setProductName("diltiazem mr tab");
        }else if(product.getProductName().contains("diltiazem xl caps")){
            product.setProductName("diltiazem xl caps");
        }else if(product.getProductName().contains("digoxin tabs") && product.getStrength().equals("62.5")){
            product.setProductName("digoxin tabs");
            product.setStrength("62.5mcg");
        }else if(product.getProductName().contains("dorzolamide/timolol eye drop")){
            product.setProductName("dorzolamide timolol eye drop");
        }else if(product.getProductName().contains("dorzolamide/timolol pf eye drop")){
            product.setProductName("dorzolamide timolol eye drop");
        }else if(product.getProductName().contains("dovobet gel generic cheaper?")){
            product.setProductName("dovobet gel");
        }else if(product.getProductName().contains("oxycycline disp tab vibramycin")){
            product.setProductName("vibramycin disp tabs");
        }else if(product.getProductName().contains("duoresp spiro budes/formet 160") && product.getStrength().equals("160/4.5")){
            product.setProductName("duoresp smax bud/form");
            product.setStrength("160/4.5mcg");
        }else if(product.getProductName().contains("duoresp spiro budes/formet 320") && product.getStrength().equals("320/9")){
            product.setProductName("duoresp smax bud/form");
            product.setStrength("320/9mcg");
        }else if(product.getProductName().contains("duraphat 2800 colgate toothpaste")){
            product.setProductName("duraphat 2800");
        }else if(product.getProductName().contains("duraphat 5000 colgate toothpaste")){
            product.setProductName("duraphat 5000");
        }else if(product.getProductName().contains("eliquis tab now generic")){
            product.setProductName("eliquis tab");
        }else if(product.getProductName().contains("ensure plus advance banana")){
            product.setProductName("ensure plus advance banana");
            product.setStrength("");
        }else if(product.getProductName().contains("ensure plus banana")){
            product.setProductName("ensure plus banana");
            product.setStrength("");
        }else if(product.getProductName().contains("entocort caps cr")){
            product.setProductName("entocort caps");
        }else if(product.getProductName().contains("estradiol pess vaginal tabs")){
            product.setProductName("estradiol vaginal tab");
        }else if(product.getProductName().contains("evotears eye drops p/f")){
            product.setProductName("evotears");
        }else if(product.getProductName().contains("fluticasone nasal spray otc")){
            product.setProductName("fluticasone nasal spray");
        }else if(product.getProductName().contains("fluticasone/salmet airflusal only") || product.getProductName().contains("fluticasone/salmet airflusal/fuse,stalp")){
            product.setProductName("fluticasone sal airflusal");
        }else if(product.getProductName().contains("flutiform 125/5 inhaler") || product.getStrength().equals("125/5")){
            product.setProductName("flutiform inhaler");
            product.setStrength("125/5mcg");
        }else if(product.getProductName().contains("flutiform 125/10 inhaler") || product.getStrength().equals("250/10")){
            product.setProductName("flutiform inhaler");
            product.setStrength("250/10mcg");
        }else if(product.getProductName().contains("fosamax 70mg tab")){
            product.setProductName("fosamax");
        }else if(product.getProductName().contains("strips")){
            product.setProductName("strip");
        }else if(product.getProductName().contains("fusidicbetamcrm xemacort")){
            product.setProductName("fusidic betamethasone crm");
        }else if(product.getProductName().contains("glyceryl trinitrate gtn spray") || product.getProductName().contains("gtn spray glycerin trinitrate")){
            product.setProductName("glyceryl trinitrate spray");
        }else if(product.getProductName().contains("hydrocortisone 1% cream otc")){
            product.setProductName("hydrocortisone 1% cream");
        }else if(product.getProductName().contains("hydroxocobalamin inj cobalin al9.5os")){
            product.setProductName("hydroxocobalamin inj");
        }else if(product.getProductName().contains("hylo-care eye drops")){
            product.setProductName("hylo-care");
        }else if(product.getProductName().contains("hylo-tear eye drops")){
            product.setProductName("hylo-tear");
        }else if(product.getProductName().contains("ibuprofen/codeine tabs")){
            product.setProductName("ibuprofen tabs");
        }else if(product.getProductName().contains("incruse ellipta") && product.getStrength().equals("55")){
            product.setProductName("incruse ellipta");
            product.setStrength("55mcg");
        }else if(product.getProductName().contains("instillagel pre-filled syr (uk)")){
            product.setProductName("instillagel pre-filled syr");
        }else if(product.getProductName().contains("invokana canaglifozin")){
            product.setProductName("invokana tab");
        }else if(product.getProductName().contains("ipratropium nebuliser sol")){
            product.setProductName("ipratropium ster-neb");
        }else if(product.getProductName().contains("ivabradine tabs (procoralan)")){
            product.setProductName("ivabradine tabs");
        }else if(product.getProductName().contains("lansoprazole oro disp tabs")){
            product.setProductName("lansoprazole odt");
        }else if(product.getProductName().contains("laxido paediatric sachets")){
            product.setProductName("laxido paediatric plain");
        }else if(product.getProductName().contains("levetiracetam oral solution")){
            product.setProductName("levetiracetam or/soln");
        }else if(product.getProductName().contains("levothyroxine oral soln")){
            product.setProductName("levothyroxine or/sol");
        }else if(product.getProductName().contains("lipitor tabs uk only")){
            product.setProductName("lipitor tabs");
        }else if(product.getProductName().contains("losartan hctz tabs") && product.getStrength().equals("50/12.5m")){
            product.setProductName("losartan hctz tabs");
            product.setStrength("50mg/12.5mg");
        }else if(product.getProductName().contains("lorazepam oval scoredgenus")){
            product.setProductName("lorazepam oval");
        }else if(product.getProductName().contains("losartan hctz")){
            product.setProductName("losartan hctz tab");
        }else if(product.getProductName().contains("medi derma-s barcr")){
            product.setProductName("medi derma-s barrier cream");
        }else if(product.getProductName().contains("menthol in aqueous cream")){
            product.setProductName("aqueous cream");
        }else if(product.getProductName().contains("methyldopa tab aldomet")){
            product.setProductName("methyldopa tab");
        }else if(product.getProductName().contains("montelukast granules sachet")){
            product.setProductName("montelukast granules");
        }else if(product.getProductName().contains("moxonidine tabs") && product.getStrength().equals("200mg")){
            product.setProductName("moxonidine tabs");
            product.setStrength("200mcg");
        }else if(product.getProductName().contains("naramig tabs")){
            product.setProductName("naramig tabs");
        }else if(product.getProductName().contains("naratriptan tabs")){
            product.setProductName("naratriptan tabs");
        }else if(product.getProductName().contains("nitrofurantoin tabs")){
            product.setProductName("nitrofurantoin tabs");
        }else if(product.getProductName().contains("norethisterone tabs")){
            product.setProductName("norethisterone tabs");
        }else if(product.getProductName().contains("novofine 30g needles agency yes")){
            product.setProductName("novofine 30g needles");
        }else if(product.getProductName().contains("novofine 31g needles nwos agy yes")){
            product.setProductName("novofine 31g needles");
        }else if(product.getProductName().contains("nizatidine caps") && product.getStrength().equals("300")){
            product.setProductName("nizatidine caps");
            product.setStrength("300mg");
        }else if(product.getProductName().contains("nalcrom cap")){
            product.setProductName("nalcrom cap");
        }else if(product.getProductName().contains("octasa mr tabs")){
            product.setProductName("octasa mr tabs");
        }else if(product.getProductName().contains("procyclidine tab kemadrine only")){
            product.setProductName("kemadrine tabs");
        }else if(product.getProductName().contains("prograf caps")){
            product.setProductName("prograf caps");
        }else if(product.getProductName().contains("promethazine hcl tabs")){
            product.setProductName("promethazine tabs");
        }else if(product.getProductName().contains("promethazine teoclate tabs")){
            product.setProductName("promethazine tabs");
        }else if(product.getProductName().contains("proshield foam+spr cleanser")){
            product.setProductName("proshield fm sry");
        }else if(product.getProductName().contains("proshield plus protect")){
            product.setProductName("proshield plus skin protectant");
        }else if(product.getProductName().contains("prostap 3 depot inj")){
            product.setProductName("rostap 3 dcs inj");
        }else if(product.getProductName().contains("pyridoxine tabs (licenced)")){
            product.setProductName("pyridoxine tabs licensed");
        }else if(product.getProductName().contains("pyridoxine tabs unlicenced")){
            product.setProductName("pyridoxine tabs unlicensed");
        }else if(product.getProductName().contains("prednisolone soluble")){
            product.setProductName("prednisolone soluble tab");
        }else if(product.getProductName().contains("quetiapine sr xl tab")){
            product.setProductName("quetiapine xl tab");
        }else if(product.getProductName().contains("requip xl tabs")){
            product.setProductName("requip xl");
        }else if(product.getProductName().contains("requip xl tabs")){
            product.setProductName("requip xl");
        }else if(product.getProductName().contains("resolor tabs prucalopride")){
            product.setProductName("resolor tabs");
        }else if(product.getProductName().contains("risperidone tabs")){
            product.setProductName("risperidone tabs");
        }else if(product.getProductName().contains("rivastigmine patch 24hr")){
            product.setProductName("rivastigmine patch");
        }else if(product.getProductName().contains("scopoderm patch") && product.getStrength().equals("1.5")){
            product.setProductName("scopoderm patch");
            product.setStrength("1.5mg");
        }else if(product.getProductName().contains("seretide acc (fluticasone/salmet)")){
            product.setProductName("seretide acc");
        }else if(product.getProductName().contains("seretide evo (fluticasone/salmet)") && product.getStrength().equals("125mcg")){
            product.setProductName("seretide evo");
            product.setStrength("125/25mcg");
        }else if(product.getProductName().contains("seretide evo (fluticasone/salmet)") && product.getStrength().equals("250mcg")){
            product.setProductName("seretide evo");
            product.setStrength("250/25mcg");
        }else if(product.getProductName().contains("seretide evo (fluticasone/salmet)")){
            product.setProductName("seretide evo");
        }else if(product.getProductName().contains("serevent (salmet) accuhaler")){
            product.setProductName("serevent accuhaler");
        }else if(product.getProductName().contains("serevent (salmet) evohaler")){
            product.setProductName("serevent evohaler");
        }else if(product.getProductName().contains("sevelamer tab nov23: 380 in stock")){
            product.setProductName("sevelamer tab");
        }else if(product.getProductName().contains("sinemet plus brand")){
            product.setProductName("sinemet plus");
        }else if(product.getProductName().contains("sirdupla fluticasone/salmet mylan") && product.getStrength().equals("25/125")){
            product.setProductName("sirdupla mylan");
            product.setStrength("25mcg/125mcg");
        }else if(product.getProductName().contains("sirdupla fluticasone/salmet mylan") && product.getStrength().equals("25/250")){
            product.setProductName("sirdupla mylan");
            product.setStrength("25mcg/250mcg");
        }else if(product.getProductName().contains("sitagliptin januvia")){
            product.setProductName("sitagliptin");
        }else if(product.getProductName().contains("tamoxifen tabs brand?")){
            product.setProductName("tamoxifen tabs");
        }else if(product.getProductName().contains("tamsulosin mr tabs flomaxtra")){
            product.setProductName("tamsulosin mr tabs");
        }else if(product.getProductName().contains("tegaderm +pad dressing")){
            product.setProductName("tegaderm pad dressing");
        }else if(product.getProductName().contains("tegaderm film dressing")){
            product.setProductName("tegaderm film dressing");
        }else if(product.getProductName().contains("tegaderm foam adh")){
            product.setProductName("tegaderm foam adh");
        }else if(product.getProductName().contains("tegretol pr tabs")){
            product.setProductName("tegretol pr tabs");
        }else if(product.getProductName().contains("terbinafine hydrochloride cream")){
            product.setProductName("terbinafine cream");
        }else if(product.getProductName().contains("thiamin vitamin b1 tabs")){
            product.setProductName("thiamin tabs");
        }else if(product.getProductName().contains("tobradex dexameth+tobramycin ed")){
            product.setProductName("tobradex ed");
        }else if(product.getProductName().contains("tolterodine xl caps (neditol)")){
            product.setProductName("tolterodine xl caps");
        }else if(product.getProductName().contains("tramadol sr cap not tabs")){
            product.setProductName("tramadol sr capsules");
        }else if(product.getProductName().contains("tramadol/paracetamol")){
            product.setProductName("tramadol/para tab");
        }else if(product.getProductName().contains("travoprost eyedrops") && product.getStrength().equals("40mg/ml")){
            product.setProductName("travoprost eye drops");
            product.setStrength("40mcg/ml");
        }else if(product.getProductName().contains("travoprost eyedrops")){
            product.setProductName("travoprost eye drops");
        }else if(product.getProductName().contains("travoprost/timolol ed")){
            product.setProductName("travoprost/timolol eye d");
        }else if(product.getProductName().contains("viagra connect tabs")){
            product.setProductName("viagra tabs");
        }else if(product.getProductName().contains("vipdomet tablets") && product.getStrength().equals("12.5/1g")){
            product.setProductName("vipdomet tablets");
            product.setStrength("12.5mg");
        }else if(product.getProductName().contains("vipdomet tablets")){
            product.setProductName("vipdomet");
        }else if(product.getProductName().contains("xarelto tabs (rivaroxaban)")){
            product.setProductName("xarelto tabs");
        }else if(product.getProductName().contains("zolmitriptan tabs orodisp")){
            product.setProductName("zolmitriptan disp tab");
        }else if(product.getProductName().contains("beconase aq nasal spray") && product.getStrength().equals("")) {
            product.setProductName("beconase aque nasal spy");
            product.setStrength("50/mcg");
        }else if(product.getProductName().contains("calcipotriol+betamet gel")) {
            product.setProductName("calcipotriol betam gel");
        }else if(product.getProductName().contains("calcipotriol+betamet oint")) {
            product.setProductName("calcipotriol betam oint");
        }else if(product.getProductName().contains("donepezil orodisp tabs")) {
            product.setProductName("donepezil tabs");
        }else if(product.getProductName().contains("donepezil orodisp tabs") || product.getProductName().contains("donepezil orodisp")) {
            product.setProductName("donepezil tabs");
        }else if(product.getProductName().contains("celluvisc eye drops (carmellose)") && product.getStrength().equals("0.50%")) {
            product.setProductName("celluvisc");
            product.setStrength("0.5%");
        }else if(product.getProductName().contains("celluvisc eye drops (carmellose)") ) {
            product.setProductName("celluvisc");
        }else if(product.getProductName().contains("co-careldopa genericrx") ) {
            product.setProductName("co-careldopa tab");
        }else if(product.getProductName().contains("co-codamol capsules") && product.getStrength().equals("8/500mg")) {
            product.setProductName("co-codamol capsules");
            product.setStrength("8");
        }else if(product.getProductName().contains("co-codamol capsules") && product.getStrength().equals("15/500mg")) {
            product.setProductName("co-codamol capsules");
            product.setStrength("15");
        }else if(product.getProductName().contains("co-codamol capsules") && product.getStrength().equals("30/500mg")) {
            product.setProductName("co-codamol capsules");
            product.setStrength("30");
        }else if(product.getProductName().contains("co-codamol capsules") && product.getStrength().equals("30/500mg")) {
            product.setProductName("co-codamol capsules");
            product.setStrength("30");
        }else if(product.getProductName().contains("co-codamol eff tabs") && product.getStrength().equals("30/500mg")) {
            product.setProductName("co-codamol eff tabs");
            product.setStrength("30");
        }else if(product.getProductName().contains("co-codamol eff tabs") && product.getStrength().equals("8/500mg")) {
            product.setProductName("co-codamol eff tabs");
            product.setStrength("8");
        }else if(product.getProductName().contains("co-codamol tabs") && product.getStrength().equals("8/500mg")) {
            product.setProductName("co-codamol tabs");
            product.setStrength("8");
        }else if(product.getProductName().contains("co-codamol tabs") && product.getStrength().equals("15/500mg")) {
            product.setProductName("co-codamol tabs");
            product.setStrength("15");
        }else if(product.getProductName().contains("co-codamol tabs") && product.getStrength().equals("30/500mg")) {
            product.setProductName("co-codamol tabs");
            product.setStrength("30");
        }else if(product.getProductName().contains("fluticasone/salmet aloflute") && product.getStrength().equals("250/25") && product.getPacksize().equals("evo120d")) {
            product.setProductName("fluticasone/sal");
            product.setStrength("250/25mcg");
            product.setPacksize("120");
        }else if(product.getProductName().contains("jardiance tabs empaglifozin") ) {
            product.setProductName("jardiance tabs");
        }else if(product.getProductName().contains("celluvisc eye drops") ) {
            product.setProductName("celluvisc");
        }else if(product.getProductName().contains("stalevo 125/31.25/200") ) {
            product.setProductName("stalevo tab 125mg/31.25mg/200mg");
        }else if(product.getProductName().contains("stalevo 175/43.75/200mg tab") ) {
            product.setProductName("stalevo tab 175mg/43.75mg/200mg");
        }else if(product.getProductName().contains("stalevo 200/50/200") ) {
            product.setProductName("stalevo tab 200mg/50mg/200mg");
        }else if(product.getProductName().contains("tiopex unit dose eye gel") ) {
            product.setProductName("tiopex eye gel");
        }else if(product.getProductName().contains("urispas tablets") ) {
            product.setProductName("urispas tablets");
        }else if(product.getProductName().contains("uro-tainer sod chl") ) {
            product.setProductName("uro-tainer sod chl");
        }else if(product.getProductName().contains("viagra connect p tabs") ) {
            product.setProductName("viagra tabs");
        }else if(product.getProductName().contains("vitamin b co tabs (unlicensed)") ) {
            product.setProductName("vitamin b co tabs");
        }else if(product.getProductName().contains("simple eye oint") && product.getStrength().equals("4gm")  && product.getPacksize().equals("1")) {
            product.setProductName("simple eye oint");
            product.setStrength("4gm");
            product.setPacksize("");
        }



    }

    public void print(List<LookupResult> lookupResults){
        lookupResults.stream().forEach(
                v -> System.out.println(" Trident " + v.getDescription()+" : "+ v.getPriceString() + " : "+ v.getAvailable())
        );
    }

    public static boolean specialConsiderationOfProductResultsFromWebsite(String websiteDescription, Product product){

        String productNameFromExcel = product.getProductName();
        String strengthFromExcel = product.getStrength();

        if(productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && !productNameFromExcel.toLowerCase().contains("foam")
                && !productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("ribbon")
                && !productNameFromExcel.toLowerCase().contains("ag+")
                && !productNameFromExcel.toLowerCase().contains("silver")
                && !productNameFromExcel.toLowerCase().contains("tulle")){
            return websiteDescription.toLowerCase().contains("ag")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && !websiteDescription.toLowerCase().contains("foam")
                    && !websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("ribbon")
                    && !websiteDescription.toLowerCase().contains("ag+")
                    && !websiteDescription.toLowerCase().contains("silver")
                    && !websiteDescription.toLowerCase().contains("tulle");
        }else if(productNameFromExcel.toLowerCase().contains("ag")
                && productNameFromExcel.toLowerCase().contains("extra")
                && !productNameFromExcel.toLowerCase().contains("foam")
                && !productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("ribbon")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return websiteDescription.toLowerCase().contains("ag")
                    && websiteDescription.toLowerCase().contains("extra")
                    && !websiteDescription.toLowerCase().contains("foam")
                    && !websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("ribbon")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && productNameFromExcel.toLowerCase().contains("foam")
                && productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("ribbon")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return websiteDescription.toLowerCase().contains("ag")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && websiteDescription.toLowerCase().contains("foam")
                    && websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("ribbon")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && productNameFromExcel.toLowerCase().contains("foam")
                && productNameFromExcel.toLowerCase().contains("adh")
                && productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("ribbon")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return websiteDescription.toLowerCase().contains("ag")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && websiteDescription.toLowerCase().contains("foam")
                    && websiteDescription.toLowerCase().contains("adh")
                    && websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("ribbon")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && !productNameFromExcel.toLowerCase().contains("foam")
                && !productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && productNameFromExcel.toLowerCase().contains("ribbon")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return websiteDescription.toLowerCase().contains("ag")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && !websiteDescription.toLowerCase().contains("foam")
                    && !websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && websiteDescription.toLowerCase().contains("ribbon")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(productNameFromExcel.toLowerCase().contains("ag+")
                && productNameFromExcel.toLowerCase().contains("extra")
                && !productNameFromExcel.toLowerCase().contains("foam")
                && !productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("ribbon")){
            return websiteDescription.toLowerCase().contains("ag+")
                    && websiteDescription.toLowerCase().contains("extra")
                    && !websiteDescription.toLowerCase().contains("foam")
                    && !websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("ribbon");
        }else if(productNameFromExcel.toLowerCase().contains("ag+")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && !productNameFromExcel.toLowerCase().contains("foam")
                && !productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && productNameFromExcel.toLowerCase().contains("ribbon")){
            return websiteDescription.toLowerCase().contains("ag+")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && !websiteDescription.toLowerCase().contains("foam")
                    && !websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && websiteDescription.toLowerCase().contains("ribbon");
        }else if(!productNameFromExcel.toLowerCase().contains("ag")
                && productNameFromExcel.toLowerCase().contains("extra")
                && !productNameFromExcel.toLowerCase().contains("foam")
                && !productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("ribbon")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return !websiteDescription.toLowerCase().contains("ag")
                    && websiteDescription.toLowerCase().contains("extra")
                    && !websiteDescription.toLowerCase().contains("foam")
                    && !websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("ribbon")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(!productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && productNameFromExcel.toLowerCase().contains("foam")
                && productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("ribbon")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return !websiteDescription.toLowerCase().contains("ag")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && websiteDescription.toLowerCase().contains("foam")
                    && websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("ribbon")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(!productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && productNameFromExcel.toLowerCase().contains("foam")
                && productNameFromExcel.toLowerCase().contains("adh")
                && productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("ribbon")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return !websiteDescription.toLowerCase().contains("ag")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && websiteDescription.toLowerCase().contains("foam")
                    && websiteDescription.toLowerCase().contains("adh")
                    && websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("ribbon")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(!productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && !productNameFromExcel.toLowerCase().contains("foam")
                && !productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && productNameFromExcel.toLowerCase().contains("ribbon")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return !websiteDescription.toLowerCase().contains("ag")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && !websiteDescription.toLowerCase().contains("foam")
                    && !websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && websiteDescription.toLowerCase().contains("ribbon")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(productNameFromExcel.toLowerCase().contains("adh") && !productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("non")){
            return websiteDescription.contains("adh") && !websiteDescription.contains("ag") && !websiteDescription.contains("non");
        }else if(productNameFromExcel.toLowerCase().contains("adh") && productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("non")){
            return websiteDescription.contains("adh") && websiteDescription.contains("ag") && !websiteDescription.contains("non");
        }else if(productNameFromExcel.toLowerCase().contains("gentle bord") && !productNameFromExcel.toLowerCase().contains("ag")){
            return websiteDescription.contains("gentle bord") && !websiteDescription.contains("ag");
        }else if(productNameFromExcel.toLowerCase().contains("non") && productNameFromExcel.toLowerCase().contains("adh")){
            return (websiteDescription.contains("non") && websiteDescription.contains("adh")) || websiteDescription.contains("n-adh");
        }/*else if(!productNameFromExcel.toLowerCase().contains("ec") && productNameFromExcel.toLowerCase().contains("tab")){
            return !websiteDescription.contains("ec") && websiteDescription.contains("tab");
        }*/else if(!productNameFromExcel.toLowerCase().contains("tulle") && productNameFromExcel.toLowerCase().contains("silver")){
            return !websiteDescription.contains("tulle") && websiteDescription.contains("silver");
        }else if(productNameFromExcel.toLowerCase().contains("tulle") && !productNameFromExcel.toLowerCase().contains("silver")){
            return websiteDescription.contains("tulle") && !websiteDescription.contains("silver");
        }else if(productNameFromExcel.toLowerCase().contains("body") && productNameFromExcel.toLowerCase().contains("wash")
                && !productNameFromExcel.toLowerCase().contains("baby") && !productNameFromExcel.toLowerCase().contains("lotion") && !productNameFromExcel.toLowerCase().contains("moist")
                && !productNameFromExcel.toLowerCase().contains("hand")){
            return websiteDescription.contains("body") && websiteDescription.contains("wash")
                    && !websiteDescription.toLowerCase().contains("baby") && !websiteDescription.toLowerCase().contains("lotion") && !websiteDescription.toLowerCase().contains("moist")
                    && !websiteDescription.toLowerCase().contains("hand");
        }else if(productNameFromExcel.toLowerCase().contains("cream") && !productNameFromExcel.toLowerCase().contains("wash")
                && !productNameFromExcel.toLowerCase().contains("baby") && !productNameFromExcel.toLowerCase().contains("lotion") && !productNameFromExcel.toLowerCase().contains("moist")
                && !productNameFromExcel.toLowerCase().contains("hand")){
            return (websiteDescription.contains("cream") || websiteDescription.contains("crm") ) && !websiteDescription.contains("wash")
                    && !websiteDescription.toLowerCase().contains("baby") && !websiteDescription.toLowerCase().contains("lotion") && !websiteDescription.toLowerCase().contains("moist")
                    && !websiteDescription.toLowerCase().contains("hand");
        }else if(productNameFromExcel.toLowerCase().contains("lotion") && productNameFromExcel.toLowerCase().contains("wash")
                && !productNameFromExcel.toLowerCase().contains("baby") &&  !productNameFromExcel.toLowerCase().contains("moist")
                && !productNameFromExcel.toLowerCase().contains("hand")){
            return websiteDescription.contains("lotion") && websiteDescription.contains("wash")
                    && !websiteDescription.toLowerCase().contains("baby") &&  !websiteDescription.toLowerCase().contains("moist")
                    && !websiteDescription.toLowerCase().contains("hand");
        }else if(productNameFromExcel.toLowerCase().contains("silicone") && !productNameFromExcel.toLowerCase().contains("adh") && !productNameFromExcel.toLowerCase().contains("lite")){
            return websiteDescription.contains("sil") && !websiteDescription.contains("adh") && !websiteDescription.contains("lite");
        }else if(productNameFromExcel.toLowerCase().contains("drowsy") && !productNameFromExcel.toLowerCase().contains("non") && !productNameFromExcel.toLowerCase().contains("nd")){
            return websiteDescription.contains("drowsy") && !websiteDescription.contains("non") && !websiteDescription.contains("nd");
        }else if(productNameFromExcel.toLowerCase().contains("neutral") && !productNameFromExcel.toLowerCase().contains("extra")){
            return websiteDescription.contains("neutral") && !websiteDescription.contains("extra");
        }else if(productNameFromExcel.toLowerCase().contains("codeine tab") && !productNameFromExcel.toLowerCase().contains("phosphate")){
            return websiteDescription.contains("codeine tab") && !websiteDescription.contains("phosphate");
        }else if(productNameFromExcel.toLowerCase().contains("co-careldopa") && !strengthFromExcel.contains("25")){
            return websiteDescription.contains("co-careldopa") && !websiteDescription.contains("25");
        }else if(productNameFromExcel.toLowerCase().contains("susp") && !(productNameFromExcel.toLowerCase().contains("sf")
                || productNameFromExcel.toLowerCase().contains("s/f")|| productNameFromExcel.toLowerCase().contains("sugar free"))){

            List<String> pOrPom = Arrays.stream(websiteDescription.toLowerCase().split("\\(|\\)|\\s|\\[|]"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            List<String> pOrPomInExcel = Arrays.stream(product.getProductNameUnmodified().toLowerCase().split("\\(|\\)|\\s|\\[|]"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            if(pOrPom.isEmpty()){
                // need to check with manasa if s/f in excel is not present then can we order sf from website???
                return websiteDescription.contains("susp");
                        //&& !(websiteDescription.contains("sf") || websiteDescription.contains("s/f"));
            }
            return websiteDescription.contains("susp")
                    //&& !(websiteDescription.contains("sf") || websiteDescription.contains("s/f"))
                    &&!pOrPomInExcel.isEmpty()? pOrPomInExcel.contains(pOrPom.get(0)):true;

        }else if(productNameFromExcel.toLowerCase().contains("susp") && (productNameFromExcel.toLowerCase().contains("sf")
                || productNameFromExcel.toLowerCase().contains("s/f")|| productNameFromExcel.toLowerCase().contains("sugar free"))){

            List<String> pOrPom = Arrays.stream(websiteDescription.toLowerCase().split("\\(|\\)|\\s|\\[|]"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            List<String> pOrPomInExcel = Arrays.stream(product.getProductNameUnmodified().toLowerCase().split("\\(|\\)|\\s|\\[|]"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            if(pOrPom.isEmpty()){
                return websiteDescription.contains("susp") && (websiteDescription.contains("sf") || websiteDescription.contains("s/f"));
            }

            return websiteDescription.contains("susp") && (websiteDescription.contains("sf") || websiteDescription.contains("s/f"))
                    &&!pOrPomInExcel.isEmpty()? pOrPomInExcel.contains(pOrPom.get(0)):true;


        }else if((productNameFromExcel.toLowerCase().contains("tab") || productNameFromExcel.toLowerCase().contains("caplet") || productNameFromExcel.toLowerCase().contains("oval"))
                && !productNameFromExcel.toLowerCase().contains("hctz")
                && !productNameFromExcel.toLowerCase().contains(" pr ") && !productNameFromExcel.toLowerCase().contains(" sr ") && !productNameFromExcel.toLowerCase().contains(" mr ") && !productNameFromExcel.toLowerCase().contains(" s/c ")
                && !productNameFromExcel.toLowerCase().contains(" ec ")
                && !productNameFromExcel.toLowerCase().contains("disp")
                && !productNameFromExcel.toLowerCase().contains("soluble")
                &&  !productNameFromExcel.toLowerCase().contains(" eff ") &&  !productNameFromExcel.toLowerCase().contains(" ef ")
                &&  !productNameFromExcel.toLowerCase().contains(" hct ") &&  !productNameFromExcel.toLowerCase().contains(" xl ")
                ){

            boolean b = websiteDescription.contains("tab") || websiteDescription.contains("capl") || websiteDescription.contains("fct") || websiteDescription.contains("f/c") || websiteDescription.contains("film coated");

            List<String> pOrPom = Arrays.stream(websiteDescription.toLowerCase().split("\\(|\\)|\\s|\\[|]"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            List<String> pOrPomInExcel = Arrays.stream(product.getProductNameUnmodified().toLowerCase().split("\\(|\\)|\\s|\\[|]"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            if(pOrPom.isEmpty()){
                return b
                        && !websiteDescription.contains(" pr ") && !websiteDescription.contains(" sr ") && !websiteDescription.contains(" s.r ") && !websiteDescription.contains(" mr ") && !websiteDescription.contains(" s/c ")
                        && !websiteDescription.contains(" ec ") && !websiteDescription.contains(" e/c ")
                        && !websiteDescription.contains("hctz")
                        && !websiteDescription.contains("disp")
                        && !websiteDescription.contains("soluble")
                        && !websiteDescription.contains(" eff ")
                        && !websiteDescription.contains(" hct ")
                        && !websiteDescription.contains(" xl ");
            }
            return b
                    && !websiteDescription.contains(" pr ") && !websiteDescription.contains(" sr ") && !websiteDescription.contains(" s.r ") && !websiteDescription.contains(" mr ") && !websiteDescription.contains(" s/c ")
                    && !websiteDescription.contains(" ec ") && !websiteDescription.contains(" e/c ")
                    && !websiteDescription.contains("hctz")
                    && !websiteDescription.contains("disp")
                    && !websiteDescription.contains("soluble")
                    && !websiteDescription.contains(" eff ")
                    && !websiteDescription.contains(" hct ")
                    && !websiteDescription.contains(" xl ")
                    && !pOrPomInExcel.isEmpty()? pOrPomInExcel.contains(pOrPom.get(0)):true;





            /*return (websiteDescription.contains("tab") || websiteDescription.contains("capl") || websiteDescription.contains("fct"))
                    && !websiteDescription.contains("pr") && !websiteDescription.contains("sr") && !websiteDescription.contains("mr")
                    && !websiteDescription.contains("ec") && !websiteDescription.contains("e/c")
                    && !websiteDescription.contains("disp")
                    && !websiteDescription.contains("eff")
                    && !websiteDescription.contains("hct")
                    && !websiteDescription.contains("xl")
                    && !websiteDescription.contains("pom");*/
        }else if(productNameFromExcel.toLowerCase().contains("cap")){
            List<String> pOrPom = Arrays.stream(websiteDescription.toLowerCase().split("\\(|\\)|\\s|\\[|]"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            List<String> pOrPomInExcel = Arrays.stream(product.getProductNameUnmodified().toLowerCase().split("\\(|\\)|\\s|\\[|]"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            if(pOrPom.isEmpty()){
                return websiteDescription.contains("cap") && !websiteDescription.contains("tab");
            }

            return websiteDescription.contains("cap")
                    && !websiteDescription.contains("tab")
                    &&!pOrPomInExcel.isEmpty()? pOrPomInExcel.contains(pOrPom.get(0)):true;

            //return websiteDescription.contains("cap") && !websiteDescription.contains("pom");
        } else if(productNameFromExcel.contains("calci") && !productNameFromExcel.contains("betam")){
            return websiteDescription.contains("calci") && !websiteDescription.contains("betam");
        }else if(productNameFromExcel.contains("dorzolamide") && !productNameFromExcel.contains("timolol")){
            return websiteDescription.contains("dorzolamide") && !websiteDescription.contains("timolol");
        }
        // make sure the below should be the last
        else if(!productNameFromExcel.toLowerCase().contains("atrauman") && !productNameFromExcel.toLowerCase().contains("tulle")){
            return !websiteDescription.contains("atrauman") && !websiteDescription.contains("tulle");
        }

        return true;
    }















    private List<LookupResult> lookupResults(WebDriver driver, String productName, String strength) throws InterruptedException {
        String prodNameToBeGivenInSearchField = productName.length()>5 ? productName.substring(0,5): productName;
        String strengthToBeGivenInSearchField = null;
        if(strength!=null){
            Pattern regex = Pattern.compile("(\\d+(?:\\.\\d+)?)");
            Matcher matcher = regex.matcher(strength);
            if(matcher.find()){
                strengthToBeGivenInSearchField = matcher.group(1);
            }
        }


        driver.findElement(By.xpath("/html[1]/body[1]/div[1]/header[1]/div[1]/div[1]/div[1]/div[4]/div[1]/div[1]/div[1]/div[1]/span[1]/lightning-input[1]/lightning-primitive-input-simple[1]/div[1]/div[1]/input[1]")).clear();
        if(strength!=null && !strength.equals("")){
            driver.findElement(By.xpath("/html[1]/body[1]/div[1]/header[1]/div[1]/div[1]/div[1]/div[4]/div[1]/div[1]/div[1]/div[1]/span[1]/lightning-input[1]/lightning-primitive-input-simple[1]/div[1]/div[1]/input[1]")).sendKeys(prodNameToBeGivenInSearchField + " "+ strengthToBeGivenInSearchField);
        }else{
            driver.findElement(By.xpath("/html[1]/body[1]/div[1]/header[1]/div[1]/div[1]/div[1]/div[4]/div[1]/div[1]/div[1]/div[1]/span[1]/lightning-input[1]/lightning-primitive-input-simple[1]/div[1]/div[1]/input[1]")).sendKeys(prodNameToBeGivenInSearchField );
        }

        //Thread.sleep(3000);
        driver.findElement(By.xpath("/html[1]/body[1]/div[1]/header[1]/div[1]/div[1]/div[1]/div[4]/div[1]/div[1]/div[1]/div[1]/span[1]/lightning-input[1]/lightning-primitive-input-simple[1]/div[1]/div[1]/input[1]"))
                .sendKeys( Keys.RETURN);
        Thread.sleep(5000);

        List<LookupResult> lookupResultList = Collections.synchronizedList(new ArrayList<>());

        List<WebElement> numberOfLis = driver.findElements(By.xpath("/html[1]/body[1]/div[1]/div[2]/span[1]/div[1]/div[1]/div[2]/div[4]/div[1]/div[2]/div[2]/span"));
        for(int i=1; i<=numberOfLis.size();i++){
            try{
                ///
                //String descriptionFromWebsite = driver.findElement(By.xpath("/html[1]/body[1]/article[1]/div[1]/div[3]/div[1]/dl[1]/div["+i+"]/dt[1]")).getText();
                String descriptionFromWebsite = driver.findElement(By.xpath("html[1]/body[1]/div[1]/div[2]/span[1]/div[1]/div[1]/div[2]/div[4]/div[1]/div[2]/div[2]/span["+i+"]/div[1]/div[1]/div[1]/div[2]/div[1]/div[1]/span[1]/p[1]/a[1]")).getText();
                /*String packFromWebsite = driver.findElement(By.xpath("html[1]/body[1]/div[1]/div[2]/span[1]/div[1]/div[1]/div[2]/div[4]/div[1]/div[2]/div[2]/span["+i+"]/div[1]/div[1]/div[1]/div[2]/div[1]/div[1]/span[1]/p[1]/a[1]")).getText();
                String strengthFromWebsite = driver.findElement(By.xpath("html[1]/body[1]/div[1]/div[2]/span[1]/div[1]/div[1]/div[2]/div[4]/div[1]/div[2]/div[2]/span["+i+"]/div[1]/div[1]/div[1]/div[2]/div[1]/div[1]/span[1]/p[1]/a[1]")).getText();
                */String priceFromWebsite = driver.findElement(By.xpath("/html[1]/body[1]/div[1]/div[2]/span[1]/div[1]/div[1]/div[2]/div[4]/div[1]/div[2]/div[2]/span["+i+"]/div[1]/div[1]/div[1]/div[2]/div[1]/div[3]/span[1]/div[1]/div[1]/span[1]")).getText();
                String availabilityFromWebsite = driver.findElement(By.xpath("/html[1]/body[1]/div[1]/div[2]/span[1]/div[1]/div[1]/div[2]/div[4]/div[1]/div[2]/div[2]/span["+i+"]/div[1]/div[1]/div[1]/div[2]/div[1]/div[2]/div[1]/div[2]")).getText();

                lookupResultList.add(LookupResult.builder().description(descriptionFromWebsite.toLowerCase()).priceString(priceFromWebsite.toLowerCase()).available(availabilityFromWebsite).build());

            }catch (Exception e){
                System.out.println("Trident exception is::::::"+e.getMessage());
                e.printStackTrace();
                Thread.sleep(1000);
            }
        }

        return lookupResultList;
    }
}