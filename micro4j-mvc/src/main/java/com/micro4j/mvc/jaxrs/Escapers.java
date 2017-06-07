package com.micro4j.mvc.jaxrs;

class Escapers {

    public static String escape(String content) {
        if (content == null) {
            return null;
        }
        int len = content.length();
        StringBuilder builder = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char ascii = content.charAt(i);
            if (ascii == 60) {
                builder.append("&lt;");   // <
            } else if (ascii == 62) {
                builder.append("&gt;");   // >
            } else if (ascii == 38) {
                builder.append("&amp;");  // &
            } else if (ascii == 39) {
                builder.append("&#39;");  // '
            } else if (ascii == 96) {
                builder.append("&#x60;"); // `
            } else {
                builder.append(ascii);
            }
        }
        return builder.toString();
    }
}
