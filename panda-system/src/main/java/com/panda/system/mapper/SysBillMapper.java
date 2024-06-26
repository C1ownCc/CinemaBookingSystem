package com.panda.system.mapper;

import com.panda.system.domin.SysBill;

import java.util.List;


public interface SysBillMapper {

    List<SysBill> findAllBills(SysBill sysBill);

    SysBill findBillById(Long id);

    SysBill findBillByCode(String code);

    int addBill(SysBill sysBill);

    int updateBill(SysBill sysBill);

    int deleteBill(Long id);

    List<SysBill> findTimeoutBill();

    Integer countUsedSeatsBySessionId(Long id);

    Integer countTotalSeatsBySessionId(Long id);
}
