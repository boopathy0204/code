import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;

class Share {
    private final BigInteger x;
    private final BigInteger y;

    public Share(BigInteger x, BigInteger y) {
        this.x = x;
        this.y = y;
    }

    public BigInteger getX() { return x; }
    public BigInteger getY() { return y; }
}

public class Main {

    public static void main(String[] args) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            try (InputStream input = Main.class
                    .getClassLoader()
                    .getResourceAsStream("data.json")) {

                if (input == null) {
                    throw new RuntimeException("data.json not found in resources folder");
                }

                JsonNode root = mapper.readTree(input);

                int k = root.get("keys").get("k").asInt();

                BigInteger prime;
                if (root.get("keys").has("prime")) {
                    prime = new BigInteger(root.get("keys").get("prime").asText());
                } else {
                    prime = new BigInteger("2089"); // default prime
                }

                List<Share> shares = new ArrayList<>();

                Iterator<Map.Entry<String, JsonNode>> fields = root.fields();

                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();

                    if (entry.getKey().equals("keys")) continue;

                    BigInteger x = new BigInteger(entry.getKey());

                    String baseStr = entry.getValue().get("base").asText();
                    String valueStr = entry.getValue().get("value").asText();

                    int base = Integer.parseInt(baseStr);

                    BigInteger y = new BigInteger(valueStr, base);

                    shares.add(new Share(x, y.mod(prime)));
                }

                shares.sort(Comparator.comparing(Share::getX));

                if (shares.size() < k) {
                    throw new IllegalArgumentException("Not enough shares to reconstruct secret");
                }

                List<Share> selectedShares = shares.subList(0, k);

                BigInteger secret = lagrangeInterpolationAtZero(selectedShares, prime);

                System.out.println("Recovered Secret: " + secret);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static BigInteger lagrangeInterpolationAtZero(List<Share> shares, BigInteger prime) {

        BigInteger result = BigInteger.ZERO;
        int k = shares.size();

        for (int i = 0; i < k; i++) {

            BigInteger xi = shares.get(i).getX();
            BigInteger yi = shares.get(i).getY();

            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i == j) continue;

                BigInteger xj = shares.get(j).getX();

                numerator = numerator.multiply(xj.negate()).mod(prime);

                denominator = denominator
                        .multiply(xi.subtract(xj))
                        .mod(prime);
            }

            BigInteger inverseDenominator = denominator.modInverse(prime);

            BigInteger term = yi
                    .multiply(numerator)
                    .mod(prime)
                    .multiply(inverseDenominator)
                    .mod(prime);

            result = result.add(term).mod(prime);
        }

        return result;
    }
}