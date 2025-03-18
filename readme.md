# MCPR - Projet Overlay
## Étape 4 : Diffusion Restreinte par Groupes

Cette étape introduit la diffusion restreinte, c’est-à-dire que les messages ne sont envoyés qu’aux applications cibles appartenant à un groupe spécifique. L’objectif est d’éviter une diffusion totale du message et de limiter la propagation uniquement aux cibles intéressées (par exemple, groupe "ventes", "RH", etc.).

---

## Table des Matières

- [Introduction](#introduction)
- [Prérequis](#prérequis)
- [Nouvelle Topologie JSON](#nouvelle-topologie-json)
- [Modifications du Code](#modifications-du-code)
  - [Classe CibleInfo](#classe-cibleinfo)
  - [ApplicationRecouvrement](#applicationrecouvrement)
  - [ApplicationCible](#applicationcible)
- [Compilation et Exécution](#compilation-et-ex%C3%A9cution)
- [Tutoriel de Test](#tutoriel-de-test)
- [Dépannage](#d%C3%A9pannage)
- [Prochaines Étapes](#prochaines-%C3%A9tapes)

---

## Introduction

Dans cette étape, nous restreignons la diffusion des messages aux applications cibles qui appartiennent à un groupe particulier.  
Pour cela, nous :
- **Mettons à jour le fichier de configuration JSON** pour y inclure un attribut `"groupe"` pour chaque application cible.
- **Créons une nouvelle classe `CibleInfo`** pour stocker la référence distante et le groupe de chaque cible.
- **Modifions l'application de recouvrement** pour qu'elle filtre les messages en fonction du groupe indiqué dans le message.
- **Adaptation de l'interface utilisateur** dans l'application cible pour permettre à l'utilisateur d'envoyer un message ciblé sur un groupe spécifique.

---

## Prérequis

- **JDK 8 ou supérieur**  
- **Accès au port 1099** pour le registre RMI  
- **Configuration correcte** du fichier `reseau.json`  
- **Connaissance de base de RMI et JSON**

---

## Nouvelle Topologie JSON

Le fichier `reseau.json` est modifié pour intégrer les informations de groupe pour les applications cibles. Voici un exemple de configuration mise à jour :

```json
{
  "recouvrements": {
    "AppRecouv_1": {
      "adresse": "198.18.61.169",
      "voisins": {
        "AppRecouv_2": 1,
        "AppRecouv_3": 1
      }
    },
    "AppRecouv_2": {
      "adresse": "198.18.60.238",
      "voisins": {
        "AppRecouv_1": 1,
        "AppCible_3": 1
      }
    },
    "AppRecouv_3": {
      "adresse": "198.18.60.239",
      "voisins": {
        "AppRecouv_1": 1,
        "AppCible_1": 1,
        "AppCible_2": 1
      }
    }
  },
  "applications_cibles": {
    "AppCible_1": { "adresse": "198.18.61.160", "groupe": "ventes" },
    "AppCible_2": { "adresse": "198.18.61.161", "groupe": "RH" },
    "AppCible_3": { "adresse": "198.18.60.245", "groupe": "ventes" }
  }
}
```

Chaque application cible a désormais une adresse et un attribut "groupe" indiquant son appartenance.

## Modifications du Code

### Classe CibleInfo 

Création d'un fichier CibleInfo.java qui encapsule la référence RMI et le groupe de l’application cible :

``` java

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
```

### ApplicationRecouvrement
1. Modification de la découverte des cibles
    Dans la méthode qui parcourt le JSON pour trouver les applications cibles, on récupère l'objet JSON spécifique à chaque cible afin d'extraire à la fois "adresse" et "groupe". 
2. Filtrage lors de la diffusion des messages
    Dans la méthode `recevoirMessage`, on vérifie si le contenu du message contient un préfixe indiquant le groupe ciblé. Si oui, on extrait ce groupe et le message n'est diffusé qu'aux cibles dont l'attribut "groupe" correspond.

### ApplicationCible

1. Mise à jour du menu interactif
    Dans la boucle du menu, on demande à l’utilisateur de saisir un groupe cible pour une diffusion restreinte. Si l'utilisateur fournit un groupe, le message est préfixé avec "group:<nomDuGroupe>;".
2. Méthode d'envoi 
    La méthode `envoyerMessage` reste inchangée, elle envoie simplement le message tel qu'il est fourni. Le préfixe sera interprété par les recouvrements.

## Compilation et exécution

Rien de changé.

## Tutoriel de test

1. Vérifiez la configuration :
Assurez-vous que le fichier reseau.json contient bien les adresses et groupes.

2. Démarrez les recouvrements :
Lancer les instances des applications de recouvrement sur les machines ou terminaux appropriés.

3. Démarrez les applications cibles :
Lancez chaque application cible. Chaque cible va s'enregistrer dans le registre RMI et se connecter au recouvrement défini dans reseau.json.

4. Utilisez le menu interactif de l'application cible :
    - Option 1 : Envoyer un message de test (diffusion totale) et observer dans les logs des recouvrements et des autres cibles que le message est reçu.
    - Option 2 : Saisir un groupe cible (par exemple, "ventes" ou "RH") et un message personnalisé. Seules les applications cibles appartenant à ce groupe devraient recevoir le message.
    - Vérifiez les logs : Les recouvrements doivent afficher le message avec le préfixe de groupe et transmettre uniquement aux cibles correspondantes.

## NB 
N'hésitez pas à augmenter le délai de tentative de reconnexion entre les applications de recouvrement lorsque ces derniers ne sont pas tous allumés, pour éviter d'avoir toutes les cinq secondes des logs d'erreurs.

## Suggestion : 

Différencier les messages provenant des cibles avec une coloration sur le terminal, par exemple. Faire de même pour les messages de logs (DEBUG en jaune, SUCCESS en vert, ERROR en rouge et Messages en bleu...)
