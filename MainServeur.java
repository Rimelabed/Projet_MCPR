import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.FileReader;

public class MainServeur {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(1099);  // On s’assure que RMI tourne

            JSONParser parser = new JSONParser();
            JSONObject reseau = (JSONObject) parser.parse(new FileReader("reseau.json"));
            JSONObject recouvrements = (JSONObject) reseau.get("recouvrements");

            for (Object key : recouvrements.keySet()) {
                String nom = (String) key;
                String adresse = (String) ((JSONObject) recouvrements.get(nom)).get("adresse");

                ApplicationRecouvrement recouvrement = new ApplicationRecouvrement(nom, adresse);
                Registry registry = LocateRegistry.getRegistry();
                registry.rebind(nom, recouvrement);

                System.out.println("[INFO] Serveur RMI démarré pour " + nom + " à " + adresse);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
