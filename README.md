Choix des classes testées

Les tests sont portés sur deux classes principales :
	•	EdgeSampling : responsable de l’interpolation et de la génération de points intermédiaires le long d’un segment géographique en fonction de la distance et de l’altitude.
EdgeSampling est un bon choix car elle intervient dans le calcul de routes précises tenant compte du relief.
	•	HeightTile : gère la lecture et l’interprétation des tuiles d’altitude en permettant d’obtenir la hauteur correspondant à une latitude et une longitude.
HeightTile est un bon choix car elle constitue la structure de base de stockage et d’accès aux données d’altitude.

⸻

Choix des classes simulées

Deux dépendances majeures ont été simulées :
	1.	ElevationProvider
Cette classe fournit les valeurs d’altitude à partir de coordonnées géographiques.
Elle a été mockée dans EdgeSamplingTest afin de contrôler entièrement les valeurs d’altitude renvoyées et de vérifier que la méthode getEle(lat, lon) est bien appelée par EdgeSampling.sample().
	2.	DistanceCalcEarth
Ce calculateur de distances géographiques a également été mocké dans EdgeSamplingTest.
L’objectif est de s’assurer que le calcul des distances repose bien sur les appels à calcDist3D() et de vérifier la logique d’ajout de points intermédiaires indépendamment du calcul réel.
	3.	DataAccess
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

Justification des valeurs simulées

Les valeurs utilisées (50 pour l’altitude et 1000 pour la distance) ne servent pas à représenter des données réelles, mais à garantir la simplicité des tests.

L’objectif est de :
	•	éliminer toute variabilité liée à des calculs réels (trigonométrie, interpolation bilinéaire, etc.) ;
	•	se concentrer sur la vérification du comportement logique des méthodes testées :
	•	ajout ou non de points intermédiaires ;
	•	bonne propagation des appels aux dépendances ;
	•	respect des conditions seuils dans les interpolations.