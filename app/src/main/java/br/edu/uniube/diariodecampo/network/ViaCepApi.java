package br.edu.uniube.diariodecampo.network;

import br.edu.uniube.diariodecampo.model.EnderecoViaCep;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ViaCepApi {
    @GET("ws/{cep}/json/")
    Call<EnderecoViaCep> buscarPorCep(@Path("cep") String cep);
}
