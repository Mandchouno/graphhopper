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
  echo "❌ Mutation score regression detected"
  exit 1
fi
```

### 1.2.5. Validation

- Sans modification des tests → score stable → workflow passe ✔️  
- Tests volontairement affaiblis → score plus faible → guard échoue ❌  
  - message clair dans le résumé GitHub Actions  
  - historique des scores affiché  
  - merge bloqué  

---

# 2. Classes testées

## 2.1. EdgeSampling

Responsable de :

- l’interpolation géographique,
- la génération de points intermédiaires,
- l’intégration des altitudes,
- la propagation correcte des distances et coordonnées.

## 2.2. HeightTile

Responsable de :

- la lecture des tuiles DEM,
- l’interprétation des valeurs binaires,
- le retour de l’altitude associée à une position latitude/longitude.

---

# 3. Dépendances mockées avec Mockito

## 3.1. ElevationProvider (EdgeSamplingTest)

```java
ElevationProvider mockElevation = mock(ElevationProvider.class);
when(mockElevation.getEle(anyDouble(), anyDouble())).thenReturn(50.0);
when(mockElevation.canInterpolate()).thenReturn(true);
```

## 3.2. DistanceCalcEarth (EdgeSamplingTest)

```java
DistanceCalcEarth mockDistance = mock(DistanceCalcEarth.class);
when(mockDistance.calcDist3D(
        anyDouble(), anyDouble(), anyDouble(),
        anyDouble(), anyDouble(), anyDouble()
)).thenReturn(1000.0);
```

## 3.3. DataAccess (HeightTileTest)

```java
DataAccess mockData = mock(DataAccess.class);
when(mockData.getShort(anyLong())).thenReturn((short) 50);
```

---

# 4. Justification des valeurs simulées

Les valeurs simulées ont été choisies pour :

- garantir un comportement déterministe ;
- éliminer la variabilité due aux données réelles ;
- tester uniquement la **logique interne** (interpolation, propagation des altitudes, lecture des tuiles) ;
- faciliter l’analyse des mutants générés par PIT.

Exemples :

- **Altitude simulée : 50 m**  
- **Distance simulée : 1000 m**

Ces valeurs sont simples, stables et suffisantes pour valider le comportement attendu.

---

# 5. Lien vers le dépôt GitHub modifié

*(À compléter par l’équipe)*

---

# 6. Documentation externe

*(À compléter : lien vers la page expliquant la démarche complète du projet)*
