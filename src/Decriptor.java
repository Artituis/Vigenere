import java.util.LinkedHashMap;
import java.util.Map;

public final class Decriptor {

    // Reference letter-frequency distribution for Portuguese (a..z, fractions summing to ~1.0).
    // Source: standard pt-BR letter-frequency table used in classic cryptanalysis references.
    private static final double[] PT_LETTER_FREQ = {
            0.1463, 0.0104, 0.0388, 0.0499, 0.1257, 0.0102, 0.0130, 0.0128,
            0.0618, 0.0040, 0.0002, 0.0278, 0.0474, 0.0505, 0.1073, 0.0252,
            0.0120, 0.0653, 0.0781, 0.0434, 0.0463, 0.0167, 0.0001, 0.0021,
            0.0001, 0.0047
    };

    private static final double RANDOM_IC = 1.0 / 26.0;
    private static final double EXPECTED_IC_PT = sumOfSquares(PT_LETTER_FREQ);

    // Midpoint between a random-text IC and a monoalphabetic-Portuguese-text IC: a candidate
    // key length whose average column IC clears this bar is behaving like natural language.
    private static final double KEY_LENGTH_THRESHOLD = (RANDOM_IC + EXPECTED_IC_PT) / 2.0;

    private Decriptor() {
    }

    private static double sumOfSquares(double[] freq) {
        double sum = 0;
        for (double f : freq) {
            sum += f * f;
        }
        return sum;
    }

    public static double indexOfCoincidence(String text) {
        int[] counts = new int[26];
        for (int i = 0; i < text.length(); i++) {
            counts[text.charAt(i) - 'A']++;
        }
        long n = text.length();
        if (n < 2) {
            return 0.0;
        }
        long numerator = 0;
        for (int count : counts) {
            numerator += (long) count * (count - 1);
        }
        return (double) numerator / (n * (n - 1));
    }

    // Average IC per candidate key length, exposed so the attack can be shown/compared
    // (as the assignment asks) instead of only returning the final guess.
    public static Map<Integer, Double> evaluateKeyLengths(String cipherText, int maxLength) {
        Map<Integer, Double> results = new LinkedHashMap<>();
        for (int length = 1; length <= maxLength; length++) {
            String[] subtexts = splitIntoSubtexts(cipherText, length);
            double sum = 0;
            for (String subtext : subtexts) {
                sum += indexOfCoincidence(subtext);
            }
            results.put(length, sum / length);
        }
        return results;
    }

    // Picks the smallest candidate length whose average IC already looks like natural
    // Portuguese, since multiples of the true key length also produce a high IC and would
    // otherwise be indistinguishable from the real answer.
    public static int estimateKeyLength(String cipherText, int maxLength) {
        Map<Integer, Double> averages = evaluateKeyLengths(cipherText, maxLength);
        for (Map.Entry<Integer, Double> entry : averages.entrySet()) {
            if (entry.getValue() > KEY_LENGTH_THRESHOLD) {
                return entry.getKey();
            }
        }
        int bestLength = 1;
        double bestIc = -1;
        for (Map.Entry<Integer, Double> entry : averages.entrySet()) {
            if (entry.getValue() > bestIc) {
                bestIc = entry.getValue();
                bestLength = entry.getKey();
            }
        }
        return bestLength;
    }

    private static String[] splitIntoSubtexts(String cipherText, int keyLength) {
        StringBuilder[] builders = new StringBuilder[keyLength];
        for (int i = 0; i < keyLength; i++) {
            builders[i] = new StringBuilder();
        }
        for (int i = 0; i < cipherText.length(); i++) {
            builders[i % keyLength].append(cipherText.charAt(i));
        }
        String[] subtexts = new String[keyLength];
        for (int i = 0; i < keyLength; i++) {
            subtexts[i] = builders[i].toString();
        }
        return subtexts;
    }

    // Chi-squared goodness of fit between the subtext decoded with `shift` and the expected
    // Portuguese distribution. Counting cipher letters once and rotating that count vector per
    // shift avoids re-scanning the subtext 26 times, which matters for large files.
    private static double chiSquared(int[] cipherLetterCounts, int shift, int subtextLength) {
        double chiSq = 0;
        for (int i = 0; i < 26; i++) {
            int observed = cipherLetterCounts[(i + shift) % 26];
            double expected = PT_LETTER_FREQ[i] * subtextLength;
            double diff = observed - expected;
            chiSq += (diff * diff) / expected;
        }
        return chiSq;
    }

    private static int bestShiftForSubtext(String subtext) {
        int[] counts = new int[26];
        for (int i = 0; i < subtext.length(); i++) {
            counts[subtext.charAt(i) - 'A']++;
        }
        int bestShift = 0;
        double bestChiSq = Double.MAX_VALUE;
        for (int shift = 0; shift < 26; shift++) {
            double chiSq = chiSquared(counts, shift, subtext.length());
            if (chiSq < bestChiSq) {
                bestChiSq = chiSq;
                bestShift = shift;
            }
        }
        return bestShift;
    }

    // Blind attack entry point: estimates the key length from the IC table, then the shift of
    // each key position via chi-squared frequency analysis, without ever being told the key.
    public static String decriptKey(String message) {
        int keyLength = estimateKeyLength(message, 10);
        String[] subtexts = splitIntoSubtexts(message, keyLength);
        StringBuilder key = new StringBuilder();
        for (String subtext : subtexts) {
            int shift = bestShiftForSubtext(subtext);
            key.append((char) ('A' + shift));
        }
        return key.toString();
    }
}
