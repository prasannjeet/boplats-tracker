package com.prasannjeet.vaxjobostader;

import com.prasannjeet.vaxjobostader.jpa.HomesRepository;
import com.prasannjeet.vaxjobostader.jpa.LastUpdated;
import com.prasannjeet.vaxjobostader.jpa.LastUpdatedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApplicationTests {

	@Autowired
	HomesRepository homesRepository;

	@Autowired
	LastUpdatedRepository lastUpdatedRepository;

	@BeforeEach
	public void setUp() {
		LastUpdated lastUpdated = new LastUpdated();
		lastUpdated.setId(1);
		// set other properties as needed
		lastUpdatedRepository.save(lastUpdated);
	}

	@Test
	void contextLoads() {
	}

}
