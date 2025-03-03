import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RmiNodeInterface extends Remote {
    void recevoirMessage(String source, String message) throws RemoteException;
    void ajouterVoisin(String nomVoisin, RmiNodeInterface voisin) throws RemoteException;
}
