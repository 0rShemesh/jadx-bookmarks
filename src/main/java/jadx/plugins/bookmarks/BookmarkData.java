package jadx.plugins.bookmarks;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

public class BookmarkData {
	private String rawClsName;
	private String clsName;
	private String funcName;
	private int line;
	private String description;
	private String iconKey;

	public BookmarkData() {
		this("", "", "", 1, "", BookmarkEmoji.STAR.getKey());
	}

	public BookmarkData(String rawClsName, String clsName, @Nullable String funcName, int line,
			String description, String iconKey) {
		this.rawClsName = rawClsName != null ? rawClsName : "";
		this.clsName = clsName != null ? clsName : "";
		this.funcName = funcName != null ? funcName : "";
		this.line = line;
		this.description = description != null ? description : "";
		this.iconKey = iconKey != null ? iconKey : BookmarkEmoji.STAR.getKey();
	}

	public String getRawClsName() {
		return rawClsName;
	}

	public void setRawClsName(String rawClsName) {
		this.rawClsName = rawClsName;
	}

	public String getClsName() {
		return clsName;
	}

	public void setClsName(String clsName) {
		this.clsName = clsName;
	}

	public String getFuncName() {
		return funcName;
	}

	public void setFuncName(String funcName) {
		this.funcName = funcName;
	}

	public int getLine() {
		return line;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getIconKey() {
		return iconKey;
	}

	public void setIconKey(String iconKey) {
		this.iconKey = iconKey;
	}

	public BookmarkEmoji getBookmarkEmoji() {
		return BookmarkEmoji.getByKey(iconKey);
	}

	/**
	 * Formats location as class:function:number_of_line
	 */
	public String getPlaceString() {
		String cls = !clsName.isEmpty() ? clsName : rawClsName;
		String func = funcName != null ? funcName : "";
		return cls + ":" + func + ":" + line;
	}

	public boolean matchesClass(String... names) {
		for (String name : names) {
			if (name != null && !name.isEmpty()) {
				if (name.equals(rawClsName) || name.equals(clsName)) {
					return true;
				}
				String simpleCls = getSimpleName(clsName);
				String simpleRaw = getSimpleName(rawClsName);
				String simpleInput = getSimpleName(name);
				if (!simpleInput.isEmpty() && (simpleInput.equals(simpleCls) || simpleInput.equals(simpleRaw))) {
					return true;
				}
			}
		}
		return false;
	}

	private static String getSimpleName(String fullName) {
		if (fullName == null || fullName.isEmpty()) {
			return "";
		}
		int lastDot = fullName.lastIndexOf('.');
		int lastSlash = fullName.lastIndexOf('/');
		int idx = Math.max(lastDot, lastSlash);
		return idx >= 0 && idx < fullName.length() - 1 ? fullName.substring(idx + 1) : fullName;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BookmarkData)) {
			return false;
		}
		BookmarkData that = (BookmarkData) o;
		if (line != that.line) {
			return false;
		}
		if (!clsName.isEmpty() && !that.clsName.isEmpty() && Objects.equals(clsName, that.clsName)) {
			return true;
		}
		if (!rawClsName.isEmpty() && !that.rawClsName.isEmpty() && Objects.equals(rawClsName, that.rawClsName)) {
			return true;
		}
		return Objects.equals(clsName, that.clsName) && Objects.equals(rawClsName, that.rawClsName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(!clsName.isEmpty() ? clsName : rawClsName, line);
	}

	@Override
	public String toString() {
		return getPlaceString() + " - " + description;
	}
}
