package com.lu.uni.igorzfeel.passport_reader

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_input.*


class InputActivity : AppCompatActivity() {

    companion object {
        val TAG: String  = "InputActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_input)

        input_btn_confirm.setOnClickListener {
            if (!validInput()) {
                return@setOnClickListener
            }
            val passportBundle: Bundle = prepareBundle()
            val intent = Intent(this, LoggingActivity::class.java)
            intent.putExtra("passportBundle", passportBundle)
            startActivity(intent)
        }
    }

    private fun prepareBundle(): Bundle {
        var passportBundle: Bundle = Bundle()
        passportBundle.putString("can", input_edittxt_can.text.toString())
        return passportBundle
    }

    // for now I have only can, since I'm working on PACE. later BAC can be added
    private fun validInput(): Boolean {
        if (input_edittxt_can.text.toString().length != 6){
            Log.d(TAG, "[ERROR] CAN has to have 6 digits")
            return false
        }
        Log.d(TAG, "Input is valid")
        return true
    }
}
