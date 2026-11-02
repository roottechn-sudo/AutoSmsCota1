# Spécifications Techniques Complètes : Application Native Android de Gestion d'Association ("RAVA")

## 1. Contexte du Projet & Objectifs
Développer une application mobile native en **Kotlin** avec **Jetpack Compose** pour la gestion financière, comptable et administrative d'une association. 
L'application fonctionne selon le modèle **Local-First (Room Database)** pour rester opérationnelle en zone à connectivité limitée (réseaux dégradés 2.75G/3G/4G au Burkina Faso), avec une synchronisation asynchrone bidirectionnelle vers **Firebase (Auth et Firestore)** gérée en tâche de fond par **WorkManager**.

---

## 2. Architecture de Sécurité Multi-Couches : Gestion des Droits d'Accès (App + Firebase)

La sécurité et l'isolation des données doivent être validées à deux niveaux indépendants et complémentaires.

### A. Gestion Côté Application (Validation UI/UX locale)
L'application doit embarquer un mécanisme de sécurité interne (par exemple, via un `DroitsService` ou une gestion d'état dans le ViewModel). L'interface utilisateur s'adapte dynamiquement en fonction du rôle mis en cache dans la session utilisateur :
- **Masquage / Affichage conditionnel :** Les boutons d'action d'écriture (ex: "Créer une campagne", "Enregistrer une cotisation") ne doivent pas être visibles ou cliquables si l'utilisateur n'a pas le rôle requis.
- **Écran de redirection / Blocage :** Si un utilisateur tente d'accéder à un écran ou à une fonctionnalité via une navigation forcée sans avoir le rôle adéquat, l'application doit immédiatement afficher un écran de redirection affichant un message explicite : *"Votre compte est en attente d'activation par un administrateur"* ou *"Droits insuffisants pour accéder à cette section"*.

### B. Gestion Côté Firebase (Validation Cloud stricte)
Toutes les requêtes de lecture et d'écriture envoyées à Firebase Firestore doivent être validées par des **Règles de Sécurité (Firestore Rules)** qui vérifient le rôle réel stocké dans le document utilisateur sur le serveur. Même si le code de l'application est altéré ou contourné localement, le serveur Firebase doit rejeter (`Permission Denied`) toute opération non autorisée.

### Matrice des Rôles & Permissions Métiers :
- **`UserRole.MEMBRE` :** Lecture seule (*Read-Only*) sur son profil, ses cotisations et les campagnes d'événements actives. Redirection immédiate si tentative d'écriture.
- **`UserRole.SECRETAIRE` :** Contrôle total sur la gestion des Campagnes d'Événements (Création, modification, clôture). Consultation de la liste des membres. Écrans financiers bloqués.
- **`UserRole.TRESORIER` :** Contrôle total sur les Cotisations et mouvements financiers associés aux campagnes ouvertes (Saisie espèces, validation/importation des SMS Orange Money, reçus). Écrans de création de campagnes bloqués.
- **`UserRole.ADMINISTRATEUR` :** Contrôle total de la plateforme. Seul rôle capable de modifier la collection des utilisateurs pour attribuer ou changer les rôles.

---

## 3. Architecture des Données & Persistance Locale (Room)

Générer les entités Room et le DAO associés avec les contraintes suivantes :
- **`UserRole` (Enum) :** `MEMBRE`, `SECRETAIRE`, `TRESORIER`, `ADMINISTRATEUR`.
- **`MemberEntity` (Table: `members`) :** `uid` (String, PrimaryKey), `nom`, `prenom`, `telephone`, `role`, `carteValide` (Boolean), `soldeArrieres` (Double).
- **`EventCampaignEntity` (Table: `event_campaigns`) :** `idCampagne` (String, PrimaryKey), `titre`, `description`, `dateCreation` (Long), `dateCloture` (Long?), `estCloture` (Boolean), `budgetEstime` (Double), `budgetReel` (Double), `creeParUid` (String).
- **`CotisationEntity` (Table: `cotisations`) :** `idTransaction` (String, PrimaryKey), `idCampagne` (String, ForeignKey), `membreUid` (String), `montant` (Double), `datePaiement` (Long), `methodePaiement` (String : ORANGE_MONEY ou ESPECE), `enregistreParUid` (String), `recuGenere` (Boolean).

### Idempotence et Anti-Doublons (SMS & Scan) :
- La clé primaire de `CotisationEntity` doit être l'identifiant unique de la transaction Orange Money (ex: `MP261031.1009.97358916`).
- Le DAO Room doit utiliser `@Insert(onConflict = OnConflictStrategy.IGNORE)` pour ignorer silencieusement toute transaction déjà enregistrée lors des scans d'historique ou d'interceptions de SMS en temps réel.

---

## 4. Extraction de Données Automatique (SMS Orange Money)

Créer une classe utilitaire de parsing (`OrangeMoneyParser`) utilisant des expressions régulières (`RegEx`) pour analyser les SMS officiels d'Orange Money Burkina Faso.
Le parseur doit extraire : le **Montant**, l'**Identifiant de Transaction (ID Trans / Trans id)**, et l'**Expéditeur/Bénéficiaire**.

### Modèles de SMS à traiter :
1. *Dépôt/Transfert reçu (Recette) :* `"Cher client, vous avez recu 10000.0 FCFA, Frais: FCFA, Taxe: FCFA de 3177363 KIENON MULTI SERVICES. Votre solde est de 15106.698 FCFA. ID Trans: CI261031.1040.8675069. Merci d'utiliser Orange Money."`
2. *Transfert émis (Dépense) :* `"Cher client, vous avez transfere 1,780.00 FCFA, Frais: 0.0 FCFA, Taxe: FCFA au numero 54766084,Nonganaba Alexandre. Votre solde est de 5611.698 FCFA. ID Trans: PP261031.1017.2901274..."`
3. *Paiement effectué (Dépense) :* `"Votre paiement de 500.00 FCFA, Frais: 4.3478 FCFA, Taxe: 0.6522 FCFA a Bebe jean-Baptiste Bougrouma a ete effectue avec succes. Votre solde est de : 5106.698 FCFA. Trans id: MP261031.1009.97358916."`

---

## 5. Synchronisation Firebase Sécurisée

### A. Code Kotlin (Repository & Worker) :
- Créer un `AuthRepository` gérant l'inscription et la connexion via Firebase Auth, avec persistance immédiate du profil utilisateur dans Firestore. Le système doit intégrer un contrôle de sécurité lors de la connexion pour charger le rôle utilisateur en cache local sécurisé avant d'ouvrir l'écran d'accueil.
- Créer un `FirebaseSyncWorker` héritant de `CoroutineWorker`. Lors de la synchronisation vers Firestore, forcer l'utilisation de l'identifiant de la transaction comme ID du document : `.collection("cotisations").document(cotisation.idTransaction).set(cotisation)`.

### B. Règles de Sécurité Firestore (`firestore.rules`) :
Générer le fichier de configuration des règles de sécurité Firestore interdisant à un utilisateur de modifier son propre rôle ou d'écrire dans une collection pour laquelle il n'a pas la permission métier requise.

---

## 6. Interface Graphique Liquide (Jetpack Compose)

Générer les écrans d'interface utilisateur en respectant scrupuleusement la contrainte **Zero Layout Overflow** :
- Utiliser exclusivement des structures fluides, des arrangements proportionnels (`Modifier.weight()`) et des pourcentages de dimensions (`fillMaxWidth(0.9f)`) à la place des tailles fixes en `dp`.
- **Écran d'Authentification :** Saisie propre et sécurisée (Email, Mot de passe).
- **Écran Tableau de Bord (Dashboard) :** Affichage dynamique adaptatif appliquant la double vérification des droits côté app (Bouton "Créer Campagne" visible uniquement pour Secrétaire/Admin, Bouton "Saisir Cotisation/Scanner SMS" visible uniquement pour Trésorier/Admin, Mode consultation avec bandeau informatif pour le Membre Simple).
- **Écran de Redirection de Sécurité :** Interface d'attente ou de blocage si un utilisateur tente de forcer une action sans posséder les privilèges adéquats.
- **Écran de Scan Historique :** Intégration d'un `DateRangePicker` Material 3 permettant de définir une plage de dates pour filtrer et lancer l'importation de l'historique des SMS via le `ContentResolver` d'Android.

---

## Livrables attendus
Fournir un code Kotlin complet, modulaire, respectant les bonnes pratiques de l'architecture MVVM, entièrement rédigé et documenté avec des commentaires clairs en français. Ne faire aucune omission de code ou de commentaires de type `// TODO`.


le code doit être livré en fichier compressé compressé 