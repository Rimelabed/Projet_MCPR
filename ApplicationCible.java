import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ApplicationCible extends UnicastRemoteObject  {
    private String nom;
    private RmiNodeInterface noeudRecouvrement;
    private String adresseLocale;

    public ApplicationCible(String nom, String adresseRecouvrement, String adresseLocale) throws RemoteException {
        super();
        this.nom = nom;
        this.adresseLocale = adresseLocale;

        try {
            // // Enregistrer cette application cible dans RMI
            // Naming.rebind("//" + adresseLocale + "/" + nom, this);
            // System.out.println(nom + " enregistré sur " + adresseLocale + " dans le registre RMI.");

            // Connexion au recouvrement
            noeudRecouvrement = (RmiNodeInterface) Naming.lookup("//" + adresseRecouvrement + "/Rec1");
            System.out.println(nom + " connecté à " + adresseRecouvrement);

        } catch (Exception e) {
            System.err.println("Erreur connexion RMI: " + e.getMessage());
        }
    }


    public void recevoirMessage(String source, String message) throws RemoteException {
        System.out.println("[Cible " + nom + "] Message reçu de " + source + " : " + message);
    }


    public void envoyerMessage(String source, String message) throws RemoteException {
        if (noeudRecouvrement != null) {
            System.out.println("[Cible " + nom + "] Envoi message : " + message);
            noeudRecouvrement.recevoirMessage(nom, message);
        } else {
            System.out.println("[Cible " + nom + "] Impossible d'envoyer le message, noeud de recouvrement non disponible.");
        }
    }


    public String getAdresseLogique() throws RemoteException {
        return nom;
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage : java ApplicationCible <nomCible> <adresseRecouvrement> <adresseLocale>");
            System.exit(1);
        }

        try {
            String nom = args[0];
            String adresseRecouvrement = args[1]; // IP du recouvrement associé
            String adresseLocale = args[2]; // IP de CETTE application cible

            ApplicationCible cible = new ApplicationCible(nom, adresseRecouvrement, adresseLocale);

            // Envoi d'un message de test après connexion
            cible.envoyerMessage(nom, "Hello depuis " + nom + " !");
        } catch (Exception e) {
            System.err.println("Erreur démarrage ApplicationCible: " + e.getMessage());
        }
    }
}
