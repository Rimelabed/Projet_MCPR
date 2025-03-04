# Réseau de Recouvrement avec Java RMI
### 1️⃣ Présentation du Projet
Ce projet met en place un réseau de recouvrement permettant la communication entre deux applications cibles (AppCible_1 et AppCible_2) via un nœud de recouvrement (AppRecouv_1). L’implémentation repose sur Java RMI, facilitant l’échange de messages entre ces applications sur un réseau distribué.

L'objectif est de structurer un réseau où les applications cibles ne communiquent pas directement entre elles, mais passent obligatoirement par un serveur intermédiaire (AppRecouv_1), qui assure le routage des messages.

### 2️⃣ Infrastructure et Configuration des Machines
Les machines utilisées sont connectées à l’infrastructure de l’école et accessibles en SSH via IPv6.

**📌 Ajout dans le fichier ~/.ssh/config pour faciliter les connexions :**

``` text
Host AppRecouv_1
    AddressFamily inet6
    HostName 2001:678:3fc:3c:baad:caff:fefe:a9
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

📌 Avec cette configuration, on peut se connecter facilement à chaque machine avec :

``` bash
ssh AppRecouv_1
ssh AppCible_1
ssh AppCible_2
```
### 3️⃣ Organisation des Fichiers
Les fichiers sont distribués sur trois machines virtuelles. Chaque machine a les fichiers nécessaires à son rôle. Tous situés dans le répertoire `_PROJET_MCPR` 

### 4️⃣ Compilation
Chaque machine doit compiler les fichiers nécessaires avant l'exécution.
**Sur `AppRecouv_1` qui est le serveur RMI en passant :**
``` bash
javac RmiNodeInterface.java
javac ApplicationRecouvrement.java
javac MainServeur.java
```

**📌 Sur AppCible_1 et AppCible_2 (Applications Cibles)**

``` bash
javac RmiNodeInterface.java
javac ApplicationCible.java
```
### 5️⃣ Exécution des Applications
📌 Les applications doivent être démarrées dans l’ordre suivant : 

**1️⃣ Démarrer Rec1 (Serveur RMI) sur AppRecouv_1**

``` bash
ssh AppRecouv_1
java MainServeur
```
**Résultat attendu :**

``` bash 
Serveur RMI prêt !
```

**2️⃣ Démarrer AppCible_1**

``` bash
java ApplicationCible <NomApp> <IPAppRecouvrement> <IPAppCible1>
```

Même syntaxe pour Appcible2, seul l'ip change. Dans notre infra, ils ont tous les deux le même app de recouvrement.

### 6️⃣ Explication du Code

**📌 ApplicationRecouvrement.java**
Ce fichier représente le serveur RMI (Rec1).
Il reçoit les messages envoyés par AppCible_1 ou AppCible_2, puis les retransmet à l’autre cible.

**📌 ApplicationCible.java**
Chaque Application Cible contacte Rec1 et envoie un message.
Rec1 s’occupe de relayer le message à l’autre ApplicationCible.

**📌 RmiNodeInterface.java**
Interface partagée entre toutes les machines définissant les méthodes RMI utilisées pour envoyer des messages.