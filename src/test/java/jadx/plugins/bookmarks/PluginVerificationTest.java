package jadx.plugins.bookmarks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;

import static org.assertj.core.api.Assertions.assertThat;

class PluginVerificationTest {

	@Test
	public void testSpiDiscovery() {
		ServiceLoader<JadxPlugin> loader = ServiceLoader.load(JadxPlugin.class);
		boolean found = false;
		for (JadxPlugin plugin : loader) {
			if (plugin instanceof BookmarksPlugin) {
				found = true;
				JadxPluginInfo info = plugin.getPluginInfo();
				assertThat(info.getPluginId()).isEqualTo("jadx-bookmarks");
				assertThat(info.getName()).isEqualTo("JADX Bookmarks");
				assertThat(info.getDescription()).contains("Bookmarks");
				assertThat(info.getHomepage()).contains("jadx-bookmarks");
				break;
			}
		}
		assertThat(found).isTrue();
	}

	@Test
	public void testManagerPersistence(@TempDir Path tempDir) throws Exception {
		// Test standalone persistence without MainWindow
		BookmarkData bm1 = new BookmarkData("com.test.MyActivity", "MyActivity", "onCreate", 15,
				"Activity entry point", "POSITIVE_CHECKMARK");
		BookmarkData bm2 = new BookmarkData("com.test.Utils", "Utils", "format", 42,
				"Important helper", "RED_FLAG");

		Path bookmarksFile = tempDir.resolve("bookmarks_test.json");

		// Save to JSON
		List<BookmarkData> list = List.of(bm1, bm2);
		String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(list);
		Files.writeString(bookmarksFile, json);

		// Read back
		String loadedJson = Files.readString(bookmarksFile);
		java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<BookmarkData>>() {}.getType();
		List<BookmarkData> loaded = new com.google.gson.Gson().fromJson(loadedJson, type);

		assertThat(loaded).hasSize(2);
		assertThat(loaded.get(0).getPlaceString()).isEqualTo("MyActivity:onCreate:15");
		assertThat(loaded.get(0).getBookmarkEmoji()).isEqualTo(BookmarkEmoji.POSITIVE_CHECKMARK);
		assertThat(loaded.get(1).getPlaceString()).isEqualTo("Utils:format:42");
		assertThat(loaded.get(1).getBookmarkEmoji()).isEqualTo(BookmarkEmoji.RED_FLAG);
	}
}
