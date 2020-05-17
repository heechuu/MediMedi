package com.example.medimedi;

public class Medi {

    private static String medInfo;
   // private String meditext;

    public static String getMedInfo() {

        return medInfo;
    }

    public static void setMedInfo(String medInfo) {

        Medi.medInfo = medInfo;
}

    @Override
    public String toString() {

        return "Medi {medInfo=" + medInfo + "}";
    }
}