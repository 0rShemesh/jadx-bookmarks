package jadx.plugins.bookmarks;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.plugins.bookmarks.gui.BookmarksGui;

public class BookmarksPlugin implements JadxPlugin {
	public static final String PLUGIN_ID = "jadx-bookmarks";

	private static BookmarksPlugin instance;
	private BookmarksGui gui;

	public static @Nullable BookmarksPlugin getInstance() {
		return instance;
	}

	@Override
	public JadxPluginInfo getPluginInfo() {
		return JadxPluginInfoBuilder.pluginId(PLUGIN_ID)
				.name("JADX Bookmarks")
				.description("Bookmarks plugin with emoji markers and navigation")
				.homepage("https://github.com/0rShemesh/jadx-bookmarks")
				.provides("gui-bookmarks")
				.build();
	}

	@Override
	public void init(JadxPluginContext context) {
		instance = this;
		JadxGuiContext guiContext = context.getGuiContext();
		if (guiContext != null) {
			this.gui = new BookmarksGui(context);
			this.gui.init(guiContext);
		}
	}

	public @Nullable BookmarkManager getBookmarkManager() {
		return gui != null ? gui.getBookmarkManager() : null;
	}
}
