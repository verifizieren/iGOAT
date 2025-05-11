package igoat.client;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for loading text in different languages
 */
public class LanguageManager {

    private static final Logger logger = LoggerFactory.getLogger(LanguageManager.class);
    private static volatile LanguageManager instance;
    private final ResourceBundle bundle;

    private LanguageManager(String baseName, Locale locale) {
        ResourceBundle tempBundle;
        try {
            tempBundle = ResourceBundle.getBundle(baseName, locale);
        } catch (MissingResourceException e) {
            logger.error("Language file not found: {}_{}", baseName, locale);
            tempBundle = null;
        }
        this.bundle = tempBundle;
    }

    /**
     * Initializes the singleton instance. Can only be called once. Subsequent calls will return the
     * already-initialized instance.
     */
    public static LanguageManager init(String baseName, Locale locale) {
        if (instance == null) {
            synchronized (LanguageManager.class) {
                if (instance == null) {
                    instance = new LanguageManager(baseName, locale);
                }
            }
        }
        return instance;
    }

    /**
     * Returns the initialized singleton instance. Throws if called before initialization.
     */
    public static LanguageManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("LanguageManager not initialized. Call init() first.");
        }
        return instance;
    }

    /**
     * Returns the localized string for the given key. Returns the key itself if not found or bundle
     * not loaded.
     */
    public String get(String key) {
        if (bundle == null) {
            return key;
        }
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    // Example usage
    public static void main(String[] args) {
        LanguageManager.init("lang.messages", Locale.ENGLISH);
        LanguageManager lm = LanguageManager.getInstance();

        System.out.println(lm.get("game.title"));
        System.out.println(lm.get("game.start"));
    }
}

