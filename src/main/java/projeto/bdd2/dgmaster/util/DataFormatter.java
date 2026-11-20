package projeto.bdd2.dgmaster.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DataFormatter {
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DataFormatter() {
    }

    public static String formatar(LocalDateTime data) {
        return data != null ? DATA_HORA.format(data) : "—";
    }

    public static String formatar(LocalDate data) {
        return data != null ? DATA.format(data) : "—";
    }
}
