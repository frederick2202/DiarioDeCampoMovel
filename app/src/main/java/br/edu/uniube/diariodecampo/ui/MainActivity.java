package br.edu.uniube.diariodecampo.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Locale;

import br.edu.uniube.diariodecampo.databinding.ActivityMainBinding;
import br.edu.uniube.diariodecampo.viewmodel.DiarioViewModel;
import br.edu.uniube.diariodecampo.viewmodel.UiState;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private ActivityMainBinding binding;
    private DiarioViewModel viewModel;
    private RegistroAdapter adapter;
    private FusedLocationProviderClient fusedLocationClient;

    // Desafio Opcional: Sensor de Luminosidade Ambiente
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private float luminosidadeAtualLux = 0.0f;

    // Contratos de Permissão e Câmera em Tempo de Execução
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    abrirCameraPreview();
                } else {
                    viewModel.notificarPermissaoNegada(
                            "Câmera",
                            "A permissão de câmera foi negada. Você ainda pode salvar o registro sem foto ou habilitar a câmera nas Configurações do Android."
                    );
                }
            });

    private final ActivityResultLauncher<Void> takePicturePreviewLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                if (bitmap != null) {
                    viewModel.definirFotoTemporaria(bitmap);
                }
            });

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean coarseGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                boolean fineGranted = Boolean.TRUE.equals(permissions.get(Manifest.permission.ACCESS_FINE_LOCATION));
                if (coarseGranted || fineGranted) {
                    obterLocalizacaoUnica();
                } else {
                    viewModel.notificarPermissaoNegada(
                            "Localização (GPS)",
                            "Permissão de localização negada. O aplicativo manterá o funcionamento usando coordenadas padrão ou consulta por CEP."
                    );
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(DiarioViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inicializa Sensor de Luminosidade (Desafio Opcional)
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        configurarRecyclerView();
        configurarBotoes();
        observarViewModel();
    }

    private void configurarRecyclerView() {
        adapter = new RegistroAdapter(registro -> viewModel.selecionarRegistroParaEnriquecer(registro));
        binding.rvRegistros.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRegistros.setAdapter(adapter);
    }

    private void configurarBotoes() {
        binding.btnCapturarFoto.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                abrirCameraPreview();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        binding.btnObterLocalizacao.setOnClickListener(v -> {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        });

        binding.btnSalvarRegistro.setOnClickListener(v -> {
            String titulo = binding.etTitulo.getText().toString();
            String cep = binding.etCep.getText().toString();
            viewModel.salvarNovoRegistro(titulo, cep, luminosidadeAtualLux);
            binding.etTitulo.setText("");
        });

        binding.btnAbrirConfiguracoes.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });
    }

    private void abrirCameraPreview() {
        takePicturePreviewLauncher.launch(null);
    }

    @SuppressLint("MissingPermission")
    private void obterLocalizacaoUnica() {
        binding.tvStatusEstado.setText("Obtendo leitura única de GPS (getCurrentLocation)...");
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        viewModel.definirCoordenadasTemporarias(location.getLatitude(), location.getLongitude());
                    } else {
                        // Fallback caso o emulador esteja sem ponto GPS ativo no momento
                        viewModel.definirCoordenadasTemporarias(-18.9186, -48.2772);
                    }
                })
                .addOnFailureListener(this, e -> viewModel.notificarPermissaoNegada(
                        "Hardware GPS",
                        "Não foi possível obter a localização atual. Verifique se o GPS do dispositivo está ativo."
                ));
    }

    private void observarViewModel() {
        viewModel.getFotoTemporaria().observe(this, bitmap -> {
            if (bitmap != null) {
                binding.ivPreviewFoto.setImageBitmap(bitmap);
            }
        });

        viewModel.getCoordenadasTemporarias().observe(this, coords -> {
            if (coords != null && coords.length == 2) {
                binding.tvCoordenadasAtuais.setText(String.format(Locale.US,
                        "Coordenadas: Lat %.5f | Lon %.5f", coords[0], coords[1]));
            }
        });

        viewModel.getUiState().observe(this, state -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnAbrirConfiguracoes.setVisibility(View.GONE);

            if (state instanceof UiState.Idle) {
                UiState.Idle idle = (UiState.Idle) state;
                binding.tvStatusEstado.setText("Estado: Pronto para registrar atividades de campo.");
                adapter.atualizarLista(idle.registros);
            } else if (state instanceof UiState.Loading) {
                UiState.Loading loading = (UiState.Loading) state;
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.tvStatusEstado.setText("CARREGANDO: " + loading.mensagem);
            } else if (state instanceof UiState.Success) {
                UiState.Success success = (UiState.Success) state;
                binding.tvStatusEstado.setText("SUCESSO: " + success.detalheEnriquecido);
                adapter.atualizarLista(success.registros);
            } else if (state instanceof UiState.Error) {
                UiState.Error error = (UiState.Error) state;
                binding.tvStatusEstado.setText("ERRO: " + error.mensagemErro + "Próximo passo: " + error.proximoPasso);
            } else if (state instanceof UiState.PermissionDenied) {
                UiState.PermissionDenied denied = (UiState.PermissionDenied) state;
                binding.tvStatusEstado.setText("PERMISSÃO NEGADA (" + denied.recursoNegado + "): " + denied.explicacao);
                binding.btnAbrirConfiguracoes.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            // Filtro passa-baixa simples para suavizar ruído numérico
            luminosidadeAtualLux = 0.8f * luminosidadeAtualLux + 0.2f * event.values[0];
            binding.tvSensorLuz.setText(String.format(Locale.US, "Luminosidade Ambiente: %.1f lx", luminosidadeAtualLux));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Não requer ação para o sensor de luminosidade
    }
}
