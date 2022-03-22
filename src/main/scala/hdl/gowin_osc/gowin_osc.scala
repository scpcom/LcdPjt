package hdl
package gowin_osc

import chisel3._
//Copyright (C)2014-2019 Gowin Semiconductor Corporation.
//All rights reserved.
//File Title: IP file
//GOWIN Version: v1.9.2Beta
//Part Number: GW1N-LV1QN48C6/I5
//Created Time: Fri Oct 25 19:30:39 2019

class Gowin_OSC() extends RawModule {

  val oscout = IO(Output(Bool()))

  val osc_inst = Module(new OSCH)
  oscout := osc_inst.OSCOUT.asTypeOf(oscout)

} //Gowin_OSC
