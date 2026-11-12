package com.example.atosTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests running the whole application against the embedded H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class UserApiIntegrationTest {

    private static final String USERS_URL = "/api/users";
    private static final String REGISTER_URL = USERS_URL + "/add";

    @Autowired
    private MockMvc mockMvc;

    private ResultActions register(String username, LocalDate birthdate, String country) throws Exception {
        return mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "%s", "birthdate": "%s", "countryOfResidence": "%s", "gender": "FEMALE"}
                        """.formatted(username, birthdate, country)));
    }

    @Test
    void registersAUserThenReturnsItsDetails() throws Exception {
        String location = register("mmartin", LocalDate.of(1985, 7, 14), "France")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.countryOfResidence").value("FR"))
                .andReturn().getResponse().getHeader("Location");

        assertThat(location).matches("http://localhost/api/users/\\d+");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mmartin"))
                .andExpect(jsonPath("$.birthdate").value("1985-07-14"))
                .andExpect(jsonPath("$.countryOfResidence").value("FR"))
                .andExpect(jsonPath("$.gender").value("FEMALE"))
                .andExpect(jsonPath("$.phoneNumber").isEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void rejectsADuplicateUsernameIgnoringCase() throws Exception {
        register("mmartin", LocalDate.of(1985, 7, 14), "FR").andExpect(status().isCreated());

        register("MMartin", LocalDate.of(1985, 7, 14), "FR")
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsAMinor() throws Exception {
        register("young", LocalDate.now().minusYears(17), "FR")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("User must be at least 18 years old to register"));
    }

    @Test
    void rejectsAForeignResident() throws Exception {
        register("hans", LocalDate.of(1985, 7, 14), "Germany")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Only residents of France can register"));
    }

    @Test
    void returns404ForAnUnknownUser() throws Exception {
        mockMvc.perform(get(USERS_URL + "/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void logsInputsOutputsAndProcessingTimeOfEachCall(CapturedOutput output) throws Exception {
        register("logged", LocalDate.of(1985, 7, 14), "FR")
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));

        assertThat(output.getOut())
                .contains("--> UserController.register input=[UserRegistrationRequest[username=logged")
                .containsPattern("<-- UserController\\.register output=.*username=logged.* duration=\\d+ms");
    }

    @Test
    void logsExceptionsThrownByControllers(CapturedOutput output) throws Exception {
        mockMvc.perform(get(USERS_URL + "/999999")).andExpect(status().isNotFound());

        assertThat(output.getOut())
                .contains("--> UserController.getUser input=[999999]")
                .containsPattern("<-- UserController\\.getUser threw UserNotFoundException: "
                        + "No user found with id 999999 duration=\\d+ms");
    }
}
