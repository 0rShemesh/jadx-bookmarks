package jadx.plugins.bookmarks;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import static org.assertj.core.api.Assertions.assertThat;

class BookmarkDataTest {

	@Test
	public void testEmojiCountAndDefaults() {
		BookmarkEmoji[] values = BookmarkEmoji.values();
		assertThat(values).hasSize(17);

		assertThat(BookmarkEmoji.STAR.getKey()).isEqualTo("STAR");
		assertThat(BookmarkEmoji.STAR.getEmoji()).isEqualTo("⭐");
		assertThat(BookmarkEmoji.STAR.getDescription()).isEqualTo("Star emoji - bookmarked");

		// Default fallback
		assertThat(BookmarkEmoji.getByKey(null)).isEqualTo(BookmarkEmoji.STAR);
		assertThat(BookmarkEmoji.getByKey("NON_EXISTENT")).isEqualTo(BookmarkEmoji.STAR);

		// Case-insensitive lookup
		assertThat(BookmarkEmoji.getByKey("star")).isEqualTo(BookmarkEmoji.STAR);
		assertThat(BookmarkEmoji.getByKey("positive_checkmark")).isEqualTo(BookmarkEmoji.POSITIVE_CHECKMARK);
		assertThat(BookmarkEmoji.getByKey("✅")).isEqualTo(BookmarkEmoji.POSITIVE_CHECKMARK);

		// Icon generation
		for (BookmarkEmoji be : values) {
			assertThat(be.getIcon()).isNotNull();
			assertThat(be.getIcon().getIconWidth()).isEqualTo(16);
			assertThat(be.getIcon().getIconHeight()).isEqualTo(16);
			assertThat(be.getDisplayText()).contains(be.getEmoji());
		}
	}

	@Test
	public void testBookmarkDataModel() {
		BookmarkData b1 = new BookmarkData("com.example.MainActivity", "MainActivity", "onCreate", 42,
				"Init view", BookmarkEmoji.RED_FLAG.getKey());

		assertThat(b1.getRawClsName()).isEqualTo("com.example.MainActivity");
		assertThat(b1.getClsName()).isEqualTo("MainActivity");
		assertThat(b1.getFuncName()).isEqualTo("onCreate");
		assertThat(b1.getLine()).isEqualTo(42);
		assertThat(b1.getDescription()).isEqualTo("Init view");
		assertThat(b1.getIconKey()).isEqualTo("RED_FLAG");
		assertThat(b1.getBookmarkEmoji()).isEqualTo(BookmarkEmoji.RED_FLAG);
		assertThat(b1.getPlaceString()).isEqualTo("MainActivity:onCreate:42");

		assertThat(b1.matchesClass("MainActivity")).isTrue();
		assertThat(b1.matchesClass("com.example.MainActivity")).isTrue();
		assertThat(b1.matchesClass("OtherClass")).isFalse();
	}

	@Test
	public void testBookmarkEquality() {
		BookmarkData b1 = new BookmarkData("com.example.MainActivity", "MainActivity", "onCreate", 42,
				"Init view", BookmarkEmoji.RED_FLAG.getKey());
		BookmarkData b2 = new BookmarkData("com.example.MainActivity", "MainActivity", "differentFunc", 42,
				"Updated description", BookmarkEmoji.STAR.getKey());
		BookmarkData b3 = new BookmarkData("com.example.MainActivity", "MainActivity", "onCreate", 43,
				"Init view", BookmarkEmoji.RED_FLAG.getKey());
		BookmarkData b4 = new BookmarkData("com.example.Other", "Other", "onCreate", 42,
				"Init view", BookmarkEmoji.RED_FLAG.getKey());

		// Equality by class and line
		assertThat(b1).isEqualTo(b2);
		assertThat(b1.hashCode()).isEqualTo(b2.hashCode());

		assertThat(b1).isNotEqualTo(b3);
		assertThat(b1).isNotEqualTo(b4);
	}

	@Test
	public void testSerialization() {
		List<BookmarkData> list = new ArrayList<>();
		list.add(new BookmarkData("com.foo.Bar", "Bar", "run", 10, "thread starter", "CROWN"));
		list.add(new BookmarkData("com.foo.Baz", "Baz", null, 55, "note", "POP"));

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(list);
		assertThat(json).contains("thread starter");
		assertThat(json).contains("CROWN");
		assertThat(json).contains("POP");

		Type type = new TypeToken<List<BookmarkData>>() {}.getType();
		List<BookmarkData> deserialized = gson.fromJson(json, type);
		assertThat(deserialized).hasSize(2);

		BookmarkData loaded1 = deserialized.get(0);
		assertThat(loaded1.getRawClsName()).isEqualTo("com.foo.Bar");
		assertThat(loaded1.getClsName()).isEqualTo("Bar");
		assertThat(loaded1.getFuncName()).isEqualTo("run");
		assertThat(loaded1.getLine()).isEqualTo(10);
		assertThat(loaded1.getDescription()).isEqualTo("thread starter");
		assertThat(loaded1.getIconKey()).isEqualTo("CROWN");
		assertThat(loaded1.getBookmarkEmoji()).isEqualTo(BookmarkEmoji.CROWN);
	}
}
