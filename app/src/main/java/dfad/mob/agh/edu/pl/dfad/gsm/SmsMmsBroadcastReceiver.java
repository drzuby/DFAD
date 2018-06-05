package dfad.mob.agh.edu.pl.dfad.gsm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public class SmsMmsBroadcastReceiver extends BroadcastReceiver {

    private SmsListener smsListener;

    private MmsListener mmsListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            String smsSender = "";
            StringBuilder smsBody = new StringBuilder();
            for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                smsSender = smsMessage.getDisplayOriginatingAddress();
                smsBody.append(smsMessage.getMessageBody());
            }
            smsListener.onTextReceived(smsSender, smsBody.toString());
        } else if (Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION.equals(intent.getAction())) {
            Cursor curPdu = null;
            Cursor curAddr = null;
            Cursor curAddr2 = null;
            try {
                String address = null;
                curPdu = context.getContentResolver().query(Telephony.Mms.Inbox.CONTENT_URI, null, null, null, null);
                if (curPdu != null && curPdu.moveToNext()) { //first MMS message curPdu.moveToNext() is false
                    String id = curPdu.getString(curPdu.getColumnIndex("_id"));

                    Uri uriAddr = Uri.parse("content://mms/" + id + "/addr");
                    curAddr = context.getContentResolver().query(uriAddr, null, "type=137", null, null);
                    if (curAddr != null && curAddr.moveToNext()) {
                        address = curAddr.getString(curAddr.getColumnIndex("address"));
                        if (address.contentEquals("insert-address-token")) {
                            curAddr2 = context.getContentResolver().query(uriAddr, null, "type=151", null, null);
                            if (curAddr2 != null && curAddr2.moveToNext()) {
                                address = curAddr2.getString(curAddr2.getColumnIndex("address"));
                            }
                        }
                    }
                }
                mmsListener.onMediaReceived(address);
            } finally {
                if (curPdu != null) {
                    curPdu.close();
                }
                if (curAddr != null) {
                    curPdu.close();
                }
                if (curAddr2 != null) {
                    curPdu.close();
                }
            }
        }
    }

    public void setSmsListener(SmsListener smsListener) {
        this.smsListener = smsListener;
    }

    public void setMmsListener(MmsListener mmsListener) {
        this.mmsListener = mmsListener;
    }

    public interface SmsListener {
        void onTextReceived(String sender, String body);
    }

    public interface MmsListener {
        void onMediaReceived(String sender);
    }
}
