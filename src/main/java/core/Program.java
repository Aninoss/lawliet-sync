package core;

public class Program {

    private static boolean production = false;

    public static void init(boolean newProduction) {
        production = newProduction;
        System.out.println("-------------------------------------");
        System.out.println("Production Mode: " + production);
        System.out.println("-------------------------------------");
    }

    public static boolean isProductionMode() {
        return production;
    }

}
