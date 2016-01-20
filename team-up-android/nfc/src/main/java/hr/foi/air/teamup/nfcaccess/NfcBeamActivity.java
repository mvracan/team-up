package hr.foi.air.teamup.nfcaccess;

import android.content.pm.PackageManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;

import hr.foi.air.teamup.Logger;

/**
 * abstract activity to extend when nfc needs to be used
 * Created by Tomislav Turek on 07.12.15..
 */
public abstract class NfcBeamActivity extends NfcActivity implements NfcAdapter.CreateNdefMessageCallback {

    protected String message;
    private static final String NFC_MIME_TYPE = "text/plain";
    protected NfcBeamMessageCallback callback;

    /**
     * creates the ndef message to beam from passed message
     * @param event devices recognized
     * @return message in ndef format
     */
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        return new NdefMessage(NdefRecord.createMime(NFC_MIME_TYPE, message.getBytes()));
    }

    /**
     * starts the nfc adapter, should be called in onCreate
     * @throws NfcNotAvailableException thrown if adapter was not initialized
     */
    protected void startNfcAdapter() throws NfcNotAvailableException, NfcNotEnabledException {
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
            throw new NfcNotAvailableException("Nfc adapter is not available on this phone");
        }
        setAdapter(NfcAdapter.getDefaultAdapter(this));
        if(getAdapter()==null){
            throw new NfcNotEnabledException("Nfc adapter is not enabled on this phone");
        }
    }

    /**
     * starts the beaming process, waits for the devices to get paired
     * @param message message to beam to the phone, null if in reader mode
     * @throws NfcNotAvailableException thrown if adapter was not initialized
     */
    protected void startNfcBeam(String message, NfcBeamMessageCallback callback) throws NfcNotAvailableException {

        NfcAdapter adapter = getAdapter();
        if(adapter == null) {
            throw new NfcNotAvailableException("Nfc adapter is not available or isn't working," +
                    " use startNfcAdapter before beaming");
        } else {
            Logger.log("Sending message" + message);
            this.message = message;
            this.callback = callback;
            adapter.setNdefPushMessageCallback(this, this);
        }
    }
}
