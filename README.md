# KoriaQuest

Quêtes hebdomadaires de faction et de joueur pour **KoriaMC** : Spigot / PaperSpigot **1.8.8**, Java 8.
Nécessite **KoriaFactions** (faction des joueurs, territoire, points de faction). Optionnels : **Vault** (récompenses
`ARGENT`) et **PlaceholderAPI**.

## Compiler

KoriaFactions doit être compilé avant (le jar `../KoriaFactions/target/KoriaFactions-1.0.0.jar` sert de dépendance) :

```
mvn clean package
```

## Fonctionnement

- **Quêtes de faction** : chaque semaine, `quetes-faction.nombre` quêtes sont tirées par difficulté, **les mêmes pour
  toutes les factions**. Tous les membres font avancer la même progression. L'objectif peut grandir avec la taille de
  la faction (`par-membre`). Un joueur qui change de faction laisse ses contributions à l'ancienne. Accès aussi par
  `/f quests` (KoriaFactions).
- **Quêtes joueur** : **chaque jour**, le joueur choisit une difficulté (`/quetes`) et reçoit `quetes-joueur.nombre`
  quêtes tirées dans cette difficulté. Les points de faction gagnés vont à sa faction du moment.
- **Récompenses** : par quête terminée (selon la difficulté) et par palier (nombre de quêtes terminées dans la
  période), dans `rewards.json`. `POINTS:n` crédite les points de faction de KoriaFactions (`/f top points`).
- **Renouvellements** : quêtes de faction chaque semaine (jour, heure et fuseau de `cycle`), quêtes joueur chaque jour à
  l'heure de `cycle-joueur` (ou chaque semaine avec `periode` = `HEBDOMADAIRE`). Les quêtes non terminées sont perdues.

## Configuration

| Fichier | Contenu |
|---|---|
| `config.json` | stockage (JSON, SQLite, MySQL / MariaDB), commande `/quetes` et sous-commandes, cycle, difficultés, quêtes de faction et joueur, anti-abus, notifications, affichage |
| `quests_faction.json` | quêtes de faction par difficulté |
| `quests_player.json` | quêtes joueur par difficulté |
| `rewards.json` | récompenses par quête et par palier |
| `messages.json` | tous les textes (`[actionbar]`, `[title]titre;;sous-titre`, `""` = désactivé) |
| `menus.json` | menus joueur, difficulté, faction et éditeur : titres, tailles, cases, objets, effet de chaque clic |

`/quetes config` ouvre l'éditeur en jeu (même fonctionnement que `/f config`). L'explication de chaque option est dans
`LISEZMOI.txt`, régénéré à chaque démarrage. `/quetes recharger` recharge tout sauf le stockage et le nom de la commande.

### Types d'objectifs

| Type | Cibles |
|---|---|
| `CASSER_BLOC`, `POSER_BLOC` | matériaux (les blocs posés par un joueur ne comptent pas au cassage) |
| `RECOLTER` | cultures mûres, melons, citrouilles, cannes, cactus |
| `TUER_MOB`, `INFLIGER_DEGATS` | types d'entités (`ZOMBIE`, `PLAYER`...), mobs de spawner ignorés |
| `TUER_JOUEUR` | relation de la victime : `ENEMY`, `ALLY`, `NO_FACTION` (délai sur la même victime, même IP ignorée) |
| `PECHER`, `CRAFTER`, `FONDRE`, `ENCHANTER`, `BRASSER`, `CONSOMMER` | matériaux obtenus ou utilisés |
| `ELEVER`, `APPRIVOISER`, `TONDRE` | types d'entités |
| `PARCOURIR` | distance en blocs (hors vol) |
| `TEMPS_EN_LIGNE` | minutes jouées hors AFK |
| `GAGNER_EXPERIENCE`, `MONTER_NIVEAU` | points ou niveaux d'expérience |
| `LIVRER` | objets livrés en cliquant sur la quête dans le menu |
| `CLAIMER` | chunks revendiqués avec KoriaFactions |

Chaque quête accepte aussi `mondes` et `zones` (`WILDERNESS`, `SAFEZONE`, `WARZONE`, `OWN`, `ALLY`, `ENEMY`).

## Commandes

| Commande | Rôle |
|---|---|
| `/quetes` | quêtes du joueur (choix de la difficulté s'il n'est pas fait) |
| `/quetes faction` | quêtes de la faction |
| `/quetes difficulte [difficulté]` | choix de la difficulté |
| `/quetes temps` | temps avant les nouvelles quêtes joueur et de faction |
| `/quetes voir <joueur>` | quêtes d'un joueur et de sa faction |
| `/quetes progression <joueur\|faction> <nom> <n°> <valeur>` | fixe la progression d'une quête |
| `/quetes reinitialiser <joueur\|faction> <nom>` | remet à zéro les quêtes en cours d'un joueur ou d'une faction |
| `/quetes renouveler [tout\|faction\|joueur]` | nouvelles quêtes immédiatement pour tout le monde |
| `/quetes config`, `/quetes sauvegarder`, `/quetes recharger` | administration |

Noms, alias et permissions se règlent dans `config.json` (`commandes`).

## PlaceholderAPI

`%koriaquest_difficulte%`, `%koriaquest_terminees%`, `%koriaquest_total%`, `%koriaquest_temps_joueur%`, `%koriaquest_temps_faction%`,
`%koriaquest_faction_terminees%`, `%koriaquest_faction_total%`,
`%koriaquest_quete_<n>_<nom|progression|objectif|pourcentage|termine>%`,
`%koriaquest_faction_quete_<n>_<nom|progression|objectif|pourcentage|termine>%`.

## API

`KoriaQuestAPI.registerObjectiveType(id, cibles)` déclare un nouveau type d'objectif utilisable dans les fichiers de
quêtes, `KoriaQuestAPI.progress(ProgressContext.of(joueur, id, quantité))` le fait avancer. Évènements :
`QuestProgressEvent` (annulable, quantité modifiable), `QuestCompleteEvent`, `BonusEarnedEvent`, `WeeklyResetEvent`, `PlayerQuestsResetEvent`.
