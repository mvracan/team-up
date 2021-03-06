package hr.foi.air.teamup.nfcaccess;

/**
 * use when nfc is not supported
 * Created by Tomislav Turek on 07.12.15..
 */
public class NfcNotAvailableException extends Exception {

    public NfcNotAvailableException() {
        super();
    }

    public NfcNotAvailableException(String detailMessage) {
        super(detailMessage);
    }

    public NfcNotAvailableException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public NfcNotAvailableException(Throwable throwable) {
        super(throwable);
    }

}

