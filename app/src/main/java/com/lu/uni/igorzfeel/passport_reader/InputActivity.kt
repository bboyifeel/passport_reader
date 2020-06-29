package com.lu.uni.igorzfeel.passport_reader_kotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.util.Log
import kotlinx.android.synthetic.main.activity_input.*

class InputActivity : AppCompatActivity() {

    companion object {
        val TAG: String  = "InputActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        defaultInput()
        input_btn_confirm.setOnClickListener {
            startReadingButton()
        }
        startReadingButton()
    }

    private fun startReadingButton() {
        if (!validInput()) {
            return
        }
        val passportBundle: Bundle = prepareBundle()
        val intent = Intent(this, LoggingActivity::class.java)
        intent.putExtra("passportBundle", passportBundle)
        startActivity(intent)
    }

    private fun prepareBundle(): Bundle {
        var passportBundle: Bundle = Bundle()

        passportBundle.putString("passportNumber", input_edittxt_passport_number.text.toString())
        passportBundle.putString("expirationDate", input_edittxt_expiration_date.text.toString())
        passportBundle.putString("birthDate", input_edittxt_birth_date.text.toString())
        passportBundle.putString("can", input_edittxt_can.text.toString())

        return passportBundle
    }

    // for now I have only can, since I'm working on PACE. later BAC can be added
    private fun validInput(): Boolean {
        if (input_edittxt_passport_number.text.toString().isEmpty()){
            Log.e(TAG, "[ERROR] Passport number cannot be empty")
            return false
        }
        if (input_edittxt_expiration_date.text.toString().length !=6){
            Log.e(TAG, "[ERROR] Expiration date has to have format yymmdd (incorrect length)")
            return false
        }
        if (input_edittxt_birth_date.text.toString().length !=6){
            Log.e(TAG, "[ERROR] Birth date has to have format yymmdd (incorrect length)")
            return false
        }
        if (input_edittxt_can.text.toString().length != 6){
            Log.e(TAG, "[ERROR] CAN has to have 6 digits")
            return false
        }
        Log.d(TAG, "Input is valid")
        return true
    }

    fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)

    private fun defaultInput() {
        val passportNumber: String = ""
        val expirationDate: String = ""
        val birthDate: String = ""
        val can: String = ""

        input_edittxt_passport_number.text = passportNumber.toEditable()
        input_edittxt_expiration_date.text = expirationDate.toEditable()
        input_edittxt_birth_date.text = birthDate.toEditable()
        input_edittxt_can.text = can.toEditable()

    }
}
