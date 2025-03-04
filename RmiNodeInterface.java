import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface RmiNodeInterface extends Remote {
    void recevoirMessage(String source, String messageId, int ttl, String contenu) throws RemoteException;
    void recevoirTableRoutage(String source, Map<String, Integer> nouvellesRoutes) throws RemoteException;
    // void ajouterVoisin(String nomVoisin, RmiNodeInterface voisin) throws RemoteException;
}
