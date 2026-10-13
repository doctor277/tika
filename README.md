# IFT3913 — Tâche 2 (Automne 2026)

Ce dépôt contient notre travail pour la Tâche 2 du cours IFT3913. La classe étudiée est
`org.apache.tika.digest.InputStreamDigester`, dans le module autorisé `tika-core`. Aucun code de
production n'a été modifié. Les changements portent sur la configuration Maven, les tests, les
rapports expérimentaux, l'intégration continue et ce README.

- Dépôt de travail : <https://github.com/doctor277/tika>
- Branche : `ift3913-tache2`
- README de la tâche : <https://github.com/doctor277/tika/blob/ift3913-tache2/README.md>

## Équipe

| Nom complet | Identifiant GitHub |
|---|---|
| Rafi Dahoui | [doctor277](https://github.com/doctor277) |
| Alexandre Soglo Booh Louha | [Soglo3006](https://github.com/Soglo3006) |

## Choix de la classe

`InputStreamDigester` avait déjà un test, `testReservedDigestKeySurvivesGuard`, mais ce test
vérifiait seulement que le digest et la propriété existaient. Une mesure avant l'ajout de tests a
confirmé que la classe n'était pas complètement couverte : 25 lignes sur 28 et 4 branches sur 6.
PIT a aussi produit 12 mutants, dont seulement 3 étaient tués. Le score initial était donc de 25 %,
avec 8 survivants et 1 mutant non couvert. Ce résultat rend le choix objectif : la classe est de
taille raisonnable, possède déjà un test, et contient des comportements observables qui n'étaient
pas protégés (longueur, rewind, fournisseur cryptographique et valeur exacte du digest).

Une seule classe a été retenue. Cela permet d'analyser les oracles et chaque mutant avec précision,
plutôt que d'ajouter beaucoup de tests superficiels sur plusieurs classes.

## Environnement réellement utilisé

| Élément | Valeur vérifiée |
|---|---|
| Système | Windows 11 Home 10.0.26200 |
| Processeur | Intel Core i7-1360P, 12 cœurs et 16 processeurs logiques |
| Mémoire | 15,69 Gio de RAM |
| Graphique | Intel Iris Xe; l'inférence observée était CPU-only |
| Java | Oracle JDK 17.0.8 |
| Maven | Wrapper Maven 3.9.12 (`mvnw.cmd`) |
| Git | 2.51.0.windows.1 |
| Ollama | 0.40.1 |
| Modèle local | `codeqwen:v1.5-chat`, 4,2 Go sur disque, environ 4,6 Go chargé |
| ChatUniTest | plugin Maven 2.1.1 |
| PIT | plugin Maven 1.30.0 et adaptateur JUnit 5 1.2.3 |

Le modèle était accessible à `http://127.0.0.1:11434/v1/chat/completions` avec la clé factice
`ollama`. Un appel simple à l'API compatible OpenAI a fonctionné. Les mesures faites avant ce
travail donnaient environ 8,1 jetons/s avec Ollama natif et 7,11 jetons/s via l'API compatible
OpenAI. ChatUniTest a ensuite demandé environ huit minutes pour analyser 361 classes et 1 617
méthodes, puis quatre à six minutes par génération ou réparation. La machine n'a pas manqué de
mémoire et aucun processus important n'a été fermé automatiquement, mais la génération est lente et
peu interactive. Le modèle est donc utilisable ponctuellement sur ce portable, pas confortable pour
une grande campagne. Aucun service d'inférence infonuagique ni aucune API payante n'a été utilisé.

## Configuration Maven

Le fichier `tika-core/pom.xml` déclare ChatUniTest sans l'attacher au cycle Maven normal. Il faut
l'invoquer explicitement, ce qui évite de dépendre d'Ollama pendant une compilation ordinaire.
La configuration fixe le modèle, l'URL locale, un seul échantillon, trois tours au maximum et une
température de 0,2. Les sorties temporaires vont sous `tika-core/target/chatunitest-info`.

PIT est limité à `InputStreamDigester` et aux classes de test dont le nom commence par
`InputStreamDigester`. Les seuils finaux de couverture et de mutation sont fixés à 100 %. Les
rapports HTML et XML sont générés dans `tika-core/target/pit-reports`.

## Génération réelle avec ChatUniTest

Le premier lancement a échoué avant la génération : les options `project` et `maxThread` proposées
dans la configuration n'étaient pas acceptées par la version 2.1.1. Elles ont été retirées, sans
modifier ChatUniTest lui-même.

Au lancement suivant, ChatUniTest a réellement analysé le projet et appelé CodeQwen localement.
Pour le premier échantillon, le tour initial et les deux réparations automatiques n'ont pas compilé.
Un deuxième échantillon indépendant a aussi échoué à son premier tour; il a alors été interrompu,
car il répétait la même boucle coûteuse après qu'un échantillon complet eut déjà été observé.
Les quatre sorties brutes avec leurs diagnostics sont conservées dans
[`tache2-results/chatunitest/raw-errors`](tache2-results/chatunitest/raw-errors/).

Le test brut essayait de simuler `TikaInputStream`, `Metadata`, `MessageDigest` et une table privée
avec Mockito. Il ne compilait pas parce que Mockito n'est pas une dépendance de `tika-core`. Il
inventait aussi `TikaInputStream.newMessageDigest()`, considérait que `Metadata.set` retournait une
valeur et utilisait les chaînes invalides `algorithm` et `metadataKey` comme configuration réelle.
Les deux réparations automatiques ont seulement réorganisé les annotations Mockito; elles n'ont pas
retiré ces hypothèses incorrectes.

Il y a donc eu **deux réparations automatiques infructueuses**, puis **une correction du test avec
l'assistance de Codex**. Cette correction comporte cinq changements concrets : retrait des mocks et imports Mockito,
utilisation d'un vrai `TikaInputStream`, création d'un vrai `Metadata`, choix de `SHA-256` et d'une
clé valide, puis remplacement de l'oracle simulé par la constante SHA-256/Base64 calculée
indépendamment. Le résultat exécutable est
[`InputStreamDigesterChatUniTest.java`](tika-core/src/test/java/org/apache/tika/digest/InputStreamDigesterChatUniTest.java).

Le test corrigé utilise `hello world` et vérifie exactement :

```text
uU0nuZNNPgilLlLX2n2r+sSE7+N6U4DukIj3rOLvzek=
```

## Comparaison des oracles

Le test original contient deux oracles d'existence : le digest n'est pas nul et la propriété est
enregistrée. Ils sont utiles pour la garde des clés réservées, mais acceptent n'importe quelle valeur
de digest non nulle. Le brouillon de CodeQwen proposait une égalité exacte, ce qui était une bonne
idée, mais la valeur provenait d'un `MessageDigest` simulé et tout le montage était inexécutable.

Après correction, l'oracle IA vérifie la vraie valeur SHA-256/Base64. Il détecte donc une boucle de
lecture qui s'arrête trop tôt et la suppression de `MessageDigest.update`. Il ne vérifie toujours ni
la longueur, ni le rewind, ni la branche du fournisseur. Les oracles des tests supplémentaires,
développés avec l'assistance de Codex après l'analyse des mutants survivants, portent sur ces effets
de bord et contrats publics. Ils sont plus diversifiés et directement liés aux mutants encore vivants.

## Résultats aux trois étapes

| Étape | Tests JUnit ciblés | Lignes | Branches | Mutants tués | Score PIT | Force des tests |
|---|---:|---:|---:|---:|---:|---:|
| Tests originaux | 1 | 25/28 (89,29 %) | 4/6 (66,67 %) | 3/12 | 25 % | 27 % |
| Original + test IA corrigé | 2 | 25/28 (89,29 %) | 4/6 (66,67 %) | 5/12 | 41,67 % (42 % affiché) | 45 % |
| Final avec tests supplémentaires assistés par Codex | 6 | 28/28 (100 %) | 6/6 (100 %) | 12/12 | 100 % | 100 % |

Les rapports utilisés pour ce tableau ont été archivés après chaque étape, avant l'ajout des tests
de l'étape suivante :

- [référence](tache2-results/baseline/) : Surefire, JaCoCo XML et PIT HTML/XML;
- [après le test IA corrigé](tache2-results/generated/) : mêmes trois familles de rapports;
- [résultat final](tache2-results/final/) : mêmes trois familles de rapports.

À chaque étape, Surefire a compté les tests ciblés, JaCoCo a mesuré les lignes et branches de
`InputStreamDigester`, et PIT a compté les mutants tués parmi les 12 mutants produits. Le score PIT
est le nombre de mutants tués divisé par 12. Les étapes de référence et intermédiaire ont été
mesurées avant l'ajout des tests suivants; les rapports archivés en gardent la preuve. Le code
actuel permet de relancer directement l'étape finale, pas de recréer les deux états antérieurs sans
changer temporairement la sélection des tests.

## Mutants et effet du test généré

| Ligne | Mutation | Original | Avec IA | Explication |
|---:|---|---|---|---|
| 110 | négation de la condition de la boucle de lecture | survivant | tué | le digest exact n'est plus produit si la boucle ne lit pas normalement |
| 111 | suppression de `MessageDigest.update` | survivant | tué | le SHA-256 obtenu devient celui d'un flux vide, différent de la constante attendue |

Le test IA n'a pas changé la couverture de lignes, mais il a renforcé l'oracle sur les lignes déjà
exécutées. Les trois mutants déjà tués par le test original se trouvent aux lignes 68, 69 et 116.
Après l'étape IA, six mutants survivaient encore et un autre n'était pas couvert : condition et
écriture de la longueur (59 et 61), branche fournisseur (71), activation du rewind (104), calcul de
la longueur (112), appel à `setContentLength` (115) et rewind final (118).

## Tests supplémentaires après l'analyse des mutants

Les quatre méthodes se trouvent dans
[`InputStreamDigesterManualTest.java`](tika-core/src/test/java/org/apache/tika/digest/InputStreamDigesterManualTest.java).
Le nom technique `ManualTest` a été conservé pour distinguer cette étape de la sortie de
ChatUniTest; il ne signifie pas que ces tests ont été écrits sans assistance. Codex a contribué à
leur conception et à leur rédaction à partir des mutants survivants.

| Test | Intention et données | Oracle et mutants visés |
|---|---|---|
| `testDigestSetsLengthAndRewindsRawStream` | Utilise 9 000 octets identiques, donc plus que le tampon de 8 192 octets, dans un `ByteArrayInputStream` brut. Cela force plusieurs lectures et exige l'activation explicite du cache de rewind. | Vérifie `Content-Length = 9000`, puis relit exactement les 9 000 octets. Tue l'addition remplacée par une soustraction, les suppressions de `setContentLength`, `enableRewind` et `rewind`, ainsi que l'écriture de longueur manquante. |
| `testDigestPreservesExistingContentLength` | Place la valeur `1234` avant de traiter trois octets. La différence volontaire permet de voir un écrasement. | Vérifie que `1234` est conservé. Tue la négation de la condition « longueur absente ou vide ». |
| `testDigestUsesConfiguredProvider` | Récupère le vrai fournisseur Java de SHA-256 et le retourne dans une sous-classe minimale. L'encodeur renvoie la taille du digest. | Vérifie `32`, taille en octets d'un SHA-256. Couvre la branche fournisseur et tue le retour `null` injecté par PIT. |
| `testUnknownAlgorithmIsRejected` | Utilise le nom impossible `not-a-message-digest`. | `assertThrows(IllegalArgumentException.class, ...)` documente le contrat d'erreur et couvre le chemin de l'exception. |

Au dernier passage, aucun mutant ne reste vivant ou non couvert.

## Commandes principales exécutées et résultats

Toutes les commandes suivantes ont été exécutées dans le dépôt Tika sous Windows PowerShell.

```powershell
# Test original et couverture de référence : 1 test réussi
.\mvnw.cmd -B -ntp -pl tika-core -am '-Dtest=org.apache.tika.digest.InputStreamDigesterTest' '-Dsurefire.failIfNoSpecifiedTests=false' test

# Installation locale des artefacts nécessaires : succès
.\mvnw.cmd -B -ntp -pl tika-core -am -DskipTests install

# Construction des cinq modules autorisés et de leurs dépendances : 25 modules, succès
.\mvnw.cmd -B -ntp -pl tika-core,tika-serialization,tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-ocr-module,tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-pdf-module,tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-microsoft-module -am package -DskipTests

# PIT de référence : 12 mutants, 3 tués, score 25 %
cd tika-core
..\mvnw.cmd -B -ntp -U org.pitest:pitest-maven:1.30.0:mutationCoverage

# Premier essai ChatUniTest : échec de configuration sur le paramètre Maven project
..\mvnw.cmd -B -ntp -U io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1:class '-DselectClass=org.apache.tika.digest.InputStreamDigester'

# Génération réelle après correction : un échantillon complet en trois tours, aucun ne compile;
# deuxième échantillon interrompu après répétition du même type d'échec
..\mvnw.cmd -B -ntp io.github.zju-aces-ise:chatunitest-maven-plugin:2.1.1:class '-DselectClass=org.apache.tika.digest.InputStreamDigester'

# Original + test IA corrigé : 2 tests réussis
cd ..
.\mvnw.cmd -B -ntp -pl tika-core -am '-Dtest=org.apache.tika.digest.InputStreamDigester*' '-Dsurefire.failIfNoSpecifiedTests=false' test

# PIT intermédiaire : 12 mutants, 5 tués, score affiché 42 %
cd tika-core
..\mvnw.cmd -B -ntp org.pitest:pitest-maven:1.30.0:mutationCoverage

# Suite finale : 6 tests réussis, aucun échec, aucune erreur, aucun test ignoré
cd ..
.\mvnw.cmd -B -ntp -pl tika-core -am '-Dtest=org.apache.tika.digest.InputStreamDigester*' '-Dsurefire.failIfNoSpecifiedTests=false' test

# Régression complète du module : tika-core 754 tests, 0 échec, 0 erreur, 3 ignorés;
# annotation processor 8 tests, tous réussis
.\mvnw.cmd -B -ntp -pl tika-core -am test

# PIT final : 12 mutants sur 12 tués, couverture et force des tests 100 %
cd tika-core
..\mvnw.cmd -B -ntp org.pitest:pitest-maven:1.30.0:mutationCoverage
```

Un premier raccourci `pitest:mutationCoverage` a aussi échoué après une panne DNS temporaire vers
Maven Central. La relance avec le triplet complet et `-U` a réussi. Les avertissements Maven sur la
version `${revision}` du POM parent existaient déjà dans le projet et ne viennent pas de ce travail.

## GitHub Actions

Le workflow [`.github/workflows/ift3913-tache2.yml`](.github/workflows/ift3913-tache2.yml) utilise
`ubuntu-latest`, Temurin 17, `checkout@v7`, `setup-java@v6`, `upload-artifact@v7` et le cache Maven.
Il installe d'abord `tika-core` et ses dépendances du réacteur avec `-DskipTests`, pour rendre les
artefacts SNAPSHOT disponibles à PIT sur un runner neuf. Il lance ensuite les six tests ciblés,
puis PIT avec les seuils finaux de 100 %, et publie les rapports Surefire, JaCoCo et PIT même en
cas d'échec. Le délai maximal est de 45 minutes. Les commandes Maven équivalentes ont été
validées localement. L'[exécution GitHub Actions #38106456007](https://github.com/doctor277/tika/actions/runs/38106456007)
a également réussi : six tests ciblés sans échec, 28/28 lignes et 6/6 branches couvertes par
JaCoCo pour `InputStreamDigester`, et 12/12 mutants tués par PIT.

## Reproduction des résultats

Le rapport final peut être régénéré avec :

```powershell
.\mvnw.cmd -B -ntp -pl tika-core -am -DskipTests install
.\mvnw.cmd -B -ntp -pl tika-core -am '-Dtest=org.apache.tika.digest.InputStreamDigester*' '-Dsurefire.failIfNoSpecifiedTests=false' test
Set-Location tika-core
..\mvnw.cmd -B -ntp org.pitest:pitest-maven:1.30.0:mutationCoverage
```

ChatUniTest exige en plus qu'Ollama soit démarré et que `codeqwen:v1.5-chat` soit déjà téléchargé.
Le test généré corrigé et les tests supplémentaires n'ont pas besoin d'Ollama pour s'exécuter.

## Vérification par rapport au barème

| Critère | État et preuve |
|---|---|
| Classes à tester (10 %) | Fait : choix justifié par 25/28 lignes et 9 mutants non tués ou non couverts à la référence. |
| IA et test (10 %) | Fait : ChatUniTest 2.1.1 est déclaré dans `tika-core/pom.xml` et configuré pour Ollama local. |
| Tests générés (10 %) | Fait : pipeline réel exécuté; quatre sorties brutes sont conservées et un test corrigé est exécutable. |
| Documentation des tests (20 %) | Fait : échecs, corrections, assertion utile et comparaison des oracles sont détaillés. |
| Mutation (15 %) | Fait : rapports PIT conservés pour les étapes originale, IA corrigée et finale. |
| Documentation des mutants (15 %) | Fait : les deux mutants nouvellement tués par l'oracle IA et les sept cas restants sont expliqués. |
| Tests supplémentaires (10 %) | Fait : quatre tests développés avec l'assistance de Codex et documentés avec intention, données et oracle. |
| Exécution (10 %) | Validation locale réussie; workflow GitHub Actions exécuté avec succès ([run #38106456007](https://github.com/doctor277/tika/actions/runs/38106456007)). |
| Format de documentation | Fait : toute la documentation de la tâche est dans ce seul README à la racine. |

## Déclaration d'utilisation de l'intelligence artificielle

Nous avons utilisé OpenAI Codex comme assistant pour inspecter le dépôt, configurer les plugins,
proposer et exécuter les commandes, analyser les rapports, corriger le test généré par ChatUniTest,
développer les tests supplémentaires à partir des mutants survivants et structurer cette
documentation. Les changements ont été vérifiés par compilation, JUnit, JaCoCo et PIT; les
résultats chiffrés de ce README proviennent des rapports conservés dans le dépôt.

Pour la génération demandée par la tâche, ChatUniTest 2.1.1 a utilisé exclusivement le modèle ouvert
`codeqwen:v1.5-chat` exécuté localement avec Ollama. Les sorties originales, y compris les échecs,
sont conservées. Aucun test brut n'a été présenté comme valide : la correction assistée par Codex et
ses raisons sont décrites ci-dessus. ChatUniTest n'a utilisé aucun service d'IA payant ni modèle
infonuagique pour générer ses tests.

## Annexe — README Apache Tika original

Le contenu amont en anglais est conservé afin de ne pas supprimer la documentation du projet étudié.

<details>
<summary>Afficher le README Apache Tika original</summary>


Welcome to Apache Tika  <https://tika.apache.org/>
=================================================

[![license](https://img.shields.io/github/license/apache/tika.svg?maxAge=2592000)](http://www.apache.org/licenses/LICENSE-2.0)
[![Jenkins](https://img.shields.io/jenkins/s/https/ci-builds.apache.org/job/Tika/job/tika-main-jdk17.svg?maxAge=3600)](https://ci-builds.apache.org/job/Tika/job/tika-main-jdk17/)
[![Jenkins tests](https://img.shields.io/jenkins/t/https/ci-builds.apache.org/job/Tika/job/tika-main-jdk17.svg?maxAge=3600)](https://ci-builds.apache.org/job/Tika/job/tika-main-jdk17/lastBuild/testReport/)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.tika/tika.svg?maxAge=86400)](http://search.maven.org/#search|ga|1|g%3A%22org.apache.tika%22)

Apache Tika(TM) is a toolkit for detecting and extracting metadata and structured text content from various documents using existing parser libraries.

Tika is a project of the [Apache Software Foundation](https://www.apache.org).

Apache Tika, Tika, Apache, the Apache feather logo, and the Apache Tika project logo are trademarks of The Apache Software Foundation.

Quick Start
===========

**Parse a file in Java:**

```java
import org.apache.tika.Tika;

Tika tika = new Tika();
String text = tika.parseToString(new File("document.pdf"));
System.out.println(text);
```

**From the command line:**

```bash
java -jar tika-app-*.jar --text document.pdf
```

**Maven dependency:**

```xml
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-parsers-standard-package</artifactId>
    <version>4.x.y</version>
    <type>pom</type>
</dependency>
```

Getting Started
===============
Pre-built binaries of Apache Tika standalone applications are available
from https://tika.apache.org/download.html . Pre-built binaries of all the
Tika jars can be fetched from Maven Central or your favourite Maven mirror.

**Tika 2.X and support for Java 8 reached End of Life (EOL) in April, 2025. 
See [Tika Roadmap 2.x, 3.x and beyond](https://cwiki.apache.org/confluence/display/TIKA/Tika+Roadmap+--+2.x%2C+3.x+and+Beyond).** 

Tika is based on **Java 17** and uses the [Maven 3](https://maven.apache.org) build system.
**N.B.** [Docker](https://www.docker.com/products/personal) is used for tests in tika-integration-tests. If Docker is not installed, those tests are skipped.

To build Tika from source, use the following command in the main directory:

    ./mvnw clean install

The Maven wrapper (`mvnw`) is included in the repository and will automatically download
the correct Maven version if needed. On Windows, use `mvnw.cmd` instead.

The build consists of a number of components, including a standalone runnable jar that you can use to try out Tika features. You can run it like this:

    java -jar tika-app/target/tika-app-*.jar --help


To build a specific project (for example, tika-server-standard):

    ./mvnw clean install -am -pl :tika-server-standard

If the ossindex-maven-plugin is causing the build to fail because a dependency
has now been discovered to have a vulnerability:

    ./mvnw clean install -Dossindex.skip


Faster Builds
=============

**Fast profile** - Use `-Pfast` to skip tests, checkstyle, and spotless:

    ./mvnw clean install -Pfast

**Parallel builds** - Add `-T1C` to build with 1 thread per CPU core:

    ./mvnw clean install -Pfast -T1C

**Maven Daemon (mvnd)** - Keeps a warm JVM running for 2-3x faster rebuilds:

```bash
# Install: https://github.com/apache/maven-mvnd
# macOS: brew install mvndaemon/tap/mvnd

# Use exactly like mvn
mvnd clean install -Pfast
mvnd test -pl :tika-core
```

**Combine both** for maximum speed during development:

    mvnd clean install -Pfast -T1C


Reproducible Builds
===================

Apache Tika supports [reproducible builds](https://reproducible-builds.org/). This means
that building the same source code with the same JDK version should produce
byte-for-byte identical artifacts, regardless of the build machine or time.

Key configuration:
- `project.build.outputTimestamp` is set in `tika-parent/pom.xml`
- All Maven plugins are configured to produce deterministic output

To verify the build plan supports reproducibility:

    ./mvnw artifact:check-buildplan

To verify two builds produce identical artifacts:

    ./mvnw clean install -DskipTests
    mv ~/.m2/repository/org/apache/tika tika-build-1
    ./mvnw clean install -DskipTests
    diff -r tika-build-1 ~/.m2/repository/org/apache/tika


Maven Dependencies
==================

Apache Tika provides *Bill of Material* (BOM) artifact to align Tika module versions and simplify version management. 
To avoid convergence errors in your own project, import this
bom or Tika's parent pom.xml in your dependency management section.

If you use Apache Maven:

```xml
<project>
  <dependencyManagement>
    <dependencies>
      <dependency>
       <groupId>org.apache.tika</groupId>
       <artifactId>tika-bom</artifactId>
       <version>4.x.y</version>
       <type>pom</type>
       <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <dependency>
      <groupId>org.apache.tika</groupId>
      <artifactId>tika-parsers-standard-package</artifactId>
      <type>pom</type>
      <!-- version not required since BOM included -->
    </dependency>
  </dependencies>
</project>
```

For Gradle:

```kotlin
dependencies {
  implementation(platform("org.apache.tika:tika-bom:4.x.y"))

  // version not required since bom (platform in Gradle terms)
  implementation("org.apache.tika:tika-parsers-standard-package@pom")
}
```

Migrating to 4.x
================
TBD

Contributing
============
See [CONTRIBUTING.md](CONTRIBUTING.md) and https://tika.apache.org/contribute.html

[![contributors](https://contributors-img.web.app/image?repo=apache/tika)](https://github.com/apache/tika/graphs/contributors)

Building from a Specific Tag
============================
Let's assume that you want to build the 3.0.1 tag:
```
0. Download and install hub.github.com
1. git clone https://github.com/apache/tika.git
2. cd tika
3. git checkout 3.0.1
4. ./mvnw clean install
```

If a new vulnerability has been discovered between the date of the
tag and the date you are building the tag, you may need to build with:

```
4. ./mvnw clean install -Dossindex.skip
```

If a local test is not working in your environment, please notify
 the project at dev@tika.apache.org. As an immediate workaround,
 you can turn off individual tests with e.g.:

```
4. ./mvnw clean install -Dossindex.skip -Dtest=\!UnpackerResourceTest#testPDFImages
```

License (see also LICENSE.txt)
==============================

Collective work: Copyright 2011 The Apache Software Foundation.

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at

<https://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the specific language governing permissions and limitations under the License.

Apache Tika includes a number of subcomponents with separate copyright notices and license terms. Your use of these subcomponents is subject to the terms and conditions of the licenses listed in the LICENSE.txt file.

Export Control
==============

This distribution includes cryptographic software.  The country in which you currently reside may have restrictions on the import, possession, use, and/or re-export to another country, of encryption software.  BEFORE using any encryption software, please  check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of encryption software, to  see if this is permitted.  See <http://www.wassenaar.org/> for more information.

The U.S. Government Department of Commerce, Bureau of Industry and Security (BIS), has classified this software as Export Commodity Control Number (ECCN) 5D002.C.1, which includes information security software using or performing cryptographic functions with asymmetric algorithms.  The form and manner of this Apache Software Foundation distribution makes it eligible for export under the License Exception ENC Technology Software Unrestricted (TSU) exception (see the BIS Export Administration Regulations, Section 740.13) for both object code and source code.

The following provides more details on the included cryptographic software:

Apache Tika uses the Bouncy Castle generic encryption libraries for extracting text content and metadata from encrypted PDF files.  See <http://www.bouncycastle.org/> for more details on Bouncy Castle.  

Mailing Lists
=============

* user@tika.apache.org - About using Tika
* dev@tika.apache.org - About developing Tika

Subscribe by sending a message to `{list}-subscribe@tika.apache.org`.

Issue Tracker
=============

https://issues.apache.org/jira/browse/TIKA

Security
========

See [SECURITY.md](SECURITY.md) and https://tika.apache.org/security.html

</details>
