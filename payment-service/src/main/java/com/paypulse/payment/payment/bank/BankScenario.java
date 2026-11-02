package com.paypulse.payment.payment.bank;

public enum BankScenario {
    SUCCESS,
    DECLINED,
    TIMEOUT_AFTER_COMMIT,
    BANK_UNAVAILABLE
}