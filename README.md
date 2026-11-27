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

## Utilisation

### Prérequis

- Java 21 ou plus.
- Jackson (`jackson-databind`) et `jakarta.annotation-api` fournis par le projet, ce que fait déjà Spring Boot.
- Accès à Internet au moment du build, y compris dans un build Docker, pour télécharger le jar depuis JitPack.

### 1. Déclarer le dépôt JitPack

Dans le `pom.xml` du projet, ou dans le `pom.xml` parent d'un projet multi-modules (les modules en héritent) :

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### 2. Ajouter la dépendance

Dans le module qui déclare les endpoints ; les modules qui en dépendent la récupèrent aussi :

```xml
<dependency>
    <groupId>com.github.l3miage-kernoub</groupId>
    <artifactId>partner-jar</artifactId>
    <version>1.0.1</version>
</dependency>
```

La version est le tag Git du dépôt.

### 3. Recharger Maven

Dans IntelliJ : *Reload All Maven Projects*. En ligne de commande : `mvn compile` (ou `./mvnw compile`).

Le jar `partner-jar-1.0.1.jar` apparaît alors dans les dépendances du projet.

### 4. Exposer l'API (boutique vendeur d'origine)

La boutique expose les quatre routes du contrat sous `/api/partner` et traduit son propre modèle vers les DTO.

Java :

```java
@RestController
@RequestMapping(PartnerApiPaths.BASE)
public class PartnerController {

    @GetMapping("/products")
    public List<ProductDto> getProducts() { ... }

    @PostMapping("/orders")
    public ResponseEntity<OrderDto> createOrder(@RequestBody OrderRequestDto request) { ... }

    @GetMapping("/orders/{id}")
    public OrderDto getOrder(@PathVariable String id) { ... }

    @PostMapping("/orders/{id}/cancel")
    public OrderDto cancelOrder(@PathVariable String id) { ... }
}
```

Kotlin :

```kotlin
@RequestMapping(PartnerApiPaths.BASE)
interface PartnerEndpoints {
    @GetMapping("/products")
    fun getProducts(): List<ProductDto>

    @PostMapping("/orders")
    fun createOrder(@RequestBody request: OrderRequestDto): ResponseEntity<OrderDto>

    @GetMapping("/orders/{id}")
    fun getOrder(@PathVariable id: String): OrderDto

    @PostMapping("/orders/{id}/cancel")
    fun cancelOrder(@PathVariable id: String): OrderDto
}
```

Les DTO se construisent avec leurs méthodes chaînées :

```kotlin
ProductDto()
    .id(produit.id.toString())
    .name(produit.nom)
    .price(produit.prix)
    .currency(CurrencyDto.EUR)
    .availableQuantity(stock)
```

Toute erreur renvoie un `ErrorDto` : `ErrorDto().message("Stock insuffisant")`.

### 5. Appeler un partenaire (boutique vitrine)

Avec le `RestClient` de Spring (le contrat fixe un délai d'attente de 3 secondes par appel, à configurer sur le client) :

```java
ProductDto[] produits = restClient.get()
        .uri(urlPartenaire + PartnerApiPaths.PRODUCTS)
        .retrieve()
        .body(ProductDto[].class);

OrderDto commande = restClient.post()
        .uri(urlPartenaire + PartnerApiPaths.ORDERS)
        .body(new OrderRequestDto()
                .reference("LAB-128")
                .addLinesItem(new OrderLineRequestDto().productId("42").quantity(2).giftWrap(true))
                .delivery(new DeliveryDto()
                        .recipientName("Jean Dupont")
                        .address("5 avenue Alsace-Lorraine")
                        .postalCode("38000")
                        .city("Grenoble")
                        .country("France"))
                .options(new OptionsDto().express(false)))
        .retrieve()
        .body(OrderDto.class);
```

Les chemins avec identifiant s'utilisent avec les variables d'URI : `.uri(urlPartenaire + PartnerApiPaths.ORDER_CANCEL, id)`.

### Lire le contrat depuis le jar

Le yaml est inclus dans le jar :

```java
InputStream contrat = PartnerApiPaths.class.getResourceAsStream(PartnerApiPaths.CONTRACT_RESOURCE);
```

### Sans Java

Les équipes qui ne sont pas en Java utilisent directement [`contrat-partenaires.yaml`](src/main/resources/contrat-partenaires.yaml), à la main ou avec un générateur OpenAPI pour leur langage.

## Publier une nouvelle version

1. Modifier `contrat-partenaires.yaml` (et `info.version` si le contrat change), après accord des équipes.
2. Mettre la version du futur tag dans `<version>` du `pom.xml`.
3. Vérifier le build : `./mvnw package`.
4. Pousser, puis créer le tag :

   ```
   git tag 1.0.2
   git push origin 1.0.2
   ```

5. Sur [jitpack.io](https://jitpack.io), chercher le dépôt et cliquer sur *Get it* pour lancer le build de la version.

JitPack compile en Java 21 (`jitpack.yml`). Le dépôt doit être public.
