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

  val H_DISPLAY = vp.H_DISPLAY.U(hregsize.W)  // horizontal display width
  val H_FRONT   = vp.H_FRONT.U(hregsize.W)    // front porch
  val H_SYNC    = vp.H_SYNC.U(hregsize.W)     // sync width
  val H_BACK    = vp.H_BACK.U(hregsize.W)     // back porch
  val V_SYNC    = vp.V_SYNC.U(vregsize.W)     // sync width
  val V_TOP     = vp.V_TOP.U(vregsize.W)      // top border
  val V_DISPLAY = vp.V_DISPLAY.U(vregsize.W)  // vertical display width
  val V_BOTTOM  = vp.V_BOTTOM.U(vregsize.W)   // bottom border
  val H_SYNC_START = H_SYNC
  val H_SYNC_END   = H_DISPLAY + H_BACK
  val H_MAX        = H_DISPLAY + H_BACK + H_FRONT
  val V_SYNC_START = V_SYNC
  val V_SYNC_END   = V_DISPLAY + V_BOTTOM + V_TOP
  val V_MAX        = V_DISPLAY + V_TOP + V_BOTTOM

  val hpos_count = RegInit(0.U((hregsize).W))
  val vpos_count = RegInit(0.U((vregsize).W))
  io.vpos := vpos_count
  io.hpos := hpos_count

  when (hpos_count === H_MAX) {
    hpos_count := "b0".U(16.W)
    vpos_count := vpos_count+"b1".U(1.W)
  } .elsewhen (vpos_count === V_MAX) {
    vpos_count := "b0".U(16.W)
    hpos_count := "b0".U(16.W)
  } .otherwise {
    hpos_count := hpos_count+"b1".U(1.W)
  }

  //注意这里HSYNC和VSYNC负极性
  io.hsync := ((hpos_count >= H_SYNC_START) && (hpos_count <= H_SYNC_END))
  //io.vsync := (Mux((((vpos_count >= 0.U) && (vpos_count <= (V_SYNC-1.U)))), "b1".U(1.W), "b0".U(1.W)) =/= 0.U) //这里不减一的话，图片底部会往下拖尾？
  io.vsync := ((vpos_count >= V_SYNC_START) && (vpos_count <= V_SYNC_END))
  //FIFO_RST := Mux(((hpos_count === 0.U)), "b1".U(1.W), "b0".U(1.W)) //留给主机H_BACK的时间进入中断，发送数据

  io.display_on := (Mux(((((hpos_count >= H_BACK) &&
                           (hpos_count <= (H_DISPLAY+H_BACK))) &&
                           (vpos_count >= V_BOTTOM)) &&
                           (vpos_count <= ((V_DISPLAY+V_BOTTOM)-1.U))), "b1".U(1.W), "b0".U(1.W)) =/= 0.U)
                                               //这里不减一，会抖动
}
