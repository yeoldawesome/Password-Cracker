import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadedBruteForce {
//c129db8be8904b40ac21c9cf5d9f5c0e24ef455d1d7a7bbfd7049fc6dc9d2429 zzzzzzzz
//c0e7aec81a1a194e9f54f6b297f6544041188c741ae8f55c2133b5c510a7dc1d dylan
//067592b448ea96dde1847791da3e6f35f1b1603b0b151773fc16db5a26e31715 joemam
    private static final char[] CHARACTERS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static volatile boolean passwordFound = false;
    private static long startTime;
    private static long endTime;
    private static AtomicInteger activeThreads = new AtomicInteger(8); // Track active threads

    public static void main(String[] args) {
        try {
            startTime = System.nanoTime();

            // Get user input (hash instead of password)
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            System.out.print("Enter the Hash (SHA-256, lowercase letters only): ");
            String targetHash = scanner.nextLine();
            scanner.close();

            if (targetHash.length() != 64) {
                System.err.println("Invalid hash length. Ensure the hash is a valid SHA-256 hash.");
                return;
            }

            System.out.println("Target Hash: " + targetHash);

            // Create a thread pool with 8 threads
            ExecutorService executor = Executors.newFixedThreadPool(8);

            // Each thread starts with its designated password length
            for (int length = 1; length <= 8; length++) {
                int finalLength = length;
                executor.submit(() -> {
                    bruteForce(targetHash, finalLength, true);
                    // System.out.println("Thread for length " + finalLength + " finished its search.");
                    if (finalLength == 1) {
                        bruteForce(targetHash, 8, false);
                    } else if (finalLength == 2) {
                        bruteForce(targetHash, 7, false);
                    } else if (finalLength == 3) {
                        bruteForce(targetHash, 6, false);
                    } else if (finalLength == 4) {
                        bruteForce(targetHash, 5, false);
                    }

                    // Decrease active thread count
                    activeThreads.decrementAndGet();
                });
            }

            // Shutdown executor after tasks are done
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            // Ensure endTime is set
            if (endTime == 0) {
                endTime = System.nanoTime();
            }

            // Calculate elapsed time
            long elapsedTime = endTime - startTime;
            double elapsedTimeInSeconds = elapsedTime / 1_000_000_000.0;
            System.out.println("Time elapsed: " + elapsedTimeInSeconds + " seconds");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Brute-force method for a specific length
    private static void bruteForce(String targetHash, int length, boolean forward) {
        generateAndCheck(new StringBuilder(), length, targetHash, forward);
    }

    // Iterative password generation to avoid recursion overhead
    private static void generateAndCheck(StringBuilder prefix, int length, String targetHash, boolean forward) {
        if (passwordFound) return; // Stop if another thread found the password

        // If length reached, check the hash
        if (prefix.length() == length) {
            String hash = generateHash(prefix.toString());
            if (hash != null && hash.equals(targetHash)) {
                System.out.println("Password found: " + prefix);
                synchronized (MultiThreadedBruteForce.class) {
                    if (!passwordFound) {
                        passwordFound = true;
                        endTime = System.nanoTime();
                    }
                }
            }
            return;
        }

        // Proceed with forward or reverse order
        if (forward) {
            for (char c : CHARACTERS) {
                prefix.append(c);
                generateAndCheck(prefix, length, targetHash, forward);
                if (passwordFound) return;
                prefix.deleteCharAt(prefix.length() - 1); // Backtrack
            }
        } else {
            for (int i = CHARACTERS.length - 1; i >= 0; i--) {
                prefix.append(CHARACTERS[i]);
                generateAndCheck(prefix, length, targetHash, forward);
                if (passwordFound) return;
                prefix.deleteCharAt(prefix.length() - 1); // Backtrack
            }
        }
    }

    // Function to generate SHA-256 hash
    private static String generateHash(String passcode) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(passcode.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: SHA-256 algorithm not found.");
            return null;
        }
    }
}
