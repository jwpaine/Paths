package simplify.paths;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.SupportMapFragment;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Map extends AppCompatActivity {



    private String pathName;
    private PathConnector pathConnector;
    private List<List> path;
    private PathView pathView;



    /* GPS */

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;


    private Switch recordingSwitch;
    private ArrayList<String> list1;
    private ArrayAdapter<String> adapter;
    private TextView pathText;

    /* Position */
    private static final int MINIMUM_TIME = 1000;  // 1s
    private static final int MINIMUM_DISTANCE = 0; // 50m

    private ListView lstView;

    private ScheduledExecutorService scheduleTaskExecutor;

    private com.mapbox.mapboxsdk.maps.MapView mapView;

    private TextView distance;
    private TextView lastupdated;
    private TextView speed;
    private DecimalFormat df;
    private Button centerButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.eyJ1IjoibWRpYm91bmQiLCJhIjoiY2ozbWVneTJ2MDAwajJxbXY2cmVlZDR3ZCJ9.a8ZnWekGMURuDmaN69KBCA");
        setContentView(R.layout.activity_path_ui2);
        mapView = (com.mapbox.mapboxsdk.maps.MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        distance = (TextView) findViewById(R.id.distanceText);
        lastupdated = (TextView) findViewById(R.id.lastUpdatedText);
        speed = (TextView) findViewById(R.id.speedText);

        /* init pathConnector to interface with DB */
        pathConnector = new PathConnector(this);

        df = new DecimalFormat();
        df.setMaximumFractionDigits(2);



     //   get path name from parent if null, add new path */
        if (getIntent().getStringExtra("pathName") == null) {
            addNewPath();
        } else {
            pathName = getIntent().getStringExtra("pathName");
        }

        /* drawmap, centering on location, on initial start */


        updateUI(true);




          /* setup location manager */
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        getSystemService(Context.LOCATION_SERVICE);
        /* init pathConnector to interface with DB */
        pathConnector = new PathConnector(this);
        recordingSwitch = (Switch) findViewById(R.id.Recording);
        centerButton = (Button) findViewById(R.id.centerButton);




        /* LISTENERS AND Scheduled Executor Services ==================================================== */

        /* updateUI */
        centerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                centerMap();
            }
        });



        recordingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                if(isChecked){
                     /* request permission */
                    checkPermissionAndRecord();
                    recordingSwitch.setText("On");

                }else{
                    mLocationManager.removeUpdates(mLocationListener);
                    recordingSwitch.setText("Off");


                }

            }
        });

        scheduleTaskExecutor = Executors.newScheduledThreadPool(5);

        // This schedule a task to run every 10 minutes:
        scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
            public void run() {

                // If you need update UI, simply do this:
                runOnUiThread(new Runnable() {
                    public void run() {
                        // update your UI component here.
                        //     updateList();
                        updateUI(false);
                    }
                });
            }
        }, 0, 3, TimeUnit.SECONDS);







    }

    public List<List> getPath() {
        return this.path;
    }





    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    public void drawMap(boolean center) {
        final boolean cent = center;
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {


                /* center map to current location if true */
                if (cent) {
                    mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(lastLon(), lastLat()))  // set the camera's center position
                                    .zoom(14)  // set the camera's zoom level
                                    .tilt(20)  // set the camera's tilt
                                    .build()));
                }





                List path = pathConnector.getPath(pathName);

                ArrayList<LatLng> points = new ArrayList<>();
                PolylineOptions polylineOptions = new PolylineOptions();
                for (int i = 0; i < path.size(); i++) {



                    List coord = (List) path.get(i);
                    /* @TODO: figure out why 0.0 is being added to begin with */
                    if ((double) coord.get(0) == 0.0) continue;


                    points.add(new LatLng((double) coord.get(1), (double) coord.get(0)));

                    System.out.println("(" + coord.get(1) + "," + coord.get(0) + ")");



                }


            // create new PolylineOptions from all points

                polylineOptions.addAll(points)
                        .color(Color.BLUE)
                        .width(3f);

                // add polyline to MapboxMap object
                mapboxMap.addPolyline(polylineOptions);

            }


        });
    }

    public void centerMap() {
        System.out.println("Centering Map");
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {


                /* center map to current location if true */
                    mapboxMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(new LatLng(lastLon(), lastLat()))  // set the camera's center position
                                    .zoom(14)  // set the camera's zoom level
                                    .tilt(20)  // set the camera's tilt
                                    .build()));
                }
        });
    }
    public void updateUI(boolean centerMap) {

        /*
        list1.add("Distance: " + String.valueOf(pathConnector.pathLength(pathName)));
            // add last date added
        SimpleDateFormat dateFormate = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss");
        String stringDate = dateFormate.format(new Date(pathConnector.lastTime(pathName)));
        list1.add("Last updated: " + stringDate);
        list1.add("Average Velocity (t minutes): " + pathConnector.averageVelocity(pathName));
        adapter.notifyDataSetChanged();

        */

        if (pathName != null) {
            drawMap(centerMap);
            /* set path length */

            double d = pathConnector.pathLength(pathName);
            distance.setText(String.valueOf( df.format(d)));
            /* set speed */
            double t = pathConnector.averageVelocity(pathName);
            speed.setText(String.valueOf(df.format(t)));
            /* set last updated date/time */
            DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy - hh:mm:ss");
            lastupdated.setText(dateFormat.format(pathConnector.lastTime(pathName)));
        }

    }

    public void updateList() {
        System.out.println("Updating List");
        if (pathName != null) {
            list1.clear();
            /* add path length */
            list1.add("Distance: " + String.valueOf(pathConnector.pathLength(pathName)));
            /* add last date added */
            SimpleDateFormat dateFormate = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss");
            String stringDate = dateFormate.format(new Date(pathConnector.lastTime(pathName)));
            list1.add("Last updated: " + stringDate);
            list1.add("Average Velocity (t minutes): " + pathConnector.averageVelocity(pathName));
            adapter.notifyDataSetChanged();
        }
    }

    public void checkPermissionAndRecord() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }



    public void addNewPath() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Path Name");
        builder.setCancelable(false);

        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                pathName = input.getText().toString();
                Log.d("Map->addNewPath", "creating path: " + pathName);
                pathConnector.createPath(pathName);


            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                finish();
            }
        });

        builder.show();




    }

    public double lastLat() {
         /* get last recorded location */
        return pathConnector.lastLocation(pathName)[0];
    }
    public double lastLon() {
         /* get last recorded location */
        return pathConnector.lastLocation(pathName)[1];
    }

    public void deletePath(View view) {


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Really delete path?");

        // Set up the buttons
        builder.setPositiveButton("Yes, Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("Map->deletePath", "Deleting path: " + pathName);
                 /* use dialog for confirmation!! */
                pathConnector.removePath(pathName);
                terminateRunnables();
                finish();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }



    public void showMap(View view) {

        if (pathName != null) {
            System.out.println("Showing Map...");
            Intent intent = new Intent(this, Map.class);
            intent.putExtra("pathName", pathName);
            startActivity(intent);
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        System.out.println("Checking Permissions...");
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    System.out.println("Granted");

                    /* setup location listener */
                    mLocationListener = new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {


                            if (pathName != null) {
                                System.out.println("Adding point to path: " + pathName);
                                pathConnector.addPoint(pathName, location.getLongitude(), location.getLatitude());
                            }

                        }



                        @Override
                        public void onStatusChanged(String provider, int status, Bundle extras) {

                        }

                        @Override
                        public void onProviderEnabled(String provider) {

                        }

                        @Override
                        public void onProviderDisabled(String provider) {

                        }
                    };

                    mLocationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, MINIMUM_TIME, MINIMUM_DISTANCE, mLocationListener);


                } else {
                    System.out.println("Denied...");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /* finish activity when backbutton is pressed */
    @Override
    public void onBackPressed() {

        terminateRunnables();

        this.finish();
    }

    public void terminateRunnables() {
        // if locationListener is active, remove updates when back button is pressed
        if (mLocationListener != null) mLocationManager.removeUpdates(mLocationListener);

        scheduleTaskExecutor.shutdown();
    }



}


