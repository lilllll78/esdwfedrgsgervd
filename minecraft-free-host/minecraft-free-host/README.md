# FreeHost – World Hosting (Fabric 1.21.1)

Héberge ton monde solo depuis ton PC et invite tes amis avec un code. Pas de serveur à louer, pas de relais :
le monde reste sur ton ordinateur et tourne tant que tu es connecté.

## Comment ça marche
1. **Host World** : tu choisis un monde, le mod le lance et l'ouvre au réseau (port local aléatoire).
2. Le mod ouvre un port sur ta box (UPnP) et démarre une « porte d'entrée » qui vérifie le code.
3. Il génère un **code d'invitation** (23 caractères) : ton IP + port + un secret aléatoire.
4. **Join World** : l'ami colle le code, le mod vérifie auprès de ton PC, puis connecte Minecraft.
5. **Stop Hosting** (ou fermer le monde / le jeu) : port fermé, code invalide, invités déconnectés.

## Compiler (sans rien installer) avec GitHub
1. Crée un compte sur github.com, puis un nouveau dépôt (bouton « New »).
2. Dans le dépôt : « Add file » > « Upload files », glisse **tout le contenu** du dossier (y compris le dossier `.github`).
3. Onglet **Actions** : le build démarre tout seul (2-3 min). Si rien ne démarre : « Build mod » > « Run workflow ».
4. Quand c'est vert, clique sur le build > en bas « Artifacts » > télécharge `freehost-mod`.
5. Dézippe : tu obtiens `freehost-0.1.0.jar`.

## Installer
- Minecraft Java **1.21.1** avec **Fabric Loader** (0.16+) et **Fabric API** 1.21.1.
- Mets `freehost-0.1.0.jar` dans `.minecraft/mods`.
- Tous les joueurs (hôte et amis) doivent avoir le mod.

## Utiliser
Bouton **World Hosting** en haut à gauche du menu principal (et du menu pause).

## Limites
- Il faut que le port soit joignable depuis Internet : box avec UPnP activé, ou redirection manuelle du port TCP 25566 (modifiable dans Settings).
- Ne marche pas en CGNAT (certaines connexions 4G/5G / IP partagée). Le mod l'indique.
- IPv4 uniquement pour l'instant.
- L'IP publique est parfois obtenue via api.ipify.org si la box ne la donne pas.
