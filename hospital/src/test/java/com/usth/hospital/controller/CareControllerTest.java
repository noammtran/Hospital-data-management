package com.usth.hospital.controller;

import com.usth.hospital.config.SecurityConfig;
import com.usth.hospital.dto.response.PageResponse;
import com.usth.hospital.repository.AppUserRepository;
import com.usth.hospital.service.impl.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CareController.class)
@Import(SecurityConfig.class)
@WithMockUser(authorities={"CARE_VIEW","CARE_REGISTER"})
class CareControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean CareQueryService query;
    @MockitoBean CareWorkflowService workflow;
    @MockitoBean CareConfigurationService configuration;
    @MockitoBean AppUserRepository users;

    @Test @WithAnonymousUser void authenticationIsRequired() throws Exception {
        mvc.perform(get("/api/v1/care/visits")).andExpect(status().isUnauthorized());
    }
    @Test @WithMockUser(authorities="VIEW_OWN_RECORD") void patientCannotUseAnyStaffCareEndpoint() throws Exception {
        mvc.perform(get("/api/v1/care/visits")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/care/options")).andExpect(status().isForbidden()); verifyNoInteractions(query);
    }
    @Test void roomQueueUsesServerPaginationAndPresenceFilter() throws Exception {
        when(query.tasks(10L,null,null,"ARRIVED",true,"Test",2,10)).thenReturn(PageResponse.from(Page.empty()));
        mvc.perform(get("/api/v1/care/tasks").param("pointId","10").param("arrival","ARRIVED").param("search","Test").param("page","2").param("pageSize","10"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.items").isArray());
        verify(query).tasks(10L,null,null,"ARRIVED",true,"Test",2,10);
    }
    @Test void invalidPageAndWorkflowStateAreRejected() throws Exception {
        mvc.perform(get("/api/v1/care/visits").param("page","0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/care/tasks").param("pageSize","101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/care/tasks").param("arrival","ANYWHERE")).andExpect(status().isBadRequest()); verifyNoInteractions(query);
    }
    @Test void registrationRequiresPatientDestinationAndComplaint() throws Exception {
        mvc.perform(post("/api/v1/care/visits").contentType(MediaType.APPLICATION_JSON).content("{\"patientId\":\"p\",\"pointId\":1,\"complaint\":\" \"}"))
            .andExpect(status().isBadRequest()); verifyNoInteractions(workflow);
    }
    @Test void nestedOrdersAreValidatedBeforeMutation() throws Exception {
        mvc.perform(post("/api/v1/care/visits/v/orders").contentType(MediaType.APPLICATION_JSON).content("{\"requestId\":\"abc\",\"items\":[{\"serviceId\":0,\"reason\":\"\"}]}"))
            .andExpect(status().isBadRequest()); verifyNoInteractions(workflow);
    }
    @Test void acknowledgementRequiresTheRevisionActuallyRead() throws Exception {
        mvc.perform(post("/api/v1/care/tasks/1/acknowledge").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()); verifyNoInteractions(workflow);
    }
    @Test void receptionistCannotChangeAssignments() throws Exception {
        mvc.perform(put("/api/v1/care/assignments").contentType(MediaType.APPLICATION_JSON).content("{\"userId\":1,\"pointId\":2,\"perform\":true,\"verify\":true,\"active\":true}"))
            .andExpect(status().isForbidden()); verifyNoInteractions(configuration);
    }
}
