package jadx.plugins.bookmarks.gui;

import java.awt.Container;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.tab.ITabStatesListener;
import jadx.gui.ui.tab.TabBlueprint;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.UiUtils;
import jadx.plugins.bookmarks.BookmarkEmoji;
import jadx.plugins.bookmarks.BookmarkManager;

public class BookmarksGui {
	private static final Logger LOG = LoggerFactory.getLogger(BookmarksGui.class);

	private final JadxPluginContext pluginContext;
	private JadxGuiContext guiContext;
	private MainWindow mainWindow;
	private BookmarkManager bookmarkManager;

	public BookmarksGui(JadxPluginContext pluginContext) {
		this.pluginContext = pluginContext;
	}

	public void init(JadxGuiContext gui) {
		this.guiContext = gui;

		guiContext.addMenuAction("Address Bookmarks (Ctrl+M)", () -> {
			if (bookmarkManager != null) {
				bookmarkManager.showBookmarksDialog();
			}
		});

		guiContext.addPopupMenuAction("Add/Edit Bookmark (Alt+M)", ref -> true, "alt M", nodeRef -> {
			if (bookmarkManager != null) {
				bookmarkManager.openBookmarkDialogForCurrentEditor();
			}
		});

		guiContext.registerGlobalKeyBinding("jadx-bookmarks:show", "control M", () -> {
			if (bookmarkManager != null) {
				bookmarkManager.showBookmarksDialog();
			}
		});

		gui.uiRun(() -> {
			try {
				JFrame frame = gui.getMainFrame();
				if (frame instanceof MainWindow) {
					this.mainWindow = (MainWindow) frame;
					this.bookmarkManager = new BookmarkManager(pluginContext, gui, mainWindow);

					initMainWindowHooks();
				}
			} catch (Exception e) {
				LOG.error("Failed to initialize JADX Bookmarks GUI", e);
			}
		});
	}

	private void initMainWindowHooks() {
		addNavigationMenuItem();

		// Alt+M global shortcut
		KeyStroke altM = KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.ALT_DOWN_MASK);
		UiUtils.addKeyBinding(mainWindow.getRootPane(), altM, "jadx-bookmarks:toggle", () -> {
			bookmarkManager.openBookmarkDialogForCurrentEditor();
		});

		// Tab selection change listener
		mainWindow.getTabbedPane().addChangeListener(e -> {
			bookmarkManager.refreshAllGutters();
		});

		// Tab container listener
		mainWindow.getTabbedPane().addContainerListener(new ContainerAdapter() {
			@Override
			public void componentAdded(ContainerEvent e) {
				SwingUtilities.invokeLater(() -> bookmarkManager.refreshAllGutters());
			}
		});

		// TabsController listener for tab open, select, and code jumps
		try {
			mainWindow.getTabsController().addListener(new ITabStatesListener() {
				@Override
				public void onTabOpen(TabBlueprint blueprint) {
					SwingUtilities.invokeLater(() -> bookmarkManager.refreshAllGutters());
				}

				@Override
				public void onTabSelect(TabBlueprint blueprint) {
					SwingUtilities.invokeLater(() -> bookmarkManager.refreshAllGutters());
				}

				@Override
				public void onTabCodeJump(TabBlueprint blueprint, @Nullable JumpPosition prevPos, JumpPosition newPos) {
					SwingUtilities.invokeLater(() -> bookmarkManager.refreshAllGutters());
				}
			});
		} catch (Throwable e) {
			LOG.debug("Could not add tabs controller listener", e);
		}

		// Project load listener
		mainWindow.addLoadListener(loaded -> {
			if (loaded) {
				bookmarkManager.loadBookmarks();
				bookmarkManager.refreshAllGutters();
			}
			return false;
		});

		// Initial refresh
		bookmarkManager.refreshAllGutters();
	}

	private void addNavigationMenuItem() {
		try {
			JMenuBar menuBar = mainWindow.getJMenuBar();
			if (menuBar != null) {
				for (int i = 0; i < menuBar.getMenuCount(); i++) {
					JMenu menu = menuBar.getMenu(i);
					if (menu != null && menu.getText() != null
							&& (menu.getText().contains("Navigation") || menu.getText().contains("Nav"))) {
						JMenuItem item = new JMenuItem("Address Bookmarks");
						item.setIcon(BookmarkEmoji.STAR.getIcon());
						item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, UiUtils.ctrlButton()));
						item.addActionListener(e -> bookmarkManager.showBookmarksDialog());
						menu.add(item);
						break;
					}
				}
			}
		} catch (Exception e) {
			LOG.debug("Could not add item to Navigation menu", e);
		}
	}

	public @Nullable BookmarkManager getBookmarkManager() {
		return bookmarkManager;
	}
}
