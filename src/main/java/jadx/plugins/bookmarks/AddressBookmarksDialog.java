package jadx.plugins.bookmarks;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.jetbrains.annotations.Nullable;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.UiUtils;

public class AddressBookmarksDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	private static AddressBookmarksDialog openInstance = null;

	private final MainWindow mainWindow;
	private final BookmarkManager bookmarkManager;
	private final BookmarksTableModel tableModel;
	private final JTable table;
	private final JCheckBox keepOpenCheckBox;

	public static synchronized void show(MainWindow mainWindow, BookmarkManager bookmarkManager) {
		if (openInstance != null && openInstance.isDisplayable()) {
			openInstance.refreshTable();
			openInstance.toFront();
			openInstance.requestFocus();
			return;
		}
		openInstance = new AddressBookmarksDialog(mainWindow, bookmarkManager);
		openInstance.setVisible(true);
	}

	public AddressBookmarksDialog(MainWindow mainWindow, BookmarkManager bookmarkManager) {
		super(mainWindow, "Address bookmarks", false);
		this.mainWindow = mainWindow;
		this.bookmarkManager = bookmarkManager;

		JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Table Model & JTable
		tableModel = new BookmarksTableModel(bookmarkManager.getAllBookmarks());
		table = new JTable(tableModel);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(24);
		table.getTableHeader().setReorderingAllowed(false);

		// Renderer for Place column (Column 0: Icon + Text)
		table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				BookmarkData bm = tableModel.getBookmarkAt(row);
				if (bm != null) {
					setIcon(bm.getBookmarkEmoji().getIcon());
					setText(bm.getPlaceString());
				} else {
					setIcon(null);
				}
				return this;
			}
		});

		// Renderer for Description column (Column 1)
		table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				setIcon(null);
				return this;
			}
		});

		table.getColumnModel().getColumn(0).setPreferredWidth(300);
		table.getColumnModel().getColumn(1).setPreferredWidth(450);

		// Double-click to jump
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
					int row = table.getSelectedRow();
					if (row >= 0) {
						jumpToSelected();
					}
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				handlePopup(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				handlePopup(e);
			}

			private void handlePopup(MouseEvent e) {
				if (e.isPopupTrigger()) {
					int row = table.rowAtPoint(e.getPoint());
					if (row >= 0) {
						table.setRowSelectionInterval(row, row);
						showPopupMenu(e.getPoint());
					}
				}
			}
		});

		// Keyboard actions on JTable
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_ENTER:
						e.consume();
						jumpToSelected();
						break;
					case KeyEvent.VK_INSERT:
						e.consume();
						modifySelected();
						break;
					case KeyEvent.VK_DELETE:
						e.consume();
						deleteSelected();
						break;
					case KeyEvent.VK_ESCAPE:
						e.consume();
						closeDialog();
						break;
					case KeyEvent.VK_C:
						if ((e.getModifiersEx() & UiUtils.ctrlButton()) != 0) {
							e.consume();
							copySelectedLine();
						}
						break;
					default:
						break;
				}
			}
		});

		// Also register Ctrl+C, Ins, Del, Enter, Esc via InputMap / ActionMap
		InputMap im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap am = table.getActionMap();

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "jump");
		am.put("jump", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				jumpToSelected();
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "modify");
		am.put("modify", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				modifySelected();
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		am.put("delete", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				deleteSelected();
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, UiUtils.ctrlButton()), "copyLine");
		am.put("copyLine", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				copySelectedLine();
			}
		});

		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
		am.put("close", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				closeDialog();
			}
		});

		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setPreferredSize(new Dimension(750, 400));
		contentPanel.add(scrollPane, BorderLayout.CENTER);

		// Bottom panel: Checkbox on left, Cancel & Ok on right
		JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));

		keepOpenCheckBox = new JCheckBox("keep open");
		bottomPanel.add(keepOpenCheckBox, BorderLayout.WEST);

		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		JButton cancelButton = new JButton("X Cancel");
		JButton okButton = new JButton("V Ok");

		cancelButton.addActionListener(e -> closeDialog());
		okButton.addActionListener(e -> {
			int row = table.getSelectedRow();
			if (row >= 0) {
				jumpToSelected();
			} else {
				closeDialog();
			}
		});

		buttonsPanel.add(cancelButton);
		buttonsPanel.add(okButton);
		bottomPanel.add(buttonsPanel, BorderLayout.EAST);

		contentPanel.add(bottomPanel, BorderLayout.SOUTH);

		getContentPane().add(contentPanel);

		pack();
		setMinimumSize(new Dimension(500, 300));
		setLocationRelativeTo(mainWindow);

		if (tableModel.getRowCount() > 0) {
			table.setRowSelectionInterval(0, 0);
		}

		SwingUtilities.invokeLater(table::requestFocusInWindow);
	}

	private void showPopupMenu(Point p) {
		int row = table.getSelectedRow();
		if (row < 0) {
			return;
		}
		BookmarkData bm = tableModel.getBookmarkAt(row);
		if (bm == null) {
			return;
		}

		JPopupMenu popup = new JPopupMenu();

		JMenuItem modifyItem = new JMenuItem("Modify comment (Ins shortcut)");
		modifyItem.addActionListener(e -> modifySelected());
		popup.add(modifyItem);

		JMenuItem deleteItem = new JMenuItem("Delete (Del shortcut)");
		deleteItem.addActionListener(e -> deleteSelected());
		popup.add(deleteItem);

		JMenuItem copyItem = new JMenuItem("Copy (copy the line)");
		copyItem.addActionListener(e -> copySelectedLine());
		popup.add(copyItem);

		popup.addSeparator();

		JMenuItem jumpItem = new JMenuItem("Jump to bookmark");
		jumpItem.addActionListener(e -> jumpToSelected());
		popup.add(jumpItem);

		popup.show(table, p.x, p.y);
	}

	private void jumpToSelected() {
		int row = table.getSelectedRow();
		if (row < 0) {
			return;
		}
		BookmarkData bm = tableModel.getBookmarkAt(row);
		if (bm != null) {
			bookmarkManager.jumpToBookmark(bm);
			if (!keepOpenCheckBox.isSelected()) {
				closeDialog();
			}
		}
	}

	private void modifySelected() {
		int row = table.getSelectedRow();
		if (row < 0) {
			return;
		}
		BookmarkData bm = tableModel.getBookmarkAt(row);
		if (bm == null) {
			return;
		}
		BookmarkInputDialog.BookmarkInputResult result = BookmarkInputDialog.show(this, bm);
		if (result != null) {
			bm.setDescription(result.getDescription());
			bm.setIconKey(result.getEmoji().getKey());
			bookmarkManager.bookmarkChanged(bm);
			refreshTable();
		}
	}

	private void deleteSelected() {
		int row = table.getSelectedRow();
		if (row < 0) {
			return;
		}
		BookmarkData bm = tableModel.getBookmarkAt(row);
		if (bm != null) {
			bookmarkManager.removeBookmark(bm);
			refreshTable();
			if (tableModel.getRowCount() > 0) {
				int nextRow = Math.min(row, tableModel.getRowCount() - 1);
				table.setRowSelectionInterval(nextRow, nextRow);
			}
		}
	}

	private void copySelectedLine() {
		int row = table.getSelectedRow();
		if (row < 0) {
			return;
		}
		BookmarkData bm = tableModel.getBookmarkAt(row);
		if (bm != null) {
			String lineText = bm.getPlaceString() + " " + bm.getDescription();
			UiUtils.copyToClipboard(lineText);
		}
	}

	public void refreshTable() {
		tableModel.update(bookmarkManager.getAllBookmarks());
	}

	private void closeDialog() {
		dispose();
		openInstance = null;
	}

	@Override
	public void dispose() {
		super.dispose();
		openInstance = null;
	}

	private static class BookmarksTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;

		private final String[] columnNames = new String[] {
				"Place",
				"Description"
		};

		private List<BookmarkData> list;

		public BookmarksTableModel(List<BookmarkData> list) {
			this.list = new ArrayList<>(list);
		}

		public void update(List<BookmarkData> newList) {
			this.list = new ArrayList<>(newList);
			fireTableDataChanged();
		}

		public @Nullable BookmarkData getBookmarkAt(int row) {
			if (row >= 0 && row < list.size()) {
				return list.get(row);
			}
			return null;
		}

		@Override
		public int getRowCount() {
			return list.size();
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public String getColumnName(int column) {
			return columnNames[column];
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			BookmarkData b = list.get(rowIndex);
			if (columnIndex == 0) {
				return b.getPlaceString();
			} else if (columnIndex == 1) {
				return b.getDescription();
			}
			return "";
		}
	}
}
