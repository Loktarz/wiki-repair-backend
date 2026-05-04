# Wiki Repair — Guide de test des nouvelles fonctionnalités

Ce guide te permet de tester pas à pas les 5 grands manques qui ont été implémentés
pour aligner l'application avec **WorkFlow-Final-Wiki-Repair.pdf** et **pROCEDURE.pdf**.

Tout est en français, dans l'ordre où tu les rencontres dans la vie réelle d'un ticket.

---

## 0. Prérequis — démarrer l'environnement

### Backend
```bash
cd "C:\Users\yassi\OneDrive\Bureau\repair-ticket-system - Copy"
./mvnw spring-boot:run
```
- Le backend doit être en JDK 17+ (Spring Boot 3.4.x).
- Vérifie qu'il démarre sur `http://localhost:8080`.
- Vérifie qu'il affiche dans la console : `Started RepairTicketSystemApplication in X seconds`.
- Vérifie qu'un dossier `uploads/signed-bons/` est créé automatiquement à la racine du projet (par `FileStorageService`).

### Backoffice (interface staff)
```bash
cd "C:\Users\yassi\OneDrive\Bureau\wiki-repair-backoffice"
npm install   # une seule fois
npm run dev
```
- Doit démarrer sur `http://localhost:5173`.

### Frontend client (optionnel pour ces tests)
```bash
cd "C:\Users\yassi\OneDrive\Bureau\wiki-repair-frontend-main"
npm run dev
```

### Configuration email (à faire une seule fois)
Dans `application.properties`, vérifie que ces lignes sont remplies :
```
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=ton.email@gmail.com
spring.mail.password=ton-mot-de-passe-app
```
> Pour Gmail il faut un **mot de passe d'application** (pas ton mot de passe normal).
> Tant que ce n'est pas configuré, les notifications par email ne partiront pas mais
> tout le reste du workflow fonctionnera.

### Comptes de test
Tu as besoin d'au moins un compte par rôle. Si tu n'en as pas, crée-les via l'admin :
- 1 ADMIN
- 1 AGENT_MAGASIN
- 1 TECHNICIAN
- 1 INFOLINE
- 1 client (avec email valide pour recevoir les notifs)

---

## 1. Bon de Réception (PDF officiel + scan signé)

> **But** : remplacer le `window.print()` HTML par un vrai PDF officiel avec numéro
> unique, et permettre le re-upload du bon signé par le client.

### Test 1A — Générer le PDF
1. Connecte-toi en **AGENT_MAGASIN** ou **ADMIN**.
2. Crée un nouveau ticket (`+ Nouveau Ticket`) avec un client de test.
3. Ouvre le ticket → tu vois la carte **« Bon de Réception »**.
4. Clique sur **« 📄 Générer le Bon (PDF) »**.
5. ✅ Un PDF se télécharge, nommé `BonReception_WR-2026-XXXXXX.pdf`.
6. ✅ La carte affiche maintenant un badge vert **« N° XXXXXX »** (6 chiffres uniques).
7. Re-clique sur le bouton → le bouton dit maintenant **« Re-télécharger »**, le N° ne change plus.

**À vérifier dans le PDF :**
- En-tête vert « WIKI Repair » + date + numéro de ticket
- Bandeau « BON DE RÉCEPTION N° XXXXXX » centré
- 5 sections : Informations client, Informations machine, Description de la panne, Accessoires & état, Observations
- Conditions générales (paragraphe légal)
- Cases « Signature du client » / « Signature de l'agent »
- Footer avec coordonnées Wiki Repair

### Test 1B — Téléverser le bon signé
1. Imprime le PDF, signe-le (ou utilise un PDF quelconque pour le test).
2. Sur le ticket, dans la carte « Bon signé », clique **« 📤 Téléverser le bon signé (PDF) »**.
3. Sélectionne le PDF.
4. ✅ La carte affiche maintenant **« ✓ Téléversé »** + date.
5. ✅ Le statut du ticket passe automatiquement à **FICHE_REPARATION_IMPRIMEE**.
6. ✅ Le client reçoit un email de notification (si SMTP configuré).
7. Clique **« 👁 Voir »** → le PDF signé s'ouvre dans un nouvel onglet.
8. Clique **« 🔄 Remplacer »** → tu peux téléverser une autre version.

### Test 1C — Sécurité
- Connecte-toi en **TECHNICIAN** : tu ne dois **pas** voir le bouton « Téléverser », mais tu peux toujours « Voir » le bon signé.
- En **INFOLINE** : tu peux générer le PDF mais pas téléverser le signé.
- Si tu uploades un fichier non-PDF : message d'erreur `Seuls les fichiers PDF sont acceptés`.

---

## 2. Tentative de réparation — routage selon résultat

> **But** : quand un technicien fait une « tentative » de réparation partielle, il doit
> indiquer si elle a réussi (→ `REPARATION_TERMINEE`) ou échoué (→ `REPARATION_IMPOSSIBLE`).

### Test 2
1. Connecte-toi en **TECHNICIAN** (ou ADMIN).
2. Ouvre un ticket et passe son statut à **TENTATIVE_REPARATION** (via le sélecteur de statut).
3. ✅ Une carte ambre **« ⚠️ Tentative de réparation en cours »** apparaît.
4. Tape une note dans le textarea (ex: « Remplacement carte mère testé, machine ne s'allume toujours pas »).
5. Clique sur **« ✗ Tentative échouée → REPARATION_IMPOSSIBLE »**.
6. Confirme dans la popup.
7. ✅ Le statut passe à **REPARATION_IMPOSSIBLE**.
8. ✅ La note avec le préfixe `[Tentative échouée]` est ajoutée aux `diagnosticNotes`.
9. ✅ Une entrée apparaît dans l'historique des statuts.
10. ✅ Le client reçoit un email.

Recommence avec un autre ticket et clique cette fois **« ✓ Tentative réussie »** → statut `REPARATION_TERMINEE`.

### Test 2 — Sécurité
- Mets un ticket sur n'importe quel autre statut → la carte ambre **n'apparaît pas**.
- Si tu appelles directement l'API `POST /api/tickets/{id}/tentative-outcome` sur un ticket qui n'est pas en `TENTATIVE_REPARATION`, tu reçois `400 / "Le ticket n'est pas en TENTATIVE_REPARATION"`.

---

## 3. Auto-relance pour retrait (PRET_RETRAIT)

> **But** : quand un ticket reste en `PRET_RETRAIT` ≥ 3 jours, le client reçoit
> automatiquement un email de relance, puis une autre relance tous les 3 jours.

### Test 3 — Test rapide en mode "manuel"
Comme le scheduler tourne tous les jours à 9h, voici comment tester immédiatement :

**Option A — Forcer en base de données** (le plus rapide)
1. Passe un ticket à **PRET_RETRAIT** via l'interface.
2. Connecte-toi à PostgreSQL :
   ```sql
   UPDATE tickets
   SET pret_retrait_at = NOW() - INTERVAL '4 days'
   WHERE id = <ID_DU_TICKET>;
   ```
3. Redémarre le backend (ou attends 9h du matin).
4. Pour forcer immédiatement, modifie temporairement le cron dans
   `PickupReminderScheduler.java` :
   ```java
   @Scheduled(cron = "0 0 9 * * *")    // ← garde en prod
   // @Scheduled(fixedRate = 60000)     // ← active pour tester (toutes les 60 sec)
   ```
5. ✅ Le client reçoit un email **« Rappel : votre appareil est prêt »**.
6. ✅ La colonne `last_pickup_relance_at` est mise à jour en base.
7. Si tu déclenches une 2e fois immédiatement → **rien ne se passe** (anti-spam : 3 jours mini entre relances).

**Option B — Attendre 3 jours réels** (test naturel)
1. Passe un ticket à PRET_RETRAIT aujourd'hui.
2. Reviens dans 3 jours après 9h → email envoyé automatiquement.

### Test 3 — Vérification logs
Dans la console du backend tu dois voir au moment du tick :
```
INFO PickupReminderScheduler — Pickup reminder scheduler running
INFO PickupReminderScheduler — Found 1 tickets eligible for pickup relance
INFO PickupReminderScheduler — Sent pickup relance for ticket WR-2026-XXXXXX
```

---

## 4. Archives — séparation des tickets actifs / livrés

> **But** : un ticket livré (`LIVRE_CLIENT`) doit disparaître de la liste principale et
> apparaître dans un onglet « Archives ».

### Test 4
1. Va sur la page **Tickets**.
2. ✅ Tu vois deux onglets en haut : **« Tickets actifs »** (vert, sélectionné) et **« 📦 Archives »** (gris).
3. Note un ticket dont le statut est `EN_REPARATION` (par ex.) → il est dans **Tickets actifs**.
4. Passe ce ticket à **LIVRE_CLIENT**.
5. Reviens sur la liste : ✅ le ticket **n'est plus** dans « Tickets actifs ».
6. Clique sur l'onglet **« 📦 Archives »** → ✅ le ticket apparaît ici.
7. Le filtre/recherche/pagination fonctionne dans les deux onglets.

### Test 4 — Vérification base
```sql
SELECT ticket_number, status, archived FROM tickets ORDER BY created_at DESC;
```
Tous les tickets `LIVRE_CLIENT` doivent avoir `archived = true`.
Tous les autres `archived = false`.

---

## 5. Facture (avec tarification différenciée)

> **But** : produire une facture PDF officielle avec le bon montant selon le scénario :
> - `LIVRE_CLIENT` → montant total du devis (pièces + main d'œuvre + 19% TVA)
> - `DEVIS_REFUSE` → 20 DT HT de diagnostic + 19% TVA
> - `REPARATION_IMPOSSIBLE` → 20 DT HT de diagnostic + 19% TVA

### Test 5A — Facture après livraison (montant complet)
1. Ouvre un ticket qui a un devis créé (lignes + main d'œuvre).
2. Fais-le passer par tout le workflow jusqu'à **LIVRE_CLIENT**.
3. Sur la page du ticket, ✅ une carte verte **« Facture »** apparaît avec le sous-titre
   « Facture finale (devis complet + TVA 19%) ».
4. Clique sur **« 💰 Télécharger la facture »**.
5. ✅ Un PDF `Facture_WR-2026-XXXXXX.pdf` se télécharge.

**À vérifier dans le PDF :**
- En-tête WIKI Repair + ticket + date
- Titre vert « FACTURE » + sous-titre « Motif : Réparation effectuée et livrée »
- Section « Informations Client »
- Section « Détail » avec **toutes** les lignes du devis (description, qté, P.U., total HT)
- Une ligne supplémentaire « Main d'œuvre »
- Bloc totaux à droite : Total pièces HT, Main d'œuvre HT, TVA (19%), **TOTAL TTC**
- Footer avec coordonnées

### Test 5B — Facture devis refusé (diagnostic seul)
1. Ouvre un ticket en statut `DEVIS_ENVOYE_CLIENT`.
2. Connecte-toi en INFOLINE et passe-le à **DEVIS_REFUSE**.
3. ✅ La carte « Facture » apparaît avec le sous-titre **« Facture diagnostic uniquement (20 DT HT + TVA 19%) »**.
4. Télécharge → ✅ le PDF montre :
   - Une seule ligne : `Frais de diagnostic — Devis refusé — diagnostic uniquement` à 20.00
   - Total pièces HT : 0.00
   - Main d'œuvre HT : 20.00
   - TVA : 3.80
   - **TOTAL TTC : 23.80 TND**

### Test 5C — Facture réparation impossible (diagnostic seul)
1. Refais le test 2, scénario « Tentative échouée » (qui passe à `REPARATION_IMPOSSIBLE`).
2. ✅ Même carte « Facture » apparaît, même montant 23.80 TND.
3. Le motif est `Réparation impossible — diagnostic uniquement`.

### Test 5D — Facture sur statut non facturable
- Ouvre un ticket en `EN_DIAGNOSTIC` ou `DEVIS_ACCEPTE` etc.
- ✅ La carte « Facture » **n'apparaît pas**.
- Si tu appelles directement `GET /api/tickets/{id}/facture` → réponse `500 / "Ticket non facturable au statut actuel : EN_DIAGNOSTIC"`.

---

## 6. Notifications email — vérification globale

Pour chaque transition de statut, le client doit recevoir un email (si SMTP configuré).
Vérifie ces 4 transitions clés :

| Action | Email attendu |
|---|---|
| Statut → `FICHE_REPARATION_IMPRIMEE` (auto via upload signé) | « Votre fiche de réparation est prête » |
| Statut → `DEVIS_ENVOYE_CLIENT` (par INFOLINE) | « Votre devis est disponible » |
| Statut → `PRET_RETRAIT` | « Votre appareil est prêt à retirer » |
| Statut → `LIVRE_CLIENT` | « Merci, à bientôt » |
| Relance auto (Test 3) | « Rappel : votre appareil est prêt » |

Si rien n'arrive :
- Vérifie `application.properties` (host/port/username/password)
- Vérifie les logs du backend → erreur SMTP ?
- Pour Gmail : 2FA + mot de passe d'application obligatoires

---

## 7. Checklist de régression — rien d'ancien n'est cassé

Avant de finir, vérifie que ce qui marchait avant marche toujours :

- [ ] Création de ticket (AGENT_MAGASIN)
- [ ] Édition d'un ticket (AGENT_MAGASIN, TECHNICIAN, ADMIN)
- [ ] Création + acceptation de devis (INFOLINE)
- [ ] Acceptation/refus ligne par ligne par le client (frontend)
- [ ] Pagination + recherche + filtre statut sur la liste
- [ ] Bouton « 🖨️ Imprimer » (la fiche HTML existante) → marche toujours en parallèle du nouveau bon PDF
- [ ] Suppression d'un ticket (ADMIN)
- [ ] Historique des statuts toujours visible

---

## 8. Ce qui n'a **pas** été fait (par choix)

Pour info, voici les manques mineurs qui restent (volontairement out-of-scope) :

1. **Stockage cloud des PDFs signés** — actuellement local (`./uploads/`). Pour la prod
   Wiki Repair voudra peut-être S3/Azure Blob. Le `FileStorageService` est déjà préparé
   pour être remplacé.
2. **Génération du devis en PDF officiel** — le devis existe en JSON dans la base, le
   PDF de devis n'a pas été créé (la procédure ne le mentionnait pas explicitement,
   mais ce serait un bon next step).
3. **Workflow de paiement** — la facture est générée mais aucun statut "PAYÉ"
   n'est tracké (pas demandé dans la procédure).
4. **Dashboard / statistiques** — non demandé dans les PDFs.

---

## 9. Si quelque chose ne marche pas

- **Le backend ne démarre pas** → vérifie `JAVA_HOME` (besoin de JDK 17+) et que PostgreSQL tourne.
- **Le frontend ne se connecte pas au back** → vérifie `vite.config.js` proxy `/api → http://localhost:8080`.
- **Les boutons sont là mais ne font rien** → ouvre la console navigateur (F12) et regarde l'onglet Network pour voir les erreurs API.
- **PDF blanc / vide** → vérifie les logs backend, OpenPDF lance souvent une exception sur les caractères non latins (mais on a tout en français standard).

Bon test ! Reviens vers moi si quelque chose bloque.
