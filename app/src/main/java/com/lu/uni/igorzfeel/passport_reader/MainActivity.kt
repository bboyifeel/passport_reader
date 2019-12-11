package com.lu.uni.igorzfeel.passport_reader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.nfc.NfcAdapter
import android.util.Log
import android.app.PendingIntent
import android.content.Intent
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import org.jmrtd.BACKey
import org.jmrtd.BACKeySpec
import org.jmrtd.lds.PACEInfo
import org.jmrtd.PassportService
import org.jmrtd.lds.CardAccessFile
import net.sf.scuba.smartcards.CardService

class MainActivity : AppCompatActivity() {

    companion object {
        val TAG: String  = "MainActivity"
    }
    // dates has to be of the "yymmdd" format
    private val passportNumber: String = ""
    private val expirationDate: String = ""
    private val birthDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val docInfo: String =
                "Passport number:   ${passportNumber}\n" +
                "Expiration date:   ${expirationDate}\n" +
                "Birthday date:   ${birthDate}"

        document_info_textview.setText(docInfo)

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
                if (passportNumber != null && !passportNumber.isEmpty()
                    && expirationDate != null && !expirationDate.isEmpty()
                    && birthDate != null && !birthDate.isEmpty()) {
                    Log.d(TAG, "Fields aren't empty")

                    val bacKey = BACKey(passportNumber, birthDate, expirationDate)

                    Log.d(TAG, "BACKey: ${bacKey.toString()}")
                    readPassport(IsoDep.get(tag), bacKey)
                }
                else {
                    Log.d(TAG, "[ERROR] Fields are empty")
                    Toast.makeText(this, "Empty field is not allowed", Toast.LENGTH_SHORT).show()
                }
            }
            else {
                Log.d(TAG, "I don't know this card")
            }
        }
    }

    private fun readPassport(isoDep: IsoDep, bacKey: BACKeySpec) {
        Log.d(TAG, "Let's read that passport")
        Log.d(TAG, "isoDep: ${isoDep::class.java.canonicalName as String}")
        try {
            val cardService = CardService.getInstance(isoDep)
            cardService.open()

//            val service = PassportService(cardService)
//            service.open()

            Log.d(TAG, "service.open() just fine")
//
//            var paceSucceeded = false
//            try {
//                val cardAccessFile =
//                    CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS))
//                val paceInfos = cardAccessFile.getPACEInfos()
//                if (paceInfos != null && paceInfos!!.size > 0) {
//                    val paceInfo = paceInfos!!.iterator().next()
//                    val objectIdentifier = paceInfo.getObjectIdentifier()
//                    val paceInfoPraramSpec = PACEInfo.toParameterSpec(paceInfo.getParameterId())
//                    service.doPACE(bacKey, objectIdentifier, paceInfoPraramSpec)
//                    paceSucceeded = true
//                } else {
//                    paceSucceeded = true
//                }
//            } catch (e: Exception) {
//                Log.w(FragmentActivity.TAG, e)
//                throw e
//            }
//
//            service.sendSelectApplet(paceSucceeded)
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }
}
