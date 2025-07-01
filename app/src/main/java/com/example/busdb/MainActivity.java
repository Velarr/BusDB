package com.example.busdb;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.AdapterView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private LoginController loginController;
    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference firebaseLocationRef;
    private TextView statusTextView;
    private Button startButton, stopButton;
    private Spinner routeSpinner;

    private Handler handler;
    private Runnable locationRunnable;

    private List<Route> routeList = new ArrayList<>();
    private Route selectedRoute;
    private String companyId = null;
    private String currentUid = null;

    private static class Route {
        String firestoreId;
        String name;
        int number;
        String color;

        Route(String id, String name, int number, String color) {
            this.firestoreId = id;
            this.name = name;
            this.number = number;
            this.color = color;
        }

        @Override
        public String toString() {
            return number + " - " + name;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        loginController = new LoginController();
        loginController.redirectIfNotLoggedIn(this, LoginActivity.class);

        FirebaseApp.initializeApp(this);

        TextView welcomeTextView = findViewById(R.id.tvWelcome);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUid = user.getUid();
            String email = user.getEmail();

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("drivers")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (!query.isEmpty()) {
                            DocumentSnapshot doc = query.getDocuments().get(0);
                            String nome = doc.getString("nome");
                            companyId = doc.getString("companyId");
                            welcomeTextView.setText("Olá " + (nome != null ? nome : "condutor") + "!");
                            if (companyId != null) {
                                carregarRotasPorCompanhia(companyId);
                            }
                        } else {
                            welcomeTextView.setText("Olá condutor!");
                        }
                    })
                    .addOnFailureListener(e -> welcomeTextView.setText("Olá condutor!"));
        }

        statusTextView = findViewById(R.id.tvStatus);
        startButton = findViewById(R.id.btnStartSharing);
        stopButton = findViewById(R.id.btnStopSharing);
        routeSpinner = findViewById(R.id.spinnerLinha);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        routeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRoute = routeList.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedRoute = null;
            }
        });

        startButton.setOnClickListener(v -> {
            if (selectedRoute == null) {
                statusTextView.setText("Por favor, selecione uma rota.");
                return;
            }
            if (hasLocationPermission()) {
                beginLocationUpdates();
            } else {
                requestLocationPermission();
            }
        });

        stopButton.setOnClickListener(v -> stopLocationUpdates());

        stopButton.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            loginController.logout(this, LoginActivity.class);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void carregarRotasPorCompanhia(String companyId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("routes")
                .whereEqualTo("companyId", companyId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    routeList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("routeName");
                        Long numLong = doc.getLong("routeNumber");
                        String color = doc.getString("color");

                        if (name == null || color == null || numLong == null) continue;

                        int number = numLong.intValue();
                        Route route = new Route(id, name, number, color);
                        routeList.add(route);
                    }

                    ArrayAdapter<Route> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, routeList);
                    routeSpinner.setAdapter(adapter);

                    if (!routeList.isEmpty()) {
                        selectedRoute = routeList.get(0);
                    }
                })
                .addOnFailureListener(e -> statusTextView.setText("Erro ao carregar rotas: " + e.getMessage()));
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST);
    }

    private void beginLocationUpdates() {
        statusTextView.setText("Iniciando transmissão de localização...");

        firebaseLocationRef = FirebaseDatabase.getInstance().getReference("locations/" + selectedRoute.name + "/" + currentUid);

        handler = new Handler();
        locationRunnable = new Runnable() {
            @Override
            public void run() {
                requestAndSendLocation();
                handler.postDelayed(this, 5000);
            }
        };

        handler.post(locationRunnable);

        startButton.setVisibility(View.GONE);
        stopButton.setVisibility(View.VISIBLE);
        routeSpinner.setEnabled(false);
    }

    private void stopLocationUpdates() {
        if (handler != null && locationRunnable != null) {
            handler.removeCallbacks(locationRunnable);
        }

        if (firebaseLocationRef != null) {
            firebaseLocationRef.removeValue()
                    .addOnCompleteListener(task -> {
                        statusTextView.setText("Localização parada e removida do Firebase.");
                        stopButton.setVisibility(View.GONE);
                        startButton.setVisibility(View.VISIBLE);
                        routeSpinner.setEnabled(true);
                    });
        }
    }

    private void requestAndSendLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            statusTextView.setText("Permissão de localização não concedida.");
            return;
        }

        statusTextView.setText("A obter localização atual...");

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setNumUpdates(1);

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this);

                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    Location location = locationResult.getLastLocation();
                    statusTextView.setText("Localização atual: " + location.getLatitude() + ", " + location.getLongitude());
                    uploadLocationToFirebase(location);
                } else {
                    statusTextView.setText("Não foi possível obter a localização.");
                }
            }
        }, getMainLooper());
    }



    private void uploadLocationToFirebase(Location location) {
        if (selectedRoute == null) {
            statusTextView.setText("Nenhuma rota selecionada.");
            return;
        }

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("drivers")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(query -> {
                    String nome = "Desconhecido";
                    if (!query.isEmpty()) {
                        nome = query.getDocuments().get(0).getString("nome");
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("latitude", location.getLatitude());
                    data.put("longitude", location.getLongitude());
                    data.put("id", selectedRoute.firestoreId);
                    data.put("condutor", nome);

                    firebaseLocationRef.setValue(data)
                            .addOnSuccessListener(aVoid -> statusTextView.setText("Localização enviada com nome."))
                            .addOnFailureListener(e -> statusTextView.setText("Erro ao enviar: " + e.getMessage()));
                })
                .addOnFailureListener(e -> statusTextView.setText("Erro ao buscar nome: " + e.getMessage()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (selectedRoute != null) {
                beginLocationUpdates();
            }
        } else {
            statusTextView.setText("Permissão de localização negada.");
        }
    }
}
