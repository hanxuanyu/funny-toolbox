package com.hxuanyu.funnytoolbox.auth;

import lombok.Data;

/**
 * 简单的认证状态返回 DTO。
 */
@Data
public class AuthStatus {
    /** 是否已通过平台认证（会话有效） */
    private boolean authenticated;

    /** 可选：用户名（若会话中存在则返回） */
    private String user;

    /** 会话ID（便于诊断） */
    private String sessionId;

    /** 服务器时间戳（毫秒） */
    private long serverTime;
}
