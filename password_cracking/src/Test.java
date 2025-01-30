import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Test {

    private static final char[] CHARACTERS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static volatile boolean passwordFound = false;
    private static long startTime;
    private static long endTime;
    private static AtomicInteger activeThreads = new AtomicInteger(8); // Track active threads

    public static void main(String[] args) {
        try {
            startTime = System.nanoTime();

            // Get user input
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            System.out.print("Enter a Password (lowercase letters only): ");
            String userInput = scanner.nextLine();
            scanner.close();

            String targetHash = generateHash(userInput);
            if (targetHash == null) {
                System.err.println("Error generating hash. Exiting.");
                return;
            }
            System.out.println("Hash Made: " + targetHash);

            // Create a thread pool with 8 threads
            ExecutorService executor = Executors.newFixedThreadPool(8);

            // Each thread starts with its designated password length
            for (int length = 1; length <= 8; length++) {
                int finalLength = length;
                executor.submit(() -> {
                    bruteForce(targetHash, finalLength, true);
                    //System.out.println("Thread for length " + finalLength + " finished its search.");

                    // If this thread is done, attempt to help the longest (length 8) thread in reverse
                    if (finalLength == 1) {
                       // System.out.println("Thread 1 is now assisting thread 8 in reverse.");
                        bruteForce(targetHash, 8, false);
                    }
                    if (finalLength == 2) {
                       // System.out.println("Thread 2 is now assisting thread 7 in reverse.");
                        bruteForce(targetHash, 7, false);
                    }
                    if (finalLength == 3) {
                       // System.out.println("Thread 3 is now assisting thread 6 in reverse.");
                        bruteForce(targetHash, 6, false);
                    }
                    if (finalLength == 4) {
                       // System.out.println("Thread 4 is now assisting thread 5 in reverse.");
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
        generateAndCheck("", length, targetHash, forward);
    }

    // Recursive method to generate passwords dynamically
    private static void generateAndCheck(String prefix, int length, String targetHash, boolean forward) {
        if (passwordFound) return; // Stop if another thread found the password

        if (prefix.length() == length) {
            String hash = generateHash(prefix);
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

        if (forward) {
            for (char c : CHARACTERS) {
                generateAndCheck(prefix + c, length, targetHash, forward);
                if (passwordFound) return;
            }
        } else {
            for (int i = CHARACTERS.length - 1; i >= 0; i--) {
                generateAndCheck(prefix + CHARACTERS[i], length, targetHash, forward);
                if (passwordFound) return;
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
