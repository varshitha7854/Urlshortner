package com.example.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Base62CodeGeneratorTests {

	private final Base62CodeGenerator generator = new Base62CodeGenerator();

	@Test
	void randomCodeUsesConfiguredLengthAndAlphabet() {
		String code = generator.randomCode(7);

		assertThat(code).hasSize(7);
		assertThat(code).matches("[0-9A-Za-z]{7}");
	}
}
