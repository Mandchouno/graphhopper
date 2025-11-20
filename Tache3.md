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

Le workflow s’exécute automatiquement sur toutes les branches :

```
push:
  branches: [ "**" ]
```

### 1.2.3. Fonctionnement

1. Exécution de PIT sur le commit courant (**HEAD**) → extraction du score.
2. Checkout du commit précédent (**HEAD^**) → exécution de PIT.
3. Extraction des scores depuis `mutations.xml`.
4. Comparaison :
   - si `score_nouveau < score_ancien - DELTA_TOL` → **échec**
   - sinon → succès

PIT analyse toutes les classes du module core, selon la configuration définie dans core/pom.xml :

```
<targetClasses>
    <param>com.graphhopper.*</param>
</targetClasses>
```

Tolérance :

```
DELTA_TOL = 0.00
```

### 1.2.4. Extraction et comparaison des scores

Exemples d’extraction :

```bash
NEW_SCORE=$(grep -oP 'mutationCoverage="\K[0-9.]+' pit-reports/head/mutations.xml)
OLD_SCORE=$(grep -oP 'mutationCoverage="\K[0-9.]+' pit-reports/parent/mutations.xml)
```

Comparaison :

```bash
if (( $(echo "$NEW_SCORE < $OLD_SCORE - $DELTA_TOL" | bc -l) )); then
  echo "Mutation score regression detected"
  exit 1
fi
```

### 1.2.5. Validation

- Sans modification des tests → score stable → workflow passe   
- Tests volontairement affaiblis → score plus faible → guard échoue   
  - message clair dans le résumé GitHub Actions  
  - historique des scores affiché  
  - merge bloqué  

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

⸻

Les valeurs utilisées (50 pour l’altitude et 1000 pour la distance) ne servent pas à représenter des données réelles, mais à garantir la simplicité des tests.

L’objectif est de :
	•	éliminer toute variabilité liée à des calculs réels (trigonométrie, interpolation bilinéaire, etc.) ;
	•	se concentrer sur la vérification du comportement logique des méthodes testées :
	•	ajout ou non de points intermédiaires ;
	•	bonne propagation des appels aux dépendances ;
	•	respect des conditions seuils dans les interpolations.

# 5. Lien vers le dépôt GitHub modifié

*(À compléter par l’équipe)*

---

# 6. Documentation externe

*(À compléter : lien vers la page expliquant la démarche complète du projet)*
