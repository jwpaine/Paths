package simplify.paths;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    /* GPS Constant Permission */
    private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;

    /* Position */
    private static final int MINIMUM_TIME = 0;  // 10s
    private static final int MINIMUM_DISTANCE = 0; // 50m

    /* GPS */
    private String mProviderName;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    private boolean isGPSEnabled;
    private boolean isNetworkEnabled;
    private boolean canGetLocation;

    private double latitude;
    private double longitude;

    private Location location;

    private PathConnector pathConnector;
    private ArrayList<String> list1;
    private ArrayList<String> list2;

    private NameArrayAdapter nameAdapter;
    private DistanceArrayAdapter distanceAdapter;


    private String currentPathName;

    private Intent i;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* init new path */
        pathConnector = new PathConnector(this);


        final ListView lstView1 = (ListView) findViewById(R.id.listview);
        final ListView lstView2 = (ListView) findViewById(R.id.listview2);


        list1 = new ArrayList<String>();
        list2 = new ArrayList<String>();

        lstView1.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lstView1.setTextFilterEnabled(true);
/// ========= not working
        nameAdapter = new NameArrayAdapter(this,android.R.layout.simple_selectable_list_item, list1);
        distanceAdapter = new DistanceArrayAdapter(this,android.R.layout.simple_selectable_list_item, list2);

        lstView1.setAdapter(nameAdapter);
        lstView2.setAdapter(distanceAdapter);

        updateListFromDB();


        /* startPath on list item click */
        lstView1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                currentPathName = nameAdapter.getItem(position);
                System.out.println("currentPathName: " +currentPathName);
                // examine distance for points
                pathConnector.pathLength(currentPathName);
                startPath(currentPathName);
            }
        });



  /*      lstView1.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                lstView2.setSelection(firstVisibleItem);//force the right listview to scrollyou may have to do some calculation to sync the scrolling status of the two listview.
            }
        }); */


    }

    public void updateListFromDB() {
        list1.clear();
        list2.clear();

        List pathNames = pathConnector.getPaths();
        /* add all pathnames to list */
        for (int i = 0; i < pathNames.size(); i++) {

            /* add names */
            list1.add((String) pathNames.get(i));

            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(3);
            list2.add(df.format(pathConnector.pathLength(  (String) pathNames.get(i)  ) ) + " km");

        }
        nameAdapter.notifyDataSetChanged();
        distanceAdapter.notifyDataSetChanged();
    }

  public void startPath(String pathName) {
      Intent intent = new Intent(this, Map.class);
      intent.putExtra("pathName", pathName);
      /* temp */
 //   pathConnector.addPoint(pathName,1,1);

      startActivityForResult(intent,1);
  }

  /* define new path and start */
  public void newPath(View view) {

    /* add dialog to get new pathName*/

      startPath(null);

  }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            Log.d("onActivityResult", "ConfigurePath activity exited");
            updateListFromDB();

        }
    }



    private class NameArrayAdapter extends ArrayAdapter<String> {


        public NameArrayAdapter(Context context, int textViewResourceId, ArrayList<String> target) {
            super(context, textViewResourceId, target);
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return list1.size();
        }

        @Override
        public String getItem(int position) {
            // TODO Auto-generated method stub

            return list1.get(position);

        }

        @Override
        public int getPosition(String item) {
            // TODO Auto-generated method stub
            return super.getPosition(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }
    private class DistanceArrayAdapter extends ArrayAdapter<String> {


        public DistanceArrayAdapter(Context context, int textViewResourceId, ArrayList<String> target) {
            super(context, textViewResourceId, target);
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return list2.size();
        }

        @Override
        public String getItem(int position) {
            // TODO Auto-generated method stub

            return list2.get(position);

        }

        @Override
        public int getPosition(String item) {
            // TODO Auto-generated method stub
            return super.getPosition(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }














}
