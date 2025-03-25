import java.io.FileReader;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;



public class ApplicationCible extends UnicastRemoteObject implements RmiNodeInterface {


    private String nom;
    private String adresseRecouvrement;
    private RmiNodeInterface noeudRecouvrement;
    private Set<String> joinedGroups = new HashSet<>();

    public ApplicationCible(String nom) throws RemoteException {
        super();
        this.nom = nom;
        this.noeudRecouvrement = null;

        try {
            // chargement des informations du réseau dpuis le fichier reseau.json - fichier contenant la topologie
            JSONParser parser = new JSONParser();
            JSONObject reseau = (JSONObject) parser.parse(new FileReader("reseau.json"));
            JSONObject applicationsCibles = (JSONObject) reseau.get("applications_cibles");
            JSONObject recouvrements = (JSONObject) reseau.get("recouvrements");

            
            
            if (!applicationsCibles.containsKey(nom)) {
                System.err.println("[ERREUR] L'application cible " + nom + " n'est pas définie dans reseau.json !");
                return;
            }

            // Pour trouver le recouvrement correspondant
            for (Object key : recouvrements.keySet()) {
                String recouvrementNom = (String) key;
                JSONObject recouvrementConfig = (JSONObject) recouvrements.get(recouvrementNom);
                JSONObject voisins = (JSONObject) recouvrementConfig.get("voisins");

                if (voisins != null && voisins.containsKey(nom)) {
                    adresseRecouvrement = (String) recouvrementConfig.get("adresse");
                    System.out.println("[INFO] " + nom + " est associé à " + recouvrementNom + " @ " + adresseRecouvrement);
                    connecterAuRecouvrement(recouvrementNom);
                    return;
                }
            }

            System.err.println("[ERREUR] Aucun recouvrement trouvé pour " + nom);
        } catch (Exception e) {
            System.err.println("[ERREUR] Impossible de charger les informations réseau : " + e.getMessage());
        }
    }

    public void recevoirTableRoutage(String source, Map<String, Integer> nouvellesRoutes) throws RemoteException
    {}

    private void connecterAuRecouvrement(String recouvrementNom) {
       while (true) {  // boucle qui va faire que l'applie Réessaie jusqu'à réussir à se connecter à une appli de recouvrement
        try {
            System.out.println("[DEBUG] " + nom + " tente de se connecter à " + recouvrementNom + " via RMI...");
            noeudRecouvrement = (RmiNodeInterface) Naming.lookup("//" + adresseRecouvrement + "/" + recouvrementNom);
            System.out.println("[SUCCESS] " + nom + " est connecté à " + recouvrementNom + " !");
            break;  // On sort de la boucle une fois connecté
        } catch (Exception e) {
            System.err.println("[ERREUR] " + nom + " : Impossible de contacter " + recouvrementNom + " (Réessaie dans 5s)");
            try {
                Thread.sleep(5000); // Attente de cinq secoondes avant la prochaine tentative - Modulable en fonction des besoins
            } catch (InterruptedException ignored) {}
        }
    }
    }

    public void envoyerMessage(String contenu) throws RemoteException {
        if (noeudRecouvrement == null) {
            System.out.println("[ERREUR] " + nom + " : Aucun noeud de recouvrement connecté !");
            return;
        }

        // génération d'un ID unique et définition d'un TTL -> Eviter de boucler dans le reseau
        String messageId = UUID.randomUUID().toString();
        int ttl = 5;

        System.out.println("[INFO] " + nom + " envoie un message avec TTL=" + ttl);
        noeudRecouvrement.recevoirMessage(nom, messageId, ttl, contenu);
    }

    public void recevoirMessage(String source, String messageId, int ttl, String contenu) throws RemoteException {
        if (ttl <= 0) {
            return;
        }
        System.out.println("[CIBLE " + nom + "] Message reçu de " + source + " : " + contenu);
    }
    @Override
    public void joinGroup(String groupe, String nomCible) throws RemoteException {
        // rien faire ici, car la gestion est assurée par le recouvrement
    }

    @Override
    public void leaveGroup(String groupe, String nomCible) throws RemoteException {
        //  rien faire ici
    }
    @Override
    public void heartbeat(String groupe, String nomCible) throws RemoteException {
        // rien faire ici
    }



    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage : java ApplicationCible <NomCible>");
            System.exit(1);
        }

        try {
            String nom = args[0];
            ApplicationCible cible = new ApplicationCible(nom);
            LocateRegistry.createRegistry(1099);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(nom, cible);
            System.out.println("[INFO] " + nom + " enregistré dans le registre RMI.");
            
            // thread heartbit -> poc que l'application cible est toujours présente -> l'appli cible est mainteanu dans son  groupe de diffusion 

             // thread heartbeat pour l'instance cible
            new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(10000); // Toutes les 10 secondes
                        // Utilisez l'instance 'cible' pour accéder à joinedGroups ()
                        for (String groupe : cible.joinedGroups) {
                            try {
                                cible.noeudRecouvrement.heartbeat(groupe, nom);
                            } catch (Exception e) {
                                System.err.println("[HEARTBEAT] Erreur lors de l'envoi du heartbeat pour le groupe " + groupe + ": " + e.getMessage());
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();



             // Menu interactif
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("\n=== Menu de " + nom + " ===");
                System.out.println("1. Tester la connexion avec le réseau");
                System.out.println("2. Envoyer un message court personnalisé");
                System.out.println("3. Rejoindre un groupe");
                System.out.println("4. Quitter un groupe");
                System.out.println("5. Afficher mes groupes");
                System.out.println("0. Quitter l'application");
                System.out.print("Votre choix : ");
                String choix = scanner.nextLine();

                if (choix.equals("1")) {
                    cible.envoyerMessage("Message de test de connexion de " + nom);
                } else if (choix.equals("2")) {
                    System.out.print("Entrez le groupe cible (laisser vide pour diffusion totale) : ");
                    String groupeCible = scanner.nextLine().trim();
                    System.out.print("Entrez votre message : ");
                    String message = scanner.nextLine();
                    if (!groupeCible.isEmpty()) {
                        message = "group:" + groupeCible + ";" + message;
                    }
                    cible.envoyerMessage(message);
                } else if (choix.equals("3")) {
                    System.out.print("Entrez le groupe auquel vous souhaitez vous abonner : ");
                    String groupe = scanner.nextLine().trim();
                    cible.noeudRecouvrement.joinGroup(groupe, nom);
                    cible.joinedGroups.add(groupe);
                } else if (choix.equals("4")) {
                    System.out.print("Entrez le groupe que vous souhaitez quitter : ");
                    String groupe = scanner.nextLine().trim();
                    cible.noeudRecouvrement.leaveGroup(groupe, nom);
                    cible.joinedGroups.remove(groupe);
                } else if (choix.equals("5")) {
                    System.out.println("Vous êtes abonné aux groupes : " + cible.joinedGroups);
                } else if (choix.equals("0")) {
                    System.out.println("Fermeture de l'application.");
                    break;
                } else {
                    System.out.println("Choix invalide. Veuillez réessayer.");
                }
            }
            scanner.close();



        } catch (Exception e) {
            System.err.println("[ERREUR] Problème lors du démarrage : " + e.getMessage());
        }


    }
}