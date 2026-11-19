# nomi

**make something delicious.** Cook smarter and waste less with a little help from your pantry.

This Java web app follows a four-step cooking workflow: create recipes, review each recipe's ingredients, manage pantry stock, and add missing ingredients to a consolidated shopping list. Recipes are the home page, sorted by pantry availability; pantry inventory has its own page, and recipe ingredients show which items are already in stock. Choose a category for each ingredient as you add a recipe. When favoriting a recipe, nomi carries those categories into the shopping list for its missing ingredients; the recipe ingredients page also lets you add missing items directly. Checking an item off adds its quantity to the pantry. Shopping-list items can be removed.

## Run locally

Requires Java 17 and Maven. With Homebrew:

```sh
brew install openjdk@17 maven
```

```sh
mvn spring-boot:run
```

Then open [http://localhost:8080](http://localhost:8080).

Pantry, recipes, favorites, and shopping-list data are stored locally in the ignored `data/` directory and survive app restarts.

## Deploy on Vercel

The root `Dockerfile` packages nomi for Vercel's Container runtime (currently in beta). Connect a PostgreSQL provider from the Vercel Marketplace (for example, Neon) and make sure `DATABASE_URL` or `POSTGRES_URL` is available to the deployment. Nomi stores its pantry, recipes, and shopping list as JSON documents in PostgreSQL; if a matching local JSON file exists when a document is first read, it is imported automatically. Without a database URL, local development continues to use the `data/` directory.

Set the Vercel project to the **Container** framework if it is not selected automatically. This version has no sign-in, so everyone who can access a deployment shares the same pantry, recipes, and shopping list; keep it private until authentication is added.
