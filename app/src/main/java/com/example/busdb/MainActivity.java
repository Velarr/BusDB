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


    private List<Route> routeList = new ArrayList<>();
    private Route selectedRoute;
    private String companyId = null;
    private String currentUid = null;
    private String driverName = null;
    private long startTimeMillis = 0;
    private LocationCallback locationCallback;


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
            if (firestoreId == null || number == -1) {
                return "Selecione uma rota...";
            }
            return number + " - " + name;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // Importante para o menu funcionar

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
                            String nome = doc.getString("name");
                            driverName = nome;
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
            if (selectedRoute == null || selectedRoute.firestoreId == null || selectedRoute.number == -1) {
                statusTextView.setText("Por favor, selecione uma rota válida.");
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
        inflater.inflate(R.menu.menu_main, menu); // Garante que o ícone de logout apareça
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

                    // Primeiro item fake
                    Route emptyRoute = new Route(null, "Selecione uma rota...", -1, "");
                    routeList.add(emptyRoute);

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

                    // Ordena os itens reais e mantém o item 0 como "Selecione..."
                    routeList.subList(1, routeList.size()).sort((r1, r2) -> Integer.compare(r1.number, r2.number));

                    ArrayAdapter<Route> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, routeList);
                    routeSpinner.setAdapter(adapter);

                    // Nenhuma rota real selecionada por padrão
                    selectedRoute = null;
                })
                .addOnFailureListener(e ->
                        statusTextView.setText("Erro ao carregar rotas: " + e.getMessage()));
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
        startTimeMillis = System.currentTimeMillis();

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setFastestInterval(3000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    uploadLocationToFirebase(location);
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());

        startButton.setVisibility(View.GONE);
        stopButton.setVisibility(View.VISIBLE);
        routeSpinner.setEnabled(false);
    }


    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }

        long endTimeMillis = System.currentTimeMillis();
        long durationMillis = endTimeMillis - startTimeMillis;

        long seconds = (durationMillis / 1000) % 60;
        long minutes = (durationMillis / (1000 * 60)) % 60;
        long hours = (durationMillis / (1000 * 60 * 60));
        String duracao = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        String horaInicio = formatTime(startTimeMillis);
        String horaFim = formatTime(endTimeMillis);
        String dataDia = getCurrentDateKey();

        Map<String, Object> tempoData = new HashMap<>();
        tempoData.put("horaInicio", horaInicio);
        tempoData.put("horaFim", horaFim);
        tempoData.put("duracao", duracao);
        tempoData.put("rota", selectedRoute.name);
        tempoData.put("idRota", selectedRoute.firestoreId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Cria o documento do dia com um campo vazio para garantir que ele exista
        db.collection("drivers")
                .document(currentUid)
                .collection("time")
                .document(dataDia)
                .set(new HashMap<String, Object>() {{
                    put("exists", true);
                }})
                .addOnSuccessListener(aVoid -> {
                    // Agora salva a viagem na subcoleção
                    db.collection("drivers")
                            .document(currentUid)
                            .collection("time")
                            .document(dataDia)
                            .collection("viagens")
                            .add(tempoData)
                            .addOnSuccessListener(docRef -> {
                                statusTextView.setText("Viagem salva com duração: " + duracao);
                            })
                            .addOnFailureListener(e -> {
                                statusTextView.setText("Erro ao salvar tempo: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    statusTextView.setText("Erro ao preparar o documento do dia: " + e.getMessage());
                });

        if (firebaseLocationRef != null) {
            firebaseLocationRef.removeValue()
                    .addOnCompleteListener(task -> {
                        stopButton.setVisibility(View.GONE);
                        startButton.setVisibility(View.VISIBLE);
                        routeSpinner.setEnabled(true);
                    });
        }
    }


    private void uploadLocationToFirebase(Location location) {
        if (selectedRoute == null || driverName == null) {
            statusTextView.setText("Informações insuficientes para envio.");
            return;
        }

        // Calcular tempo decorrido
        long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
        long seconds = (elapsedMillis / 1000) % 60;
        long minutes = (elapsedMillis / (1000 * 60)) % 60;
        long hours = (elapsedMillis / (1000 * 60 * 60));
        String tempo = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        Map<String, Object> data = new HashMap<>();
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());
        data.put("id", selectedRoute.firestoreId);
        data.put("condutor", driverName);
        data.put("tempo", tempo); // <-- novo campo

        firebaseLocationRef.setValue(data)
                .addOnSuccessListener(aVoid -> statusTextView.setText("Tempo: " + tempo))
                .addOnFailureListener(e -> statusTextView.setText("Erro ao enviar: " + e.getMessage()));
    }

    private String formatTime(long millis) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(millis));
    }

    private String getCurrentDateKey() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
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
