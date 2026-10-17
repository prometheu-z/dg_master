package projeto.bdd2.dgmaster.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DataFormatter {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DataFormatter() {}

    public static String formatar(LocalDateTime data) {
        return data != null ? FMT.format(data) : "—";
    }
}
