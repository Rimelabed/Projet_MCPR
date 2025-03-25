public class CibleInfo {
    private RmiNodeInterface cible;
    private String groupe;
    private String cibleName;
    private long lastHeartbeat; // Timestamp du dernier heartbeat

    public CibleInfo(RmiNodeInterface cible, String groupe, String cibleName) {
        this.cible = cible;
        this.groupe = groupe;
        this.cibleName = cibleName;
        updateHeartbeat(); // Initialise avec l'heure actuelle
    }

    public RmiNodeInterface getCible() {
        return cible;
    }

    public String getGroupe() {
        return groupe;
    }
    
    public String getCibleName() {
        return cibleName;
    }
    
    public void updateHeartbeat() {
        lastHeartbeat = System.currentTimeMillis();
    }
    
    public long getLastHeartbeat() {
        return lastHeartbeat;
    }
}
