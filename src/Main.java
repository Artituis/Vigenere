
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;


public class Main {
    static void main() {
        Path inputFile = Path.of("input.txt");
        Path encodedFile = Path.of("encoded.txt");
        Path outputFile = Path.of("output.txt");
        String key = "segredo";
        try {
            // Read the entire file into a String
            String content = Files.readString(inputFile);
            content = content.toLowerCase();
            content = Normalizer.normalize(content, Normalizer.Form.NFD);          // á -> "a" + separate accent mark
            content = content.replaceAll("\\p{InCombiningDiacriticalMarks}+", ""); // strip the accent mark, keep "a"
            content = content.replaceAll("[^a-z]", "");                           // remove anything that isn't a letter

            String encoded = Vigenere.encode(content, key);
            Files.writeString(encodedFile, encoded);

            // Blind attack: the real key is never passed to Decriptor, only the ciphertext.
            String guessedKey = Decriptor.decriptKey(encoded);
            System.out.println("Key is: " + guessedKey);
            String decoded = Vigenere.decode(encoded, guessedKey);

            // Save the content to the output file
            Files.writeString(outputFile, decoded);
            System.out.println(decoded.equals(content.toUpperCase()));
            System.out.println("Content saved to " + outputFile);

        } catch (IOException e) {
            System.err.println("Error reading/writing file: " + e.getMessage());
        }
    }
}
