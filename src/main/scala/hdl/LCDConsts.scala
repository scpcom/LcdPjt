import chisel3._
import fpgamacro.gowin.PLLParams
import hdmicore.video.{VideoMode,VideoParams}

package object LCDConsts {
  /* 27 MHz * (36+1) / (4+1) = 199.8 MHz / 6 = 33.3 MHz ; 199.8 MHz / 5 = 39.96 MHz */
  val p39960khz  = PLLParams(IDIV_SEL = 4, FBDIV_SEL = 36, ODIV_SEL = 4, DYN_SDIV_SEL = 6)

  //pluse include in back pluse; t=pluse, sync act; t=bp, data act; t=bp+height, data end
  val m480x272 = VideoMode(
    params = VideoParams(
        V_BOTTOM = 12,
        V_BACK = 12,
        V_SYNC = 11,
        V_DISPLAY = 272,
        V_TOP = 8,

        H_BACK = 50,
        H_SYNC = 10,
        H_DISPLAY = 480,
        H_FRONT = 8,
    ),
    pll = p39960khz
  )

  val m800x480 = VideoMode(
    params = VideoParams(
        V_BOTTOM = 0,
        V_BACK = 0, //6
        V_SYNC = 5,
        V_DISPLAY = 480,
        V_TOP = 45, //62

        H_BACK = 182, //NOTE: 高像素时钟时，增加这里的延迟，方便K210加入中断
        H_SYNC = 1,
        H_DISPLAY = 800,
        H_FRONT = 210,
    ),
    pll = p39960khz
  )
}
