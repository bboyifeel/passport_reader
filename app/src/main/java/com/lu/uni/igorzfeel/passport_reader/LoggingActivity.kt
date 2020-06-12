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
    }

    private var nfcAdapter: NfcAdapter? = null

    // dates has to be of the "yymmdd" format
    private var passportNumber: String = ""
    private var expirationDate: String = ""
    private var birthDate: String = ""
    private var can: String = ""

    private var log: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logging)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val passportBundle: Bundle = getIntent().getBundleExtra("passportBundle")
        extractBundle(passportBundle)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        updateLog("NFC supported ${(nfcAdapter != null).toString()}")
        updateLog("NFC enabled ${(nfcAdapter?.isEnabled).toString()}")
    }


    override fun onTagDiscovered(tag: Tag?) {
        updateLog("NFC card has been discovered")
        val isoDep = IsoDep.get(tag)
        readPassport(isoDep)
    }


    private fun extractBundle(bundle: Bundle) {
        passportNumber = bundle.getString("passportNumber").toString()
        expirationDate = bundle.getString("expirationDate").toString()
        birthDate = bundle.getString("birthDate").toString()
        can = bundle.getString("can").toString()
        updateLog("Bundle has been extracted")
        updateLog("passportNumber " + passportNumber)
        updateLog("expirationDate " + expirationDate)
        updateLog("birthDate " + birthDate)
        updateLog("can " + can)
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
//        Log.d(TAG, msg)
        System.out.println(msg)
        log += msg + "\n"
        logging_txtview_log.text = log
    }

    private fun readPassport(isoDep: IsoDep) {
        updateLog("Let's read that passport")
        if (!(passportNumber.isNotEmpty()
            && expirationDate.isNotEmpty()
            && birthDate.isNotEmpty()
            && can.isNotEmpty())) {
            updateLog("[ERROR] Fields are empty")
            Toast.makeText(this, "Empty field is not allowed", Toast.LENGTH_SHORT).show()
            return
        }

        //////////////////////[begin] TEST

//        isoDep.connect()
//        val command: ByteArray = HexStringToByteArray("00A4040C07A0000002471001")
//        val result = isoDep.transceive(command)
//        updateLog(ByteArrayToHexString(result))
//        return
        //////////////////////[end] TEST

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

            updateLog("Passport Service has been opened")

            var paceSucceeded = doPace(passportService, paceKey)

            try {
                passportService.sendSelectApplet(paceSucceeded)
            } catch (e: Exception) {
                // PACE didn't succeed
            }

            if (!paceSucceeded) {
                updateLog("Let's try out to proceed with BAC")
                passportService.doBAC(bacKey)
                updateLog("BAC success")
            }

            extractPrivateData(passportService)
        } catch (e: Exception) {
            updateLog(e.toString())
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
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
            updateLog("PACE has failed with next error:")
            updateLog(e.toString())
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


    //////////////////////[begin] TEST
    fun ByteArrayToHexString(bytes: ByteArray): String {
        val hexArray = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F'
        )
        val hexChars =
            CharArray(bytes.size * 2) // Each byte has two hex characters (nibbles)
        var v: Int
        for (j in bytes.indices) {
            v = bytes[j].toInt()  and 0xFF // Cast bytes[j] to int, treating as unsigned value
            hexChars[j * 2] = hexArray[v ushr 4] // Select hex character from upper nibble
            hexChars[j * 2 + 1] =
                hexArray[v and 0x0F] // Select hex character from lower nibble
        }
        return String(hexChars)
    }


    @Throws(IllegalArgumentException::class)
    fun HexStringToByteArray(s: String): ByteArray {
        val len = s.length
        require(len % 2 != 1) { "Hex string must have even number of characters" }
        val data =
            ByteArray(len / 2) // Allocate 1 byte per 2 hex characters
        var i = 0
        while (i < len) {

            // Convert each character into a integer (base-16), then bit-shift into place
            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    //////////////////////[end] TEST
}
