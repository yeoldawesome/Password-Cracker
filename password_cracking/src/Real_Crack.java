import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Real_Crack {
    private static final char[] CHARACTERS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static AtomicBoolean passwordFound = new AtomicBoolean(false);
    private static long startTime;
    private static long endTime;

    public static void main(String[] args) {
        try {
            startTime = System.nanoTime();
    
            java.util.Scanner scanner = new java.util.Scanner(System.in);
            System.out.println("Enter '1' to provide a SHA-256 hash or '2' to provide a regular password:");
            String choice = scanner.nextLine();
            final String targetHash;
    
            if (choice.equals("1")) {
                System.out.print("Enter the Hash (SHA-256, lowercase letters only): ");
                targetHash = scanner.nextLine();
    
                if (targetHash.length() != 64) {
                    System.err.println("Invalid hash length. Ensure the hash is a valid SHA-256 hash.");
                    return;
                }
                System.out.println("Target Hash: " + targetHash);
            } else if (choice.equals("2")) {
                System.out.print("Enter the regular password: ");
                String regularPassword = scanner.nextLine();
                targetHash = generateHash(regularPassword);
    
                System.out.println("Target Hash (SHA-256): " + targetHash);
            } else {
                System.err.println("Invalid choice. Please enter '1' or '2'.");
                return;
            }

            System.out.println("Do you want to print each password being checked to the terminal? (yes/no)");
            String printChoice = scanner.nextLine();
            final boolean printPasswords = printChoice.equalsIgnoreCase("yes");

            int availableThreads = Runtime.getRuntime().availableProcessors();
            ExecutorService executor = Executors.newFixedThreadPool(availableThreads);
    
            for (int length = 8; length <= 13; length++) {
                final int finalLength = length;
                final boolean finalPrintPasswords = printPasswords;
                executor.submit(() -> {
                    bruteForce(targetHash, finalLength, finalPrintPasswords);
                });
            }
    
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void bruteForce(String targetHash, int length, boolean printPasswords) {
        int numThreads = Runtime.getRuntime().availableProcessors();
        int startIdx = 0;
        int endIdx = (int) Math.pow(26, length);
        int chunkSize = (endIdx - startIdx) / numThreads;

        for (int i = 0; i < numThreads; i++) {
            int threadStart = startIdx + i * chunkSize;
            int threadEnd = (i == numThreads - 1) ? endIdx : startIdx + (i + 1) * chunkSize;

            new Thread(() -> generateAndCheck(threadStart, threadEnd, length, targetHash, printPasswords)).start();
        }
    }

    private static void generateAndCheck(int startIdx, int endIdx, int length, String targetHash, boolean printPasswords) {
        if (passwordFound.get()) return;

        System.out.println(Thread.currentThread().getName() + " is checking passwords from index " + startIdx + " to " + (endIdx - 1));

        for (int i = startIdx; i < endIdx; i++) {
            String password = indexToPassword(i, length);
            String hash = generateHash(password);

            if (printPasswords) {
                System.out.println(Thread.currentThread().getName() + " checking password: " + password);
            }

            if (hash != null && hash.equals(targetHash)) {
                System.out.println("Password found: " + password);
                if (endTime == 0) {
                    endTime = System.nanoTime();
                }
                long elapsedTime = endTime - startTime;
                double elapsedTimeInSeconds = elapsedTime / 1_000_000_000.0;
                System.out.println("Time elapsed: " + elapsedTimeInSeconds + " seconds");

                passwordFound.set(true);
                endTime = System.nanoTime();
                break;
            }
        }
    }

    private static String indexToPassword(int index, int length) {
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int charIdx = index % 26;
            password.append(CHARACTERS[charIdx]);
            index /= 26;
        }

        return password.toString();
    }

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
