package GameModuleFramework.symbols;

import java.util.Objects;

/** A stable symbol identity shared by game modules and common mechanics. */
public record Symbol(String id) {
    public Symbol {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Symbol ID must not be blank");
        }
    }
}
