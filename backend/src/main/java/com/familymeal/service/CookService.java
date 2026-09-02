package com.familymeal.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.familymeal.entity.CookToday;
import com.familymeal.mapper.CookTodayMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CookService {

    private final CookTodayMapper cookTodayMapper;

    public CookService(CookTodayMapper cookTodayMapper) {
        this.cookTodayMapper = cookTodayMapper;
    }

    /** 今日做饭人角色：husband / wife，默认 wife（与 init.sql 预置一致） */
    public String getCookWho() {
        CookToday row = cookTodayMapper.selectById(1L);
        if (row == null || row.getCookWho() == null || row.getCookWho().isEmpty()) {
            return "wife";
        }
        return row.getCookWho();
    }

    /** 切换今日做饭人（仅 husband/wife 二选一） */
    public CookToday switchCook(String who, Long switchedBy) {
        if (!"husband".equals(who) && !"wife".equals(who)) {
            throw new RuntimeException("做饭人只能是 husband 或 wife");
        }
        CookToday row = cookTodayMapper.selectById(1L);
        if (row == null) {
            row = new CookToday();
            row.setId(1L);
            row.setCookWho(who);
            row.setSwitchedBy(switchedBy);
            row.setSwitchedAt(LocalDateTime.now());
            cookTodayMapper.insert(row);
        } else {
            cookTodayMapper.update(null, new LambdaUpdateWrapper<CookToday>()
                    .eq(CookToday::getId, 1L)
                    .set(CookToday::getCookWho, who)
                    .set(CookToday::getSwitchedBy, switchedBy)
                    .set(CookToday::getSwitchedAt, LocalDateTime.now()));
            row.setCookWho(who);
            row.setSwitchedBy(switchedBy);
        }
        return row;
    }

    /** 指定角色是否为今日做饭人 */
    public boolean isCook(String role) {
        return getCookWho().equals(role);
    }

    /** 今日做饭人的用户ID（husband=1, wife=2，与 init.sql 预置账号一致） */
    public Long getCookUserId() {
        return "husband".equals(getCookWho()) ? 1L : 2L;
    }
}
