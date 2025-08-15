package com.example.wheremythings;

import java.util.*;

public class NlpExtractor {

    private static final Set<String> CATEGORY_SET = new HashSet<>(Arrays.asList(
            "wallet", "backpack","cat", "dog"
    ));

    private static final Set<String> COLOR_SET = new HashSet<>(Arrays.asList(
            "black", "white", "red", "blue", "green", "yellow", "orange",
            "pink", "purple", "brown", "gray", "grey"
    ));

    private static final Map<String, List<String>> COLOR_SIMILARITY_MAP = new HashMap<>();
    static {
        COLOR_SIMILARITY_MAP.put("black", Arrays.asList("dark gray", "dark grey", "charcoal"));
        COLOR_SIMILARITY_MAP.put("blue", Arrays.asList("navy", "sky blue", "light blue"));
        COLOR_SIMILARITY_MAP.put("red", Arrays.asList("maroon", "scarlet", "burgundy"));
        COLOR_SIMILARITY_MAP.put("green", Arrays.asList("lime", "olive", "mint"));
        COLOR_SIMILARITY_MAP.put("yellow", Arrays.asList("gold", "mustard"));
        COLOR_SIMILARITY_MAP.put("orange", Arrays.asList("amber", "peach"));
        COLOR_SIMILARITY_MAP.put("purple", Arrays.asList("violet", "lavender"));
        COLOR_SIMILARITY_MAP.put("gray", Arrays.asList("grey", "silver", "ash"));
    }

    private static final List<String> LOCATION_KEYWORDS = Arrays.asList(
            "station", "school", "park", "library", "mall", "market",
            "subway", "metro", "airport", "bus station", "restaurant", "hospital"
    );

    private NlpExtractor() {
        // no instances
    }

    public static Map<String, String> extract(String description) {
        Map<String, String> result = new HashMap<>();
        if (description == null) {
            result.put("category", null);
            result.put("color", null);
            result.put("location", null);
            return result;
        }
        String lower = description.toLowerCase(Locale.US);

        // Extract category
        String foundCategory = null;
        for (String cat : CATEGORY_SET) {
            if (lower.contains(cat)) {
                foundCategory = cat;
                break;
            }
        }
        result.put("category", foundCategory);

        // Extract colour (resolve synonyms to base colour)
        String foundColor = null;
        for (String colour : COLOR_SET) {
            if (lower.contains(colour)) {
                foundColor = colour;
                break;
            }
            List<String> synonyms = COLOR_SIMILARITY_MAP.get(colour);
            if (synonyms != null) {
                for (String syn : synonyms) {
                    if (lower.contains(syn)) {
                        foundColor = colour;
                        break;
                    }
                }
            }
            if (foundColor != null) {
                break;
            }
        }
        result.put("color", foundColor);

        String foundLocation = null;
        for (String loc : LOCATION_KEYWORDS) {
            if (lower.contains(loc)) {
                foundLocation = loc;
                break;
            }
        }
        result.put("location", foundLocation);

        return result;
    }

    public static boolean isSimilarColor(String colorA, String colorB) {
        if (colorA == null || colorB == null) {
            return false;
        }
        if (colorA.equals(colorB)) {
            return true;
        }
        List<String> listA = COLOR_SIMILARITY_MAP.get(colorA);
        if (listA != null && listA.contains(colorB)) {
            return true;
        }
        List<String> listB = COLOR_SIMILARITY_MAP.get(colorB);
        return listB != null && listB.contains(colorA);
    }
}