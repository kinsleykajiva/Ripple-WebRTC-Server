package africa.jopen.models.configs.main;

import org.eclipse.collections.api.list.MutableList;

public record Logs(String prefix  , String folderPath, MutableList<String> show) {
}
