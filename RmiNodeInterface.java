import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface RmiNodeInterface extends Remote {
    void recevoirMessage(String source, String messageId, int ttl, String contenu) throws RemoteException;
    void recevoirTableRoutage(String source, Map<String, Integer> nouvellesRoutes) throws RemoteException;

     // Méthodes pour la gestion dynamique des abonnements
    void joinGroup(String groupe, String nomCible) throws RemoteException;
    void leaveGroup(String groupe, String nomCible) throws RemoteException;

    void heartbeat(String groupe, String nomCible) throws RemoteException;

}