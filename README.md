# nomi

**make something delicious.** Cook smarter and waste less with a little help from your pantry.

This Java web app follows a four-step cooking workflow: create recipes, review each recipe's ingredients, manage pantry stock, and add missing ingredients to a consolidated shopping list. Recipes are the home page, sorted by pantry availability; pantry inventory has its own page, and recipe ingredients show which items are already in stock. Choose a category for each ingredient as you add a recipe. When favoriting a recipe, nomi carries those categories into the shopping list for its missing ingredients; the recipe ingredients page also lets you add missing items directly. Checking an item off adds its quantity to the pantry. Shopping-list items can be removed.

## Accounts

Create an account with your name, email, and a password of at least 8 characters, or sign in with an existing account. Passwords are stored as BCrypt hashes. Pantry ingredients, recipes, favorites, and shopping lists are kept separate for each account. Sign out from the profile area in the app navigation.

## Run locally

Requires Java 17 and Maven.

```sh
mvn spring-boot:run
```

Then open [http://localhost:8080](http://localhost:8080) and create an account.

The pantry starts empty; add ingredients as you stock your kitchen. By default, accounts and app data are stored locally in the ignored `data/` directory and survive app restarts. When PostgreSQL is configured with `DATABASE_URL`, `POSTGRES_URL`, or `SPRING_DATASOURCE_URL`, accounts and app data are stored in the database instead. Existing shared data is not automatically assigned to a new account.
