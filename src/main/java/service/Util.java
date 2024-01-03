package service;

import model.LookupResult;
import model.LookupResultOptions;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    public static boolean websiteDescContainsProductName(String websiteDescription, String productNameFromExcel){

        List<String> productNameSplitByspaceFromExcel = Arrays.asList(productNameFromExcel.split("\\s|\\["));
        boolean foundMatch = true;
        for(String word : productNameSplitByspaceFromExcel){
            if(!foundMatch){
                break;
            }
            //if(!Arrays.asList(websiteDescription.toLowerCase().split(" ")).contains(word)){
            if(!websiteDescription.toLowerCase().contains(word)){
                if(word.equals("tab") || word.equals("tabs") || word.equals("tablets") || word.equals("tablet")  || word.equals("caplets") || word.equals("caplet") || word.equals("fct") || word.equals("oval") || word.equals("f/c")){
                    foundMatch = websiteDescription.contains("tablets") || websiteDescription.contains("tab") || websiteDescription.contains("tabs")
                            || websiteDescription.contains("capl") || websiteDescription.contains("fct") || websiteDescription.contains("f/c") || websiteDescription.contains(" oad ");
                }else if(word.equals("ec") ){
                    foundMatch = websiteDescription.contains("e/c") ;
                }else if(word.equals("caps") || word.equals("cap") || word.equals("capsules")){
                    foundMatch = (websiteDescription.contains("capsules") || websiteDescription.contains("caps") || websiteDescription.contains("cap")) && !websiteDescription.contains("capl");
                }else if(word.equals("oral") || word.equals("rinse") ||word.equals("orl") || word.equals("mouthwash")){
                    foundMatch = websiteDescription.contains("ora") || websiteDescription.contains("mouthwash") || websiteDescription.contains("mouth wash") || websiteDescription.contains("oral sol")
                            || websiteDescription.contains("o/sol") || websiteDescription.contains("sol") || websiteDescription.contains("m/wash") || websiteDescription.contains("susp") || websiteDescription.contains("or so");
                }else if(word.equals("throat")){
                    foundMatch = websiteDescription.contains("oromucosal");
                }else if(word.equals("nebs") || word.equals("nebuliser")){
                    foundMatch = websiteDescription.contains("nebules") || websiteDescription.contains("neb");
                }else if(word.equals("border")){
                    foundMatch = websiteDescription.contains("bord") || websiteDescription.contains("brdr");
                }else if(word.equals("ns") || word.equals("nasal")){
                    foundMatch = websiteDescription.contains("nasal") || websiteDescription.contains("nsl");
                }else if(word.equals("udv")){
                    foundMatch = websiteDescription.contains("ud");
                }else if(word.equals("non")){
                    foundMatch = websiteDescription.contains("n");
                }else if(word.equals("hctz")){
                    foundMatch = websiteDescription.contains("hct");
                }else if(word.equals("sachet") || word.equals("sachets") || word.equals("sachet") || word.equals("sac")){
                    foundMatch = websiteDescription.contains("sach");
                }else if(word.equals("cream") || word.equals("cr") || word.equals("crm")){
                    foundMatch = websiteDescription.contains("crm") || websiteDescription.contains("cr") || websiteDescription.contains("cream");
                }else if(word.equals("susp") || word.equals("suspension")){
                    foundMatch = websiteDescription.contains("suspension") || websiteDescription.contains("susp") || websiteDescription.contains("sus");
                }else if(word.equals("oint") || word.equals("ointment") ){
                    foundMatch = websiteDescription.contains("ointment") || websiteDescription.contains("oint");
                }else if(word.equals("vitamin")){
                    foundMatch = websiteDescription.contains("vit");
                }else if(word.equals("adhesive") || word.equals("adh")){
                    foundMatch = websiteDescription.contains("adhesive") || websiteDescription.contains("adh");
                }else if(word.equals("non-adh") || word.equals("non adh") ){
                    foundMatch = websiteDescription.contains("non adh") || websiteDescription.contains("non adhesive");
                }else if(word.equals("dressings") || word.equals("dress") ){
                    foundMatch = websiteDescription.contains("dressing") || websiteDescription.contains("dres");
                }else if(word.equals("sp") || word.equals("spr")){
                    foundMatch = websiteDescription.contains("sp") ||websiteDescription.contains("spr") ||websiteDescription.contains("spray");
                }else if(word.equals("suppositories")){
                    foundMatch = websiteDescription.contains("suppos") || websiteDescription.contains("supps");
                } else if(word.equals("swabs") || word.equals("swab")){
                    foundMatch = websiteDescription.contains("swabs") ||websiteDescription.contains("swab") ;
                }
                else if(word.equals("s/f") || word.equals("(s/f)")  || word.equals("s/free") || word.equals("(s/free)") || word.equals("sf")){
                    foundMatch = websiteDescription.contains("sf") ||websiteDescription.contains("sugar free") || websiteDescription.contains("s/f") ;
                }
                else if(word.equals("s/r") || word.equals("sr") || word.equals("pr") || word.equals("mr") || word.equals("xl") || word.equals("s/c")){
                    foundMatch = websiteDescription.contains(" sr ") || websiteDescription.contains("prolonged release") || websiteDescription.contains("s/c") || websiteDescription.contains("s.r")
                            || websiteDescription.contains(" pr ")
                            || websiteDescription.contains("retard")
                            || websiteDescription.contains("modified") || websiteDescription.contains("release") || websiteDescription.contains("mr") || websiteDescription.contains(" m/r ")
                            || websiteDescription.contains("xl")
                    ;
                }else if(word.equals("syr") ){
                    foundMatch = websiteDescription.contains("syringe");
                }else if(word.equals("silicone") ){
                    foundMatch = websiteDescription.contains("sil") ||websiteDescription.contains("silicone");
                }
                else if(word.equals("ed") || word.equals("e/d") || word.equals("eyedrops")){
                    foundMatch = websiteDescription.contains("eye drops") || websiteDescription.contains("eye drop") || websiteDescription.contains("e/dr")
                    || websiteDescription.contains("eye drp");
                }else if(word.equals("drop") || word.equals("drops")){
                    foundMatch = websiteDescription.contains("drps") || websiteDescription.contains("dps")|| websiteDescription.contains("drop");
                }else if(word.equals("eff") || word.equals("effervescent")){
                    foundMatch = websiteDescription.contains("effervescent") || websiteDescription.contains("eff");
                }else if(word.equals("orodisp") || word.equals("oro")){
                    foundMatch = websiteDescription.contains("oral") || websiteDescription.contains("disp") || websiteDescription.contains("ordsp")|| websiteDescription.contains("orodis");
                }else if(word.equals("adh") || word.equals("adhesive")){
                    foundMatch = websiteDescription.contains("adh") || websiteDescription.contains("adhesive");
                }else if(word.equals("non-adh")){
                    foundMatch = websiteDescription.contains("non");
                }else if(word.equals("disp") || word.equals("soluble") ){
                    foundMatch = websiteDescription.contains("sol") || websiteDescription.contains("dispersible");
                }else if(word.equals("soln") || word.equals("solution") ){
                    foundMatch = websiteDescription.contains("solution") || websiteDescription.contains("sol") || websiteDescription.contains("liq")
                            || websiteDescription.contains("or/soln") || websiteDescription.contains("orl soln") || websiteDescription.contains("so");
                }else if(word.equals("inj")){
                    foundMatch = websiteDescription.contains("injection") ;
                }else if(word.equals("inhlaer")){
                    foundMatch = websiteDescription.contains("breezhaler") ;
                }else if(word.equals("inhaler")){
                    foundMatch = websiteDescription.contains("inh") ;
                }else if(word.equals("acc")){
                    foundMatch = websiteDescription.contains("accuhaler") ;
                }else if(word.equals("evo")){
                    foundMatch = websiteDescription.contains("evohale") ;
                }else if(word.equals("liquid") || word.equals("syrup")){
                    foundMatch = websiteDescription.contains("syrup") || websiteDescription.contains("syr") || websiteDescription.contains("syp") ;
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
                }else if(word.equals("gentle")){
                    foundMatch = websiteDescription.contains("gent") || websiteDescription.contains("gntl");
                }else if(word.equals("extra")){
                    foundMatch = websiteDescription.contains("ext") || websiteDescription.contains("xta");
                }else if(word.equals("ribbon")){
                    foundMatch = websiteDescription.contains("rib") ;
                }else if(word.equals("elxir") || word.equals("elixir")){
                    foundMatch = websiteDescription.contains("elixir")  || websiteDescription.contains("elix");
                }else if(word.equals("salmet")){
                    foundMatch = websiteDescription.contains("sal") ;
                }else if(word.equals("pre-filled")){
                    foundMatch = websiteDescription.contains("pre-fill") ;
                }else if(word.equals("mononitrate")){
                    foundMatch = websiteDescription.contains("mono") ;
                }else if(word.equals("clear")){
                    foundMatch = websiteDescription.contains("clr") ;
                }else if(word.equals("kemadrine")){
                    foundMatch = websiteDescription.contains("kemadrin") ;
                }else if(word.equals("cromoglycate")){
                    foundMatch = websiteDescription.contains("cromoglicate") ;
                }else if(word.equals("wipes")){
                    foundMatch = websiteDescription.contains("wipe") ;
                }else if(word.equals("ster-neb")){
                    foundMatch = websiteDescription.contains("steri-neb") ;
                }else if(word.equals("venlafaxine")){
                    foundMatch = websiteDescription.contains("venlalic") ;
                }else if(word.equals("aripiprazole")){
                    foundMatch = websiteDescription.contains("aripipraz") ;
                }else if(word.equals("atovaquone")){
                    foundMatch = websiteDescription.contains("atova") ;
                }else if(word.equals("proguanil")){
                    foundMatch = websiteDescription.contains("prog") ;
                }else if(word.equals("bendroflumethiazide")){
                    foundMatch = websiteDescription.contains("bendrof") || websiteDescription.contains("bendroflum") ;
                }else if(word.equals("betahistine")){
                    foundMatch = websiteDescription.contains("betahistin") ;
                }else if(word.equals("betamethasone")){
                    foundMatch = websiteDescription.contains("betameth") ;
                }else if(word.equals("bimatoprost")){
                    foundMatch = websiteDescription.contains("bimato") ;
                }else if(word.equals("brimonidine")){
                    foundMatch = websiteDescription.contains("brimonid") ;
                }else if(word.equals("timolol")){
                    foundMatch = websiteDescription.contains("timol") ;
                }else if(word.equals("buprenorphine")){
                    foundMatch = websiteDescription.contains("buprenorp")  || websiteDescription.contains("buprenor");
                }else if(word.equals("patch")){
                    foundMatch = websiteDescription.contains("ptch") || websiteDescription.contains("pat") ;
                }else if(word.equals("calcipotriol")){
                    foundMatch = websiteDescription.contains("calcipo") ;
                }else if(word.equals("carbamazepine")){
                    foundMatch = websiteDescription.contains("carbamazepin") ;
                }else if(word.equals("carbocisteine")){
                    foundMatch = websiteDescription.contains("carbociste") ;
                }else if(word.equals("clarithromycin")){
                    foundMatch = websiteDescription.contains("clarithromy") ;
                }else if(word.equals("clonazepam")){
                    foundMatch = websiteDescription.contains("clonaz") ;
                }else if(word.equals("phosphate")){
                    foundMatch = websiteDescription.contains("phos") ;
                }else if(word.equals("blackcurrant")){
                    foundMatch = websiteDescription.contains("b/cur") ;
                }else if(word.equals("pf")){
                    foundMatch = websiteDescription.contains("chloramphenicol") ;
                }else if(word.equals("application")){
                    foundMatch = websiteDescription.contains("app") ;
                }else if(word.equals("ferrous")){
                    foundMatch = websiteDescription.contains("ferrous") || websiteDescription.contains("ferr") ;
                }else if(word.equals("fumarate")){
                    foundMatch = websiteDescription.contains("fuma") ;
                }else if(word.equals("evohaler")){
                    foundMatch = websiteDescription.contains("evo") ;
                }else if(word.equals("infants")){
                    foundMatch = websiteDescription.contains("infant") ;
                }else if(word.equals("dinitrate")){
                    foundMatch = websiteDescription.contains("dinit") ;
                }else if(word.equals("shampoo")){
                    foundMatch = websiteDescription.contains("shamp") ;
                }else if(word.equals("menthol")){
                    foundMatch = websiteDescription.contains("ment") ;
                }else if(word.equals("chewable")){
                    foundMatch = websiteDescription.contains("chew") ;
                }else if(word.equals("granules")){
                    foundMatch = websiteDescription.contains("gran") ;
                }else if(word.equals("step")){
                    foundMatch = websiteDescription.contains("stp") ;
                }else if(word.equals("oxybutinin")){
                    foundMatch = websiteDescription.contains("oxybutynin") ;
                }else if(word.equals("sulphate")){
                    foundMatch = (websiteDescription.contains("sulp") || websiteDescription.contains("sulfate")
                    ||  websiteDescription.contains("sulf")) && !websiteDescription.contains("bisu");
                }else if(word.equals("bicarbonate")){
                    foundMatch = websiteDescription.contains("bica") ;
                }else if(word.equals("turbohaler")){
                    foundMatch = websiteDescription.contains("turbo") ;
                }else if(word.equals("chloride")){
                    foundMatch = websiteDescription.contains("chlor") ;
                }else if(word.equals("anis")){
                    foundMatch = websiteDescription.contains("aniseed") ;
                }else if(word.equals("pepp")){
                    foundMatch = websiteDescription.contains("p/mint") ;
                }else if(word.equals("amisulpride")){
                    foundMatch = websiteDescription.contains("amisulpride") ;
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
    }

    public static boolean websiteDescContainsStrength(String description , String strengthFromExcel){
        List<String> strengthAllPermutations = getStrengthPermutations(strengthFromExcel);
        List<String> strengthAllPermutationsCopy = Collections.synchronizedList(new ArrayList<>(strengthAllPermutations));
        if(strengthAllPermutations.isEmpty()){
            return true;
        }
        if(description.contains("800 iu")){
            description = description.replace("800 iu", "800iu");
        }
        List<String> descriptionWords = Arrays.asList(description.replaceAll("#|disp.|ce|app.|p.i"," ").split("\\s|/|x|\\*|-|\\+|\\(|\\)|\\[|]"));
        strengthAllPermutationsCopy.retainAll(descriptionWords);

        return !strengthAllPermutationsCopy.isEmpty();
        /*boolean b = strengthAllPermutations.stream().anyMatch(description::contains);
        return b;*/
    }


    public static boolean websiteDescContainsPacksize(String description, String packsizeFromExcel){
        List<String> packsizeAllPermutations = getPackSizePermutations(packsizeFromExcel);
        if(description.contains("2 gm")){
            description = description.replace("2 gm", "2gm");
        }
        if(packsizeAllPermutations.isEmpty()){
            return true;
        }
        return packsizeAllPermutations.stream().anyMatch(description::contains);
    }

    private static List<String> getPackSizePermutations(String packSizeFromExcel){
        return Arrays.asList(packSizeFromExcel.split("\\/|x|\\*|or"));
    }

    private static List<String> getStrengthPermutations(String strengthFromExcel){
        strengthFromExcel = strengthFromExcel.replaceAll("\\.0%","%");
        String multipleUnitsRegex = "(\\d+(?:\\.\\d+)?)(?:\\s+)?(gm|mg|mcg|ml|cm|%|cm2|mm|CM|oz|iu|m|kg|g)?(?:\\/|x|X|\\*|-)(\\d+(?:\\.\\d+)?)(?:\\s+)?(gm|mg|mcg|ml|cm|%|cm2|mm|CM|oz|iu|m|kg|g)?";
        String singleUnitRegex = "(\\d+(?:\\.\\d+)?)(?:\\s+)?(gm|mg|mcg|ml|cm|%|cm2|mm|CM|oz|iu|m|kg|g)?";


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

    public static LookupResultOptions getCheapestOption(List<LookupResult> lookupResults){
        if(lookupResults.isEmpty()){
            return LookupResultOptions.builder()
                    .cheapestOption(LookupResult.builder().priceString("-1").description("NA").available("NA").build())
                    .cheapestAvailableOption(LookupResult.builder().priceString("-1").description("NA").available("NA").build())
                    .build();
        }
        LookupResult cheapestOption =  lookupResults.stream()
                .min(Comparator.comparingDouble(
                        result -> Double.parseDouble(result.getPriceString().replaceAll("£|,","")))
                ).orElse(LookupResult.builder().priceString("-1").description("NA").available("NA").build());

        LookupResult cheapestAvailableOption =  lookupResults.stream()
                .filter(lookupResult -> lookupResult.getAvailable().equals("available") || lookupResult.getAvailable().equals("low stock")  || lookupResult.getAvailable().equals("In stock"))
                .min(Comparator.comparingDouble(
                        result -> Double.parseDouble(result.getPriceString().replaceAll("£|,","")))
                ).orElse(LookupResult.builder().priceString("-1").description("NA").available("NA").build());


       return LookupResultOptions.builder()
               .cheapestAvailableOption(cheapestAvailableOption)
               .cheapestOption(cheapestOption)
               .build();
    }
}

