package com.example.msbaseprj.dto;

import java.util.List;

public record CursorPageResponse<T>(List<T> content, Integer pageSize, Long nextCursor, Boolean hasNext) {
	public CursorPageResponse {
		content = List.copyOf(content);
	}

}
