package com.familymeal.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.familymeal.entity.CookToday;
import com.familymeal.mapper.CookTodayMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class CookService {

    private final CookTodayMapper cookTodayMapper;
    private final WeChatService weChatService;

    public CookService(CookTodayMapper cookTodayMapper, WeChatService weChatService) {
        this.cookTodayMapper = cookTodayMapper;
        this.weChatService = weChatService;
    }

    /** 今日做饭人角色：husband / wife，默认 wife（与 init.sql 预置一致） */
    public String getCookWho() {
        CookToday row = cookTodayMapper.selectById(1L);
        if (row == null || row.getCookWho() == null || row.getCookWho().isEmpty()) {
            return "wife";
        }
        return row.getCookWho();
    }

    /** 今日做饭状态：cooking / done，默认 cooking */
    public String getCookStatus() {
        CookToday row = cookTodayMapper.selectById(1L);
        if (row == null || row.getCookStatus() == null || row.getCookStatus().isEmpty()) {
            return "cooking";
        }
        return row.getCookStatus();
    }

    /** 切换今日做饭人（仅 husband/wife 二选一），同时把做饭状态重置为 cooking */
    public CookToday switchCook(String who, Long switchedBy) {
        if (!"husband".equals(who) && !"wife".equals(who)) {
            throw new RuntimeException("做饭人只能是 husband 或 wife");
        }
        CookToday row = cookTodayMapper.selectById(1L);
        if (row == null) {
            row = new CookToday();
            row.setId(1L);
            row.setCookWho(who);
            row.setCookStatus("cooking");
            row.setSwitchedBy(switchedBy);
            row.setSwitchedAt(LocalDateTime.now());
            cookTodayMapper.insert(row);
        } else {
            cookTodayMapper.update(null, new LambdaUpdateWrapper<CookToday>()
                    .eq(CookToday::getId, 1L)
                    .set(CookToday::getCookWho, who)
                    .set(CookToday::getCookStatus, "cooking")
                    .set(CookToday::getSwitchedBy, switchedBy)
                    .set(CookToday::getSwitchedAt, LocalDateTime.now()));
            row.setCookWho(who);
            row.setCookStatus("cooking");
            row.setSwitchedBy(switchedBy);
        }
        return row;
    }

    /** 更新做饭状态：cooking / done。done 时通知点餐端（husband 固定 id=1）饭好了。 */
    public CookToday updateCookStatus(String status, Long switchedBy) {
        if (!"cooking".equals(status) && !"done".equals(status)) {
            throw new RuntimeException("状态只能是 cooking 或 done");
        }
        CookToday row = cookTodayMapper.selectById(1L);
        if (row == null) {
            row = new CookToday();
            row.setId(1L);
            row.setCookWho("wife");
            row.setCookStatus(status);
            row.setSwitchedBy(switchedBy);
            row.setSwitchedAt(LocalDateTime.now());
            cookTodayMapper.insert(row);
        } else {
            cookTodayMapper.update(null, new LambdaUpdateWrapper<CookToday>()
                    .eq(CookToday::getId, 1L)
                    .set(CookToday::getCookStatus, status)
                    .set(CookToday::getSwitchedBy, switchedBy)
                    .set(CookToday::getSwitchedAt, LocalDateTime.now()));
            row.setCookStatus(status);
        }
        if ("done".equals(status)) {
            String timeText = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"));
            weChatService.sendCookDoneNotify(1L, timeText, "饭做好啦，快来吃饭吧");
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
