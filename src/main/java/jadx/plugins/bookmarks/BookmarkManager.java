package jadx.plugins.bookmarks;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.gui.jobs.IBackgroundTask;
import jadx.gui.jobs.SimpleTask;
import jadx.gui.jobs.TaskWithExtraOnFinish;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.UiUtils;

public class BookmarkManager {
	private static final Logger LOG = LoggerFactory.getLogger(BookmarkManager.class);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final JadxPluginContext pluginContext;
	private final JadxGuiContext guiContext;
	private final MainWindow mainWindow;
	private final List<BookmarkData> bookmarks = new CopyOnWriteArrayList<>();

	public BookmarkManager(JadxPluginContext pluginContext, JadxGuiContext guiContext, MainWindow mainWindow) {
		this.pluginContext = pluginContext;
		this.guiContext = guiContext;
		this.mainWindow = mainWindow;
		loadBookmarks();
	}

	public List<BookmarkData> getAllBookmarks() {
		return Collections.unmodifiableList(bookmarks);
	}

	public synchronized void addBookmark(BookmarkData bookmark) {
		bookmarks.remove(bookmark);
		bookmarks.add(bookmark);
		saveBookmarks();
		refreshAllGutters();
	}

	public synchronized void removeBookmark(BookmarkData bookmark) {
		bookmarks.remove(bookmark);
		saveBookmarks();
		refreshAllGutters();
	}

	public synchronized void removeBookmark(String clsName, int line) {
		BookmarkData existing = getBookmark(clsName, line);
		if (existing != null) {
			removeBookmark(existing);
		}
	}

	public synchronized void bookmarkChanged(BookmarkData bookmark) {
		saveBookmarks();
		refreshAllGutters();
	}

	public synchronized @Nullable BookmarkData getBookmark(String clsName, int line) {
		for (BookmarkData b : bookmarks) {
			if (b.getLine() == line && b.matchesClass(clsName)) {
				return b;
			}
		}
		return null;
	}

	public List<BookmarkData> getBookmarksForNode(JNode node) {
		if (node == null) {
			return Collections.emptyList();
		}
		String rawName = null;
		String fullName = null;
		if (node instanceof JClass) {
			JavaClass cls = ((JClass) node).getCls();
			rawName = cls.getRawName();
			fullName = cls.getFullName();
		} else if (node.getRootClass() != null) {
			JavaClass cls = node.getRootClass().getCls();
			rawName = cls.getRawName();
			fullName = cls.getFullName();
		} else {
			fullName = node.getName();
		}

		List<BookmarkData> result = new ArrayList<>();
		for (BookmarkData b : bookmarks) {
			if (b.matchesClass(rawName, fullName, node.getName())) {
				result.add(b);
			}
		}
		return result;
	}

	public void showBookmarksDialog() {
		AddressBookmarksDialog.show(mainWindow, this);
	}

	public void openBookmarkDialogForCurrentEditor() {
		ContentPanel selectedPanel = mainWindow.getTabbedPane().getSelectedContentPanel();
		if (selectedPanel instanceof ClassCodeContentPanel) {
			AbstractCodeArea codeArea = ((ClassCodeContentPanel) selectedPanel).getCurrentCodeArea();
			if (codeArea != null) {
				int pos = codeArea.getCaretPosition();
				int line = 1;
				try {
					line = codeArea.getLineOfOffset(pos) + 1;
				} catch (Exception e) {
					// ignore
				}
				openBookmarkDialog(codeArea, line, pos);
			}
		} else if (selectedPanel instanceof AbstractCodeContentPanel) {
			AbstractCodeArea codeArea = ((AbstractCodeContentPanel) selectedPanel).getCodeArea();
			if (codeArea != null) {
				int pos = codeArea.getCaretPosition();
				int line = 1;
				try {
					line = codeArea.getLineOfOffset(pos) + 1;
				} catch (Exception e) {
					// ignore
				}
				openBookmarkDialog(codeArea, line, pos);
			}
		}
	}

	public void openBookmarkDialog(AbstractCodeArea codeArea, int line) {
		int pos = 0;
		try {
			pos = codeArea.getLineStartOffset(Math.max(0, line - 1));
		} catch (Exception e) {
			pos = codeArea.getCaretPosition();
		}
		openBookmarkDialog(codeArea, line, pos);
	}

	public void openBookmarkDialog(AbstractCodeArea codeArea, int line, int pos) {
		JNode node = codeArea.getNode();
		String rawCls = "";
		String clsName = "";
		String funcName = "";

		if (node instanceof JClass) {
			JavaClass cls = ((JClass) node).getCls();
			rawCls = cls.getRawName();
			clsName = cls.getFullName();
		} else if (node != null) {
			clsName = node.getName();
			if (node.getRootClass() != null) {
				rawCls = node.getRootClass().getCls().getRawName();
			}
		}

		if (codeArea instanceof CodeArea) {
			JavaNode javaNode = ((CodeArea) codeArea).getJavaNodeAtOffset(pos);
			if (javaNode instanceof JavaMethod) {
				funcName = javaNode.getName();
			}
		}

		BookmarkData existing = findExistingBookmark(rawCls, clsName, line);

		BookmarkInputDialog.BookmarkInputResult result = BookmarkInputDialog.show(mainWindow, existing);
		if (result == null) {
			// Cancel clicked -> do nothing
			return;
		}

		if (existing != null) {
			existing.setDescription(result.getDescription());
			existing.setIconKey(result.getEmoji().getKey());
			bookmarkChanged(existing);
		} else {
			BookmarkData newBm = new BookmarkData(rawCls, clsName, funcName, line,
					result.getDescription(), result.getEmoji().getKey());
			addBookmark(newBm);
		}
	}

	private @Nullable BookmarkData findExistingBookmark(String rawCls, String clsName, int line) {
		for (BookmarkData b : bookmarks) {
			if (b.getLine() == line && (b.matchesClass(rawCls) || b.matchesClass(clsName))) {
				return b;
			}
		}
		return null;
	}

	public void jumpToBookmark(BookmarkData bookmark) {
		JNode node = resolveNode(bookmark);
		if (node == null) {
			LOG.warn("Cannot resolve node for bookmark: {}", bookmark);
			return;
		}
		JClass rootCls = node.getRootClass();
		if (rootCls == null && node instanceof JClass) {
			rootCls = (JClass) node;
		}
		if (rootCls == null) {
			mainWindow.getTabsController().codeJump(node);
			return;
		}

		JClass finalRootCls = rootCls;
		Runnable scrollAction = () -> {
			mainWindow.getTabsController().selectTab(finalRootCls);
			ContentPanel panel = mainWindow.getTabbedPane().getTabByNode(finalRootCls);
			if (panel instanceof ClassCodeContentPanel) {
				ClassCodeContentPanel ccp = (ClassCodeContentPanel) panel;
				AbstractCodeArea codeArea = ccp.getCurrentCodeArea();
				if (codeArea != null) {
					scrollToLine(codeArea, bookmark.getLine());
				}
			} else if (panel instanceof AbstractCodeContentPanel) {
				AbstractCodeArea codeArea = ((AbstractCodeContentPanel) panel).getCodeArea();
				if (codeArea != null) {
					scrollToLine(codeArea, bookmark.getLine());
				}
			}
		};

		mainWindow.getTabsController().codeJump(finalRootCls);

		SimpleTask loadTask = finalRootCls.getLoadTask();
		if (loadTask == null) {
			UiUtils.uiRun(scrollAction);
		} else {
			executeBackgroundTask(new TaskWithExtraOnFinish(loadTask, scrollAction));
		}
	}

	private void executeBackgroundTask(IBackgroundTask task) {
		try {
			Object bgExecutor = mainWindow.getBackgroundExecutor();
			if (bgExecutor != null) {
				Method executeMethod = bgExecutor.getClass().getMethod("execute", IBackgroundTask.class);
				executeMethod.invoke(bgExecutor, task);
				return;
			}
		} catch (Exception e) {
			LOG.warn("Failed to run task on BackgroundExecutor via reflection", e);
		}
		UiUtils.uiRun(() -> {
			try {
				task.onFinish(null);
			} catch (Exception ignored) {
			}
		});
	}

	private void scrollToLine(AbstractCodeArea codeArea, int line) {
		SwingUtilities.invokeLater(() -> {
			int line0 = Math.max(0, line - 1);
			if (line0 < codeArea.getLineCount()) {
				try {
					int offset = codeArea.getLineStartOffset(line0);
					codeArea.scrollToPos(offset);
					codeArea.setCaretPosition(offset);
					codeArea.requestFocusInWindow();
				} catch (Exception e) {
					LOG.warn("Failed to scroll to bookmark line {}", line, e);
				}
			}
		});
	}

	public @Nullable JNode resolveNode(BookmarkData bookmark) {
		try {
			if (bookmark.getRawClsName() != null && !bookmark.getRawClsName().isEmpty()) {
				JavaClass javaClass = mainWindow.getWrapper().searchJavaClassByRawName(bookmark.getRawClsName());
				if (javaClass != null) {
					return mainWindow.getCacheObject().getNodeCache().makeFrom(javaClass);
				}
			}
			if (bookmark.getClsName() != null && !bookmark.getClsName().isEmpty()) {
				JavaClass javaClass = mainWindow.getWrapper().searchJavaClassByFullAlias(bookmark.getClsName());
				if (javaClass == null) {
					javaClass = mainWindow.getWrapper().searchJavaClassByOrigClassName(bookmark.getClsName());
				}
				if (javaClass != null) {
					return mainWindow.getCacheObject().getNodeCache().makeFrom(javaClass);
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to resolve node for bookmark", e);
		}
		return null;
	}

	public void attachToCodeArea(AbstractCodeArea codeArea) {
		if (codeArea == null) {
			return;
		}
		if (codeArea.getClientProperty("jadx-bookmarks:attached") == null) {
			codeArea.putClientProperty("jadx-bookmarks:attached", Boolean.TRUE);

			KeyStroke altM = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.KeyEvent.ALT_DOWN_MASK);
			UiUtils.addKeyBinding(codeArea, altM, "jadx-bookmarks:toggle", () -> {
				int pos = codeArea.getCaretPosition();
				int line = 1;
				try {
					line = codeArea.getLineOfOffset(pos) + 1;
				} catch (Exception ex) {
					// ignore
				}
				openBookmarkDialog(codeArea, line, pos);
			});

			JPopupMenu popup = codeArea.getPopupMenu();
			if (popup != null) {
				JMenuItem bookmarkItem = new JMenuItem("Add/Edit Bookmark (Alt+M)");
				bookmarkItem.setIcon(BookmarkEmoji.STAR.getIcon());
				bookmarkItem.setAccelerator(altM);
				bookmarkItem.addActionListener(e -> {
					int pos = UiUtils.getOffsetAtMousePosition(codeArea);
					if (pos < 0) {
						pos = codeArea.getCaretPosition();
					}
					int line = 1;
					try {
						line = codeArea.getLineOfOffset(pos) + 1;
					} catch (Exception ex) {
						// ignore
					}
					openBookmarkDialog(codeArea, line, pos);
				});
				popup.addSeparator();
				popup.add(bookmarkItem);
			}

			// When document content is loaded/changed, refresh gutter icons
			codeArea.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					SwingUtilities.invokeLater(() -> updateGutter(codeArea));
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					SwingUtilities.invokeLater(() -> updateGutter(codeArea));
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					SwingUtilities.invokeLater(() -> updateGutter(codeArea));
				}
			});

			// When codeArea becomes showing, refresh gutter icons
			codeArea.addHierarchyListener(e -> {
				if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && codeArea.isShowing()) {
					SwingUtilities.invokeLater(() -> updateGutter(codeArea));
				}
			});
		}
		attachToGutter(codeArea);
	}

	public void attachToGutter(AbstractCodeArea codeArea) {
		if (codeArea == null) {
			return;
		}
		Gutter gutter = RSyntaxUtilities.getGutter(codeArea);
		if (gutter == null) {
			return;
		}
		// Register listener once
		if (gutter.getClientProperty("jadx-bookmarks:listener-attached") == null) {
			gutter.putClientProperty("jadx-bookmarks:listener-attached", Boolean.TRUE);
			gutter.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent me) {
					try {
						int offset = codeArea.viewToModel(new Point(0, me.getY()));
						int line = codeArea.getLineOfOffset(offset) + 1;
						codeArea.setCaretPosition(offset);
						codeArea.requestFocus();
						if (me.getClickCount() == 2 || SwingUtilities.isRightMouseButton(me)) {
							openBookmarkDialog(codeArea, line, offset);
						}
					} catch (Exception ex) {
						// ignore
					}
				}
			});
		}
		updateGutter(codeArea);
	}

	public void updateGutter(AbstractCodeArea codeArea) {
		if (codeArea == null) {
			return;
		}
		Gutter gutter = RSyntaxUtilities.getGutter(codeArea);
		if (gutter == null) {
			return;
		}

		@SuppressWarnings("unchecked")
		List<GutterIconInfo> myIcons = (List<GutterIconInfo>) codeArea.getClientProperty("jadx-bookmarks:icons");
		if (myIcons != null) {
			for (GutterIconInfo gi : myIcons) {
				try {
					gutter.removeTrackingIcon(gi);
				} catch (Exception ignored) {
				}
			}
			myIcons.clear();
		} else {
			myIcons = new ArrayList<>();
			codeArea.putClientProperty("jadx-bookmarks:icons", myIcons);
		}

		JNode node = codeArea.getNode();
		if (node == null) {
			gutter.revalidate();
			gutter.repaint();
			return;
		}

		List<BookmarkData> bms = getBookmarksForNode(node);
		if (bms.isEmpty()) {
			gutter.revalidate();
			gutter.repaint();
			return;
		}

		gutter.setIconRowHeaderEnabled(true);
		gutter.setIconRowHeaderInheritsGutterBackground(true);
		for (BookmarkData b : bms) {
			int line0 = b.getLine() - 1;
			if (line0 >= 0 && line0 < codeArea.getLineCount()) {
				try {
					Icon icon = b.getBookmarkEmoji().getIcon();
					String tip = b.getPlaceString() + ": " + b.getDescription();
					GutterIconInfo info = gutter.addLineTrackingIcon(line0, icon, tip);
					if (info != null) {
						myIcons.add(info);
					}
				} catch (Exception e) {
					LOG.debug("Failed to add tracking icon for bookmark at line {}", b.getLine(), e);
				}
			}
		}
		gutter.revalidate();
		gutter.repaint();
		codeArea.repaint();
	}

	public void refreshAllGutters() {
		UiUtils.uiRun(() -> {
			for (ContentPanel tab : mainWindow.getTabbedPane().getTabs()) {
				if (tab instanceof ClassCodeContentPanel) {
					ClassCodeContentPanel ccp = (ClassCodeContentPanel) tab;
					attachToCodeArea(ccp.getJavaCodePanel().getCodeArea());
					attachToCodeArea(ccp.getSmaliCodeArea());

					// Hook into inner tab switching (Code vs Smali)
					if (tab.getClientProperty("jadx-bookmarks:inner-tabs-hooked") == null) {
						tab.putClientProperty("jadx-bookmarks:inner-tabs-hooked", Boolean.TRUE);
						hookInnerTabs(tab);
					}
				} else if (tab instanceof AbstractCodeContentPanel) {
					AbstractCodeArea ca = ((AbstractCodeContentPanel) tab).getCodeArea();
					if (ca != null) {
						attachToCodeArea(ca);
					}
				}
			}
		});
	}

	private void hookInnerTabs(Component comp) {
		if (comp instanceof JTabbedPane) {
			((JTabbedPane) comp).addChangeListener(e -> {
				SwingUtilities.invokeLater(() -> refreshAllGutters());
			});
		}
		if (comp instanceof java.awt.Container) {
			for (Component child : ((java.awt.Container) comp).getComponents()) {
				hookInnerTabs(child);
			}
		}
	}

	public void loadBookmarks() {
		try {
			Path storageFile = getStorageFilePath();
			if (storageFile != null && Files.exists(storageFile)) {
				String json = Files.readString(storageFile, StandardCharsets.UTF_8);
				Type type = new TypeToken<List<BookmarkData>>() {}.getType();
				List<BookmarkData> loaded = GSON.fromJson(json, type);
				if (loaded != null) {
					bookmarks.clear();
					bookmarks.addAll(loaded);
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to load bookmarks", e);
		}
	}

	public void saveBookmarks() {
		try {
			Path storageFile = getStorageFilePath();
			if (storageFile != null) {
				Files.createDirectories(storageFile.getParent());
				String json = GSON.toJson(bookmarks);
				Files.writeString(storageFile, json, StandardCharsets.UTF_8);
			}
		} catch (Exception e) {
			LOG.error("Failed to save bookmarks", e);
		}
	}

	private @Nullable Path getStorageFilePath() {
		try {
			Path dir;
			if (pluginContext != null && pluginContext.files() != null) {
				dir = pluginContext.files().getPluginConfigDir();
			} else {
				dir = Path.of(System.getProperty("user.home"), ".config", "jadx", "plugins", "jadx-bookmarks");
			}
			String projectKey = getProjectKey();
			return dir.resolve("bookmarks_" + projectKey + ".json");
		} catch (Exception e) {
			return null;
		}
	}

	private String getProjectKey() {
		try {
			if (mainWindow != null && mainWindow.getProject() != null) {
				if (mainWindow.getProject().getProjectPath() != null) {
					return mainWindow.getProject().getProjectPath().getFileName().toString().replaceAll("[^a-zA-Z0-9_.-]", "_");
				}
				List<Path> filePaths = mainWindow.getProject().getFilePaths();
				if (filePaths != null && !filePaths.isEmpty()) {
					return filePaths.get(0).getFileName().toString().replaceAll("[^a-zA-Z0-9_.-]", "_");
				}
			}
			if (pluginContext != null && pluginContext.getArgs() != null) {
				List<File> files = pluginContext.getArgs().getInputFiles();
				if (files != null && !files.isEmpty()) {
					return files.get(0).getName().replaceAll("[^a-zA-Z0-9_.-]", "_");
				}
			}
		} catch (Exception e) {
			// ignore
		}
		return "default";
	}
}
