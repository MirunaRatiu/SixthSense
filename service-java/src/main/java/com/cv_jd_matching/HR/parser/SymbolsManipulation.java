package com.cv_jd_matching.HR.parser;

public class SymbolsManipulation {



    public static String encodeDiacritics(String text) {
        if (text == null) {
            return null;
        }

        text = text.replace("ă", "\\u0103");
        text = text.replace("î", "\\u00EE");
        text = text.replace("ț", "\\u021B");
        text = text.replace("ș", "\\u0219");
        text = text.replace("â", "\\u00E2");

        text = text.replace("Ă", "\\u0102");
        text = text.replace("Î", "\\u00CE");
        text = text.replace("Ț", "\\u021A");
        text = text.replace("Ș", "\\u0218");
        text = text.replace("Â", "\\u00C2");
        text = text.replace("'", "\\u0027");
        text = text.replace("’", "\\u2019");
        text = text.replaceAll( "\"","\\u0022");

        text = text.replace("/", "\\u002F");
        text = text.replace("I", "\\u0049");
        text = text.replace("|", "\\u007C");

        text = text.replace("\\u2018", "‘");
        text = text.replace("\\u201C", "“");
        text = text.replace("\\u201D", "”");
        text = text.replace("\\u201A", "‚");
        text = text.replace("\\u201B", "‛");


//        text = text.replace("\\", "\\u005C");
//        text = text.replace("|", "\\u007C");
//        text = text.replace("-", "\\u002D");

        return text;
    }

    public static String decodeDiacritics(String text) {
        if (text == null) {
            return null;
        }

        text = text.replace("\\u0103", "ă");
        text = text.replace("\\u00EE", "î");
        text = text.replace("\\u021B", "ț");
        text = text.replace("\\u0219", "ș");
        text = text.replace("\\u00E2", "â");


        text = text.replace("\\u0102", "Ă");
        text = text.replace("\\u00CE", "Î");
        text = text.replace("\\u021A", "Ț");
        text = text.replace("\\u0218", "Ș");
        text = text.replace("\\u00C2", "Â");
        text = text.replace("\\u0027", "'");
        text = text.replace("\\u2019", "’");
        text = text.replaceAll("\\u0022", "\"");


        text = text.replace( "\\u002F","/");
        text = text.replace("\\u0049","I");
        text = text.replace( "\\u007C","|");

        text = text.replace("\\u2018", "‘");
        text = text.replace("\\u201C", "“");
        text = text.replace("\\u201D", "”");
        text = text.replace("\\u201A", "‚");
        text = text.replace("\\u201B", "‛");

//        text = text.replace("\\u005C", "\\");
//        text = text.replace("\\u007C", "|");
//        text = text.replace("\\u002D", "-");

        return text;
    }
}
