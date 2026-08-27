
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;


public class Main {
    static void main() {
        Path inputFile = Path.of("input.txt");
        Path encodedFile = Path.of("encoded.txt");
        Path outputFile = Path.of("output.txt");
        String key = "secret";
        try {
            // Read the entire file into a String
            String content = Files.readString(inputFile);
            content = content.replaceAll("[^a-zA-Z]", "");
            // Print the content
            //System.out.println(content);

            String encoded = Vigenere.encode(content, key);
            Files.writeString(encodedFile, encoded);
            System.out.println("Key is: " + Decriptor.decriptKey(encoded));
            String decoded = Vigenere.decode(encoded, key);

            // Save the content to the output file
            Files.writeString(outputFile, decoded);
            System.out.println(decoded.equals(content.toUpperCase()));
            System.out.println("Content saved to " + outputFile);

        } catch (IOException e) {
            System.err.println("Error reading/writing file: " + e.getMessage());
        }
    }
}
