package hdl

import chisel3._
import chisel3.util.log2Ceil
import hdmicore.video.VideoParams

class VGASync(val vp: VideoParams = VideoParams(
    H_DISPLAY = 640, H_FRONT = 8, H_SYNC = 96, H_BACK = 40,
    V_SYNC = 4,  V_BACK = 25,   V_TOP = 4, V_DISPLAY = 480, V_BOTTOM = 14
    )) extends Module { // with Formal { scala version problem

  val hregsize = log2Ceil(vp.H_DISPLAY + vp.H_BACK + vp.H_FRONT  + vp.H_SYNC)
  val vregsize = log2Ceil(vp.V_DISPLAY + vp.V_TOP  + vp.V_BOTTOM + vp.V_SYNC)
  val io = IO(new Bundle {
     val hsync = Output(Bool())
     val vsync = Output(Bool())
     val display_on = Output(Bool())
     val hpos = Output(UInt(hregsize.W))
     val vpos = Output(UInt(vregsize.W))
  })

  val V_BackPorch = vp.V_BACK.U(16.W)
  val V_Pluse = vp.V_SYNC.U(16.W)
  val HightPixel = vp.V_DISPLAY.U(16.W)
  val V_FrontPorch = vp.V_TOP.U(16.W)

  val H_BackPorch = vp.H_BACK.U(16.W)
  val H_Pluse = vp.H_SYNC.U(16.W)
  val WidthPixel = vp.H_DISPLAY.U(16.W)
  val H_FrontPorch = vp.H_FRONT.U(16.W)

  val PixelForHS = (WidthPixel+H_BackPorch)+H_FrontPorch
  val LineForVS = (HightPixel+V_BackPorch)+V_FrontPorch

  val hpos_count = RegInit(0.U((hregsize).W))
  val vpos_count = RegInit(0.U((vregsize).W))
  io.vpos := vpos_count
  io.hpos := hpos_count

  when (hpos_count === PixelForHS) {
    hpos_count := "b0".U(16.W)
    vpos_count := vpos_count+"b1".U(1.W)
  } .elsewhen (vpos_count === LineForVS) {
    vpos_count := "b0".U(16.W)
    hpos_count := "b0".U(16.W)
  } .otherwise {
    hpos_count := hpos_count+"b1".U(1.W)
  }

  //注意这里HSYNC和VSYNC负极性
  io.hsync := (Mux(((hpos_count >= H_Pluse) && (hpos_count <= (WidthPixel+H_BackPorch))), "b0".U(1.W), "b1".U(1.W)) =/= 0.U)
  //io.vsync := (Mux((((vpos_count >= 0.U) && (vpos_count <= (V_Pluse-1.U)))), "b1".U(1.W), "b0".U(1.W)) =/= 0.U) //这里不减一的话，图片底部会往下拖尾？
  io.vsync := (Mux((((vpos_count >= V_Pluse) && (vpos_count <= (HightPixel+V_BackPorch+V_FrontPorch)))), "b0".U(1.W), "b1".U(1.W)) =/= 0.U)
  //FIFO_RST := Mux(((hpos_count === 0.U)), "b1".U(1.W), "b0".U(1.W)) //留给主机H_BackPorch的时间进入中断，发送数据

  io.display_on := (Mux(((((hpos_count >= H_BackPorch) &&
                           (hpos_count <= (WidthPixel+H_BackPorch))) &&
                           (vpos_count >= V_BackPorch)) &&
                           (vpos_count <= ((HightPixel+V_BackPorch)-1.U))), "b1".U(1.W), "b0".U(1.W)) =/= 0.U)
                                               //这里不减一，会抖动
}
