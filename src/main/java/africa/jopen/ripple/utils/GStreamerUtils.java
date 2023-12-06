package africa.jopen.ripple.utils;

import com.sun.jna.Platform;

import java.io.File;
import java.util.stream.Stream;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import jakarta.json.JsonObject;

public  final class GStreamerUtils{
	private GStreamerUtils(){}
	/**
	 * Configures paths to the GStreamer libraries. On Windows queries various
	 * GStreamer environment variables, and then sets up the PATH environment
	 * variable. On macOS, adds the location to jna.library.path (macOS binaries
	 * link to each other). On both, the gstreamer.path system property can be
	 * used to override. On Linux, assumes GStreamer is in the path already.
	 */
	public static void configurePaths() {
		if (Platform.isWindows()) {
			String gstPath = System.getProperty("gstreamer.path", findWindowsLocation());
			if (!gstPath.isEmpty()) {
				String systemPath = System.getenv("PATH");
				if (systemPath == null || systemPath.trim().isEmpty()) {
					Kernel32.INSTANCE.SetEnvironmentVariable("PATH", gstPath);
				} else {
					Kernel32.INSTANCE.SetEnvironmentVariable("PATH", gstPath
							+ File.pathSeparator + systemPath);
				}
			}
		} else if (Platform.isMac()) {
			String gstPath = System.getProperty("gstreamer.path",
					"/Library/Frameworks/GStreamer.framework/Libraries/");
			if (!gstPath.isEmpty()) {
				String jnaPath = System.getProperty("jna.library.path", "").trim();
				if (jnaPath.isEmpty()) {
					System.setProperty("jna.library.path", gstPath);
				} else {
					System.setProperty("jna.library.path", jnaPath + File.pathSeparator + gstPath);
				}
			}
			
		}
	}
	
	/**
	 * Query over a stream of possible environment variables for GStreamer
	 * location, filtering on the first non-null result, and adding \bin\ to the
	 * value.
	 *
	 * @return location or empty string
	 */
	static String findWindowsLocation() {
		if (Platform.is64Bit()) {
			return Stream.of("GSTREAMER_1_0_ROOT_MSVC_X86_64",
							"GSTREAMER_1_0_ROOT_MINGW_X86_64",
							"GSTREAMER_1_0_ROOT_X86_64")
					.map(System::getenv)
					.filter(p -> p != null)
					.map(p -> p.endsWith("\\") ? p + "bin\\" : p + "\\bin\\")
					.findFirst().orElse("");
		} else {
			return "";
		}
	}
}

