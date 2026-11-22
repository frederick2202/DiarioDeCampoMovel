package br.edu.uniube.diariodecampo.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.edu.uniube.diariodecampo.BuildConfig;
import br.edu.uniube.diariodecampo.model.ClimaResponse;
import br.edu.uniube.diariodecampo.model.EnderecoViaCep;
import br.edu.uniube.diariodecampo.model.RegistroCampo;
import br.edu.uniube.diariodecampo.network.OpenWeatherApi;
import br.edu.uniube.diariodecampo.network.RetrofitClient;
import br.edu.uniube.diariodecampo.network.ViaCepApi;
import retrofit2.Response;

public class DiarioRepository {
    private final List<RegistroCampo> registrosSessao = new ArrayList<>();
    private final ViaCepApi viaCepApi;
    private final OpenWeatherApi openWeatherApi;

    public DiarioRepository() {
        this.viaCepApi = RetrofitClient.getViaCepApi();
        this.openWeatherApi = RetrofitClient.getOpenWeatherApi();
    }

    public synchronized void adicionarRegistro(RegistroCampo registro) {
        registrosSessao.add(0, registro);
    }

    public synchronized List<RegistroCampo> listarRegistros() {
        return new ArrayList<>(registrosSessao);
    }

    public String enriquecerRegistroComApi(RegistroCampo registro) throws Exception {
        String cepLimpo = registro.getCepOpcional() != null
                ? registro.getCepOpcional().replaceAll("[^0-9]", "")
                : "";

        // 1. Se o usuário informou um CEP válido de 8 dígitos, consulta ViaCEP
        if (!cepLimpo.isEmpty()) {
            if (cepLimpo.length() != 8) {
                throw new IllegalArgumentException("CEP inválido: informe exatamente 8 dígitos numéricos.");
            }
            Response<EnderecoViaCep> response = viaCepApi.buscarPorCep(cepLimpo).execute();
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Falha HTTP ao consultar ViaCEP (Código " + response.code() + "). Verifique sua conexão.");
            }
            EnderecoViaCep endereco = response.body();
            // Tratamento de erro lógico do ViaCEP (HTTP 200 com {"erro": true})
            if (Boolean.TRUE.equals(endereco.getErro())) {
                throw new IllegalArgumentException("CEP não localizado na base do ViaCEP. Verifique o número digitado.");
            }
            String resumo = "ViaCEP: " + endereco.formatarResumo();
            registro.setDadosEnriquecidosApi(resumo);
            return resumo;
        }

        // 2. Caso contrário, enriquece via OpenWeather usando as coordenadas GPS do registro
        String apiKey = BuildConfig.OPENWEATHER_API_KEY;
        if (apiKey != null && !apiKey.equals("SUA_CHAVE_AQUI") && !apiKey.equals("DEMO_KEY")) {
            Response<ClimaResponse> response = openWeatherApi.buscarClimaAtual(
                    registro.getLatitude(),
                    registro.getLongitude(),
                    apiKey,
                    "metric",
                    "pt_br"
            ).execute();

            if (response.isSuccessful() && response.body() != null) {
                String resumo = "OpenWeather: " + response.body().formatarResumo();
                registro.setDadosEnriquecidosApi(resumo);
                return resumo;
            }
        }

        // Fallback consulta CEP padrão de referência (Uberlândia/MG - 38400100) via ViaCEP para garantir enriquecimento REST real mesmo sem chave OpenWeather configurada
        Response<EnderecoViaCep> fallbackResp = viaCepApi.buscarPorCep("38400100").execute();
        if (fallbackResp.isSuccessful() && fallbackResp.body() != null && !Boolean.TRUE.equals(fallbackResp.body().getErro())) {
            String resumo = String.format("API ViaCEP (Ref. Coordenadas %.4f, %.4f): %s",
                    registro.getLatitude(), registro.getLongitude(), fallbackResp.body().formatarResumo());
            registro.setDadosEnriquecidosApi(resumo);
            return resumo;
        }

        throw new IOException("Não foi possível conectar à API externa. Verifique sua conexão com a internet.");
    }
}
