import java.util.HashMap;
import java.util.Map;

public class TableRoutage {
    private Map<String, Integer> routes;

    public TableRoutage() {
        this.routes = new HashMap<>();
    }

    public boolean mettreAJour(String destination, int distance) {
        if (!routes.containsKey(destination) || routes.get(destination) > distance) {
            routes.put(destination, distance);
            System.out.println("[ROUTAGE] Mise à jour : " + destination + " via " + distance + " sauts");
            return true;
        }
        return false;
    }

    public Map<String, Integer> getRoutes() {
        return new HashMap<>(routes);
    }
}
