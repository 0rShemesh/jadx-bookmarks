package jadx.plugins.bookmarks;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum BookmarkEmoji {
	STAR("STAR", "star.png", "⭐", "Star emoji - bookmarked"),
	POSITIVE_CHECKMARK("POSITIVE_CHECKMARK", "positive_checkmark.png", "✅", "Positive checkmark"),
	RED_X("RED_X", "red_x.png", "❌", "Red X mark"),
	THUMBS_UP("THUMBS_UP", "thumbs_up.png", "👍", "Thumbs up"),
	THUMBS_DOWN("THUMBS_DOWN", "thumbs_down.png", "👎", "thumbs down"),
	CROWN("CROWN", "crown.png", "👑", "Crown"),
	RED_FLAG("RED_FLAG", "red_flag.png", "🚩", "Red flag"),
	GREEN_FLAG("GREEN_FLAG", "green_flag.png", "⛳", "green flag"),
	NUM_1("NUM_1", "num_1.png", "1️⃣", "1 number emoji"),
	NUM_2("NUM_2", "num_2.png", "2️⃣", "2 number emoji"),
	NUM_3("NUM_3", "num_3.png", "3️⃣", "3 number emoji"),
	POP("POP", "pop.png", "💩", "Pop emoji"),
	WHALE("WHALE", "whale.png", "🐳", "Whale"),
	MONKEY("MONKEY", "monkey.png", "🐵", "Monkey"),
	HAPPY_FACE("HAPPY_FACE", "happy_face.png", "😊", "Happy Face"),
	CRY_FACE("CRY_FACE", "cry_face.png", "😢", "Cry Face"),
	HEARTS_IN_EYES("HEARTS_IN_EYES", "hearts_in_eyes.png", "😍", "Hearts in the eyes face");

	private static final Logger LOG = LoggerFactory.getLogger(BookmarkEmoji.class);

	private final String key;
	private final String imageFileName;
	private final String emoji;
	private final String description;
	private transient ImageIcon icon16;
	private transient ImageIcon icon20;

	BookmarkEmoji(String key, String imageFileName, String emoji, String description) {
		this.key = key;
		this.imageFileName = imageFileName;
		this.emoji = emoji;
		this.description = description;
	}

	public String getKey() {
		return key;
	}

	public String getEmoji() {
		return emoji;
	}

	public String getDescription() {
		return description;
	}

	public String getDisplayText() {
		return emoji + "  " + description;
	}

	public synchronized ImageIcon getIcon() {
		if (icon16 == null) {
			icon16 = loadIcon(16);
		}
		return icon16;
	}

	public synchronized ImageIcon getLargeIcon() {
		if (icon20 == null) {
			icon20 = loadIcon(20);
		}
		return icon20;
	}

	private ImageIcon loadIcon(int size) {
		String resPath = "/icons/emojis/" + imageFileName;
		try (InputStream is = BookmarkEmoji.class.getResourceAsStream(resPath)) {
			if (is != null) {
				BufferedImage original = ImageIO.read(is);
				if (original != null) {
					BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g2 = result.createGraphics();
					g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
					g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g2.drawImage(original, 0, 0, size, size, null);
					g2.dispose();
					return new ImageIcon(result);
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to load emoji image: {}", resPath, e);
		}
		return createFallbackIcon(emoji, size);
	}

	public static BookmarkEmoji getByKey(String key) {
		if (key != null) {
			for (BookmarkEmoji be : values()) {
				if (be.key.equalsIgnoreCase(key) || be.name().equalsIgnoreCase(key) || be.emoji.equals(key)) {
					return be;
				}
			}
		}
		return STAR;
	}

	public static ImageIcon createIcon(String emoji, int size) {
		for (BookmarkEmoji be : values()) {
			if (be.emoji.equals(emoji) || be.key.equalsIgnoreCase(emoji)) {
				return be.getIcon();
			}
		}
		return createFallbackIcon(emoji, size);
	}

	private static ImageIcon createFallbackIcon(String emoji, int size) {
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = img.createGraphics();
		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setColor(new Color(60, 60, 60));
			g2.setFont(new Font(Font.DIALOG, Font.BOLD, Math.max(10, size - 4)));
			FontMetrics fm = g2.getFontMetrics();
			int strWidth = fm.stringWidth(emoji);
			int drawX = Math.max(0, (size - strWidth) / 2);
			int drawY = (size - fm.getHeight()) / 2 + fm.getAscent();
			g2.drawString(emoji, drawX, drawY);
		} finally {
			g2.dispose();
		}
		return new ImageIcon(img);
	}

	@Override
	public String toString() {
		return getDisplayText();
	}
}
