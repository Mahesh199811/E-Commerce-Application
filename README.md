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

## Run with Docker

Build the image from the project root. The final `.` is the Docker build context:

```sh
docker build -t e-commerce:1.0 .
```

Run it on host port 8081, which avoids conflicting with the local Java server on port 8080:

```sh
docker run --rm --name e-commerce -p 8081:8080 e-commerce:1.0
```

Open [http://localhost:8081](http://localhost:8081). If Docker says the name `e-commerce` is already in use from a failed run, remove that stopped container once with `docker rm e-commerce` and retry.

## Tiers

- Presentation: `frontend/src/main.jsx` contains the React storefront; `src/main/java/com/fieldnote/store/presentation/StoreServer.java` serves the production build and HTTP API.
- Service: catalog and checkout validation live in `src/main/java/com/fieldnote/store/service/`.
- Data: `src/main/java/com/fieldnote/store/repository/StoreRepository.java` provides the seeded product catalog, inventory, and in-memory orders.

Orders and inventory reset when the server restarts. Product photography loads from Unsplash, and the typefaces load from Google Fonts.