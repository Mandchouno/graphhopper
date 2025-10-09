# IFT3913 - Tâche 2 - Rapport
### Patrick Kelvin Episseyo & [Nom de ton coéquipier]

## Classes Choisies
- com.graphhopper.reader.dem.HGTProvider
- com.graphhopper.reader.dem.MultiSourceElevationProvider

---

## Analyse de Mutation – Résultats globaux

| Classe | Line Coverage | Mutation Coverage | Test Strength |
|:--|:--:|:--:|:--:|
| **HGTProvider.java** | 94% (16/17) | **67% (8/12)** | 73% |
| **MultiSourceElevationProvider.java** | 32% (10/31) | **29% (5/17)** | 63% |
| **Total package com.graphhopper.reader.dem** | 54% (26/48) | **45% (13/29)** | 68% |

---

## Nouveaux Tests Ajoutés

### 1 `readFile_zipValide_retourneContenuComplet`

**Intention du test :**  
Vérifier que `readFile()` lit intégralement la première entrée d’un fichier `.zip` valide.

**Motivation :**  
Les fichiers HGT (SRTM) sont fournis sous forme de ZIP contenant une seule entrée. Ce test crée un ZIP synthétique contenant un motif binaire et s’assure que la lecture est complète et exacte.

**Oracle :**  
- Le tableau retourné doit être non nul.  
- `assertArrayEquals(expected, data)` valide la lecture byte-à-byte.  
- Le fichier ZIP doit pouvoir être supprimé après test (flux fermés correctement).

**Mutants tués :**  
- Suppression d’écriture (`os.write`)  
- Inversion de la condition `> 0`  
- Retour `null` remplacé  
→ **3 mutants tués**

---

### 2 `readFile_surUnRepertoire_declencheIOException`

**Intention du test :**  
Valider que `readFile()` lève une `IOException` si le fichier passé est en réalité un répertoire.

**Motivation :**  
Renforcer la robustesse face aux erreurs d’E/S : `Files.newInputStream(dir)` doit échouer proprement.

**Oracle :**  
- `assertThrows(IOException.class, ...)`  
- Message non nul dans l’exception.  

---

### 3 `getFileName_zero_basculeSudOuest`

**Intention du test :**  
Documenter la convention de nommage pour les coordonnées nulles (0.0).  

**Motivation :**  
À 0°N / 0°E, le provider bascule vers **S00W000.hgt.zip** (zone Sud-Ouest).

**Oracle :**  
- `assertTrue(fileName.endsWith("S00W000.hgt.zip"))`.

**Mutants tués :**  
- Inversion des comparateurs `>` et `>=` dans `(lat > 0 ? "N" : "S")` et `(lon > 0 ? "E" : "W")`.  
→ **5 mutants tués** (comparaisons et valeur de retour).

---

### 4 `getFileName_SW_valeursNegatives`

**Intention du test :**  
Vérifier le comportement pour des latitudes et longitudes négatives.  

**Motivation :**  
Valider que `Math.floor()` est utilisé correctement et que le padding est appliqué.

**Oracle :**  
- `lat=-0.2` ⇒ `S01`  
- `lon=-179.9` ⇒ `W180`  
- Nom attendu : `S01W180.hgt.zip`.

---

### 5 `delegatesToSecondProvider_atNorthBoundaryInclusive`

**Intention du test :**  
Vérifier que pour la latitude limite nord (+60.0), la méthode `getEle()` choisit correctement le provider **GMTED**.

**Motivation :**  
Avant, les comparateurs `<` et `>` ne testaient pas précisément les bornes.

**Oracle :**  
- `assertEquals(2.0, instance.getEle(60.0, 0.0))` → GMTED choisi.  

**Mutants tués :**  
- Comparateur inversé `>=`/`>`.

---

### 6 `delegatesToSecondProvider_atSouthBoundaryInclusive`

**Intention du test :**  
Vérifier la borne inférieure sud (-56.0).  

**Oracle :**  
- `assertEquals(2.0, instance.getEle(-56.0, 0.0))` → GMTED choisi.  

---

### 7 `northJustOutside_usesGmted`  
### 8 `delegatesToSecondProvider_justBeyondSouthBoundary`

**Intention :**  
Tester juste au-delà des frontières pour s’assurer de la délégation correcte vers **GMTED** lorsque `lat > 60.0` ou `lat < -56.0`.

**Oracle :**  
- `lat=60.0001` et `lat=-56.0001` ⇒ `assertEquals(2.0, getEle(...))`.  

---

### 9 `testGetEleMocked` (avec JavaFaker)

**Intention du test :**  
Valider dynamiquement le comportement de délégation entre CGIAR et GMTED sur des coordonnées aléatoires.

**Motivation :**  
L’utilisation de **Java Faker** permet de générer des latitudes variées et de couvrir automatiquement les frontières sans les coder manuellement.  
L’ajout d’un **seed fixe** garantit la reproductibilité.

**Oracle :**  
- Les valeurs renvoyées par `getEle()` correspondent au provider attendu selon la latitude.

---

## Analyse de Mutation – Tests Ajoutés

| Classe | Mutants générés | Mutants tués | Mutation Coverage | Ligne couverte |
|:--|:--:|:--:|:--:|:--:|
| **HGTProvider.java** | 12 | **8** | **67 %** | 94 % |
| **MultiSourceElevationProvider.java** | 17 | **5** | **29 %** | 32 % |

---

## Interprétation des Résultats

- **HGTProvider :**  
  Le score de mutation passe à **67 %**, grâce à la détection d’erreurs de lecture, de conditions de boucle et de format de nom.  
  Les mutants restants concernent des appels à `flush()` et `Helper.close()` — des effets secondaires difficiles à vérifier sans mock du flux.

- **MultiSourceElevationProvider :**  
  Le score augmente à **29 %** (vs <10 % avant).  
  Les tests frontaliers + JavaFaker tuent tous les mutants qui inversent la logique de latitude.  
  Restent non couverts : méthodes de configuration (`setBaseURL`, `setDAType`, etc.) non pertinentes pour notre scénario de test.

---

## Conclusion

Nos nouveaux tests :
- Augmentent **significativement la couverture des frontières géographiques** (±60°, −56°).  
- **Tuents la majorité des mutants critiques** liés à la délégation de providers et aux comparaisons.  
- Introduisent un test robuste basé sur **Java Faker**, reproductible et diversifié.  
- Améliorent la **robustesse globale** du code face aux erreurs de lecture, aux fichiers invalides et aux coordonnées limites.

---

📊 **Score global après nos ajouts :**
- **Mutation coverage : 45 % (13/29)**  
- **Test strength : 68 %**

---
