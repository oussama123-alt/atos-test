package com.example.atosTest.user.web;

import com.example.atosTest.user.domain.User.Gender;
import com.example.atosTest.user.dto.UserRegistrationRequest;
import com.example.atosTest.user.dto.UserResponse;
import com.example.atosTest.user.exception.RegistrationNotAllowedException;
import com.example.atosTest.user.exception.UnknownCountryException;
import com.example.atosTest.user.exception.UserNotFoundException;
import com.example.atosTest.user.exception.UsernameAlreadyTakenException;
import com.example.atosTest.user.service.UserService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final String USERS_URL = "/api/users";
    private static final String REGISTER_URL = USERS_URL + "/add";
    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    private static final String VALID_BODY = """
            {
              "username": "jdupont",
              "birthdate": "1990-03-21",
              "countryOfResidence": "France",
              "phoneNumber": "+33612345678",
              "gender": "MALE"
            }
            """;

    private static final UserResponse CREATED_USER = new UserResponse(
            1L, "jdupont", LocalDate.of(1990, 3, 21), "FR", "+33612345678", Gender.MALE,
            Instant.parse("2026-06-15T10:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private ResultActions postUser(String body) throws Exception {
        return mockMvc.perform(post(REGISTER_URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Nested
    class Register {

        @Test
        void returns201WithTheCreatedUserAndItsLocation() throws Exception {
            when(userService.register(any())).thenReturn(CREATED_USER);

            postUser(VALID_BODY)
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/users/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.username").value("jdupont"))
                    .andExpect(jsonPath("$.birthdate").value("1990-03-21"))
                    .andExpect(jsonPath("$.countryOfResidence").value("FR"))
                    .andExpect(jsonPath("$.phoneNumber").value("+33612345678"))
                    .andExpect(jsonPath("$.gender").value("MALE"))
                    .andExpect(jsonPath("$.createdAt").value("2026-06-15T10:00:00Z"));

            verify(userService).register(new UserRegistrationRequest(
                    "jdupont", LocalDate.of(1990, 3, 21), "France", "+33612345678", Gender.MALE));
        }


        @Test
        void returns422WhenTheUserIsNotEligible() throws Exception {
            when(userService.register(any()))
                    .thenThrow(new RegistrationNotAllowedException("Only residents of France can register"));

            postUser(VALID_BODY)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("Registration not allowed"))
                    .andExpect(jsonPath("$.detail").value("Only residents of France can register"));
        }


    }

    @Nested
    class GetUser {

        @Test
        void returns200WithTheUser() throws Exception {
            when(userService.getById(1L)).thenReturn(CREATED_USER);

            mockMvc.perform(get(USERS_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.username").value("jdupont"));
        }

        @Test
        void returns404WhenTheUserDoesNotExist() throws Exception {
            when(userService.getById(42L)).thenThrow(new UserNotFoundException(42L));

            mockMvc.perform(get(USERS_URL + "/42"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                    .andExpect(jsonPath("$.title").value("User not found"))
                    .andExpect(jsonPath("$.detail").value("No user found with id 42"));
        }

    }
}
