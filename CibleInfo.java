public class CibleInfo {
    private RmiNodeInterface cible;
    private String groupe;

    public CibleInfo(RmiNodeInterface cible, String groupe) {
        this.cible = cible;
        this.groupe = groupe;
    }

    public RmiNodeInterface getCible() {
        return cible;
    }

    public String getGroupe() {
        return groupe;
    }
}
