package com.panda.web.controller.system;

import com.panda.common.exception.DataNotFoundException;
import com.panda.common.response.ResponseResult;
import com.panda.common.utils.ApplicationContextUtils;
import com.panda.system.domin.SysBill;
import com.panda.system.domin.SysMovie;
import com.panda.system.domin.SysSession;
import com.panda.system.domin.vo.SysBillVo;
import com.panda.system.service.impl.SysBillServiceImpl;
import com.panda.system.service.impl.SysMovieServiceImpl;
import com.panda.system.service.impl.SysSessionServiceImpl;
import com.panda.web.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@RestController
public class SysBillController extends BaseController {

    @Autowired
    private SysBillServiceImpl sysBillService;

    @Autowired
    private SysSessionServiceImpl sysSessionService;

    @Autowired
    private SysMovieServiceImpl sysMovieService;

    @GetMapping("/sysBill")
    public ResponseResult findAllBills(SysBill sysBill) {
        startPage();
        // 取消所有超时订单并释放占座资源
        ApplicationContextUtils.getBean("cancelTimeoutBill");
        List<SysBill> data = sysBillService.findAllBills(sysBill);
        return getResult(data);
    }

    @GetMapping("/sysBill/{id}")
    public ResponseResult findBillById(@PathVariable Long id) {
        return getResult(sysBillService.findBillById(id));
    }

    @PostMapping("/sysBill")
    public ResponseResult addBill(@Validated @RequestBody SysBillVo sysBillVo) {
        // 获取当前场次信息
        SysSession curSession = sysSessionService.findOneSession(sysBillVo.getSysBill().getSessionId());
        if (curSession == null) {
            throw new DataNotFoundException("添加订单的场次没找到");
        }
        System.out.println(curSession.getSessionSeats());
        curSession.setSessionSeats(sysBillVo.getSessionSeats());

        int addSallNums = sysBillVo.getSysBill().getSeats().split(",").length;
        curSession.setSallNums(curSession.getSallNums() + addSallNums);
        // 更新场次座位信息
        sysSessionService.updateSession(curSession);

        // 设置默认值
        if (sysBillVo.getSysBill().getUseState() == null) {
            sysBillVo.getSysBill().setUseState(false);
        }

        // 生成 viewing_code
        Long userId = sysBillVo.getSysBill().getUserId(); // 假设 userId 已经存在于 SysBill 中
        String seats = sysBillVo.getSysBill().getSeats();
        String extractedSeats = formatSeats(seats);
        String dateSuffix = getCurrentDateSuffix();
        String viewingCode = "U" + userId + "S" + extractedSeats + "D" + dateSuffix;
        sysBillVo.getSysBill().setViewingCode(viewingCode);

        Object obj = sysBillService.addBill(sysBillVo.getSysBill());
        if (obj instanceof Integer) {
            return getResult((Integer) obj);
        }
        return getResult(obj);
    }

    private String formatSeats(String seats) {
        StringBuilder sb = new StringBuilder();
        String[] seatArray = seats.split(",");
        for (String seat : seatArray) {
            // 提取每个座位的信息，例如 "4排5座" 提取为 "45"
            seat = seat.replaceAll("[^\\d]", ""); // 移除非数字字符
            sb.append(seat).append("-");
        }
        // 移除最后一个多余的分隔符
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private String getCurrentDateSuffix() {
        LocalDate currentDate = LocalDate.now();
        String year = String.valueOf(currentDate.getYear()).substring(2); // 获取年份的后两位
        String month = String.format("%02d", currentDate.getMonthValue()); // 获取两位月份
        String day = String.format("%02d", currentDate.getDayOfMonth()); // 获取两位日期
        return year + month + day;
    }

    @PutMapping("/sysBill")
    public ResponseResult pay(@RequestBody SysBill sysBill) {
        int rows = sysBillService.updateBill(sysBill);
        if (rows > 0 && sysBill.getPayState()) {
            //更新场次的座位状态
            SysSession curSession = sysSessionService.findOneSession(sysBill.getSessionId());
            if (curSession == null) {
                throw new DataNotFoundException("支付订单的场次没找到");
            }
            //更新电影票房
            SysMovie curMovie = sysMovieService.findOneMovie(curSession.getMovieId());
            if (curMovie == null) {
                throw new DataNotFoundException("支付订单的电影没找到");
            }
            //订单的座位数
            int seatNum = sysBill.getSeats().split(",").length;
            double price = curSession.getSessionPrice();
            curMovie.setMovieBoxOffice(curMovie.getMovieBoxOffice() + seatNum * price);
            sysMovieService.updateMovie(curMovie);
        }
        return getResult(rows);
    }

    @PutMapping("/sysBill/cancel")
    public ResponseResult cancel(@RequestBody SysBillVo sysBillVo) {
        // 订单取消，更新订单状态
        System.out.println("sysBillVo:" + sysBillVo);
        int rows = sysBillService.updateBill(sysBillVo.getSysBill());
        if (rows > 0 && sysBillVo.getSysBill().getCancelState()) {
            // 订单取消座位不再占用，更新场次的座位状态
            SysSession curSession = sysSessionService.findOneSession(sysBillVo.getSysBill().getSessionId());
            // 取消的订单座位数
            int cancelSallNums = sysBillVo.getSysBill().getSeats().split(",").length;
            curSession.setSallNums(curSession.getSallNums() - cancelSallNums);
            if (curSession == null) {
                throw new DataNotFoundException("添加订单的场次没找到");
            }
            curSession.setSessionSeats(sysBillVo.getSessionSeats());
            sysSessionService.updateSession(curSession);
        }
        return getResult(rows);
    }

    @PutMapping("/sysBill/inspection")
    public String inspection(@RequestParam String code){

        SysBill billByCode = sysBillService.findBillByCode(code);
        System.out.println("billByCode:---------------------------" + billByCode);
        System.out.println(code);
        if (billByCode == null){
            return "订单不存在";
        }else if (!billByCode.getPayState() || billByCode.getCancelState()){
            return "订单未支付或已取消";
        }else if (billByCode.getUseState()){
            return "电影票已使用";
        }else {
            billByCode.setUseState(true);
            sysBillService.updateBill(billByCode);
            return "检票成功！";
        }
    }

    @DeleteMapping("/sysBill/{ids}")
    public ResponseResult deleteBill(@PathVariable Long[] ids) {
        return getResult(sysBillService.deleteBill(ids));
    }

}
