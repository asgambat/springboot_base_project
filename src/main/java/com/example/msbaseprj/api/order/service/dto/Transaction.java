package com.example.msbaseprj.api.order.service.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record Transaction(String id, String user, Double amount) {

	public static List<String> duplicates(List<Transaction> transactions) {
		return transactions.stream().collect(Collectors.groupingBy(Transaction::id)).entrySet().stream()
				.filter(hasDuplicates()).map(Map.Entry::getKey).toList();
	}

	public static Double avarageAmount(List<Transaction> transactions) {
		return transactions.stream().collect(Collectors.averagingDouble(Transaction::amount));
	}

	public static Optional<String> mostFrequentUser(List<Transaction> transactions) {
		return transactions.stream().collect(Collectors.groupingBy(Transaction::user, Collectors.counting())).entrySet()
				.stream().max(Map.Entry.<String, Long>comparingByValue()).map(Entry::getKey);
	}

	private static Predicate<? super Entry<String, List<Transaction>>> hasDuplicates() {
		return it -> it.getValue().size() > 1;
	}

	private static List<Long> findDuplicateTransactions(List<Transaction> transactions) {
		return transactions.stream().collect(Collectors.groupingBy(Transaction::id, Collectors.counting())).entrySet()
				.stream().filter(entry -> entry.getValue() > 1).map(Map.Entry::getValue).toList();
	}

	private static Character findFirstNotRepeatingChar(String inpuString) {
		Map<Character, Long> charCountMap = inpuString.chars().mapToObj(c -> (char) c)
				.collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));

		return charCountMap.entrySet().stream().filter(entry -> entry.getValue() == 1).map(Map.Entry::getKey)
				.findFirst().orElse(null);
	}

}
