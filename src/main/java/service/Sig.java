package service;

import model.LookupResult;
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

public class Sig implements Callable<Map<Integer, LookupResult>> {

    String fileName;
    Map<Integer, LookupResult> concurrentHashMap = new ConcurrentHashMap<>();

    public Sig(String fileName){
        this.fileName = fileName;
    }

    /*public Map<Integer, LookupResult> getConcurrentHashMap(){
        return this.concurrentHashMap;
    }*/



    @Override
    public Map<Integer, LookupResult> call() throws Exception {


        WebDriverManager.chromedriver().setup();;
        WebDriver driver = new ChromeDriver();
        driver.get("https://www.sigconnect.co.uk/login");

        driver.findElement(By.id("loginform-username")).sendKeys("bridgwater.pharmacy@nhs.net");
        driver.findElement(By.id("loginform-password")).sendKeys("Br@8486");
        driver.findElement(By.id("login_btn"))
                .sendKeys(Keys.RETURN);



        //String fileName = "C:\\JavaWorkSpace\\ProductLookup\\TestingLookup\\src\\main\\resources\\JagOrderList.xlsx";
        //String fileName = "/Users/juppala/MyNewWorkspace/prodfinder/src/main/resources/JagOrderList.xlsx";
        FileInputStream file = new FileInputStream(fileName);
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0);
        int productNameColumnNumber = 0;
        int strengthColumnNumber = 1;
        int packSizeColumnNumber = 2;
        int quantityColumnNumber = 3;
        int notesColumnNumber = 5;

        List<Product> productNames = Collections.synchronizedList(new ArrayList<>());
        for (int i = 1; i <= sheet.getLastRowNum() && sheet.getRow(i) != null && sheet.getRow(i).getCell(productNameColumnNumber) != null; i++) {
            if (sheet.getRow(i).getCell(quantityColumnNumber).getCellType() != CellType.BLANK
                    && !sheet.getRow(i).getCell(quantityColumnNumber).toString().trim().equals("")
               ) {


                String productName = sheet.getRow(i).getCell(productNameColumnNumber) != null ? new DataFormatter().formatCellValue(sheet.getRow(i).getCell(productNameColumnNumber)).toLowerCase() : null;
                String strenth = sheet.getRow(i).getCell(productNameColumnNumber) != null ? new DataFormatter().formatCellValue(sheet.getRow(i).getCell(strengthColumnNumber)).toLowerCase() : null;
                String packsize = sheet.getRow(i).getCell(productNameColumnNumber) != null ? new DataFormatter().formatCellValue(sheet.getRow(i).getCell(packSizeColumnNumber)).toLowerCase() : null;
                productNames.add(Product.builder().productName(productName).strength(strenth).packsize(packsize).productNameUnmodified(productName).rowNumber(i).build());
            }
        }

        for(Product product : productNames){
            System.out.println("Sigma Product:"+product.getProductName()+" Strength:"+product.getStrength() + " PackSize:"+ product.getPacksize());
            overrideProductBeforeEvenSearch(product);

            try{

                List<LookupResult> lookupResultList = lookupResults(driver, product.getProductName(), product.getStrength());
                System.out.println("Sigma Result list from website");
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
                            .filter(websiteDesc -> Util.websiteDescContainsProductName(websiteDesc.getDescription().toLowerCase(),
                                    product.getProductName().toLowerCase().replaceAll("\\+"," ") ))
                            .filter(websiteDesc -> Util.websiteDescContainsStrength(websiteDesc.getDescription().toLowerCase(), product.getStrength().toLowerCase()))
                            .filter(websiteDesc -> specialConsiderationOfProductResultsFromWebsite(websiteDesc.getDescription().toLowerCase(),
                                    product))
                            //.collect(Collectors.toList());
                            .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
                }


                /*List<LookupResult> matchedWithAllWords = lookupResultList.stream()
                        .filter( websiteDesc -> websiteDescContainsProductNameStrengthAndPackSize(websiteDesc.getDescription(),
                                (productName.getProductName().toLowerCase().replaceAll("\\+"," ") + " "+ productName.getStrength().toLowerCase() + " " + productName.getPacksize().toLowerCase()).split(" ")))
                        .collect(Collectors.toList());*/
                System.out.println("matched result with desc, strength, packsize");
                print(matchedWithProdNameAndStrengthAndPackSize);
                if(!matchedWithProdNameAndStrength.isEmpty()){
                    System.out.println("tried matched result with desc, strength and without packsize");
                    print(matchedWithProdNameAndStrength);
                }


                System.out.println("--------------------------------------------------------");
            }catch (Exception e){
                System.out.println("Sigma exception:::"+product.getProductName() + ":" + product.getStrength() + ":" + e.getMessage());
                e.printStackTrace();
            }

        }

        driver.close();
        driver.quit();
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

        if(product.getProductName().contains("accu-chek aviva aah min £200 7%")){
            product.setProductName("accu-chek aviva");
        }else if(product.getProductName().contains("amoxicillin susp s/f")){
            product.setProductName("amoxicillin s/f");
        }else if(product.getProductName().contains("aciclovir cream")){
            product.setProductName("aciclovir cream");
        }else if(product.getProductName().contains("aciclovir tabs disp act,acc only")){
            product.setProductName("aciclovir tabs disp");
        }else if(product.getProductName().contains("acidex advance anis")){
            product.setProductName("acidex advance anis");
        }else if(product.getProductName().contains("acidex advance pepp")){
            product.setProductName("acidex advance pepp");
        }else if(product.getProductName().contains("acidex standard anis")){
            product.setProductName("acidex anis");
        }else if(product.getProductName().contains("acidex standard pep")){
            product.setProductName("acidex pep");
        }else if(product.getProductName().contains("aldara cream sachets")){
            product.setProductName("aldara cream");
        }else if(product.getProductName().contains("alendronic once weekly tabs")){
            product.setProductName("alendronic tabs");
        }else if(product.getProductName().contains("algivon honey alginate dres")){
            product.setProductName("algivon honey dres");
        }else if(product.getProductName().contains("allevyn adhesive dressing") && product.getStrength().equals("7.5x7.5")){
            product.setProductName("allevyn adhesive");
            product.setStrength("7.5x7.5cm");
        }else if(product.getProductName().contains("allevyn adhesive dressing")){
            product.setProductName("allevyn adhesive");
        }else if(product.getProductName().contains("allevyn ag adh dressing")){
            product.setProductName("allevyn ag adh");
        }else if(product.getProductName().contains("allevyn ag gentle border")){
            product.setProductName("allevyn ag gentle border");
        }else if(product.getProductName().contains("allevyn gentle border")){
            product.setProductName("allevyn gentle border");
        }else if(product.getProductName().contains("allevyn non-adh dressing")){
            product.setProductName("allevyn non adh");
        }else if(product.getProductName().contains("amorolfine nail lacq")){
            product.setProductName("amorolfine nail lacq");
        }else if(product.getProductName().contains("aquacel ag dressing")){
            product.setProductName("aquacel ag");
        }else if(product.getProductName().contains("aquacel ag extra dressing") && product.getStrength().equals("10x10cm")){
            product.setProductName("aquacel ag extra");
            product.setStrength("10x10");
        }
        else if(product.getProductName().contains("aquacel ag extra dressing")){
            product.setProductName("aquacel ag extra");
        }else if(product.getProductName().contains("aquacel ag foam non-adhesive")){
            product.setProductName("aquacel ag foam non adhesive");
        }else if(product.getProductName().contains("aquacel ag ribbon")){
            product.setProductName("aquacel ag ribbon");
        }else if(product.getProductName().contains("aquacel ag+extra") && product.getStrength().equals("15x15cm")){
            product.setProductName("aquacel ag+ extra");
            product.setStrength("15x15");
        }else if(product.getProductName().contains("aquacel ag+extra")){
            product.setProductName("aquacel ag+ extra");
        }else if(product.getProductName().contains("aquacel ag+ribbon") && product.getStrength().equals("2x45cm")){
            product.setProductName("aquacel ag+ ribbon");
            product.setStrength("2x45");
        }else if(product.getProductName().contains("aquacel ag+ribbon")){
            product.setProductName("aquacel ag+ ribbon");
        }else if(product.getProductName().contains("aquacel dressing")){
            product.setProductName("aquacel");
        }else if(product.getProductName().contains("aquacel extra dressing")){
            product.setProductName("aquacel extra");
        }else if(product.getProductName().contains("aquacel foam adhesive") && product.getStrength().equals("10x10cm")){
            product.setProductName("aquacel foam adhesive");
            product.setStrength("10x10");
        }else if(product.getProductName().contains("aquacel foam adhesive")){
            product.setProductName("aquacel foam adhesive");
        }else if(product.getProductName().contains("aquacel ribbon")){
            product.setProductName("aquacel ribbon");
        }else if(product.getProductName().contains("atovaquone/proguanil (malarone)")){
            product.setProductName("atovaquone");
        }else if(product.getProductName().contains("atrauman dressing")){
            product.setProductName("atrauman");
        }else if(product.getProductName().contains("atrauman tulle dressing")) {
            product.setProductName("atrauman tulle");
        }else if(product.getProductName().contains("b-d pen needles") && product.getStrength().equals("31g/8mm")){
            product.setProductName("bd          pen needles");
            product.setStrength("8mm");
        }else if(product.getProductName().contains("b-d pen needles") && product.getStrength().equals("31g/5mm")){
            product.setProductName("bd          pen needles");
            product.setStrength("5mm");
        }else if(product.getProductName().contains("bd u100 syr.0.5ml 29g 324824 mf+") && product.getStrength().equals("0.5ml")){
            product.setProductName("bd u100 syr");
            product.setStrength("29g");
        }else if(product.getProductName().contains("beconase aq nasal spray")){
            product.setProductName("beconase aq n/s");
        }else if(product.getProductName().contains("benzoylperox clindamycin gel")){
            product.setProductName("benzoyl clindamycin gel");
        }else if(product.getProductName().contains("benzydamine oral rinse")){
            product.setProductName("benzydamine m/wash");
        }else if(product.getProductName().contains("benzydamine throat spray")){
            product.setProductName("benzydamine spray");
        }else if(product.getProductName().contains("biatain non adh dressing")){
            product.setProductName("biatain non adh");
        }else if(product.getProductName().contains("biatain silicone dressing") || product.getProductName().contains("biatain silicone  dressing")){
            product.setProductName("biatain silicone");
        }else if(product.getProductName().contains("brimonidine+timolol combigan ed")){
            product.setProductName("brimonidine timolol ed");
        }else if(product.getProductName().contains("briviact tabs (brivaracetam)")){
            product.setProductName("briviact tabs brivaracetam");
        }else if(product.getProductName().contains("bromocriptine tabs")){
            product.setProductName("bromocriptine tabs");
        }else if(product.getProductName().contains("buccolam 10mg/2ml syr branded rx")){
            product.setProductName("buccolam syr");
        }else if(product.getProductName().contains("buccolam 5mg/ml syr rx is branded")){
            product.setProductName("buccolam syr");
        }else if(product.getProductName().contains("budesonide ns rhinocort 3.49+")){
            product.setProductName("budesonide ns");
        }else if(product.getProductName().contains("buprenorphine patch")){
            product.setProductName("buprenorp patch");
        }else if(product.getProductName().contains("buprenorphine tabs")){
            product.setProductName("buprenorphine tabs");
        }else if(product.getProductName().contains("buscopan tabs (p)")){
            product.setProductName("buscopan tabs");
        }else if(product.getProductName().contains("biatain silicone dressing")){
            product.setProductName("biatain silicone");
        }else if(product.getProductName().contains("candesartan tabs")){
            product.setProductName("candesartan tabs");
        }else if(product.getProductName().contains("carbimazole-longlife")){
            product.setProductName("carbimazole");
        }else if(product.getProductName().contains("carboflex dressing")){
            product.setProductName("carboflex");
        }else if(product.getProductName().contains("carbomer pf 0.6ml not carmellose")){
            product.setProductName("carbomer pf");
        }else if(product.getProductName().contains("carmellosepf cellus,evolv,ocu-lub,pfdr,vizc")){
            product.setProductName("carmellose pf");
        }else if(product.getProductName().contains("amoxicillin s/f sac longdate only") && product.getStrength().equals("3g")){
            product.setProductName("amoxicillin s/f sac");
            product.setStrength("3gm");
        }else if(product.getProductName().contains("binosto eff tab (alendronic)")){
            product.setProductName("binosto eff tab");
        }else if(product.getProductName().contains("calcipotriol+betamet gel")){
            product.setProductName("calcipotriol betamet gel");
        }else if(product.getProductName().contains("cavilon barrier cream")){
            product.setProductName("cavilon barrier cream");
        }else if(product.getProductName().contains("chloramphenicol eye drops otc")){
            product.setProductName("chloramphenicol eye drops p");
        }else if(product.getProductName().contains("chloramphenicol eye drops otc")){
            product.setProductName("chloramphenicol eye drops p");
        }else if(product.getProductName().contains("chloramphenicol eye oint p")){
            product.setProductName("chloramphenicol eye oint p");
        }else if(product.getProductName().contains("co-careldopa") && product.getStrength().equals("10/100")){
            product.setProductName("co-careldopa");
            product.setStrength("10");
        }else if(product.getProductName().contains("co-careldopa") && product.getStrength().equals("12.5/50")){
            product.setProductName("co-careldopa");
            product.setStrength("12.5");
        }else if(product.getProductName().contains("co-careldopa") && product.getStrength().equals("25/100")){
            product.setProductName("co-careldopa");
            product.setStrength("25");
        }else if(product.getProductName().contains("co-codamol tabs oval")){
            product.setProductName("co-codamol tabs cap");
        }else if(product.getProductName().contains("colestyramine 4g sf sachet (light)")){
            product.setProductName("colestyramine 4g sf sachet");
        }else if(product.getProductName().contains("combisal inh")){
            product.setProductName("combisal inh");
        }else if(product.getProductName().contains("ciloxan (ciprofloxacin) eye drops")){
            product.setProductName("ciloxan ciprofloxacin eye drops");
        }else if(product.getProductName().contains("clobetasol/clobaderm (dermov) crm")){
            product.setProductName("clobetasol crm");
        }else if(product.getProductName().contains("clobetasol/clobaderm (dermov) oint")){
            product.setProductName("clobetasol oint");
        }else if(product.getProductName().contains("co-beneldopa caps") && product.getStrength().equals("12.5/50")){
            product.setProductName("co-beneldopa caps");
            product.setStrength("50mg/12.5mg");
        }else if(product.getProductName().contains("cetraban")){
            product.setProductName(product.getProductName().replaceAll("cetraban", "cetraben"));
        }else if(product.getProductName().contains("ciclosporin")){
            product.setProductName("ciclosporin caps");
        }else if(product.getProductName().contains("cilodex (ciprofloxacin dexameth) ed")){
            product.setProductName("cilodex ciprofloxacin ed");
        }else if(product.getProductName().contains("ciloxan (ciprofloxacin) eye drops")){
            product.setProductName("ciloxan ciprofloxacin eye drops");
        }else if(product.getProductName().contains("ciprofloxacin susp") && product.getStrength().equals("250mg/5m")){
            product.setProductName("ciprofloxacin susp");
            product.setStrength("250mg/5ml");
        }else if(product.getProductName().contains("co-amilofruse ls tabs")){
            product.setProductName("co-amilofruse tabs");
        }else if(product.getProductName().contains("cetraben emollient cr agcy yes")){
            product.setProductName("cetraben emollient cr");
        }else if(product.getProductName().contains("circadin tab")){
            product.setProductName("circadin tab");
        }else if(product.getProductName().contains("co-amoxiclav s/f susp") && product.getStrength().equals("125mg")){
            product.setProductName("co-amoxiclav s/f syrup");
            product.setStrength("125mg/31mg");
        }else if(product.getProductName().contains("co-amoxiclav s/f susp") && product.getStrength().equals("250/62")){
            product.setProductName("co-amoxiclav s/f syrup");
            product.setStrength("250mg/62mg");
        }else if(product.getProductName().contains("co-amoxiclav s/f susp") && product.getStrength().equals("400/57")){
            product.setProductName("co-amoxiclav s/f syrup");
            product.setStrength("400mg/57mg");
        }else if(product.getProductName().contains("co-careldopa") && product.getStrength().equals("10/100")){
            product.setProductName("co-careldopa");
            product.setStrength("10mg/100mg");
        }else if(product.getProductName().contains("co-careldopa") && product.getStrength().equals("12.5/50")){
            product.setProductName("co-careldopa");
            product.setStrength("12.5mg/50mg");
        }else if(product.getProductName().contains("co-careldopa") && product.getStrength().equals("25/100")){
            product.setProductName("co-careldopa");
            product.setStrength("25mg/100mg");
        }else if(product.getProductName().contains("co-codamol tabs oval") && product.getStrength().equals("30/500mg")){
            product.setProductName("co-codamol caple");
            product.setStrength("30/500");
        }else if(product.getProductName().contains("colecalciferol tabs valupak vit d3")){
            product.setProductName("colecalciferol tabs d3");
        }else if(product.getProductName().contains("corsodyl mouthwash")){
            product.setProductName("corsodyl mouthwash");
        }else if(product.getProductName().contains("co-trimoxazole tabs") && product.getStrength().equals("160/800")){
            product.setProductName("co-trimoxazole tabs");
            product.setStrength("160/800mg");
        }else if(product.getProductName().contains("dermovate cream")){
            product.setProductName("dermovate cream");
        }else if(product.getProductName().contains("dermovate cream")){
            product.setProductName("dermovate cream");
        }/*else if(product.getProductName().contains("diclofenac ec tabs")){
            product.setProductName("diclofenac tabs");
        }*/else if(product.getProductName().contains("diclofenac solaraze gel brand only")){
            product.setProductName("diclofenac solaraze gel");
        }else if(product.getProductName().contains("digoxin tabs") && product.getStrength().equals("62.5")){
            product.setProductName("digoxin tabs");
            product.setStrength("62.5mcg");
        }else if(product.getProductName().contains("diltiazem mr tildiemoct23 stock80")){
            product.setProductName("diltiazem mr tab");
        }else if(product.getProductName().contains("diltiazem xl caps")){
            product.setProductName("diltiazem xl caps");
        }else if(product.getProductName().contains("diltiazem xl caps via7.36 zemt6.2")){
            product.setProductName("diltiazem xl caps");
        }else if(product.getProductName().contains("dovobet gel generic cheaper?")){
            product.setProductName("dovobet gel");
        }else if(product.getProductName().contains("doxycycline disp tab vibramycin")){
            product.setProductName("doxycycline disp tab");
        }else if(product.getProductName().contains("duoderm extra thin")){
            product.setProductName("duoderm x-thin");
        }else if(product.getProductName().contains("duoresp spiro")){
            product.setProductName("duoresp spiro");
        }else if(product.getProductName().contains("dermovate scalp application") && product.getStrength().equals("0.05")){
            product.setProductName("dermovate scalp application");
            product.setStrength("0.05%");
        }else if(product.getProductName().contains("debrisoft")){
            product.setProductName("debrisoft");
        }else if(product.getProductName().contains("easifix bandage") && product.getStrength().equals("7.5x4m")){
            product.setProductName("easifix bandage");
            product.setStrength("7.5cmx4mtr");
        }else if(product.getProductName().contains("eklira aclidinium 60 dose") && product.getStrength().equals("322mg")){
            product.setProductName("eklira aclidinium");
            product.setStrength("322mcg");
        }else if(product.getProductName().contains("eliquis tab")){
            product.setProductName("eliquis tab");
        }else if(product.getProductName().contains("eropid (viagra connect generic)")){
            product.setProductName("eropid");
        }else if(product.getProductName().contains("erythromycin ec tabs")){
            product.setProductName("erythromycin tabs");
        }else if(product.getProductName().contains("estradiol pess vaginal tabs")){
            product.setProductName("estradiol tabs");
        }else if(product.getProductName().contains("estriol crm with applicator 80g")){
            product.setProductName("estriol crm");
        }else if(product.getProductName().contains("evorel conti 24pk cheaper than 3*8?")){
            product.setProductName("evorel conti");
        }else if(product.getProductName().contains("easifix")){
            product.setProductName("easifix");
        }else if(product.getProductName().contains("efudix")){
            product.setProductName("efudix");
        }else if(product.getProductName().contains("fentanyl patch generic") && product.getStrength().equals("12mg")){
            product.setProductName("fentanyl patch");
            product.setStrength("12mcg");
        }else if(product.getProductName().contains("fentanyl matrifen patch") || product.getProductName().contains("fentanyl patch") || product.getProductName().contains("fentanyl")  ){
            product.setProductName("fentanyl patch");
        }else if(product.getProductName().contains("ferrous fumarate tabs")){
            product.setProductName("ferrous fumarate tabs");
        }else if(product.getProductName().contains("firmagon -degarelix")){
            product.setProductName("firmagon degarelix");
        }else if(product.getProductName().contains("fluticasone nasal spray otc")){
            product.setProductName("fluticasone nasal spray");
        }else if(product.getProductName().contains("fosamax 70mg tab")){
            product.setProductName("fosamax 70mg");
        }else if(product.getProductName().contains("fosamax 70mg tab")){
            product.setProductName("fosamax 70mg");
        }else if(product.getProductName().contains("flaminal forte")){
            product.setProductName("flaminal forte");
        }else if(product.getProductName().contains("flu vaccine quad <65")){
            product.setProductName("flu vaccine quad under 65");
        }else if(product.getProductName().contains("ganfort eye drop sol zd")){
            product.setProductName("ganfort e/d");
        }else if(product.getProductName().contains("gauze swab 4ply")){
            product.setProductName("gauze swab 4ply");
        }else if(product.getProductName().contains("gauze swab 8ply")){
            product.setProductName("gauze swab 8ply");
        }else if(product.getProductName().contains("gauze topper sterile swabs")){
            product.setProductName("gauze sterile swabs");
        }else if(product.getProductName().contains("gentamicin")){
            product.setProductName("gentamycin");
        }else if(product.getProductName().contains("glyc/lemon/honey linctus")){
            product.setProductName("glyc lemon honey");
        }else if(product.getProductName().contains("glycerin suppositories adult") && product.getStrength().equals("4g")){
            product.setProductName("glycerin suppositories adult");
            product.setStrength("4gm");
        }else if(product.getProductName().contains("hydrocoll border dressing")){
            product.setProductName("hydrocoll border");
        }else if(product.getProductName().contains("hydrocortisone orom buccal tab")){
            product.setProductName("hydrocortisone tab");
        }else if(product.getProductName().contains("hydrofilm dressing")){
            product.setProductName("hydrofilm");
        }else if(product.getProductName().contains("hydroxocobalamin inj cobalin al9.5os")){
            product.setProductName("hydroxocobalamin inj");
        }else if(product.getProductName().contains("ivabradine tabs (procoralan)")){
            product.setProductName("ivabradine tabs");
        }else if(product.getProductName().contains("kaltostat dress")){
            product.setProductName("kaltostat dress");
        }else if(product.getProductName().contains("kendall amd foam dr")){
            product.setProductName("kendall amd foam dr");
        }else if(product.getProductName().contains("kerramaxcare dressing")){
            product.setProductName("kerramax care dressing");
        }else if(product.getProductName().contains("levonelle one") && product.getStrength().equals("1500mcg")){
            product.setProductName("levonelle one step");
            product.setStrength("1500");
        }else if(product.getProductName().contains("levonorgestrel tab generic") && product.getStrength().equals("1500mcg")){
            product.setProductName("levonelle one step");
            product.setStrength("1500");
        }else if(product.getProductName().contains("levonelle one")){
            product.setProductName("levonelle one step");
        }else if(product.getProductName().contains("levonorgestrel tab generic")){
            product.setProductName("levonelle one step");
        }else if(product.getProductName().contains("levothyroxine tabs") && product.getStrength().equals("12.5mg")){
            product.setProductName("levothyroxine tabs");
            product.setStrength("12.5mcg");
        }else if(product.getProductName().contains("lipitor tabs uk only")){
            product.setProductName("lipitor tabs");
        }else if(product.getProductName().contains("loperamide caps (p)")){
            product.setProductName("loperamide caps p");
        }else if(product.getProductName().contains("loratadine tabs (p)")){
            product.setProductName("loratadine tabs p");
        }else if(product.getProductName().contains("macrogol comp sf sachet (laxido)")){
            product.setProductName("macrogol sf sachet laxido");
        }else if(product.getProductName().contains("menthol in aqueous cream")){
            product.setProductName("menthol aqueous cream");
        }else if(product.getProductName().contains("mepilex border dress")){
            product.setProductName("mepilex border");
        }else if(product.getProductName().contains("mepilex border lite")){
            product.setProductName("mepilex border lite");
        }else if(product.getProductName().contains("mepore dressing")){
            product.setProductName("mepore dressing");
        }else if(product.getProductName().contains("metformin sr generic")){
            product.setProductName("metformin sr");
        }else if(product.getProductName().contains("metrogel gel")){
            product.setProductName("metrogel gel");
        }else if(product.getProductName().contains("miadzolam 5mg/ml syr rx is generic")){
            product.setProductName("miadzolam 5mg/ml syr");
        }else if(product.getProductName().contains("moxonidine tabs") && product.getStrength().equals("200mg")){
            product.setProductName("moxonidine tabs");
            product.setStrength("200mcg");
        }else if(product.getProductName().contains("mepilex border dressing")){
            product.setProductName("mepilex border");
        }else if(product.getProductName().contains("naramig tabs")){
            product.setProductName("naramig tabs");
        }else if(product.getProductName().contains("nitrofurantoin tabs (caps cheaper?)")){
            product.setProductName("nitrofurantoin tabs");
        }else if(product.getProductName().contains("norethisterone tabs")){
            product.setProductName("norethisterone tabs");
        }else if(product.getProductName().contains("novofine 30g needles agency yes")){
            product.setProductName("novofine needles");
        }else if(product.getProductName().contains("nizatidine caps") && product.getStrength().equals("300")){
            product.setProductName("nizatidine caps");
            product.setStrength("300mg");
        }else if(product.getProductName().contains("octasa mr tabs (mesalazine)")){
            product.setProductName("octasa mr tabs mesalazine");
        }else if(product.getProductName().contains("perindopril glen,tev,sand")){
            product.setProductName("perindopril");
        }else if(product.getProductName().contains("pregabalin 20mg/1ml solution")){
            product.setProductName("pregabalin solution");
        }else if(product.getProductName().contains("procyclidine tab kemadrine only")){
            product.setProductName("procyclidine tab");
        }else if(product.getProductName().contains("prograf caps need 10")){
            product.setProductName("prograf caps");
        }else if(product.getProductName().contains("promethazine hcl tabs")){
            product.setProductName("promethazine tabs");
        }else if(product.getProductName().contains("pyridoxine tabs (licenced)")){
            product.setProductName("pyridoxine tabs");
        }else if(product.getProductName().contains("pyridoxine tabs unlicenced")){
            product.setProductName("pyridoxine tabs");
        }else if(product.getProductName().contains("requip xl tabs gen islarge, 2*4 ok")){
            product.setProductName("requip xl tabs");
        }else if(product.getProductName().contains("risperidone tabs (liq is cheaper)")){
            product.setProductName("risperidone tabs");
        }else if(product.getProductName().contains("rivastigmine patch 24hr")){
            product.setProductName("rivastigmine patch");
        }else if(product.getProductName().contains("salbutamol inhaler (salamol) ivax")){
            product.setProductName("salbutamol easibrth salamol");
        }else if(product.getProductName().contains("saline irrigation sod chl clinipod")){
            product.setProductName("saline irrigation clinipod");
        }else if(product.getProductName().contains("salmet/fluticasone generic (avenor)")){
            product.setProductName("salmet fluticasone");
        }else if(product.getProductName().contains("scanpore tape")){
            product.setProductName("scanpore tape");
        }else if(product.getProductName().contains("scopoderm patch") && product.getStrength().equals("1.5")){
            product.setProductName("scopoderm patch");
            product.setStrength("1.5mg");
        }else if(product.getProductName().contains("sereflo inhaler") && product.getStrength().equals("25/250")){
            product.setProductName("sereflo inhaler");
            product.setStrength("250mcg");
        }else if(product.getProductName().contains("seretide acc (fluticasone/salmet)")){
            product.setProductName("seretide acc");
        }else if(product.getProductName().contains("seretide evo (fluticasone/salmet)")){
            product.setProductName("seretide evo");
        }else if(product.getProductName().contains("sinemet plus brand")){
            product.setProductName("sinemet plus");
        }else if(product.getProductName().contains("serevent (salmet) accuhaler")){
            product.setProductName("serevent salmet accuhaler");
        }else if(product.getProductName().contains("serevent (salmet) evohaler")){
            product.setProductName("serevent salmet evohaler");
        }else if(product.getProductName().contains("sevelamer tab nov23: 380 in stock")){
            product.setProductName("sevelamer tab");
        }else if(product.getProductName().contains("sirdupla fluticasone/salmet mylan")){
            product.setProductName("sirdupla fluticasone salmet");
        }else if(product.getProductName().contains("sitagliptin januvia")){
            product.setProductName("sitagliptin");
        }else if(product.getProductName().contains("sodium bicarbonate 420/5 sodibic")){
            product.setProductName("sodium bicarbonate");
        }else if(product.getProductName().contains("tamoxifen tabs brand?")){
            product.setProductName("tamoxifen tabs");
        }else if(product.getProductName().contains("tegaderm film dressing")){
            product.setProductName("tegaderm film");
        }else if(product.getProductName().contains("tegaderm foam adh") || product.getProductName().contains("tegaderm foamadh")){
            product.setProductName("tegaderm foam adh");
        }else if(product.getProductName().contains("tegaderm foamnon")){
            product.setProductName("tegaderm foam non");
        }else if(product.getProductName().contains("tegretol pr tabs")){
            product.setProductName("tegretol pr");
        }else if(product.getProductName().contains("tolterodine xl caps")){
            product.setProductName("tolterodine xl caps");
        }else if(product.getProductName().contains("tramadol sr cap not tabs")){
            product.setProductName("tramadol sr cap");
        }else if(product.getProductName().contains("tramadol sr caps maxitram?")){
            product.setProductName("tramadol sr caps");
        }else if(product.getProductName().contains("travoprost eyedrops generic") && product.getStrength().equals("40mg/ml")) {
            product.setProductName("travoprost eyedrops");
            product.setStrength("40mcg/ml");
        }else if(product.getProductName().contains("travoprost eyedrops generic")){
            product.setProductName("travoprost eyedrops");
        }else if(product.getProductName().contains("tubifast blue") && product.getStrength().equals("5m")){
            product.setProductName("tubifast blue");
            product.setStrength("5mtr");
        }else if(product.getProductName().contains("tubifast blue") && product.getStrength().equals("3m")){
            product.setProductName("tubifast blue");
            product.setStrength("3mtr");
        }else if(product.getProductName().contains("tubifast green") && product.getStrength().equals("5m")){
            product.setProductName("tubifast green");
            product.setStrength("5mtr");
        }else if(product.getProductName().contains("tubifast yellow") && product.getStrength().equals("3m")){
            product.setProductName("tubifast yellow");
            product.setStrength("3mtr");
        }else if(product.getProductName().contains("tubifast yellow") && product.getStrength().equals("5m")){
            product.setProductName("tubifast yellow");
            product.setStrength("5mtr");
        }else if(product.getProductName().contains("venlafaxine xl caps gensberg")){
            product.setProductName("venlafaxine xl caps");
        }else if(product.getProductName().contains("venlafaxine xl tabs venlalic")){
            product.setProductName("venlafaxine xl tabs");
        }else if(product.getProductName().contains("vitamin b co tabs (unlicensed)")){
            product.setProductName("vitamin b co tabs");
        }else if(product.getProductName().contains("voltarol emugel")){
            product.setProductName("voltarol emulgel");
        }else if(product.getProductName().contains("venlafaxine xl caps") && product.getStrength().equals("37.5mg")){
            product.setProductName("venlafaxine xl caps");
            product.setStrength("37.5");
        }else if(product.getProductName().contains("vipdomet tablets") && product.getStrength().equals("12.5/1g")){
            product.setProductName("vipdomet tablets");
            product.setStrength("12.5mg/1g");
        }else if(product.getProductName().contains("white soft liq paraffin emoll 50/50") || product.getProductName().contains("white soft paraffin bp")){
            product.setProductName("white soft paraffin");
        }else if(product.getProductName().contains("xarelto tabs (rivaroxaban)")){
            product.setProductName("xarelto tabs rivaroxaban");
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
            product.setProductName("azithromycin");
        }else if(product.getProductName().contains("betmiga mr tabs")){
            product.setProductName("betmiga mr tabs");
        }else if(product.getProductName().contains("bezafibrate mr tabs")){
            product.setProductName("bezafibrate mr tabs");
        }else if(product.getProductName().contains("brintelix tab vortioxetine")){
            product.setProductName("brintellix tab vortioxetine");
        }else if(product.getProductName().contains("brinzolamide+timolol gen azarga")){
            product.setProductName("brinzolamide timolol e/d");
        }else if(product.getProductName().contains("buprenorphine patch")){
            product.setProductName("buprenorphine patch");
        }else if(product.getProductName().contains("carbocisteine solution") && product.getStrength().equals("750/5")){
            product.setProductName("carbocisteine syr");
            product.setStrength("750mg/5ml");
        }else if(product.getProductName().contains("carbomer pf")){
            product.setProductName("carbomer pf gel");
        }else if(product.getProductName().contains("carmellose eye drops")){
            product.setProductName("carmellose");
        }else if(product.getProductName().contains("cefalexin syrup")){
            product.setProductName("cefalexin oral susp");
        }else if(product.getProductName().contains("cinnarizine tabs")){
            product.setProductName("cinnarazine tabs");
        }else if(product.getProductName().contains("larithromycin susp")){
            product.setProductName("larithromycin syrup");
        }else if(product.getProductName().contains("clexane inj")){
            product.setProductName("clexane pf syr");
        }else if(product.getProductName().contains("clotrimazole vag tabs pessary")){
            product.setProductName("clotrimazole pessary");
        }else if(product.getProductName().contains("co-amilofruse ls tabs")){
            product.setProductName("co-amilofruse tabs");
        }else if(product.getProductName().contains("co-amoxiclav s/f susp")){
            product.setProductName("co-amoxiclav s/f syr");
        }else if(product.getProductName().contains("co-codamol tabs oval") && product.getStrength().equals("30/500mg")){
            product.setProductName("co-codamol tabs oval");
            product.setStrength("30/500");
        }else if(product.getProductName().contains("co-cyprindiol tabs") && product.getStrength().equals("2000/35")){
            product.setProductName("co-cyprindiol tabs");
            product.setStrength("2000/35mcg");
        }else if(product.getProductName().contains("colestyramine 4g") && product.getStrength().equals("4gm")){
            product.setProductName("colestyramine 4g");
            product.setStrength("");
        }else if(product.getProductName().contains("covonia bronchial")){
            product.setProductName("covonia bronchial");
        }else if(product.getProductName().contains("covonia chesty cough")){
            product.setProductName("covonia chesty cough");
        }else if(product.getProductName().contains("covonia dry & tickly")){
            product.setProductName("covonia dry & tickly");
        }else if(product.getProductName().contains("covonia night time")){
            product.setProductName("covonia night time");
        }else if(product.getProductName().contains("curanail med nail lacquer") && product.getStrength().equals("5%w/v")){
            product.setProductName("curanail nail lacquer");
            product.setStrength("5%");
        }else if(product.getProductName().contains("cutimed protect cream")){
            product.setProductName("cutimed protect cream");
        }else if(product.getProductName().contains("dermovate scalp application") && product.getStrength().equals("0.05")){
            product.setProductName("dermovate scalp application");
            product.setStrength("0.05%");
        }else if(product.getProductName().contains("desloratadine tabs")){
            product.setProductName("desloratidine tabs");
        }else if(product.getProductName().contains("dioralyte sachet blackcurrant")){
            product.setProductName("dioralyte blackcurrant");
        }else if(product.getProductName().contains("dioralyte sachet citrus")){
            product.setProductName("dioralyte citrus");
        }else if(product.getProductName().contains("dioralyte sachet plain/natural")){
            product.setProductName("dioralyte natural");
        }else if(product.getProductName().contains("betamethasone cream") && product.getPacksize().equals("30gm")){
            product.setProductName("betamethasone cream");
            product.setPacksize("30g");
        }else if(product.getProductName().contains("betamethasone cream") && product.getPacksize().equals("100gm")){
            product.setProductName("betamethasone cream");
            product.setPacksize("100g");
        }else if(product.getProductName().contains("betamethasone oint") && product.getPacksize().equals("30gm")){
            product.setProductName("betamethasone oint");
            product.setPacksize("30g");
        }else if(product.getProductName().contains("betamethasone oint") && product.getPacksize().equals("100gm")){
            product.setProductName("betamethasone oint");
            product.setPacksize("100g");
        }else if(product.getProductName().contains("chloramphenicol eye oint otc") ){
            product.setProductName("chloramphenicol eye oint");
        }else if(product.getProductName().contains("co-amoxiclav s/f susp") ){
            product.setProductName("co-amoxiclav");
        }else if(product.getProductName().contains("coversyl arginine") && product.getStrength().equals("5/1.25mg") ){
            product.setProductName("coversyl arginine");
            product.setStrength("5mg");
        }else if(product.getProductName().contains("coversyl arginine") ){
            product.setProductName("coversyl arginine");
        }else if(product.getProductName().contains("dorzolamide/timolol pf eye drop") ){
            product.setProductName("dorzolamide timolol pf eye drop");
        }else if(product.getProductName().contains("dymista nasal spray") ){
            product.setProductName("dymista n/sp");
        }else if(product.getProductName().contains("emollin aerosol") && product.getPacksize().equals("240") ){
            product.setProductName("emollin");
            product.setStrength("240ml");
        }else if(product.getProductName().contains("erythromycin susp") ){
            product.setProductName("erythromycin syrup");
        }else if(product.getProductName().contains("fluticasone nasal spray") ){
            product.setProductName("fluticasone n/sp");
        }else if(product.getProductName().contains("half securon sr tabs") ){
            product.setProductName("half securon sr");
        }else if(product.getProductName().contains("ibuprofen/codeine tabs") && product.getStrength().equals("200/8mg") ){
            product.setProductName("ibuprofen codeine tabs");
            product.setStrength("8mg");
        }else if(product.getProductName().contains("imigran nasal spray") ){
            product.setProductName("imigran n/spray");
        }else if(product.getProductName().contains("imiquimod crm sachets") ){
            product.setProductName("imiquimod sachets");
        }/*else if(product.getProductName().contains("ipratropium inhaler") && product.getStrength().equals("200d")){
            product.setProductName("ipratropium");
        }else if(product.getProductName().contains("ipratropium nebuliser sol") ){
            product.setProductName("iipratropium nebuliser");
        }*/else if(product.getProductName().contains("lactulose syrup") ){
            product.setProductName("lactulose solution");
        }else if(product.getProductName().contains("leuprolin") ){
            product.setProductName("leuprorelin");
        }else if(product.getProductName().contains("levonorgestrel tab generic") ){
            product.setProductName("levonorgestrel tab");
        }else if(product.getProductName().contains("loratadine syrup") ){
            product.setProductName("loratadine solution");
        }else if(product.getProductName().contains("lorazepam oval scoredgenus") ){
            product.setProductName("lorazepam oval");
        }else if(product.getProductName().contains("fosamax 70mg tab") ){
            product.setProductName("fosamax");
        }else if(product.getProductName().contains("gtn spray glycerin trinitrate") ){
            product.setProductName("gtn spray");
        }else if(product.getProductName().contains("mebeverine tabs s/c only") ){
            product.setProductName("mebeverine tabs");
        }else if(product.getProductName().contains("migraitan sumatriptan") ){
            product.setProductName("sumatriptan tab");
        }else if(product.getProductName().contains("mometasone nasal spray") ){
            product.setProductName("mometasone n/sp");
        }else if(product.getProductName().contains("monuril fosfomycin sachet") ){
            product.setProductName("fosfomycin");
        }else if(product.getProductName().contains("morphine sulphate solution") ){
            product.setProductName("morphine solution");
        }else if(product.getProductName().contains("naratriptan tabs generic") ){
            product.setProductName("naratriptan tabs");
        }else if(product.getProductName().contains("nicotinell tts 30 patch") ){
            product.setProductName("nicotinell tts 30");
        }else if(product.getProductName().contains("nystatin oral susp nystan = 1.80") ){
            product.setProductName("nystatin oral susp");
        }else if(product.getProductName().contains("orlistat") && product.getStrength().equals("60")){
            product.setProductName("orlistat");
            product.setStrength("60mg");
        }else if(product.getProductName().contains("orlistat")){
            product.setProductName("orlistat");
        }else if(product.getProductName().contains("orphenadrine solution") ){
            product.setProductName("orphenadrine syrup");
        }else if(product.getProductName().contains("pentasa mr sachet") ){
            product.setProductName("pentasa sachet");
        }else if(product.getProductName().contains("prostap 3 depot inj") && product.getStrength().equals("3.75")){
            product.setProductName("prostap 3");
            product.setStrength("3.75mg");
        }else if(product.getProductName().contains("prostap 3 depot inj") ){
            product.setProductName("prostap 3");
        }else if(product.getProductName().contains("rozex cream") && product.getPacksize().equals("30g")){
            product.setProductName("rozex cream");
            product.setPacksize("30gm");
        }else if(product.getProductName().contains("rozex cream") && product.getPacksize().equals("40g")){
            product.setProductName("rozex cream");
            product.setPacksize("40gm");
        }else if(product.getProductName().contains("saflutan eye drops bottle") ){
            product.setProductName("saflutan e/d");
        }else if(product.getProductName().contains("simple linctus with sugar") ){
            product.setProductName("simple linctus sugar");
        }else if(product.getProductName().contains("sodium valp epil 2.31/30= 7.7/100") ){
            product.setProductName("sodium valp epil");
        }else if(product.getProductName().contains("sominex promethazine tabs") ){
            product.setProductName("sominex tabs");
        }else if(product.getProductName().contains("thiamin vitamin b1 tabs") ){
            product.setProductName("thiamin tabs");
        }else if(product.getProductName().contains("tiopex unit dose eye gel 0.4g") && product.getStrength().equals("1mg/1g")){
            product.setProductName("tiopex eye gel");
            product.setStrength("1mg/g");
        }else if(product.getProductName().contains("trimethoprim susp")){
            product.setProductName("trimethoprim syrup");
        }else if(product.getProductName().contains("trospium chlorid mr caps (regurin)")){
            product.setProductName("trospium chloride mr caps");
        }else if(product.getProductName().contains("viagra connect tabs")){
            product.setProductName("viagra tabs");
        }else if(product.getProductName().contains("vipidia tabs alogliptin")){
            product.setProductName("vipidia alogliptin");
        }else if(product.getProductName().contains("vipidia tabs alogliptin")){
            product.setProductName("vipidia alogliptin");
        }else if(product.getProductName().contains("folic acid oral sf (0.5ml / 1ml)")){
            product.setProductName("folic acid oral sf");
        }else if(product.getProductName().contains("ipratropium")){
            product.setProductName("ipratropium");
        }else if(product.getProductName().contains("olanzapine orodisp")){
            product.setProductName("olanzapine orodisp");
        }else if(product.getProductName().contains("meptazinol")){
            product.setProductName("meptazinol");
        }else if(product.getProductName().contains("meptazinol")){
            product.setProductName("meptazinol");
        }else if(product.getProductName().contains("celluvisc eye drops") && product.getStrength().equals("0.50%")){
            product.setProductName("celluvisc eye");
            product.setStrength("0.5%");
        }



    }

    public void print(List<LookupResult> lookupResults){
        lookupResults.stream().forEach(
                v -> System.out.println(" Sigma: " + v.getDescription()+" : "+ v.getPriceString() + " : "+ v.getAvailable())
        );
    }

    /*public static boolean specialConsiderationOfProductResultsFromWebsite(String websiteDescription, String productNameFromExcel){

        if(productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && !productNameFromExcel.toLowerCase().contains("foam")
                && !productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("rib")
                && !productNameFromExcel.toLowerCase().contains("ag+")
                && !productNameFromExcel.toLowerCase().contains("silver")
                && !productNameFromExcel.toLowerCase().contains("tulle")){
            return websiteDescription.toLowerCase().contains("ag")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && !websiteDescription.toLowerCase().contains("foam")
                    && !websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("rib")
                    && !websiteDescription.toLowerCase().contains("ag+")
                    && !websiteDescription.toLowerCase().contains("silver")
                    && !websiteDescription.toLowerCase().contains("tulle");
        }else if(productNameFromExcel.toLowerCase().contains("ag")
                && productNameFromExcel.toLowerCase().contains("extra")
                && !productNameFromExcel.toLowerCase().contains("foam")
                && !productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("rib")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return websiteDescription.toLowerCase().contains("ag")
                    && websiteDescription.toLowerCase().contains("extra")
                    && !websiteDescription.toLowerCase().contains("foam")
                    && !websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("rib")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && productNameFromExcel.toLowerCase().contains("foam")
                && productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("rib")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return websiteDescription.toLowerCase().contains("ag")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && websiteDescription.toLowerCase().contains("foam")
                    && websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("rib")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && productNameFromExcel.toLowerCase().contains("foam")
                && productNameFromExcel.toLowerCase().contains("adh")
                && productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("rib")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return websiteDescription.toLowerCase().contains("ag")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && websiteDescription.toLowerCase().contains("foam")
                    && ((websiteDescription.toLowerCase().contains("adh") &&
                     websiteDescription.toLowerCase().contains("non")) || websiteDescription.toLowerCase().contains("n/a")|| websiteDescription.toLowerCase().contains("non-ad"))
                    && !websiteDescription.toLowerCase().contains("rib")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && !productNameFromExcel.toLowerCase().contains("foam")
                && !productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && productNameFromExcel.toLowerCase().contains("rib")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return websiteDescription.toLowerCase().contains("ag")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && !websiteDescription.toLowerCase().contains("foam")
                    && !websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && websiteDescription.toLowerCase().contains("rib")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(productNameFromExcel.toLowerCase().contains("ag+")
                && productNameFromExcel.toLowerCase().contains("extra")
                && !productNameFromExcel.toLowerCase().contains("foam")
                && !productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("rib")){
            return websiteDescription.toLowerCase().contains("ag+")
                    && websiteDescription.toLowerCase().contains("extra")
                    && !websiteDescription.toLowerCase().contains("foam")
                    && !websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("rib");
        }else if(productNameFromExcel.toLowerCase().contains("ag+")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && !productNameFromExcel.toLowerCase().contains("foam")
                && !productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && productNameFromExcel.toLowerCase().contains("rib")){
            return websiteDescription.toLowerCase().contains("ag+")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && !websiteDescription.toLowerCase().contains("foam")
                    && !websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && websiteDescription.toLowerCase().contains("rib");
        }else if(!productNameFromExcel.toLowerCase().contains("ag")
                && productNameFromExcel.toLowerCase().contains("extra")
                && !productNameFromExcel.toLowerCase().contains("foam")
                && !productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("rib")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return !websiteDescription.toLowerCase().contains("ag")
                    && websiteDescription.toLowerCase().contains("extra")
                    && !websiteDescription.toLowerCase().contains("foam")
                    && !websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("rib")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(!productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && productNameFromExcel.toLowerCase().contains("foam")
                && productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("rib")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return !websiteDescription.toLowerCase().contains("ag")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && websiteDescription.toLowerCase().contains("foam")
                    && websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("rib")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(!productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && productNameFromExcel.toLowerCase().contains("foam")
                && productNameFromExcel.toLowerCase().contains("adh")
                && productNameFromExcel.toLowerCase().contains("non")
                && !productNameFromExcel.toLowerCase().contains("rib")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return !websiteDescription.toLowerCase().contains("ag")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && websiteDescription.toLowerCase().contains("foam")
                    && websiteDescription.toLowerCase().contains("adh")
                    && websiteDescription.toLowerCase().contains("non")
                    && !websiteDescription.toLowerCase().contains("rib")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(!productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("extra")
                && !productNameFromExcel.toLowerCase().contains("foam")
                && !productNameFromExcel.toLowerCase().contains("adh")
                && !productNameFromExcel.toLowerCase().contains("non")
                && productNameFromExcel.toLowerCase().contains("rib")
                && !productNameFromExcel.toLowerCase().contains("ag+")){
            return !websiteDescription.toLowerCase().contains("ag")
                    && !websiteDescription.toLowerCase().contains("extra")
                    && !websiteDescription.toLowerCase().contains("foam")
                    && !websiteDescription.toLowerCase().contains("adh")
                    && !websiteDescription.toLowerCase().contains("non")
                    && websiteDescription.toLowerCase().contains("rib")
                    && !websiteDescription.toLowerCase().contains("ag+");
        }else if(productNameFromExcel.toLowerCase().contains("adh") && !productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("non")){
            return websiteDescription.contains("adh") && !websiteDescription.contains("ag") && !websiteDescription.contains("non");
        }else if(productNameFromExcel.toLowerCase().contains("adh") && productNameFromExcel.toLowerCase().contains("ag")
                && !productNameFromExcel.toLowerCase().contains("non")){
            return websiteDescription.contains("adh") && websiteDescription.contains("ag") && !websiteDescription.contains("non");
        }else if(productNameFromExcel.toLowerCase().contains("gentle bord") && !productNameFromExcel.toLowerCase().contains("ag")){
            return (websiteDescription.contains("gentle bord") || websiteDescription.contains("gen/bor") || websiteDescription.contains("gen.bor")) && !websiteDescription.contains("ag");
        }else if(productNameFromExcel.toLowerCase().contains("non") && productNameFromExcel.toLowerCase().contains("adh")){
            return (websiteDescription.contains("non") && websiteDescription.contains("adh")) || websiteDescription.contains("n-adh") || websiteDescription.contains("n/a") || websiteDescription.contains("non-ad");
        }else if(productNameFromExcel.toLowerCase().contains("susp") && (productNameFromExcel.toLowerCase().contains("sf") || productNameFromExcel.toLowerCase().contains("s/f")|| productNameFromExcel.toLowerCase().contains("sugar free"))){
            return (websiteDescription.contains("sus" )|| websiteDescription.contains("syrup" ) || websiteDescription.contains("syp" )) && (websiteDescription.contains("sf") || websiteDescription.contains("s/f") || websiteDescription.contains("liquid"));
        }else if(productNameFromExcel.toLowerCase().contains("susp") ){
            return (websiteDescription.contains("susp" ) || websiteDescription.contains("syr") || websiteDescription.contains("syp") || websiteDescription.contains("liquid"));
        }*//*else if(!productNameFromExcel.toLowerCase().contains("ec") && productNameFromExcel.toLowerCase().contains("tab")){
            return !websiteDescription.contains("ec") && websiteDescription.contains("tab");
        }*//*else if(!productNameFromExcel.toLowerCase().contains("tulle") && productNameFromExcel.toLowerCase().contains("silver")){
            return !websiteDescription.contains("tulle") && websiteDescription.contains("silver");
        }else if(productNameFromExcel.toLowerCase().contains("tulle") && !productNameFromExcel.toLowerCase().contains("silver")){
            return websiteDescription.contains("tulle") && !websiteDescription.contains("silver");
        }else if(productNameFromExcel.toLowerCase().contains("body") && productNameFromExcel.toLowerCase().contains("wash")
                && !productNameFromExcel.toLowerCase().contains("baby") && !productNameFromExcel.toLowerCase().contains("lotion") && !productNameFromExcel.toLowerCase().contains("moist")
                && !productNameFromExcel.toLowerCase().contains("hand")){
            return websiteDescription.contains("body") && websiteDescription.contains("wash")
                    && !websiteDescription.toLowerCase().contains("baby") && !websiteDescription.toLowerCase().contains("lotion") && !websiteDescription.toLowerCase().contains("moist")
                    && !websiteDescription.toLowerCase().contains("hand");
        }else if(productNameFromExcel.toLowerCase().contains("cream") && !productNameFromExcel.toLowerCase().contains("wash") && !productNameFromExcel.toLowerCase().contains("wash")
                && !productNameFromExcel.toLowerCase().contains("baby") && !productNameFromExcel.toLowerCase().contains("lotion") && !productNameFromExcel.toLowerCase().contains("moist")
                && !productNameFromExcel.toLowerCase().contains("oil")
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
        }else if(productNameFromExcel.toLowerCase().contains("tab") && !productNameFromExcel.toLowerCase().contains(" pr ")){
            return websiteDescription.contains("tab") && !websiteDescription.contains(" pr ");
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
                &&  !productNameFromExcel.toLowerCase().contains(" hct ") &&  !productNameFromExcel.toLowerCase().contains(" xl ")
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
        }
        // make sure the below should be the last
        else if(!productNameFromExcel.toLowerCase().contains("atrauman") && !productNameFromExcel.toLowerCase().contains("tulle")){
            return !websiteDescription.contains("atrauman") && !websiteDescription.contains("tulle");
        }

        return true;
    }







    /*public static boolean websiteDescContainsProductName(String websiteDescription, String productNameFromExcel){

        List<String> productNameSplitByspaceFromExcel = Arrays.asList(productNameFromExcel.split(" "));
        boolean foundMatch = true;
        for(String word : productNameSplitByspaceFromExcel){
            if(!foundMatch){
                break;
            }
            //if(!Arrays.asList(websiteDescription.toLowerCase().split(" ")).contains(word)){
            if(!websiteDescription.toLowerCase().contains(word)){
                if(word.equals("tab") || word.equals("tabs") || word.equals("tablet") || word.equals("tablets") ){
                    foundMatch = websiteDescription.contains("tablets") || websiteDescription.contains("tab") || websiteDescription.contains("tabs");
                }else if(word.equals("caps") || word.equals("cap") || word.equals("capsules")){
                    foundMatch = websiteDescription.contains("capsules") || websiteDescription.contains("caps") || (websiteDescription.contains("cap")&& !websiteDescription.contains("capl"));
                }else if(word.equals("oral") || word.equals("rinse")){
                    foundMatch = websiteDescription.contains("mouthwash") || websiteDescription.contains("mouth wash") || websiteDescription.contains("sol");
                } else if(word.equals("mouthwash") ){
                    foundMatch = websiteDescription.contains("m/wash");
                }else if(word.equals("throat")){
                    foundMatch = websiteDescription.contains("oromucosal");
                }else if(word.equals("nebs") || word.equals("nebuliser")){
                    foundMatch = websiteDescription.contains("nebules") || websiteDescription.contains("nebu");
                }else if(word.equals("border")){
                    foundMatch = websiteDescription.contains("bor");
                }else if(word.equals("ns") || word.equals("nasal") || word.equals("spray")){
                    foundMatch = websiteDescription.contains("nasal sp")|| websiteDescription.contains("n/s") ;
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
                    foundMatch = websiteDescription.contains("suspension") || websiteDescription.contains("liq") || websiteDescription.contains("syr");
                }else if(word.equals("oint")){
                    foundMatch = websiteDescription.contains("ointment") || websiteDescription.contains("oin") ;
                }else if(word.equals("vitamin")){
                    foundMatch = websiteDescription.contains("vit");
                }else if(word.equals("adhesive") || word.equals("adh")){
                    foundMatch = websiteDescription.contains("adhesive") || websiteDescription.contains("adh") || websiteDescription.contains("ad");
                }else if(word.equals("non-adh") ){
                    foundMatch = websiteDescription.contains("non");
                }else if(word.equals("dressings") || word.equals("dres") || word.equals("dressing")){
                    foundMatch = websiteDescription.contains("dressing") || websiteDescription.contains("dres");
                }else if(word.equals("sp") || word.equals("spr")){
                    foundMatch = websiteDescription.contains("sp") ||websiteDescription.contains("spr") ||websiteDescription.contains("spray");
                }else if(word.equals("suppositories")){
                    foundMatch = websiteDescription.contains("suppository") || websiteDescription.contains("supps") || websiteDescription.contains("suppos");
                } else if(word.equals("swabs") || word.equals("swab")){
                    foundMatch = websiteDescription.contains("swabs") ||websiteDescription.contains("swab") ;
                }
                else if(word.equals("s/f") || word.equals("(s/free)") || word.equals("s/free") || word.equals("(sf)") || word.equals("sf")){
                    foundMatch = websiteDescription.contains("sf") ||websiteDescription.contains("sugar free");
                }else if(word.equals("pf") ){
                    foundMatch = websiteDescription.contains("p/f")||websiteDescription.contains("p/free");
                }
                else if(word.equals("s/r") || word.equals("sr") || word.equals("pr")){
                    foundMatch = websiteDescription.contains("sr") || websiteDescription.contains("prolonged release")
                            || websiteDescription.contains("retard");
                }else if(word.equals("syr") ){
                    foundMatch = websiteDescription.contains("syringe");
                }else if(word.equals("silicone") ){
                    foundMatch = websiteDescription.contains("sil") ||websiteDescription.contains("silicone");
                }
                else if(word.equals("ed") || word.equals("eye") || word.equals("drop") || word.equals("drops")){
                    foundMatch = websiteDescription.contains("eye drops") || websiteDescription.contains("eye drop") || websiteDescription.contains("e/d")
                            || websiteDescription.contains("eye drp")
                            || websiteDescription.contains("drps") || websiteDescription.contains("dps") ;
                }*//*else if(word.equals("drop") || word.equals("drops")){
                    foundMatch = websiteDescription.contains("drps") || websiteDescription.contains("dps");
                }*//*else if(word.equals("eff") || word.equals("effervescent")){
                    foundMatch = websiteDescription.contains("effervescent") || websiteDescription.contains("eff");
                }else if(word.equals("orodisp")){
                    foundMatch = websiteDescription.contains("oral") || websiteDescription.contains("disp") || websiteDescription.contains("oro") || websiteDescription.contains("orodis");
                }else if(word.equals("mr")){
                    foundMatch = websiteDescription.contains("modified") || websiteDescription.contains("release") || websiteDescription.contains("mr") || websiteDescription.contains("m/r");
                }else if(word.equals("disp") || word.equals("soluble")){
                    foundMatch = websiteDescription.contains("soluble") || websiteDescription.contains("dispersible") || websiteDescription.contains("sol")
                            || websiteDescription.contains("disp.");
                }else if(word.equals("soln") || word.equals("solution")|| word.equals("sol")  ){
                    foundMatch = websiteDescription.contains("solution") || websiteDescription.contains("sol")  || websiteDescription.contains("susp") ;
                }else if(word.equals("inj")){
                    foundMatch = websiteDescription.contains("injection") ;
                }else if(word.equals("inhaler")){
                    foundMatch = websiteDescription.contains("inh") ;
                }else if(word.equals("inhlaer")){
                    foundMatch = websiteDescription.contains("breezhaler") ;
                }else if(word.equals("acc")){
                    foundMatch = websiteDescription.contains("accuhaler") ;
                }else if(word.equals("evo")){
                    foundMatch = websiteDescription.contains("evohale") ;
                }else if(word.equals("liquid") || word.equals("syrup")){
                    foundMatch = websiteDescription.contains("syrup") || websiteDescription.contains("syr") || websiteDescription.contains("oral sus");
                }
                else if(word.equals("amisulpiride")){
                    foundMatch = websiteDescription.contains("amisulpride");
                }else if(word.equals("anastrazole")){
                    foundMatch = websiteDescription.contains("anastrozole");
                }*//*else if(word.equals("betametasone")){
                    foundMatch = websiteDescription.contains("betamethasone");
                }*//*else if(word.equals("cinacalet")){
                    foundMatch = websiteDescription.contains("cinacalcet");
                }else if(word.equals("lansoprozole")){
                    foundMatch = websiteDescription.contains("lansoprazole");
                }else if(word.equals("mirtazipine")){
                    foundMatch = websiteDescription.contains("mirtazapine");
                }else if(word.equals("nortriptyine")){
                    foundMatch = websiteDescription.contains("nortriptyline");
                }else if(word.equals("needles")){
                    foundMatch = websiteDescription.contains("need");
                }else if(word.equals("amoxicillin")){
                    foundMatch = websiteDescription.contains("amoxicil");
                }else if(word.equals("aripiprazole")){
                    foundMatch = websiteDescription.contains("aripipra") || websiteDescription.contains("aripipraz")
                            || websiteDescription.contains("aripiprazo")|| websiteDescription.contains("aripiprazol");
                }else if(word.equals("atovaquone")){
                    foundMatch = websiteDescription.contains("atova") || websiteDescription.contains("atovaquone");
                }else if(word.equals("gentle")){
                    foundMatch = websiteDescription.contains("gen");
                }else if(word.equals("brimonidine")){
                    foundMatch = websiteDescription.contains("brimonid");
                }*//*else if(word.equals("timolol")){
                    foundMatch = websiteDescription.contains("timol");
                }*//*else if(word.equals("brintelix")){
                    foundMatch = websiteDescription.contains("brintellix");
                }else if(word.equals("patch")){
                    foundMatch = websiteDescription.contains("ptch") || websiteDescription.contains("pat");
                }else if(word.equals("cinnarizine")){
                    foundMatch = websiteDescription.contains("cinnarazine");
                }else if(word.equals("original")){
                    foundMatch = websiteDescription.contains("org") || websiteDescription.contains("orig");
                }else if(word.equals("phosphate")){
                    foundMatch = websiteDescription.contains("phos");
                }else if(word.equals("application")){
                    foundMatch = websiteDescription.contains("app");
                }else if(word.equals("spiro")){
                    foundMatch = websiteDescription.contains("spir");
                }else if(word.equals("blackcurrant")){
                    foundMatch = websiteDescription.contains("blckcurrant");
                }else if(word.equals("ferrous")){
                    foundMatch = websiteDescription.contains("ferr");
                }else if(word.equals("fumarate")){
                    foundMatch = websiteDescription.contains("fuma");
                }else if(word.equals("evohaler")){
                    foundMatch = websiteDescription.contains("evo");
                }else if(word.equals("infants")){
                    foundMatch = websiteDescription.contains("inf");
                }else if(word.equals("dressing")){
                    foundMatch = websiteDescription.contains("dress");
                }else if(word.equals("dinitrate")){
                    foundMatch = websiteDescription.contains("dinit");
                }else if(word.equals("mononitrate")){
                    foundMatch = websiteDescription.contains("mono");
                }else if(word.equals("menthol")){
                    foundMatch = websiteDescription.contains("ment");
                }else if(word.equals("chewable")){
                    foundMatch = websiteDescription.contains("chew");
                }else if(word.equals("granules")){
                    foundMatch = websiteDescription.contains("gran");
                }else if(word.equals("ec")){
                    foundMatch = websiteDescription.contains("e/c");
                }else if(word.equals("clear")){
                    foundMatch = websiteDescription.contains("clr");
                }else if(word.equals("sulphate")){
                    foundMatch = websiteDescription.contains("sulp") || websiteDescription.contains("sulf")&& !websiteDescription.contains("bisulp");
                }else if(word.equals("bisulphate")){
                    foundMatch = websiteDescription.contains("bisulp") ;
                }else if(word.equals("turbohaler")){
                    foundMatch = websiteDescription.contains("turbo") ;
                }else if(word.equals("valp")){
                    foundMatch = websiteDescription.contains("valp") ;
                }else if(word.equals("maxitram")){
                    foundMatch = websiteDescription.contains("maxi") || websiteDescription.contains("max");
                }else if(word.equals("chloride") || word.equals("chlorid")){
                    foundMatch = websiteDescription.contains("chlor")  ;
                }else if(word.equals("tolterodine")){
                    foundMatch = websiteDescription.contains("tolter");
                }else if(word.equals("clopidogrel")){
                    foundMatch = websiteDescription.contains("clopid");
                }else if(word.length() >= 7){
                    foundMatch = websiteDescription.contains(word.substring(0,7));
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
    }*/

    /*private static boolean websiteDescContainsStrength(String description , String strengthFromExcel){
        List<String> strengthAllPermutations = getStrengthPermutations(strengthFromExcel);
        List<String> strengthAllPermutationsCopy = new ArrayList<>(strengthAllPermutations);
        if(strengthAllPermutations.isEmpty()){
            return true;
        }
        if(description.contains("2 gm")){
            description = description.replace("2 gm", "2gm");
        }

        List<String> descriptionWords = Arrays.asList(description.replaceAll("#|disp.|ce"," ").split("\\s|/|x|\\*|-|\\+"));
        strengthAllPermutationsCopy.retainAll(descriptionWords);

        return !strengthAllPermutationsCopy.isEmpty();
        *//*boolean b = strengthAllPermutations.stream().anyMatch(description::contains);
        return b;*//*
    }*/

    /*private static boolean websiteDescContainsPacksize(String description, String packsizeFromExcel){
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
        //strengthFromExcel = strengthFromExcel.replaceAll("\\.0","");
        //String multipleUnitsRegex = "(\\d+(?:\\.\\d+)?)(?:\\s+)?(g|gm|mg|mcg|ml|cm|%|cm2|mm|CM|oz|iu|m|kg)?(?:\\/|x|X|\\*|-)(\\d+(?:\\.\\d+)?)(?:\\s+)?(g|gm|mg|mcg|ml|cm|%|cm2|mm|CM|oz|iu|m|kg)?";
        String multipleUnitsRegex = "(\\d+(?:\\.\\d+)?)(?:\\s+)?(g|gm|mg|mcg|ml|cm|%|cm2|mm|CM|oz|iu|mtr|kg|m)?(?:\\s+)?(?:\\/|x|X|\\*|-)(?:\\s+)?(\\d+(?:\\.\\d+)?)(?:\\s+)?(g|gm|mg|mcg|ml|cm|%|cm2|mm|CM|oz|iu|mtr|kg|m)?";
        //String singleUnitRegex = "(\\d+(?:\\.\\d+)?)(?:\\s+)?(g|gm|mg|mcg|ml|cm|%|cm2|mm|CM|oz|iu|m|kg)?";
        String singleUnitRegex = "(\\d+(?:\\.\\d+)?)(?:\\s+)?(g|gm|mg|mcg|ml|cm|%|cm2|mm|CM|oz|iu|mtr|kg|m)?";


        List<String> strengthWithUnits = new ArrayList<>();
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
    }*/



    private String stockAvailability(String stockAvailablityClass){
        switch (stockAvailablityClass){
            case "ng-binding no_stock":
                return "No Stock";
            case "ng-binding":
                return "Available";
            case "ng-binding low_stock":
                return "Low Stock";
            default:
                return null;

        }
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


        Thread.sleep(1000);
        driver.findElement(By.xpath("/html[1]/body[1]/article[1]/div[1]/div[1]/form[1]/div[1]/div[1]/input[1]")).clear();
        if(strength!=null && !strength.equals("")){
            driver.findElement(By.xpath("/html[1]/body[1]/article[1]/div[1]/div[1]/form[1]/div[1]/div[1]/input[1]")).sendKeys(prodNameToBeGivenInSearchField + " "+ strengthToBeGivenInSearchField);
        }else{
            driver.findElement(By.xpath("/html[1]/body[1]/article[1]/div[1]/div[1]/form[1]/div[1]/div[1]/input[1]")).sendKeys(prodNameToBeGivenInSearchField );
        }

        Thread.sleep(1000);
        driver.findElement(By.xpath("/html[1]/body[1]/article[1]/div[1]/div[1]/form[1]/div[1]/div[1]/input[1]"))
                .sendKeys( Keys.RETURN);

        Thread.sleep(1000);

        List<LookupResult> lookupResultList = Collections.synchronizedList(new ArrayList<>());


        List<WebElement> numberOfLis = driver.findElements(By.xpath("/html[1]/body[1]/article[1]/div[1]/div[3]/div[1]/dl[1]/div"));
        for(int i=1; i<=numberOfLis.size();i++){
            String stockAvailabilityClassAttribute = driver.findElement(By.xpath("/html[1]/body[1]/article[1]/div[1]/div[3]/div[1]/dl[1]/div["+i+"]/dt[1]")).getAttribute("class");
            if(!stockAvailabilityClassAttribute.equalsIgnoreCase("ng-binding special")){
                try{
                    String descriptionFromWebsite = driver.findElement(By.xpath("/html[1]/body[1]/article[1]/div[1]/div[3]/div[1]/dl[1]/div["+i+"]/dt[1]")).getText();
                    String packFromWebsite = driver.findElement(By.xpath("/html[1]/body[1]/article[1]/div[1]/div[3]/div[1]/dl[1]/div["+i+"]/dd[1]/span["+1+"]")).getText();
                    String strengthFromWebsite = driver.findElement(By.xpath("/html[1]/body[1]/article[1]/div[1]/div[3]/div[1]/dl[1]/div["+i+"]/dd[1]/span["+2+"]")).getText();
                    String priceFromWebsite = driver.findElement(By.xpath("/html[1]/body[1]/article[1]/div[1]/div[3]/div[1]/dl[1]/div["+i+"]/dd[1]/span["+3+"]")).getText();
                    priceFromWebsite = priceFromWebsite.replaceAll("£","");
                    String wholeDescription = descriptionFromWebsite + " "+ strengthFromWebsite + " "+ packFromWebsite;
                    lookupResultList.add(LookupResult.builder().description(wholeDescription.toLowerCase()).priceString(priceFromWebsite.toLowerCase()).available(stockAvailability(stockAvailabilityClassAttribute).toLowerCase()).build());

                }catch (Exception e){
                    System.out.println("Sigma exception is::::::"+productName+":"+strength+":"+e.getMessage());
                    e.printStackTrace();
                    Thread.sleep(5000);
                }

            }
        }
        return lookupResultList;
    }


}