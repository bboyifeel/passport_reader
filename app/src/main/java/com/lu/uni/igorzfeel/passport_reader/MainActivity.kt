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
import org.jmrtd.PassportService
import net.sf.scuba.smartcards.CardService
import java.io.InputStream
import org.jmrtd.lds.LDSFileUtil
import org.jmrtd.lds.icao.DG1File


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
                readPassport(IsoDep.get(tag))
            }
            else {
                Log.d(TAG, "I don't know this card")
            }
        }
    }

    private fun readPassport(isoDep: IsoDep) {
        Log.d(TAG, "Let's read that passport")

        if (passportNumber != null && !passportNumber.isEmpty()
            && expirationDate != null && !expirationDate.isEmpty()
            && birthDate != null && !birthDate.isEmpty()) {
            Log.d(TAG, "Fields aren't empty")
        }
        else {
            Log.d(TAG, "[ERROR] Fields are empty")
            Toast.makeText(this, "Empty field is not allowed", Toast.LENGTH_SHORT).show()
            return
        }

        val bacKey = BACKey(passportNumber, birthDate, expirationDate)

        try {
            val cardService = CardService.getInstance(isoDep)
            cardService.open()

            val pasportService = PassportService(cardService
                , PassportService.NORMAL_MAX_TRANCEIVE_LENGTH
                , PassportService.DEFAULT_MAX_BLOCKSIZE
                , false
                , true)
            pasportService.open()

//            var paceSucceeded = false
//            try {
//                val cardAccessFile =
//                    CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS))
//                val secInfos = cardAccessFile.securityInfos
//                if (secInfos != null && secInfos.isNotEmpty()) {
//                    val paceInfo = secInfos.iterator().next()
//                    val oid = paceInfo.objectIdentifier
//                    Log.d(TAG, oid)
//
//                    service.doPACE(bacKey, oid, paceInfoPraramSpec, null)

//                    val paceInfoPraramSpec = PACEInfo.toParameterSpec(paceInfo.getParameterId())
//

//                    paceSucceeded = true
//                } else {
//                    paceSucceeded = true
//                }
//            } catch (e: Exception) {
//                Log.d(TAG, e.toString())
//                throw e
//            }

            pasportService.sendSelectApplet(false)
            pasportService.doBAC(bacKey)
            Log.d(TAG, "BAC success")

            var inputStream: InputStream = pasportService.getInputStream(PassportService.EF_DG1)
            val dg1 = LDSFileUtil.getLDSFile(PassportService.EF_DG1, inputStream) as DG1File
            Log.d(TAG, dg1.mrzInfo.personalNumber)
            Log.d(TAG, dg1.mrzInfo.dateOfBirth)
            Log.d(TAG, dg1.mrzInfo.nationality)
            Log.d(TAG, dg1.mrzInfo.documentNumber)
            Log.d(TAG, dg1.mrzInfo.documentCode)
            Log.d(TAG, dg1.mrzInfo.dateOfExpiry)
            Log.d(TAG, dg1.mrzInfo.gender.toString())

        } catch (e: Exception) {
            Log.d(TAG, e.toString())
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }
}
