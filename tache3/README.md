# IFT3913 — Tâche 3 (automne 2026)

**Équipe :** Rafi Dahoui (`doctor277`) et Alexandre Soglo Booh Louha (`Soglo3006`).

Ce travail est sur la branche `rafi` du [fork Tika](https://github.com/doctor277/tika/tree/rafi). La branche part du travail de la tâche 2, au commit `88c415786765056543b08d6e265624025ee95fed`. Aucun changement n'est destiné à la branche `main`. La [consigne 2026](https://github.com/umontreal-diro/IFT3913/blob/2026/tache3/readme.md) nomme Tika comme cas d'étude dans le plan de cours, mais emploie encore « graphhopper » dans la phrase sur le rickroll. Nous avons appliqué cette phrase aux tests du cas d'étude 2026, donc à Tika.

## Porte de sécurité dans GitHub Actions

Le workflow [`.github/workflows/ift3913-tache3.yml`](../.github/workflows/ift3913-tache3.yml) se lance sur la branche `rafi`, sur demande, ou sur une pull request. Sous Ubuntu avec Java 17, il installe les modules Maven nécessaires, exécute les six tests ciblés de `InputStreamDigester` et produit JaCoCo. Il installe ensuite Semgrep Community Edition 1.185.0, valide les sept règles locales [`.semgrep/tache3.yml`](../.semgrep/tache3.yml), analyse `tika-core/src/main/java` et conserve le rapport JSON.

Le script [`compare_semgrep.py`](../scripts/ift3913_tache3/compare_semgrep.py) compare chaque résultat au [rapport de référence](../tache3-results/baseline/custom.json) par identifiant de règle, chemin, ligne et colonne. Il retourne 0 si aucun résultat nouveau n'apparaît, 1 s'il en apparaît et 2 si un rapport est invalide. Les erreurs de Semgrep sont bloquantes. Les rapports sont téléversés même lorsque le job échoue. Cette référence est volontairement fixe : elle représente l'état avant les sept ajouts. Un simple déplacement de ligne peut être signalé comme nouveau résultat; le rapport doit alors être examiné avant de modifier la référence. Nous ne mettons pas automatiquement la référence à jour, car cela masquerait une régression.

La validation locale du comparateur a donné `0 → 0`, code de sortie **0**, puis `0 → 7`, code de sortie **1**. Ainsi, le job avec les sept exemples doit échouer à l'étape de comparaison; cet échec est la démonstration attendue de la porte de sécurité, pas un échec des tests Java. Le comportement de GitHub Actions doit être confirmé par un vrai run après publication de la branche; un résultat local ne prouve pas à lui seul le résultat distant.

Si l'étape JUnit échoue, le workflow invoque l'[action composite réutilisable](../.github/actions/tache3-rickroll/action.yml). Elle affiche le lien vers *Never Gonna Give You Up* dans une annotation et dans le résumé du job. Sa condition porte précisément sur l'échec de l'étape JUnit; un échec Semgrep seul ne déclenche pas le rickroll. Les tests ne sont pas rendus artificiellement rouges.

## Sept vulnérabilités introduites

Les sept exemples sont réunis dans [`Tache3SecurityExamples.java`](../tika-core/src/main/java/org/apache/tika/utils/Tache3SecurityExamples.java), un nouveau fichier de `tika-core`. Ce choix rend les différences avant/après vérifiables et évite d'altérer les chemins normaux de parsing. La classe est volontairement non appelée par Tika et ne doit pas être intégrée à une version de production. Les risques décrits ci-dessous supposent qu'un appelant lui passe des données non fiables. Chaque ligne est détectée par une règle Semgrep locale distincte.

| Type | Méthode | Pourquoi le code est vulnérable | Règle et ligne du résultat |
|---|---|---|---|
| XXE (CWE-611) | `parseUntrustedXml` | Autorise explicitement les déclarations DOCTYPE et les entités externes pour un XML fourni par l'appelant. | `ift3913-t3-xxe`, ligne 53 |
| Traversée de chemin (CWE-22) | `readRelativeFile` | Résout un nom fourni par l'appelant sous un répertoire sans vérifier que le chemin normalisé y reste. | `ift3913-t3-path-traversal`, ligne 59 |
| Exécution de commande (CWE-78) | `runTool` | Transmet directement la commande fournie par l'appelant à `Runtime.exec`, sans liste de commandes permises. | `ift3913-t3-command-injection`, ligne 63 |
| Injection SQL (CWE-89) | `lookupRecord` | Concatène le nom fourni par l'appelant dans la requête au lieu d'utiliser un paramètre SQL. | `ift3913-t3-sql-injection`, ligne 68 |
| Désérialisation non fiable (CWE-502) | `restoreObject` | Lit un objet Java depuis un flux fourni par l'appelant sans filtre de types. | `ift3913-t3-deserialization`, ligne 78 |
| Chiffrement ECB (CWE-327) | `encryptBlocks` | Utilise AES/ECB, qui révèle l'égalité des blocs de texte clair. | `ift3913-t3-ecb`, ligne 86 |
| HTTP en clair (CWE-319) | `fetchConfiguration` | Récupère une configuration via HTTP, exposée à la lecture et à la modification en transit. | `ift3913-t3-cleartext-http`, ligne 92 |

`Runtime.exec(String)` n'invoque pas de shell par défaut; le danger démontré est l'exécution d'un programme ou d'arguments contrôlés par l'appelant, et non une expansion shell implicite. Ces exemples sont pédagogiques et ne prétendent pas être des chemins exploitables dans l'application Tika normale.

## Rapports avant et après

Le rapport [avant, règles locales](../tache3-results/baseline/custom.json) couvre les **324 fichiers Java** présents avant l'ajout et contient **0 résultat**, sans erreur. Le rapport [après, règles locales](../tache3-results/introduced/custom.json) couvre **325 fichiers** et contient **7 résultats**, sans erreur : un pour chacun des sept types ci-dessus. Le rapport avant a été regénéré avec la version définitive des règles en excluant seulement le nouveau fichier, pour conserver les mêmes 324 fichiers du point de départ.

Une deuxième analyse avec le pack public `p/java` (60 règles Java) donne [3 résultats préexistants](../tache3-results/baseline/semgrep.json) et [9 résultats après l'ajout](../tache3-results/introduced/semgrep.json), soit **6 nouveaux résultats** dans le fichier d'exemples. Ces six alertes concernent trois catégories : XXE (3), SQL (1) et AES/ECB (2). Les règles locales documentées ci-dessus couvrent les quatre autres catégories; nous ne prétendons pas que le pack public détecte les sept types. Aucun des trois résultats préexistants n'a été modifié dans ce travail.

## Reproduction locale

Depuis la racine du dépôt, avec Java 17, Python 3.11 et Semgrep 1.185.0 :

```powershell
.\mvnw.cmd -B -ntp -pl tika-core -am -DskipTests compile
.\mvnw.cmd -B -ntp -pl tika-core -am '-Dtest=org.apache.tika.digest.InputStreamDigester*' '-Dsurefire.failIfNoSpecifiedTests=false' test
semgrep scan --validate --config .semgrep/tache3.yml
semgrep scan --config .semgrep/tache3.yml --metrics=off --no-git-ignore --json --output tache3-results/introduced/custom.json tika-core/src/main/java
python scripts/ift3913_tache3/compare_semgrep.py tache3-results/baseline/custom.json tache3-results/introduced/custom.json
```

La dernière commande doit sortir avec le code **1** et afficher sept lignes `NEW`. Pour reproduire le contrôle sans régression, comparer le fichier de référence avec lui-même : le code de sortie doit être **0**. Les rapports du pack public se reproduisent avec `semgrep scan --config p/java --metrics=off --no-git-ignore --json --output tache3-results/introduced/semgrep.json tika-core/src/main/java` (la version distante du pack peut évoluer).

**Assistance IA :** Codex a aidé à lire les consignes, rédiger les règles et le workflow, construire les exemples et préparer ce rapport. Les chiffres ci-dessus proviennent des commandes locales réellement exécutées. Le workflow et les scans Semgrep n'appellent aucune API payante ni aucun modèle d'IA.
