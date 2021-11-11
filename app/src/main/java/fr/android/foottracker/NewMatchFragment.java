package fr.android.foottracker;

import android.os.Build;
import static android.content.ContentValues.TAG;
import static android.content.Context.LOCATION_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;

import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import fr.android.foottracker.database.DAO.GameDAO;
import fr.android.foottracker.database.DAO.TeamDAO;
import fr.android.foottracker.models.Game;
import fr.android.foottracker.models.Team;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.Locale;

public class NewMatchFragment extends Fragment implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private LocationManager lm;
    Button btnShowAddress;
    TextView tvAddress;
    Spinner spinnerTeam1;
    Spinner spinnerTeam2;
    String teamName1;
    String teamName2;
    String date;
    String localisation;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        try {
            View view = inflater.inflate(R.layout.fragment_newmatch, container, false);
            initializeComponents(view);
            Button button = (Button) view.findViewById(R.id.createMatchButton);
            button.setOnClickListener(this::createGame);
            SupportMapFragment supportMapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.google_map);

            supportMapFragment.getMapAsync(this);

            View mapView = supportMapFragment.getView();
            mapView.post(new Runnable() {
                @Override
                public void run() {
                    int width = mapView.getMeasuredWidth();
                    int height = mapView.getMeasuredHeight();

                    String widthAndHeight = width + " " + height;
                }
            });

            lm = (LocationManager) view.getContext().getSystemService(LOCATION_SERVICE);
            tvAddress = view.findViewById(R.id.localisation);

            return view;

        }catch (Exception e) {
            Log.e(TAG, "onCreateView", e);
            throw e;
        }

    }

    @SuppressLint("MissingPermission")
    public void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15000, 10, this);
        }
        else {
            ActivityCompat.requestPermissions(this.getActivity(), new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },1);
        }
    }


    public void onPause() {
        super.onPause();

        // 4- unregister from the service when the activity becomes invisible
        lm.removeUpdates(this);
    }


    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

    }


    public void onLocationChanged(@NonNull Location location) {
        // 3- received a new location from the GPS
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        String result_ville;
        String result_postal;
        String result_country;

        // Add a marker and move the camera
        LatLng newPos = new LatLng(lat, lng);
        mMap.addMarker(new MarkerOptions().position(newPos));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(newPos));

        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        String result = null;
        try {
            List<Address> addressList = geocoder.getFromLocation(
                    lat, lng, 1);
            if (addressList != null && addressList.size() > 0) {
                Address address = addressList.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    sb.append(address.getAddressLine(i)).append("\n");
                }
                sb.append(address.getLocality()).append("\n");
                sb.append(address.getPostalCode()).append("\n");
                sb.append(address.getCountryName());
                result = sb.toString();
                System.out.println(result);
                tvAddress.setText(result);
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable connect to Geocoder", e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createGame(View view){
        date = "29-12-1997";
        localisation = "6 rue jytrstyd";
        Game gameToCreate = new Game(0, teamName1, teamName2, date, localisation);
//        System.out.println("before creating a game");
        int newGame = new GameDAO().create(gameToCreate); //Save Game in database and return id of created game

        //Open Statistics Fragment and pass arguments
        Bundle bundle = new Bundle();
        bundle.putInt("idGame", newGame);
        bundle.putString("teamName1", teamName1);
        bundle.putString("teamName2", teamName2);

        StatisticsFragment fragmentStatistics = new StatisticsFragment();
        fragmentStatistics.setArguments(bundle);
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragmentStatistics);
        fragmentTransaction.commit();
    }

    private void initializeComponents(View view) {
         spinnerTeam1 = (Spinner) view.findViewById(R.id.spinnerTeam1);
         spinnerTeam2 = (Spinner) view.findViewById(R.id.spinnerTeam2);

        try{
            Callable<List<Team>> callable = () -> new TeamDAO().getAll();
            List<Team> teamList = callable.call();
            ArrayList<String> teamNameList = new ArrayList<>();

            for (Team t : teamList) {
                teamNameList.add(t.getName());
                System.out.println(t.getName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, teamNameList);

            // Layout for All ROWs of Spinner.  (Optional for ArrayAdapter).
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTeam1.setAdapter(adapter);
            spinnerTeam2.setAdapter(adapter);
            spinnerTeam1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view,
                                           int position, long id) {
                    Object item = adapterView.getItemAtPosition(position);
                    if (item != null) {
                        teamName1 = item.toString();
                    }
                }

               @Override
               public void onNothingSelected(AdapterView<?> adapterView) {

               }

            });
            spinnerTeam2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view,
                                           int position, long id) {
                    Object item = adapterView.getItemAtPosition(position);
                    if (item != null) {
                        teamName2 = item.toString();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }

            });


        } catch (Exception e) {
                e.printStackTrace();
            }

    }


}

