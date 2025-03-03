import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class MainServeur {
    public static void main(String[] args) {
        try {
            // Démarrer le registre RMI sur Rec1
            LocateRegistry.createRegistry(1099);

            // Création et enregistrement d'un seul recouvrement
            ApplicationRecouvrement rec1 = new ApplicationRecouvrement("Rec1");

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("Rec1", rec1);

            System.out.println("Serveur RMI prêt !");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
