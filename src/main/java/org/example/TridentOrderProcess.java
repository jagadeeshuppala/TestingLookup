package org.example;

import service.TridentOrderToBePlaced;

public class TridentOrderProcess {

    public static void main(String[] args) throws Exception {

        String fileName = "C:\\PharmacyProjectWorkspace\\TestingLookup\\src\\main\\resources\\newSpreadSheet_copy_4_1_2024.xlsx";
        int tridentResultsColNumber = 17;
        int quantityColNumber = 3;
        TridentOrderToBePlaced tridentOrderToBePlaced = new TridentOrderToBePlaced(fileName, tridentResultsColNumber, quantityColNumber);
        tridentOrderToBePlaced.placeOrder();

    }
}
