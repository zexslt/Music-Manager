package musicmanager.app.models;

import java.util.List;

public class Tour {
    private List<String> cities;
    private String status;
    private int revenue;

    public Tour(List<String> cities, String status, int revenue) {
        setCities(cities);
        setStatus(status);
        setRevenue(revenue);
    }

    //Getters
    public List<String> getCities() {
        return cities;
    }

    public String getStatus() {
        return status;
    }

    public int getRevenue() {
        return revenue;
    }

    //Setters
    public void setCities(List<String> cities) {
        this.cities = cities;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRevenue(int revenue) {
        this.revenue = revenue;
    }

    //Print Info
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Статус тура: ").append(status != null ? status : "не указан").append("\n");
        sb.append("Выручка: ").append(revenue).append(" $.\n");
        sb.append("Маршрут (города): ");

        if (cities != null && !cities.isEmpty()) {
            sb.append(String.join(", ", cities));
        } else {
            sb.append("список городов пуст");
        }

        return sb.toString();
    }
}

