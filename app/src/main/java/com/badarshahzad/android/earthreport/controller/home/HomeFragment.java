package com.badarshahzad.android.earthreport.controller.home;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.badarshahzad.android.earthreport.controller.favourit.FavouriteActivity;
import com.badarshahzad.android.earthreport.R;
import com.badarshahzad.android.earthreport.model.api.EarthquakeLoader;
import com.badarshahzad.android.earthreport.model.pojos.EarthQuakes;
import com.badarshahzad.android.earthreport.model.pojos.FavoriteCountries;
import com.badarshahzad.android.earthreport.model.utilties.DataProviderFormat;
import com.badarshahzad.android.earthreport.model.utilties.ParseFavoritCountriesJsonUtils;
import com.badarshahzad.android.earthreport.model.utilties.ParseUSGSJsonUtils;
import com.badarshahzad.android.earthreport.controller.timeline.TimelineFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment implements OnMapReadyCallback {

    public static final String FAVOURIT_LIST = "favouritList";
    private static final String TAG = HomeFragment.class.getSimpleName();
    public static String TODAY_COUNT_KEY = "todayCountkey";
    public static String YESTERDAY_COUNT_KEY = "yesterdayCountkey";
    public static String WEEK_COUNT_KEY = "weekCountkey";
    public static String MONTH_COUNT_KEY = "monthCountkey";
    public static final String COUNT_KEY_ARRAY[] = {TODAY_COUNT_KEY, YESTERDAY_COUNT_KEY, WEEK_COUNT_KEY, MONTH_COUNT_KEY};
    public static int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    public static Handler handler;
    public static String todayDate = DataProviderFormat.getformateDate(new Date());
    private static String COUNT_KEY = "countKey";
    public Double selectedMinMagnitude;
    public String selectedCountery;

    private TextView todayEarthquakes;
    private TextView yesterdayEarthquakes;
    private TextView thisMonthEarthquakes;
    private TextView thisWeekEarthquakes;
    private TextView todayMapStatus;
    private String[] countURLS = new String[4];
    private ProgressBar progressBar;
    private List<EarthQuakes> alldayList;
    private String selectedSearchPlaceName;
    private LatLng selectedSearchPlaceLatLng;
    private FusedLocationProviderClient fusedLocationProviderClient;



    //Today earthquakes count
    private String CountURL = "https://earthquake.usgs.gov/fdsnws/event/1/count?format=geojson&starttime=";

    public HomeFragment() {

    }

    //Get the all countries name
    public static ArrayList<String> getCountriesName() {
        ArrayList<String> list = new ArrayList<String>();

        String[] locales = Locale.getISOCountries();

        for (String countryCode : locales) {

            Locale obj = new Locale("", countryCode);

            //Log.i(TAG, "Country Name = " + obj.getDisplayCountry());
            list.add(obj.getDisplayCountry());

        }
        return list;
    }

    //Does the Lati&Longi is equale to someone countery?
    public static boolean isCounteryExist(Context context, double latitude, double longitude) {
        String counteryName = null;
        try {
            counteryName = getCountryName(context, latitude, longitude);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<String> list = new ArrayList<String>();

        String[] locales = Locale.getISOCountries();

        for (String countryCode : locales) {

            Locale obj = new Locale("", countryCode);

            //Log.i(TAG, "Country Name = " + obj.getDisplayCountry());
            list.add(obj.getDisplayCountry());


            if (counteryName != null && counteryName.equals(obj.getDisplayCountry())) {
                Log.i(TAG, "Get Display Countery:" + obj.getDisplayCountry());
                Log.i(TAG, "Countery Name: " + obj.getDisplayCountry());
                return true;
            }
        }
        return false;
    }

    //if name exist then tell me the name
    public static String getCountryName(Context context, double latitude, double longitude) throws IOException {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

        if (addresses.isEmpty()) {
            return null;
        }
        //Log.i(TAG, "getCountryName:1 "+addresses.get(0).getCountryName());
        Address result;

        if (addresses != null && !addresses.isEmpty()) {
            //  Log.i(TAG, "getCountryName:2 "+addresses.get(0).getCountryName());
            return addresses.get(0).getCountryName();
        }
        return null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Yes it has the menu item
        setHasOptionsMenu(true);

        //Fragment protect from destroy and recreate and retain
        // the current instance of the fragment when the activity is recreated.
        // CheckedTodo: The retaininstance helping me on activity orientation change
        setRetainInstance(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        //Firstly clear the existed menu & and add my own menu
        menu.clear();
        inflater.inflate(R.menu.menu_home, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_bookmark) {
             Intent intent = new Intent(getContext(), FavouriteActivity.class);
             startActivity(intent);


        }

        //Reference:https://materialdoc.com/components/dialogs/
        if (itemId == R.id.action_add_alert) {

          showAddAlertDialog(getLayoutInflater(),getActivity(),getContext());
        }


        return super.onOptionsItemSelected(item);
    }

    public void showAddAlertDialog(LayoutInflater inflater2, Activity activity, final Context context) {



       // LayoutInflater inflater2 = getLayoutInflater();
        View addAlertView = inflater2.inflate(R.layout.fragment_add_alert, null,false);

        ///Add the location fragment

        final Spinner minMagnitudeSpinner = addAlertView.findViewById(R.id.minOfSpinner);
        final Spinner counteryOfSpinner = addAlertView.findViewById(R.id.regionSelected);



        List<String> countries = getCountriesName();

        //sort the countries name in ascending order (This class Java 8 API Collection framework)
        // to show countries name in spinner a-z
        Collections.sort(countries);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                activity,
                android.R.layout.simple_spinner_item,
                countries
        );

        counteryOfSpinner.setAdapter(adapter);

        AlertDialog.Builder addAlert = new AlertDialog.Builder(context);
        addAlert.setTitle("Add Alert");
        addAlert.setView(addAlertView);
        addAlert.setCancelable(false);


        addAlert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

//                    Toast.makeText(getContext(),"Cancel",Toast.LENGTH_SHORT).show();
            }
        });

        addAlert.setPositiveButton("Done", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //TODO: make a list of favorit countery to get notificaiton
                //TODO: then CRUD operation will be necessery (Right Badi ! Yeah why not?)

                //this true will help me to update the adapter in favourite list actitivty

                SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(context);

                //the spinner return the object of selected index so its my duty to convert
                //There could another way to give spinner just double value I did this way
                selectedMinMagnitude = Double.valueOf(
                        String.valueOf(minMagnitudeSpinner.getSelectedItem()));


                selectedCountery = String.valueOf(counteryOfSpinner.getSelectedItem());

                Log.i(TAG, "onClick: first: "+ selectedMinMagnitude);
                Log.i(TAG, "onClick: first: "+ selectedCountery);
                Log.i(TAG, "onClick: store values" + shPref.getString(FAVOURIT_LIST, "null"));

                if (selectedSearchPlaceLatLng != null || selectedSearchPlaceName != null) {
                    selectedCountery = selectedSearchPlaceName;
                    Log.i(TAG, "place selected entery: " + selectedCountery);
                    Log.i(TAG, "select longitude: " + selectedSearchPlaceLatLng);
                    Log.i(TAG, "select longitude: " + selectedSearchPlaceLatLng.latitude);
                    Log.i(TAG, "select longitude: " + selectedSearchPlaceLatLng.longitude);
                    try {
                        Log.i(TAG, "get country name: " +
                                getCountryName(context,
                                        Double.valueOf(selectedSearchPlaceLatLng.latitude), Double.valueOf(selectedSearchPlaceLatLng.longitude)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                int indexOfFavritList= 0;

                List<FavoriteCountries> favoriteCountriesList;
                //data stored as a json string in shared preferences
                if (shPref.getString(FAVOURIT_LIST, "null").equals("null")) {

                    //for the first time list create
                    favoriteCountriesList = new ArrayList<>();
                    //first entry add as it is
                    favoriteCountriesList.add(new FavoriteCountries(selectedCountery,selectedMinMagnitude));
                    //converted the object list into json and simply jugar to convert into string :D that's a way
                    shPref.edit().remove(FAVOURIT_LIST).putString(FAVOURIT_LIST,new Gson().toJson(favoriteCountriesList).toString()).commit();

                } else {

                    //get the store string from default shared preferance document
                    String favouritListString = shPref.getString(FAVOURIT_LIST, "null");
                    //parse the string into json and return the list after parsing the string
                    favoriteCountriesList = ParseFavoritCountriesJsonUtils.parseJsonInToList(favouritListString);
                    //check if already exist country then don't add country
                    // if magnitude change then update index
                    if ((indexOfFavritList = isExistInList(selectedCountery, favoriteCountriesList)) != -1) {
                        if (favoriteCountriesList.get(indexOfFavritList).getMagnitude() != selectedMinMagnitude) {

                            Log.i(TAG, "onClick: index: "+indexOfFavritList);
                            Log.i(TAG, "onClick: Before Magnitude: "+favoriteCountriesList.get(indexOfFavritList).getMagnitude());

                            favoriteCountriesList.get(indexOfFavritList).setMagnitude(selectedMinMagnitude);

                            Log.i(TAG, "onClick: After Magnitude: "+favoriteCountriesList.get(indexOfFavritList).getMagnitude());
                            Log.i(TAG, "onClick: size of list 1: " + favoriteCountriesList.size());
                            shPref.edit().remove(FAVOURIT_LIST).putString(FAVOURIT_LIST,new Gson().toJson(favoriteCountriesList).toString()).apply();

                        }
                    } else {
                        Log.i(TAG, "onClick: index: "+indexOfFavritList);

                        favoriteCountriesList.add(new FavoriteCountries(selectedCountery, Double.valueOf(selectedMinMagnitude)));
                        shPref.edit().remove(FAVOURIT_LIST).putString(FAVOURIT_LIST,new Gson().toJson(favoriteCountriesList).toString()).apply();
                        Log.i(TAG, "onClick: size of list 1: " + favoriteCountriesList.size());

                    }
                }

                // Log.i(TAG, "onClick: size of list"+favoriteCountries.size());


            }
        });

        AlertDialog dialog = addAlert.create();
        dialog.show();

        // NotificationsUtils.remindUser(getContext());
    }


    private int isExistInList(String selectedCountery, List<FavoriteCountries> country) {

        for (int index = 0; index < country.size(); index++) {
            if (selectedCountery.equals(country.get(index).getCountry())) {
                return index;
            }
        }

        return -1;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Find a reference to the {@link ListView} in the layout
        // Inflate the layout for this fragment
        // To get the referance we don't have findviewbyId method in fragment so we use view

        View view = inflater.inflate(R.layout.fragment_home, container, false);


        //Mapfragment
        SupportMapFragment mapFragment = (SupportMapFragment) this.getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //cast (TextView) is redendent
        todayEarthquakes = view.findViewById(R.id.magnitude);
        yesterdayEarthquakes = view.findViewById(R.id.yesterday);
        thisWeekEarthquakes = view.findViewById(R.id.week);
        thisMonthEarthquakes = view.findViewById(R.id.month);
        todayMapStatus = view.findViewById(R.id.today_mapStatus);

        //Find a reference to the {@link Progressbar} int the layout
        progressBar = view.findViewById(R.id.progressbar);

        // java.util.calender class
        // REFERENCE: https://stackoverflow.com/questions/3747490/android-get-date-before-7-days-one-week

        // Date of 7 days back
        Date newDateForWeek = getDate(7);
//        Log.i(TAG, "Seven day back " + newDateForWeek);

        //Made the 1st date of this month
        Calendar calendar1 = Calendar.getInstance();
        calendar1.set(Calendar.DAY_OF_MONTH, 1);
        Date newDateForMonth = calendar1.getTime();
//        Log.i(TAG, "Month date " + newDateForMonth);

        //Date for previous day
        Date newDateForYesterday = getDate(1);
//        Log.i(TAG, "newDateForYesterday: " + newDateForYesterday);

        String yesterdayDate = DataProviderFormat.getformateDate(newDateForYesterday);
        String weekDate = DataProviderFormat.getformateDate(newDateForWeek);
        String monthDate = DataProviderFormat.getformateDate(newDateForMonth);

//        Log.i(TAG, "newDateForYesterday  in Format: " + yesterdayDate);

        /**
         * This is very importanted to be notice why I'm just extracting dates and concatenating with this
         * URL somthing odd right! Badi did this bcz in USA I don't know why the USGS considering the 'allday'
         * as from 5am to 5am e.g 2017-12-12 05:00 am to 2017-12-13 05:00 am.
         *
         * Experiment on this link: https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_day.geojson
         * Date: 2018-01-03
         * Website used to convert timestamp: https://www.epochconverter.com/batch#results
         * Comparision Values: 1st index of  features-> [index 1] property -> [index 3] = time
         *                      (Timestamp=1514956505260  Converted into  Date=2018-01-03 05:15:05)
         *                      Last index of  features-> [index 1] property -> [index 2] = time
         *                      Timestamp=1514870465160 Converted into 2018-01-02 05:21:05
         *
         * Conclusion: The purpose is to show the user today its mean 12:00 am to 12:00 am
         *              So, the values will be get as an instance from the java class fro cell phone (correct me if
         *              im wrong) and then it I concatenate with URL and fetch TODAY earthquakes! :)
         * */

        //today date concatenate url string
        countURLS[0] = CountURL + todayDate;
        Log.i(TAG, "onCreateView: todayCount: " + countURLS[0]);


        //yesterday date concatenate url string
        countURLS[1] = CountURL + yesterdayDate;

        //week concatenate url string
        countURLS[2] = CountURL + weekDate;

        //month concatenate url string
        countURLS[3] = CountURL + monthDate;

        //get the counts eathquakes
        dataFetch(countURLS);
        displayProgressBar(true);


        //Show today Earthqukes on the home map


        //My location
        fusedLocationProviderClient = new FusedLocationProviderClient(getContext());



      createHandlerMessage();




        //TODO:If time allow me I will add some more information on click the circles
        //Add listeners to the Circles and Textviews
//        todayEarthquakes.setOnClickListener(showDataList);
//        yesterdayEarthquakes.setOnClickListener(showDataList);
//        thisWeekEarthquakes.setOnClickListener(showDataList);
//        thisMonthEarthquakes.setOnClickListener(showDataList);

        return view;
    }

    private void createHandlerMessage() {
        // After fetching the number of earthquakes set in the views
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {

                //TODO: If net is not available handle this in network package
                //But progressbar issue?

                Bundle bundle = msg.getData();
                int countKey[] = bundle.getIntArray(COUNT_KEY);

                for (int a = 0; a < countKey.length; a++) {
                    COUNT_KEY_ARRAY[a] = String.valueOf(countKey[a]);
                }

                //set the number of earthquakes
                todayEarthquakes.setText(COUNT_KEY_ARRAY[0]);
                yesterdayEarthquakes.setText(COUNT_KEY_ARRAY[1]);
                thisWeekEarthquakes.setText(COUNT_KEY_ARRAY[2]);
                thisMonthEarthquakes.setText(COUNT_KEY_ARRAY[3]);

                // Log.i(TAG, "month earthquakes: "+COUNT_KEY_ARRAY[3]);
                todayMapStatus.setText("Today " + COUNT_KEY_ARRAY[0] + " Earthquakes occure");
                //Stop the progressbar and hide
                displayProgressBar(false);
            }
        };
    }

    //TODO: Data fetch time out then hide progress bar and show notificaiton
    private void displayProgressBar(boolean visiblity) {
        if (visiblity) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    //TODO: We can move datafetch method as class in network package
    //count the earthquakes
    private void dataFetch(final String[] URLS) {

        //start the progressbar and visible

        final int[] count = new int[URLS.length];
        Runnable runnable = new Runnable() {
            @Override
            public void run() {

//                Log.i(TAG, "dataFetch: thread started");

                for (int a = 0; a < count.length; a++) {


//                    Log.i(TAG, "run: counting");
                    String jasonStr = EarthquakeLoader.HttpHandler.makeServeiceCall(URLS[a]);
//                    Log.i(TAG, "request send");
                    //if the internet available and the jason data receive in jasonStr then
                    if (jasonStr != null) {

                        //get json string values as an object
                        JSONObject root = null;

                        try {
                            root = new JSONObject(jasonStr);
                            count[a] = root.getInt("count");
                            // Log.i("COUNT", count[0] + "");


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                }

                //if today is and yesterday result get empty then show previous data
                if (count[0] == 0 && count[1] == 0) {
                    //TODO:previoud sharedpreference data
                }

                //set the text of quakes count
                Message message = new Message();
                Bundle bundle = new Bundle();
                bundle.putIntArray(COUNT_KEY, count);
                message.setData(bundle);

                if(handler!=null){
                    handler.sendMessage(message);
                }else{
                    createHandlerMessage();
                    handler.sendMessage(message);
                }

//                Log.i(TAG, "dataFetch: send back");

            }
        };

        Thread th = new Thread(runnable);
        th.start();

//        Log.i(TAG, "thread started ");
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(final GoogleMap googleMap) {


        alldayList = new ArrayList<>();
        AsyncTask background;

        //Going to get the today earthquakes
        background = new AsyncTask() {


            @Override
            protected Object doInBackground(Object[] objects) {


                Log.i(TAG, "going to get home json response");
                //Why I didn't do this to count the today earthquake give to count well it was possible but I do this way
                //Today URL of the earthquakes
                //Get the today date 00:00  to today date 23:59 furthur could understand in getDateFormatedURL method
                String jsonResponse = EarthquakeLoader.HttpHandler.makeServeiceCall(
                        TimelineFragment.getDateFormatedURL(todayDate, todayDate));

                Log.i(TAG, "json response of home: "+jsonResponse);

                // Log.i(TAG, "doInBackground: json " + jsonResponse.toString());
                ParseUSGSJsonUtils parseUSGSJsonUtils = new ParseUSGSJsonUtils();
                alldayList = parseUSGSJsonUtils.parseJsonIntoData(alldayList, jsonResponse, getContext());
                // Log.i(TAG, "doInBackground: " + alldayList.size());

                Log.i(TAG, "doInBackground: of maps mark working" + alldayList.size());
                //if I return null then alldayList on main thread null
                return null;


            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                if (alldayList != null) {
                    for (int a = 0; a < alldayList.size(); a++) {
                        LatLng location = new LatLng(alldayList.get(a).getLatitude(), alldayList.get(a).getLongitude());

                        googleMap.addMarker(new MarkerOptions()
                                .position(location)
                                .title(alldayList.get(a).getCityname())
                                .snippet("Magnitude:" + alldayList.get(a).getMagnitude() +
                                        " Date:" + DataProviderFormat.getformateDate(
                                        new Date(
                                                Long.valueOf(alldayList.get(a).getTimeStamp())
                                        ))
                                )
                        );
                        Log.i(TAG, "onMapReady: calling");
                    }
                }

            }
        };

        background.execute();


/*

        googleMap.addCircle(new CircleOptions().center(new LatLng(31,72))
                .radius(2000)
                .fillColor(Color.RED)
                .strokeWidth(100));
*/

        Log.i(TAG, "onMapReady: and size of alldayList: " + alldayList.size());

        //Previously I got the today earthquakes now I'm gonna add marks on the map

        //TODO: give user facility in the future to set differen to check different maps

        /* googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        */
    }

    public Date getDate(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -day);
        Date newDateForWeek = calendar.getTime();
        return newDateForWeek;
    }

    //Life Cycle Methods
    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart: ");
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.i(TAG, "onResume: ");
    }

    @Override
    public void onStop() {
        super.onStop();

       // editor.putString(FAVOURIT_LIST, new Gson().toJson(favoriteCountries).toString());
       // editor.apply();

        Log.i(TAG, "onStop: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "onDestroy: ");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.i(TAG, "onSaveInstanceState: ");
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.i(TAG, "onPause: ");
    }

    @Override
    public void onDetach() {
        super.onDetach();

        Log.i(TAG, "onDetach: ");
    }

}
