# MCPR - Projet Overlay
## Étape 4 : Diffusion Restreinte Dynamique (Multicast par Abonnements)

Dans cette étape, la diffusion est restreinte **dynamiquement** aux seules applications cibles qui se sont abonnées à un groupe via un mécanisme de **join/leave**. Cela simule un fonctionnement proche du multicast réel (type IGMP), où les hôtes rejoignent ou quittent un groupe à tout moment.

---

## Table des Matières
- [Introduction](#introduction)
- [Prérequis](#prérequis)
- [Nouvelles Structures et Méthodes](#nouvelles-structures-et-méthodes)
  - [Interface RmiNodeInterface](#interface-rminodeinterface)
  - [Classe CibleInfo](#classe-cibleinfo)
  - [ApplicationRecouvrement](#applicationrecouvrement)
  - [ApplicationCible](#applicationcible)
- [Heartbeat et Vérification Périodique](#heartbeat-et-vérification-périodique)
- [Diffusion dynamique et restreinte](#diffusion-dynamique-et-restreinte)
- [Compilation et Exécution](#compilation-et-exécution)
- [Améliorations et Couleurs](#améliorations-et-couleurs)
- [Dépannage](#dépannage)
- [NB](#NB)

---

## Introduction

Pour répondre à l’exigence d’une **diffusion restreinte et dynamique**, nous avons mis en place :

1. Un **système d’abonnement** permettant aux applications cibles de rejoindre ou quitter un groupe à l’exécution.  
2. Un **mécanisme de heartbeat** pour vérifier que les cibles abonnées sont toujours actives.  
3. Une **table de routage dynamique** dans l’application de recouvrement, qui ne diffuse un message à un groupe que vers les cibles qui s’y sont abonnées.

---

## Prérequis

- **Java 8 ou supérieur**  
- **Port 1099** libre pour RMI  
- **Fichier `reseau.json`** : Utilisé pour connaître l’adresse de chaque recouvrement et de chaque cible (mais plus pour gérer les groupes).  
- **Connaissances de base en RMI** et en **Java Concurrency** (threads, ScheduledExecutorService).

# Nouvelles structures et méthodes

### Interface-rminodeinterface
Voici les modifications apportées à l’interface RmiNodeInterface pour intégrer la gestion dynamique des abonnements et le mécanisme de heartbeat :

**Ajout des méthodes d’abonnement**
Nous avons ajouté deux méthodes :

`joinGroup(String groupe, String nomCible)`: Pour permettre à une application cible de rejoindre un groupe.

`leaveGroup(String groupe, String nomCible)`: Pour permettre à une application cible de quitter un groupe.

**Ajout de la méthode heartbeat**
La méthode `heartbeat(String groupe, String nomCible)` a été introduite pour que le recouvrement puisse mettre à jour le timestamp de la dernière confirmation de présence d’une cible dans un groupe. Cela permet de retirer automatiquement les cibles inactives.
``` java
//  gestion dynamique des abonnements
    void joinGroup(String groupe, String nomCible) throws RemoteException;
    void leaveGroup(String groupe, String nomCible) throws RemoteException;
    
    // Méthode de heartbeat pour vérifier l'activité d'une cible dans un groupe
    void heartbeat(String groupe, String nomCible) throws RemoteException;

```

### Classe CibleInfo 

Création d'un fichier CibleInfo.java qui encapsule la référence RMI et le groupe de l’application cible afin d’associer les informations de chaque cible au recouvrement (référence RMI, nom de la cible, timestamp de heartbeat, etc.)

``` java
public class CibleInfo {
    private RmiNodeInterface cible;
    private String cibleName;
    private long lastHeartbeat;

    public CibleInfo(RmiNodeInterface cible, String cibleName) {
        this.cible = cible;
        this.cibleName = cibleName;
        updateHeartbeat();
    }
```

### ApplicationRecouvrement
1. Modification de la découverte des cibles
    Dans la méthode qui parcourt le JSON pour trouver les applications cibles, on récupère l'objet JSON spécifique à chaque cible afin d'extraire à la fois "adresse" et "groupe". 
2. Filtrage lors de la diffusion des messages
    Dans la méthode `recevoirMessage`, on vérifie si le contenu du message contient un préfixe indiquant le groupe ciblé. Si oui, on extrait ce groupe et le message n'est diffusé qu'aux cibles dont l'attribut "groupe" correspond.

Note : Dans le projet, on garde encore un champ groupe pour le groupe “initial” dans le fichier de topologie, mais ici, on s’appuie principalement sur la table d’abonnements dynamique.

### ApplicationRecouvrement
Structure `subscriptions` : un `Map<String`, `Set<CibleInfo>>` associant chaque groupe à l’ensemble des cibles qui s’y sont abonnées.

Méthodes `joinGroup` et `leaveGroup` :

`joinGroup` ajoute la cible dans subscriptions[groupe].

`leaveGroup` la retire.

Méthode `heartbeat` : met à jour la timestamp de la cible dans subscriptions, si elle est toujours présente.

`Diffusion` : dans `recevoirMessage`, si le message est préfixé par "`group:<nomGroupe>;`", on n’envoie qu’aux cibles abonnées à <nomGroupe> ; sinon on envoie à toutes les cibles connues.

### ApplicationCible

1. Mise à jour du menu interactif
    Dans la boucle du menu, on demande à l’utilisateur de saisir un groupe cible pour une diffusion restreinte. Si l'utilisateur fournit un groupe, le message est préfixé avec "`group:<nomDuGroupe>;`".
2. Méthode d'envoi 
    La méthode `envoyerMessage` reste inchangée, elle envoie simplement le message tel qu'il est fourni. Le préfixe sera interprété par les recouvrements.
3. Option "Rejoindre un groupe" => appelle noeudRecouvrement.joinGroup(groupe, monNom);

4. Option "Quitter un groupe" => appelle `noeudRecouvrement.leaveGroup(groupe, monNom);`

5. Liste des groupes rejoints (`joinedGroups`) : stockés localement pour que l’application sache à quels groupes elle est abonnée.

Thread de heartbeat : envoie périodiquement `noeudRecouvrement.heartbeat(groupe, monNom)`; pour chaque groupe abonné, afin de signaler que la cible est toujours active.

## Heartbeat et vérification périodique
Le recouvrement lance un ScheduledExecutorService (initHeartbeat()) qui, toutes les X secondes, vérifie pour chaque groupe si la dernière timestamp (lastHeartbeat) dépasse un certain seuil (ex. 30 secondes). Si c’est le cas, la cible est considérée inactive et on la retire de subscriptions.

L’application cible, de son côté, envoie un heartbeat (ex. toutes les 10 secondes) pour chaque groupe auquel elle est abonnée. Ainsi, si elle s’arrête brutalement, le recouvrement la détecte et l’exclut de la diffusion.


## Diffusion dynamique et restreinte.

Le système permet aux cibles de rejoindre ou de quitter un groupe à tout moment via des commandes joinGroup et leaveGroup. Le recouvrement, qui maintient une table dynamique (subscriptions), ne diffuse les messages qu’aux cibles actives qui ont rejoint le groupe ciblé. Ainsi, l’arbre de diffusion se construit et se met à jour en temps réel en fonction des abonnements.

Routes et Flux Multicast :
Contrairement à un protocole multicast complet (comme DVMRP ou PIM) qui calcule dynamiquement des routes optimales en fonction de la topologie du réseau et des flux de trafic, notre code ne "réapprend" pas automatiquement des routes de diffusion multicast.
Il simule plutôt ce comportement par le biais du mécanisme d’abonnement : seuls les nœuds qui se sont abonné à un groupe (et qui envoient des heartbeats) font partie de l’arbre de diffusion pour ce groupe.
En résumé, même si nous n’utilisons pas un algorithme de routage multicast sophistiqué, la combinaison de join/leave dynamique et de heartbeats permet de restreindre la diffusion aux cibles intéressées et actives, ce qui répond aux exigences de diffusion dynamique et restreinte.

Ainsi, notre système construit un "arbre" de diffusion en temps réel, basé sur les abonnements (joins) et la vérification périodique (heartbeats) pour ne transmettre les messages qu’aux cibles actives appartenant à un groupe donné, simulant ainsi un environnement multicast dynamique.

## Compilation et exécution

Rien de changé.

## Amélioration et couleurs
Les messages sont en couleurs à présents, donc plus facile à distinguer. (DEBUG en bleu, SUCCESS en vert, INFO en jaune, ERROR en rouge, Heartbeat et SUBSCRIPTIONS en bleau clair, et Messages en violet...)


## Dépannage

1. Vérifiez la configuration :
Assurez-vous que le fichier reseau.json contient bien les adresses et groupes.

2. Démarrez les recouvrements :
Lancer les instances des applications de recouvrement sur les machines ou terminaux appropriés.
  - Vérifiez les logs : Les recouvrements doivent afficher le message avec le préfixe de groupe et transmettre uniquement aux cibles correspondantes

3. Démarrez les applications cibles :
Lancez chaque application cible. Chaque cible va s'enregistrer dans le registre RMI et se connecter au recouvrement défini dans reseau.json.


## NB 
N'hésitez pas à augmenter le délai de tentative de reconnexion entre les applications de recouvrement lorsque ces derniers ne sont pas tous allumés, pour éviter d'avoir toutes les cinq secondes des logs d'erreurs.

