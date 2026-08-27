public class Vigenere {
    public static String encode(String plaintext, String keyword) {
        StringBuilder ciphertext = new StringBuilder();
        keyword = keyword.toUpperCase();
        plaintext = plaintext.toUpperCase();

        for (int i = 0; i < plaintext.length(); i++) {
            char letter = plaintext.charAt(i);
            char shift = keyword.charAt(i % keyword.length());
            char encryptedLetter = (char)((letter + shift - 2 * 'A') % 26 + 'A');
            ciphertext.append(encryptedLetter);
        }

        return ciphertext.toString();
    }

    public static String decode(String ciphertext, String keyword) {
        StringBuilder plaintext = new StringBuilder();
        keyword = keyword.toUpperCase();
        ciphertext = ciphertext.toUpperCase();

        for (int i = 0; i < ciphertext.length(); i++) {
            char letter = ciphertext.charAt(i);
            char shift = keyword.charAt(i % keyword.length());
            char decryptedLetter = (char)((letter - shift + 26) % 26 + 'A');
            plaintext.append(decryptedLetter);
        }

        return plaintext.toString();
    }
}
