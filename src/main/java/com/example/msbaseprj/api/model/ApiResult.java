package com.example.msbaseprj.api.model;

import java.util.Objects;
import java.util.function.Function;

import com.example.msbaseprj.api.error.ErrorInfo;

import com.example.msbaseprj.api.error.ApiException;

public sealed interface ApiResult<T> permits ApiResult.Success, ApiResult.Failure {

	record Success<T>(T data) implements ApiResult<T> {
		public Success {
			Objects.requireNonNull(data);
		}
	}

	record Failure<T>(ErrorInfo error) implements ApiResult<T> {
	}

	default T getOrThrow() {
		return switch (this) {
			case Success<T> s -> s.data();

			case Failure<T> f ->
				throw new ApiException(f.error().message(), f.error().status(), f.error().type(), f.error().cause());
		};
	}

	default <R> ApiResult<R> map(Function<T, R> mapper) {
		return switch (this) {
			case Success<T> s -> new Success<>(mapper.apply(s.data()));

			case Failure<T> f -> new Failure<>(f.error());
		};
	}

	default <R> ApiResult<R> flatMap(Function<T, ApiResult<R>> mapper) {
		return switch (this) {
			case Success<T> s -> mapper.apply(s.data());

			case Failure<T> f -> new Failure<>(f.error());
		};
	}

}
