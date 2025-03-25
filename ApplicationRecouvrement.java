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
import java.util.concurrent.*;

public class ApplicationRecouvrement extends UnicastRemoteObject implements RmiNodeInterface {
    private String nom;
    private String adresse;
    private Map<String, RmiNodeInterface> voisins;
    private TableRoutage tableRoutage;
    private Set<String> messagesRecus;
    private Map<String, CibleInfo> applicationsCibles = new HashMap<>();
    // Clé : nom du groupe ; Valeur : ensemble des informations sur les cibles abonnées
    private Map<String, Set<CibleInfo>> subscriptions = new HashMap<>();

    public ApplicationRecouvrement(String nom, String adresse) throws RemoteException {
        super();
        this.nom = nom;
        this.adresse = adresse;
        this.voisins = new HashMap<>();
        this.tableRoutage = new TableRoutage();
        this.messagesRecus = new HashSet<>();

        initHeartbeat();
        initAffichageSubscriptions();
    }

    // heartbeat pour déterminer combien de temps une AppliCible peut rester sans envoyer de "ping" > threshold
    public void initHeartbeat() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long threshold = 30000; // 30 secondes
                for (String groupe : subscriptions.keySet()) {
                    Set<CibleInfo> abonnes = subscriptions.get(groupe);
                    if (abonnes != null) {
                        abonnes.removeIf(info -> {
                            boolean expired = (now - info.getLastHeartbeat()) > threshold;
                            if (expired) {
                                System.out.println(ConsoleColors.YELLOW 
                                    + "[INFO] " 
                                    + info.getCibleName() 
                                    + " retiré du groupe " 
                                    + groupe 
                                    + " (heartbeat expiré)."
                                    + ConsoleColors.RESET);
                            }
                            return expired;
                        });
                    }
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    // affichage périodique de la liste des groupes et des abonnés
    private void initAffichageSubscriptions() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                System.out.println(ConsoleColors.CYAN 
                    + "[SUBSCRIPTIONS] État actuel des groupes :"
                    + ConsoleColors.RESET);
                for (String groupe : subscriptions.keySet()) {
                    Set<CibleInfo> abonnes = subscriptions.get(groupe);
                    System.out.print(ConsoleColors.CYAN 
                        + "Groupe '" + groupe + "': " 
                        + ConsoleColors.RESET);
                    if (abonnes != null && !abonnes.isEmpty()) {
                        for (CibleInfo info : abonnes) {
                            System.out.print(info.getCibleName() + " ");
                        }
                    } else {
                        System.out.print("Aucun abonné");
                    }
                    System.out.println();
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
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

                            if (recouvrements.containsKey(voisinNom)) {
                                // C'est un recouvrement → On cherche dans le registre RMI
                                voisinIP = (String) ((JSONObject) recouvrements.get(voisinNom)).get("adresse");
                                System.out.println(ConsoleColors.BLUE 
                                    + "[DEBUG] " 
                                    + voisinNom 
                                    + " est un recouvrement."
                                    + ConsoleColors.RESET);

                                try {
                                    System.out.println(ConsoleColors.BLUE 
                                        + "[DEBUG] " 
                                        + nom 
                                        + " tente de se connecter à " 
                                        + voisinNom 
                                        + " via RMI..."
                                        + ConsoleColors.RESET);
                                    RmiNodeInterface voisin = (RmiNodeInterface) Naming.lookup("//" + voisinIP + "/" + voisinNom);
                                    voisins.put(voisinNom, voisin);
                                    System.out.println(ConsoleColors.GREEN 
                                        + "[SUCCESS] " 
                                        + nom 
                                        + " est connecté à " 
                                        + voisinNom 
                                        + " !"
                                        + ConsoleColors.RESET);
                                } catch (Exception e) {
                                    System.err.println(ConsoleColors.RED 
                                        + "[ERREUR] Impossible de se connecter à " 
                                        + voisinNom 
                                        + " : " 
                                        + e.getMessage()
                                        + ConsoleColors.RESET);
                                }
                            }
                            else if (reseau.containsKey("applications_cibles")) {
                                JSONObject ciblesConfig = (JSONObject) reseau.get("applications_cibles");
                                if (ciblesConfig.containsKey(voisinNom)) {
                                    // Ici, la valeur est un objet JSON contenant "adresse" et "groupe"
                                    JSONObject cibleObj = (JSONObject) ciblesConfig.get(voisinNom);
                                    voisinIP = (String) cibleObj.get("adresse");
                                    String groupe = (String) cibleObj.get("groupe");
                                    System.out.println(ConsoleColors.BLUE 
                                        + "[DEBUG] " 
                                        + voisinNom 
                                        + " est une application cible appartenant au groupe " 
                                        + groupe + "," 
                                        + voisinIP
                                        + ConsoleColors.RESET);
                                    try {
                                        RmiNodeInterface cible = (RmiNodeInterface) Naming.lookup("//" + voisinIP + "/" + voisinNom);
                                        CibleInfo info = new CibleInfo(cible, groupe, voisinNom);
                                        this.applicationsCibles.put(voisinNom, info);
                                        System.out.println(ConsoleColors.GREEN 
                                            + "[SUCCESS] " 
                                            + voisinNom 
                                            + " (groupe " 
                                            + groupe 
                                            + ") est maintenant connu de " 
                                            + nom
                                            + ConsoleColors.RESET);
                                    } catch (Exception e) {
                                        System.err.println(ConsoleColors.RED 
                                            + "[ERREUR] Impossible d'ajouter l'application cible " 
                                            + voisinNom 
                                            + " : " 
                                            + e.getMessage()
                                            + ConsoleColors.RESET);
                                    }
                                }
                            }
                            else {
                                System.err.println(ConsoleColors.RED 
                                    + "[ERREUR] " 
                                    + voisinNom 
                                    + " n'existe ni dans recouvrements ni dans applications_cibles !"
                                    + ConsoleColors.RESET);
                                continue;
                            }
                        }
                    }

                    Thread.sleep(5000);
                } catch (Exception e) {
                    System.err.println(ConsoleColors.RED 
                        + "[ERREUR] Problème lors de la découverte des voisins : " 
                        + e.getMessage()
                        + ConsoleColors.RESET);
                }
            }
        }).start(); // Lancer la boucle de reconnexion en parallèle
    }

    public void recevoirMessage(String source, String messageId, int ttl, String contenu) throws RemoteException {

        if (messagesRecus.contains(messageId) || ttl <= 0) {
            return;
        }
        messagesRecus.add(messageId);
        System.out.println(ConsoleColors.PURPLE 
            + "[Recouvrement " + nom + "] Message reçu de " + source + " : " + contenu + " (TTL=" + ttl + ")"
            + ConsoleColors.RESET);

        // Propager aux autres recouvrements
        for (String voisin : voisins.keySet()) {
            if (!voisin.equals(source)) {
                voisins.get(voisin).recevoirMessage(this.nom, messageId, ttl - 1, contenu);
            }
        }

        // Extraction du groupe ciblé (si présent)
        String groupeCible = null;
        String contenuFinal = contenu;
        if (contenu.startsWith("group:")) {
            int idx = contenu.indexOf(";");
            if (idx != -1) {
                groupeCible = contenu.substring(6, idx);
                contenuFinal = contenu.substring(idx + 1);
            }
        }

        // Diffusion aux cibles selon les abonnements
        if (groupeCible != null) {
            Set<CibleInfo> abonnes = subscriptions.get(groupeCible);
            if (abonnes != null) {
                for (CibleInfo info : abonnes) {
                    info.getCible().recevoirMessage(this.nom, messageId, ttl - 1, contenuFinal);
                }
            }
        } else {
            // Diffusion totale (si aucun groupe n'est spécifié)
            for (Map.Entry<String, CibleInfo> entry : applicationsCibles.entrySet()) {
                if (!entry.getKey().equals(source)) {
                    entry.getValue().getCible().recevoirMessage(this.nom, messageId, ttl -1, contenuFinal);
                }
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

    @Override
    public void joinGroup(String groupe, String nomCible) throws RemoteException {
        if (!applicationsCibles.containsKey(nomCible)) {
            System.err.println(ConsoleColors.RED 
                + "[ERREUR] Cible " 
                + nomCible 
                + " inconnue, impossible de rejoindre le groupe " 
                + groupe
                + ConsoleColors.RESET);
            return;
        }
        CibleInfo info = applicationsCibles.get(nomCible);
        subscriptions.computeIfAbsent(groupe, k -> new HashSet<>()).add(info);
        System.out.println(ConsoleColors.YELLOW 
            + "[INFO] " 
            + nomCible 
            + " a rejoint le groupe " 
            + groupe
            + ConsoleColors.RESET);
    }

    @Override
    public void leaveGroup(String groupe, String nomCible) throws RemoteException {
        if (subscriptions.containsKey(groupe)) {
            subscriptions.get(groupe).removeIf(info -> nomCible.equals(info.getCibleName()));
            System.out.println(ConsoleColors.YELLOW 
                + "[INFO] " 
                + nomCible 
                + " a quitté le groupe " 
                + groupe
                + ConsoleColors.RESET);
        }
    }

    @Override
    public void heartbeat(String groupe, String nomCible) throws RemoteException {
        Set<CibleInfo> abonnes = subscriptions.get(groupe);
        if (abonnes != null) {
            for (CibleInfo info : abonnes) {
                if (info.getCibleName().equals(nomCible)) {
                    info.updateHeartbeat();
                    System.out.println(ConsoleColors.CYAN 
                        + "[HEARTBEAT] " 
                        + nomCible 
                        + " (groupe " 
                        + groupe 
                        + ") est toujours actif."
                        + ConsoleColors.RESET);
                    return;
                }
            }
        }
        System.err.println(ConsoleColors.RED 
            + "[HEARTBEAT] Aucune cible trouvée pour " 
            + nomCible 
            + " dans le groupe " 
            + groupe
            + ConsoleColors.RESET);
    }

    public String getNom() {
        return nom;
    }

    public Map<String, RmiNodeInterface> getVoisins() {
        return voisins;
    }
}
