package com.tustanovskyy.taxi.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
@Slf4j
public class VerificationCodeException extends RuntimeException {
	public VerificationCodeException(String message) {
		super(message);
		log.error(message, this.getCause());
	}
}
