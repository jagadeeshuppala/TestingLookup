package service;

import model.LookupResult;
import model.LookupResultOptions;
import model.Product;
import io.github.bonigarcia.wdm.WebDriverManager;
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
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BnS implements Callable<Map<Integer, LookupResultOptions>> {

    private String fileName;
    private boolean topup;
    Map<Integer, LookupResultOptions> concurrentHashMap = new ConcurrentHashMap<>();

    public BnS(String fileName, boolean topup){
        this.fileName = fileName;
        this.topup = topup;
    }

    /*public Map<Integer, LookupResult> getConcurrentHashMap(){
        return this.concurrentHashMap;
    }*/






    @Override
    public Map<Integer, LookupResultOptions> call() throws Exception {
        WebDriverManager.chromedriver().clearDriverCache().setup();
        WebDriverManager.chromedriver().clearResolutionCache().setup();
        WebDriver driver = new ChromeDriver();
        driver.get("https://www.bnsgroup.co.uk/login.do");

        Thread.sleep(1000);




        try{
            //driver.findElement(By.id("userName")).sendKeys("bridgwater.pharmacy@nhs.net");
            driver.findElement(By.xpath("/html[1]/body[1]/main[1]/div[1]/div[2]/div[1]/div[1]/div[1]/div[1]/div[2]/form[1]/div[2]/input[1]")).sendKeys("bridgwater.pharmacy@nhs.net");
            Thread.sleep(1000);
            //driver.findElement(By.id("pass")).sendKeys("Bridg@8486");
            driver.findElement(By.xpath("/html[1]/body[1]/main[1]/div[1]/div[2]/div[1]/div[1]/div[1]/div[1]/div[2]/form[1]/div[3]/input[1]")).sendKeys("Bridg@8486");
            Thread.sleep(1000);
            driver.findElement(By.xpath("/html[1]/body[1]/main[1]/div[1]/div[2]/div[1]/div[1]/div[1]/div[1]/div[2]/form[1]/div[4]/button[1]"))
                    .sendKeys(Keys.RETURN);
            Thread.sleep(1000);
            driver.findElement(By.xpath("/html[1]/body[1]/strong[1]/div[2]/div[2]/div[1]/div[1]/input[1]")).clear();
        }catch (Exception e){
            e.printStackTrace();
            //if there is any exception, that means website is not allowing. so no point in continuing. so return the concurrent hashmap
            System.out.println("Looks like BnS is not allowing software to login to website");
            driver.close();
            driver.quit();
            return concurrentHashMap;
        }

        FileInputStream file = new FileInputStream(fileName);
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);
        int productNameColumnNumber = 0;
        int strengthColumnNumber = 1;
        int packSizeColumnNumber = 2;
        int quantityColumnNumber = 3;
        int notesColumnNumber = 5;
        int fromColumnNumber = 4;

        List<Product> productNames = Collections.synchronizedList(new ArrayList<>());
        for (int i = 1; i <= sheet.getLastRowNum() && sheet.getRow(i) != null && sheet.getRow(i).getCell(productNameColumnNumber) != null; i++) {
            if(topup){
                if (sheet.getRow(i).getCell(quantityColumnNumber).getCellType() != CellType.BLANK
                        && !sheet.getRow(i).getCell(quantityColumnNumber).toString().trim().equals("")
                        && sheet.getRow(i).getCell(fromColumnNumber).getCellType() == CellType.BLANK
                ) {


                    String productName = sheet.getRow(i).getCell(productNameColumnNumber) != null ? new DataFormatter().formatCellValue(sheet.getRow(i).getCell(productNameColumnNumber)).toLowerCase() : null;
                    String strenth = sheet.getRow(i).getCell(productNameColumnNumber) != null ? new DataFormatter().formatCellValue(sheet.getRow(i).getCell(strengthColumnNumber)).toLowerCase() : null;
                    String packsize = sheet.getRow(i).getCell(productNameColumnNumber) != null ? new DataFormatter().formatCellValue(sheet.getRow(i).getCell(packSizeColumnNumber)).toLowerCase() : null;
                    productNames.add(Product.builder().productName(productName).strength(strenth).packsize(packsize).productNameUnmodified(productName)
                            .rowNumber(i).build());
                }
            }else{
                if (sheet.getRow(i).getCell(quantityColumnNumber).getCellType() != CellType.BLANK
                        && !sheet.getRow(i).getCell(quantityColumnNumber).toString().trim().equals("")
                ) {


                    String productName = sheet.getRow(i).getCell(productNameColumnNumber) != null ? new DataFormatter().formatCellValue(sheet.getRow(i).getCell(productNameColumnNumber)).toLowerCase() : null;
                    String strenth = sheet.getRow(i).getCell(productNameColumnNumber) != null ? new DataFormatter().formatCellValue(sheet.getRow(i).getCell(strengthColumnNumber)).toLowerCase() : null;
                    String packsize = sheet.getRow(i).getCell(productNameColumnNumber) != null ? new DataFormatter().formatCellValue(sheet.getRow(i).getCell(packSizeColumnNumber)).toLowerCase() : null;
                    productNames.add(Product.builder().productName(productName).strength(strenth).packsize(packsize).productNameUnmodified(productName)
                            .rowNumber(i).build());
                }
            }

        }


        //for(Product product : productNames){
        for(int i=0;i<productNames.size();i++){
            System.out.println("Bns Still "+ (productNames.size() - i)+" more to go");
            Product product = productNames.get(i);
            System.out.println("Bns Product:"+product.getProductName()+" Strength:"+product.getStrength() + " PackSize:"+ product.getPacksize());
            overrideProductBeforeEvenSearch(product);

            try{

                List<LookupResult> lookupResultList = lookupResults(driver, product.getProductName(), product.getStrength());
                System.out.println("Bns Result list from website");
                print(lookupResultList);

                List<LookupResult> matchedWithProdNameAndStrengthAndPackSize =  lookupResultList.stream()
                        .filter(websiteDesc -> Util.websiteDescContainsProductName(websiteDesc.getDescription().toLowerCase(),
                                product.getProductName().toLowerCase().replaceAll("\\+","+ ") ))
                        .filter(websiteDesc -> Util.websiteDescContainsStrength(websiteDesc.getDescription().toLowerCase(), product.getStrength().toLowerCase()))
                        .filter(websiteDesc -> Util.websiteDescContainsPacksize(websiteDesc.getDescription().toLowerCase(), product.getPacksize().toLowerCase()))
                        .filter(websiteDesc -> specialConsiderationOfProductResultsFromWebsite(websiteDesc.getDescription().toLowerCase(),
                                product))
                        //.collect(Collectors.toList());
                        .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
                concurrentHashMap.put(product.getRowNumber(), Util.getCheapestOption(matchedWithProdNameAndStrengthAndPackSize));

                List<LookupResult> matchedWithProdNameAndStrength = Collections.synchronizedList(new ArrayList<>());
                if(matchedWithProdNameAndStrengthAndPackSize.isEmpty()){
                    matchedWithProdNameAndStrength =  lookupResultList.stream()
                            .filter(websiteDesc -> websiteDescContainsProductName(websiteDesc.getDescription().toLowerCase(),
                                    product.getProductName().toLowerCase().replaceAll("\\+"," ") ))
                            .filter(websiteDesc -> websiteDescContainsStrength(websiteDesc.getDescription().toLowerCase(), product.getStrength().toLowerCase()))
                            .filter(websiteDesc -> specialConsiderationOfProductResultsFromWebsite(websiteDesc.getDescription().toLowerCase(),
                                    product))
                            //.collect(Collectors.toList());
                            .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
                }

                System.out.println("matched result with desc, strength, packsize");
                print(matchedWithProdNameAndStrengthAndPackSize);
                if(!matchedWithProdNameAndStrength.isEmpty()){
                    System.out.println("tried matched result with desc, strength and without packsize");
                    print(matchedWithProdNameAndStrength);
                }


                System.out.println("--------------------------------------------------------");
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        driver.close();
        driver.quit();
        file.close();

        return concurrentHashMap;
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

        if(product.getProductName().contains("estriol crm with applicator 80g")){
            product.setProductName("estriol crm 80g");
        }else if(product.getProductName().contains("ferrous fumarate tabs (un-lic)")){
            product.setProductName("ferrous fumarate");
        }else if(product.getProductName().contains("metformin sr generic")){
            product.setProductName("metformin sr");
        }else if(product.getProductName().contains("perindopril glen,tev,sand")){
            product.setProductName("perindopril");
        }else if(product.getProductName().contains("salbutamol inhaler (salamol) ivax")){
            product.setProductName("salbutamol");
        }/*else if(product.getProductName().contains("tamsulosin mr caps")){
            product.setProductName("tamsulosin caps");
        }*/else if(product.getProductName().contains("vipidia tabs alogliptin")){
            product.setProductName("vipidia tabs");
        }else if(product.getProductName().contains("aciclovir tabs disp act,acc only")){
            product.setProductName("aciclovir tabs disp");
        }else if(product.getProductName().contains("acidex advance anis (gaviscon adv")){
            product.setProductName("acidex advance anis");
        }else if(product.getProductName().contains("acidex advance pepp (gaviscon adv")){
            product.setProductName("acidex advance pepp");
        }else if(product.getProductName().contains("acidex standard anis (gaviscon orig)")){
            product.setProductName("acidex standard anis");
        }else if(product.getProductName().contains("acidex standard pep (gaviscon orig)")){
            product.setProductName("acidex standard pep");
        }else if(product.getProductName().contains("alendronic once weekly tabs")){
            product.setProductName("alendronic tabs");
        }else if(product.getProductName().contains("algivon honey alginate dres cr3659")){
            product.setProductName("algivon cr3659");
        }else if(product.getProductName().contains("algivon honey alginate dres cr3831")){
            product.setProductName("algivon cr3831");
        }else if(product.getProductName().contains("allevyn adhesive dressing")){
            product.setProductName("allevyn adhesive");
        }else if(product.getProductName().contains("allevyn ag adh dressing")){
            product.setProductName("allevyn ag adh");
        }else if(product.getProductName().contains("allevyn ag gentle border p3467685")){
            product.setProductName("allevyn ag gentle border");
        }else if(product.getProductName().contains("allevyn non-adh dressing" )){
            product.setProductName("allevyn non-adh");
        }else if(product.getProductName().contains("allevyn gentle border pip 3342060" )){
            product.setProductName("allevyn gentle border");
        }else if(product.getProductName().contains("amoxicillin s/f sac longdate only" )){
            product.setProductName("amoxicillin s/f sac");
        }else if(product.getProductName().contains("aquacel ag dressing (s7505)" )){
            product.setProductName("aquacel ag");
        }else if(product.getProductName().contains("aquacel ag dressing (s7506)" )){
            product.setProductName("aquacel ag");
        }else if(product.getProductName().contains("aquacel ag extra dressing" )){
            product.setProductName("aquacel ag extra");
        }else if(product.getProductName().contains("aquacel ag ribbon 420128" )){
            product.setProductName("aquacel ag ribbon");
        }else if(product.getProductName().contains("aquacel ag ribbon s7509ag" )){
            product.setProductName("aquacel ag ribbon");
        }else if(product.getProductName().contains("aquacel ag+ extra pip 3862703" )){
            product.setProductName("aquacel ag+ extra");
        }else if(product.getProductName().contains("aquacel ag+ extra pip 3862711" )){
            product.setProductName("aquacel ag+ extra");
        }else if(product.getProductName().contains("aquacel ag+ ribbon pip" )){
            product.setProductName("aquacel ag+ ribbon");
        }else if(product.getProductName().contains("aquacel ag+ ribbon pip 3862729" )){
            product.setProductName("aquacel ag+ ribbon");
        }else if(product.getProductName().contains("aquacel dressing (s7500)" )){
            product.setProductName("aquacel s7500");
        }else if(product.getProductName().contains("aquacel dressing (s7501)" )){
            product.setProductName("aquacel s7501");
        }else if(product.getProductName().contains("aquacel extra dressing" )){
            product.setProductName("aquacel extra");
        }else if(product.getProductName().contains("aquacel foam adhesive 420680" )){
            product.setProductName("aquacel foam adhesive");
        }else if(product.getProductName().contains("aquacel foam adhesive p3950581" )){
            product.setProductName("aquacel foam adhesive");
        }else if(product.getProductName().contains("aquacel ribbon 420127 p3615978" )){
            product.setProductName("aquacel ribbon");
        }else if(product.getProductName().contains("aquacel ribbon s7503 p8420093" )){
            product.setProductName("aquacel ribbon");
        }else if(product.getProductName().contains("atovaquone/proguanil" ) && product.getStrength().equals("250/100")){
            product.setProductName("atovaquone proguanil");
            product.setStrength("250mg/100mg");
        }else if(product.getProductName().contains("atovaquone/proguanil" )){
            product.setProductName("atovaquone proguanil");
        }else if(product.getProductName().contains("atrauman ag dressing" )){
            product.setProductName("atrauman ag");
        }else if(product.getProductName().contains("atrauman dressing" )){
            product.setProductName("atrauman");
        }else if(product.getProductName().contains("atrauman dressing nwos = singles" )){
            product.setProductName("atrauman");
        }else if(product.getProductName().contains("atrauman tulle dressing" )){
            product.setProductName("atrauman tulle");
        }else if(product.getProductName().contains("betmiga mr tabs allianc 26.88 jan23" )){
            product.setProductName("betmiga mr tabs");
        }else if(product.getProductName().contains("non adh" )){
            product.setProductName("n-adh");
        }else if(product.getProductName().contains("biatain silicone dressing" )){
            product.setProductName("biatain silicone");
        }else if(product.getProductName().contains("aacetazolamide tabs" )){
            product.setProductName("acetazolamide tabs");
        }else if(product.getProductName().contains("aciclovir cream (gsl)" )){
            product.setProductName("aciclovir cream");
        }else if(product.getProductName().contains("aciclovir cream" )){
            product.setProductName("aciclovir cream");
        }else if(product.getProductName().contains("amorolfine nail lacq rx=6ml j gluck" )){
            product.setProductName("amorolfine nail lacq");
        }







        if(product.getProductName().contains("non-adhesive")){
            product.setProductName(product.getProductName().replaceAll("non-adhesive","non adhesive"));
        }else if(product.getProductName().contains("ag+extra")){
            product.setProductName(product.getProductName().replaceAll("ag+extra","ag+ extra"));
        }else if(product.getProductName().contains("bard flip-flo")){
            product.setProductName(product.getProductName().replaceAll("bard flip-flo", "flipflo"));
        }else if(product.getProductName().contains("b-d") && product.getStrength().equals("31g/5mm")){
            product.setProductName(product.getProductName().replaceAll("b-d", "bd   "));
            product.setStrength(product.getStrength().replaceAll("31g/5mm", "5mm"));
        }else if(product.getProductName().contains("b-d") && product.getStrength().equals("31g/8mm")){
            product.setProductName(product.getProductName().replaceAll("b-d", "bd   "));
            product.setStrength(product.getStrength().replaceAll("31g/8mm", "8mm"));
        }else if(product.getProductName().contains("b-d pen needles sep23 6in stock") && product.getStrength().equals("32g/4mm")){
            product.setProductName(product.getProductName().replaceAll("b-d pen needles sep23 6in stock", "bd    pen needles"));
            product.setStrength(product.getStrength().replaceAll("31g/8mm", "8mm"));
        }else if(product.getProductName().contains("b-d")){
            product.setProductName(product.getProductName().replaceAll("b-d pen needles", "bd   pen needles"));
        }else  if(product.getProductName().contains("bd u100 syr.0.5ml 29g 324824 mf+")){
            product.setProductName(product.getProductName().replaceAll("bd u100 syr.0.5ml 29g 324824 mf+", "bd syr"));
        }else  if(product.getProductName().contains("beclomethasone aq sp only") && product.getStrength().equals("05%") ){
            product.setProductName(product.getProductName().replaceAll("beclomethasone aq sp only", "beclometasone  sp"));
            product.setStrength(product.getStrength().replaceAll("05%", "50"));
        }else  if(product.getProductName().contains("beclomethasone aqu spr ") && product.getStrength().equals("05%") ){
            product.setProductName(product.getProductName().replaceAll("beclomethasone aqu spr", "beclometasone  spr"));
            product.setStrength(product.getStrength().replaceAll("05%", "50"));
        }else  if(product.getProductName().contains("benylin drowsy orig chesty")){
            product.setProductName(product.getProductName().replaceAll("benylin drowsy orig chesty", "benylin drowsy chesty"));
        }else  if(product.getProductName().contains("betamethasone") && product.getPacksize().equals("30gm")){
            product.setPacksize("30g");
        }else  if(product.getProductName().contains("betamethasone") && product.getPacksize().equals("100gm")){
            product.setPacksize("100g");
        }else if(product.getProductName().contains("bimatoprost eye drops") && product.getStrength().equals("0.10%")){
            product.setProductName("bimatoprost eye drops");
            product.setStrength("100mcg");
        }else if(product.getProductName().contains("bimatoprost timolol ed") && product.getStrength().equals("0.3/5mg")){
            product.setProductName("bimatoprost timolol ed");
            product.setStrength("300mcgs/5mg");
        }else if(product.getProductName().contains("brimonidine eye drops") && product.getStrength().equals("0.2%")){
            product.setProductName("brimonidine eye drops");
            product.setStrength("2mg/ml");
        }else if(product.getProductName().contains("brimonidine timolol combigan ed")){
            product.setProductName("brimonidine timolol ed");
        }else if(product.getProductName().contains("brintelix tab vortioxetine")){
            product.setProductName("brintellix tab");
        }else if(product.getProductName().contains("binosto eff tab (alendronic)")){
            product.setProductName("binosto eff tab");
        }else if(product.getProductName().contains("briviact tabs (brivaracetam)")){
            product.setProductName("briviact tabs");
        }else if(product.getProductName().contains("bromocriptine tabs")){
            product.setProductName("bromocriptine tabs");
        }else if(product.getProductName().contains("buccolam 10mg/2ml syr") ){
            product.setProductName("buccolam syringes");
        }else if(product.getProductName().contains("buccolam 5mg/ml syr") && product.getStrength().equals("5mg/ml")){
            product.setProductName("buccolam syringes");
            product.setStrength("5mg/1ml");
        }else if(product.getProductName().contains("budesonide ns")){
            product.setProductName("budesonide ns");
        }else if(product.getProductName().contains("buprenorphine patch")){
            product.setProductName("buprenorphine patch");
        }else if(product.getProductName().contains("buprenorphine tabs")){
            product.setProductName("buprenorphine tabs");
        }else if(product.getProductName().contains("buscopan tabs")){
            product.setProductName("buscopan tabs");
        }else if(product.getProductName().contains("cabergoline tab") && product.getStrength().contains("500mcg")){
            product.setProductName("cabergoline tab");
            product.setStrength("0.5mg");
        }else if(product.getProductName().contains("calpol infant under 6 sf") && product.getStrength().equals("120")){
            product.setProductName("calpol infant sf");
            product.setStrength("120mg");
        }else if(product.getProductName().contains("calpol infant under 6 sf")) {
            product.setProductName("calpol infant sf");
        }else if(product.getProductName().contains("candesartan tabs")){
            product.setProductName("candesartan tabs");
        }else if(product.getProductName().contains("canesten clotrimazole cream thrush")){
            product.setProductName("canesten cream thrush");
        }else if(product.getProductName().contains("canesten combi  pess/crm")){
            product.setProductName("canesten combi pess");
        }else if(product.getProductName().contains("carbimazole-longlife")){
            product.setProductName("carbimazole");
        }else if(product.getProductName().contains("carbocisteine solution") && product.getStrength().equals("750/5")){
            product.setProductName("carbocisteine syrup");
            product.setStrength("750mg/5ml");
        }else if(product.getProductName().contains("carbocisteine solution")){
            product.setProductName("carbocisteine syrup");
        }else if(product.getProductName().contains("carmellose eye drops pf 0.4ml") ){
            product.setProductName("carmellose eye drops 0.4ml");
        }else if(product.getProductName().contains("carmellosepf cellus,evolv,ocu-lub,pfdr,vizc")){
            product.setProductName("carmellose");
        }else if(product.getProductName().contains("cavilon barrier cream")){
            product.setProductName("cavilon barrier cream");
        }else if(product.getProductName().contains("cavilon barrier wipes")){
            product.setProductName("cavilon barrier wipes");
        }else if(product.getProductName().contains("celluvisc eye drops (carmellose)") && product.getStrength().equals("0.50%")){
            product.setProductName("celluvisc eye drops");
            product.setStrength("0.5%");
        }else if(product.getProductName().contains("celluvisc eye drops (carmellose)")){
            product.setProductName("celluvisc eye drops");
        }else if(product.getProductName().contains("cetirizine solution benadryl")){
            product.setProductName("cetirizine solution");
        }else if(product.getProductName().contains("cetraban")){
            product.setProductName(product.getProductName().replaceAll("cetraban","cetraben"));
        }else if(product.getProductName().contains("cetraben emollient cr agcy yes")){
            product.setProductName("cetraben emollient cr");
        }/*else if(product.getProductName().contains("chloramphenicol eye oint pom")){
            product.setProductName("chloramphenicol eye oint");
        }else if(product.getProductName().contains("chloramphenicol eye oint p otc")){
            product.setProductName("chloramphenicol eye oint");
        }*/else if(product.getProductName().contains("chlorpheniramine elxir")){
            product.setProductName("chlorphenamine");
        }else if(product.getProductName().contains("chlorpheniramine tabs")){
            product.setProductName("chlorphenamine tabs");
        }else if(product.getProductName().contains("chlorpromazine oral sol")){
            product.setProductName("chlorpromazine oral syrup");
        }else if(product.getProductName().contains("ciclosporin caps (deximune)")){
            product.setProductName("deximune caps");
        }else if(product.getProductName().contains("ciclosporin:capim,capsor,dexim,vanq")){
            product.setProductName("deximune caps");
        }else if(product.getProductName().contains("ciloxan (ciprofloxacin) eye drops")){
            product.setProductName("ciloxan eye drops");
        }else if(product.getProductName().contains("circadin tab is rx generic? (cheaper)")){
            product.setProductName("circadin pr tab");
        }else if(product.getProductName().contains("clarithromycin xl tabs")){
            product.setProductName("clarithromycin xl tabs");
        }else if(product.getProductName().contains("clobetasol/clobaderm (dermov) crm")){
            product.setProductName("clobetasol cream");
        }else if(product.getProductName().contains("clobetasol/clobaderm (dermov) oint")){
            product.setProductName("clobetasol oint");
        }else if(product.getProductName().contains("clotrimazole vag tabs pessary")){
            product.setProductName("clotrimazole pessary");
        }else if(product.getProductName().contains("co-amilofruse ls tabs")){
            product.setProductName("co-amilofruse tabs");
        }else if(product.getProductName().contains("co-careldopa")){
            product.setProductName("co-careldopa");
        }else if(product.getProductName().contains("co-codamol capsules")){
            product.setProductName("co-codamol capsules");
        }else if(product.getProductName().contains("co-codamol tabs oval")){
            product.setProductName("co-codamol caplets");
        }else if(product.getProductName().contains("colecalciferol tabs")){
            product.setProductName("colecalciferol tabs");
        }else if(product.getProductName().contains("colestyramine 4g sf sachet (light)")){
            product.setProductName("colestyramine 4g sf sachet");
        }else if(product.getProductName().contains("comfifast yellow 10.75cm p8134249")){
            product.setProductName("comfifast yellow 10.75cm");
        }else if(product.getProductName().contains("covonia night time formula mix")){
            product.setProductName("covonia night time");
        }else if(product.getProductName().contains("debrisoft 10cmx10cm")){
            product.setProductName("debrisoft");
        }else if(product.getProductName().contains("depo-medrone")){
            product.setProductName("depomedrone");
        }else if(product.getProductName().contains("dermovate cream (clobetasol)")){
            product.setProductName("dermovate cream");
        }else if(product.getProductName().contains("dermovate oint (clobetasol)")){
            product.setProductName("dermovate oint");
        }else if(product.getProductName().contains("dermovate scalp application") && product.getStrength().equals("0.05")){
            product.setProductName("dermovate scalp application");
            product.setStrength("0.05%");
        }else if(product.getProductName().contains("desmopressin tabs") && product.getStrength().equals("100mcg")){
            product.setProductName("desmopressin tabs");
            product.setStrength("0.1mg");
        }else if(product.getProductName().contains("desmopressin tabs") && product.getStrength().equals("200mcg")){
            product.setProductName("desmopressin tabs");
            product.setStrength("0.2mg");
        }else if(product.getProductName().contains("dicycloverine oral sol")){
            product.setProductName("dicycloverine");
        }else if(product.getProductName().contains("diltiazem mr tildiemoct23 stock80")){
            product.setProductName("diltiazem mr");
        }else if(product.getProductName().contains("dioralyte sachet plain/natural")){
            product.setProductName("dioralyte sachet natural");
        }else if(product.getProductName().contains("domperidone oral susp")){
            product.setProductName("domperidone susp");
        }else if(product.getProductName().contains("dorzolamide eye drop") && product.getStrength().equals("2%")){
            product.setProductName("dorzolamide eye drop");
            product.setStrength("20mg/ml");
        }else if(product.getProductName().contains("dorzolamide/timolol eye drop") && product.getStrength().equals("2%/0.2%")){
            product.setProductName("dorzolamide/timolol eye drop");
            product.setStrength("20mg/5mg");
        }else if(product.getProductName().contains("dovobet gel generic cheaper?")){
            product.setProductName("dovobet gel");
        }else if(product.getProductName().contains("doxazosin tabs xl")){
            product.setProductName("doxazosin tabs xl");
        }else if(product.getProductName().contains("duoderm extra thin")){
            product.setProductName("duoderm extra thin");
        }else if(product.getProductName().contains("debrisoft lolly p3985124")){
            product.setProductName("debrisoft lolly");
        }else if(product.getProductName().contains("dexamethasone tabs soluble")){
            product.setProductName("dexamethasone tabs soluble");
        }else if(product.getProductName().contains("easifix bandage") && product.getStrength().equals("10cm10cmx4m4m")){
            product.setProductName("easifix 10cm x 4m");
            product.setStrength("");
        }else if(product.getProductName().contains("easifix bandage") && product.getStrength().equals("7.5x4m")){
            product.setProductName("easifix 7.5cm x 4m");
            product.setStrength("");
        }else if(product.getProductName().contains("easifix or k-band bandage") && product.getStrength().equals("5cmx4m")){
            product.setProductName("easifix 5cm x 4m");
            product.setStrength("");
        }else if(product.getProductName().contains("eklira aclidinium 60 dose") && product.getStrength().equals("322mg")){
            product.setProductName("eklira 60 dose");
            product.setStrength("322mcg");
        }else if(product.getProductName().contains("eliquis tab now generic") ){
            product.setProductName("eliquis tab");
        }else if(product.getProductName().contains("emulsifying oint bp") ){
            product.setProductName("emulsifying oint");
        }else if(product.getProductName().contains("ensure plus advance banana")){
            product.setProductName("ensure plus advance banana");
            product.setStrength("");
        }else if(product.getProductName().contains("ensure plus banana")){
            product.setProductName("ensure plus banana");
            product.setStrength("");
        }else if(product.getProductName().contains("ensure plus juce apple")){
            product.setProductName("ensure plus apple");
            product.setStrength("");
        }else if(product.getProductName().contains("ensure plus juce fruit punch")){
            product.setProductName("ensure plus fruit punch");
            product.setStrength("");
        }else if(product.getProductName().contains("ensure plus juce orange")){
            product.setProductName("ensure plus orange");
            product.setStrength("");
        }else if(product.getProductName().contains("eropid (viagra connect generic)")){
            product.setProductName("eropid");
        }else if(product.getProductName().contains("erythromycin susp (s/free)")){
            product.setProductName("erythromycin susp (s/f)");
        }else if(product.getProductName().contains("estriol crm with applicator 80g")){
            product.setProductName("estriol crm 80g");
        }else if(product.getProductName().contains("exemestane tabs")){
            product.setProductName("exemestane");
        }else if(product.getProductName().contains("fentanyl matrifen patch")){
            product.setProductName("fentanyl patch");
        }else if(product.getProductName().contains("fentanyl patch generic") && product.getStrength().equals("12mg")){
            product.setProductName("fentanyl patch");
            product.setStrength("12mcg");
        }else if(product.getProductName().contains("fentanyl yem(san), vict(acc), osm(zen)")){
            product.setProductName("fentanyl patch");
        }else if(product.getProductName().contains("ferrous fumarate oral soln") && product.getStrength().equals("140mg")){
            product.setProductName("ferrous fum oral sol");
            product.setStrength("140");
        }else if(product.getProductName().contains("ferrous gluconate")){
            product.setProductName("ferrous gluc");
        }else if(product.getProductName().contains("folic acid oral sf (0.5ml / 1ml)")){
            product.setProductName("folic acid sf");
        }else if(product.getProductName().contains("fortijuce bottle blackcurrant")){
            product.setProductName("fortijuce blackcurrant");
        }else if(product.getProductName().contains("fortijuce liquid forest fruits")){
            product.setProductName("fortijuice fruit of forest");
        }else if(product.getProductName().contains("fortijuce liquid tropical")){
            product.setProductName("fortijuce tropical");
        }else if(product.getProductName().contains("fortijuice liquid apple")){
            product.setProductName("fortijuice apple");
        }else if(product.getProductName().contains("fortisip compact liquid")){
            product.setProductName("fortisip compact");
        }else if(product.getProductName().contains("fresubin protien energy strawb")){
            product.setProductName("fresubin protein energy w/straw");
        }else if(product.getProductName().contains("ganfort eye drop sol zd")){
            product.setProductName("ganfort eye drop sol");
        }else if(product.getProductName().contains("gauze swab 4ply non woven")){
            product.setProductName("gauze swab 4ply");
        }else if(product.getProductName().contains("gauze swab 8ply type13 bp1988")){
            product.setProductName("gauze swab 8ply");
        }else if(product.getProductName().contains("gauze topper sterile swabs")){
            product.setProductName("gauze swab");
        }else if(product.getProductName().contains("hydrocortisone orom buccal tab")){
            product.setProductName("hydrocortisone buccal tab");
        }else if(product.getProductName().contains("hylo night (vita-pos) eye oint")){
            product.setProductName("hylonight eye oint");
        }else if(product.getProductName().contains("hylo-care eye drops")){
            product.setProductName("hylo care eye drops");
        }else if(product.getProductName().contains("hylo-forte eye drops")){
            product.setProductName("hylo forte eye drops");
        }else if(product.getProductName().contains("hylo-tear eye drops")){
            product.setProductName("hylo tear eye drops");
        }else if(product.getProductName().contains("hypromellose eye drops") && product.getStrength().equals("0.30%")){
            product.setProductName("hylo tear eye drops");
            product.setStrength("0.3%");
        }else if(product.getProductName().contains("ibuprofen susp") && product.getStrength().equals("100mg/5")){
            product.setProductName("ibuprofen susp");
            product.setStrength("100mg/5ml");
        }else if(product.getProductName().contains("ibuprofen/codeine tabs")){
            product.setProductName("ibuprofen tabs");
        }else if(product.getProductName().contains("imiquimod crm sachets")){
            product.setProductName("imiquimod sachets");
        }else if(product.getProductName().contains("instillagel pre-filled syr (uk)")){
            product.setProductName("instillagel pre-filled syr");
        }else if(product.getProductName().contains("invokana canaglifozin")){
            product.setProductName("invokana");
        }else if(product.getProductName().contains("iodoflex paste dressing 4x6cm")){
            product.setProductName("iodoflex paste");
        }else if(product.getProductName().contains("ipratropium nebuliser sol")){
            product.setProductName("ipratropium neb");
        }else if(product.getProductName().contains("isotard xl tab")&& product.getStrength().equals("60mg")){
            product.setProductName("isotard 60xl tab");
            product.setStrength("");
        }else if(product.getProductName().contains("ivabradine tabs (procoralan)")){
            product.setProductName("ivabradine tabs");
        }else if(product.getProductName().contains("jardiance tabs empaglifozin")){
            product.setProductName("jardiance tabs");
        }else if(product.getProductName().contains("kaltostat cavity 2g dressing")){
            product.setProductName("kaltostat cavity 2g");
        }else if(product.getProductName().contains("kaltostat dress")){
            product.setProductName("kaltostat dressing");
        }else if(product.getProductName().contains("kerramaxcare dressing") && product.getStrength().equals("13.5x15.5")){
            product.setProductName("kerramax care 13.5cm x 15.5cm");
            product.setStrength("");
        }else if(product.getProductName().contains("kerramaxcare dressing") && product.getStrength().equals("20x30cm")){
            product.setProductName("kerramax care 20cm x 30cm");
            product.setStrength("");
        }else if(product.getProductName().contains("kerramaxcare dressing") && product.getStrength().equals("10x10cm")){
            product.setProductName("kerramax care 10cm x 10cm");
            product.setStrength("");
        }else if(product.getProductName().contains("kerramaxcare dressing") && product.getStrength().equals("10x22cm")){
            product.setProductName("kerramax care 10cm x 22cm");
            product.setStrength("");
        }else if(product.getProductName().contains("kerramaxcare dressing") && product.getStrength().equals("20x22cm")){
            product.setProductName("kerramax care 20cm x 22cm");
            product.setStrength("");
        }else if(product.getProductName().contains("kerramaxcare dressing") && product.getStrength().equals("20x50cm")){
            product.setProductName("kerramax care 20cm x 50cm");
            product.setStrength("");
        }else if(product.getProductName().contains("kerraped boot small")){
            product.setProductName("kerraped small");
        }else if(product.getProductName().contains("levonorgestrel tab generic")){
            product.setProductName("levonorgestrel tab p");
        }else if(product.getProductName().contains("lipitor tabs uk only")){
            product.setProductName("lipitor tabs");
        }else if(product.getProductName().contains("lumigan eye") && product.getStrength().equals("0.10%")){
            product.setProductName("lumigan eye");
            product.setStrength("0.1mg/ml");
        }else if(product.getProductName().contains("mebeverine tabs s/c only")){
            product.setProductName("mebeverine tabs s/c");
        }else if(product.getProductName().contains("mepilex border dressing")){
            product.setProductName("mepilex border");
        }else if(product.getProductName().contains("mepilex border dres now comfort")){
            product.setProductName("mepilex border");
        }else if(product.getProductName().contains("mepilex border dressing")){
            product.setProductName("mepilex border");
        }else if(product.getProductName().contains("mepilex border lite")){
            product.setProductName("mepilex border lite");
        }else if(product.getProductName().contains("mepilex xt dressing")){
            product.setProductName("mepilex xt");
        }else if(product.getProductName().contains("mepore dressing")){
            product.setProductName("mepore dressing");
        }else if(product.getProductName().contains("methyldopa tab aldomet")){
            product.setProductName("methyldopa tab");
        }else if(product.getProductName().contains("metrogel gel (ardinrx=40g; rozexok)")){
            product.setProductName("metronidazole gel");
        }else if(product.getProductName().contains("micropore tape") && product.getStrength().equals("5cm")){
            product.setProductName("micropore");
            product.setStrength("5.0cm");
        }else if(product.getProductName().contains("micropore tape")){
            product.setProductName("micropore");
        }else if(product.getProductName().contains("migraleve ultra sumatriptan")){
            product.setProductName("migraleve sumatriptan");
        }else if(product.getProductName().contains("mometasone cream")){
            product.setProductName("mometasone cream");
            product.setStrength("");
        }else if(product.getProductName().contains("monuril fosfomycin sachet")){
            product.setProductName("monuril sachet");
        }else if(product.getProductName().contains("morphine sulphate solution")){
            product.setProductName("morphine sulphate oral solution");
        }else if(product.getProductName().contains("moxonidine tabs") && product.getStrength().equals("200mg")){
            product.setProductName("moxonidine tabs");
            product.setStrength("200mcg");
        }else if(product.getProductName().contains("mepilex border dressing")){
            product.setProductName("mepilex border");
        }else if(product.getProductName().contains("naramig tabs brand")){
            product.setProductName("naramig tabs");
        }else if(product.getProductName().contains("naratriptan tabs generic")){
            product.setProductName("naratriptan tabs");
        }else if(product.getProductName().contains("niquitin patch clear")){
            product.setProductName("niquitin patch clear");
        }else if(product.getProductName().contains("nitrofurantoin tabs")){
            product.setProductName("nitrofurantoin tabs");
        }else if(product.getProductName().contains("nizoral cream") && product.getStrength().equals("0.02")){
            product.setProductName("nizoral cream");
            product.setStrength("2%");
        }else if(product.getProductName().contains("norethisterone tabs utov, primolut")){
            product.setProductName("norethisterone tabs");
        }else if(product.getProductName().contains("novofine 30g needles agency yes")){
            product.setProductName("novofine 30g needles");
        }else if(product.getProductName().contains("novofine 31g needles nwos agy yes")){
            product.setProductName("novofine 31g needles");
        }else if(product.getProductName().contains("nystatin oral susp nystan = 1.80")){
            product.setProductName("nystatin oral susp");
        }else if(product.getProductName().contains("octasa mr tabs (mesalazine)")){
            product.setProductName("octasa mr tabs");
        }else if(product.getProductName().contains("permethrin")){
            product.setProductName(product.getProductName().replaceAll("permethrin", "permetherin"));
        }else if(product.getProductName().contains("pivmecillinam selexid")){
            product.setProductName("pivmecillinam");
        }else if(product.getProductName().contains("pregabalin 20mg/1ml solution")){
            product.setProductName("pregabalin 20mg/ml oral solution");
        }else if(product.getProductName().contains("prochlorperazine buccastem tab")){
            product.setProductName("prochlorperazine bucc tab");
        }else if(product.getProductName().contains("procyclidine tab kemadrine only")){
            product.setProductName("procyclidine tab");
        }else if(product.getProductName().contains("prograf caps")){
            product.setProductName("prograf caps");
        }else if(product.getProductName().contains("promethazine hcl tabs") || product.getProductName().equals("promethazine teoclate tabs")){
            product.setProductName("promethazine tabs");
        }else if(product.getProductName().contains("proshield foam+spr cleanser")){
            product.setProductName("proshield foam spr");
        }else if(product.getProductName().contains("proshield plus protect cr 8213")){
            product.setProductName("proshield plus protect");
        }else if(product.getProductName().contains("permethrin cream") && product.getStrength().equals("5.0%")){
            product.setProductName("permethrin cream");
            product.setStrength("5%");
        }else if(product.getProductName().contains("ranolazine tabs ranexa")){
            product.setProductName("ranolazine tabs");
        }else if(product.getProductName().contains("requip xl tabs gen islarge, 2*4 ok")){
            product.setProductName("requip xl tabs");
        }else if(product.getProductName().contains("resolor tabs prucalopride")){
            product.setProductName("resolor tabs");
        }else if(product.getProductName().contains("risperidone tabs (liq is cheaper)")){
            product.setProductName("risperidone tabs");
        }else if(product.getProductName().contains("rectogesic oint") && product.getStrength().equals("0.40%")){
            product.setProductName("rectogesic oint");
            product.setStrength("0.4%");
        }else if(product.getProductName().contains("scanpore tape")){
            product.setProductName("scanpor tape");
        }else if(product.getProductName().contains("senokot maxtab or generic")){
            product.setProductName("senokot max tab");
        }else if(product.getProductName().contains("seretide acc (fluticasone/salmet)")){
            product.setProductName("seretide acc");
        }else if(product.getProductName().contains("seretide evo (fluticasone/salmet)")){
            product.setProductName("seretide evo");
        }else if(product.getProductName().contains("serevent (salmet) accuhaler")){
            product.setProductName("serevent accuhaler");
        }else if(product.getProductName().contains("serevent (salmet) evohaler")){
            product.setProductName("serevent evohaler");
        }else if(product.getProductName().contains("sevelamer tab nov23: 380 in stock")){
            product.setProductName("sevelamer tab");
        }else if(product.getProductName().contains("simple linctus paed")){
            product.setProductName("simple linctus");
        }else if(product.getProductName().contains("sirdupla fluticasone/salmet mylan") && product.getStrength().equals("25/125")){
            product.setProductName("sirdupla evo");
            product.setStrength("25mcg/125mcg");
        }else if(product.getProductName().contains("sirdupla fluticasone/salmet mylan") && product.getStrength().equals("25/250")){
            product.setProductName("sirdupla evo");
            product.setStrength("25mcg/125mcg");
        }else if(product.getProductName().contains("sitagliptin januvia")){
            product.setProductName("sitagliptin tab");
        }else if(product.getProductName().contains("sodium feredetate sytron")){
            product.setProductName("sodium feredetate");
        }else if(product.getProductName().contains("sodium valp epil 2.31/30= 7.7/100")){
            product.setProductName("sodium valp");
        }else if(product.getProductName().contains("sominex promethazine tabs")){
            product.setProductName("sominex tabs");
        }else if(product.getProductName().contains("sodium valp epil 2.31/30= 7.7/100")){
            product.setProductName("sodium valp");
        }else if(product.getProductName().contains("tamoxifen tabs brand?")){
            product.setProductName("tamoxifen tabs");
        }else if(product.getProductName().contains("tamsulosin mr tabs flomaxtra")){
            product.setProductName("tamsulosin mr tabs");
        }else if(product.getProductName().contains("tegaderm +pad dressing")){
            product.setProductName("tegaderm plus pad");
        }else if(product.getProductName().contains("tegaderm film dressing")){
            product.setProductName("tegaderm film dressing");
        }else if(product.getProductName().contains("tegaderm foam adh") || product.getProductName().contains("tegaderm foamadh")){
            product.setProductName("tegaderm foam adh");
        }else if(product.getProductName().contains("tegretol pr tabs") ){
            product.setProductName("tegretol pr tabs");
        }else if(product.getProductName().contains("terbinafine hydrochloride cream") ){
            product.setProductName("terbinafine cream");
        }else if(product.getProductName().contains("tiopex unit dose eye gel 0.4g") ){
            product.setProductName("tiopex eye gel 0.4g");
        }else if(product.getProductName().contains("tiotropium tiogiva inh capsules") ){
            product.setProductName("tiotropium inh capsules");
        }else if(product.getProductName().contains("tobradex dexameth+tobramycin ed") ){
            product.setProductName("tobradex ed");
        }else if(product.getProductName().contains("tolterodine xl caps (neditol)") ){
            product.setProductName("tolterodine xl caps");
        }else if(product.getProductName().contains("tramadol sr cap not tabs") ){
            product.setProductName("tramadol sr cap");
        }else if(product.getProductName().contains("tramadol sr caps maxitram?") ){
            product.setProductName("tramadol sr caps");
        }else if(product.getProductName().contains("tramadol/paracetamol") && product.getStrength().equals("37.5/325") ){
            product.setProductName("tramadol/paracetamol");
            product.setStrength("37.5mg/325");
        }else if(product.getProductName().contains("travatan eyedrops generic") ){
            product.setProductName("travatan eyedrops");
        }else if(product.getProductName().contains("urispas tablets") ){
            product.setProductName("urispas tablets");
        }else if(product.getProductName().contains("uro-tainer sod chl saline") ){
            product.setProductName("uro-tainer saline");
        }else if(product.getProductName().contains("venlafaxine xl caps") ){
            product.setProductName("venlafaxine xl caps");
        }else if(product.getProductName().contains("venlafaxine xl tabs") ){
            product.setProductName("venlafaxine xl tabs");
        }else if(product.getProductName().contains("white soft liq paraffin emoll 50/50") ){
            product.setProductName("white soft liq paraffin");
        }else if(product.getProductName().contains("xarelto tabs (rivaroxaban)") ){
            product.setProductName("xarelto tabs");
        }else if(product.getProductName().contains("aacetazolamide tabs" )){
            product.setProductName("acetazolamide tabs");
        }else if(product.getProductName().contains("paracetamol soluble")){
            product.setProductName("paracetamol soluble");
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
        }else if(product.getProductName().contains("azithromycin solution")){
            product.setProductName("azithromycin oral suspension");
        }else if(product.getProductName().contains("bactroban nasal oint")){
            product.setProductName("bactroban oint");
        }else if(product.getProductName().contains("brinzolamide eye drops") && product.getStrength().equals("10mg/1ml")){ // need to check with Manish if its 10mg/1ml or 10g/ml??
            product.setProductName("brinzolamide ed");
            product.setStrength("10mg/ml");
        }else if(product.getProductName().contains("brinzolamide+timolol gen azarga")){
            product.setProductName("brinzolamide timolol ed");
        }else if(product.getProductName().contains("azarga eye drops") && product.getStrength().equals("10+5mg")){
            product.setProductName("azarga ed");
            product.setStrength("10mg/5mg");
        }else if(product.getProductName().contains("codeine linctus bp")){
            product.setProductName("codeine linctus");
        }else if(product.getProductName().contains("co-trimoxazole tabs") && product.getStrength().equals("80/400")){
            product.setProductName("co-trimoxazole tabs");
            product.setStrength("80mg/400mg");
        }else if(product.getProductName().contains("co-trimoxazole tabs") && product.getStrength().equals("160/800")){
            product.setProductName("co-trimoxazole tabs");
            product.setStrength("160mg/800mg");
        }else if(product.getProductName().contains("covonia bronchial bal orig mix")) {
            product.setProductName("covonia original bronchial balsam oral solution");
        }else if(product.getProductName().contains("covonia chesty cough mixture")) {
            product.setProductName("covonia chesty cough");
        }else if(product.getProductName().contains("covonia dry & tickly")) {
            product.setProductName("covonia dry tickly");
        }else if(product.getProductName().contains("curanail med nail lacquer") && product.getStrength().equals("5%w/v")) {
            product.setProductName("curanail nail lacquer");
            product.setStrength("5%");
        }else if(product.getProductName().contains("curanail med nail lacquer")) {
            product.setProductName("curanail nail lacquer");
        }else if(product.getProductName().contains("dalacin vaginal cream")) {
            product.setProductName("dalacin cream");
        }else if(product.getProductName().contains("digoxin tabs") && product.getStrength().equals("62.5")) {
            product.setProductName("digoxin tabs");
            product.setStrength("62.5mcg");
        }else if(product.getProductName().contains("duoresp spiro") && product.getStrength().equals("160/4.5")) {
            product.setProductName("duoresp spiromax");
            product.setStrength("160mcg/4.5mcg");
        }else if(product.getProductName().contains("duoresp spiro") && product.getStrength().equals("320/9")) {
            product.setProductName("duoresp spiromax");
            product.setStrength("320mcg/9mcg");
        }else if(product.getProductName().contains("co-amoxiclav s/f susp") && product.getStrength().equals("250/62")) {
            product.setProductName("co-amoxiclav s/f susp");
            product.setStrength("250mg/62mg");
        }else if(product.getProductName().contains("co-amoxiclav s/f susp") && product.getStrength().equals("400/57")) {
            product.setProductName("co-amoxiclav s/f susp");
            product.setStrength("400mg/57mg");
        }else if(product.getProductName().contains("estradiol pess vaginal tabs")) {
            product.setProductName("estradiol tabs");
        }else if(product.getProductName().contains("evorel conti")) {
            product.setProductName("evorel conti");
        }else if(product.getProductName().contains("flutiform 125/5 inhaler") && product.getStrength().equals("125/5")) {
            product.setProductName("flutiform inhaler");
            product.setStrength("125mcg");
        }else if(product.getProductName().contains("lactulose syrup")) {
            product.setProductName("lactulose oral solution");
        }else if(product.getProductName().contains("losartan hctz") && product.getStrength().equals("100/12.5")) {
            product.setProductName("losartan");
            product.setStrength("100mg/12.5mg");
        }else if(product.getProductName().contains("losartan hctz") && product.getStrength().equals("50/12.5m")) {
            product.setProductName("losartan");
            product.setStrength("50mg/12.5mg");
        }else if(product.getProductName().contains("medi derma-s barcr 60345 agyyes") && product.getStrength().equals("90g")) {
            product.setProductName("medi derma-s b/crm");
            product.setStrength("");
        }else if(product.getProductName().contains("medihoney barier cream")) {
            product.setProductName("medihoney barrier cream");
        }else if(product.getProductName().contains("metformin sachet") && product.getStrength().equals("500")) {
            product.setProductName("metformin sachet");
            product.setStrength("500mg");
        }else if(product.getProductName().contains("metformin sachet") && product.getStrength().equals("500")) {
            product.setProductName("metformin sachet");
            product.setStrength("500mg");
        }else if(product.getProductName().contains("nizatidine caps") && product.getStrength().equals("300")) {
            product.setProductName("nizatidine caps");
            product.setStrength("300mg");
        }else if(product.getProductName().contains("ondansetron oral solution")) {
            product.setProductName("ondansetron syrup");
        }else if(product.getProductName().contains("prostap")) {
            product.setProductName("prostap");
        }else if(product.getProductName().contains("sereflo inhaler") && product.getStrength().equals("25/250")) {
            product.setProductName("sereflo inhaler");
            product.setStrength("25mcg/250mcg");
        }else if(product.getProductName().contains("stalevo") && product.getStrength().equals("200/50/200")) {
            product.setProductName("stalevo");
            product.setStrength("200mg/50mg/200mg");
        }else if(product.getProductName().contains("travatan eyedrops brand") && product.getStrength().equals("40mg/ml")) {
            product.setProductName("travatan eyedrops");
            product.setStrength("40mcg/ml");
        }else if(product.getProductName().contains("travoprost/timolol ed") && product.getStrength().equals("40/5mg")) {
            product.setProductName("travoprost timolol ed");
            product.setStrength("40mcg/ml");
        }else if(product.getProductName().contains("travoprost eyedrops generic") && product.getStrength().equals("40mg/ml")) {
            product.setProductName("travoprost ed");
            product.setStrength("40mcg/ml");
        }else if(product.getProductName().contains("trimethoprim susp") && product.getStrength().equals("50mg")) {
            product.setProductName("trimethoprim susp");
            product.setStrength("50/5ml");
        }else if(product.getProductName().contains("white soft paraffin bp")) {
            product.setProductName("white soft paraffin");
        }else if(product.getProductName().contains("hydroxocobalamin inj")) {
            product.setProductName("hydroxocobalamin inj");
        }





    }

    public void print(List<LookupResult> lookupResults){
        lookupResults.stream().forEach(
                v -> System.out.println(" Bns: " + v.getDescription()+" : "+ v.getPriceString() + " : "+ v.getAvailable())
        );
    }

    /*public static boolean specialConsiderationOfProductResultsFromWebsite(String websiteDescription, String productNameFromExcel){



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
        }else if(productNameFromExcel.toLowerCase().contains("susp") && !(productNameFromExcel.toLowerCase().contains("sf") || productNameFromExcel.toLowerCase().contains("s/f")|| productNameFromExcel.toLowerCase().contains("sugar free"))){
            return websiteDescription.contains("susp") && !(websiteDescription.contains("sf") || websiteDescription.contains("s/f"));
        }else if(!productNameFromExcel.toLowerCase().contains("ec") && productNameFromExcel.toLowerCase().contains("tab")){
            return !websiteDescription.contains("ec") && websiteDescription.contains("tab");
        }else if(!productNameFromExcel.toLowerCase().contains("tulle") && productNameFromExcel.toLowerCase().contains("silver")){
            return !websiteDescription.contains("tulle") && websiteDescription.contains("silver");
        }else if(productNameFromExcel.toLowerCase().contains("tulle") && !productNameFromExcel.toLowerCase().contains("silver")){
            return websiteDescription.contains("tulle") && !websiteDescription.contains("silver");
        }else if(!productNameFromExcel.toLowerCase().contains("tulle") && productNameFromExcel.toLowerCase().contains("silver")){
            return !websiteDescription.contains("tulle") && websiteDescription.contains("silver");
        }else if(productNameFromExcel.toLowerCase().contains("body") && productNameFromExcel.toLowerCase().contains("wash")
                && !productNameFromExcel.toLowerCase().contains("baby") && !productNameFromExcel.toLowerCase().contains("lotion") && !productNameFromExcel.toLowerCase().contains("moist")
                && !productNameFromExcel.toLowerCase().contains("hand")){
            return websiteDescription.contains("body") && websiteDescription.contains("wash")
                    && !websiteDescription.toLowerCase().contains("baby") && !websiteDescription.toLowerCase().contains("lotion") && !websiteDescription.toLowerCase().contains("moist")
                    && !websiteDescription.toLowerCase().contains("hand");
        }else if(productNameFromExcel.toLowerCase().contains("cream") && !productNameFromExcel.toLowerCase().contains("wash")
                && !productNameFromExcel.toLowerCase().contains("baby") && !productNameFromExcel.toLowerCase().contains("lotion") && !productNameFromExcel.toLowerCase().contains("moist")
                && !productNameFromExcel.toLowerCase().contains("hand")){
            return websiteDescription.contains("cream") && !websiteDescription.contains("wash")
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
        }else if(productNameFromExcel.toLowerCase().contains("tab") && !productNameFromExcel.toLowerCase().contains("pr")){
            return websiteDescription.contains("tab") && !websiteDescription.contains("pr");
        }// macke sure the below should be the last
        else if(!productNameFromExcel.toLowerCase().contains("atrauman") && !productNameFromExcel.toLowerCase().contains("tulle")){
            return !websiteDescription.contains("atrauman") && !websiteDescription.contains("tulle");
        }

        return true;
    }*/

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
        }else if(productNameFromExcel.toLowerCase().startsWith("codeine") && !productNameFromExcel.toLowerCase().contains("dihydrocodine")
                && productNameFromExcel.toLowerCase().contains("tab") && !productNameFromExcel.toLowerCase().contains("phos")){
            return websiteDescription.contains("codeine") && !websiteDescription.contains("dihydrocodine") && websiteDescription.contains("tab") && !websiteDescription.contains("phos");
        }/*else if(productNameFromExcel.toLowerCase().contains("neutral") && !productNameFromExcel.toLowerCase().contains("extra")){
            return websiteDescription.contains("neutral") && !websiteDescription.contains("extra");
        }else if(productNameFromExcel.toLowerCase().contains("codeine tab") && !productNameFromExcel.toLowerCase().contains("phosphate")){
            return websiteDescription.contains("codeine tab") && !websiteDescription.contains("phosphate");
        }else if(productNameFromExcel.toLowerCase().contains("co-careldopa") && !strengthFromExcel.contains("25")){
            return websiteDescription.contains("co-careldopa") && !websiteDescription.contains("25");
        }*/else if(productNameFromExcel.toLowerCase().contains("susp") && (productNameFromExcel.toLowerCase().contains("sf")
                || productNameFromExcel.toLowerCase().contains("s/f")|| productNameFromExcel.toLowerCase().contains("sugar free"))){

            List<String> pOrPom = Arrays.stream(websiteDescription.toLowerCase().split("\\(|\\)|\\s|\\[|]|'"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            List<String> pOrPomInExcel = Arrays.stream(product.getProductNameUnmodified().toLowerCase().split("\\(|\\)|\\s|\\[|]|'"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            if(pOrPom.isEmpty()){
                return websiteDescription.contains("susp") && (websiteDescription.contains("sf") || websiteDescription.contains("s/f"));
            }

            return websiteDescription.contains("susp") && (websiteDescription.contains("sf") || websiteDescription.contains("s/f"))
                    &&!pOrPomInExcel.isEmpty()? pOrPomInExcel.contains(pOrPom.get(0)):true;


        }else if(productNameFromExcel.toLowerCase().contains("susp") ){

            List<String> pOrPom = Arrays.stream(websiteDescription.toLowerCase().split("\\(|\\)|\\s|\\[|]|'"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            List<String> pOrPomInExcel = Arrays.stream(product.getProductNameUnmodified().toLowerCase().split("\\(|\\)|\\s|\\[|]|'"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            if(pOrPom.isEmpty()){
                // need to check with manasa if s/f in excel is not present then can we order sf from website???
                return websiteDescription.contains("sus");
                //&& !(websiteDescription.contains("sf") || websiteDescription.contains("s/f"));
            }
            return websiteDescription.contains("susp")
                    //&& !(websiteDescription.contains("sf") || websiteDescription.contains("s/f"))
                    &&!pOrPomInExcel.isEmpty()? pOrPomInExcel.contains(pOrPom.get(0)):true;

        }
        else if((productNameFromExcel.toLowerCase().contains("tab") || productNameFromExcel.toLowerCase().contains("caplet") || productNameFromExcel.toLowerCase().contains("oval"))
                && !productNameFromExcel.toLowerCase().contains("hctz")
                && !productNameFromExcel.toLowerCase().contains(" pr ") && !productNameFromExcel.toLowerCase().contains(" sr ") && !productNameFromExcel.toLowerCase().contains(" mr ") && !productNameFromExcel.toLowerCase().contains(" s/c ")
                && !productNameFromExcel.toLowerCase().contains(" ec ")
                && !productNameFromExcel.toLowerCase().contains("disp")
                && !productNameFromExcel.toLowerCase().contains("soluble")
                &&  !productNameFromExcel.toLowerCase().contains(" eff ") &&  !productNameFromExcel.toLowerCase().contains(" ef ") &&  !productNameFromExcel.toLowerCase().contains(" effervescent ")
                &&  !productNameFromExcel.toLowerCase().contains(" hct ") &&  !(productNameFromExcel.toLowerCase().contains(" xl ") || productNameFromExcel.toLowerCase().contains(" xl"))
        ){

            boolean b = websiteDescription.contains("tab") || websiteDescription.contains("capl") || websiteDescription.contains("fct")
                    || websiteDescription.contains("f/c") || websiteDescription.contains("film coated") || websiteDescription.contains(" oad ");

            List<String> pOrPom = Arrays.stream(websiteDescription.toLowerCase().split("\\(|\\)|\\s|\\[|]|'"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            List<String> pOrPomInExcel = Arrays.stream(product.getProductNameUnmodified().toLowerCase().split("\\(|\\)|\\s|\\[|]|'"))
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
                        && !websiteDescription.contains(" effervescent ")
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
                    && !websiteDescription.contains(" effervescent ")
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
        }else if(productNameFromExcel.toLowerCase().contains("eye")){

            List<String> pOrPom = Arrays.stream(websiteDescription.toLowerCase().split("\\(|\\)|\\s|\\[|]|'"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            List<String> pOrPomInExcel = Arrays.stream(product.getProductNameUnmodified().toLowerCase().split("\\(|\\)|\\s|\\[|]|'"))
                    .filter(v -> !v.isEmpty())
                    .filter(v -> v.equals("p") || v.equals("pom"))
                    //.collect(Collectors.toList());
                    .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

            if(pOrPom.isEmpty()){
                return (websiteDescription.contains("eye") || websiteDescription.contains("e/d"));
            }
            return (websiteDescription.contains("eye") || websiteDescription.contains("e/d"))
                    &&!pOrPomInExcel.isEmpty()? pOrPomInExcel.contains(pOrPom.get(0)):true;

        }
        else if(productNameFromExcel.contains("calci") && !productNameFromExcel.contains("betam")){
            return websiteDescription.contains("calci") && !websiteDescription.contains("betam");
        }else if(productNameFromExcel.contains("dorzolamide") && !productNameFromExcel.contains("timolol")){
            return websiteDescription.contains("dorzolamide") && !websiteDescription.contains("timolol");
        }else if(productNameFromExcel.contains("paraffin") && !productNameFromExcel.contains("liquid")){
            return websiteDescription.contains("paraffin") && !websiteDescription.contains("liquid");
        }
        // make sure the below should be the last
        else if(!productNameFromExcel.toLowerCase().contains("atrauman") && !productNameFromExcel.toLowerCase().contains("tulle")){
            return !websiteDescription.contains("atrauman") && !websiteDescription.contains("tulle");
        }

        return true;
    }









    public static boolean websiteDescContainsProductName(String websiteDescription, String productNameFromExcel){

        List<String> productNameSplitByspaceFromExcel = Arrays.asList(productNameFromExcel.split(" "));
        boolean foundMatch = true;
        for(String word : productNameSplitByspaceFromExcel){
            if(!foundMatch){
                break;
            }
            //if(!Arrays.asList(websiteDescription.toLowerCase().split(" ")).contains(word)){
            if(!websiteDescription.toLowerCase().contains(word)){
                if(word.equals("tab") || word.equals("tabs")){
                    foundMatch = websiteDescription.contains("tablets") || websiteDescription.contains("tab") || websiteDescription.contains("tabs");
                }else if(word.equals("caps") || word.equals("cap") || word.equals("capsules")){
                    foundMatch = websiteDescription.contains("capsules") || websiteDescription.contains("caps") || websiteDescription.contains("cap");
                }else if(word.equals("oral") || word.equals("rinse")){
                    foundMatch = websiteDescription.contains("mouthwash") || websiteDescription.contains("mouth wash");
                }else if(word.equals("throat")){
                    foundMatch = websiteDescription.contains("oromucosal");
                }else if(word.equals("nebs")){
                    foundMatch = websiteDescription.contains("nebules") || websiteDescription.contains("nebu");
                }else if(word.equals("border")){
                    foundMatch = websiteDescription.contains("bord");
                }else if(word.equals("ns")){
                    foundMatch = websiteDescription.contains("nasal spray");
                }else if(word.equals("udv")){
                    foundMatch = websiteDescription.contains("ud");
                }else if(word.equals("non")){
                    foundMatch = websiteDescription.contains("n");
                }else if(word.equals("hctz")){
                    foundMatch = websiteDescription.contains("hct");
                }else if(word.equals("sachet") || word.equals("sachets")){
                    foundMatch = websiteDescription.contains("sach");
                }else if(word.equals("cream") || word.equals("cr") || word.equals("crm")){
                    foundMatch = websiteDescription.contains("crm") || websiteDescription.contains("cr") || websiteDescription.contains("cream");
                }else if(word.equals("susp")){
                    foundMatch = websiteDescription.contains("suspension");
                }else if(word.equals("oint")){
                    foundMatch = websiteDescription.contains("ointment");
                }else if(word.equals("vitamin")){
                    foundMatch = websiteDescription.contains("vit");
                }else if(word.equals("adhesive") || word.equals("adh")){
                    foundMatch = websiteDescription.contains("adhesive") || websiteDescription.contains("adh");
                }else if(word.equals("non-adh") || word.equals("non adh") ){
                    foundMatch = websiteDescription.contains("non adh") || websiteDescription.contains("non adhesive");
                }else if(word.equals("dressings")  ){
                    foundMatch = websiteDescription.contains("dressing");
                }else if(word.equals("sp") || word.equals("spr")){
                    foundMatch = websiteDescription.contains("sp") ||websiteDescription.contains("spr") ||websiteDescription.contains("spray");
                }else if(word.equals("suppositories")){
                    foundMatch = websiteDescription.contains("suppository");
                } else if(word.equals("swabs") || word.equals("swab")){
                    foundMatch = websiteDescription.contains("swabs") ||websiteDescription.contains("swab") ;
                }
                else if(word.equals("s/f") ){
                    foundMatch = websiteDescription.contains("sf") ||websiteDescription.contains("sugar free");
                }
                else if(word.equals("s/r") || word.equals("sr") || word.equals("pr")){
                    foundMatch = websiteDescription.contains("sr") || websiteDescription.contains("prolonged release")
                            || websiteDescription.contains("retard");
                }else if(word.equals("syr") ){
                    foundMatch = websiteDescription.contains("syringe");
                }else if(word.equals("silicone") ){
                    foundMatch = websiteDescription.contains("sil") ||websiteDescription.contains("silicone");
                }
                else if(word.equals("ed")){
                    foundMatch = websiteDescription.contains("eye drops") || websiteDescription.contains("eye drop");
                }else if(word.equals("drop")){
                    foundMatch = websiteDescription.contains("drps") || websiteDescription.contains("dps");
                }else if(word.equals("eff")){
                    foundMatch = websiteDescription.contains("effervescent");
                }else if(word.equals("orodisp")){
                    foundMatch = websiteDescription.contains("oral") || websiteDescription.contains("disp");
                }else if(word.equals("mr")){
                    foundMatch = websiteDescription.contains("modified") || websiteDescription.contains("release") || websiteDescription.contains("mr");
                }else if(word.equals("adh") || word.equals("adhesive")){
                    foundMatch = websiteDescription.contains("adh") || websiteDescription.contains("adhesive");
                }else if(word.equals("non-adh")){
                    foundMatch = websiteDescription.contains("non");
                }else if(word.equals("disp")){
                    foundMatch = websiteDescription.contains("soluble") || websiteDescription.contains("dispersible");
                }else if(word.equals("soln")){
                    foundMatch = websiteDescription.contains("solution") || websiteDescription.contains("sol");
                }else if(word.equals("inj")){
                    foundMatch = websiteDescription.contains("injection") ;
                }else if(word.equals("inhlaer")){
                    foundMatch = websiteDescription.contains("breezhaler") ;
                }else if(word.equals("acc")){
                    foundMatch = websiteDescription.contains("accuhaler") ;
                }else if(word.equals("evo")){
                    foundMatch = websiteDescription.contains("evohale") ;
                }else if(word.equals("liquid")){
                    foundMatch = websiteDescription.contains("syrup") ;
                }
                else if(word.equals("amisulpiride")){
                    foundMatch = websiteDescription.contains("amisulpride");
                }else if(word.equals("anastrazole")){
                    foundMatch = websiteDescription.contains("anastrozole");
                }else if(word.equals("betametasone")){
                    foundMatch = websiteDescription.contains("betamethasone");
                }else if(word.equals("cinacalet")){
                    foundMatch = websiteDescription.contains("cinacalcet");
                }else if(word.equals("lansoprozole")){
                    foundMatch = websiteDescription.contains("lansoprazole");
                }else if(word.equals("mirtazipine")){
                    foundMatch = websiteDescription.contains("mirtazapine");
                }else if(word.equals("nortriptyine")){
                    foundMatch = websiteDescription.contains("nortriptyline");
                }else if(word.equals("needles")){
                    foundMatch = websiteDescription.contains("need");
                }
                else{
                    foundMatch = false;
                }

            }else{
                if(word.equals("sulphate")){
                    foundMatch = Arrays.asList(websiteDescription.split(" ")).contains("sulphate");
                }
            }
        }

        return foundMatch;
    }

    private static boolean websiteDescContainsStrength(String description , String strengthFromExcel){
        List<String> strengthAllPermutations = getStrengthPermutations(strengthFromExcel);
        List<String> strengthAllPermutationsCopy = Collections.synchronizedList(new ArrayList<>(strengthAllPermutations));
        if(strengthAllPermutations.isEmpty()){
            return true;
        }
        List<String> descriptionWords = Arrays.asList(description.replaceAll("#|disp.|ce"," ").split("\\s|/|x|\\*|-|\\+"));
        strengthAllPermutationsCopy.retainAll(descriptionWords);

        return !strengthAllPermutationsCopy.isEmpty();
        /*boolean b = strengthAllPermutations.stream().anyMatch(description::contains);
        return b;*/
    }


    private static boolean websiteDescContainsPacksize(String description, String packsizeFromExcel){
        List<String> packsizeAllPermutations = getPackSizePermutations(packsizeFromExcel);
        if(packsizeAllPermutations.isEmpty()){
            return true;
        }
        return packsizeAllPermutations.stream().anyMatch(description::contains);
    }

    private static List<String> getPackSizePermutations(String packSizeFromExcel){
        return Arrays.asList(packSizeFromExcel.split("\\/|x|\\*|or"));
    }

    private static List<String> getStrengthPermutations(String strengthFromExcel){
        strengthFromExcel = strengthFromExcel.replaceAll("\\.0","");
        String multipleUnitsRegex = "(\\d+(?:\\.\\d+)?)(?:\\s+)?(g|gm|mg|mcg|ml|cm|%|cm2|mm|CM|oz|iu|m|kg)?(?:\\/|x|X|\\*|-)(\\d+(?:\\.\\d+)?)(?:\\s+)?(g|gm|mg|mcg|ml|cm|%|cm2|mm|CM|oz|iu|m|kg)?";
        String singleUnitRegex = "(\\d+(?:\\.\\d+)?)(?:\\s+)?(g|gm|mg|mcg|ml|cm|%|cm2|mm|CM|oz|iu|m|kg)?";


        List<String> strengthWithUnits = Collections.synchronizedList(new ArrayList<>());
        Pattern multipleUnitsPattern = Pattern.compile(multipleUnitsRegex);
        Matcher multipleUnitsMatcher = multipleUnitsPattern.matcher(strengthFromExcel);

        Pattern singleUnitsPattern = Pattern.compile(singleUnitRegex);
        Matcher singleUnitsMatcher = singleUnitsPattern.matcher(strengthFromExcel);
        if(multipleUnitsMatcher.find()){
            String firstUnit = multipleUnitsMatcher.group(2);
            String secondUnit = multipleUnitsMatcher.group(4);
            if(firstUnit == null && secondUnit !=null){
                firstUnit = secondUnit;
            }
            if(secondUnit == null && firstUnit != null){
                secondUnit = firstUnit;
            }
            if(firstUnit == null && secondUnit == null){
                firstUnit = "";
                secondUnit = "";
            }
            strengthWithUnits.add(multipleUnitsMatcher.group(1) + firstUnit);
            strengthWithUnits.add(multipleUnitsMatcher.group(3)+ secondUnit);

        }else if(singleUnitsMatcher.find()){
            if(singleUnitsMatcher.group(2)!=null){
                strengthWithUnits.add(singleUnitsMatcher.group(1) + singleUnitsMatcher.group(2));
            }else{
                strengthWithUnits.add(singleUnitsMatcher.group(1));
            }

        }
        return strengthWithUnits;
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
///html[1]/body[1]/article[1]/div[1]/div[1]/form[1]/div[1]/div[1]/input[1]

        driver.findElement(By.xpath("/html[1]/body[1]/strong[1]/div[2]/div[2]/div[1]/div[1]/input[1]")).clear();
        if(strength!=null && !strength.equals("")){
            driver.findElement(By.xpath("/html[1]/body[1]/strong[1]/div[2]/div[2]/div[1]/div[1]/input[1]")).sendKeys(prodNameToBeGivenInSearchField + " "+ strengthToBeGivenInSearchField);
        }else{
            driver.findElement(By.xpath("/html[1]/body[1]/strong[1]/div[2]/div[2]/div[1]/div[1]/input[1]")).sendKeys(prodNameToBeGivenInSearchField );
        }

        Thread.sleep(2000);

        List<LookupResult> lookupResultList = Collections.synchronizedList(new ArrayList<>());

        List<WebElement> numberOfLis = driver.findElements(By.xpath("/html[1]/body[1]/ul[1]/li"));
        if (numberOfLis.size() > 2) {
            for (int i = 3; i <= numberOfLis.size(); i++) {
                String description = driver.findElement(By.xpath("/html[1]/body[1]/ul[1]/li[" + i + "]/a[1]/table[1]/tbody[1]/tr[1]/td[2]")).getAttribute("innerHTML");
                description = description.toLowerCase();
                String removeSpaceBetweenUnitsRegex = "(\\d+(?:\\.\\d+)?)(?:\\s+)?(g|gm|mg|mcg|ml|cm|%|cm2|mm|CM|oz|iu|m|kg)?";
                description = description.replaceAll(removeSpaceBetweenUnitsRegex,"$1$2");

                WebElement availabilityWebelement = driver.findElement(By.xpath("/html[1]/body[1]/ul[1]/li[" + i + "]/a[1]/table[1]/tbody[1]/tr[1]/td[3]/i[1]"));
                String price = driver.findElement(By.xpath("/html[1]/body[1]/ul[1]/li[" + i + "]/a[1]/table[1]/tbody[1]/tr[1]/td[4]")).getAttribute("innerHTML");
                price = price.replaceAll("£","");
                String availability = availabilityWebelement.getAttribute("class").equals("fa fa-circle icon_green") ? "Available" : "No Stock";
                lookupResultList.add(LookupResult.builder().description(description.toLowerCase()).priceString(price.toLowerCase()).available(availability.toLowerCase()).build());

            }
        }
        return lookupResultList;
    }



}