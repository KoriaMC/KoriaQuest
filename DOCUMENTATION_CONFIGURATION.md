# KoriaQuest : documentation de configuration

Ce document décrit **tous les réglages** de KoriaQuest : où ils se trouvent, leur valeur par défaut et leur effet,
ainsi que la manière d'écrire des quêtes et des récompenses. Il s'adresse aux administrateurs du serveur.

## 1. Principes

### Fichiers

Dans `plugins/KoriaQuest/` :

| Fichier | Contenu |
|---|---|
| `config.json` | stockage, commande `/quetes` et sous-commandes, renouvellements, difficultés, quêtes de faction et joueur, anti-abus, notifications, affichage |
| `quests_faction.json` | quêtes de faction, par difficulté |
| `quests_player.json` | quêtes joueur, par difficulté |
| `rewards.json` | récompenses par quête et bonus par palier |
| `messages.json` | tous les textes envoyés aux joueurs |
| `menus.json` | menus joueur, difficulté, faction et éditeur |
| `LISEZMOI.txt` | explication des options, **régénéré à chaque démarrage** |
| `data/`, `blocs-poses.dat` | progression des joueurs et des factions, blocs posés (ne pas modifier serveur allumé) |

### Fonctionnement en bref

- **Quêtes de faction, chaque semaine** (jour et heure de `cycle`) : `quetes-faction.nombre` quêtes sont tirées par
  difficulté, **les mêmes pour toutes les factions**. Tous les membres font avancer la même progression. Les joueurs
  les ouvrent avec `/quetes faction` ou `/f quests`.
- **Quêtes joueur, chaque jour** (heure de `cycle-joueur`) : le joueur choisit une difficulté, puis reçoit
  `quetes-joueur.nombre` quêtes de cette difficulté, tirées pour lui. `cycle-joueur.periode` = `HEBDOMADAIRE` les
  renouvelle plutôt chaque semaine, avec les factions.
- À chaque renouvellement, la progression concernée repart de zéro et les quêtes non terminées sont perdues.
- **Récompenses** : à chaque quête terminée, et en bonus à chaque palier (nombre de quêtes terminées dans la période).
  Les points de faction (`POINTS`) apparaissent dans `/f top points` de KoriaFactions.

### Écrire du JSON

Texte entre guillemets doubles, nombres et `true` / `false` sans guillemets, une virgule entre deux éléments et
jamais après le dernier, listes entre crochets, **pas de commentaires**. Une erreur est signalée dans la console avec
la ligne et la colonne ; au rechargement, l'ancienne version du fichier reste active jusqu'à correction.

### Valeurs par défaut et sections libres

Une option absente prend la valeur par défaut du plugin ; supprimer un fichier le régénère au démarrage suivant.
Les **sections libres** ne sont pas complétées par les valeurs par défaut : on peut y ajouter et y supprimer des
entrées. Ce sont les quêtes (tout `quests_faction.json` et `quests_player.json`), `difficultes`,
`quetes-faction.nombre`, les récompenses par difficulté et les paliers de `rewards.json`, et les `objets` des menus.

### Appliquer les changements

- `/quetes recharger` recharge tout et liste les problèmes (quête invalide, matériau inconnu...).
- Redémarrage nécessaire pour `stockage.type` et le nom / les alias de la commande principale.
- **Changer le jour, l'heure ou le fuseau de `cycle` démarre immédiatement une nouvelle semaine de quêtes de
  faction ; changer `cycle-joueur` renouvelle immédiatement les quêtes joueur.**
- Les quêtes déjà tirées restent en place : une nouvelle quête n'apparaît qu'au prochain tirage.

### Éditeur en jeu : `/quetes config`

Permission `koriaquest.admin.config` (OP par défaut). Même fonctionnement que `/f config` : clic pour basculer,
clic gauche / droit pour augmenter / diminuer un nombre, touche Q pour saisir au clavier, clic sur un choix pour
passer à la valeur suivante (ex. le `type` d'une quête), Ctrl + Q pour revenir à la valeur par défaut ou
supprimer une entrée libre. Le bouton émeraude d'une section libre **copie la dernière entrée** sous un nouveau nom :
c'est le moyen le plus simple de créer une quête ou un palier.

### Formats communs

| Format | Exemple | Règle |
|---|---|---|
| Couleurs | `&aFacile` | codes `&0` à `&f`, `&l` gras, `&o` italique, `&r` réinitialiser |
| Matériaux | `DIAMOND_ORE`, `56`, `35:14` | nom 1.8.8, ID ou ID:data (blocs moddés compris) |
| Entités | `ZOMBIE`, `COW`, `PLAYER` | noms d'entités 1.8.8 |
| Sons | `LEVEL_UP`, `NOTE_PLING:1:2` | nom 1.8.8, puis volume et hauteur optionnels ; `""` = aucun son |
| Territoires | `WILDERNESS`, `SAFEZONE`, `WARZONE`, `OWN`, `ALLY`, `ENEMY` | territoire KoriaFactions vu par le joueur |
| Ticks | `40` | 20 ticks = 1 seconde |

## 2. `config.json`

### Options générales

| Option | Défaut | Description |
|---|---|---|
| `debug` | `false` | Affiche des informations supplémentaires dans la console. |

### `stockage`

Où sont enregistrées les quêtes des joueurs et des factions. Changer le type nécessite un redémarrage.

| Option | Défaut | Description |
|---|---|---|
| `stockage.​type` | `"JSON"` | Type de stockage (redémarrage nécessaire). MYSQL fonctionne aussi avec MariaDB.<br>*Valeurs :* `JSON`, `SQLITE`, `MYSQL` |
| `stockage.​sauvegarde-auto-minutes` | `5` | Sauvegarde automatique toutes les X minutes (0 = seulement à l'arrêt). |
| `stockage.​copies-de-securite` | `3` | Nombre de copies de sécurité des fichiers JSON conservées (0 = aucune). |
| `stockage.​json-indente` | `false` | Fichiers de données JSON lisibles (plus gros) ou compacts. |
| `stockage.​sqlite-fichier` | `"donnees.db"` | Nom du fichier SQLite dans le dossier du plugin. |
| `stockage.​mysql.​hote` | `"localhost"` | Adresse du serveur MySQL / MariaDB. |
| `stockage.​mysql.​port` | `3306` | Port du serveur MySQL / MariaDB. |
| `stockage.​mysql.​base` | `"koriaquest"` | Nom de la base de données. |
| `stockage.​mysql.​utilisateur` | `"root"` | Utilisateur de la base. |
| `stockage.​mysql.​mot-de-passe` | `""` | Mot de passe de la base. |
| `stockage.​mysql.​prefixe-tables` | `"kq_"` | Préfixe ajouté au nom des tables. |
| `stockage.​mysql.​parametres` | `"?useSSL=false&characterEncoding=utf8"` | Paramètres ajoutés à l'adresse de connexion. |

### `commande`

Commande principale (/quetes). Changer le nom nécessite un redémarrage.

| Option | Défaut | Description |
|---|---|---|
| `commande.​nom` | `"quetes"` | Nom de la commande principale (redémarrage nécessaire). |
| `commande.​alias` | `["quests", "quete", "q"]` | Autres noms de la commande principale (redémarrage nécessaire). |
| `commande.​description` | `"Quêtes hebdomadaires"` | Description de la commande (aide du serveur). |

### `commandes`

Sous-commandes. L'usage et la description affichés dans l'aide sont dans messages.json (help.commands).

| Entrée | `actif` | `alias` | `permission` |
|---|---|---|---|
| `aide` | `true` | `["help", "?"]` | `"koriaquest.command.aide"` |
| `menu` | `true` | `["joueur", "mes-quetes"]` | `"koriaquest.command.menu"` |
| `faction` | `true` | `["f", "fac"]` | `"koriaquest.command.faction"` |
| `difficulte` | `true` | `["diff", "choisir"]` | `"koriaquest.command.difficulte"` |
| `temps` | `true` | `["reset", "fin"]` | `"koriaquest.command.temps"` |
| `voir` | `true` | `["info"]` | `"koriaquest.admin.voir"` |
| `progression` | `true` | `["progres"]` | `"koriaquest.admin.progression"` |
| `reinitialiser` | `true` | `["reinit"]` | `"koriaquest.admin.reinitialiser"` |
| `renouveler` | `true` | `["nouvelle-semaine"]` | `"koriaquest.admin.renouveler"` |
| `config` | `true` | `["configuration", "cfg"]` | `"koriaquest.admin.config"` |
| `sauvegarder` | `true` | `["save"]` | `"koriaquest.admin.sauvegarder"` |
| `recharger` | `true` | `["reload"]` | `"koriaquest.admin.recharger"` |

- `actif` : false pour désactiver complètement la commande.
- `alias` : Autres noms de la sous-commande.
- `permission` : Permission requise (vide = aucune). Donnée à tous par défaut, aux OP pour les commandes d'administration.

### `cycle`

Semaine des quêtes de faction (et fuseau horaire commun). Changer le jour, l'heure ou le fuseau démarre aussitôt une nouvelle semaine.

| Option | Défaut | Description |
|---|---|---|
| `cycle.​jour` | `"LUNDI"` | Jour du renouvellement des quêtes de faction.<br>*Valeurs :* `LUNDI`, `MARDI`, `MERCREDI`, `JEUDI`, `VENDREDI`, `SAMEDI`, `DIMANCHE` |
| `cycle.​heure` | `"00:00"` | Heure du renouvellement des quêtes de faction (HH:mm). |
| `cycle.​fuseau` | `"Europe/Paris"` | Fuseau horaire de tous les renouvellements (ex : Europe/Paris). |
| `cycle.​annonce-nouvelle-semaine` | `true` | Annonce à tout le serveur le renouvellement des quêtes de faction. |
| `cycle.​rappel-quetes-non-terminees` | `true` | À chaque renouvellement, prévient les joueurs connectés des quêtes qu'ils n'ont pas terminées. |

### `cycle-joueur`

Renouvellement des quêtes joueur. Changer la période ou l'heure renouvelle aussitôt les quêtes joueur.

| Option | Défaut | Description |
|---|---|---|
| `cycle-joueur.​periode` | `"QUOTIDIEN"` | QUOTIDIEN : chaque jour à l'heure indiquée. HEBDOMADAIRE : en même temps que les quêtes de faction.<br>*Valeurs :* `QUOTIDIEN`, `HEBDOMADAIRE` |
| `cycle-joueur.​heure` | `"00:00"` | Heure du renouvellement quotidien (HH:mm). |
| `cycle-joueur.​annonce-renouvellement` | `true` | Annonce à tout le serveur le renouvellement des quêtes joueur. |

### `difficultes`

Difficultés disponibles. Chaque difficulté doit avoir ses quêtes dans quests_*.json et ses récompenses dans rewards.json.

| Entrée | `nom` |
|---|---|
| `facile` | `"&aFacile"` |
| `moyen` | `"&6Moyen"` |
| `difficile` | `"&cDifficile"` |

- `nom` : Nom affiché de la difficulté ({difficulte}).

### `quetes-faction`

Quêtes de faction : les mêmes quêtes pour toutes les factions, progression partagée par les membres.

| Option | Défaut | Description |
|---|---|---|
| `quetes-faction.​actif` | `true` | Active les quêtes de faction. |
| `quetes-faction.​nombre.​facile` | `1` | Nombre de quêtes de cette difficulté. |
| `quetes-faction.​nombre.​moyen` | `1` | Nombre de quêtes de cette difficulté. |
| `quetes-faction.​nombre.​difficile` | `1` | Nombre de quêtes de cette difficulté. |
| `quetes-faction.​membres-minimum` | `1` | Membres nécessaires pour qu'une faction ait des quêtes. |
| `quetes-faction.​prevenir-la-faction` | `true` | Prévient les membres connectés quand une quête de faction est terminée. |
| `quetes-faction.​annonce-globale` | `false` | Annonce à tout le serveur chaque quête de faction terminée. |

### `quetes-joueur`

Quêtes personnelles : le joueur choisit une difficulté et reçoit des quêtes de cette difficulté.

| Option | Défaut | Description |
|---|---|---|
| `quetes-joueur.​actif` | `true` | Active les quêtes joueur. |
| `quetes-joueur.​nombre` | `3` | Nombre de quêtes données au joueur après son choix. |
| `quetes-joueur.​changer-difficulte` | `false` | Autorise à changer de difficulté avant le renouvellement (la progression repart de zéro). |
| `quetes-joueur.​confirmation-choix` | `true` | Le choix de la difficulté doit être confirmé par un second clic. |
| `quetes-joueur.​sans-faction` | `"BLOQUER"` | Joueur sans faction : BLOQUER (pas de quêtes) ou AUTORISER (points perdus, autres récompenses données).<br>*Valeurs :* `BLOQUER`, `AUTORISER` |
| `quetes-joueur.​ouvrir-choix-automatiquement` | `true` | /quetes ouvre directement le choix de la difficulté tant qu'elle n'est pas choisie. |

### `anti-abus`

Protections contre la triche aux quêtes.

| Option | Défaut | Description |
|---|---|---|
| `anti-abus.​mondes-desactives` | `[]` | Mondes où les quêtes ne progressent pas. |
| `anti-abus.​ignorer-creatif` | `true` | Les joueurs en créatif ne font pas avancer les quêtes. |
| `anti-abus.​ignorer-blocs-poses` | `true` | Un bloc posé par un joueur ne compte pas quand il est cassé (CASSER_BLOC, RECOLTER). |
| `anti-abus.​blocs-poses-memoire-max` | `200000` | Nombre maximum de blocs posés mémorisés (les plus anciens sont oubliés). |
| `anti-abus.​ignorer-mobs-spawner` | `true` | Les mobs sortis d'un spawner ne comptent pas (TUER_MOB, INFLIGER_DEGATS). |
| `anti-abus.​ignorer-mobs-oeufs` | `true` | Les mobs sortis d'un œuf ne comptent pas. |
| `anti-abus.​delai-meme-victime-minutes` | `30` | Minutes avant qu'un nouveau kill sur la même victime compte (0 = aucun délai). |
| `anti-abus.​ignorer-meme-ip` | `true` | Un kill sur un joueur de même adresse IP ne compte pas. |
| `anti-abus.​afk-minutes` | `5` | Minutes sans bouger la caméra ni parler avant d'être AFK : le temps AFK ne compte pas (TEMPS_EN_LIGNE). 0 = désactivé. |
| `anti-abus.​parcourir-en-vehicule` | `false` | La distance parcourue en véhicule (bateau, cheval, minecart) compte pour PARCOURIR. |

### `notifications`

Messages pendant la progression.

| Option | Défaut | Description |
|---|---|---|
| `notifications.​progression-actionbar` | `true` | Affiche la progression (quest.progress) à chaque action utile. |
| `notifications.​paliers-pourcentage` | `[25,50,75]` | Pourcentages qui déclenchent le message quest.milestone. |
| `notifications.​sons.​progression` | `""` | Son (nom 1.8.8, ex : LEVEL_UP, ORB_PICKUP, NOTE_PLING). |
| `notifications.​sons.​palier` | `"ORB_PICKUP"` | Son (nom 1.8.8, ex : LEVEL_UP, ORB_PICKUP, NOTE_PLING). |
| `notifications.​sons.​quete-terminee` | `"LEVEL_UP"` | Son (nom 1.8.8, ex : LEVEL_UP, ORB_PICKUP, NOTE_PLING). |
| `notifications.​sons.​bonus` | `"ENDERDRAGON_GROWL:0.6:1.2"` | Son (nom 1.8.8, ex : LEVEL_UP, ORB_PICKUP, NOTE_PLING). |
| `notifications.​sons.​choix-difficulte` | `"NOTE_PLING"` | Son (nom 1.8.8, ex : LEVEL_UP, ORB_PICKUP, NOTE_PLING). |
| `notifications.​sons.​clic-menu` | `"CLICK:0.5:1"` | Son (nom 1.8.8, ex : LEVEL_UP, ORB_PICKUP, NOTE_PLING). |
| `notifications.​sons.​erreur` | `"VILLAGER_NO"` | Son (nom 1.8.8, ex : LEVEL_UP, ORB_PICKUP, NOTE_PLING). |

### `affichage`

Affichage de la progression dans les menus et messages.

| Option | Défaut | Description |
|---|---|---|
| `affichage.​barre.​longueur` | `20` | Nombre de symboles de la barre. |
| `affichage.​barre.​symbole` | `"\|"` | Symbole répété. |
| `affichage.​barre.​couleur-pleine` | `"&a"` | Couleur de la partie accomplie. |
| `affichage.​barre.​couleur-vide` | `"&8"` | Couleur de la partie restante. |
| `affichage.​contributeurs-affiches` | `5` | Nombre de meilleurs contributeurs affichés sur une quête de faction ({contributeurs}). |
| `affichage.​decimales-pourcentage` | `0` | Décimales du pourcentage ({pourcentage}), de 0 à 2. |

### `titres`

Durées des titres ([title] dans messages.json), en ticks (20 = 1 seconde).

| Option | Défaut | Description |
|---|---|---|
| `titres.​apparition` | `5` | Durée d'apparition. |
| `titres.​duree` | `40` | Durée d'affichage. |
| `titres.​disparition` | `10` | Durée de disparition. |


**Deux renouvellements.** `cycle` règle la semaine des quêtes de faction : jour `LUNDI` à `DIMANCHE` (ou les noms
anglais), heure au format `HH:mm`, et le fuseau (`Europe/Paris`) commun aux deux. `cycle-joueur` règle les quêtes
joueur : `QUOTIDIEN` (chaque jour à `cycle-joueur.heure`, par défaut) ou `HEBDOMADAIRE` (en même temps que les
factions). `/quetes temps` affiche les deux temps restants.

**Difficultés.** Les identifiants de `difficultes` sont utilisés partout : sections des fichiers de quêtes,
`quetes-faction.nombre`, `rewards.json` et actions `CHOISIR:<id>` du menu. Voir « Ajouter une difficulté »
dans les tâches courantes.

**Objectifs de faction et taille.** L'objectif d'une quête de faction vaut `objectif + par-membre × membres`,
calculé la première fois que la faction est concernée dans la semaine (action d'un membre, ouverture du menu) ;
il ne change plus ensuite, même si la faction grandit.

## 3. Écrire une quête

Les deux fichiers ont la même forme : une section par difficulté, et dans chacune une entrée par quête, dont le nom
de section est l'**identifiant** (unique dans sa difficulté, sans espace).

```json
"moyen": {
  "fer": {
    "actif": true,
    "nom": "&fMineur de fer",
    "type": "CASSER_BLOC",
    "cibles": ["IRON_ORE"],
    "objectif": 150,
    "mondes": ["minage"],
    "zones": [],
    "poids": 10,
    "icone": "IRON_ORE",
    "description": ["&7Minez &f{objectif} &7minerais de fer."],
    "recompenses": []
  }
}
```

### Clés d'une quête

| Clé | Type | Description |
|---|---|---|
| `actif` | oui / non | false pour retirer la quête du tirage. |
| `nom` | texte | Nom affiché ({quete}). |
| `type` | texte | Type d'objectif.<br>*Valeurs :* `CASSER_BLOC`, `POSER_BLOC`, `RECOLTER`, `TUER_MOB`, `TUER_JOUEUR`, `INFLIGER_DEGATS`, `PECHER`, `CRAFTER`, `FONDRE`, `ENCHANTER`, `BRASSER`, `CONSOMMER`, `ELEVER`, `APPRIVOISER`, `TONDRE`, `PARCOURIR`, `TEMPS_EN_LIGNE`, `GAGNER_EXPERIENCE`, `MONTER_NIVEAU`, `LIVRER`, `CLAIMER` |
| `cibles` | liste | Ce qui compte (vide = tout) : matériaux (nom, ID ou ID:data) pour les blocs et objets, types d'entités (ZOMBIE, COW...) pour les mobs et animaux, relation de la victime (ENEMY, ALLY, NO_FACTION) pour TUER_JOUEUR. |
| `objectif` | nombre | Quantité à atteindre (blocs, kills, minutes, blocs parcourus, points de dégâts...). |
| `par-membre` | nombre | Quantité ajoutée à l'objectif pour chaque membre de la faction (0 = objectif fixe). |
| `mondes` | liste | Mondes où l'action compte (vide = tous). |
| `zones` | liste | Territoires où l'action compte (vide = tous) : WILDERNESS, SAFEZONE, WARZONE, OWN, ALLY, ENEMY. |
| `poids` | nombre | Chance d'être tirée par rapport aux autres quêtes de la même difficulté. |
| `icone` | texte | Objet affiché dans le menu ({icone}). |
| `description` | liste | Lignes de description ({description}), {objectif} est remplacé. |
| `recompenses` | liste | Récompenses propres à cette quête (vide = celles de la difficulté dans rewards.json). |

`par-membre` n'existe que pour les quêtes de faction. `recompenses` accepte les mêmes actions que
`rewards.json` (section 4) ; vide, la quête donne les récompenses de sa difficulté.

### Types d'objectifs

| Type | Cibles | Ce qui compte |
|---|---|---|
| `CASSER_BLOC` | matériaux | chaque bloc cassé, sauf ceux posés par un joueur |
| `POSER_BLOC` | matériaux | chaque bloc posé |
| `RECOLTER` | matériaux (`CROPS`, `CARROT`, `POTATO`, `NETHER_WARTS`, `COCOA`, `MELON_BLOCK`, `PUMPKIN`, `SUGAR_CANE_BLOCK`, `CACTUS`) | cultures mûres, melons et citrouilles non posés, cannes et cactus au-dessus de leur base |
| `TUER_MOB` | entités (`ZOMBIE`, `ENDERMAN`...) | chaque mob tué, sauf mobs de spawner ou d'œuf (options anti-abus) |
| `TUER_JOUEUR` | relation de la victime : `ENEMY`, `ALLY`, `MEMBER`, `NO_FACTION` | chaque joueur tué (même IP ignorée, même victime espacée de `delai-meme-victime-minutes`) |
| `INFLIGER_DEGATS` | entités (`PLAYER` pour le PvP) | points de dégâts infligés (1 cœur = 2) |
| `PECHER` | matériaux attrapés | chaque prise |
| `CRAFTER` | matériaux fabriqués | nombre d'objets fabriqués |
| `FONDRE` | matériaux sortis du four | nombre d'objets retirés |
| `ENCHANTER` | matériaux enchantés | chaque enchantement |
| `BRASSER` | potions (`POTION:8229`...) | chaque potion préparée |
| `CONSOMMER` | matériaux mangés / bus | chaque consommation |
| `ELEVER` | entités (`COW`, `SHEEP`...) | chaque naissance après avoir nourri les parents |
| `APPRIVOISER` | entités (`WOLF`, `OCELOT`, `HORSE`) | chaque animal apprivoisé |
| `TONDRE` | entités (`SHEEP`, `MUSHROOM_COW`) | chaque tonte |
| `PARCOURIR` | aucune | blocs parcourus (sans voler ; en véhicule selon `parcourir-en-vehicule`) |
| `TEMPS_EN_LIGNE` | aucune | minutes jouées sans être AFK |
| `GAGNER_EXPERIENCE` | aucune | points d'expérience ramassés |
| `MONTER_NIVEAU` | aucune | niveaux gagnés |
| `LIVRER` | matériaux | objets donnés en **cliquant sur la quête** dans le menu (objets renommés refusés) |
| `CLAIMER` | aucune | chunks revendiqués avec KoriaFactions |

Une liste `cibles` vide accepte tout. `mondes` et `zones` s'ajoutent aux cibles : par exemple une quête
`TUER_JOUEUR` avec `"zones": ["WARZONE"]` ne compte que les kills en WarZone.

### `quests_faction.json`


**Quêtes `facile` par défaut**

| Id | Nom | Type | Cibles | Objectif | Par membre | Zones | Poids |
|---|---|---|---|---|---|---|---|
| `carriere` | La carrière | `CASSER_BLOC` | `STONE`, `COBBLESTONE` | 5 000 | 250 | toutes | 10 |
| `bucherons` | Les bûcherons | `CASSER_BLOC` | `LOG`, `LOG_2` | 2 000 | 100 | toutes | 10 |
| `nettoyage` | Grand nettoyage | `TUER_MOB` | `ZOMBIE`, `SKELETON`, `SPIDER`, `CREEPER` | 300 | 20 | toutes | 10 |
| `peche` | Partie de pêche | `PECHER` | tout | 200 | 10 | toutes | 6 |

**Quêtes `moyen` par défaut**

| Id | Nom | Type | Cibles | Objectif | Par membre | Zones | Poids |
|---|---|---|---|---|---|---|---|
| `fer` | L'âge du fer | `CASSER_BLOC` | `IRON_ORE` | 800 | 40 | toutes | 10 |
| `forge` | La forge | `FONDRE` | `IRON_INGOT`, `GOLD_INGOT` | 1 000 | 50 | toutes | 8 |
| `moisson` | La moisson | `RECOLTER` | `CROPS`, `CARROT`, `POTATO`, `NETHER_WARTS` | 1 500 | 75 | toutes | 8 |
| `expansion` | Expansion | `CLAIMER` | tout | 20 | 0 | toutes | 5 |

**Quêtes `difficile` par défaut**

| Id | Nom | Type | Cibles | Objectif | Par membre | Zones | Poids |
|---|---|---|---|---|---|---|---|
| `diamants` | Fièvre du diamant | `CASSER_BLOC` | `DIAMOND_ORE` | 300 | 15 | toutes | 10 |
| `guerre` | En guerre | `TUER_JOUEUR` | `ENEMY` | 100 | 5 | toutes | 10 |
| `tresor` | Le trésor de guerre | `LIVRER` | `DIAMOND_BLOCK` | 64 | 4 | toutes | 6 |

### `quests_player.json`


**Quêtes `facile` par défaut**

| Id | Nom | Type | Cibles | Objectif | Zones | Poids |
|---|---|---|---|---|---|---|
| `mineur` | Mineur débutant | `CASSER_BLOC` | `STONE`, `COBBLESTONE` | 500 | toutes | 10 |
| `bucheron` | Bûcheron | `CASSER_BLOC` | `LOG`, `LOG_2` | 200 | toutes | 10 |
| `chasseur` | Chasseur de nuit | `TUER_MOB` | `ZOMBIE`, `SKELETON` | 50 | toutes | 10 |
| `pecheur` | Pêcheur | `PECHER` | tout | 20 | toutes | 8 |
| `randonneur` | Randonneur | `PARCOURIR` | tout | 5 000 | toutes | 6 |
| `gourmand` | Gourmand | `CONSOMMER` | `COOKED_BEEF`, `GRILLED_PORK`, `COOKED_CHICKEN`, `BREAD` | 30 | toutes | 6 |
| `eleveur` | Éleveur | `ELEVER` | `COW`, `SHEEP`, `PIG`, `CHICKEN` | 10 | toutes | 6 |

**Quêtes `moyen` par défaut**

| Id | Nom | Type | Cibles | Objectif | Zones | Poids |
|---|---|---|---|---|---|---|
| `fer` | Mineur de fer | `CASSER_BLOC` | `IRON_ORE` | 150 | toutes | 10 |
| `or` | Chercheur d'or | `CASSER_BLOC` | `GOLD_ORE` | 64 | toutes | 8 |
| `fermier` | Fermier | `RECOLTER` | `CROPS`, `CARROT`, `POTATO` | 300 | toutes | 8 |
| `enchanteur` | Enchanteur | `ENCHANTER` | tout | 10 | toutes | 6 |
| `berger` | Berger | `TONDRE` | `SHEEP` | 32 | toutes | 5 |
| `fidele` | Fidèle au poste | `TEMPS_EN_LIGNE` | tout | 180 | toutes | 5 |
| `experience` | Sage | `MONTER_NIVEAU` | tout | 30 | toutes | 5 |

**Quêtes `difficile` par défaut**

| Id | Nom | Type | Cibles | Objectif | Zones | Poids |
|---|---|---|---|---|---|---|
| `diamant` | Mineur de diamant | `CASSER_BLOC` | `DIAMOND_ORE` | 48 | toutes | 10 |
| `tueur` | Tueur | `TUER_JOUEUR` | `ENEMY` | 10 | toutes | 10 |
| `gladiateur` | Gladiateur | `TUER_JOUEUR` | tout | 5 | `WARZONE` | 6 |
| `brute` | Brute | `INFLIGER_DEGATS` | `PLAYER` | 500 | toutes | 6 |
| `alchimiste` | Alchimiste | `BRASSER` | tout | 64 | toutes | 6 |
| `chasseur-ender` | Chasseur d'Endermen | `TUER_MOB` | `ENDERMAN` | 64 | toutes | 6 |
| `offrande` | L'offrande | `LIVRER` | `EMERALD_BLOCK` | 16 | toutes | 5 |

## 4. `rewards.json`

### Actions de récompense

| Action | Effet | Qui la reçoit |
|---|---|---|
| `POINTS:n` | points de faction KoriaFactions | la faction (une seule fois), perdus si le joueur n'a pas de faction |
| `ARGENT:n` | argent Vault | chaque destinataire |
| `ITEM:matériau:quantité` ou `ITEM:matériau:data:quantité` | objet dans l'inventaire, au sol s'il est plein | chaque destinataire |
| `CONSOLE:commande` | commande exécutée par la console (`{joueur}` = destinataire) | une fois par destinataire |
| `COMMANDE:commande` | commande exécutée par le joueur | chaque destinataire |
| `MESSAGE:texte` | message privé (`[actionbar]`, `[title]` acceptés) | chaque destinataire |
| `ANNONCE:texte` | message à tout le serveur | une seule fois |
| `SON:nom` | son | chaque destinataire |

Variables : `{joueur}`, `{faction}`, `{quete}`, `{difficulte}`, `{terminees}` (paliers), `{prefix}`.
Un joueur hors ligne reçoit ses récompenses personnelles à sa prochaine connexion.

### Structure

- `faction.par-quete.<difficulté>` : `cible`, `recompenses`, `texte` pour chaque quête de faction terminée.
- `faction.paliers.<n>` : bonus quand la faction a terminé **n** quêtes dans la semaine.
- `joueur.<difficulté>.par-quete` : `recompenses`, `texte` pour chaque quête joueur terminée.
- `joueur.<difficulté>.paliers.<n>` : bonus quand le joueur a terminé **n** quêtes.
- `texte` : lignes affichées dans les menus à la place de la description automatique (vide = automatique :
  « 50 points de faction », « 500$ », « 5x Diamond »).

| Cible (faction) | Destinataires des récompenses personnelles |
|---|---|
| `CONTRIBUTEURS` | membres actuels qui ont fait avancer la quête (pour un palier : au moins une quête de la semaine) |
| `MEMBRES_EN_LIGNE` | membres connectés au moment de la réussite |
| `MEMBRES` | tous les membres, les absents à leur connexion |
| `AUCUNE` | personne : seuls `POINTS` et `ANNONCE` s'appliquent |

Plusieurs paliers sont possibles (`"2"` et `"3"` par exemple) : chacun est donné une fois par période, dès que le
nombre de quêtes terminées l'atteint.

### Valeurs par défaut

**Quêtes de faction**

| Difficulté | Cible | Récompenses par quête |
|---|---|---|
| `facile` | `CONTRIBUTEURS` | `POINTS:50` |
| `moyen` | `CONTRIBUTEURS` | `POINTS:120` |
| `difficile` | `CONTRIBUTEURS` | `POINTS:250`<br>`ARGENT:500` |

| Palier (quêtes terminées) | Cible | Bonus |
|---|---|---|
| `3` | `MEMBRES` | `POINTS:200`<br>`ARGENT:1000`<br>`ANNONCE:{prefix}&6La faction &e{faction} &6a terminé toutes ses quêtes de la semaine !` |

**Quêtes joueur**

| Difficulté | Récompenses par quête | Paliers |
|---|---|---|
| `facile` | `POINTS:10` | **3** : `POINTS:20`, `ARGENT:250` |
| `moyen` | `POINTS:25` | **3** : `POINTS:50`, `ARGENT:750` |
| `difficile` | `POINTS:60` | **3** : `POINTS:120`, `ARGENT:2000`, `ITEM:DIAMOND:5`, `SON:ENDERDRAGON_GROWL` |

## 5. `messages.json`

Les **clés** ne doivent pas être modifiées, seulement les textes. Un texte peut être une ligne, une liste de lignes,
`"[actionbar]texte"`, `"[title]Titre;;Sous-titre"` ou `""` (désactivé). `{prefix}` insère le préfixe, `{label}`
le nom de la commande. Les variables disponibles sont celles déjà utilisées dans le texte par défaut.

| Groupe | Contenu | Clés | Variables utilisées |
|---|---|---|---|
| `prefix` | Préfixe inséré par {prefix} | 1 |  |
| `general` | Erreurs et réponses communes, rechargement, sauvegarde | 13 | `{label}` `{command}` `{usage}` `{player}` `{faction}` `{input}` `{quests}` `{count}` `{problem}` |
| `help` | /quetes aide : en-tête, lignes, et usage + description de chaque commande | 27 | `{label}` `{command}` `{usage}` `{description}` |
| `quest` | Progression, réussite, quêtes non terminées au renouvellement, livraison, erreurs de quête | 16 | `{quete}` `{progression}` `{objectif}` `{pourcentage}` `{recompense}` `{points}` `{faction}` `{quantite}` `{min}` |
| `bonus` | Bonus de palier joueur et faction | 2 | `{difficulte}` `{terminees}` |
| `difficulty` | Choix de la difficulté (confirmation, déjà choisie, inconnue, rappel) | 5 | `{difficulte}` `{difficultes}` `{label}` |
| `cycle` | Annonces de renouvellement (faction, joueur) et temps restants | 3 | `{label}` `{temps_joueur}` `{temps_faction}` |
| `rewards` | Récompenses reçues hors ligne, inventaire plein, description des récompenses dans les menus | 6 | `{quantite}` `{objet}` |
| `admin` | Commandes d'administration (/quetes voir, progression, reinitialiser, renouveler) | 12 | `{player}` `{difficulte}` `{faction}` `{terminees}` `{total}` `{numero}` `{etat}` `{quete}` `{progression}` `{objectif}` |
| `values` | Petits mots réutilisés (aucune...) | 2 |  |
| `time` | Unités de durée (j, h, min, s) | 6 |  |
| `config-editor` | Éditeur /quetes config | 20 | `{path}` `{empty}` `{cancel}` `{file}` `{value}` `{input}` `{key}` `{error}` |

Variables des messages de quête : `{quete}`, `{progression}`, `{objectif}`, `{restant}`, `{pourcentage}`,
`{barre}`, `{difficulte}`, `{points}` (points réellement crédités), `{faction}`, `{joueur}`.

## 6. `menus.json`

Même principe que les menus de KoriaFactions : titre (32 caractères maximum), `lignes` (1 à 6), `fond`
(`"materiau": "AIR"` pour un fond vide), `objets` libres, et pour chaque objet `case` (0 à 53), `materiau`,
`quantite`, `nom`, `description`, `brillant`, `tete` et l'effet des clics (`action` ou `clic`, `clic-gauche`,
`clic-droit`, `shift-clic`, `touche-q`, `ctrl-q`...).

Les menus joueur et faction ont en plus une section `quetes` : `cases` (où placer les quêtes, dans l'ordre),
`en-cours` et `terminee` (apparence d'une quête selon son état ; `{icone}` reprend l'icône de la quête), et
`textes` (petits textes insérés dans les variables).

| Action | Effet |
|---|---|
| `MENU:joueur`, `MENU:difficulte`, `MENU:faction`, `MENU:config`, `MENU:config:<fichier>` | ouvre un menu |
| `CHOISIR:<difficulté>` | choisit la difficulté (second clic si `confirmation-choix`) |
| `LIVRER` | sur une quête `LIVRER` : donne les objets de l'inventaire |
| `COMMANDE:`, `CONSOLE:`, `MESSAGE:`, `FERMER`, `ACTUALISER` | comme dans KoriaFactions |

| Où | Variables |
|---|---|
| Tous les menus | `{player}`, `{faction}`, `{label}`, `{temps_joueur}`, `{temps_faction}`, `{temps_restant}` (quêtes joueur dans les menus joueur et difficulté, quêtes de faction dans le menu faction) |
| Une quête | `{quete}`, `{description}`, `{progression}`, `{objectif}`, `{restant}`, `{pourcentage}`, `{barre}`, `{difficulte}`, `{icone}`, `{points}`, `{recompenses}`, `{livrer}` |
| Une quête de faction | `{contribution}` (celle du joueur), `{contributeurs}` (classement, format `textes.contributeur`) |
| Menus joueur et faction | `{difficulte}`, `{terminees}`, `{total}`, `{restantes}`, `{bonus}`, `{bonus_etat}`, `{points}` (faction) |
| Menu difficulté | `{nombre}`, et pour chaque difficulté `{recompenses_<id>}`, `{bonus_<id>}`, `{nom_<id>}` |

### Menus par défaut

### Menu `joueur`

| Clé | Défaut |
|---|---|
| `titre` | `"&8Mes quêtes &7» {difficulte}"` |
| `lignes` | `4` |

**`fond`** : `materiau`, `nom`

**`quetes`** : `cases`, `en-cours`, `terminee`

**`textes`** : `livrer`, `bonus-obtenu`, `bonus-a-obtenir`, `sans-difficulte`

**`objets`** (objets libres) :

| Objet | Case | Matériau | Nom | Clic |
|---|---|---|---|---|
| `infos` | `4` | `"BOOK"` | `"&6Quêtes du jour"` | — |
| `bonus` | `31` | `"NETHER_STAR"` | `"&6Bonus du jour"` | — |
| `faction` | `27` | `"BANNER:4"` | `"&bQuêtes de faction"` | `"MENU:faction"` |
| `fermer` | `35` | `"BARRIER"` | `"&cFermer"` | `"FERMER"` |

Variables utilisées par défaut : `{difficulte}` `{icone}` `{quete}` `{description}` `{progression}` `{objectif}` `{barre}` `{pourcentage}` `{recompenses}` `{livrer}` `{restantes}` `{terminees}` `{total}` `{temps_restant}` `{bonus}` `{bonus_etat}`

### Menu `difficulte`

| Clé | Défaut |
|---|---|
| `titre` | `"&8Choisis ta difficulté"` |
| `lignes` | `3` |

**`fond`** : `materiau`, `nom`

**`objets`** (objets libres) :

| Objet | Case | Matériau | Nom | Clic |
|---|---|---|---|---|
| `facile` | `11` | `"WOOL:5"` | `"&a&lFacile"` | `"CHOISIR:facile"` |
| `moyen` | `13` | `"WOOL:1"` | `"&6&lMoyen"` | `"CHOISIR:moyen"` |
| `difficile` | `15` | `"WOOL:14"` | `"&c&lDifficile"` | `"CHOISIR:difficile"` |
| `infos` | `4` | `"PAPER"` | `"&eAttention"` | — |
| `faction` | `18` | `"BANNER:4"` | `"&bQuêtes de faction"` | `"MENU:faction"` |
| `fermer` | `26` | `"BARRIER"` | `"&cFermer"` | `"FERMER"` |

Variables utilisées par défaut : `{nombre}` `{recompenses_facile}` `{bonus_facile}` `{recompenses_moyen}` `{bonus_moyen}` `{recompenses_difficile}` `{bonus_difficile}` `{temps_restant}`

### Menu `faction`

| Clé | Défaut |
|---|---|
| `titre` | `"&8Quêtes de {faction}"` |
| `lignes` | `4` |

**`fond`** : `materiau`, `nom`

**`quetes`** : `cases`, `en-cours`, `terminee`

**`textes`** : `livrer`, `contributeur`, `aucun-contributeur`, `bonus-obtenu`, `bonus-a-obtenir`

**`objets`** (objets libres) :

| Objet | Case | Matériau | Nom | Clic |
|---|---|---|---|---|
| `infos` | `4` | `"BANNER:4"` | `"&b{faction}"` | — |
| `bonus` | `31` | `"NETHER_STAR"` | `"&6Bonus de faction"` | — |
| `retour` | `27` | `"ARROW"` | `"&7Mes quêtes"` | `"MENU:joueur"` |
| `fermer` | `35` | `"BARRIER"` | `"&cFermer"` | `"FERMER"` |

Variables utilisées par défaut : `{faction}` `{icone}` `{quete}` `{difficulte}` `{description}` `{progression}` `{objectif}` `{barre}` `{pourcentage}` `{contribution}` `{contributeurs}` `{recompenses}` `{livrer}` `{rang}` `{joueur}` `{quantite}` `{restantes}` `{terminees}` `{total}` `{points}` `{temps_restant}` `{bonus}` `{bonus_etat}`


Le menu `editeur` (apparence de `/quetes config`) a la même structure que celui de KoriaFactions.

## 7. Tâches courantes

| Besoin | Réglage |
|---|---|
| Renouveler les quêtes de faction le dimanche à 20h | `cycle.jour` → `DIMANCHE`, `cycle.heure` → `20:00` (démarre une nouvelle semaine) |
| Renouveler les quêtes joueur à 6h du matin | `cycle-joueur.heure` → `06:00` |
| Quêtes joueur hebdomadaires | `cycle-joueur.periode` → `HEBDOMADAIRE` (et adapter les textes « du jour » des menus et messages) |
| Ajouter une quête | copier une quête (bouton émeraude dans `/quetes config` ou dans le fichier), changer son id, son `type`, ses `cibles` et son `objectif`, puis `/quetes recharger` ; elle entre dans le prochain tirage |
| Retirer une quête du tirage | `actif` → `false` |
| Rendre une quête plus fréquente | augmenter son `poids` |
| Quête valable seulement en minage | `"mondes": ["minage"]` |
| Kills seulement en WarZone | `"type": "TUER_JOUEUR"`, `"zones": ["WARZONE"]` |
| Objectif de faction selon la taille | `par-membre` (ex. 250 : +250 par membre) |
| 2 quêtes de faction faciles par semaine | `quetes-faction.nombre.facile` → `2` |
| Bonus dès 2 quêtes sur 3 | ajouter un palier `"2"` dans `rewards.json` |
| Récompense spéciale pour une quête | remplir sa liste `recompenses` |
| Donner un grade ou une clé de caisse | `CONSOLE:lp user {joueur} parent add ...` ou `CONSOLE:crate give {joueur} ...` |
| Autoriser les joueurs sans faction | `quetes-joueur.sans-faction` → `AUTORISER` (leurs points sont perdus) |
| Laisser changer de difficulté | `quetes-joueur.changer-difficulte` → `true` (la progression repart de zéro) |
| Désactiver les quêtes de faction | `quetes-faction.actif` → `false` |
| Ajouter une difficulté `extreme` | `difficultes.extreme`, sections `extreme` dans les deux fichiers de quêtes, `quetes-faction.nombre.extreme`, `rewards.json` (`faction.par-quete.extreme`, `joueur.extreme`), un objet `CHOISIR:extreme` dans le menu `difficulte` |
| Donner de nouvelles quêtes tout de suite | `/quetes renouveler` (tout), `/quetes renouveler faction` ou `/quetes renouveler joueur` |
| Changer la commande de `/f quests` | KoriaFactions, `config.json` → `commandes.quests.commande` |
| Corriger la progression d'un joueur | `/quetes progression joueur <pseudo> <n° quête> <valeur>` |
