package com.example.msbaseprj.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.msbaseprj.dto.UserProjection;
import com.example.msbaseprj.repository.UserRepository;

@Service
public class UserExportService {
	private static final int MAX_BATCH_SIZE = 100;

	private final UserRepository repo;

	public UserExportService(UserRepository repo) {
		this.repo = repo;
	}

	@Transactional(readOnly = true)
	public void exportToCsv(Path file) throws IOException {
		int counter = 0;

		try (Stream<UserProjection> users = repo.findAllBy(); var writer = Files.newBufferedWriter(file)) {
			for (var user : (Iterable<UserProjection>) users::iterator) {
				writer.write(toCsvLine(user));
				writer.newLine();

				if (++counter % MAX_BATCH_SIZE == 0)
					writer.flush();
			}
		}
	}

	private String toCsvLine(UserProjection user) {
		return user.getUserId() + "," + user.getUserName();
	}

}
