/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.error;

import java.util.Objects;

/** Exception métier NKULKAP : porte un code du catalogue et les arguments de substitution du message traduit. */
public class NkulkapException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final NkulkapErrorCode code;
    private final transient Object[] args;

    /** Le message est destiné au développeur (anglais), jamais affiché à l'utilisateur. */
    public NkulkapException(NkulkapErrorCode code, String message, Object... args) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
        this.args = args == null ? new Object[0] : args.clone();
    }

    public NkulkapException(NkulkapErrorCode code, String message, Throwable cause, Object... args) {
        super(message, cause);
        this.code = Objects.requireNonNull(code, "code");
        this.args = args == null ? new Object[0] : args.clone();
    }

    public NkulkapErrorCode code() {
        return code;
    }

    public Object[] args() {
        return args.clone();
    }
}
