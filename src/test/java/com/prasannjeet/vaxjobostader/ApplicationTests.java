package com.prasannjeet.vaxjobostader;

import com.prasannjeet.vaxjobostader.jpa.HomesRepository;
import com.prasannjeet.vaxjobostader.jpa.UserSelectedHomesRepository;
import com.prasannjeet.vaxjobostader.testbeans.Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(Config.class)
class ApplicationTests {

	@Autowired
	HomesRepository homesRepository;

	@Autowired
	UserSelectedHomesRepository userSelectedHomesRepository;

	@Test
	void contextLoads() {
	}

}
