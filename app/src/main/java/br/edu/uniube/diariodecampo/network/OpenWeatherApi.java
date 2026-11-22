package br.edu.uniube.diariodecampo.network;

import br.edu.uniube.diariodecampo.model.ClimaResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface OpenWeatherApi {
    @GET("data/2.5/weather")
    Call<ClimaResponse> buscarClimaAtual(
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("appid") String apiKey,
            @Query("units") String units,
            @Query("lang") String lang
    );
}
