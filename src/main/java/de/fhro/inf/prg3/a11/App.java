package de.fhro.inf.prg3.a11;

import de.fhro.inf.prg3.a11.openmensa.OpenMensaAPI;
import de.fhro.inf.prg3.a11.openmensa.OpenMensaAPIService;
import de.fhro.inf.prg3.a11.openmensa.model.Canteen;
import de.fhro.inf.prg3.a11.openmensa.model.Meal;
import de.fhro.inf.prg3.a11.openmensa.model.PageInfo;
import de.fhro.inf.prg3.a11.openmensa.model.State;
import okhttp3.Headers;
import retrofit2.Response;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Peter Kurfer
 * Created on 12/16/17.
 */
public class App {
    private static final String OPEN_MENSA_DATE_FORMAT = "yyyy-MM-dd";

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(OPEN_MENSA_DATE_FORMAT, Locale.getDefault());
    private static final Scanner inputScanner = new Scanner(System.in);
    private static final OpenMensaAPI openMensaAPI = OpenMensaAPIService.getInstance().getOpenMensaAPI();
    private static final Calendar currentDate = Calendar.getInstance();
    private static int currentCanteenId = -1;

    public static void main(String[] args) {
        MenuSelection selection;

        /* loop while true to get back to the menu every time an action was performed */
        do {
            selection = menu();
            switch (selection) {
                case SHOW_CANTEENS:
                    printCanteens();
                    break;
                case SET_CANTEEN:
                    readCanteen();
                    break;
                case SHOW_MEALS:
                    printMeals();
                    break;
                case SET_DATE:
                    readDate();
                    break;
                case QUIT:
                    System.exit(0);

            }
        } while (true);
    }

    private static void printCanteens() {
        System.out.print("Fetching canteens [");
        /* DONE: fetch all canteens and print them to STDOUT
         * at first get a page without an index to be able to extract the required pagination information
         * afterwards you can iterate the remaining pages
         * keep in mind that you should await the process as the user has to select canteen with a specific id */

        List<Canteen> latestPage = null;
        Response<List<Canteen>> canteensResponse = null;
        try {
            canteensResponse = openMensaAPI.getCanteens().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        PageInfo pageInfo = PageInfo.extractFromResponse(canteensResponse);
        //add first page of Canteens to the List of allCanteens
        List<Canteen> allCanteens = new ArrayList<>(canteensResponse.body());

        for(int i = 2; i < pageInfo.getTotalCountOfPages(); i++){
            //add each Mensa to the List of allCanteens, one page at a time, starting at the second page

            latestPage = openMensaAPI.getCanteens(i).join();
            allCanteens.addAll(latestPage);
        }

        while(latestPage == null){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for(Canteen c: allCanteens){
            System.out.println(c.toString());
        }

    }

    private static void printMeals() {
        /* TODO fetch all meals for the currently selected canteen
         * to avoid errors retrieve at first the state of the canteen and check if the canteen is opened at the selected day
         * don't forget to check if a canteen was selected previously! */
        if(currentCanteenId == -1){
            System.out.println("Before showing the Meals of today, please first select a canteen");
            return;
        }
        State canteenState = openMensaAPI.getCanteenState(currentCanteenId, dateFormat.format(currentDate.getTime())).join();

        if(canteenState.isClosed())
            System.out.println("The Canteen is closed today. Go order a pizza or something.");
        else{
            List<Meal> meals = openMensaAPI.getMeals(currentCanteenId, dateFormat.format(currentDate.getTime())).join();
            for(Meal m: meals){
                System.out.println(m.getName());
            }
        }


    }

    /**
     * Utility method to select a canteen
     */
    private static void readCanteen() {
        /* typical input reading pattern */
        boolean readCanteenId = false;
        do {
            try {
                System.out.println("Enter canteen id:");
                currentCanteenId = inputScanner.nextInt();
                readCanteenId = true;
            }catch (Exception e) {
                System.out.println("Sorry could not read the canteen id");
            }
        }while (!readCanteenId);
    }

    /**
     * Utility method to read a date and update the calendar
     */
    private static void readDate() {
        /* typical input reading pattern */
        boolean readDate = false;
        do {
            try {
                System.out.println("Pleae enter date in the format yyyy-mm-dd:");
                Date d = dateFormat.parse(inputScanner.next());
                currentDate.setTime(d);
                readDate = true;
            }catch (ParseException p) {
                System.out.println("Sorry, the entered date could not be parsed.");
            }
        }while (!readDate);

    }

    /**
     * Utility method to print menu and read the user selection
     * @return user selection as MenuSelection
     */
    private static MenuSelection menu() {
        IntStream.range(0, 20).forEach(i -> System.out.print("#"));
        System.out.println();
        System.out.println("1) Show canteens");
        System.out.println("2) Set canteen");
        System.out.println("3) Show meals");
        System.out.println("4) Set date");
        System.out.println("5) Quit");
        IntStream.range(0, 20).forEach(i -> System.out.print("#"));
        System.out.println();

        switch (inputScanner.nextInt()) {
            case 1:
                return MenuSelection.SHOW_CANTEENS;
            case 2:
                return MenuSelection.SET_CANTEEN;
            case 3:
                return MenuSelection.SHOW_MEALS;
            case 4:
                return MenuSelection.SET_DATE;
            default:
                return MenuSelection.QUIT;
        }
    }
}
