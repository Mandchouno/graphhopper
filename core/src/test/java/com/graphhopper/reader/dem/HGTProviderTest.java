/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.graphhopper.reader.dem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HGTProviderTest {

    @TempDir
    java.nio.file.Path tempDir;

    @Disabled
    @Test
    void getEle() {
        HGTProvider hgt = new HGTProvider("/your/path/to/hgt/");
        assertEquals(511, hgt.getEle(49.1, 11.7), 1);
        assertEquals(0, hgt.getEle(0.6, 0.6), 1);
    }


    /**
     * readFile_zipValide_retourneContenuComplet
     * Intention : Vérifier que readFile lit intégralement la première entrée d’un .zip valide.
     * Motivation : Générer un ZIP synthétique avec une entrée unique contenant un motif de bytes
     *              de taille 1200*1200*2 octets (format HGT classique 1201x1201, 2 octets/échantillon).
     * Oracle : tableau non nul et égalité stricte byte-à-byte (assertArrayEquals) avec le contenu écrit.
     */
    @Test
    void readFile_zipValide_retourneContenuComplet() throws Exception {
        File tmpDir = tempDir.toFile();
        HGTProvider hgt = new HGTProvider(tmpDir.getAbsolutePath());

        // 1) Préparer le ZIP synthétique
        File zipFile = new File(tmpDir, "test.hgt.zip");
        byte[] expected = new byte[1200 * 1200 * 2];
        for (int i = 0; i < expected.length; i++) expected[i] = (byte) (i & 0xFF); 

        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zos.putNextEntry(new ZipEntry("N00E000.hgt"));
            zos.write(expected);
            zos.closeEntry();
        }

        // 2) Act
        byte[] data = hgt.readFile(zipFile);

        // 3) Assert
        assertNotNull(data, "readFile doit retourner un tableau non nul");
        assertArrayEquals(expected, data, "Le contenu lu doit être identique au contenu écrit");
        assertTrue(zipFile.delete(), "Le ZIP doit être supprimable (flux correctement fermés)");
    }


    /**
     * readFile_surUnRepertoire_declencheIOException
     * Intention : Vérifier qu’une erreur d’E/S (ouvrir un répertoire) remonte bien en IOException.
     * Motivation : Passer un dossier au lieu d’un fichier ZIP ; Files.newInputStream(...) lève une IOException.
     * Oracle : IOException levée (et message non nul).
     */
    @Test
    void readFile_surUnRepertoire_declencheIOException() throws Exception {
        File tmpDir = tempDir.toFile();
        HGTProvider hgt = new HGTProvider(tmpDir.getAbsolutePath());

        File dir = new File(tmpDir, "aDir");
        assertTrue(dir.mkdir(), "Échec de création du répertoire de test");

        IOException ex = assertThrows(IOException.class, () -> hgt.readFile(dir));
        assertNotNull(ex.getMessage());

        // Test : Force un échec volontaire pour tester rickroll
        assertEquals(1, 2, "Forcing failure to test rickroll #2");
    }


    /**
     * getFileName_zero_basculeSudOuest
     * Intention : Documenter la convention : 0.0 => S/W (lat>0 ? N : S ; lon>0 ? E : W).
     * Motivation : lat=0.0, lon=0.0 avec cacheDir temporaire.
     * Oracle : le suffixe doit être 'S00W000.hgt.zip' (chemin ignoré car OS-dépendant).
     */
    @Test
    void getFileName_zero_basculeSudOuest() {
        HGTProvider hgt = new HGTProvider(tempDir.toFile().getAbsolutePath());
        String fileName = hgt.getFileName(0.0, 0.0);
        assertTrue(fileName.endsWith("S00W000.hgt.zip"));
    }


    /**
     * getFileName_SW_valeursNegatives
     * Intention : Vérifier l’usage de Math.floor sur valeurs négatives et le padding.
     * Motivation : lat=-0.2 -> floor=-1 => S01 ; lon=-179.9 -> floor=-180 => W180.
     * Oracle : suffixe 'S01W180.hgt.zip' (vérifié avec endsWith pour rester portable).
     */
    @Test
    public void getFileName_SW_valeursNegatives() {
        HGTProvider hgt = new HGTProvider(tempDir.toFile().getAbsolutePath());
        String fileName = hgt.getFileName(-0.2, -179.9);
        assertTrue(fileName.endsWith("S01W180.hgt.zip"));
    }

    
}