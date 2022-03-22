package hdl
package gowin_osc

import chisel3._
import chisel3.experimental.Param
//Copyright (C)2014-2019 Gowin Semiconductor Corporation.
//All rights reserved.
//File Title: IP file
//GOWIN Version: v1.9.2Beta
//Part Number: GW1N-LV1QN48C6/I5
//Created Time: Fri Oct 25 19:30:39 2019

class OSCH(val pm: Map[String, Param]) extends BlackBox(pm){
    val io = IO(new Bundle{
        val OSCOUT = Output(Clock())

    })
}

class Gowin_OSC() extends RawModule {

  val oscout = IO(Output(Bool()))

  val om: Map[String, Param] = Map(
  "FREQ_DIV" -> 10,
  "DEVICE" -> "GW1N-1")

  val osc_inst = Module(new OSCH(om))
  oscout := osc_inst.io.OSCOUT.asTypeOf(oscout)

} //Gowin_OSC
