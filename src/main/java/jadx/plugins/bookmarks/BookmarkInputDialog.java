package jadx.plugins.bookmarks;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jetbrains.annotations.Nullable;

public class BookmarkInputDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	public static class BookmarkInputResult {
		private final String description;
		private final BookmarkEmoji emoji;

		public BookmarkInputResult(String description, BookmarkEmoji emoji) {
			this.description = description;
			this.emoji = emoji;
		}

		public String getDescription() {
			return description;
		}

		public BookmarkEmoji getEmoji() {
			return emoji;
		}
	}

	private final JTextField inputField;
	private final JComboBox<BookmarkEmoji> emojiComboBox;
	private boolean okPressed = false;

	public static @Nullable BookmarkInputResult show(Component parent, @Nullable BookmarkData existing) {
		Frame frame = JOptionPaneFrame(parent);
		BookmarkInputDialog dialog = new BookmarkInputDialog(frame, existing);
		dialog.setVisible(true);
		if (dialog.okPressed) {
			return new BookmarkInputResult(dialog.inputField.getText().trim(),
					(BookmarkEmoji) dialog.emojiComboBox.getSelectedItem());
		}
		return null;
	}

	private static @Nullable Frame JOptionPaneFrame(Component parent) {
		if (parent instanceof Frame) {
			return (Frame) parent;
		}
		return (Frame) SwingUtilities.getAncestorOfClass(Frame.class, parent);
	}

	public BookmarkInputDialog(@Nullable Frame parent, @Nullable BookmarkData existing) {
		super(parent, "Please enter a string", true);

		// Layout
		JPanel mainPanel = new JPanel(new BorderLayout(10, 15));
		mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		// Top panel: Label + Input
		JPanel topPanel = new JPanel(new BorderLayout(10, 5));
		JLabel label = new JLabel("Enter mark description");
		inputField = new JTextField(30);
		if (existing != null) {
			inputField.setText(existing.getDescription());
		}
		topPanel.add(label, BorderLayout.WEST);
		topPanel.add(inputField, BorderLayout.CENTER);

		// Bottom panel: Left = emoji dropdown, Right = Cancel & Ok buttons
		JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));

		emojiComboBox = new JComboBox<>(BookmarkEmoji.values());
		emojiComboBox.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index,
					boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof BookmarkEmoji) {
					BookmarkEmoji be = (BookmarkEmoji) value;
					setIcon(be.getLargeIcon());
					setText("  " + be.getDescription());
				}
				return this;
			}
		});
		if (existing != null) {
			emojiComboBox.setSelectedItem(existing.getBookmarkEmoji());
		} else {
			emojiComboBox.setSelectedItem(BookmarkEmoji.STAR);
		}
		bottomPanel.add(emojiComboBox, BorderLayout.WEST);

		// Buttons
		JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		JButton cancelButton = new JButton("Cancel");
		JButton okButton = new JButton("OK");

		cancelButton.addActionListener(e -> {
			okPressed = false;
			dispose();
		});

		okButton.addActionListener(e -> {
			okPressed = true;
			dispose();
		});

		buttonsPanel.add(cancelButton);
		buttonsPanel.add(okButton);
		bottomPanel.add(buttonsPanel, BorderLayout.EAST);

		mainPanel.add(topPanel, BorderLayout.CENTER);
		mainPanel.add(bottomPanel, BorderLayout.SOUTH);

		getContentPane().add(mainPanel);

		getRootPane().setDefaultButton(okButton);

		// Keyboard actions
		inputField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					okButton.doClick();
				} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					cancelButton.doClick();
				}
			}
		});

		emojiComboBox.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					okButton.doClick();
				} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					cancelButton.doClick();
				}
			}
		});

		pack();
		setResizable(false);
		setLocationRelativeTo(parent);

		SwingUtilities.invokeLater(() -> {
			inputField.requestFocusInWindow();
			inputField.selectAll();
		});
	}
}
