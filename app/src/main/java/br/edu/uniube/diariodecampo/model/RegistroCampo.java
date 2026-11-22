package br.edu.uniube.diariodecampo.model;

import android.graphics.Bitmap;

public class RegistroCampo {
    private final String id;
    private final String titulo;
    private final String cepOpcional;
    private final String dataHora;
    private final double latitude;
    private final double longitude;
    private final float luminosidadeLux;
    private final Bitmap fotoMiniatura;
    private String dadosEnriquecidosApi;

    public RegistroCampo(String id, String titulo, String cepOpcional, String dataHora,
                         double latitude, double longitude, float luminosidadeLux, Bitmap fotoMiniatura) {
        this.id = id;
        this.titulo = titulo;
        this.cepOpcional = cepOpcional;
        this.dataHora = dataHora;
        this.latitude = latitude;
        this.longitude = longitude;
        this.luminosidadeLux = luminosidadeLux;
        this.fotoMiniatura = fotoMiniatura;
        this.dadosEnriquecidosApi = null;
    }

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getCepOpcional() { return cepOpcional; }
    public String getDataHora() { return dataHora; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public float getLuminosidadeLux() { return luminosidadeLux; }
    public Bitmap getFotoMiniatura() { return fotoMiniatura; }
    public String getDadosEnriquecidosApi() { return dadosEnriquecidosApi; }
    public void setDadosEnriquecidosApi(String dadosEnriquecidosApi) {
        this.dadosEnriquecidosApi = dadosEnriquecidosApi;
    }
}
