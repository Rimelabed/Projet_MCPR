import java.io.FileReader;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ApplicationRecouvrement extends UnicastRemoteObject implements RmiNodeInterface {
    private String nom;
    private String adresse;
    private Map<String, RmiNodeInterface> voisins;
    private TableRoutage tableRoutage;
    private Set<String> messagesRecus;

    public ApplicationRecouvrement(String nom, String adresse) throws RemoteException {
        super();
        this.nom = nom;
        this.adresse = adresse;
        this.voisins = new HashMap<>();
        this.tableRoutage = new TableRoutage();
        this.messagesRecus = new HashSet<>();
        //chargerVoisins();
    }

    public void chargerVoisins() {
        try {
            JSONParser parser = new JSONParser();
            JSONObject reseau = (JSONObject) parser.parse(new FileReader("reseau.json"));
            JSONObject recouvrements = (JSONObject) reseau.get("recouvrements");
            JSONObject config = (JSONObject) recouvrements.get(nom);

            if (config != null) {
                JSONObject voisinsConfig = (JSONObject) config.get("voisins");

            for (Object key : voisinsConfig.keySet()) {
                String voisinNom = (String) key;
                String voisinIP = null;

                System.out.println("[DEBUG] Vérification du voisin : " + voisinNom);

                // Vérifier si le voisin est un recouvrement
                if (recouvrements.containsKey(voisinNom)) {
                    System.out.println("[DEBUG] " + voisinNom + " est un recouvrement.");
                    JSONObject voisinConfig = (JSONObject) recouvrements.get(voisinNom);
                    
                    if (voisinConfig == null) {
                        System.err.println("[ERREUR] voisinConfig est NULL pour " + voisinNom);
                        continue;
                    }

                    voisinIP = (String) voisinConfig.get("adresse");

                    if (voisinIP == null) {
                        System.err.println("[ERREUR] Impossible de récupérer l'adresse de " + voisinNom);
                    }
                }
                // Vérifier si le voisin est une application cible
                else if (reseau.containsKey("applications_cibles")) {
                    JSONObject applicationsCibles = (JSONObject) reseau.get("applications_cibles");

                    if (applicationsCibles == null) {
                        System.err.println("[ERREUR] La section applications_cibles est NULL !");
                        continue;
                    }

                    if (applicationsCibles.containsKey(voisinNom)) {
                        System.out.println("[DEBUG] " + voisinNom + " est une application cible.");
                        voisinIP = (String) applicationsCibles.get(voisinNom);

                        if (voisinIP == null) {
                            System.err.println("[ERREUR] Impossible de récupérer l'adresse de " + voisinNom + " dans applications_cibles.");
                        }
                    }
                } else {
                    System.err.println("[ERREUR] " + voisinNom + " n'existe ni dans recouvrements ni dans applications_cibles !");
                }

                if (voisinIP == null) {
                    System.err.println("[ERREUR] " + nom + " : Impossible de trouver l'IP de " + voisinNom);
                    continue;
                }

                System.out.println("[INFO] " + nom + " a pour voisin : " + voisinNom + " @ " + voisinIP);
            }


            }
        } catch (Exception e) {
            System.err.println("[ERREUR] Impossible de charger le fichier JSON : " + e.getMessage());
        }
    }

    public void recevoirMessage(String source, String messageId, int ttl, String contenu) throws RemoteException {
        if (messagesRecus.contains(messageId) || ttl <= 0) {
            return;
        }
        messagesRecus.add(messageId);
        System.out.println("[Recouvrement " + nom + "] Message reçu de " + source + " : " + contenu + " (TTL=" + ttl + ")");
        for (String voisin : voisins.keySet()) {
            if (!voisin.equals(source)) {
                voisins.get(voisin).recevoirMessage(this.nom, messageId, ttl - 1, contenu);
            }
        }
    }

    public void recevoirTableRoutage(String source, Map<String, Integer> nouvellesRoutes) throws RemoteException {
        boolean modifie = false;
        for (Map.Entry<String, Integer> entry : nouvellesRoutes.entrySet()) {
            if (tableRoutage.mettreAJour(entry.getKey(), entry.getValue() + 1)) {
                modifie = true;
            }
        }
        if (modifie) {
            propagerTableRoutage();
        }
    }

    private void propagerTableRoutage() throws RemoteException {
        for (RmiNodeInterface voisin : voisins.values()) {
            voisin.recevoirTableRoutage(nom, tableRoutage.getRoutes());
        }
    }
    public String getNom() {
        return nom;
    }

    public Map<String, RmiNodeInterface> getVoisins() {
        return voisins;
    }

}
