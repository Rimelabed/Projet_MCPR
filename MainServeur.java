import java.util.Map;
import java.util.HashMap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.FileReader;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MainServeur {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage : java MainServeur <NomRecouvrement>");
            System.exit(1);
        }

        String nom = args[0];  // L'identité de CE recouvrement
        try {
            // Charger le fichier JSON
            JSONObject reseau = (JSONObject) new JSONParser().parse(new FileReader("reseau.json"));
            System.out.println("[DEBUG] JSON chargé : " + reseau.toJSONString());

            JSONObject recouvrements = (JSONObject) reseau.get("recouvrements");
            JSONObject monConfig = (JSONObject) recouvrements.get(nom);  // Récupère juste CE recouvrement

            if (monConfig == null) {
                System.err.println("[ERREUR] Impossible de trouver la configuration pour " + nom);
                return;
            }

            String adresse = (String) monConfig.get("adresse");
            System.out.println("[INFO] " + nom + " s'initialise avec l'adresse " + adresse);

            // Démarrer RMI sur CETTE machine
            LocateRegistry.createRegistry(1099);
            ApplicationRecouvrement recouvrement = new ApplicationRecouvrement(nom, adresse);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(nom, recouvrement);
            System.out.println("[INFO] " + nom + " enregistré dans le registre RMI.");

            // Attendre avant de charger les voisins
            System.out.println("[INFO] Attente de stabilisation...");
            Thread.sleep(3000);

            recouvrement.chargerVoisins();
            System.out.println("[INFO] Initialisation complète pour " + nom);
        } catch (Exception e) {
            System.err.println("[ERREUR] Problème au démarrage de " + args[0] + " : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
