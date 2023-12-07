import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class IRoadTrip {
    private Map<String, List<String>> countryBorders; // Store country borders
    private Map<String, String> countryAbbreviations; // Store country abbreviations
    private Map<String, Integer> capitalDistances; // Store capital distances

    public IRoadTrip(String[] args) {
        countryBorders = new HashMap<>();
        countryAbbreviations = new HashMap<>();
        capitalDistances = new HashMap<>();

        // Read and parse borders.txt
        try {
            BufferedReader bordersReader = new BufferedReader(new FileReader(args[0]));
            String line;
            while ((line = bordersReader.readLine()) != null) {
                String[] parts = line.split(" = ");
                String country = parts[0].trim().split("\\(")[0].trim(); // Get initial country name
                String[] borders = parts.length > 1 ? parts[1].split(";") : new String[0];

                List<String> borderingCountries = new ArrayList<>();
                for (String border : borders) {
                    String borderCountry = border.split("\\d")[0].trim().split("\\(")[0].trim(); // Get initial country name
                    borderingCountries.add(borderCountry);
                }
                countryBorders.put(country, borderingCountries);
            }
            bordersReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error reading borders.txt");
            System.exit(1);
        }

        // Read and parse capdist.csv
        try {
            BufferedReader capDistReader = new BufferedReader(new FileReader(args[1]));
            String line;
            boolean headerSkipped = false;
            while ((line = capDistReader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue; // Skip header
                }
                String[] parts = line.split(",");
                String code1 = parts[1];
                String code2 = parts[3];
                int distance = Integer.parseInt(parts[4]);

                capitalDistances.put(code1 + "_" + code2, distance);
                capitalDistances.put(code2 + "_" + code1, distance);
            }
            capDistReader.close();
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            System.err.println("Error reading capdist.csv");
            System.exit(1);
        }

        // Read and parse state_name.tsv
        try {
            BufferedReader stateNameReader = new BufferedReader(new FileReader(args[2]));
            String line;
            boolean headerSkipped = false;
            while ((line = stateNameReader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue; // Skip header
                }
                String[] parts = line.split("\t");
                if (parts.length >= 5 && parts[4].equals("2020-12-31")) { // Check for the latest date
                    String countryId = parts[1];
                    String countryName = parts[2].trim().split("\\(")[0].trim(); // Get initial country name
                    countryAbbreviations.put(countryName, countryId);
                }
            }
            stateNameReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error reading state_name.tsv");
            System.exit(1);
        }
    }

    public int getDistance(String country1, String country2) {
        if (!countryBorders.containsKey(country1) || !countryBorders.containsKey(country2)) {
            return -1; // Country doesn't exist or invalid input
        }

        String abbrevCountry1 = countryAbbreviations.get(country1);
        String abbrevCountry2 = countryAbbreviations.get(country2);

        if (abbrevCountry1 == null || abbrevCountry2 == null) {
            return -1; //  not found for one of the countries
        }

        // Check distances from capitalDistances map
        int distance = capitalDistances.getOrDefault(abbrevCountry1 + "_" + abbrevCountry2, -1);
        if (distance == -1) {
            distance = capitalDistances.getOrDefault(abbrevCountry2 + "_" + abbrevCountry1, -1);
        }

        return distance; // Return found distance or -1 if not found
    }

    public List<String> findPath(String country1, String country2) {
        if (!countryBorders.containsKey(country1) || !countryBorders.containsKey(country2)) {
            return new ArrayList<>(); // Country doesn't exist or invalid input
        }

        PriorityQueue<Map.Entry<String, Integer>> queue = new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> parentMap = new HashMap<>();

        for (String country : countryBorders.keySet()) {
            distances.put(country, Integer.MAX_VALUE);
        }

        queue.add(new AbstractMap.SimpleEntry<>(country1, 0));
        distances.put(country1, 0);

        while (!queue.isEmpty()) {
            Map.Entry<String, Integer> current = queue.poll();
            String currentCountry = current.getKey();
            int currentDistance = current.getValue();

            if (currentCountry.equals(country2)) {
                break;
            }

            List<String> neighbors = countryBorders.get(currentCountry);
            if (neighbors != null) {
                for (String neighbor : neighbors) {
                    int distanceToNeighbor = getDistance(currentCountry, neighbor);

                    if (distanceToNeighbor != -1) { // Check if distance is valid (-1 indicates unknown)
                        int totalDistance = currentDistance + distanceToNeighbor;

                        if (totalDistance < distances.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                            distances.put(neighbor, totalDistance);
                            parentMap.put(neighbor, currentCountry);
                            queue.add(new AbstractMap.SimpleEntry<>(neighbor, totalDistance));
                        }
                    }
                }
            }
        }

        List<String> path = new ArrayList<>();
        String country = country2;
        while (country != null) {
            String parent = parentMap.get(country);
            if (parent != null) {
                int distance = getDistance(parent, country); // Get distance between capitals
                if (distance != -1) {
                    path.add(parent + " --> " + country + " (" + distance + " km.)");
                } else {
                    path.add(parent + " --> " + country + " (Distance unknown)");
                }
            }
            country = parent;
        }
        Collections.reverse(path);
        return path;
    }

    public void acceptUserInput() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Enter the name of the first country (type EXIT to quit): ");
            String country1 = scanner.nextLine().trim();

            if (country1.equalsIgnoreCase("EXIT")) {
                break;
            }

            if (!countryBorders.containsKey(country1)) {
                System.out.println("Invalid country name. Please enter a valid country name.");
                continue;
            }

            System.out.print("Enter the name of the second country (type EXIT to quit): ");
            String country2 = scanner.nextLine().trim();

            if (country2.equalsIgnoreCase("EXIT")) {
                break;
            }

            if (!countryBorders.containsKey(country2)) {
                System.out.println("Invalid country name. Please enter a valid country name.");
                continue;
            }

            List<String> path = findPath(country1, country2);
            if (path.isEmpty()) {
                System.out.println("No valid path exists between " + country1 + " and " + country2);
            } else {
                System.out.println("Route from " + country1 + " to " + country2 + ":");
                for (String step : path) {
                    System.out.println("* " + step);
                }
            }
        }
        scanner.close();
    }

    public void printDataStructures() {
        // Print countryBorders
        System.out.println("Country Borders:");
        for (Map.Entry<String, List<String>> entry : countryBorders.entrySet()) {
            System.out.println(entry.getKey() + " borders: " + entry.getValue());
        }

        // Print countryAbbreviations
        System.out.println("\nCountry Abbreviations:");
        for (Map.Entry<String, String> entry : countryAbbreviations.entrySet()) {
            System.out.println(entry.getKey() + " abbreviation: " + entry.getValue());
        }

        // Print capitalDistances
        System.out.println("\nCapital Distances:");
        for (Map.Entry<String, Integer> entry : capitalDistances.entrySet()) {
            System.out.println(entry.getKey() + " distance: " + entry.getValue());
        }
    }

    public static void main(String[] args) {
        IRoadTrip r1 = new IRoadTrip(args);

        //a3.printDataStructures();

        /*
        // Test cases for getDistance method
        System.out.println("Distance between USA and Canada: " + a3.getDistance("My House", "Canada"));
        System.out.println("Distance between France and Spain: " + a3.getDistance("France", "Spain"));
        System.out.println("Distance between India and Bangladesh: " + a3.getDistance("India", "Bangladesh"));
        System.out.println("Distance between Germany and Japan: " + a3.getDistance("Germany", "Japan"));



        // Testing findPath method
        String country1 = "Paraguay";
        String country2 = "Colombia";
        List<String> path = a3.findPath(country1, country2);

        if (path.isEmpty()) {
            System.out.println("No path found between " + country1 + " and " + country2);
        } else {
            System.out.println("Path from " + country1 + " to " + country2 + ":");
            for (String step : path) {
                System.out.println("* " + step);
            }
        }
         */
        r1.acceptUserInput();
    }

}
