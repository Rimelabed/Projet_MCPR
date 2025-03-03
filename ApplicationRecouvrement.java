import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class ApplicationRecouvrement extends UnicastRemoteObject implements RmiNodeInterface {
    private String nom;
    private Map<String, RmiNodeInterface> voisins;

    public ApplicationRecouvrement(String nom) throws RemoteException {
        super();
        this.nom = nom;
        this.voisins = new HashMap<>();
    }

    @Override
    public void ajouterVoisin(String nomVoisin, RmiNodeInterface voisin) throws RemoteException {
        voisins.put(nomVoisin, voisin);
        System.out.println("[INFO] " + nom + " connecté à " + nomVoisin);
    }

    @Override
    public void recevoirMessage(String source, String message) throws RemoteException {
        System.out.println("[Recouvrement " + nom + "] Message reçu de " + source + " : " + message);
        
        // Transfert aux voisins
        for (Map.Entry<String, RmiNodeInterface> voisin : voisins.entrySet()) {
            if (!voisin.getKey().equals(source)) {
                voisin.getValue().recevoirMessage(nom, message);
            }
        }
    }
}
