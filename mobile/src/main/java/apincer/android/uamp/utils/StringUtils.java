package apincer.android.uamp.utils;

import java.text.BreakIterator;
import java.util.List;
import java.util.Locale;

public class StringUtils {
    public static final String UNKNOWN = "<unknown>";
    public static final String UNKNOWN_CAP = "<Unknown>";
    public static final String UNKNOWN_ALL_CAP = "<UNKNOWN>";


    public static String trimTitle(String text) {
        if(text == null) return "";
        if("-".equals(text)) return "";
        //if(UNKNOWN.equalsIgnoreCase(text)) return "";
        text = StringUtils.remove(text, UNKNOWN);
        text = StringUtils.remove(text, UNKNOWN_ALL_CAP);
        text = StringUtils.remove(text, UNKNOWN_CAP);
        if("-/-".equals(text)) return "";
        return StringUtils.trimToEmpty(text);
    }



    public static boolean isEmpty(String input) {
        if(input == null) {
            return true;
        }else if(input.trim().length()==0) {
            return true;
        }
        return false;
    }

    public static String capitalize(final String str, final char... delimiters) {
        final int delimLen = delimiters == null ? -1 : delimiters.length;
        if (StringUtils.isEmpty(str) || delimLen == 0) {
            return str;
        }
        if(str.equals("VA")) return str;
        if(str.startsWith("VA ")) return str;

        final char[] buffer = str.toCharArray();
        boolean capitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (isDelimiter(ch, delimiters)) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch);
                capitalizeNext = false;
            }else {
                buffer[i] = Character.toLowerCase(ch);
            }
        }
        return new String(buffer);
    }

    public static String uncapitalize(final String str, final char... delimiters) {
        final int delimLen = delimiters == null ? -1 : delimiters.length;
        if (StringUtils.isEmpty(str) || delimLen == 0) {
            return str;
        }
        final char[] buffer = str.toCharArray();
        boolean uncapitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (isDelimiter(ch, delimiters)) {
                uncapitalizeNext = true;
            } else if (uncapitalizeNext) {
                buffer[i] = Character.toLowerCase(ch);
                uncapitalizeNext = false;
            }
        }
        return new String(buffer);
    }

    /**
     * Is the character a delimiter.
     *
     * @param ch  the character to check
     * @param delimiters  the delimiters
     * @return true if it is a delimiter
     */
    private static boolean isDelimiter(final char ch, final char[] delimiters) {
        if (delimiters == null) {
            return Character.isWhitespace(ch);
        }
        for (final char delimiter : delimiters) {
            if (ch == delimiter) {
                return true;
            }
        }
        return false;
    }

    public static String truncate(String input, int maxLength) {
        if(input == null) {
            return "";
        }else if (input.length() <= maxLength) {
            return input;
        } else {
            return input.substring(0, maxLength - 3) + "...";
        }
    }

    public static boolean compare(String s1, String s2) {
        if(isEmpty(s1) && !isEmpty(s2)) return false; // first is null
        if(isEmpty(s2)) return true; // do not compare
        s1 = s1.trim();
        s2 = s2.trim();

        return s1.equalsIgnoreCase(s2);
    }

    public static boolean contains(String s1, String s2) {
	if(StringUtils.isEmpty(s1) || StringUtils.isEmpty(s2)) {
		return false;
	}
	s1 = s1.trim().toLowerCase();
	s2 = s2.trim().toLowerCase();

        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return true; /* both strings are zero length */
        }
        return longer.contains(shorter);
    }

    /**
     * Calculates the similarity (a number within 0 and 1) between two strings.
     */
    public static double similarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0; /* both strings are zero length */
        }
    /* // If you have StringUtils, you can use it to calculate the edit distance:
    return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) /
                               (double) longerLength; */
        return (longerLength - editDistance(longer, shorter)) / (double) longerLength;

    }

    // Example implementation of the Levenshtein Edit Distance
    // See http://rosettacode.org/wiki/Levenshtein_distance#Java
    public static int editDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue),
                                    costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    public static String getFirstWord(String text) {
       // String firstWord = text.split("\\W")[0];
        Locale thaiLocale = new Locale("th");
        BreakIterator boundary = BreakIterator.getWordInstance(thaiLocale);
        boundary.setText(text);
        int start = boundary.first();
        int end = boundary.next();
        return text.substring(start, end);
    }

    public static String trimToEmpty(String substring) {
        if(substring==null) return "";
        return substring.trim();
    }

    public static String merge(List<String> list, String s) {
        StringBuilder builder = new StringBuilder();
        for(String str:list) {
            if(builder.length()>0) {
                builder.append(s);
            }
            builder.append(str);
        }
        return builder.toString();
    }

    public static String remove(String txt, String toRemove) {
        if(isEmpty(txt)) return "";
        if(isEmpty(toRemove)) return txt;
        while(txt.indexOf(toRemove)>=0) {
            txt = txt.replace(toRemove, "");
        }
        return txt;
    }

    public static String getChars(String text, int num) {
        if(StringUtils.isEmpty(text)) {
            return "*";
         }
         if(text.length()<=num) {
            return StringUtils.trimToEmpty(text);
         }
         return  StringUtils.trimToEmpty(text.substring(0, num));
    }
}
