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
    private Map<String, RmiNodeInterface> applicationsCibles;


    public ApplicationRecouvrement(String nom, String adresse) throws RemoteException {
        super();
        this.nom = nom;
        this.adresse = adresse;
        this.voisins = new HashMap<>();
        this.applicationsCibles = new HashMap<>();
        this.tableRoutage = new TableRoutage();
        this.messagesRecus = new HashSet<>();
        //chargerVoisins();
    }

    public void chargerVoisins() {
    new Thread(() -> {
        while (true) {  
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

                        // // Vérifier si le voisin est un recouvrement
                        // if (recouvrements.containsKey(voisinNom)) {
                        //     voisinIP = (String) ((JSONObject) recouvrements.get(voisinNom)).get("adresse");
                        // }

                        


                        // Vérifier si déjà connecté
                       if (recouvrements.containsKey(voisinNom)) {
                            // C'est un recouvrement → On cherche dans le registre RMI
                            voisinIP = (String) ((JSONObject) recouvrements.get(voisinNom)).get("adresse");
                            System.out.println("[DEBUG] " + voisinNom + " est un recouvrement.");
                            
                            try {
                                System.out.println("[DEBUG] " + nom + " tente de se connecter à " + voisinNom + " via RMI...");
                                RmiNodeInterface voisin = (RmiNodeInterface) Naming.lookup("//" + voisinIP + "/" + voisinNom);
                                voisins.put(voisinNom, voisin);
                                System.out.println("[SUCCESS] " + nom + " est connecté à " + voisinNom + " !");
                            } catch (Exception e) {
                                System.err.println("[ERREUR] Impossible de se connecter à " + voisinNom + " : " + e.getMessage());
                            }
                        }
                        else if (reseau.containsKey("applications_cibles")) {
                            JSONObject ciblesConfig = (JSONObject) reseau.get("applications_cibles");
                            if (ciblesConfig.containsKey(voisinNom)) {
                                voisinIP = (String) ciblesConfig.get(voisinNom);
                                System.out.println("[DEBUG] " + voisinNom + " est une application cible.");
                                try {
                                    // Récupération de l'instance distante de la cible via RMI
                                    RmiNodeInterface cible = (RmiNodeInterface) Naming.lookup("//" + voisinIP + "/" + voisinNom);
                                    this.applicationsCibles.put(voisinNom, cible);
                                    System.out.println("[SUCCESS] " + voisinNom + " est maintenant connu de " + nom);
                                } catch (Exception e) {
                                    System.err.println("[ERREUR] Impossible d'ajouter l'application cible " + voisinNom + " : " + e.getMessage());
                                }
                            }
                        }                        
                         else {
                            System.err.println("[ERREUR] " + voisinNom + " n'existe ni dans recouvrements ni dans applications_cibles !");
                            continue;
                        }
                    }
                }

                // 🕒 Attendre 5 secondes avant de réessayer
                Thread.sleep(5000);
            } catch (Exception e) {
                System.err.println("[ERREUR] Problème lors de la découverte des voisins : " + e.getMessage());
            }
        }
    }).start(); // Lancer la boucle de reconnexion en parallèle
}


    public void recevoirMessage(String source, String messageId, int ttl, String contenu) throws RemoteException {
        if (messagesRecus.contains(messageId) || ttl <= 0) {
            return;
        }
        messagesRecus.add(messageId);
        System.out.println("[Recouvrement " + nom + "] Message reçu de " + source + " : " + contenu + " (TTL=" + ttl + ")");

        // Propager aux autres recouvrements
        for (String voisin : voisins.keySet()) {
            if (!voisin.equals(source)) {
                voisins.get(voisin).recevoirMessage(this.nom, messageId, ttl - 1, contenu);
            }
        }

        // Envoyer aussi aux applications cibles
        for (String cible : applicationsCibles.keySet()) {
            if (!cible.equals(source)) {
                applicationsCibles.get(cible).recevoirMessage(this.nom, messageId, ttl - 1, contenu);
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
