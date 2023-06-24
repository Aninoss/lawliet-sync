package core.util;

public class StringUtil {

    private StringUtil() {
    }

    public static boolean stringIsInt(String string) {
        try {
            Integer.parseInt(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String shortenString(String str, int limit) {
        return shortenString(str, limit, "…", false);
    }

    public static String shortenStringLine(String str, int limit) {
        return shortenString(str, limit, "\n…", true);
    }

    public static String shortenString(String str, int limit, String postfix, boolean focusLineBreak) {
        if (str.length() <= limit) {
            return str;
        }

        while (str.length() > limit - postfix.length() && str.contains("\n")) {
            int pos = str.lastIndexOf("\n");
            str = str.substring(0, pos);
        }

        if (!focusLineBreak) {
            while (str.length() > limit - postfix.length() && str.contains(" ")) {
                int pos = str.lastIndexOf(" ");
                str = str.substring(0, pos);
            }
        }

        while (str.length() > 0 && (str.charAt(str.length() - 1) == ',' || str.charAt(str.length() - 1) == '.' || str.charAt(str.length() - 1) == ' ' || str.charAt(str.length() - 1) == '\n')) {
            str = str.substring(0, str.length() - 1);
        }

        return str.substring(0, Math.min(str.length(), limit - postfix.length())) + postfix;
    }

}
