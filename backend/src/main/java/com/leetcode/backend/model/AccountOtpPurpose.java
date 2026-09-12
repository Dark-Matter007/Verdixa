package com.leetcode.backend.model;

/** Purpose isolation prevents one security code from authorizing another account action. */
public enum AccountOtpPurpose {
    USERNAME_CHANGE,
    EMAIL_CHANGE_CURRENT,
    EMAIL_CHANGE_NEW,
    PASSWORD_SETUP
}
