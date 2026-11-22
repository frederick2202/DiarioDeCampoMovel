package br.edu.uniube.diariodecampo.model;

import java.util.List;

public class ClimaResponse {
    private String name;
    private Main main;
    private List<Weather> weather;

    public static class Main {
        private double temp;
        private int humidity;

        public double getTemp() { return temp; }
        public int getHumidity() { return humidity; }
    }

    public static class Weather {
        private String description;
        public String getDescription() { return description; }
    }

    public String getName() { return name; }
    public Main getMain() { return main; }
    public List<Weather> getWeather() { return weather; }

    public String formatarResumo() {
        String desc = (weather != null && !weather.isEmpty()) ? weather.get(0).getDescription() : "Clima estável";
        double temp = (main != null) ? main.getTemp() : 0.0;
        int umid = (main != null) ? main.getHumidity() : 0;
        String local = (name != null && !name.isEmpty()) ? name : "Coordenadas GPS";
        return String.format("Local: %s | %.1f°C (%s) | Umidade: %d%%", local, temp, desc, umid);
    }
}
