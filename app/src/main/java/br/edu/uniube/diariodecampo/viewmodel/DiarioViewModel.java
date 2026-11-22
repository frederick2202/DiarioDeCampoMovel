package br.edu.uniube.diariodecampo.viewmodel;

import android.graphics.Bitmap;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.edu.uniube.diariodecampo.model.RegistroCampo;
import br.edu.uniube.diariodecampo.repository.DiarioRepository;

public class DiarioViewModel extends ViewModel {

    private final DiarioRepository repository = new DiarioRepository();
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>();
    private final MutableLiveData<Bitmap> fotoTemporaria = new MutableLiveData<>();
    private final MutableLiveData<double[]> coordenadasTemporarias = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public DiarioViewModel() {
        uiState.setValue(new UiState.Idle(repository.listarRegistros()));
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public LiveData<Bitmap> getFotoTemporaria() {
        return fotoTemporaria;
    }

    public LiveData<double[]> getCoordenadasTemporarias() {
        return coordenadasTemporarias;
    }

    public void definirFotoTemporaria(Bitmap bitmap) {
        fotoTemporaria.setValue(bitmap);
    }

    public void definirCoordenadasTemporarias(double lat, double lon) {
        coordenadasTemporarias.setValue(new double[]{lat, lon});
    }

    public void notificarPermissaoNegada(String recurso, String explicacao) {
        uiState.setValue(new UiState.PermissionDenied(recurso, explicacao));
    }

    public void salvarNovoRegistro(String titulo, String cepOpcional, float luminosidadeLux) {
        if (titulo == null || titulo.trim().isEmpty()) {
            uiState.setValue(new UiState.Error(
                    "Título obrigatório ausente.",
                    "Digite um título descritivo para o registro de campo antes de salvar."
            ));
            return;
        }

        Bitmap foto = fotoTemporaria.getValue();
        double[] coords = coordenadasTemporarias.getValue();
        double lat = (coords != null) ? coords[0] : -18.9186; // Fallback caso sem permissão GPS
        double lon = (coords != null) ? coords[1] : -48.2772;

        String dataAtual = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        RegistroCampo novo = new RegistroCampo(
                UUID.randomUUID().toString(),
                titulo.trim(),
                cepOpcional != null ? cepOpcional.trim() : "",
                dataAtual,
                lat,
                lon,
                luminosidadeLux,
                foto
        );

        repository.adicionarRegistro(novo);
        List<RegistroCampo> listaAtualizada = repository.listarRegistros();
        uiState.setValue(new UiState.Success(
                listaAtualizada,
                "Registro '" + novo.getTitulo() + "' salvo na sessão (" + listaAtualizada.size() + " registro(s) armazenado(s)). Toque em um item da lista para enriquecer via API REST."
        ));
    }

    public void selecionarRegistroParaEnriquecer(RegistroCampo registro) {
        uiState.setValue(new UiState.Loading("Consultando API REST para enriquecer '" + registro.getTitulo() + "'..."));
        executor.execute(() -> {
            try {
                String resultadoApi = repository.enriquecerRegistroComApi(registro);
                List<RegistroCampo> atualizados = repository.listarRegistros();
                uiState.postValue(new UiState.Success(atualizados, resultadoApi));
            } catch (IllegalArgumentException e) {
                uiState.postValue(new UiState.Error(
                        e.getMessage(),
                        "Corrija o CEP informado (use 8 dígitos numéricos válidos) e tente novamente."
                ));
            } catch (Exception e) {
                uiState.postValue(new UiState.Error(
                        "Falha de comunicação com a API: " + e.getMessage(),
                        "Verifique sua conexão Wi-Fi/Dados Móveis e toque novamente no registro."
                ));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdownNow();
    }
}
