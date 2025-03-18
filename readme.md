# MCPR - Projet Overlay
## Étape 3 : Diffusion et Réception de Contenu

Ce projet vise à créer une application distribuée permettant de diffuser des messages courts via un réseau de recouvrement. Dans cette étape, les **applications cibles** (clients) sont capables d'envoyer et de recevoir des messages via des **applications de recouvrement** (nœuds de routage), en utilisant RMI pour la communication.

Ce README détaille comment configurer, compiler, exécuter et tester l'étape 3 de votre projet.

---

## Table des Matières
- [Introduction](#introduction)
- [Prérequis](#prérequis)
- [Configuration](#configuration)
- [Compilation](#compilation)
- [Exécution](#exécution)
  - [Lancer une Application de Recouvrement](#lancer-une-application-de-recouvrement)
  - [Lancer une Application Cible](#lancer-une-application-cible)
- [Menu Interactif et Tests](#menu-interactif-et-tests)
- [Dépannage](#dépannage)
- [Prochaines Étapes](#prochaines-étapes)

---

## Introduction

Le projet MCPR consiste à développer une solution de diffusion de messages courts reposant sur un réseau d'applications de recouvrement et d'applications cibles.  
Dans **l'étape 3**, chaque application cible :
- S'enregistre dans le registre RMI.
- Se connecte à une application de recouvrement (déterminée à partir du fichier de configuration `reseau.json`).
- Possède un menu interactif permettant de tester la connexion et d'envoyer des messages personnalisés.

Les applications de recouvrement se chargent de recevoir les messages et de les propager vers les cibles concernées.

---

## Prérequis

- **JDK 8 ou supérieur** : Assurez-vous d'avoir une version récente de Java.
- **Accès au Port 1099** : Le RMI Registry doit être accessible (vérifiez que le port 1099 n'est pas bloqué par un pare-feu).
- **Configuration Réseau** : Un fichier `reseau.json` correctement configuré pour décrire la topologie du réseau.
- **Environnement de Développement** : VSCode, Eclipse, ou tout autre IDE.

---

---

## Configuration

``` diff
Host AppRecouv_2
    AddressFamily inet6
    Hostname 2001:678:3fc:3c:baad:caff:fefe:d6
    User etu
    Port 2222
    ForwardAgent yes

Host AppRecouv_3
    AddressFamily inet6
    Hostname 2001:678:3fc:3c:baad:caff:fefe:d7
    User etu
    Port 2222
    ForwardAgent yes

Host AppRecouv_1
    AddressFamily inet6
    HostName 2001:678:3fc:3c:baad:caff:fefe:a9
    User etu
    Port 2222
    ForwardAgent yes

Host AppCible_3
    AddressFamily inet6
    HostName 2001:678:3fc:3c:baad:caff:fefe:d8
    User etu
    Port 2222
    ForwardAgent yes

Host AppCible_1
    AddressFamily inet6
    HostName 2001:678:3fc:3c:baad:caff:fefe:a0
    User etu
    Port 2222
    ForwardAgent yes

Host AppCible_2
    AddressFamily inet6
    HostName 2001:678:3fc:3c:baad:caff:fefe:a1
    User etu
    Port 2222
    ForwardAgent yes
    
```

Modifiez le fichier `reseau.json` pour refléter votre configuration réseau. Exemple :

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
    "AppCible_1": "198.18.61.160",
    "AppCible_2": "198.18.61.161",
    "AppCible_3": "198.18.60.245"
  }
}

```

## Compilation
Compilez l'ensemble des fichiers Java depuis le répertoire du projet :
``` bash
javac *.java
```


## Exécution
Lancer une Application de Recouvrement
Sur la machine prévue pour le recouvrement, lancez l'application via le MainServeur. Par exemple :

``` bash
java MainServeur AppRecouv_1
``` 

Ce lancement :

Charge la configuration réseau depuis reseau.json.
Crée un registre RMI (port 1099).
Enregistre l'objet de recouvrement dans le registre.
Vous pouvez lancer d'autres recouvrements en passant le nom approprié (AppRecouv_2, AppRecouv_3, etc.) sur d'autres machines ou dans des fenêtres séparées.

Lancer une Application Cible
Sur la machine destinée à être une cible, lancez l'application cible. Par exemple :

``` bash
java ApplicationCible AppCible_1
``` 

Cela fera en sorte que :

-L'application cible charge le fichier reseau.json pour déterminer à quel recouvrement elle doit se connecter.
-Elle crée un registre RMI local et s'enregistre avec son nom.
-Une fois enregistrée, elle se connecte au recouvrement approprié et envoie un message de test.

## Menu Interactif et Tests
Après l'exécution, l'application cible affiche un menu interactif, par exemple :
``` csharp
[INFO] AppCible_1 est associé à AppRecouv_3 @ 198.18.60.239
[DEBUG] AppCible_1 tente de se connecter à AppRecouv_3 via RMI...
[SUCCESS] AppCible_1 est connecté à AppRecouv_3 !
[INFO] AppCible_1 enregistré dans le registre RMI.
Hello depuis AppCible_1 !

```

Puis, le menu : 

``` diff
=== Menu de AppCible_1 ===
1. Tester la connexion avec le réseau
2. Envoyer un message court personnalisé
0. Quitter
Votre choix :
```

Option 1 : Envoie un message de test pour vérifier la connexion avec le réseau.
Option 2 : Permet de saisir un message personnalisé qui sera transmis via le recouvrement.
Option 0 : Quitte l'application.
Testez les fonctionnalités en choisissant différentes options et vérifiez que :

Le message de test ou personnalisé est bien envoyé.
Les autres cibles et les recouvrements affichent les logs indiquant la réception du message.

## Dépannage :

- Port RMI : Assurez-vous que le port 1099 est libre et accessible sur chaque machine.
- Configuration reseau.json : Vérifiez que le fichier est correctement formaté et que les adresses IP correspondent à votre environnement.
- Logs : Utilisez les messages [DEBUG], [INFO] et [ERREUR] affichés dans la console pour diagnostiquer d'éventuels problèmes de connexion ou d'enregistrement.

## Prochaines Étapes
- **Diffusion Restreinte** : Pour l'étape 4, nous allons ajouter des attributs (ex : groupe ou caractéristiques) à chaque application cible et modifier la logique de diffusion pour qu'elle ne s'adresse qu'aux cibles concernées.Peut être en modifiant directement le fichier de topologie reseau.json ?

- **Optimisation du Routage** : Nous allons également explorer des mécanismes de filtrage inspirés des algorithmes de multicast (par exemple, reverse path broadcasting) pour optimiser la diffusion.

- **Interface Utilisateur Améliorée** : Tenter d'enrichir le menu interactif pour offrir plus d'options de test ou de configuration.
