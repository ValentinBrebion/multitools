package com.example.multitools.tools.http;

import java.util.List;

public record HttpStatusInfo(
    int code,
    String phrase,
    String meaning,
    List<String> commonCauses,
    List<String> troubleshooting
) {
    public HttpStatusInfo {
        commonCauses = List.copyOf(commonCauses);
        troubleshooting = List.copyOf(troubleshooting);
    }

    public String searchText() {
        return (code + " " + phrase + " " + meaning + " "
            + String.join(" ", commonCauses) + " "
            + String.join(" ", troubleshooting)).toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public String toString() {
        return code + " — " + phrase;
    }
}
