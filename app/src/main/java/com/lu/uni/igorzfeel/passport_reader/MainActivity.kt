package com.lu.uni.igorzfeel.passport_reader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.nfc.NfcAdapter
import android.util.Log
import android.app.PendingIntent
import android.content.Intent
import android.nfc.Tag
import android.widget.Toast


class MainActivity : AppCompatActivity() {

    companion object {
        val TAG: String  = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        Log.d(TAG,"NFC supported ${(nfcAdapter != null).toString()}")
        Log.d(TAG, "NFC enabled ${(nfcAdapter?.isEnabled).toString()}")
    }

    override fun onResume() {
        super.onResume()

        val adapter = NfcAdapter.getDefaultAdapter(this)

        if (adapter != null) {
            if (!adapter.isEnabled()) {
                Toast.makeText(this, "NFC is off, turn it on!", Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(applicationContext, this.javaClass)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            val filter = arrayOf(arrayOf("android.nfc.tech.IsoDep"))
            adapter.enableForegroundDispatch(this, pendingIntent, null, filter)
        }
        else {
            Toast.makeText(this, "Device doesn't support NFC", Toast.LENGTH_SHORT).show()
            return
        }
    }

    override fun onPause() {
        super.onPause()

        val adapter = NfcAdapter.getDefaultAdapter(this)
        adapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if(intent == null)
            return
        if(NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.action)) {
            Log.d(TAG, "NFC card has been discovered")
            var tag: Tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            if (tag.techList.toList().contains("android.nfc.tech.IsoDep")) {
                Log.d(TAG, "This is Iso supported tag")
            }
            else {
                Log.d(TAG, "I don't know this card")
            }
        }
    }

}
