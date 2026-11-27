# partner-jar : contrat partenaires MarketHub

Contrat commun de l'API `/api/partner` entre les boutiques partenaires de l'itération 3, et les DTO Java générés à partir de ce contrat.

- **Le contrat qui fait foi** : [`src/main/resources/contrat-partenaires.yaml`](src/main/resources/contrat-partenaires.yaml) (OpenAPI 3.0.3).
- **Les DTO** sont générés à partir du yaml à chaque build (`openapi-generator-maven-plugin`). On ne les écrit pas à la main : pour changer le contrat, on modifie le yaml et on publie une nouvelle version.

## Contenu du jar

| Élément | Paquet ou chemin |
|---|---|
| DTO du contrat | `fr.uga.miage.partner.model` |
| Chemins de l'API | `fr.uga.miage.partner.PartnerApiPaths` |
| Contrat OpenAPI | `/contrat-partenaires.yaml` (ressource du classpath) |

DTO disponibles :

| DTO | Usage |
|---|---|
| `ProductDto` | Élément de la réponse de `GET /products` |
| `OrderRequestDto` | Corps de `POST /orders` |
| `OrderLineRequestDto` | Ligne d'une commande envoyée |
| `DeliveryDto` | Adresse de livraison |
| `OptionsDto` | Options de toute la commande (`express`) |
| `OrderDto` | Réponse de `POST /orders`, `GET /orders/{id}` et `POST /orders/{id}/cancel` |
| `OrderLineDto` | Ligne d'une commande renvoyée |
| `OrderStatusDto` | `VALIDATED`, `IN_PREPARATION`, `SHIPPED`, `DELIVERED`, `CANCELLED` |
| `CurrencyDto` | `EUR` |
| `ErrorDto` | Corps de toute réponse d'erreur : `{ "message": "..." }` |

Le jar n'apporte que les DTO : chaque boutique écrit ses propres contrôleurs, son client HTTP et la traduction vers son modèle.

## Utilisation (Maven, via JitPack)

Dans le `pom.xml` de la boutique :

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.COMPTE_GITHUB</groupId>
    <artifactId>partner-jar</artifactId>
    <version>1.0.0</version>
</dependency>
```

Remplacer `COMPTE_GITHUB` par le compte qui héberge ce dépôt ; la version est le tag Git.

Le projet qui l'utilise doit fournir Jackson (`jackson-databind`) et `jakarta.annotation-api`, ce que fait déjà Spring Boot.

Exemple côté serveur :

```java
@RestController
@RequestMapping(PartnerApiPaths.BASE)
public class PartnerController {

    @GetMapping("/products")
    public List<ProductDto> products() { ... }

    @PostMapping("/orders")
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderRequestDto request) { ... }
}
```

Exemple côté client :

```java
ProductDto[] produits = restClient.get()
        .uri(urlPartenaire + PartnerApiPaths.PRODUCTS)
        .retrieve()
        .body(ProductDto[].class);
```

Les équipes qui ne sont pas en Java utilisent directement le yaml.

## Publier une nouvelle version

1. Modifier `contrat-partenaires.yaml` (et `info.version`), après accord des équipes.
2. Mettre la même version dans `<version>` du `pom.xml`.
3. Vérifier le build : `mvn package`.
4. Pousser, puis créer le tag :

   ```
   git tag 1.0.1
   git push origin 1.0.1
   ```

5. Sur [jitpack.io](https://jitpack.io), chercher le dépôt et cliquer sur *Get it* pour lancer le build de la version.

JitPack compile en Java 21 (`jitpack.yml`). Le dépôt doit être public.
