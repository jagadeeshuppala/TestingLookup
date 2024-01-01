package service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test {

    public static void main(String[] args) {
        /*List<String> one = Arrays.asList("A","B","C");
        List<String> two = Arrays.asList("A","B","F");

        List<String> oneCopy = new ArrayList<>(one);
        oneCopy.retainAll(two);
        System.out.println(oneCopy);

        String s = "bactigrxas dress.10x10cm 7457 # 45.5% pk/10";
        System.out.println(Arrays.asList(s.split("\\s|/|x|\\*")));

        String s1 = "0.05%";
        s1 = s1.replaceAll("\\.0%$","%");
        System.out.println(s1);

        String p1 = "loperamide caps (p)";
        String p2 = "loperamide tabs pom";
        String p3 = "loperamide caps p";
        String p4 = "loratadine tabs (p)";
        String p5 = "loratadine tabs";

                //(?:\s+)?


        Pattern p = Pattern.compile("(\\s+p|\\s+\\(p\\)|\\s+pom|\\s+\\(pom\\))"); //

        Matcher m = p.matcher (p1);
        if (m.find()) {
            System.out.println(m.group());
        }

        System.out.println(p1.matches(".\\s+p|\\s+\\(p\\)|\\s+pom|\\s+\\(pom\\)."));

        System.out.println("p1"+p1.replaceAll("(\\s+p|\\s+\\(p\\)|\\s+pom|\\s+\\(pom\\))", ""));
        System.out.println("p2"+p2.replaceAll("(\\w+)(\\s+p|\\s+\\(p\\)|\\s+pom|\\s+\\(pom\\))(\\w+)", "$1 $3"));
        System.out.println("p3"+p3.replaceAll("(\\s+p|\\s+\\(p\\)|\\s+pom|\\s+\\(pom\\))", " "));
        System.out.println("p4"+p4.replaceAll("(\\s+p|\\s+\\(p\\)|\\s+pom|\\s+\\(pom\\))", " "));
        System.out.println("p5"+p5.replaceAll("(\\s+p|\\s+\\(p\\)|\\s+pom|\\s+\\(pom\\))", " "));


        System.out.println(p1.replaceAll("(\\s+p|\\s+\\(p\\))", " p " ));
        System.out.println(p2.replaceAll("(\\s+pom|\\s+\\(pom\\))", " pom " ));
        System.out.println(p3.replaceAll("(\\s+p|\\s+\\(p\\))", " p " ));
        System.out.println(p4.replaceAll("(\\s+p|\\s+\\(p\\))", " p " ));*/

        /*String sonnet = "loratadine tabs (pom) par";

        Pattern pattern = Pattern.compile("\\bpom\\b|\\bp\\b|\\b\\(pom\\)\\b|\\b\\(p\\)\\b");
        Matcher matcher = pattern.matcher(sonnet);
        while (matcher.find()) {
            String group = matcher.group();
            int start = matcher.start();
            int end = matcher.end();
            System.out.println(group + " " + start + " " + end);
            sonnet = sonnet.replaceAll(group, "");
            sonnet = sonnet.replaceAll("\\(\\)","");
        }

        System.out.println(sonnet);

        String s2 ="loratadine tabs (p)";
        System.out.println(Arrays.asList(s2.split("\\s|\\(|\\)")));*/

        String prodName = "fluconazole caps (p) da";

        prodName = prodName.toLowerCase().replaceAll("\\(p\\)", " p ");
        prodName = prodName.toLowerCase().replaceAll("\\(pom\\)", " pom ");

        Pattern pattern = Pattern.compile("\\sp\\b|\\spom\\b");
        Matcher matcher = pattern.matcher(prodName);
        while (matcher.find()) {
            String group = matcher.group();
            int start = matcher.start();
            int end = matcher.end();
            System.out.println(group + " " + start + " " + end);

            prodName = prodName.replaceAll(group,"");

            /*product.setProductName(product.getProductName().replaceAll(group,""));
            product.setProductName(product.getProductName().replaceAll("\\(\\)",""));*/
        }
        System.out.println(prodName);
    }





}
