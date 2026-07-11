package app.diaita.data.seed

import app.diaita.data.db.entities.FoodItem
import app.diaita.data.db.entities.FoodSource

/**
 * ~95 common foods with per-100g macros, shipped in-app so search works fully offline
 * on a fresh install before any OpenFoodFacts lookups have happened (Phase 3).
 * Values are widely accepted approximate nutrition figures, not medical-grade data.
 */
object SeedFoods {

    private fun food(name: String, kcal: Double, protein: Double, fat: Double, carbs: Double) =
        FoodItem(
            name = name,
            caloriesPer100g = kcal,
            proteinPer100g = protein,
            fatPer100g = fat,
            carbsPer100g = carbs,
            source = FoodSource.SEED,
        )

    val all: List<FoodItem> = listOf(
        // Grains & starches
        food("White rice, cooked", 130.0, 2.7, 0.3, 28.0),
        food("Brown rice, cooked", 123.0, 2.6, 1.0, 26.0),
        food("Pasta, cooked", 131.0, 5.0, 1.1, 25.0),
        food("White bread", 265.0, 9.0, 3.2, 49.0),
        food("Whole wheat bread", 247.0, 13.0, 3.4, 41.0),
        food("Oats, dry", 389.0, 16.9, 6.9, 66.0),
        food("Quinoa, cooked", 120.0, 4.4, 1.9, 21.0),
        food("Potato, boiled", 87.0, 1.9, 0.1, 20.0),
        food("Sweet potato, boiled", 90.0, 2.0, 0.1, 21.0),
        food("Couscous, cooked", 112.0, 3.8, 0.2, 23.0),
        food("Buckwheat, cooked", 92.0, 3.4, 0.6, 20.0),
        food("Corn, boiled", 96.0, 3.4, 1.5, 21.0),
        food("Bagel", 250.0, 10.0, 1.5, 49.0),
        food("Tortilla, flour", 218.0, 6.0, 4.5, 36.0),
        food("Rice noodles, cooked", 109.0, 0.9, 0.2, 25.0),

        // Meat, fish, eggs, dairy
        food("Chicken breast, cooked", 165.0, 31.0, 3.6, 0.0),
        food("Chicken thigh, cooked", 209.0, 26.0, 10.9, 0.0),
        food("Beef, lean ground, cooked", 250.0, 26.0, 15.0, 0.0),
        food("Beef steak, cooked", 271.0, 25.0, 19.0, 0.0),
        food("Pork chop, cooked", 231.0, 27.0, 13.0, 0.0),
        food("Salmon, cooked", 208.0, 20.0, 13.0, 0.0),
        food("Tuna, canned in water", 116.0, 26.0, 0.8, 0.0),
        food("Shrimp, cooked", 99.0, 24.0, 0.3, 0.2),
        food("Cod, cooked", 105.0, 23.0, 0.9, 0.0),
        food("Egg, whole", 155.0, 13.0, 11.0, 1.1),
        food("Egg white", 52.0, 11.0, 0.2, 0.7),
        food("Turkey breast, cooked", 135.0, 30.0, 1.0, 0.0),
        food("Bacon, cooked", 541.0, 37.0, 42.0, 1.4),
        food("Ham", 145.0, 21.0, 5.5, 1.5),
        food("Milk, whole", 61.0, 3.2, 3.3, 4.8),
        food("Milk, skim", 34.0, 3.4, 0.1, 5.0),
        food("Greek yogurt, plain", 59.0, 10.0, 0.4, 3.6),
        food("Yogurt, plain whole milk", 61.0, 3.5, 3.3, 4.7),
        food("Cheddar cheese", 402.0, 25.0, 33.0, 1.3),
        food("Mozzarella cheese", 280.0, 28.0, 17.0, 3.1),
        food("Cottage cheese", 98.0, 11.0, 4.3, 3.4),
        food("Butter", 717.0, 0.9, 81.0, 0.1),
        food("Tofu, firm", 144.0, 15.8, 8.7, 3.9),
        food("Tempeh", 192.0, 20.0, 11.0, 8.0),

        // Fruits
        food("Banana", 89.0, 1.1, 0.3, 23.0),
        food("Apple", 52.0, 0.3, 0.2, 14.0),
        food("Orange", 47.0, 0.9, 0.1, 12.0),
        food("Strawberries", 32.0, 0.7, 0.3, 8.0),
        food("Blueberries", 57.0, 0.7, 0.3, 14.0),
        food("Grapes", 69.0, 0.7, 0.2, 18.0),
        food("Watermelon", 30.0, 0.6, 0.2, 8.0),
        food("Pineapple", 50.0, 0.5, 0.1, 13.0),
        food("Mango", 60.0, 0.8, 0.4, 15.0),
        food("Avocado", 160.0, 2.0, 15.0, 9.0),
        food("Pear", 57.0, 0.4, 0.1, 15.0),
        food("Peach", 39.0, 0.9, 0.3, 10.0),
        food("Kiwi", 61.0, 1.1, 0.5, 15.0),
        food("Raisins", 299.0, 3.1, 0.5, 79.0),

        // Vegetables
        food("Broccoli, raw", 34.0, 2.8, 0.4, 7.0),
        food("Spinach, raw", 23.0, 2.9, 0.4, 3.6),
        food("Carrot, raw", 41.0, 0.9, 0.2, 10.0),
        food("Tomato", 18.0, 0.9, 0.2, 3.9),
        food("Cucumber", 15.0, 0.7, 0.1, 3.6),
        food("Bell pepper", 31.0, 1.0, 0.3, 6.0),
        food("Onion", 40.0, 1.1, 0.1, 9.0),
        food("Garlic", 149.0, 6.4, 0.5, 33.0),
        food("Lettuce", 15.0, 1.4, 0.2, 2.9),
        food("Zucchini", 17.0, 1.2, 0.3, 3.1),
        food("Mushrooms", 22.0, 3.1, 0.3, 3.3),
        food("Green beans", 31.0, 1.8, 0.2, 7.0),
        food("Cauliflower", 25.0, 1.9, 0.3, 5.0),
        food("Cabbage", 25.0, 1.3, 0.1, 6.0),
        food("Sweet corn, canned", 76.0, 2.5, 0.8, 17.0),

        // Legumes & nuts
        food("Black beans, cooked", 132.0, 8.9, 0.5, 24.0),
        food("Chickpeas, cooked", 164.0, 8.9, 2.6, 27.0),
        food("Lentils, cooked", 116.0, 9.0, 0.4, 20.0),
        food("Kidney beans, cooked", 127.0, 8.7, 0.5, 23.0),
        food("Peanuts", 567.0, 26.0, 49.0, 16.0),
        food("Almonds", 579.0, 21.0, 50.0, 22.0),
        food("Walnuts", 654.0, 15.0, 65.0, 14.0),
        food("Cashews", 553.0, 18.0, 44.0, 30.0),
        food("Peanut butter", 588.0, 25.0, 50.0, 20.0),
        food("Chia seeds", 486.0, 17.0, 31.0, 42.0),

        // Fats & oils
        food("Olive oil", 884.0, 0.0, 100.0, 0.0),
        food("Vegetable oil", 884.0, 0.0, 100.0, 0.0),
        food("Mayonnaise", 680.0, 1.0, 75.0, 0.6),

        // Prepared foods & misc
        food("Pizza, cheese slice", 266.0, 11.0, 10.0, 33.0),
        food("Hamburger, fast food", 250.0, 12.0, 9.0, 30.0),
        food("French fries", 312.0, 3.4, 15.0, 41.0),
        food("White sugar", 387.0, 0.0, 0.0, 100.0),
        food("Honey", 304.0, 0.3, 0.0, 82.0),
        food("Ketchup", 101.0, 1.3, 0.1, 27.0),
        food("Dark chocolate", 546.0, 4.9, 31.0, 61.0),
        food("Milk chocolate", 535.0, 7.6, 30.0, 59.0),
        food("Potato chips", 536.0, 7.0, 34.0, 53.0),
        food("Popcorn, air-popped", 387.0, 13.0, 4.5, 78.0),
        food("Ice cream, vanilla", 207.0, 3.5, 11.0, 24.0),
        food("Soda, cola", 41.0, 0.0, 0.0, 10.6),
        food("Orange juice", 45.0, 0.7, 0.2, 10.4),
        food("Coffee, black", 1.0, 0.1, 0.0, 0.0),
        food("Beer", 43.0, 0.5, 0.0, 3.6),
    )
}
