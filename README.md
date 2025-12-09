### Auteurs

- Valentin CHAUD et Jordan BAUMARD
- Outils utilisés : Gemini

### Choix de conception (écarts et raisons)

- Compilation uniquement si le type de l’expression est `INT`, sauf si l’expression top‑level est une fonction. Raison: permettre les tests avec des fonctions au niveau racine tout en gardant une sortie exécutable simple.
- Prélude WAT: chargement prioritaire de `test/am.wat`. Suppression d’un bloc `(table ...)` existant du prélude. Raison: éviter une double table et faciliter le remplacement du prélude.
- Fermetures: indices stables et emplacement `_self` pour aligner l’environnement avec `$apply`. Raison: correspondre à la convention d’extension d’environnement côté WAT.
- Vérification: comparaison stricte des résultats seulement pour les entiers; on ne compare pas les fonctions. Raison: l’égalité de fonctions n’est pas définie dans le cadre des tests.

### Ce qui marche

- Analyse, typage et évaluation: nombres, `+ - * /`, `ifz`, `let`, variables, fonctions, récursion (`fix`).
- Génération WAT pour ces constructions; écriture automatique du `.wat` associé à un `.pcf`.
- Mode vérification AM: exécution de l’AM et comparaison avec l’interpréteur (option `-vm`).
- Tests WABT: la chaîne `wat2wasm` + `wasm-interp` est supportée via les scripts de test fournis.

### Ce qui ne marche pas ou manque

- On dépend de `src/test/am.wat`.
- Le WAT généré suppose des symboles du prélude: `$ENV`, `$ACC`, `$apply`, `$search`, `$cons`, `$pair`. Si le prélude diffère, l’exécution peut échouer.
- Couverture WAT incomplète pour des instructions non utilisées (ex. `Ret` n’est pas émis côté WAT car le retour est géré par la structure des fonctions).
- Pré‑requis externes: nécessite WABT (`wat2wasm`, `wasm-interp`) installés et dans le `PATH`.

### Comment corriger / compléter

- Ajouter un prélude propre: créer `wat/prelude.wat` compatible (déclarer `$ENV`, `$ACC`, `$apply`, `$search`, `$cons`, `$pair`, mémoire, globals). Cela rend le build indépendant des fichiers de test.
- Si le prélude change, aligner les appels dans `src/generator/Generator.scala` (émission de `call $...`) et, si besoin, ajuster la logique de sauvegarde/restauration (`$ACC`, `$ENV`).
- Étendre la génération WAT si de nouvelles instructions sont ajoutées: compléter `emitIns` et la mise en forme associée.
- Politique top‑level: si exigé par le sujet, limiter la compilation aux seules expressions de type `INT` (modifier `src/pcf/PCF.scala`).
- Stabiliser les tests: vérifier/adapter `src/test/Test.scala` et s’assurer que WABT est bien installé.
