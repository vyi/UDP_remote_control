package com.example.udp

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


class MainActivity : AppCompatActivity(){

    private var address = InetAddress.getByName("192.168.1.135")
    private var port: Int = 5001
    private var data_to_send : String = ""
    private var socket = DatagramSocket()
    private var v = 0.0 //give in cm/s
    private var w = 0.0 //provide in rad/s
    private var udpclosed=true
    private val VMAX = 1.0
    private val WMAX = 1.0
    private val INCV = 0.033
    private val INCW = 0.033

    var V_set = 0.15//50.0
    var W_set = 0.05//53.0

    private lateinit var sliderV: SeekBar
    private lateinit var sliderW: SeekBar
    private lateinit var toggle: ToggleButton
    var pwm_flag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vVal.text = V_set.toString()
        wVal.text = W_set.toString()

        plusV.visibility = View.INVISIBLE
        plusW.visibility = View.INVISIBLE
        minusV.visibility = View.INVISIBLE
        minusW.visibility = View.INVISIBLE
        vVal.visibility = View.INVISIBLE
        wVal.visibility = View.INVISIBLE

//        plusV.setOnClickListener {
//            V_set += INCV
//            V_set = V_set.round(2)
//
//            vVal.text = V_set.toString()
//            //wVal.text = W_set.toString()
//
//            Log.d("CLiCK", "v changed to $V_set")
//        }
//
//        plusW.setOnClickListener {
//            W_set += INCW
//            W_set = W_set.round(2)
//            Log.d("CLiCK", "w changed to $W_set")
//
//            //vVal.text = V_set.toString()
//            wVal.text = W_set.toString()
//        }
//
//        minusV.setOnClickListener {
//            V_set -= INCV
//            V_set = V_set.round(2)
//            Log.d("CLiCK", "v changed to $V_set")
//
//            vVal.text = V_set.toString()
//            //wVal.text = W_set.toString()
//        }
//
//        minusW.setOnClickListener {
//            W_set -= INCW
//            W_set = W_set.round(2)
//            Log.d("CLiCK", "w changed to $W_set")
//
//            //vVal.text = V_set.toString()
//            wVal.text = W_set.toString()
//        }

        forward_button.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v = V_set //in cm/s
                    w = 0.0//W_set //in rad/s    //UNDO THIS LINE
                    dispatchUDP()
                    //uni_to_DD(v,w)
                }
                MotionEvent.ACTION_UP -> {
                    v = 0.0
                    w = 0.0
                    dispatchUDP()
                    //uni_to_DD(v,w)
                }
            }
            true
        }

        left_button.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v = V_set    //in cm/s
                    w = -W_set  //in rad/s
                    dispatchUDP()
                    //uni_to_DD(v,w)

                }
                MotionEvent.ACTION_UP -> {
                    v = 0.0
                    w = 0.0
                    dispatchUDP()
                    //uni_to_DD(v,w)

                }
            }
            true
        }

        stop_button.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v = 0.0
                    w = 0.0
                    dispatchUDP()
                    //uni_to_DD(v,w)
                }
                MotionEvent.ACTION_UP -> {
                    v = 0.0
                    w = 0.0
                    dispatchUDP()
                    //uni_to_DD(v,w)
                }
            }
            true
        }

        right_button.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v = V_set    //in cm/s
                    w = W_set   //in rad/s
                    dispatchUDP()
                    //uni_to_DD(v,w)
                }
                MotionEvent.ACTION_UP -> {
                    v = 0.0
                    w = 0.0
                    dispatchUDP()
                    //uni_to_DD(v,w)
                }
            }
            true
        }

        sliderV = findViewById<SeekBar>(R.id.seekBar)
        sliderW = findViewById<SeekBar>(R.id.seekBar2)

        sliderV.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {

                if (pwm_flag){
                    val temp = (p1*2.55).toInt()
                    findViewById<TextView>(R.id.PWM_left).text = "PWM Left: $temp"
                    V_set = temp.toDouble()
                }else{
                    val temp = p1*0.01
                    findViewById<TextView>(R.id.PWM_left).text = "Velocity Left: $temp"
                    V_set = temp
                }
            }


            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        sliderW.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {

                if (pwm_flag){
                    val temp = (p1*2.55).toInt()
                    findViewById<TextView>(R.id.PWM_right).text = "PWM Right: $temp"
                    V_set = temp.toDouble()
                }else{
                    val temp = (p1-50)*0.01
                    findViewById<TextView>(R.id.PWM_right).text = "Velocity Right: $temp"
                    V_set = temp
                }

            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })


        toggle = findViewById<ToggleButton>(R.id.togg)
        toggle.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked){
                //Toast.makeText(this, "Checked", Toast.LENGTH_SHORT).show()
                //Change labels
                var temp = (sliderV.progress*2.55).toInt()
                findViewById<TextView>(R.id.PWM_left).text = "PWM Left: $temp"
                temp = (sliderW.progress*2.55).toInt()
                findViewById<TextView>(R.id.PWM_right).text = "PWM Right: $temp"
                //sliderV.progress = 0
                //sliderW.progress = 0
                pwm_flag = true
            }
            else{
                //Toast.makeText(this, "Un-checked", Toast.LENGTH_SHORT).show()
                var temp = (sliderV.progress*0.01)
                findViewById<TextView>(R.id.PWM_left).text = "Velocity Left: $temp"
                temp = (sliderW.progress-50)*0.01
                findViewById<TextView>(R.id.PWM_right).text = "Velocity Right: $temp"
                //sliderV.progress = 0
                //sliderW.progress = 50
                pwm_flag = false
            }
        }


    }

    // Round off the decimal representation to two places
    fun Double.round(decimals: Int):Double {
        var multiplier = 1.0
        repeat(decimals) {multiplier *= 10}
        return kotlin.math.round(this * multiplier) /multiplier
    }

    private fun dispatchUDP(){
        //data_to_send = "v_${velocityLinearLeft}_w_${velocityLinearRight}\r\n"
//        if(v>VMAX){
//            v=VMAX
//        }else {
//            if (v < 0) {
//                v = 0.0
//            }
//        }
//
//        if(w>WMAX){
//            w=WMAX
//        }else {
//            if (w < -WMAX) {
//                w = -WMAX
//            }
//        }


        if(pwm_flag){
            data_to_send = "pwmL_${V_set}_pwmR_${W_set}\r\n"
        }else {
            data_to_send = "v_${v}_w_${w}\r\n"
        }
        val thread = Thread(Runnable {
            try {
                if(udpclosed){
                    port = Integer.parseInt(editText4.text.toString())
                    address = InetAddress.getByName((editText3.text).toString())
                    socket = DatagramSocket()
                    socket.connect(address, port)
                    udpclosed=false
                }
                val pack = DatagramPacket(data_to_send.toByteArray(), data_to_send.length, address, port)
                socket.send(pack)
                Log.d("UDP sender", "Attempting to send data $data_to_send")

            } catch (e: Exception) {
                Log.d("UDP sender", "Encountered error")
                Log.d("UDP_sender", e.printStackTrace().toString())
            }
        })

        thread.start()
    }

    override fun onPause() {
        super.onPause()
        if(!udpclosed) {
            socket.close()
            udpclosed=true
        }
    }
}
