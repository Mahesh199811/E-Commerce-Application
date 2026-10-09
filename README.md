# Fieldnote Supply

A small React and Java 17 e-commerce demo built as a three-tier application.

## Develop locally

You need Java 17 or later and Node.js/npm. Start the Java API in one terminal:

```sh
./run.sh
```

In another terminal, start the React development server:

```sh
cd frontend
npm ci
npm run dev
```

Open the Vite URL shown in the terminal, usually [http://localhost:5173](http://localhost:5173). Vite forwards `/api` requests to the Java API on port 8080.

To serve the compiled React app from Java on port 8080 instead, build it with `cd frontend && npm ci && npm run build`, then run `./run.sh`.

## Run with Docker Compose

Compose starts three services: the React/Vite frontend, Java API backend, and MySQL database. Products, inventory, and orders persist in a Docker volume. From the project root, run:

```sh
docker compose up --build
```

Open [http://localhost:5174](http://localhost:5174). The frontend forwards API requests to the backend, which stores data in MySQL over the private Compose network. If port 5174 is busy, choose another host port:

```sh
APP_PORT=5175 docker compose up --build
```

Stop the services with `Ctrl+C`, then remove their containers with `docker compose down`.

The Compose file uses demo-only database passwords by default. Set `MYSQL_PASSWORD` and `MYSQL_ROOT_PASSWORD` in your shell or a local `.env` file before using this outside a local demo. `docker compose down -v` also deletes the database volume and all stored data.

## Tiers

- Presentation: `frontend/src/main.jsx` contains the React storefront; the Vite frontend proxies requests to the Java API.
- API: `src/main/java/com/fieldnote/store/presentation/StoreServer.java` serves the HTTP API.
- Service: catalog and checkout validation live in `src/main/java/com/fieldnote/store/service/`.
- Data: `src/main/java/com/fieldnote/store/repository/MySqlStoreRepository.java` stores products, inventory, and orders in MySQL. The `StoreRepository` interface also has an in-memory implementation for local Java runs without `DB_URL`.

The first database startup seeds the catalog; orders and inventory persist across container restarts. Running `./run.sh` without `DB_URL` uses in-memory storage for local development, so data resets when that process stops. Product photography loads from Unsplash, and the typefaces load from Google Fonts.