package simplify.paths;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * PathConnector abstracts away the underlying complexity of reading/writing path data from/to local SQL database,
 */

public class PathConnector extends SQLiteOpenHelper {


    private int pathID;

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "pathsdb";

    // Contacts table name
    private static final String TABLE_PATHS = "paths";

    // Contacts Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "path_name";
    private static final String KEY_LON = "lon";
    private static final String KEY_LAT = "lat";
    private static final String KEY_DATE = "date";
    private static final String KEY_DISTANCE = "distance";

    public PathConnector(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    /* update path pathID with point (lon,lat) */
    public void addPoint(String name, double lon, double lat) {

        if (lon == 0 || lat == 0) return;

        System.out.println("Adding new point for path: "+ name + "(" + lat + "," + lon + ")");
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name); // Contact Name
        values.put(KEY_LON, lon); // Contact Phone Number
        values.put(KEY_LAT, lat); // Contact Phone Number
        values.put(KEY_DATE, System.currentTimeMillis()); // Contact Phone Number
        values.put(KEY_DISTANCE, pathLength(name));

        // Inserting Row
        db.insert(TABLE_PATHS, null, values);
        db.close(); // Closing database connection

    }

    public void createPath(String name) {
        /* TODO: create only if not exist */
        Log.d("PathConnector", "createPath: " + name);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name); // Contact Name

        // Inserting Row
        db.insert(TABLE_PATHS, null, values);
        db.close(); // Closing database connection

    }

    /* return a list of all path names */
    public List getPaths() {
        List<String> pathList = new ArrayList<String>();

        String selectQuery = "SELECT  * FROM " + TABLE_PATHS + " GROUP BY " + KEY_NAME;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(1);
                pathList.add(name);
            } while (cursor.moveToNext());
        }
        // return contact list
        return pathList;
    }

    /* return a list of all points in path */
    public List getPath(String name) {
        List<List> pathList = new ArrayList<List>();

     //   String selectQuery = "SELECT * FROM " + TABLE_PATHS + " WHERE " + KEY_NAME + "=" + name;

        String selectQuery = "SELECT * FROM " + TABLE_PATHS + " WHERE " + KEY_NAME + " ='" + name +"'";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
             //   String lon = cursor.getString(2);
             //   String lat = cursor.getString(3);
                double lon = cursor.getDouble(2);
                double lat = cursor.getDouble(3);
                pathList.add(Arrays.asList(lon, lat));
            } while (cursor.moveToNext());
        }


        // return contact list
        return pathList;
    }

    /* return velocity to traverse last 3 entries, given time stamps for each */
    public double averageVelocity(String name) {

        String selectQuery = "SELECT * FROM " + TABLE_PATHS + " WHERE " + KEY_NAME + " ='" + name +"'"
                + " ORDER BY " + KEY_ID + " DESC LIMIT 5";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);



        long time_min = 0;
        long time_max = 0;
        double distance_min = 0;
        double distance_max = 0;

        if (cursor.moveToFirst()) {


            distance_max = cursor.getDouble(5);



            do {

                distance_min = cursor.getDouble(5);


                if (cursor.getLong(4) > time_max) {
                    time_max = cursor.getLong(4);
                } else {
                    time_min = cursor.getLong(4);
                }





            } while (cursor.moveToNext());
        }


        SimpleDateFormat dateformate = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss");
        String  date_min = dateformate.format(time_min);
        System.out.println("min Date collected: " + date_min);
        String  date_max = dateformate.format(time_max);
        System.out.println("max Date collected: " + date_max);

      //  System.out.println("Change in position: " + String.valueOf(distance_max - distance_min));
        System.out.println("Change in time (ms): " + String.valueOf(time_max - time_min));

        System.out.println("distance_max: " + distance_max + ", distance_min: " + distance_min);
        double time = (time_max - time_min) / 1000;
        return ((distance_max - distance_min) / (time_max - time_min))*1000*60*60;

    }

    /* return last recorded location */
    public double[] lastLocation(String name) {

        double[] location = new double[2];

        String selectQuery = "SELECT * FROM " + TABLE_PATHS + " WHERE " + KEY_NAME + " ='" + name +"'"
                + " ORDER BY " + KEY_ID + " DESC LIMIT 1";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                double lon = cursor.getDouble(2);
                double lat = cursor.getDouble(3);
                location[0] = cursor.getDouble(2);
                location[1] = cursor.getDouble(3);
            } while (cursor.moveToNext());
        }


        System.out.println("Last recorded location: " + String.valueOf(location[0]) + "," + String.valueOf(location[1]));

        // return contact list
        return location;

    }

    /* return last recorded location */
    public long lastTime(String name) {


        String selectQuery = "SELECT * FROM " + TABLE_PATHS + " WHERE " + KEY_NAME + " ='" + name +"'"
                + " ORDER BY " + KEY_ID + " DESC LIMIT 1";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list

        if (cursor.moveToFirst()) {
            do {
                long longDate = cursor.getLong(4);
                return longDate;
            } while (cursor.moveToNext());
        }

        return 0;
    }

    public int totalPoints(String name) {
        return 0;
    }

    public double pathLength(String name) {
        List<List> pathList = this.getPath(name);

        if (pathList.size() < 2) return 0; // if less than two points, return 0 distance

        double distance = 0;

        for (int i = 1; i < pathList.size(); i++) {
            double lat0 = (double) pathList.get(i-1).get(0);
            double lon0 = (double) pathList.get(i-1).get(1);
            double lat1 = (double) pathList.get(i).get(0);
            double lon1 = (double) pathList.get(i).get(1);

            if (lat0 == 0 || lon0 == 0) continue;

            distance +=  pointDistance(lat0, lon0, lat1, lon1);
        //    System.out.println("points: (" + lat0 + "," + lon0 + ") and (" + lat1 + "," + lon1 + ") Distance: " +  pointDistance(lat0, lon0, lat1, lon1));

        }
        System.out.println("Total Distance: " + distance);
        return distance;
    }


    public double pointDistance(double lat0, double lon0, double lat1, double lon1) {

        double EARTH_KM = 6371;
        double latDistance = Math.toRadians(lat0 - lat1);
        double lonDistance = Math.toRadians(lon0 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat0)) * Math.cos(Math.toRadians(lat1)) *
                        Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2*Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return c*EARTH_KM;

    }

    /* remove path from database */
    public void removePath(String name) {

        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PATHS, KEY_NAME + " = ?", new String[] { name });
        db.close();;

    }

    public int getPathID() {
        return pathID;
    }

    /* return path length */
    public double length() {
        return 0;
    }

    /* return time elapsed */
    public double timeElapsed() {
        return 0;
    }

    /* return start time */
    public double startTime() {
        return 0;
    }


    @Override
    public void onCreate(SQLiteDatabase db) {

        System.out.println("Creating Database for first time use");

        String CREATE_PATHS_TABLE = "CREATE TABLE " + TABLE_PATHS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT,"
                + KEY_LON + " REAL," + KEY_LAT + " REAL," + KEY_DATE + " REAL," + KEY_DISTANCE + " REAL )";



        db.execSQL(CREATE_PATHS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PATHS);

        // Create tables again
        onCreate(db);
    }


}
