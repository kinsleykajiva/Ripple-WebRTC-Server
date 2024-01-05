package africa.jopen.ripple.utils;

import africa.jopen.ripple.models.MainConfigModel;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;

public class XUtils {
	private static final Logger LOGGER = Logger.getLogger(XUtils.class.getName());
	public static String          BASE_APP_LOCATION_PATH;
	public static MainConfigModel MAIN_CONFIG_MODEL;
	
	static {
		LoggerConfig.setupLogger(LOGGER);
	}
	
	private XUtils() {
	}
	public static void copyConfigFilesTemplates(Path sourceFolder, Path destinationFolder) throws IOException {
    if (!Files.exists(destinationFolder)) {
        Files.createDirectories(destinationFolder);
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceFolder)) {
        for (Path file: stream) {
            Path destFile = Paths.get(destinationFolder.toString(), file.getFileName().toString());
            if (!Files.exists(destFile)) {
                Files.copy(file, destFile);
            }
        }
    }
}
	
	public static void generateJwtKeys(Path privateKeyPath, Path publicKeyPath) throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		KeyPair kp = kpg.generateKeyPair();
		
		if (Files.notExists(privateKeyPath.getParent())) {
			Files.createDirectories(privateKeyPath.getParent());
		}
		
		try (BufferedWriter writer = Files.newBufferedWriter(privateKeyPath,
				StandardCharsets.UTF_8)) {
			writer.write("-----BEGIN PRIVATE KEY-----");
			writer.write(System.lineSeparator());
			writer.write(Base64.getMimeEncoder().encodeToString(kp.getPrivate().getEncoded()));
			writer.write(System.lineSeparator());
			writer.write("-----END PRIVATE KEY-----");
		}
		
		try (BufferedWriter writer = Files.newBufferedWriter(publicKeyPath,
				StandardCharsets.UTF_8)) {
			writer.write("-----BEGIN PUBLIC KEY-----");
			writer.write(System.lineSeparator());
			writer.write(Base64.getMimeEncoder().encodeToString(kp.getPublic().getEncoded()));
			writer.write(System.lineSeparator());
			writer.write("-----END PUBLIC KEY-----");
		}
	}
	public static String IdGenerator() {
		UUID uuid = UUID.randomUUID();
		// Remove dashes from UUID and concatenate with time string
		String concatenated = uuid.toString().replaceAll("-", "") + System.currentTimeMillis();
		return concatenated.replaceAll("-", "");
	}
	
	public static JSONObject failedResponse(String transactionId, String message, JSONObject data) {
		LOGGER.info("Building error/failed response");
		
		JSONObject returnObject = new JSONObject()
				.put("success", false)
				.put("transactionId", transactionId)
				.put("timeStamp", System.currentTimeMillis())
				.put("message", message)
				.put("data", data);
		
		return returnObject;
	}
	
	public static JSONObject successResponse(String transactionId, String message, JSONObject data) {
		LOGGER.info("Building success response");
		
		JSONObject returnObject = new JSONObject()
				.put("success", true)
				.put("transactionId", transactionId)
				.put("timeStamp", System.currentTimeMillis())
				.put("message", message)
				.put("data", data);
		
		return returnObject;
	}
	
}
