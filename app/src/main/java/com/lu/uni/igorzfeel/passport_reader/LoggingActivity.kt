package com.lu.uni.igorzfeel.passport_reader_kotlin

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_logging.*
import net.sf.scuba.smartcards.CardService
import org.jmrtd.BACKey
import org.jmrtd.PACEKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.CardAccessFile
import org.jmrtd.lds.LDSFileUtil
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.icao.DG1File
import java.io.InputStream


class LoggingActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        val TAG: String  = "LoggingActivity"
        private const val TIMEOUT_ISODEP = 10_000
    }

    private var nfcAdapter: NfcAdapter? = null

    // dates has to be of the "yymmdd" format
    private var passportNumber: String = ""
    private var expirationDate: String = ""
    private var birthDate: String = ""
    private var can: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logging)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val passportBundle: Bundle = getIntent().getBundleExtra("passportBundle")
        extractBundle(passportBundle)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        logGeneralInfo()
    }

    private fun logGeneralInfo() {
        updateLog("NFC supported - ${(nfcAdapter != null).toString()}")
        updateLog("NFC enabled - ${(nfcAdapter?.isEnabled).toString()}")
        updateLog("passportNumber: " + passportNumber)
        updateLog("expirationDate: " + expirationDate)
        updateLog("birthDate: " + birthDate)
        updateLog("CAN: " + can)
    }


    override fun onTagDiscovered(tag: Tag?) {
        clearLog()
        logGeneralInfo()
        updateLog("NFC card has been discovered")

        val isoDep = IsoDep.get(tag)
        isoDep.timeout = TIMEOUT_ISODEP
        readPassport(isoDep)
    }


    private fun extractBundle(bundle: Bundle) {
        passportNumber = bundle.getString("passportNumber").toString()
        expirationDate = bundle.getString("expirationDate").toString()
        birthDate = bundle.getString("birthDate").toString()
        can = bundle.getString("can").toString()
    }


    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(this, this,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null)
    }


    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }


    private fun updateLog(msg: String) {
        Log.i(TAG, msg)
        runOnUiThread {logging_txtview_log.append(msg + "\n") }
    }


    private fun clearLog() {
        runOnUiThread {logging_txtview_log.text = "" }
    }


    private fun updateError(msg: String) {
        Log.e(TAG, msg)
        runOnUiThread {logging_txtview_log.append("[ERROR] " + msg + "\n") }
    }


    private fun readPassport(isoDep: IsoDep) {
        updateLog("Let's read that passport")
        if (!(passportNumber.isNotEmpty()
            && expirationDate.isNotEmpty()
            && birthDate.isNotEmpty()
            && can.isNotEmpty())) {
            updateError("Fields are empty")
            Toast.makeText(this, "Empty field is not allowed", Toast.LENGTH_SHORT).show()
            return
        }

        val bacKey = BACKey(passportNumber, birthDate, expirationDate)
        val paceKey = PACEKeySpec.createCANKey(can)

        try {
            val cardService = CardService.getInstance(isoDep)
            val passportService = PassportService(cardService
                , PassportService.NORMAL_MAX_TRANCEIVE_LENGTH
                , PassportService.DEFAULT_MAX_BLOCKSIZE
                , false
                , true)
            passportService.open()

//            invokingReaderApp(isoDep)

            var paceSucceeded = doPace(passportService, paceKey)

            try {
                passportService.sendSelectApplet(paceSucceeded)
            } catch (e: Exception) {
                updateError(e.toString())
            }

            if (!paceSucceeded) {
                updateLog("Let's try out to proceed with BAC")
                passportService.doBAC(bacKey)
                updateLog("BAC success")
            }

            extractPrivateData(passportService)
        } catch (e: Exception) {
            updateError(e.toString())
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }


    private fun invokingReaderApp(isoDep: IsoDep) {
        // this function is a fix of the passport emulator side issue
        // should not be used with a real document

        try {
            var command: ByteArray = Utils.hexStringToByteArray("00A4040C07A0000002471001")
            updateLog("Sending " + "00A4040C07A0000002471001")
            var result = isoDep.transceive(command)
            updateLog(Utils.toHex(result))
        } catch (e: Exception) {
            updateError("First capdu failed for some reason")
            updateError(e.toString())
        }
    }


    private fun doPace(
        passportService: PassportService,
        paceKey: PACEKeySpec?
    ): Boolean {
        var paceSucceeded = false
        try {
            val cardAccessFile =
                CardAccessFile(passportService.getInputStream(PassportService.EF_CARD_ACCESS))
            val secInfos = cardAccessFile.securityInfos
            updateLog(cardAccessFile.toString())

            if (secInfos != null && secInfos.isNotEmpty()) {
                Log.d(TAG, "PACE info has been found")
                val paceInfo = secInfos.iterator().next() as PACEInfo
                val oid = paceInfo.objectIdentifier
                val paramId = paceInfo.parameterId
                val params = PACEInfo.toParameterSpec(paramId)

                updateLog(paceInfo.protocolOIDString)
                updateLog(PACEInfo.toStandardizedParamIdString(paramId))

                passportService.doPACE(paceKey, oid, params, paramId)
                paceSucceeded = true
                updateLog("PACE has succeeded")
            }
        } catch (e: Exception) {
            updateError("PACE has failed with next error:")
            updateError(e.toString())
        }

        return paceSucceeded
    }


    @Throws(net.sf.scuba.smartcards.CardServiceException::class)
    private fun extractPrivateData(passportService: PassportService) {
        var inputStream: InputStream = passportService.getInputStream(PassportService.EF_DG1)
        val dg1 = LDSFileUtil.getLDSFile(PassportService.EF_DG1, inputStream) as DG1File
        updateLog(dg1.mrzInfo.nationality)
        updateLog(dg1.mrzInfo.documentNumber)
        updateLog(dg1.mrzInfo.dateOfExpiry)
        updateLog(dg1.mrzInfo.gender.toString())
        updateLog(dg1.mrzInfo.issuingState.toString())
    }


}
