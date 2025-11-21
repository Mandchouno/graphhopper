# README – Tâche 3 (IFT3913)

## 1. Modification des workflows GitHub Actions

### 1.1. Workflow principal : build + rickroll

Le fichier `.github/workflows/build.yml` a été modifié pour :

- compiler et exécuter les tests sous Java **24** et **25-ea** ;
- continuer l’exécution même si les tests échouent afin de permettre l’affichage du rickroll ;
- afficher un rickroll personnalisé dans le résumé du job lorsque les tests échouent ;
- forcer l’échec final du job après le rickroll.

#### 1.1.1. Ajout de l’action locale « rickroll »

Une action composite GitHub a été ajoutée dans :

```
.github/actions/rickroll/action.yml
```

Cette action écrit un message dans `GITHUB_STEP_SUMMARY` :

```yaml
name: Rickroll
description: Simple rickroll for failed builds
runs:
  using: composite
  steps:
    - shell: bash
      run: |
        cat >> "$GITHUB_STEP_SUMMARY" <<'EOF'
## Tests failed

"Never gonna pass your tests,
Never gonna fix your bugs,
Never gonna push a build and
watch it turn green suddenly..."

Click here for emotional support:
https://youtu.be/dQw4w9WgXcQ
EOF
```

Intégration dans le workflow :

```yaml
- name: Rickroll because tests failed
  if: steps.tests.outcome == 'failure'
  uses: ./.github/actions/rickroll
```

---

## 1.2. Workflow secondaire : PIT Guard  
(détection de régression du score de mutation)

Le fichier `.github/workflows/pit-guard.yml` implémente une protection qui échoue le build si le score de mutation PIT diminue par rapport au commit précédent.

### 1.2.1. Objectif

Garantir qu’aucun commit ne diminue la qualité globale des tests (score PIT stable ou croissant).

### 1.2.2. Déclencheur

Le workflow s’exécute automatiquement sur la branche "master" :

```
push:
  branches: [ "master" ]
```

### 1.2.3. Fonctionnement général

Le processus se déroule en quatre étapes :

1. **Exécution de PIT sur le commit courant (HEAD)**  
   - PIT est exécuté uniquement sur les classes ciblées grâce au paramètre :  
     ```
     -DtargetClasses=com.graphhopper.reader.dem.EdgeSampling,com.graphhopper.reader.dem.HeightTile
     ```
   - Un petit script (`scripts/pit_score.sh`) extrait ensuite le score exact à partir du fichier `mutations.xml`.

2. **Récupération du score de référence**  
   - Lors de la première exécution, aucun score n’est disponible : le score courant devient alors la référence.  
   - Lors des exécutions suivantes, le workflow télécharge le score stocké précédemment (sur la branche `master`).

3. **Comparaison du score courant avec le score de référence**  
   - Si le score courant est inférieur au score de référence **au-delà d’une tolérance configurée**, le workflow échoue.  
   - Sinon, le score est considéré valide.

4. **Mise à jour du score de référence**  
   - Si aucun recul n’est détecté, le score courant est enregistré afin de servir de référence à la prochaine exécution.

### 1.2.4. Tolérance (DELTA_TOL)

PIT peut produire de légères variations d’un run à l’autre sur GitHub Actions (charges variables, parallélisme, timeouts).  
Pour éviter des échecs injustifiés, une tolérance est introduite :

```
DELTA_TOL = 0.50    # tolérance de 0.50 point de pourcentage
```

Ainsi, une baisse trop faible pour être significative (< 0.5 point) est ignorée, mais toute diminution réelle du score entraîne bien l’échec du workflow.

### 1.2.5. Extraction du score

Le score est extrait dans `scripts/pit_score.sh` à partir du fichier `mutations.xml`, selon la formule :

```
score = (mutants_détectés / mutants_viables) * 100
```

Le script est robuste :  
- ignore les mutants marqués NON_VIABLE,  
- gère les rapports PIT datés,  
- produit une valeur numérique simple (ex : `82.35`).

### 1.2.6. Comparaison et validation

Exemple de logique utilisée dans le workflow :

```bash
if (diff > DELTA_TOL) then
    # le score a réellement baissé -> échec
else
    # pas de régression -> succès
fi
```

Si le score courant ne présente pas de régression, il devient le **nouveau score de référence** pour les exécutions futures.

### 1.2.7. Résultat attendu

- **Score stable ou amélioré** → le workflow réussit et met à jour la valeur de référence  
- **Score réellement diminué** → le workflow échoue et bloque l’intégration  
- **Faible variation (< DELTA_TOL)** → considérée comme du bruit ; mise à jour acceptée

Ce mécanisme garantit que les modifications futures ne pourront pas dégrader la robustesse des tests sans être détectées.

---

Choix des classes simulées

Deux dépendances majeures ont été simulées :
1. ElevationProvider
Cette classe fournit les valeurs d’altitude à partir de coordonnées géographiques.
Elle a été mockée dans EdgeSamplingTest afin de contrôler entièrement les valeurs d’altitude renvoyées et de vérifier que la méthode getEle(lat, lon) est bien appelée par EdgeSampling.sample().
2. DistanceCalcEarth
Ce calculateur de distances géographiques a également été mocké dans EdgeSamplingTest.
L’objectif est de s’assurer que le calcul des distances repose bien sur les appels à calcDist3D() et de vérifier la logique d’ajout de points intermédiaires indépendamment du calcul réel.
3. DataAccess
Cette interface a été simulée dans HeightTileTest.
Elle représente un accès bas niveau aux données binaires des tuiles DEM.

Définition des mocks et choix des valeurs simulées

Le mock permet de contrôler la valeur retournée par getShort(long index) sans avoir à créer ou initialiser de véritables fichiers de données.
Les mocks ont été définis à l’aide de la bibliothèque Mockito, en combinant les méthodes mock(), when() et verify().

1. Mock d’ElevationProvider
```java
ElevationProvider mockElevation = mock(ElevationProvider.class);
when(mockElevation.getEle(anyDouble(), anyDouble())).thenReturn(50.0);
when(mockElevation.canInterpolate()).thenReturn(true);
```
Ce mock renvoie une altitude fixe (50 m) pour simplifier les tests et s’assurer que la logique d’ajout de points repose uniquement sur la distance, pas sur la topographie réelle

2. Mock de DistanceCalcEarth
```java
DistanceCalcEarth mockDistance = mock(DistanceCalcEarth.class);
when(mockDistance.calcDist3D(anyDouble(), anyDouble(), anyDouble(),
                             anyDouble(), anyDouble(), anyDouble()))
        .thenReturn(1000.0);
```

Le mock renvoie une distance constante de 1000 m entre tous les points.
Cela permet de vérifier la logique du code d’EdgeSampling.sample() sans dépendre du calcul trigonométrique réel.

3. Mock de DataAccess
```java
DataAccess mockData = mock(DataAccess.class);
when(mockData.getShort(anyLong())).thenReturn((short) 50);
```
Ici, getShort() renvoie toujours 50, simulant une tuile d’altitude uniforme.
Ce choix permet d’obtenir un résultat prévisible de la méthode getHeight() sans avoir à utiliser une vraie mémoire RAMDirectory.

Les valeurs utilisées (50 pour l’altitude et 1000 pour la distance) ne servent pas à représenter des données réelles, mais à garantir la simplicité des tests.

L’objectif est de :
	•	éliminer toute variabilité liée à des calculs réels (trigonométrie, interpolation bilinéaire, etc.) ;
	•	se concentrer sur la vérification du comportement logique des méthodes testées :
	•	ajout ou non de points intermédiaires ;
	•	bonne propagation des appels aux dépendances ;
	•	respect des conditions seuils dans les interpolations.