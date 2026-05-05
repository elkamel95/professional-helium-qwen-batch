# CLAUDE.md — Règles strictes de traçabilité des modifications de code

> Ce fichier impose des règles obligatoires pour toute modification apportée au projet.
> Aucune modification de code ne peut être effectuée sans respecter ce format.

---

## ⚠️ Règle fondamentale

**Toute modification doit être justifiée, tracée et documentée.**
Il est strictement interdit de répondre avec uniquement du code sans explication.
Chaque réponse doit suivre le format standard défini dans ce fichier.

---

## 📋 Format standard obligatoire pour toute modification

### 1. 🏷️ Type de modification
Préciser obligatoirement l'une des catégories suivantes :

| Type | Description |
|------|-------------|
| `feature` | Nouvelle fonctionnalité |
| `bug-fix` | Correction d'un bug |
| `refactor` | Restructuration sans changement fonctionnel |
| `performance` | Optimisation des performances |
| `security` | Correction ou amélioration de sécurité |
| `config` | Modification de configuration |
| `test` | Ajout ou modification de tests |
| `doc` | Documentation uniquement |

---

### 2. 📁 Fichiers modifiés
Lister tous les fichiers impactés :

```
Fichiers modifiés :
- src/main/java/com/example/productai/batch/BatchConfig.java
- src/main/java/com/example/productai/batch/ProductAiProcessor.java
- src/main/resources/application.yml
```

---

### 3. 💬 Explication de la modification
Répondre obligatoirement aux questions :
- **Pourquoi** cette modification est-elle nécessaire ?
- **Quel problème** résout-elle ou quelle fonctionnalité apporte-t-elle ?
- **Quelle alternative** a été envisagée ?

---

### 4. 🔄 Code AVANT / APRÈS

Toujours afficher le code dans ce format :

**AVANT :**
```java
// Code original — état avant modification
@Bean
public Step analyzeHeliumKeywordsStep(...) {
    return new StepBuilder("analyzeHeliumKeywordsStep", jobRepository)
            .<HeliumKeywordRow, ProductAnalysisResult>chunk(10, transactionManager)
            .reader(heliumReader)
            .processor(processor)
            .writer(writer)
            .build();
}
```

**APRÈS :**
```java
// Code modifié — état après modification
@Bean
public Step analyzeHeliumKeywordsStep(...) {
    return new StepBuilder("analyzeHeliumKeywordsStep", jobRepository)
            .<HeliumKeywordRow, ProductAnalysisResult>chunk(chunkSize, transactionManager)
            .reader(heliumReader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            .skipLimit(1000)
            .skip(Exception.class)
            .build();
}
```

---

### 5. 📊 Impact de la modification

Toujours renseigner les trois dimensions :

#### Impact fonctionnel
> Décrit ce que l'utilisateur ou le système voit différemment.
> Ex : "Le batch tolère désormais jusqu'à 1000 erreurs sans s'arrêter."

#### Risques potentiels
> Décrit les effets secondaires possibles.
> Ex : "Des lignes en erreur sont ignorées silencieusement si skipLimit est atteint."

#### Effets de bord
> Décrit les impacts sur d'autres composants.
> Ex : "Le Writer ne recevra pas les lignes skippées — vérifier les logs de suivi."

---

## 🏗️ Règles spécifiques Spring Batch

### Job / Step

- Toujours nommer les Jobs et Steps de manière explicite et unique.
- Ne jamais modifier un Job existant sans vérifier la compatibilité avec les `JobInstance` existantes en base.
- Tout changement de `chunkSize` doit être justifié par des métriques (temps, mémoire).
- Documenter les transitions entre Steps (`on("FAILED").to(...)`, etc.).

```java
// OBLIGATOIRE : nom descriptif + gestion des erreurs
@Bean
public Job heliumKeywordAnalysisJob(JobRepository jobRepository, Step step) {
    return new JobBuilder("heliumKeywordAnalysisJob", jobRepository)
            .start(step)
            .on("FAILED").end()   // documenter chaque transition
            .end()
            .build();
}
```

---

### Reader

- Toujours préciser l'encodage des fichiers lus (`UTF-8` par défaut).
- Documenter le format du fichier source (CSV, JSON, XML) et sa structure.
- Ne jamais ignorer les lignes d'en-tête sans commentaire explicite.
- Tout changement de `FlatFileItemReader` ou `JdbcCursorItemReader` doit préciser l'impact sur la mémoire.

```java
// OBLIGATOIRE : préciser encoding + linesToSkip
reader.setEncoding("UTF-8");
reader.setLinesToSkip(1); // skip header line — colonne : [keyword, volume, asin]
```

---

### Processor

- Chaque `ItemProcessor` doit avoir une responsabilité unique (principe SRP).
- Documenter le cas `return null` (signifie que l'item est filtré/ignoré).
- Toute transformation métier doit être couverte par un test unitaire.
- Les appels à des APIs externes (ex : Qwen LLM) doivent être protégés par un timeout et un retry.

```java
// OBLIGATOIRE : documenter le cas null
@Override
public ProductAnalysisResult process(HeliumKeywordRow item) {
    if (item.getKeyword() == null || item.getKeyword().isBlank()) {
        return null; // item filtré — ne sera pas transmis au Writer
    }
    // ...
}
```

---

### Writer

- Toujours gérer les erreurs d'écriture explicitement.
- Documenter la destination des données (fichier, BDD, API, logs).
- Ne jamais committer dans le Writer sans transaction explicite.
- Préciser le comportement en cas d'échec partiel.

```java
// OBLIGATOIRE : documenter destination + comportement d'erreur
@Override
public void write(Chunk<? extends ProductAnalysisResult> items) {
    // Destination : fichier CSV de sortie dans /output/results/
    // En cas d'erreur : log + skip (géré par faultTolerant au niveau du Step)
    items.forEach(item -> outputWriter.writeLine(item.toCsvLine()));
}
```

---

### Transactions

- Toujours définir explicitement le niveau d'isolation si différent du défaut.
- Ne jamais effectuer d'appel réseau (HTTP, API externe) dans une transaction.
- Documenter la propagation (`REQUIRED`, `REQUIRES_NEW`, etc.).
- Toute modification du `PlatformTransactionManager` doit être accompagnée d'un test d'intégration.

```java
// OBLIGATOIRE : documenter propagation + isolation
@Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
public void saveResult(ProductAnalysisResult result) {
    // Nouvelle transaction pour isoler chaque écriture
    repository.save(result);
}
```

---

### Performance SQL

- Toujours expliquer pourquoi une requête a été modifiée (index manquant, N+1, etc.).
- Toujours afficher le plan d'exécution AVANT/APRÈS (`EXPLAIN ANALYZE`).
- Toute requête en boucle est interdite — utiliser le batch insert/update.
- Documenter le volume de données attendu pour chaque requête.

```java
// INTERDIT : requête dans une boucle
for (ProductAnalysisResult result : results) {
    repository.save(result); // N requêtes SQL !
}

// OBLIGATOIRE : batch insert
repository.saveAll(results); // 1 seule requête SQL
```

---

## ✅ Bonnes pratiques

### Code

- Respecter le principe **SOLID** — chaque classe a une responsabilité unique.
- Pas de magic numbers — toujours utiliser des constantes nommées.
- Préférer l'immutabilité (`final`, records Java) quand possible.
- Toujours logger avec le niveau approprié (`INFO` pour le flux normal, `WARN` pour les cas dégradés, `ERROR` pour les échecs).

```java
// INTERDIT
int chunkSize = 50; // magic number

// OBLIGATOIRE
private static final int DEFAULT_CHUNK_SIZE = 50;
```

### Git

- Une branche par feature/bug : `feature/SCRUM-XX-description` ou `fix/SCRUM-XX-description`.
- Commit atomique : un commit = un changement logique.
- Message de commit en anglais, format : `type(scope): description` (ex: `feat(batch): add retry on processor`).
- Toute PR doit référencer le ticket Jira correspondant.

### Configuration

- Ne jamais hardcoder de valeurs sensibles (tokens, passwords, URLs).
- Toujours utiliser `application.yml` ou variables d'environnement.
- Documenter chaque propriété dans `application.yml` avec un commentaire.

```yaml
app:
  chunk-size: 50        # Nombre d'items traités par transaction batch
  retry-max-attempts: 3 # Nombre de tentatives max pour les appels Qwen LLM
  input-dir: /input     # Répertoire de surveillance des fichiers entrants
```

---

## 🧪 Tests et validation

### Règles obligatoires

- Toute nouvelle méthode publique doit avoir au moins un test unitaire.
- Toute modification d'un `Processor` ou `Writer` doit être couverte par un test.
- Les tests doivent couvrir : le cas nominal, les cas limites, les cas d'erreur.
- Utiliser `@WebFluxTest` pour les controllers, `@SpringBatchTest` pour les jobs.

### Structure des tests

```java
@DisplayName("Description claire du comportement testé")
@Test
void should_[résultat_attendu]_when_[condition]() {
    // GIVEN — état initial
    HeliumKeywordRow input = new HeliumKeywordRow("wireless headphones", 12000);

    // WHEN — action testée
    ProductAnalysisResult result = processor.process(input);

    // THEN — vérification
    assertThat(result).isNotNull();
    assertThat(result.getDescription()).isNotBlank();
}
```

### Checklist avant toute PR

- [ ] Tests unitaires ajoutés ou mis à jour
- [ ] Aucune régression sur les tests existants (`mvn test`)
- [ ] Code AVANT/APRÈS documenté dans la PR
- [ ] Impact fonctionnel, risques et effets de bord décrits
- [ ] Ticket Jira référencé dans le commit et la PR
- [ ] Secrets et tokens absents du code versionné
- [ ] Swagger/Javadoc mis à jour si endpoint modifié

---

## 📌 Exemple complet de modification tracée

### Type
`performance`

### Fichiers modifiés
```
- src/main/java/com/example/productai/batch/BatchConfig.java
- src/main/resources/application.yml
```

### Explication
Le `chunkSize` était fixé à `10` en dur dans le code, ce qui causait des performances dégradées sur des fichiers de plus de 10 000 lignes. La valeur est externalisée dans `application.yml` pour permettre un ajustement sans recompilation.

### AVANT
```java
.<HeliumKeywordRow, ProductAnalysisResult>chunk(10, transactionManager)
```

### APRÈS
```java
.<HeliumKeywordRow, ProductAnalysisResult>chunk(chunkSize, transactionManager)
// chunkSize injecté via @Value("${app.chunk-size}")
```

### Impact
- **Fonctionnel** : Le chunkSize est configurable sans recompilation.
- **Risques** : Une valeur trop élevée peut saturer la mémoire JVM.
- **Effets de bord** : Modifier `app.chunk-size` en production nécessite un redémarrage du conteneur Docker.

---

*Ce fichier est la référence absolue pour toute contribution au projet.*
*Toute modification non conforme à ces règles sera refusée en code review.*
