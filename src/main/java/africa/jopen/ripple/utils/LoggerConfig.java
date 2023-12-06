package africa.jopen.ripple.utils;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LoggerConfig {
	private static Thread logMaintenanceThread = null;
	
	public static void setupLogger(Logger logger) {
		logger.setUseParentHandlers(false);
		ConsoleHandler handler = new ColoredConsoleHandler();
		logger.addHandler(handler);
		/*try {
			FileHandler handler = new FileHandler("%t/ripple-server-_%g.log", 1024 * 1024 * 1024, 1, true);
			handler.setFormatter(new SimpleFormatter());
			
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to setup logger handler", e);
		}*/
		
		/*if (logMaintenanceThread == null) {
			logMaintenanceThread = new Thread(() -> {
				
				while (true) {
					try {
						Files.find(Paths.get(System.getProperty("java.io.tmpdir")),
										2,
										(path, basicFileAttributes) -> path.toString().endsWith(".log"))
								.forEach(path -> {
									try {
										// Zip old log files
										Path zipPath = Paths.get(path.toString() + ".zip");
										try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
											ZipEntry zipEntry = new ZipEntry(path.getFileName().toString());
											zipOutputStream.putNextEntry(zipEntry);
											Files.copy(path, zipOutputStream);
											zipOutputStream.closeEntry();
										} catch (IOException e) {
											logger.log(Level.SEVERE, "Failed to zip log file", e);
										}
										
										// Check if the file is in use before attempting to delete it
										try {
											FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE);
											channel.close();
											
											// File is not in use, we can delete it
											
											if (path != null) {
												// test if this file still exists
												if (Files.exists(path)) {
													Files.delete(path);
												}
											}
										} catch (IOException e) {
											logger.log(Level.WARNING, "File is in use, couldn't delete.", e);
											logger.log(Level.SEVERE, "Failed to delete log file", e);
										}
										
										// Delete zipped log files after 2 weeks
										try {
											BasicFileAttributes attr         = Files.readAttributes(zipPath, BasicFileAttributes.class);
											long                creationTime = attr.creationTime().toMillis();
											if (System.currentTimeMillis() - creationTime > 14 * 24 * 60 * 60 * 1000) {
												Files.delete(zipPath);
											}
										} catch (IOException e) {
											logger.log(Level.SEVERE, "Failed to delete zipped log file", e);
										}
									} catch (Exception e) {
										logger.log(Level.SEVERE, "Failed to maintain log files", e);
									}
								});
						Thread.sleep(24 * 60 * 60 * 1000); // Sleep for a day
					} catch (Exception e) {
						logger.log(Level.SEVERE, "Failed to maintain log files", e);
					}
				}
			});
			logMaintenanceThread.start();
		}*/
	}
}