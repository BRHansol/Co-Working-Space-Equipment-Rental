package com.example.roombooking.exception;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ยิงผ่าน controller -> service -> repository จริง เพราะ Spring Data โยน error ตอน query
// ค่า sort=string มาจากตัวอย่างใน Swagger UI ถ้าไม่ลบออกเดิมจะได้ 500
@SpringBootTest
@AutoConfigureMockMvc
class InvalidSortIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/rooms", "/api/v1/equipments", "/api/v1/rooms/1/bookings"})
    void unknownSortProperty_returns400(String path) throws Exception {
        mockMvc.perform(get(path).param("sort", "string"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/rooms?sort=name,asc", "/api/v1/equipments?sort=name,desc"})
    void validSortProperty_stillWorks(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }
}
