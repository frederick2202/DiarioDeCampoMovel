package br.edu.uniube.diariodecampo.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static ViaCepApi viaCepApi;
    private static OpenWeatherApi openWeatherApi;

    public static synchronized ViaCepApi getViaCepApi() {
        if (viaCepApi == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://viacep.com.br/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            viaCepApi = retrofit.create(ViaCepApi.class);
        }
        return viaCepApi;
    }

    public static synchronized OpenWeatherApi getOpenWeatherApi() {
        if (openWeatherApi == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.openweathermap.org/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            openWeatherApi = retrofit.create(OpenWeatherApi.class);
        }
        return openWeatherApi;
    }
}
