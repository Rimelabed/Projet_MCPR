import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.UUID;

public class ApplicationCible extends UnicastRemoteObject {
    private String nom;
    private RmiNodeInterface noeudRecouvrement;

    public ApplicationCible(String nom, String adresseRecouvrement) throws RemoteException {
        super();
        this.nom = nom;

        try {
            // Connexion au recouvrement via RMI
            noeudRecouvrement = (RmiNodeInterface) Naming.lookup("//" + adresseRecouvrement + "/Recouvrement");
            System.out.println(nom + " connecté à " + adresseRecouvrement);
        } catch (Exception e) {
            System.err.println("Erreur connexion RMI: " + e.getMessage());
        }
    }

    public void envoyerMessage(String contenu) throws RemoteException {
        // ✅ Définition propre du messageId et du TTL
        String messageId = UUID.randomUUID().toString();  // Génère un ID unique pour le message
        int ttl = 3;  // Définition du TTL initial

        if (noeudRecouvrement != null) {
            System.out.println("[Cible " + nom + "] Envoi du message avec TTL=" + ttl);
            noeudRecouvrement.recevoirMessage(nom, messageId, ttl, contenu);
        } else {
            System.out.println("[Cible " + nom + "] Erreur : Noeud de recouvrement non disponible !");
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage : java ApplicationCible <NomCible> <AdresseRecouvrement>");
            System.exit(1);
        }

        try {
            String nom = args[0];
            String adresseRecouvrement = args[1];
            ApplicationCible cible = new ApplicationCible(nom, adresseRecouvrement);

            // ✅ Envoi d'un message de test après connexion
            cible.envoyerMessage("Hello depuis " + nom + " !");
        } catch (Exception e) {
            System.err.println("Erreur démarrage ApplicationCible: " + e.getMessage());
        }
    }
}
