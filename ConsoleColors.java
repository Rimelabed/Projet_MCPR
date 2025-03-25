public class ConsoleColors {

// L'intérêt de cette classe est la coloration du terminal notamment au niveau des applications de recouvrmeent
// Ces derniers ont de nombreux messages de logs, de tout type
// D'où la nécessité de pouvoir appliquer des couleurs distinctifs pour les différents types de messages logs

// source : https://gist.github.com/spdeepak/9900c17bc6657541dfd162d30d498950

    // rénitialisation
    public static final String RESET = "\u001B[0m";

    // choix de couleurs
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
}
