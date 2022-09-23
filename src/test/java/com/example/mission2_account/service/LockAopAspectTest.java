package com.example.mission2_account.service;

import com.example.mission2_account.dto.UseBalance;
import com.example.mission2_account.exception.AccountException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.example.mission2_account.type.ErrorCode.ACCOUNT_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LockAopAspectTest {
    @Mock
    private LockService lockService;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    private LockAopAspect lockAopAspect;

    @Test
    @DisplayName("계좌 잠금 해제 성공")
    void lockAndUnlock() throws Throwable {
        //given
        ArgumentCaptor<String> lockCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unLockCaptor = ArgumentCaptor.forClass(String.class);
        UseBalance.Request request = new UseBalance.Request(1234L, "1234", 1000L);

        //when
        lockAopAspect.aroundMethod(proceedingJoinPoint, request);

        //then
        verify(lockService, times(1)).lock(lockCaptor.capture());
        verify(lockService, times(1)).unlock(unLockCaptor.capture());
        assertEquals("1234", lockCaptor.getValue());
        assertEquals("1234", unLockCaptor.getValue());
    }

    @Test
    @DisplayName("계좌 잠금 해제 실패")
    void lockAndUnlock_evenIfThrow() throws Throwable {
        //given
        ArgumentCaptor<String> lockCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unLockCaptor = ArgumentCaptor.forClass(String.class);
        UseBalance.Request request = new UseBalance.Request(
                1234L, "54321", 1000L);
        given(proceedingJoinPoint.proceed())
                .willThrow(new AccountException(ACCOUNT_NOT_FOUND));

        //when
        assertThrows(AccountException.class, () ->
                lockAopAspect.aroundMethod(proceedingJoinPoint, request));

        //then
        verify(lockService, times(1)).lock(lockCaptor.capture());
        verify(lockService, times(1)).unlock(unLockCaptor.capture());
        assertEquals("54321", lockCaptor.getValue());
        assertEquals("54321", unLockCaptor.getValue());
    }
}