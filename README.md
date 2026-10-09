# Fieldnote Supply

A small, dependency-free Java 17 e-commerce demo built as a three-tier application.

## Run

```sh
./run.sh
```

Open [http://localhost:8080](http://localhost:8080). To use a different port, pass it to the script, for example `./run.sh 9090`.

## Tiers

- Presentation: `presentation/StoreServer.java` serves the storefront and HTTP API; `public/` contains the browser UI.
- Service: catalog and checkout validation live in `service/`.
- Data: `repository/StoreRepository.java` provides the seeded product catalog, inventory, and in-memory orders.

Orders and inventory reset when the server restarts. Product photography loads from Unsplash, and the typefaces load from Google Fonts.